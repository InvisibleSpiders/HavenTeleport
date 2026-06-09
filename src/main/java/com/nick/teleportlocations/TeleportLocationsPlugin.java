package com.nick.teleportlocations;

import com.nick.teleportlocations.command.AdminTeleportCommand;
import com.nick.teleportlocations.command.BukkitOnlinePlayerLookup;
import com.nick.teleportlocations.command.BukkitPlayerLookup;
import com.nick.teleportlocations.command.PlayerLocationCommand;
import com.nick.teleportlocations.command.TeleportRequestCommand;
import com.nick.teleportlocations.dialog.DialogActionExecutor;
import com.nick.teleportlocations.dialog.DialogActionRouter;
import com.nick.teleportlocations.claim.BukkitLandClaimsGateway;
import com.nick.teleportlocations.dialog.DialogMenuService;
import com.nick.teleportlocations.dialog.PaperDialogPresenter;
import com.nick.teleportlocations.elevator.ElevatorActivationService;
import com.nick.teleportlocations.elevator.ElevatorCooldownService;
import com.nick.teleportlocations.elevator.bukkit.ElevatorItemService;
import com.nick.teleportlocations.elevator.bukkit.ElevatorParticleTask;
import com.nick.teleportlocations.listener.ElevatorListener;
import com.nick.teleportlocations.listener.SpawnListener;
import com.nick.teleportlocations.teleport.ManagedTeleportService;
import com.nick.teleportlocations.teleport.effect.BukkitTeleportEffectService;
import com.nick.teleportlocations.tpa.TeleportWarmupService;
import dev.invisiblespiders.haven.api.HavenAPI;
import dev.invisiblespiders.haven.api.service.HavenDataSource;
import dev.invisiblespiders.haven.api.service.HavenEconomyService;
import java.io.File;
import java.time.Instant;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.java.JavaPlugin;

public final class TeleportLocationsPlugin extends JavaPlugin {
    private RuntimeServices services;
    private BukkitTask elevatorParticleTask;
    private ManagedTeleportService managedTeleports;

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();
        saveDefaultConfig();
        saveResourceIfMissing("messages.yml");
        services = RuntimeServices.open(
                HavenAPI.get(HavenDataSource.class),
                HavenAPI.optional(HavenEconomyService.class),
                BukkitLandClaimsGateway.discover(getServer().getServicesManager()),
                getClassLoader()
        );
        managedTeleports = new ManagedTeleportService(
                new BukkitTeleportEffectService(services.config().teleportEffects(), getLogger()),
                runnable -> getServer().getScheduler().runTask(this, runnable)
        );
        DialogRuntime dialogs = registerCommands();
        getServer().getPluginManager().registerEvents(new SpawnListener(
                services.spawnService(),
                services.spawnPolicyService(),
                services.teleportSafetyService(),
                managedTeleports
        ), this);
        registerElevators(dialogs);
        getLogger().info("TeleportLocations enabled.");
    }

    @Override
    public void onDisable() {
        if (elevatorParticleTask != null) {
            elevatorParticleTask.cancel();
            elevatorParticleTask = null;
        }
        if (services != null) {
            services.close();
            services = null;
        }
        getLogger().info("TeleportLocations disabled.");
    }

    private void registerElevators(DialogRuntime dialogs) {
        ElevatorItemService elevatorItems = new ElevatorItemService(this);
        getServer().removeRecipe(elevatorItems.recipeKey());
        getServer().addRecipe(elevatorItems.createRecipe());

        ElevatorActivationService activations = new ElevatorActivationService(
                services.elevatorService(),
                new ElevatorCooldownService(services.config().elevatorCooldownSeconds(), Instant::now),
                services.config().elevatorMaxDistance()
        );
        getServer().getPluginManager().registerEvents(
                new ElevatorListener(
                        services.elevatorService(),
                        activations,
                        elevatorItems,
                        dialogs.menus(),
                        dialogs.presenter(),
                        services.adminBypassService(),
                        services.teleportAccessService(),
                        managedTeleports
                ),
                this
        );
        if (services.config().elevatorParticlesEnabled()) {
            int interval = Math.max(1, services.config().elevatorParticleIntervalTicks());
            elevatorParticleTask = getServer().getScheduler().runTaskTimer(
                    this,
                    new ElevatorParticleTask(services.elevatorRepository()),
                    interval,
                    interval
            );
        }
    }

    private void saveResourceIfMissing(String name) {
        File target = getDataFolder().toPath().resolve(name).toFile();
        if (!target.exists()) {
            saveResource(name, false);
        }
    }

    private DialogRuntime registerCommands() {
        getCommand("ht").setExecutor(new AdminTeleportCommand(
                services.spawnService(),
                services.limitService(),
                services.serverWarpService(),
                services.adminBypassService(),
                new BukkitPlayerLookup(),
                new BukkitOnlinePlayerLookup(),
                managedTeleports
        ));
        TeleportWarmupService tpaWarmups = new TeleportWarmupService(
                this,
                services.config().tpaWarmupSeconds(),
                services.config().tpaCancelWarmupOnMove()
        );
        TeleportRequestCommand tpaCommand = new TeleportRequestCommand(
                new BukkitOnlinePlayerLookup(),
                services.teleportRequestService(),
                tpaWarmups,
                services.teleportAccessService(),
                services.adminBypassService(),
                services.config().tpaEnabled(),
                managedTeleports
        );
        getServer().getPluginManager().registerEvents(tpaWarmups, this);
        getServer().getPluginManager().registerEvents(tpaCommand, this);
        getCommand("tpa").setExecutor(tpaCommand);
        getCommand("tpahere").setExecutor(tpaCommand);
        getCommand("tpaccept").setExecutor(tpaCommand);
        getCommand("tpdecline").setExecutor(tpaCommand);
        DialogMenuService dialogMenus = new DialogMenuService();
        PaperDialogPresenter dialogPresenter = new PaperDialogPresenter();
        DialogActionRouter dialogActions = new DialogActionRouter(
                services.homeService(),
                services.playerWarpService(),
                services.shopWarpService(),
                services.outpostService(),
                services.serverWarpService(),
                services.elevatorService(),
                dialogMenus,
                services.adminBypassService(),
                this::hasOnlinePermission
        );
        dialogPresenter.setActionHandler(new DialogActionExecutor(
                dialogActions,
                dialogPresenter,
                services.teleportChargeService(),
                services.teleportAccessService(),
                services.teleportSafetyService(),
                services.adminBypassService(),
                managedTeleports
        ));
        PlayerLocationCommand playerCommand = new PlayerLocationCommand(
                services.homeService(),
                services.playerWarpService(),
                services.shopWarpService(),
                services.outpostService(),
                services.serverWarpService(),
                services.spawnService(),
                services.teleportChargeService(),
                services.teleportAccessService(),
                services.teleportSafetyService(),
                services.adminBypassService(),
                dialogMenus,
                dialogPresenter,
                hideInaccessibleDestinations(),
                managedTeleports
        );
        getCommand("home").setExecutor(playerCommand);
        getCommand("homes").setExecutor(playerCommand);
        getCommand("sethome").setExecutor(playerCommand);
        getCommand("delhome").setExecutor(playerCommand);
        getCommand("mainhome").setExecutor(playerCommand);
        getCommand("warp").setExecutor(playerCommand);
        getCommand("warps").setExecutor(playerCommand);
        getCommand("setwarp").setExecutor(playerCommand);
        getCommand("delwarp").setExecutor(playerCommand);
        getCommand("shops").setExecutor(playerCommand);
        getCommand("setshop").setExecutor(playerCommand);
        getCommand("delshop").setExecutor(playerCommand);
        getCommand("setoutpost").setExecutor(playerCommand);
        getCommand("outpost").setExecutor(playerCommand);
        getCommand("deloutpost").setExecutor(playerCommand);
        getCommand("spawn").setExecutor(playerCommand);
        return new DialogRuntime(dialogMenus, dialogPresenter);
    }

    private boolean hasOnlinePermission(java.util.UUID playerId, String permission) {
        Player player = getServer().getPlayer(playerId);
        return player != null && player.hasPermission(permission);
    }

    private boolean hideInaccessibleDestinations() {
        return "hide".equalsIgnoreCase(services.config().inaccessibleDestinationMode());
    }

    private record DialogRuntime(DialogMenuService menus, PaperDialogPresenter presenter) {
    }
}

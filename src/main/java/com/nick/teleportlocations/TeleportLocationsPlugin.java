package com.nick.teleportlocations;

import com.nick.teleportlocations.command.AdminTeleportCommand;
import com.nick.teleportlocations.command.BukkitPlayerLookup;
import com.nick.teleportlocations.command.PlayerLocationCommand;
import com.nick.teleportlocations.dialog.DialogActionExecutor;
import com.nick.teleportlocations.dialog.DialogActionRouter;
import com.nick.teleportlocations.claim.BukkitLandClaimsGateway;
import com.nick.teleportlocations.dialog.DialogMenuService;
import com.nick.teleportlocations.dialog.PaperDialogPresenter;
import com.nick.teleportlocations.listener.SpawnListener;
import dev.invisiblespiders.haven.api.HavenAPI;
import dev.invisiblespiders.haven.api.service.HavenDataSource;
import dev.invisiblespiders.haven.api.service.HavenEconomyService;
import java.io.File;
import org.bukkit.plugin.java.JavaPlugin;

public final class TeleportLocationsPlugin extends JavaPlugin {
    private RuntimeServices services;

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
        registerCommands();
        getServer().getPluginManager().registerEvents(new SpawnListener(services.spawnService(), services.spawnPolicyService()), this);
        getLogger().info("TeleportLocations enabled.");
    }

    @Override
    public void onDisable() {
        if (services != null) {
            services.close();
            services = null;
        }
        getLogger().info("TeleportLocations disabled.");
    }

    private void saveResourceIfMissing(String name) {
        File target = getDataFolder().toPath().resolve(name).toFile();
        if (!target.exists()) {
            saveResource(name, false);
        }
    }

    private void registerCommands() {
        getCommand("ht").setExecutor(new AdminTeleportCommand(
                services.spawnService(),
                services.limitService(),
                services.serverWarpService(),
                new BukkitPlayerLookup()
        ));
        DialogMenuService dialogMenus = new DialogMenuService();
        PaperDialogPresenter presenter = new PaperDialogPresenter();
        DialogActionRouter dialogActions = new DialogActionRouter(
                services.homeService(),
                services.playerWarpService(),
                services.shopWarpService(),
                services.outpostService(),
                services.serverWarpService(),
                dialogMenus
        );
        presenter.setActionHandler(new DialogActionExecutor(dialogActions, presenter, services.teleportChargeService()));
        PlayerLocationCommand playerCommand = new PlayerLocationCommand(
                services.homeService(),
                services.playerWarpService(),
                services.shopWarpService(),
                services.outpostService(),
                services.serverWarpService(),
                services.spawnService(),
                services.teleportChargeService(),
                dialogMenus,
                presenter
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
    }
}

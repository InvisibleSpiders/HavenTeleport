package com.nick.teleportlocations.listener;

import com.nick.teleportlocations.admin.AdminBypassService;
import com.nick.teleportlocations.bukkit.BukkitLocations;
import com.nick.teleportlocations.cost.ChargeResult;
import com.nick.teleportlocations.dialog.DialogMenuService;
import com.nick.teleportlocations.dialog.PaperDialogPresenter;
import com.nick.teleportlocations.elevator.ElevatorCooldownService;
import com.nick.teleportlocations.home.HomeService;
import com.nick.teleportlocations.location.SavedPosition;
import com.nick.teleportlocations.location.TeleportLocation;
import com.nick.teleportlocations.location.LocationService;
import com.nick.teleportlocations.serverwarp.ServerWarpService;
import com.nick.teleportlocations.shop.ShopWarpService;
import com.nick.teleportlocations.teleport.ManagedTeleportService;
import com.nick.teleportlocations.teleport.TeleportAccessResult;
import com.nick.teleportlocations.teleport.TeleportAccessService;
import com.nick.teleportlocations.teleport.TeleportChargeMessages;
import com.nick.teleportlocations.teleport.TeleportChargeService;
import com.nick.teleportlocations.teleport.TeleportSafetyResult;
import com.nick.teleportlocations.teleport.TeleportSafetyService;
import com.nick.teleportlocations.teleportblock.TeleportBlock;
import com.nick.teleportlocations.teleportblock.TeleportBlockResult;
import com.nick.teleportlocations.teleportblock.TeleportBlockService;
import com.nick.teleportlocations.warp.PlayerWarpService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class TeleportBlockListener implements Listener {
    private final TeleportBlockService teleportBlocks;
    private final LocationService locations;
    private final HomeService homes;
    private final PlayerWarpService warps;
    private final ShopWarpService shops;
    private final ServerWarpService serverWarps;
    private final TeleportSafetyService safety;
    private final TeleportAccessService access;
    private final TeleportChargeService charges;
    private final ElevatorCooldownService cooldowns;
    private final DialogMenuService menus;
    private final PaperDialogPresenter presenter;
    private final AdminBypassService bypass;
    private final ManagedTeleportService managedTeleports;
    private final int maxDistance;
    private final Map<UUID, SavedPosition> selectedBlocks = new HashMap<>();

    public TeleportBlockListener(
            TeleportBlockService teleportBlocks,
            LocationService locations,
            HomeService homes,
            PlayerWarpService warps,
            ShopWarpService shops,
            ServerWarpService serverWarps,
            TeleportSafetyService safety,
            TeleportAccessService access,
            TeleportChargeService charges,
            ElevatorCooldownService cooldowns,
            DialogMenuService menus,
            PaperDialogPresenter presenter,
            AdminBypassService bypass,
            ManagedTeleportService managedTeleports,
            int maxDistance
    ) {
        this.teleportBlocks = teleportBlocks;
        this.locations = locations;
        this.homes = homes;
        this.warps = warps;
        this.shops = shops;
        this.serverWarps = serverWarps;
        this.safety = safety;
        this.access = access;
        this.charges = charges;
        this.cooldowns = cooldowns;
        this.menus = menus;
        this.presenter = presenter;
        this.bypass = bypass;
        this.managedTeleports = managedTeleports;
        this.maxDistance = Math.max(1, maxDistance);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!isTeleportBlockMaterial(event.getBlockPlaced().getType())) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("teleportlocations.teleportblock.place")) {
            event.setCancelled(true);
            send(player, "You do not have permission to place teleport blocks.", NamedTextColor.RED);
            return;
        }
        boolean bypassClaims = claimBypass(player);
        remindBypass(player, bypassClaims);
        TeleportBlockResult result = teleportBlocks.place(player.getUniqueId(), BukkitLocations.save(event.getBlockPlaced().getLocation()), bypassClaims);
        if (result.status() == TeleportBlockResult.Status.CLAIM_DENIED) {
            event.setCancelled(true);
            send(player, "Teleport blocks can only be placed in claims you own.", NamedTextColor.RED);
            return;
        }
        send(player, "Teleport block placed.", NamedTextColor.GREEN);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        SavedPosition position = BukkitLocations.save(event.getBlock().getLocation());
        if (teleportBlocks.findAt(position).isEmpty()) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("teleportlocations.teleportblock.break")) {
            event.setCancelled(true);
            send(player, "You do not have permission to break teleport blocks.", NamedTextColor.RED);
            return;
        }
        boolean bypassClaims = claimBypass(player);
        remindBypass(player, bypassClaims);
        TeleportBlockResult result = teleportBlocks.breakBlock(player.getUniqueId(), position, bypassClaims);
        if (result.status() == TeleportBlockResult.Status.ACCESS_DENIED) {
            event.setCancelled(true);
            send(player, "You need build access in this claim to break that teleport block.", NamedTextColor.RED);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null || !changedBlock(event.getFrom(), event.getTo())) {
            return;
        }
        Block sourceBlock = blockBelow(event.getTo());
        if (!isTeleportBlockMaterial(sourceBlock.getType()) || !isLit(sourceBlock)) {
            return;
        }
        activate(event.getPlayer(), sourceBlock);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        SavedPosition position = BukkitLocations.save(event.getClickedBlock().getLocation());
        if (teleportBlocks.findAt(position).isEmpty()) {
            return;
        }
        Player player = event.getPlayer();
        if (player.isSneaking()) {
            event.setCancelled(true);
            showSettings(player, position);
            return;
        }
        if (event.getItem() == null || event.getItem().getType() != Material.ECHO_SHARD) {
            return;
        }
        event.setCancelled(true);
        linkWithEchoShard(player, position);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cooldowns.clear(event.getPlayer().getUniqueId());
        selectedBlocks.remove(event.getPlayer().getUniqueId());
    }

    private void activate(Player player, Block sourceBlock) {
        SavedPosition sourcePosition = BukkitLocations.save(sourceBlock.getLocation());
        Optional<TeleportBlock> source = teleportBlocks.findAt(sourcePosition);
        if (source.isEmpty() || !player.hasPermission("teleportlocations.teleportblock.use")) {
            return;
        }
        boolean bypassClaims = claimBypass(player);
        if (!cooldowns.tryUse(player.getUniqueId(), player.hasPermission("teleportlocations.admin.bypass.cooldown"))) {
            managedTeleports.denied(player);
            send(player, "Teleport block is on cooldown for " + cooldowns.remainingSeconds(player.getUniqueId()) + "s.", NamedTextColor.YELLOW);
            return;
        }
        if (!teleportBlocks.canUse(player.getUniqueId(), sourcePosition, bypassClaims)) {
            managedTeleports.denied(player);
            send(player, "You do not have access to use that teleport block.", NamedTextColor.RED);
            return;
        }
        Optional<TeleportBlock> destinationBlock = teleportBlocks.linkedDestination(source.orElseThrow());
        Optional<TeleportLocation> targetLocation = teleportBlocks.targetLocationId(source.orElseThrow()).flatMap(locations::findById);
        if (targetLocation.isPresent()) {
            teleportToLocation(player, targetLocation.orElseThrow(), bypassClaims);
            return;
        }
        if (destinationBlock.isEmpty()) {
            managedTeleports.denied(player);
            send(player, "That teleport block is not linked.", NamedTextColor.RED);
            return;
        }
        Location destination = destinationLocation(destinationBlock.orElseThrow(), player);
        if (destination == null || !isLit(destination.getBlock())) {
            managedTeleports.denied(player);
            send(player, "The linked teleport block is inactive.", NamedTextColor.RED);
            return;
        }
        if (!teleportBlocks.canUse(player.getUniqueId(), destinationBlock.orElseThrow().position(), bypassClaims)) {
            managedTeleports.denied(player);
            send(player, "You do not have access to the linked teleport block.", NamedTextColor.RED);
            return;
        }
        managedTeleports.teleport(player, destination.clone().add(0.5D, 1.0D, 0.5D));
    }

    private void teleportToLocation(Player player, TeleportLocation target, boolean bypassClaims) {
        TeleportSafetyResult safetyResult = safety.validate(target.position());
        if (!safetyResult.safe()) {
            managedTeleports.denied(player);
            send(player, "That teleport block target is unsafe: " + safetyResult.reason() + ".", NamedTextColor.RED);
            return;
        }
        Location destination = BukkitLocations.load(target.position());
        if (destination == null) {
            managedTeleports.denied(player);
            send(player, "That teleport block target world is not loaded.", NamedTextColor.RED);
            return;
        }
        TeleportAccessResult accessResult = access.canEnter(player.getUniqueId(), target.position(), bypassClaims);
        if (!accessResult.allowed()) {
            managedTeleports.denied(player);
            send(player, "You cannot enter that teleport block target.", NamedTextColor.RED);
            return;
        }
        ChargeResult charge = charges.chargeIfNeeded(
                player.getUniqueId(),
                player.hasPermission("teleportlocations.admin.bypass.cost"),
                target
        );
        if (!charge.success()) {
            managedTeleports.denied(player);
            send(player, TeleportChargeMessages.failure(charge.reason()), NamedTextColor.RED);
            return;
        }
        managedTeleports.teleport(player, destination);
    }

    private void showSettings(Player player, SavedPosition position) {
        Optional<TeleportBlock> block = teleportBlocks.findAt(position);
        if (block.isEmpty()) {
            return;
        }
        boolean bypassClaims = claimBypass(player);
        remindBypass(player, bypassClaims);
        boolean canEdit = player.hasPermission("teleportlocations.teleportblock.link");
        ArrayList<TeleportLocation> playerTargets = new ArrayList<>();
        playerTargets.addAll(homes.listHomes(player.getUniqueId()));
        playerTargets.addAll(warps.ownerWarps(player.getUniqueId()));
        playerTargets.addAll(shops.ownerShops(player.getUniqueId()));
        ArrayList<TeleportLocation> adminTargets = new ArrayList<>();
        if (player.hasPermission("teleportlocations.admin.teleportblock") && bypassClaims) {
            spawnTarget().ifPresent(adminTargets::add);
            adminTargets.addAll(serverWarps.visibleWarps());
            adminTargets.addAll(warps.visibleWarps(player.getUniqueId()));
            adminTargets.addAll(shops.visibleShops(player.getUniqueId()));
        }
        presenter.show(player, menus.teleportBlockSettingsMenu(block.orElseThrow(), playerTargets, adminTargets, canEdit));
    }

    private Optional<TeleportLocation> spawnTarget() {
        return locations.list("spawn").stream().findFirst();
    }

    private void linkWithEchoShard(Player player, SavedPosition position) {
        if (!player.hasPermission("teleportlocations.teleportblock.link")) {
            send(player, "You do not have permission to link teleport blocks.", NamedTextColor.RED);
            return;
        }
        SavedPosition first = selectedBlocks.remove(player.getUniqueId());
        if (first == null) {
            selectedBlocks.put(player.getUniqueId(), position);
            send(player, "First teleport block selected. Right-click another with an Echo Shard to link them.", NamedTextColor.YELLOW);
            return;
        }
        boolean bypassClaims = claimBypass(player);
        remindBypass(player, bypassClaims);
        TeleportBlockResult result = teleportBlocks.link(player.getUniqueId(), first, position, bypassClaims, maxDistance);
        switch (result.status()) {
            case LINKED -> send(player, "Teleport blocks linked.", NamedTextColor.GREEN);
            case DISTANCE_TOO_FAR -> send(player, "Teleport blocks are too far apart.", NamedTextColor.RED);
            case SAME_BLOCK -> send(player, "Select two different teleport blocks.", NamedTextColor.RED);
            case ACCESS_DENIED -> send(player, "You need access to edit both teleport blocks.", NamedTextColor.RED);
            case NOT_FOUND -> send(player, "One of those teleport blocks no longer exists.", NamedTextColor.RED);
            default -> send(player, "Teleport blocks could not be linked.", NamedTextColor.RED);
        }
    }

    private boolean claimBypass(Player player) {
        return player.hasPermission("teleportlocations.admin.bypass.claims") && bypass.claims(player.getUniqueId());
    }

    private static boolean isTeleportBlockMaterial(Material material) {
        String name = material.name().toLowerCase(Locale.ROOT);
        return name.startsWith("waxed_") && name.endsWith("_copper_bulb");
    }

    private static boolean isLit(Block block) {
        return block.getBlockData() instanceof Lightable lightable && lightable.isLit();
    }

    private static Block blockBelow(Location location) {
        return location.clone().subtract(0.0D, 1.0D, 0.0D).getBlock();
    }

    private static Location destinationLocation(TeleportBlock block, Player player) {
        Location location = BukkitLocations.load(block.position());
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return new Location(
                location.getWorld(),
                block.blockX(),
                block.blockY(),
                block.blockZ(),
                player.getLocation().getYaw(),
                player.getLocation().getPitch()
        );
    }

    private static boolean changedBlock(Location from, Location to) {
        return from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ();
    }

    private static void remindBypass(Player player, boolean bypassClaims) {
        if (bypassClaims) {
            send(player, "Admin claim bypass is active for this teleport block action.", NamedTextColor.YELLOW);
        }
    }

    private static void send(Player player, String message, NamedTextColor color) {
        player.sendMessage(Component.text(message, color));
    }
}

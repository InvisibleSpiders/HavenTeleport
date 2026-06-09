package com.nick.teleportlocations.listener;

import com.nick.teleportlocations.bukkit.BukkitLocations;
import com.nick.teleportlocations.elevator.ElevatorActivationResult;
import com.nick.teleportlocations.elevator.ElevatorActivationService;
import com.nick.teleportlocations.elevator.ElevatorBlock;
import com.nick.teleportlocations.elevator.ElevatorDirection;
import com.nick.teleportlocations.elevator.ElevatorResult;
import com.nick.teleportlocations.elevator.ElevatorService;
import com.nick.teleportlocations.elevator.bukkit.ElevatorItemService;
import com.nick.teleportlocations.dialog.DialogActionRouter;
import com.nick.teleportlocations.dialog.DialogMenuService;
import com.nick.teleportlocations.dialog.PaperDialogPresenter;
import com.nick.teleportlocations.location.SavedPosition;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public final class ElevatorListener implements Listener {
    private final ElevatorService elevators;
    private final ElevatorActivationService activations;
    private final ElevatorItemService itemService;
    private final DialogMenuService menus;
    private final PaperDialogPresenter presenter;

    public ElevatorListener(
            ElevatorService elevators,
            ElevatorActivationService activations,
            ElevatorItemService itemService,
            DialogMenuService menus,
            PaperDialogPresenter presenter
    ) {
        this.elevators = elevators;
        this.activations = activations;
        this.itemService = itemService;
        this.menus = menus;
        this.presenter = presenter;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!itemService.isElevatorItem(event.getItemInHand())) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("teleportlocations.elevator.place")) {
            event.setCancelled(true);
            send(player, "You do not have permission to place elevator blocks.", NamedTextColor.RED);
            return;
        }
        ElevatorResult result = elevators.place(player.getUniqueId(), BukkitLocations.save(event.getBlockPlaced().getLocation()), false);
        if (result.status() == ElevatorResult.Status.CLAIM_DENIED) {
            event.setCancelled(true);
            send(player, "Elevator blocks can only be placed in claims you own.", NamedTextColor.RED);
            return;
        }
        send(player, "Elevator block placed.", NamedTextColor.GREEN);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        SavedPosition position = BukkitLocations.save(event.getBlock().getLocation());
        if (elevators.findAt(position).isEmpty()) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("teleportlocations.elevator.break")) {
            event.setCancelled(true);
            send(player, "You do not have permission to break elevator blocks.", NamedTextColor.RED);
            return;
        }
        ElevatorResult result = elevators.breakBlock(player.getUniqueId(), position, false);
        if (result.status() == ElevatorResult.Status.ACCESS_DENIED) {
            event.setCancelled(true);
            send(player, "You need build access in this claim to break that elevator.", NamedTextColor.RED);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null || event.getTo().getY() <= event.getFrom().getY() + 0.1) {
            return;
        }
        activate(event.getPlayer(), blockBelow(event.getFrom()), ElevatorDirection.UP);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) {
            return;
        }
        activate(event.getPlayer(), blockBelow(event.getPlayer().getLocation()), ElevatorDirection.DOWN);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        SavedPosition position = BukkitLocations.save(event.getClickedBlock().getLocation());
        Optional<ElevatorBlock> elevator = elevators.findAt(position);
        if (elevator.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        if (event.getPlayer().isSneaking() && event.getPlayer().hasPermission("teleportlocations.elevator.menu")) {
            Player player = event.getPlayer();
            ElevatorBlock block = elevator.orElseThrow();
            boolean canEdit = block.ownerId().equals(player.getUniqueId()) || player.hasPermission("teleportlocations.admin.elevator");
            presenter.show(player, menus.elevatorSettingsMenu(
                    block,
                    canEdit,
                    particle -> player.hasPermission(DialogActionRouter.particlePermission(particle))
            ));
        }
    }

    private void activate(Player player, Block sourceBlock, ElevatorDirection direction) {
        SavedPosition source = BukkitLocations.save(sourceBlock.getLocation());
        Optional<ElevatorBlock> elevator = elevators.findAt(source);
        if (elevator.isEmpty() || !player.hasPermission("teleportlocations.elevator.use")) {
            return;
        }
        ElevatorActivationResult result = activations.activate(
                player.getUniqueId(),
                source,
                direction,
                false,
                player.hasPermission("teleportlocations.admin.bypass.cooldown")
        );
        if (result.status() == ElevatorActivationResult.Status.ACCESS_DENIED) {
            send(player, "You do not have access to use that elevator.", NamedTextColor.RED);
            return;
        }
        if (result.status() != ElevatorActivationResult.Status.TELEPORT) {
            return;
        }
        Location destination = destinationLocation(result.destination().orElseThrow(), player);
        if (destination != null) {
            player.teleportAsync(destination);
        }
    }

    private static Block blockBelow(Location location) {
        return location.clone().subtract(0.0, 1.0, 0.0).getBlock();
    }

    private static Location destinationLocation(ElevatorBlock block, Player player) {
        Location location = BukkitLocations.load(block.position());
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return new Location(
                location.getWorld(),
                block.blockX() + 0.5,
                block.blockY() + 1.0,
                block.blockZ() + 0.5,
                player.getLocation().getYaw(),
                player.getLocation().getPitch()
        );
    }

    private static void send(Player player, String message, NamedTextColor color) {
        player.sendMessage(Component.text(message, color));
    }
}

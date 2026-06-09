package com.nick.teleportlocations.command;

import com.nick.teleportlocations.admin.AdminBypassService;
import com.nick.teleportlocations.bukkit.BukkitLocations;
import com.nick.teleportlocations.teleport.ManagedTeleportService;
import com.nick.teleportlocations.teleport.TeleportAccessResult;
import com.nick.teleportlocations.teleport.TeleportAccessService;
import com.nick.teleportlocations.teleport.effect.NoOpTeleportEffectService;
import com.nick.teleportlocations.tpa.TeleportAcceptResult;
import com.nick.teleportlocations.tpa.TeleportDeclineResult;
import com.nick.teleportlocations.tpa.TeleportRequest;
import com.nick.teleportlocations.tpa.TeleportRequestResult;
import com.nick.teleportlocations.tpa.TeleportRequestService;
import com.nick.teleportlocations.tpa.TeleportRequestType;
import com.nick.teleportlocations.tpa.TeleportWarmupService;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class TeleportRequestCommand implements CommandExecutor, Listener {
    private final OnlinePlayerLookup players;
    private final TeleportRequestService requests;
    private final TeleportWarmupService warmups;
    private final TeleportAccessService access;
    private final AdminBypassService bypass;
    private final boolean enabled;
    private final ManagedTeleportService managedTeleports;
    private final Map<UUID, PendingWarmup> warmupsByMovingPlayer = new HashMap<>();
    private final Map<UUID, PendingWarmup> warmupsByDestinationPlayer = new HashMap<>();

    public TeleportRequestCommand(
            OnlinePlayerLookup players,
            TeleportRequestService requests,
            TeleportWarmupService warmups,
            TeleportAccessService access,
            AdminBypassService bypass,
            boolean enabled
    ) {
        this(
                players,
                requests,
                warmups,
                access,
                bypass,
                enabled,
                new ManagedTeleportService(new NoOpTeleportEffectService(), Runnable::run)
        );
    }

    public TeleportRequestCommand(
            OnlinePlayerLookup players,
            TeleportRequestService requests,
            TeleportWarmupService warmups,
            TeleportAccessService access,
            AdminBypassService bypass,
            boolean enabled,
            ManagedTeleportService managedTeleports
    ) {
        this.players = players;
        this.requests = requests;
        this.warmups = warmups;
        this.access = access;
        this.bypass = bypass;
        this.enabled = enabled;
        this.managedTeleports = managedTeleports;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }
        if (!enabled) {
            player.sendMessage(Component.text("Teleport requests are disabled.", NamedTextColor.RED));
            return true;
        }
        switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "tpa" -> request(player, args, TeleportRequestType.TPA, "teleportlocations.tpa");
            case "tpahere" -> request(player, args, TeleportRequestType.TPA_HERE, "teleportlocations.tpahere");
            case "tpaccept" -> accept(player, args);
            case "tpdecline" -> decline(player, args);
            case "tpcancel" -> cancel(player);
            case "tptoggle" -> toggle(player);
            default -> player.sendMessage(Component.text("Usage: /tpa <player>, /tpahere <player>, /tpaccept [player], /tpdecline [player], /tpcancel, /tptoggle", NamedTextColor.YELLOW));
        }
        return true;
    }

    private void request(Player requester, String[] args, TeleportRequestType type, String permission) {
        if (!requester.hasPermission(permission)) {
            requester.sendMessage(Component.text("You do not have permission to send that teleport request.", NamedTextColor.RED));
            return;
        }
        if (args.length == 0) {
            requester.sendMessage(Component.text(type == TeleportRequestType.TPA ? "Usage: /tpa <player>" : "Usage: /tpahere <player>", NamedTextColor.YELLOW));
            return;
        }
        Optional<Player> target = players.find(args[0]);
        if (target.isEmpty()) {
            requester.sendMessage(Component.text("Player " + args[0] + " is not online.", NamedTextColor.RED));
            return;
        }
        TeleportRequestResult result = requests.request(
                requester.getUniqueId(),
                target.orElseThrow().getUniqueId(),
                type,
                requester.hasPermission("teleportlocations.admin.bypass.tpa.cooldown")
        );
        switch (result.status()) {
            case REQUESTED -> {
                requester.sendMessage(Component.text("Teleport request sent to " + target.orElseThrow().getName() + ".", NamedTextColor.GREEN));
                sendRequestMessage(requester, target.orElseThrow(), type);
            }
            case SELF_REQUEST -> requester.sendMessage(Component.text("You cannot send a teleport request to yourself.", NamedTextColor.RED));
            case COOLDOWN -> {
                managedTeleports.denied(requester);
                requester.sendMessage(Component.text("Teleport requests are on cooldown for " + result.remainingCooldownSeconds() + "s.", NamedTextColor.YELLOW));
            }
            case TARGET_DISABLED -> {
                managedTeleports.denied(requester);
                requester.sendMessage(Component.text(target.orElseThrow().getName() + " is not accepting teleport requests.", NamedTextColor.RED));
            }
        }
    }

    private void accept(Player receiver, String[] args) {
        if (!receiver.hasPermission("teleportlocations.tpaccept")) {
            receiver.sendMessage(Component.text("You do not have permission to accept teleport requests.", NamedTextColor.RED));
            return;
        }
        Optional<UUID> requesterId = requesterId(args);
        if (args.length > 0 && requesterId.isEmpty()) {
            receiver.sendMessage(Component.text("Player " + args[0] + " is not online.", NamedTextColor.RED));
            return;
        }
        TeleportAcceptResult result = requests.accept(receiver.getUniqueId(), requesterId);
        if (result.status() == TeleportAcceptResult.Status.NOT_FOUND) {
            receiver.sendMessage(Component.text("No pending teleport request found.", NamedTextColor.RED));
            return;
        }
        executeAcceptedRequest(receiver, result.request().orElseThrow());
    }

    private void decline(Player receiver, String[] args) {
        if (!receiver.hasPermission("teleportlocations.tpdecline")) {
            receiver.sendMessage(Component.text("You do not have permission to decline teleport requests.", NamedTextColor.RED));
            return;
        }
        Optional<UUID> requesterId = requesterId(args);
        if (args.length > 0 && requesterId.isEmpty()) {
            receiver.sendMessage(Component.text("Player " + args[0] + " is not online.", NamedTextColor.RED));
            return;
        }
        TeleportDeclineResult result = requests.decline(receiver.getUniqueId(), requesterId);
        if (result.status() == TeleportDeclineResult.Status.NOT_FOUND) {
            receiver.sendMessage(Component.text("No pending teleport request found.", NamedTextColor.RED));
            return;
        }
        Player requester = players.find(result.request().orElseThrow().requesterId()).orElse(null);
        receiver.sendMessage(Component.text("Teleport request declined.", NamedTextColor.YELLOW));
        if (requester != null) {
            requester.sendMessage(Component.text(receiver.getName() + " declined your teleport request.", NamedTextColor.RED));
        }
    }

    private void cancel(Player player) {
        if (!player.hasPermission("teleportlocations.tpcancel")) {
            player.sendMessage(Component.text("You do not have permission to cancel teleport requests.", NamedTextColor.RED));
            return;
        }

        boolean cancelledWarmup = cancelWarmup(player);
        List<TeleportRequest> cancelledRequests = requests.cancelOutgoing(player.getUniqueId());
        for (TeleportRequest request : cancelledRequests) {
            players.find(request.targetId()).ifPresent(target -> target.sendMessage(Component.text(
                    player.getName() + " cancelled their teleport request.",
                    NamedTextColor.YELLOW
            )));
        }
        if (!cancelledWarmup && cancelledRequests.isEmpty()) {
            managedTeleports.denied(player);
            player.sendMessage(Component.text("No pending teleport request to cancel.", NamedTextColor.RED));
            return;
        }
        player.sendMessage(Component.text("Teleport request cancelled.", NamedTextColor.YELLOW));
    }

    private void toggle(Player player) {
        if (!player.hasPermission("teleportlocations.tptoggle")) {
            player.sendMessage(Component.text("You do not have permission to toggle teleport requests.", NamedTextColor.RED));
            return;
        }
        boolean enabled = requests.toggleIncomingRequests(player.getUniqueId());
        player.sendMessage(Component.text(
                enabled ? "Teleport requests are now enabled." : "Teleport requests are now disabled.",
                enabled ? NamedTextColor.GREEN : NamedTextColor.YELLOW
        ));
    }

    private Optional<UUID> requesterId(String[] args) {
        if (args.length == 0) {
            return Optional.empty();
        }
        return players.find(args[0]).map(Player::getUniqueId);
    }

    private void executeAcceptedRequest(Player receiver, TeleportRequest request) {
        Optional<Player> requester = players.find(request.requesterId());
        Optional<Player> target = players.find(request.targetId());
        if (requester.isEmpty() || target.isEmpty()) {
            managedTeleports.denied(receiver);
            receiver.sendMessage(Component.text("That teleport request can no longer be completed.", NamedTextColor.RED));
            return;
        }
        Player moving = request.type() == TeleportRequestType.TPA ? requester.orElseThrow() : receiver;
        Player destinationOwner = request.type() == TeleportRequestType.TPA ? receiver : requester.orElseThrow();
        if (!canEnter(moving, destinationOwner)) {
            managedTeleports.denied(moving);
            moving.sendMessage(Component.text("You cannot teleport there because you do not have claim entry access.", NamedTextColor.RED));
            receiver.sendMessage(Component.text("Teleport request could not be completed because the destination is not accessible.", NamedTextColor.RED));
            return;
        }
        receiver.sendMessage(Component.text("Teleport request accepted.", NamedTextColor.GREEN));
        requester.orElseThrow().sendMessage(Component.text(receiver.getName() + " accepted your teleport request.", NamedTextColor.GREEN));
        PendingWarmup pending = new PendingWarmup(moving.getUniqueId(), destinationOwner.getUniqueId(), moving);
        warmupsByMovingPlayer.put(moving.getUniqueId(), pending);
        warmupsByDestinationPlayer.put(destinationOwner.getUniqueId(), pending);
        warmups.begin(moving, () -> {
            clearWarmup(pending);
            teleport(moving, destinationOwner);
        });
    }

    private void teleport(Player moving, Player destinationOwner) {
        if (!moving.isOnline() || !destinationOwner.isOnline()) {
            if (moving.isOnline()) {
                managedTeleports.denied(moving);
            }
            return;
        }
        if (!canEnter(moving, destinationOwner)) {
            managedTeleports.denied(moving);
            moving.sendMessage(Component.text("Teleport cancelled: destination is no longer accessible.", NamedTextColor.RED));
            return;
        }
        Location destination = destinationOwner.getLocation();
        managedTeleports.teleport(moving, destination);
        moving.sendMessage(Component.text("Teleporting to " + destinationOwner.getName() + ".", NamedTextColor.GREEN));
    }

    private boolean canEnter(Player moving, Player destinationOwner) {
        TeleportAccessResult result = access.canEnter(
                moving.getUniqueId(),
                BukkitLocations.save(destinationOwner.getLocation()),
                moving.hasPermission("teleportlocations.admin.bypass.claims") && bypass.claims(moving.getUniqueId())
        );
        return result.allowed();
    }

    private static void sendRequestMessage(Player requester, Player target, TeleportRequestType type) {
        String message = type == TeleportRequestType.TPA
                ? requester.getName() + " wants to teleport to you."
                : requester.getName() + " wants you to teleport to them.";
        target.sendMessage(Component.text(message, NamedTextColor.YELLOW)
                .append(Component.text(" "))
                .append(action("[Accept]", NamedTextColor.GREEN, "/tpaccept " + requester.getName(), "Accept teleport request"))
                .append(Component.text(" "))
                .append(action("[Decline]", NamedTextColor.RED, "/tpdecline " + requester.getName(), "Decline teleport request")));
    }

    private static Component action(String label, NamedTextColor color, String command, String hover) {
        return Component.text(label, color)
                .clickEvent(ClickEvent.runCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text(hover, NamedTextColor.GRAY)));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        PendingWarmup destinationWarmup = warmupsByDestinationPlayer.remove(playerId);
        if (destinationWarmup != null) {
            warmupsByMovingPlayer.remove(destinationWarmup.movingPlayerId());
            managedTeleports.denied(destinationWarmup.movingPlayer());
            warmups.cancel(
                    destinationWarmup.movingPlayerId(),
                    "Teleport cancelled: " + event.getPlayer().getName() + " logged off.",
                    destinationWarmup.movingPlayer()
            );
        }
        PendingWarmup movingWarmup = warmupsByMovingPlayer.remove(playerId);
        if (movingWarmup != null) {
            warmupsByDestinationPlayer.remove(movingWarmup.destinationPlayerId());
        }
        requests.clear(playerId);
    }

    private void clearWarmup(PendingWarmup pending) {
        warmupsByMovingPlayer.remove(pending.movingPlayerId());
        warmupsByDestinationPlayer.remove(pending.destinationPlayerId());
    }

    private boolean cancelWarmup(Player player) {
        PendingWarmup pending = warmupsByMovingPlayer.remove(player.getUniqueId());
        if (pending == null) {
            return false;
        }
        warmupsByDestinationPlayer.remove(pending.destinationPlayerId());
        managedTeleports.denied(player);
        warmups.cancel(player.getUniqueId(), "Teleport cancelled.", player);
        return true;
    }

    private record PendingWarmup(UUID movingPlayerId, UUID destinationPlayerId, Player movingPlayer) {
    }
}

package com.nick.teleportlocations.dialog;

import com.nick.teleportlocations.bukkit.BukkitLocations;
import com.nick.teleportlocations.cost.ChargeResult;
import com.nick.teleportlocations.location.TeleportLocation;
import com.nick.teleportlocations.teleport.TeleportChargeMessages;
import com.nick.teleportlocations.teleport.TeleportChargeService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class DialogActionExecutor implements DialogActionHandler {
    private final DialogActionRouter router;
    private final PaperDialogPresenter presenter;
    private final TeleportChargeService charges;

    public DialogActionExecutor(DialogActionRouter router, PaperDialogPresenter presenter, TeleportChargeService charges) {
        this.router = router;
        this.presenter = presenter;
        this.charges = charges;
    }

    @Override
    public void handle(Player player, String actionKey, DialogInputValues inputValues) {
        DialogActionRouteResult result = router.route(player.getUniqueId(), actionKey, inputValues);
        switch (result.status()) {
            case TELEPORT -> teleport(player, result.location().orElseThrow());
            case SHOW_MENU -> presenter.show(player, result.menu().orElseThrow());
            case MESSAGE -> player.sendMessage(Component.text(result.message(), NamedTextColor.GREEN));
            case NOT_FOUND, ACCESS_DENIED, UNKNOWN_ACTION -> player.sendMessage(Component.text(result.message(), NamedTextColor.RED));
        }
    }

    private void teleport(Player player, TeleportLocation location) {
        Location destination = BukkitLocations.load(location.position());
        if (destination == null) {
            player.sendMessage(Component.text("That location world is not loaded.", NamedTextColor.RED));
            return;
        }
        ChargeResult charge = charges.chargeIfNeeded(
                player.getUniqueId(),
                player.hasPermission("teleportlocations.admin.bypass.cost"),
                location
        );
        if (!charge.success()) {
            player.sendMessage(Component.text(TeleportChargeMessages.failure(charge.reason()), NamedTextColor.RED));
            return;
        }
        player.teleportAsync(destination);
        player.sendMessage(Component.text("Teleported to " + location.name() + ".", NamedTextColor.GREEN));
    }
}

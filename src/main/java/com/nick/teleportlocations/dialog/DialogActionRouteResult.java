package com.nick.teleportlocations.dialog;

import com.nick.teleportlocations.location.TeleportLocation;
import java.util.Optional;

public record DialogActionRouteResult(
        Status status,
        Optional<TeleportLocation> location,
        Optional<DialogMenuModel> menu,
        String message
) {
    public enum Status {
        TELEPORT,
        SHOW_MENU,
        NOT_FOUND,
        ACCESS_DENIED,
        UNKNOWN_ACTION
    }

    public static DialogActionRouteResult teleport(TeleportLocation location) {
        return new DialogActionRouteResult(Status.TELEPORT, Optional.of(location), Optional.empty(), "");
    }

    public static DialogActionRouteResult showMenu(DialogMenuModel menu) {
        return new DialogActionRouteResult(Status.SHOW_MENU, Optional.empty(), Optional.of(menu), "");
    }

    public static DialogActionRouteResult notFound() {
        return new DialogActionRouteResult(Status.NOT_FOUND, Optional.empty(), Optional.empty(), "Location not found.");
    }

    public static DialogActionRouteResult accessDenied() {
        return new DialogActionRouteResult(Status.ACCESS_DENIED, Optional.empty(), Optional.empty(), "You cannot edit that location.");
    }

    public static DialogActionRouteResult unknownAction() {
        return new DialogActionRouteResult(Status.UNKNOWN_ACTION, Optional.empty(), Optional.empty(), "That dialog action is no longer available.");
    }
}

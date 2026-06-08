package com.nick.teleportlocations.dialog;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface DialogActionHandler {
    void handle(Player player, String actionKey, DialogInputValues inputValues);

    default void handle(Player player, String actionKey) {
        handle(player, actionKey, DialogInputValues.empty());
    }

    static DialogActionHandler noop() {
        return (player, actionKey, inputValues) -> {
        };
    }
}

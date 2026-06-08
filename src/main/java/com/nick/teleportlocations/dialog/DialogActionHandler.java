package com.nick.teleportlocations.dialog;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface DialogActionHandler {
    void handle(Player player, String actionKey);

    static DialogActionHandler noop() {
        return (player, actionKey) -> {
        };
    }
}

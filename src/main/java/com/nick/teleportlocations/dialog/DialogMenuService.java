package com.nick.teleportlocations.dialog;

import com.nick.teleportlocations.location.TeleportLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DialogMenuService {
    public DialogMenuModel homesMenu(UUID viewerId, List<TeleportLocation> homes) {
        List<String> lines = new ArrayList<>();
        List<DialogActionModel> actions = new ArrayList<>();
        for (TeleportLocation home : homes) {
            String marker = home.mainHome() ? "Main" : "Home";
            lines.add(marker + ": " + home.name());
            actions.add(new DialogActionModel("teleport:home:" + home.normalizedName(), "Teleport"));
            if (home.owner().playerIdOptional().filter(viewerId::equals).isPresent()) {
                actions.add(new DialogActionModel("edit:home:" + home.normalizedName(), "Edit"));
            }
        }
        return new DialogMenuModel("Homes", List.copyOf(lines), List.copyOf(actions));
    }

    public DialogMenuModel playerWarpsMenu(UUID viewerId, List<TeleportLocation> warps) {
        List<String> lines = new ArrayList<>();
        List<DialogActionModel> actions = new ArrayList<>();
        for (TeleportLocation warp : warps) {
            lines.add("Warp: " + warp.name());
            actions.add(new DialogActionModel("teleport:player_warp:" + warp.normalizedName(), "Teleport"));
            if (warp.owner().playerIdOptional().filter(viewerId::equals).isPresent()) {
                actions.add(new DialogActionModel("edit:player_warp:" + warp.normalizedName(), "Edit"));
            }
        }
        return new DialogMenuModel("Player Warps", List.copyOf(lines), List.copyOf(actions));
    }

    public DialogMenuModel warpsMenu(UUID viewerId, List<TeleportLocation> serverWarps, List<TeleportLocation> playerWarps) {
        List<String> lines = new ArrayList<>();
        List<DialogActionModel> actions = new ArrayList<>();
        for (TeleportLocation warp : serverWarps) {
            lines.add("Server: " + warp.name());
            actions.add(new DialogActionModel("teleport:server_warp:" + warp.normalizedName(), "Teleport"));
        }
        for (TeleportLocation warp : playerWarps) {
            lines.add("Player: " + warp.name());
            actions.add(new DialogActionModel("teleport:player_warp:" + warp.normalizedName(), "Teleport"));
            if (warp.owner().playerIdOptional().filter(viewerId::equals).isPresent()) {
                actions.add(new DialogActionModel("edit:player_warp:" + warp.normalizedName(), "Edit"));
            }
        }
        return new DialogMenuModel("Warps", List.copyOf(lines), List.copyOf(actions));
    }

    public DialogMenuModel shopWarpsMenu(UUID viewerId, List<TeleportLocation> shops) {
        List<String> lines = new ArrayList<>();
        List<DialogActionModel> actions = new ArrayList<>();
        for (TeleportLocation shop : shops) {
            lines.add("Shop: " + shop.name());
            actions.add(new DialogActionModel("teleport:shop:" + shop.normalizedName(), "Teleport"));
            if (shop.owner().playerIdOptional().filter(viewerId::equals).isPresent()) {
                actions.add(new DialogActionModel("edit:shop:" + shop.normalizedName(), "Edit"));
            }
        }
        return new DialogMenuModel("Shop Warps", List.copyOf(lines), List.copyOf(actions));
    }

    public DialogMenuModel editMenu(TeleportLocation location) {
        List<String> lines = new ArrayList<>();
        List<DialogActionModel> actions = new ArrayList<>();
        lines.add("Name: " + location.name());
        lines.add("Access: " + title(location.accessMode().name()));
        lines.add("Visibility: " + title(location.visibilityMode().name()));
        lines.add("Cost: " + title(location.cost().type().name()));
        if ("home".equals(location.category())) {
            actions.add(new DialogActionModel("set-main:home:" + location.normalizedName(), "Set Main"));
        }
        actions.add(new DialogActionModel("delete:" + location.category() + ":" + location.normalizedName(), "Delete"));
        return new DialogMenuModel("Edit " + title(location.category()), List.copyOf(lines), List.copyOf(actions));
    }

    private String title(String value) {
        String normalized = value.toLowerCase().replace('_', ' ');
        String[] words = normalized.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return builder.toString();
    }
}

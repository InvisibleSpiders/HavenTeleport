package com.nick.teleportlocations.dialog;

import com.nick.teleportlocations.elevator.ElevatorBlock;
import com.nick.teleportlocations.elevator.ElevatorParticle;
import com.nick.teleportlocations.location.TeleportLocation;
import com.nick.teleportlocations.teleportblock.TeleportBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Predicate;

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
        return warpsMenu(viewerId, serverWarps, playerWarps, location -> true, false);
    }

    public DialogMenuModel warpsMenu(
            UUID viewerId,
            List<TeleportLocation> serverWarps,
            List<TeleportLocation> playerWarps,
            Predicate<TeleportLocation> canEnter,
            boolean hideInaccessible
    ) {
        List<String> lines = new ArrayList<>();
        List<DialogActionModel> actions = new ArrayList<>();
        for (TeleportLocation warp : serverWarps) {
            lines.add("Server: " + warp.name());
            actions.add(new DialogActionModel("teleport:server_warp:" + warp.normalizedName(), "Teleport"));
        }
        for (TeleportLocation warp : playerWarps) {
            boolean accessible = canEnter.test(warp);
            boolean owner = warp.owner().playerIdOptional().filter(viewerId::equals).isPresent();
            if (!accessible && hideInaccessible && !owner) {
                continue;
            }
            lines.add("Player: " + warp.name() + (accessible ? "" : " (No claim access)"));
            if (accessible) {
                actions.add(new DialogActionModel("teleport:player_warp:" + warp.normalizedName(), "Teleport"));
            }
            if (owner) {
                actions.add(new DialogActionModel("edit:player_warp:" + warp.normalizedName(), "Edit"));
            }
        }
        return new DialogMenuModel("Warps", List.copyOf(lines), List.copyOf(actions));
    }

    public DialogMenuModel shopWarpsMenu(UUID viewerId, List<TeleportLocation> shops) {
        return shopWarpsMenu(viewerId, shops, location -> true, false);
    }

    public DialogMenuModel shopWarpsMenu(
            UUID viewerId,
            List<TeleportLocation> shops,
            Predicate<TeleportLocation> canEnter,
            boolean hideInaccessible
    ) {
        List<String> lines = new ArrayList<>();
        List<DialogActionModel> actions = new ArrayList<>();
        for (TeleportLocation shop : shops) {
            boolean accessible = canEnter.test(shop);
            boolean owner = shop.owner().playerIdOptional().filter(viewerId::equals).isPresent();
            if (!accessible && hideInaccessible && !owner) {
                continue;
            }
            lines.add("Shop: " + shop.name() + (accessible ? "" : " (No claim access)"));
            if (accessible) {
                actions.add(new DialogActionModel("teleport:shop:" + shop.normalizedName(), "Teleport"));
            }
            if (owner) {
                actions.add(new DialogActionModel("edit:shop:" + shop.normalizedName(), "Edit"));
            }
        }
        return new DialogMenuModel("Shop Warps", List.copyOf(lines), List.copyOf(actions));
    }

    public DialogMenuModel editMenu(TeleportLocation location) {
        if ("player_warp".equals(location.category())) {
            List<String> lines = List.of(
                    "Name: " + location.name(),
                    "Access: " + title(location.accessMode().name()),
                    "Visibility: " + title(location.visibilityMode().name()),
                    "Cost: " + title(location.cost().type().name())
            );
            List<DialogActionModel> actions = List.of(
                    new DialogActionModel("show-access-menu:player_warp:" + location.normalizedName(), "Access"),
                    new DialogActionModel("show-visibility-menu:player_warp:" + location.normalizedName(), "Visibility"),
                    new DialogActionModel("show-cost-menu:player_warp:" + location.normalizedName(), "Cost"),
                    new DialogActionModel("show-rename-menu:player_warp:" + location.normalizedName(), "Rename"),
                    new DialogActionModel("relocate:player_warp:" + location.normalizedName(), "Relocate"),
                    new DialogActionModel("show-delete-confirm:player_warp:" + location.normalizedName(), "Delete")
            );
            return new DialogMenuModel("Edit Player Warp", lines, actions);
        }
        if ("shop".equals(location.category())) {
            List<String> lines = List.of(
                    "Name: " + location.name(),
                    "Location: " + locationSummary(location)
            );
            List<DialogActionModel> actions = List.of(
                    new DialogActionModel("show-rename-menu:shop:" + location.normalizedName(), "Rename"),
                    new DialogActionModel("relocate:shop:" + location.normalizedName(), "Relocate"),
                    new DialogActionModel("show-delete-confirm:shop:" + location.normalizedName(), "Delete")
            );
            return new DialogMenuModel("Edit Shop", lines, actions);
        }
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

    public DialogMenuModel accessMenu(TeleportLocation location) {
        List<String> lines = List.of(
                "Name: " + location.name(),
                "Access: " + title(location.accessMode().name())
        );
        List<DialogActionModel> actions = List.of(
                new DialogActionModel("set-access:player_warp:" + location.normalizedName() + ":public", "Public"),
                new DialogActionModel("set-access:player_warp:" + location.normalizedName() + ":trusted", "Trusted"),
                new DialogActionModel("set-access:player_warp:" + location.normalizedName() + ":private", "Private")
        );
        return new DialogMenuModel("Access", lines, actions);
    }

    public DialogMenuModel visibilityMenu(TeleportLocation location) {
        List<String> lines = List.of(
                "Name: " + location.name(),
                "Visibility: " + title(location.visibilityMode().name())
        );
        List<DialogActionModel> actions = List.of(
                new DialogActionModel("set-visibility:player_warp:" + location.normalizedName() + ":listed", "Listed"),
                new DialogActionModel("set-visibility:player_warp:" + location.normalizedName() + ":unlisted", "Unlisted"),
                new DialogActionModel("set-visibility:player_warp:" + location.normalizedName() + ":hidden", "Hidden")
        );
        return new DialogMenuModel("Visibility", lines, actions);
    }

    public DialogMenuModel costMenu(TeleportLocation location) {
        List<String> lines = List.of(
                "Name: " + location.name(),
                "Cost: " + title(location.cost().type().name())
        );
        return new DialogMenuModel("Cost", lines, costActions(location));
    }

    public DialogMenuModel renameMenu(TeleportLocation location) {
        List<String> lines = List.of("Name: " + location.name());
        List<DialogActionModel> actions = List.of(new DialogActionModel(
                "rename-input:" + location.category() + ":" + location.normalizedName(),
                "Rename"
        ));
        List<DialogInputModel> inputs = List.of(DialogInputModel.text("name", "Name", location.name(), 32));
        return new DialogMenuModel("Rename " + title(location.category()), lines, actions, inputs);
    }

    public DialogMenuModel deleteConfirmMenu(TeleportLocation location) {
        List<String> lines = List.of("Name: " + location.name());
        List<DialogActionModel> actions = List.of(
                new DialogActionModel("confirm-delete:" + location.category() + ":" + location.normalizedName(), "Confirm Delete"),
                new DialogActionModel("cancel-delete:" + location.category() + ":" + location.normalizedName(), "Cancel")
        );
        return new DialogMenuModel("Delete " + title(location.category()), lines, actions);
    }

    public DialogMenuModel customCostMenu(TeleportLocation location, String costType) {
        String title = "Custom " + title(costType) + " Cost";
        List<String> lines = List.of("Name: " + location.name());
        List<DialogActionModel> actions = List.of(new DialogActionModel(
                "set-cost-input:" + location.category() + ":" + location.normalizedName() + ":" + costType,
                "Apply"
        ));
        List<DialogInputModel> inputs = List.of(costInput(costType));
        return new DialogMenuModel(title, lines, actions, inputs);
    }

    public DialogMenuModel elevatorSettingsMenu(ElevatorBlock block, boolean canEdit, Predicate<ElevatorParticle> canSelectParticle) {
        List<String> lines = new ArrayList<>();
        List<DialogActionModel> actions = new ArrayList<>();
        lines.add("Particle: " + title(block.particle().name()));
        if (canEdit) {
            for (ElevatorParticle particle : ElevatorParticle.values()) {
                if (canSelectParticle.test(particle)) {
                    actions.add(new DialogActionModel(
                            "set-elevator-particle:" + block.id() + ":" + particle.name().toLowerCase(),
                            title(particle.name())
                    ));
                }
            }
        }
        return new DialogMenuModel("Elevator Settings", List.copyOf(lines), List.copyOf(actions));
    }

    public DialogMenuModel teleportBlockSettingsMenu(
            TeleportBlock block,
            List<TeleportLocation> playerTargets,
            List<TeleportLocation> adminTargets,
            boolean canEdit
    ) {
        List<String> lines = new ArrayList<>();
        List<DialogActionModel> actions = new ArrayList<>();
        lines.add(block.linkedBlockId().isPresent() ? "Linked: Teleport Block" : "Linked: None");
        lines.add(block.targetLocationId().isPresent() ? "Target: Saved Location" : "Target: None");
        if (canEdit) {
            for (TeleportLocation target : playerTargets) {
                actions.add(new DialogActionModel(
                        "set-teleport-block-target:" + block.id() + ":" + target.id(),
                        title(target.category()) + ": " + target.name()
                ));
            }
            for (TeleportLocation target : adminTargets) {
                actions.add(new DialogActionModel(
                        "set-teleport-block-target:" + block.id() + ":" + target.id(),
                        "Admin " + title(target.category()) + ": " + target.name()
                ));
            }
        }
        return new DialogMenuModel("Teleport Block", List.copyOf(lines), List.copyOf(actions));
    }

    public DialogMenuModel adminMenu(boolean claimBypassEnabled) {
        List<String> lines = List.of("Claim Bypass: " + (claimBypassEnabled ? "Enabled" : "Disabled"));
        List<DialogActionModel> actions = List.of(
                new DialogActionModel("admin-toggle-claims-bypass", "Toggle Claim Bypass"),
                new DialogActionModel("admin-show-server-warps", "Server Warps")
        );
        return new DialogMenuModel("HavenTeleport Admin", lines, actions);
    }

    public DialogMenuModel adminServerWarpsMenu(List<TeleportLocation> serverWarps) {
        List<String> lines = new ArrayList<>();
        List<DialogActionModel> actions = new ArrayList<>();
        for (TeleportLocation warp : serverWarps) {
            lines.add("Warp: " + warp.name());
            actions.add(new DialogActionModel("teleport:server_warp:" + warp.normalizedName(), "Teleport"));
        }
        return new DialogMenuModel("Server Warps", List.copyOf(lines), List.copyOf(actions));
    }

    private DialogInputModel costInput(String costType) {
        return switch (costType) {
            case "money" -> new DialogInputModel("amount", "Amount", 0.0f, 100000.0f, 1.0f, 10.0f, "$%.0f");
            case "xp-levels" -> new DialogInputModel("amount", "Levels", 0.0f, 1000.0f, 1.0f, 5.0f, "%.0f levels");
            case "xp-points" -> new DialogInputModel("amount", "XP Points", 0.0f, 100000.0f, 1.0f, 100.0f, "%.0f XP");
            default -> new DialogInputModel("amount", "Amount", 0.0f, 100000.0f, 1.0f, 0.0f, "%.0f");
        };
    }

    private List<DialogActionModel> costActions(TeleportLocation location) {
        String normalizedName = location.normalizedName();
        return List.of(
                new DialogActionModel("set-cost:player_warp:" + normalizedName + ":free:0", "Free"),
                new DialogActionModel("set-cost:player_warp:" + normalizedName + ":money:10", "$10"),
                new DialogActionModel("set-cost:player_warp:" + normalizedName + ":money:50", "$50"),
                new DialogActionModel("set-cost:player_warp:" + normalizedName + ":money:100", "$100"),
                new DialogActionModel("set-cost:player_warp:" + normalizedName + ":xp-levels:5", "5 Levels"),
                new DialogActionModel("set-cost:player_warp:" + normalizedName + ":xp-levels:10", "10 Levels"),
                new DialogActionModel("set-cost:player_warp:" + normalizedName + ":xp-points:100", "100 XP"),
                new DialogActionModel("set-cost:player_warp:" + normalizedName + ":xp-points:500", "500 XP"),
                new DialogActionModel("show-cost-editor:player_warp:" + normalizedName + ":money", "Custom Money"),
                new DialogActionModel("show-cost-editor:player_warp:" + normalizedName + ":xp-levels", "Custom Levels"),
                new DialogActionModel("show-cost-editor:player_warp:" + normalizedName + ":xp-points", "Custom XP")
        );
    }

    private String locationSummary(TeleportLocation location) {
        return String.format(
                Locale.ROOT,
                "%s (%.0f, %.0f, %.0f)",
                location.position().worldName(),
                location.position().x(),
                location.position().y(),
                location.position().z()
        );
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

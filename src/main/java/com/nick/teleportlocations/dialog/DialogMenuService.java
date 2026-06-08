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
            actions.add(new DialogActionModel("teleport:" + home.normalizedName(), "Teleport"));
            if (home.owner().playerIdOptional().filter(viewerId::equals).isPresent()) {
                actions.add(new DialogActionModel("edit:" + home.normalizedName(), "Edit"));
            }
        }
        return new DialogMenuModel("Homes", List.copyOf(lines), List.copyOf(actions));
    }
}

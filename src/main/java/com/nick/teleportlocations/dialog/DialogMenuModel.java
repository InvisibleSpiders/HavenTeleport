package com.nick.teleportlocations.dialog;

import java.util.List;

public record DialogMenuModel(
        String title,
        List<String> lines,
        List<DialogActionModel> actions,
        List<DialogInputModel> inputs
) {
    public DialogMenuModel(String title, List<String> lines, List<DialogActionModel> actions) {
        this(title, lines, actions, List.of());
    }
}

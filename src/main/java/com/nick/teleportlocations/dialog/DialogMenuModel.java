package com.nick.teleportlocations.dialog;

import java.util.List;

public record DialogMenuModel(String title, List<String> lines, List<DialogActionModel> actions) {
}

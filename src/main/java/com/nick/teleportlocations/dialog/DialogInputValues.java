package com.nick.teleportlocations.dialog;

public interface DialogInputValues {
    Float getFloat(String key);

    default String getText(String key) {
        return null;
    }

    static DialogInputValues empty() {
        return key -> null;
    }
}

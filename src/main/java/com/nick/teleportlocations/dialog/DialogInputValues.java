package com.nick.teleportlocations.dialog;

public interface DialogInputValues {
    Float getFloat(String key);

    static DialogInputValues empty() {
        return key -> null;
    }
}

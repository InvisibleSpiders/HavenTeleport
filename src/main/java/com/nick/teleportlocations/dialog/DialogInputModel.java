package com.nick.teleportlocations.dialog;

public record DialogInputModel(
        Kind kind,
        String key,
        String label,
        float min,
        float max,
        float step,
        float initial,
        String labelFormat,
        String textInitial,
        int maxLength
) {
    public enum Kind {
        NUMBER,
        TEXT
    }

    public DialogInputModel(String key, String label, float min, float max, float step, float initial, String labelFormat) {
        this(Kind.NUMBER, key, label, min, max, step, initial, labelFormat, null, 0);
    }

    public static DialogInputModel text(String key, String label, String initial, int maxLength) {
        if (maxLength <= 0) {
            throw new IllegalArgumentException("maxLength must be positive");
        }
        return new DialogInputModel(
                Kind.TEXT,
                key,
                label,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                null,
                initial == null ? "" : initial,
                maxLength
        );
    }
}

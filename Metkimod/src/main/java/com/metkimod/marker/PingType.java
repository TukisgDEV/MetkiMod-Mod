package com.metkimod.marker;

public enum PingType {
    DEFAULT("!", "Метка", 0xFFFFFF),
    ENEMY("!!", "Враг здесь!", 0xFF4444),
    TRAP("/!\\", "Ловушка!", 0xFF8844),
    GO_HERE(">>", "Сюда!", 0x44FF44),
    HELP("SOS", "Помогите!", 0xFF4466),
    DANGER("X", "Опасность!", 0xFF2222),
    FULL_STACK("!!!", "Фуллстак!", 0xFF6600),
    CHASING("~>", "Меня чейзят!", 0xDD44FF);

    private final String icon;
    private final String message;
    private final int defaultColor;

    PingType(String icon, String message, int defaultColor) {
        this.icon = icon;
        this.message = message;
        this.defaultColor = defaultColor;
    }

    public String getIcon() {
        return icon;
    }

    public String getMessage() {
        return message;
    }

    public int getDefaultColor() {
        return defaultColor;
    }

        public static PingType fromOrdinal(int ordinal) {
        PingType[] values = values();
        if (ordinal < 0 || ordinal >= values.length)
            return DEFAULT;
        return values[ordinal];
    }
}

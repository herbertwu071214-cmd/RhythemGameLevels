package com.herb.rhythm.rhythemgamelevels;

enum GameMode {
    PRACTICE("Practice", Integer.MAX_VALUE, "unlimited misses"),
    NORMAL("Normal", 10, "10 misses max");

    private final String label;
    private final int missLimit;
    private final String description;

    GameMode(String label, int missLimit, String description) {
        this.label = label;
        this.missLimit = missLimit;
        this.description = description;
    }

    String label() {
        return label;
    }

    int missLimit() {
        return missLimit;
    }

    String description() {
        return description;
    }
}

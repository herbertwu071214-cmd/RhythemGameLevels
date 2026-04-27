package com.herb.rhythm.rhythemgamelevels;

import javafx.scene.input.KeyCode;

final class Note {
    static final double HEAD_HEIGHT = 26;

    private double y;
    private final KeyCode key;
    private final boolean hold;
    private final double totalHeight;
    private boolean activeHold;

    Note(double y, KeyCode key, boolean hold, double totalHeight) {
        this.y = y;
        this.key = key;
        this.hold = hold;
        this.totalHeight = totalHeight;
    }

    double y() {
        return y;
    }

    void move(double amount) {
        y += amount;
    }

    KeyCode key() {
        return key;
    }

    boolean hold() {
        return hold;
    }

    double totalHeight() {
        return totalHeight;
    }

    double headCenterY() {
        return y + HEAD_HEIGHT / 2.0;
    }

    double tailY() {
        return y + totalHeight;
    }

    boolean activeHold() {
        return activeHold;
    }

    void setActiveHold(boolean activeHold) {
        this.activeHold = activeHold;
    }
}

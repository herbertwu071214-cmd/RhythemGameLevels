package com.herb.rhythm.rhythemgamelevels;

import javafx.scene.paint.Color;

enum Judgement {
    PERFECT("PERFECT", Color.web("#f8f9fa"), 36, 920),
    EXCELLENT("EXCELLENT", Color.web("#9bf6ff"), 30, 840),
    GREAT("GREAT", Color.web("#72efdd"), 24, 760),
    GOOD("GOOD", Color.web("#ffd166"), 16, 660),
    HOLD("HOLD", Color.web("#b8f2e6"), 18, 540),
    MISS("MISS", Color.web("#ff6b6b"), 0, 260),
    DROP("MISS", Color.web("#ef476f"), 0, 220),
    EARLY("MISS", Color.web("#f28482"), 0, 200),
    START("START", Color.web("#8ecae6"), 0, 600);

    private final String label;
    private final Color color;
    private final int points;
    private final int toneHz;

    Judgement(String label, Color color, int points, int toneHz) {
        this.label = label;
        this.color = color;
        this.points = points;
        this.toneHz = toneHz;
    }

    String label() {
        return label;
    }

    Color color() {
        return color;
    }

    int points() {
        return points;
    }

    int toneHz() {
        return toneHz;
    }
}

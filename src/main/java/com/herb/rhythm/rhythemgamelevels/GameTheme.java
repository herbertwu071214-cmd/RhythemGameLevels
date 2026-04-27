package com.herb.rhythm.rhythemgamelevels;

import javafx.scene.paint.Color;

enum GameTheme {
    NEON(
            "Neon Run",
            Color.web("#08111f"),
            Color.web("#101c33"),
            Color.web("#05070d"),
            Color.web("#8ecae6"),
            Color.web("#ffd166"),
            Color.web("#ff6b6b"),
            Color.color(0.04, 0.06, 0.11, 0.84),
            new int[]{110, 165, 220, 262},
            new int[]{330, 392, 440, 494, 523, 587, 659, 698},
            new Color[]{
                    Color.web("#ff6b6b"),
                    Color.web("#ffd166"),
                    Color.web("#06d6a0"),
                    Color.web("#4cc9f0"),
                    Color.web("#72efdd"),
                    Color.web("#a78bfa"),
                    Color.web("#f72585"),
                    Color.web("#fb8500")
            }
    ),
    SUNSET(
            "Sunset Tape",
            Color.web("#1b1028"),
            Color.web("#3d1f4b"),
            Color.web("#120914"),
            Color.web("#ffcad4"),
            Color.web("#f6bd60"),
            Color.web("#ff595e"),
            Color.color(0.14, 0.07, 0.12, 0.82),
            new int[]{98, 147, 196, 247},
            new int[]{294, 370, 392, 440, 494, 554, 587, 659},
            new Color[]{
                    Color.web("#f94144"),
                    Color.web("#f3722c"),
                    Color.web("#f8961e"),
                    Color.web("#f9c74f"),
                    Color.web("#90be6d"),
                    Color.web("#43aa8b"),
                    Color.web("#577590"),
                    Color.web("#c77dff")
            }
    ),
    GLACIER(
            "Glacier Pulse",
            Color.web("#031926"),
            Color.web("#0a3d62"),
            Color.web("#02111b"),
            Color.web("#d9ed92"),
            Color.web("#76c893"),
            Color.web("#ef476f"),
            Color.color(0.03, 0.09, 0.14, 0.84),
            new int[]{123, 185, 247, 277},
            new int[]{311, 370, 415, 466, 554, 622, 659, 740},
            new Color[]{
                    Color.web("#caf0f8"),
                    Color.web("#ade8f4"),
                    Color.web("#90e0ef"),
                    Color.web("#48cae4"),
                    Color.web("#00b4d8"),
                    Color.web("#0096c7"),
                    Color.web("#0077b6"),
                    Color.web("#023e8a")
            }
    );

    private final String title;
    private final Color backgroundTop;
    private final Color backgroundMid;
    private final Color backgroundBottom;
    private final Color accent;
    private final Color glow;
    private final Color warning;
    private final Color panel;
    private final int[] bassNotes;
    private final int[] leadNotes;
    private final Color[] laneColors;

    GameTheme(
            String title,
            Color backgroundTop,
            Color backgroundMid,
            Color backgroundBottom,
            Color accent,
            Color glow,
            Color warning,
            Color panel,
            int[] bassNotes,
            int[] leadNotes,
            Color[] laneColors
    ) {
        this.title = title;
        this.backgroundTop = backgroundTop;
        this.backgroundMid = backgroundMid;
        this.backgroundBottom = backgroundBottom;
        this.accent = accent;
        this.glow = glow;
        this.warning = warning;
        this.panel = panel;
        this.bassNotes = bassNotes;
        this.leadNotes = leadNotes;
        this.laneColors = laneColors;
    }

    String title() {
        return title;
    }

    Color backgroundTop() {
        return backgroundTop;
    }

    Color backgroundMid() {
        return backgroundMid;
    }

    Color backgroundBottom() {
        return backgroundBottom;
    }

    Color accent() {
        return accent;
    }

    Color glow() {
        return glow;
    }

    Color warning() {
        return warning;
    }

    Color panel() {
        return panel;
    }

    int bassNote(int step) {
        return bassNotes[Math.floorMod(step, bassNotes.length)];
    }

    int leadNote(int step) {
        return leadNotes[Math.floorMod(step, leadNotes.length)];
    }

    Color laneColor(int laneIndex) {
        return laneColors[laneIndex % laneColors.length];
    }
}

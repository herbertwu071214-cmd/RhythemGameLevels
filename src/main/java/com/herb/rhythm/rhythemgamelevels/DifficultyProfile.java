package com.herb.rhythm.rhythemgamelevels;

import javafx.scene.input.KeyCode;

import java.util.List;

record DifficultyProfile(int level, double noteSpeed, double spawnIntervalSec, double holdChance, List<KeyCode> keys) {
    static DifficultyProfile forLevel(int level) {
        return switch (level) {
            case 1 -> new DifficultyProfile(1, 165, 0.80, 0.10, List.of(KeyCode.J));
            case 2 -> new DifficultyProfile(2, 210, 0.62, 0.14, List.of(KeyCode.F, KeyCode.J));
            case 3 -> new DifficultyProfile(3, 255, 0.46, 0.18, List.of(KeyCode.D, KeyCode.F, KeyCode.J, KeyCode.K));
            case 4 -> new DifficultyProfile(4, 305, 0.36, 0.22, List.of(KeyCode.S, KeyCode.D, KeyCode.F, KeyCode.J, KeyCode.K, KeyCode.L));
            case 5 -> new DifficultyProfile(5, 355, 0.30, 0.28, List.of(KeyCode.A, KeyCode.S, KeyCode.D, KeyCode.F, KeyCode.H, KeyCode.J, KeyCode.K, KeyCode.L));
            default -> forLevel(3);
        };
    }
}

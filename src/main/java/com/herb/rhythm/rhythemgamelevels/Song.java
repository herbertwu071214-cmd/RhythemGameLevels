package com.herb.rhythm.rhythemgamelevels;

import java.util.List;

record Song(
        String title,
        String artist,
        String caption,
        GameTheme theme,
        double bpm,
        double previewBeats,
        String mediaPath,
        double targetDurationSeconds,
        DifficultyProfile profile,
        List<ChartNote> chart,
        int[] bassPattern,
        int[] leadPattern
) {
    double secondsPerBeat() {
        return 60.0 / bpm;
    }

    double durationSeconds() {
        if (targetDurationSeconds > 0) {
            return targetDurationSeconds;
        }
        double lastBeat = previewBeats;
        for (ChartNote note : chart) {
            lastBeat = Math.max(lastBeat, note.beat() + note.holdBeats() + 4);
        }
        return lastBeat * secondsPerBeat();
    }

    int sourceLaneCount() {
        return profile.keys().size();
    }

    boolean hasMediaFile() {
        return mediaPath != null && !mediaPath.isBlank();
    }

    boolean hasVideoBackground() {
        return hasMediaFile() && mediaPath.toLowerCase().endsWith(".mp4");
    }

    int bassAt(int step) {
        return bassPattern[Math.floorMod(step, bassPattern.length)];
    }

    int leadAt(int step) {
        return leadPattern[Math.floorMod(step, leadPattern.length)];
    }
}

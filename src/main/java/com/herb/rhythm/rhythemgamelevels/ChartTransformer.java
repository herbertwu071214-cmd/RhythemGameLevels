package com.herb.rhythm.rhythemgamelevels;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class ChartTransformer {
    private ChartTransformer() {
    }

    static List<ChartNote> forDifficulty(Song song, DifficultyProfile profile) {
        List<ChartNote> chart = expandToDuration(song);
        List<ChartNote> adapted = new ArrayList<>();
        int sourceLaneCount = song.sourceLaneCount();
        int targetLaneCount = profile.keys().size();

        for (int index = 0; index < chart.size(); index++) {
            ChartNote note = chart.get(index);
            if (!shouldKeep(note, index, profile.level())) {
                continue;
            }
            int mappedLane = mapLane(note.laneSeed(), sourceLaneCount, targetLaneCount, index);
            adapted.add(new ChartNote(note.beat(), mappedLane, note.holdBeats()));
        }

        if (profile.level() >= 4) {
            addChordAccents(adapted, profile.level(), targetLaneCount);
        }
        if (profile.level() >= 5) {
            addGapFills(adapted, targetLaneCount);
        }

        adapted.sort(Comparator.comparingDouble(ChartNote::beat));
        return adapted;
    }

    private static List<ChartNote> expandToDuration(Song song) {
        List<ChartNote> baseChart = song.chart();
        if (baseChart.isEmpty()) {
            return baseChart;
        }

        double targetBeats = song.durationSeconds() / song.secondsPerBeat();
        double baseEndBeat = 0;
        for (ChartNote note : baseChart) {
            baseEndBeat = Math.max(baseEndBeat, note.beat() + note.holdBeats());
        }
        baseEndBeat = Math.max(baseEndBeat + 4, song.previewBeats());
        if (baseEndBeat >= targetBeats) {
            return baseChart;
        }

        List<ChartNote> expanded = new ArrayList<>();
        int loopIndex = 0;
        double loopOffset = 0;
        while (loopOffset < targetBeats) {
            for (int i = 0; i < baseChart.size(); i++) {
                ChartNote note = baseChart.get(i);
                double beat = note.beat() + loopOffset;
                if (beat >= targetBeats - 2) {
                    break;
                }
                int shiftedLane = Math.floorMod(note.laneSeed() + loopIndex, song.sourceLaneCount());
                double holdBeats = loopIndex % 2 == 0 ? note.holdBeats() : Math.max(0, note.holdBeats() - 0.25);
                expanded.add(new ChartNote(beat, shiftedLane, holdBeats));
            }
            loopIndex++;
            loopOffset += baseEndBeat;
        }
        return expanded;
    }

    private static boolean shouldKeep(ChartNote note, int index, int level) {
        if (level >= 3) {
            return true;
        }

        double beatFraction = fractionalBeat(note.beat());
        boolean strongBeat = nearlyZero(beatFraction) || nearlyZero(beatFraction - 0.5);
        if (note.isHold() || strongBeat) {
            return true;
        }

        if (level == 2) {
            return index % 2 == 0;
        }
        return index % 4 == 0;
    }

    private static void addChordAccents(List<ChartNote> chart, int level, int laneCount) {
        List<ChartNote> additions = new ArrayList<>();
        for (int index = 0; index < chart.size(); index++) {
            ChartNote note = chart.get(index);
            if (note.isHold()) {
                continue;
            }
            double fraction = fractionalBeat(note.beat());
            boolean accentBeat = nearlyZero(fraction) || nearlyZero(fraction - 0.5);
            if (!accentBeat) {
                continue;
            }
            if (level == 4 && index % 3 != 0) {
                continue;
            }
            int offset = (index % 2 == 0) ? 1 : -1;
            int accentLane = wrapLane(note.laneSeed() + offset, laneCount);
            if (accentLane != note.laneSeed()) {
                additions.add(new ChartNote(note.beat(), accentLane, 0));
            }
        }
        chart.addAll(additions);
    }

    private static void addGapFills(List<ChartNote> chart, int laneCount) {
        if (chart.size() < 2) {
            return;
        }

        List<ChartNote> additions = new ArrayList<>();
        for (int index = 0; index < chart.size() - 1; index++) {
            ChartNote current = chart.get(index);
            ChartNote next = chart.get(index + 1);
            double gap = next.beat() - current.beat();
            if (gap < 0.9 || current.isHold()) {
                continue;
            }

            double fillBeat = current.beat() + gap / 2.0;
            int fillLane = wrapLane(current.laneSeed() + (index % 2 == 0 ? 1 : -1), laneCount);
            additions.add(new ChartNote(fillBeat, fillLane, 0));
        }
        chart.addAll(additions);
    }

    private static int mapLane(int laneSeed, int sourceLaneCount, int targetLaneCount, int index) {
        if (targetLaneCount <= 1) {
            return 0;
        }
        if (sourceLaneCount <= 1) {
            return Math.floorMod(index, targetLaneCount);
        }

        int sourceLane = Math.floorMod(laneSeed, sourceLaneCount);
        int laneStart = (int) Math.floor((double) sourceLane * targetLaneCount / sourceLaneCount);
        int laneEnd = (int) Math.ceil((double) (sourceLane + 1) * targetLaneCount / sourceLaneCount) - 1;
        laneEnd = Math.max(laneStart, Math.min(targetLaneCount - 1, laneEnd));

        int span = laneEnd - laneStart + 1;
        return laneStart + Math.floorMod(index + sourceLane, span);
    }

    private static int wrapLane(int lane, int laneCount) {
        return Math.floorMod(lane, laneCount);
    }

    private static double fractionalBeat(double beat) {
        return beat - Math.floor(beat);
    }

    private static boolean nearlyZero(double value) {
        return Math.abs(value) < 0.0001;
    }
}

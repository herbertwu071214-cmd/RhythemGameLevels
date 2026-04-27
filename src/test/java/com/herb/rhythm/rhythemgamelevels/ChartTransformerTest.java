package com.herb.rhythm.rhythemgamelevels;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartTransformerTest {
    @Test
    void higherDifficultyProducesMoreNotesThanLowerDifficulty() {
        Song song = SongLibrary.builtInSongs().get(2);

        List<ChartNote> easyChart = ChartTransformer.forDifficulty(song, DifficultyProfile.forLevel(1));
        List<ChartNote> hardChart = ChartTransformer.forDifficulty(song, DifficultyProfile.forLevel(5));

        assertTrue(hardChart.size() > easyChart.size(), "level 5 should be denser than level 1");
    }

    @Test
    void sixLaneSongExpandsAcrossRightSideOnEightLaneDifficulty() {
        Song song = SongLibrary.builtInSongs().get(1);
        DifficultyProfile eightLane = DifficultyProfile.forLevel(5);

        Set<KeyCode> usedKeys = ChartTransformer.forDifficulty(song, eightLane).stream()
                .map(note -> eightLane.keys().get(note.laneSeed()))
                .collect(Collectors.toSet());

        assertTrue(usedKeys.contains(KeyCode.H), "expanded chart should use the right-side H lane");
        assertTrue(usedKeys.contains(KeyCode.J), "expanded chart should use the right-side J lane");
        assertTrue(usedKeys.contains(KeyCode.K), "expanded chart should use the right-side K lane");
        assertTrue(usedKeys.contains(KeyCode.L), "expanded chart should use the far-right L lane");
    }
}

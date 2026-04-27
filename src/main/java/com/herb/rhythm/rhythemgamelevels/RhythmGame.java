package com.herb.rhythm.rhythemgamelevels;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class RhythmGame extends Application {
    private static final double CANVAS_WIDTH = 1120;
    private static final double CANVAS_HEIGHT = 720;
    private static final double VIDEO_PANEL_WIDTH = 320;
    private static final double LANE_Y = 610;
    private static final double NOTE_WIDTH = 60;
    private static final double LANE_GAP = 12;
    private static final double HIT_WINDOW = 48;
    private static final double RELEASE_WINDOW = 14;
    private static final double MISS_WINDOW = 42;

    private static final Font TITLE_FONT = Font.font("Verdana", FontWeight.BOLD, 44);
    private static final Font SUBTITLE_FONT = Font.font("Verdana", FontWeight.NORMAL, 18);
    private static final Font PANEL_LABEL_FONT = Font.font("Verdana", FontWeight.BOLD, 14);
    private static final Font PANEL_VALUE_FONT = Font.font("Verdana", FontWeight.BOLD, 28);
    private static final Font PANEL_TEXT_FONT = Font.font("Verdana", FontWeight.NORMAL, 16);
    private static final Font FEEDBACK_FONT = Font.font("Verdana", FontWeight.BOLD, 30);
    private static final Font KEY_FONT = Font.font("Verdana", FontWeight.BOLD, 18);
    private static final Font MENU_FONT = Font.font("Verdana", FontWeight.NORMAL, 17);

    private final SoundEngine soundEngine = new SoundEngine();
    private final List<Song> songs = SongLibrary.builtInSongs();
    private final List<Note> notes = new ArrayList<>();
    private final Set<KeyCode> pressedKeys = new HashSet<>();
    private List<ChartNote> activeChart = List.of();

    private GameState state = GameState.MENU;
    private int selectedSongIndex = 0;
    private int selectedDifficulty = 3;
    private int selectedModeIndex = 1;
    private int menuFieldIndex = 0;

    private Song selectedSong = songs.get(0);
    private DifficultyProfile activeProfile = DifficultyProfile.forLevel(selectedDifficulty);
    private GameMode activeMode = GameMode.NORMAL;

    private int score;
    private int combo;
    private int misses;
    private int nextChartIndex;
    private double songTime;
    private double pulseTime;
    private double feedbackTimer;
    private String feedbackText = "";
    private Color feedbackColor = Color.WHITE;

    @Override
    public void start(Stage stage) {
        MediaView mediaView = new MediaView();
        mediaView.setFitWidth(VIDEO_PANEL_WIDTH);
        mediaView.setFitHeight(CANVAS_HEIGHT);
        soundEngine.attachMediaView(mediaView);

        Canvas canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        StackPane root = new StackPane(mediaView, canvas);
        StackPane.setAlignment(mediaView, Pos.CENTER_RIGHT);
        root.setFocusTraversable(true);
        Scene scene = new Scene(root);

        scene.setOnKeyPressed(event -> {
            KeyCode key = event.getCode();
            pressedKeys.add(key);
            switch (state) {
                case MENU -> handleMenuInput(key);
                case PLAYING -> handlePlayPress(key);
                case GAME_OVER -> handleGameOverInput(key);
            }
        });

        scene.setOnKeyReleased(event -> {
            KeyCode key = event.getCode();
            pressedKeys.remove(key);
            if (state == GameState.PLAYING) {
                handlePlayRelease(key);
            }
        });

        AnimationTimer timer = new AnimationTimer() {
            private long lastFrame;

            @Override
            public void handle(long now) {
                if (lastFrame == 0) {
                    lastFrame = now;
                }
                double delta = (now - lastFrame) / 1_000_000_000.0;
                lastFrame = now;

                pulseTime += delta;
                if (feedbackTimer > 0) {
                    feedbackTimer = Math.max(0, feedbackTimer - delta);
                }
                if (state == GameState.PLAYING) {
                    updateGame(delta);
                }
                draw(gc);
            }
        };
        timer.start();

        stage.setScene(scene);
        stage.setTitle("Rhythm Game");
        stage.show();
        root.requestFocus();
    }

    private void handleMenuInput(KeyCode key) {
        switch (key) {
            case UP -> {
                menuFieldIndex = Math.floorMod(menuFieldIndex - 1, 4);
                soundEngine.playMenuMove();
            }
            case DOWN -> {
                menuFieldIndex = Math.floorMod(menuFieldIndex + 1, 4);
                soundEngine.playMenuMove();
            }
            case LEFT -> changeMenuValue(-1);
            case RIGHT -> changeMenuValue(1);
            case S -> {
                soundEngine.toggle();
                showFeedback(soundEngine.isEnabled() ? "SOUND ON" : "SOUND OFF", currentTheme().accent());
            }
            case ENTER -> {
                if (menuFieldIndex == 3) {
                    startRound();
                } else {
                    changeMenuValue(1);
                }
            }
            default -> {
            }
        }
    }

    private void changeMenuValue(int direction) {
        switch (menuFieldIndex) {
            case 0 -> {
                selectedSongIndex = Math.floorMod(selectedSongIndex + direction, songs.size());
                selectedSong = songs.get(selectedSongIndex);
            }
            case 1 -> selectedDifficulty = clamp(selectedDifficulty + direction, 1, 5);
            case 2 -> {
                int next = Math.floorMod(selectedModeIndex + direction, GameMode.values().length);
                selectedModeIndex = next;
            }
            case 3 -> {
                if (direction > 0) {
                    startRound();
                    return;
                }
            }
            default -> {
            }
        }
        soundEngine.playMenuMove();
    }

    private void handleGameOverInput(KeyCode key) {
        if (key == KeyCode.R) {
            startRound();
        } else if (key == KeyCode.ENTER) {
            state = GameState.MENU;
            notes.clear();
            pressedKeys.clear();
            soundEngine.stopBackingTrack();
            showFeedback("MENU", currentTheme().accent());
            soundEngine.playMenuMove();
        }
    }

    private void handlePlayPress(KeyCode key) {
        if (!activeProfile.keys().contains(key)) {
            return;
        }

        Note note = findHittableNote(key);
        if (note == null) {
            registerMiss(Judgement.MISS);
            return;
        }

        Judgement judgement = judge(note);
        if (judgement == null) {
            registerMiss(Judgement.MISS);
            return;
        }

        score += judgement.points() + combo / 4;
        combo++;
        showFeedback(judgement.label(), judgement.color());
        if (!selectedSong.hasMediaFile()) {
            soundEngine.playJudgement(judgement);
        }

        if (note.hold()) {
            note.setActiveHold(true);
        } else {
            notes.remove(note);
        }
    }

    private void handlePlayRelease(KeyCode key) {
        Note held = findHeldNote(key);
        if (held == null) {
            return;
        }
        if (held.tailY() >= LANE_Y - RELEASE_WINDOW) {
            finishHold(held);
            return;
        }
        notes.remove(held);
        registerMiss(Judgement.EARLY);
    }

    private void startRound() {
        activeProfile = DifficultyProfile.forLevel(selectedDifficulty);
        activeMode = GameMode.values()[selectedModeIndex];
        activeChart = ChartTransformer.forDifficulty(selectedSong, activeProfile);
        state = GameState.PLAYING;
        notes.clear();
        pressedKeys.clear();
        score = 0;
        combo = 0;
        misses = 0;
        nextChartIndex = 0;
        songTime = 0;
        soundEngine.startBackingTrack(selectedSong);
        showFeedback(Judgement.START.label(), currentTheme().accent());
        soundEngine.playStart();
    }

    private void updateGame(double delta) {
        songTime += delta;
        spawnChartNotes();

        Iterator<Note> iterator = notes.iterator();
        while (iterator.hasNext()) {
            Note note = iterator.next();
            note.move(activeProfile.noteSpeed() * delta);

            if (note.activeHold()) {
                if (!pressedKeys.contains(note.key())) {
                    iterator.remove();
                    registerMiss(Judgement.EARLY);
                    continue;
                }
                if (note.tailY() >= LANE_Y) {
                    iterator.remove();
                    rewardHold();
                }
                continue;
            }

            if (note.headCenterY() > LANE_Y + MISS_WINDOW) {
                iterator.remove();
                registerMiss(Judgement.DROP);
            }
        }

        if (nextChartIndex >= activeChart.size() && notes.isEmpty() && songTime > selectedSong.durationSeconds()) {
            soundEngine.stopBackingTrack();
            state = GameState.GAME_OVER;
            showFeedback("CLEAR", currentTheme().accent());
        }
    }

    private void spawnChartNotes() {
        double travelTime = (LANE_Y + Note.HEAD_HEIGHT / 2.0) / activeProfile.noteSpeed();
        while (nextChartIndex < activeChart.size()) {
            ChartNote chartNote = activeChart.get(nextChartIndex);
            double noteTime = chartNote.beat() * selectedSong.secondsPerBeat();
            if (noteTime - travelTime > songTime) {
                break;
            }

            int laneIndex = Math.floorMod(chartNote.laneSeed(), activeProfile.keys().size());
            KeyCode key = activeProfile.keys().get(laneIndex);
            double holdHeight = chartNote.isHold()
                    ? Note.HEAD_HEIGHT + chartNote.holdBeats() * selectedSong.secondsPerBeat() * activeProfile.noteSpeed()
                    : Note.HEAD_HEIGHT;
            notes.add(new Note(-Note.HEAD_HEIGHT, key, chartNote.isHold(), holdHeight));
            nextChartIndex++;
        }
    }

    private Note findHittableNote(KeyCode key) {
        for (Note note : notes) {
            if (note.key() == key && !note.activeHold() && judge(note) != null) {
                return note;
            }
        }
        return null;
    }

    private Note findHeldNote(KeyCode key) {
        for (Note note : notes) {
            if (note.key() == key && note.activeHold()) {
                return note;
            }
        }
        return null;
    }

    private Judgement judge(Note note) {
        double distance = Math.abs(note.headCenterY() - LANE_Y);
        if (distance <= 9) {
            return Judgement.PERFECT;
        }
        if (distance <= 18) {
            return Judgement.EXCELLENT;
        }
        if (distance <= 30) {
            return Judgement.GREAT;
        }
        if (distance <= HIT_WINDOW) {
            return Judgement.GOOD;
        }
        return null;
    }

    private void rewardHold() {
        score += Judgement.HOLD.points() + combo / 5;
        showFeedback(Judgement.HOLD.label(), Judgement.HOLD.color());
        if (!selectedSong.hasMediaFile()) {
            soundEngine.playJudgement(Judgement.HOLD);
        }
    }

    private void finishHold(Note note) {
        notes.remove(note);
        rewardHold();
    }

    private void registerMiss(Judgement judgement) {
        if (state != GameState.PLAYING) {
            return;
        }
        combo = 0;
        misses++;
        showFeedback(judgement.label(), judgement.color());
        if (!selectedSong.hasMediaFile()) {
            soundEngine.playMiss();
        }
        if (misses >= activeMode.missLimit()) {
            soundEngine.stopBackingTrack();
            state = GameState.GAME_OVER;
        }
    }

    private void showFeedback(String text, Color color) {
        feedbackText = text;
        feedbackColor = color;
        feedbackTimer = 0.7;
    }

    private double laneX(int laneIndex) {
        double totalWidth = activeProfile.keys().size() * NOTE_WIDTH + (activeProfile.keys().size() - 1) * LANE_GAP;
        return (playfieldWidth() - totalWidth) / 2 + laneIndex * (NOTE_WIDTH + LANE_GAP);
    }

    private void draw(GraphicsContext gc) {
        drawBackground(gc);
        if (state == GameState.MENU) {
            drawMenu(gc);
            return;
        }

        drawHud(gc);
        drawLanes(gc);
        drawHitLine(gc);
        drawNotes(gc);
        drawFeedback(gc, 42, 230, TextAlignment.LEFT);

        if (state == GameState.GAME_OVER) {
            drawGameOver(gc);
        }
    }

    private void drawBackground(GraphicsContext gc) {
        GameTheme theme = currentTheme();
        LinearGradient fill = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, theme.backgroundTop()),
                new Stop(0.5, theme.backgroundMid()),
                new Stop(1, theme.backgroundBottom())
        );
        gc.setFill(fill);
        gc.fillRect(0, 0, soundEngine.hasActiveVideoBackground() ? playfieldWidth() : CANVAS_WIDTH, CANVAS_HEIGHT);

        if (soundEngine.hasActiveVideoBackground()) {
            double panelX = playfieldWidth() + 18;
            double panelWidth = CANVAS_WIDTH - panelX - 18;
            gc.setGlobalAlpha(0.26);
            gc.setFill(fill);
            gc.fillRect(playfieldWidth(), 0, CANVAS_WIDTH - playfieldWidth(), CANVAS_HEIGHT);
            gc.setGlobalAlpha(1);
            gc.setFill(Color.color(0.02, 0.05, 0.08, 0.14));
            gc.fillRoundRect(panelX, 22, panelWidth, CANVAS_HEIGHT - 44, 28, 28);
            gc.setStroke(theme.glow().deriveColor(0, 1, 1, 0.55));
            gc.strokeRoundRect(panelX, 22, panelWidth, CANVAS_HEIGHT - 44, 28, 28);
        }

        gc.setGlobalAlpha(0.18);
        gc.setFill(theme.accent());
        gc.fillOval(-120, -90, 340, 220);
        gc.setFill(theme.glow());
        gc.fillOval(CANVAS_WIDTH - 240, 32, 320, 240);
        gc.setFill(theme.laneColor(3));
        gc.fillOval(110, CANVAS_HEIGHT - 160, 420, 210);
        gc.setGlobalAlpha(1);
    }

    private void drawMenu(GraphicsContext gc) {
        Song song = selectedSong;
        GameTheme theme = currentTheme();
        DifficultyProfile previewProfile = DifficultyProfile.forLevel(selectedDifficulty);
        GameMode previewMode = GameMode.values()[selectedModeIndex];

        gc.setFill(Color.color(0, 0, 0, 0.18));
        gc.fillRoundRect(72, 54, CANVAS_WIDTH - 144, CANVAS_HEIGHT - 108, 28, 28);

        gc.setFill(theme.panel());
        gc.fillRoundRect(92, 78, CANVAS_WIDTH - 184, CANVAS_HEIGHT - 144, 28, 28);
        gc.setStroke(Color.color(1, 1, 1, 0.12));
        gc.strokeRoundRect(92, 78, CANVAS_WIDTH - 184, CANVAS_HEIGHT - 144, 28, 28);

        gc.setFill(theme.accent());
        gc.setFont(TITLE_FONT);
        gc.fillText("GAME MENU", 128, 148);

        gc.setFill(Color.WHITE);
        gc.setFont(SUBTITLE_FONT);
        gc.fillText("Use up/down to move, left/right to change, enter to start.", 128, 184);

        double leftPanelX = 122;
        double leftPanelY = 214;
        double leftPanelWidth = 430;
        double rowHeight = 82;
        double rowGap = 10;

        drawMenuRow(gc, 0, "TRACK", song.title(), songDetail(song), leftPanelX, leftPanelY, leftPanelWidth, rowHeight, theme);
        drawMenuRow(gc, 1, "DIFFICULTY", String.valueOf(selectedDifficulty), "lanes  " + previewProfile.keys().size() + "    bpm  " + (int) song.bpm(), leftPanelX, leftPanelY + (rowHeight + rowGap), leftPanelWidth, rowHeight, theme);
        drawMenuRow(gc, 2, "MODE", previewMode.label(), previewMode.description() + "    sound  " + (soundEngine.isEnabled() ? "on" : "off"), leftPanelX, leftPanelY + (rowHeight + rowGap) * 2, leftPanelWidth, rowHeight, theme);
        drawMenuRow(gc, 3, "START", "Play", "press enter to launch this chart", leftPanelX, leftPanelY + (rowHeight + rowGap) * 3, leftPanelWidth, rowHeight, theme);

        drawSongPreview(gc, song, previewProfile);

        gc.setFont(MENU_FONT);
        gc.setFill(Color.color(1, 1, 1, 0.75));
        gc.fillText("Author: Herb", 790, 140);
        gc.fillText("S toggles sound", 790, 166);
        gc.fillText("MP4 lyric video on the right in-game", 720, 192);

        drawFeedback(gc, CANVAS_WIDTH - 165, 132, TextAlignment.CENTER);
    }

    private void drawMenuRow(GraphicsContext gc, int rowIndex, String label, String value, String detail,
                             double x, double y, double width, double height, GameTheme theme) {
        boolean selected = menuFieldIndex == rowIndex;
        gc.setFill(selected ? theme.accent().deriveColor(0, 1, 1, 0.17) : Color.color(1, 1, 1, 0.05));
        gc.fillRoundRect(x, y, width, height, 18, 18);
        gc.setStroke(selected ? theme.glow().deriveColor(0, 1, 1, 0.8) : Color.color(1, 1, 1, 0.08));
        gc.strokeRoundRect(x, y, width, height, 18, 18);

        gc.setFont(PANEL_LABEL_FONT);
        gc.setFill(selected ? theme.glow() : Color.color(1, 1, 1, 0.72));
        gc.fillText(label, x + 18, y + 24);
        gc.setFont(PANEL_VALUE_FONT);
        gc.setFill(Color.WHITE);
        gc.fillText(trimToFit(value, PANEL_VALUE_FONT, width - 36), x + 18, y + 54);
        gc.setFont(PANEL_TEXT_FONT);
        gc.setFill(Color.color(1, 1, 1, 0.78));
        gc.fillText(trimToFit(detail, PANEL_TEXT_FONT, width - 36), x + 18, y + 74);
    }

    private void drawSongPreview(GraphicsContext gc, Song song, DifficultyProfile previewProfile) {
        double previewX = 612;
        double previewY = 216;
        double previewWidth = 250;
        double previewHeight = 286;
        gc.setFill(Color.color(1, 1, 1, 0.05));
        gc.fillRoundRect(previewX, previewY, previewWidth, previewHeight, 24, 24);

        int laneCount = Math.min(4, previewProfile.keys().size());
        for (int i = 0; i < laneCount; i++) {
            double laneX = previewX + 22 + i * 52;
            gc.setFill(song.theme().laneColor(i).deriveColor(0, 1, 1, 0.26));
            gc.fillRoundRect(laneX, previewY + 18, 40, previewHeight - 36, 16, 16);
        }

        int previewNotes = Math.min(12, song.chart().size());
        for (int i = 0; i < previewNotes; i++) {
            ChartNote note = song.chart().get(i);
            int lane = Math.floorMod(note.laneSeed(), laneCount);
            double x = previewX + 28 + lane * 52;
            double y = previewY + 36 + i * 16;
            gc.setFill(song.theme().laneColor(lane));
            gc.fillRoundRect(x, y, 28, note.isHold() ? 42 : 16, 10, 10);
        }

        gc.setStroke(song.theme().glow());
        gc.setLineWidth(4);
        gc.strokeLine(previewX + 16, previewY + previewHeight - 34, previewX + previewWidth - 16, previewY + previewHeight - 34);
    }

    private void drawHud(GraphicsContext gc) {
        Song song = selectedSong;
        GameTheme theme = currentTheme();

        gc.setFill(theme.panel());
        gc.fillRoundRect(24, 24, 260, 176, 24, 24);
        gc.setStroke(Color.color(1, 1, 1, 0.12));
        gc.strokeRoundRect(24, 24, 260, 176, 24, 24);

        gc.setFill(theme.accent());
        gc.setFont(PANEL_LABEL_FONT);
        gc.fillText(trimToFit(song.title().toUpperCase(), PANEL_LABEL_FONT, 224), 42, 48);

        gc.setFill(Color.WHITE);
        gc.setFont(PANEL_VALUE_FONT);
        gc.fillText(String.valueOf(score), 42, 84);

        gc.setFont(PANEL_TEXT_FONT);
        gc.fillText("combo  " + combo, 42, 114);
        gc.fillText("mode   " + activeMode.label(), 42, 138);
        gc.fillText("miss   " + misses + "/" + missLabel(), 42, 162);
        gc.fillText(song.artist().isBlank() ? song.caption() : "artist " + song.artist(), 42, 186);
    }

    private String missLabel() {
        return activeMode == GameMode.PRACTICE ? "INF" : String.valueOf(activeMode.missLimit());
    }

    private void drawLanes(GraphicsContext gc) {
        double pulse = 0.60 + 0.40 * Math.sin(pulseTime * 2.8);
        for (int i = 0; i < activeProfile.keys().size(); i++) {
            double x = laneX(i);
            Color laneColor = currentTheme().laneColor(i);
            LinearGradient fill = new LinearGradient(
                    0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, laneColor.deriveColor(0, 1, 1.1, 0.30)),
                    new Stop(1, Color.color(1, 1, 1, 0.04))
            );
            gc.setFill(fill);
            gc.fillRoundRect(x, 34, NOTE_WIDTH, CANVAS_HEIGHT - 68, 18, 18);

            gc.setStroke(Color.color(1, 1, 1, 0.12));
            gc.strokeRoundRect(x, 34, NOTE_WIDTH, CANVAS_HEIGHT - 68, 18, 18);

            gc.setFill(laneColor.deriveColor(0, 1, 1, 0.22 + 0.14 * pulse));
            gc.fillRoundRect(x + 6, 48, NOTE_WIDTH - 12, 18, 10, 10);

            gc.setTextAlign(TextAlignment.CENTER);
            gc.setFill(Color.WHITE);
            gc.setFont(KEY_FONT);
            gc.fillText(activeProfile.keys().get(i).getName(), x + NOTE_WIDTH / 2, LANE_Y - 18);
        }
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawHitLine(GraphicsContext gc) {
        GameTheme theme = currentTheme();
        double glow = 0.55 + 0.45 * Math.sin(pulseTime * 6);
        gc.setStroke(Color.color(1, 1, 1, 0.18));
        gc.strokeLine(84, LANE_Y + 10, playfieldWidth() - 84, LANE_Y + 10);
        gc.setStroke(theme.glow().deriveColor(0, 1, 1, 0.35 + 0.35 * glow));
        gc.setLineWidth(8);
        gc.strokeLine(84, LANE_Y, playfieldWidth() - 84, LANE_Y);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeLine(84, LANE_Y, playfieldWidth() - 84, LANE_Y);
    }

    private void drawNotes(GraphicsContext gc) {
        for (Note note : notes) {
            int laneIndex = activeProfile.keys().indexOf(note.key());
            if (laneIndex < 0) {
                continue;
            }

            double x = laneX(laneIndex);
            Color laneColor = currentTheme().laneColor(laneIndex);

            if (note.hold()) {
                double bodyHeight = Math.max(0, note.totalHeight() - Note.HEAD_HEIGHT);
                gc.setFill(laneColor.deriveColor(0, 1, 1, note.activeHold() ? 0.55 : 0.30));
                gc.fillRoundRect(x + 16, note.y() + Note.HEAD_HEIGHT - 4, NOTE_WIDTH - 32, bodyHeight + 8, 12, 12);
            }

            gc.setFill(laneColor.deriveColor(0, 1, 1.25, 0.18));
            gc.fillRoundRect(x - 4, note.y() - 5, NOTE_WIDTH + 8, Note.HEAD_HEIGHT + 10, 16, 16);

            LinearGradient fill = new LinearGradient(
                    0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, laneColor.brighter()),
                    new Stop(1, laneColor.darker())
            );
            gc.setFill(fill);
            gc.fillRoundRect(x, note.y(), NOTE_WIDTH, Note.HEAD_HEIGHT, 14, 14);
            gc.setStroke(Color.color(1, 1, 1, 0.42));
            gc.strokeRoundRect(x, note.y(), NOTE_WIDTH, Note.HEAD_HEIGHT, 14, 14);
        }
    }

    private void drawFeedback(GraphicsContext gc, double x, double y, TextAlignment align) {
        if (feedbackTimer <= 0) {
            return;
        }
        gc.setGlobalAlpha(Math.min(1, feedbackTimer * 1.6));
        gc.setTextAlign(align);
        gc.setFill(feedbackColor);
        gc.setFont(FEEDBACK_FONT);
        gc.fillText(feedbackText, x, y - (0.7 - feedbackTimer) * 34);
        gc.setGlobalAlpha(1);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawGameOver(GraphicsContext gc) {
        GameTheme theme = currentTheme();
        gc.setFill(Color.color(0, 0, 0, 0.58));
        gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        gc.setFill(theme.panel());
        gc.fillRoundRect(CANVAS_WIDTH / 2 - 186, CANVAS_HEIGHT / 2 - 104, 372, 212, 28, 28);
        gc.setStroke(Color.color(1, 1, 1, 0.15));
        gc.strokeRoundRect(CANVAS_WIDTH / 2 - 186, CANVAS_HEIGHT / 2 - 104, 372, 212, 28, 28);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(theme.warning());
        gc.setFont(TITLE_FONT);
        gc.fillText("RUN OVER", CANVAS_WIDTH / 2, CANVAS_HEIGHT / 2 - 30);

        gc.setFill(Color.WHITE);
        gc.setFont(SUBTITLE_FONT);
        gc.fillText("score  " + score + "    combo  " + combo, CANVAS_WIDTH / 2, CANVAS_HEIGHT / 2 + 12);
        gc.fillText("R to replay    ENTER for menu", CANVAS_WIDTH / 2, CANVAS_HEIGHT / 2 + 52);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private GameTheme currentTheme() {
        return selectedSong.theme();
    }

    private double playfieldWidth() {
        return CANVAS_WIDTH - (soundEngine.hasActiveVideoBackground() ? VIDEO_PANEL_WIDTH : 0);
    }

    private String songDetail(Song song) {
        if (song.artist().isBlank()) {
            return song.caption();
        }
        return song.artist() + "  |  " + song.caption();
    }

    private String trimToFit(String text, Font font, double maxWidth) {
        if (text == null || text.isBlank()) {
            return "";
        }
        Text helper = new Text(text);
        helper.setFont(font);
        if (helper.getLayoutBounds().getWidth() <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        for (int i = text.length() - 1; i > 0; i--) {
            String candidate = text.substring(0, i) + ellipsis;
            helper.setText(candidate);
            if (helper.getLayoutBounds().getWidth() <= maxWidth) {
                return candidate;
            }
        }
        return ellipsis;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static void main(String[] args) {
        launch(args);
    }
}

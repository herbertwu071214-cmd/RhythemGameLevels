package com.herb.rhythm.rhythemgamelevels;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

final class SoundEngine {
    private static final float SAMPLE_RATE = 44_100f;

    private final ExecutorService effectsWorker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "rhythm-sound");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean backingTrackRunning = new AtomicBoolean(false);

    private volatile boolean enabled = true;
    private volatile Thread backingTrackThread;
    private volatile MediaPlayer backingPlayer;
    private volatile MediaView mediaView;
    private volatile boolean videoBackgroundActive;

    boolean isEnabled() {
        return enabled;
    }

    void toggle() {
        enabled = !enabled;
        if (!enabled) {
            stopBackingTrack();
        }
    }

    void playMenuMove() {
        playTone(520, 55, 0.16);
    }

    void playStart() {
        playTone(620, 90, 0.18);
    }

    void playJudgement(Judgement judgement) {
        playTone(judgement.toneHz(), judgement == Judgement.HOLD ? 110 : 75, 0.18);
    }

    void playMiss() {
        playTone(220, 120, 0.18);
    }

    void startBackingTrack(Song song) {
        stopBackingTrack();
        if (!enabled) {
            return;
        }

        if (tryStartMedia(song)) {
            return;
        }

        backingTrackRunning.set(true);
        Thread thread = new Thread(() -> runBackingTrack(song), "rhythm-backing");
        thread.setDaemon(true);
        backingTrackThread = thread;
        thread.start();
    }

    void attachMediaView(MediaView mediaView) {
        this.mediaView = mediaView;
        mediaView.setPreserveRatio(false);
    }

    boolean hasActiveVideoBackground() {
        return videoBackgroundActive;
    }

    void stopBackingTrack() {
        backingTrackRunning.set(false);
        videoBackgroundActive = false;
        Thread thread = backingTrackThread;
        backingTrackThread = null;
        if (thread != null) {
            thread.interrupt();
        }
        MediaPlayer player = backingPlayer;
        backingPlayer = null;
        if (player != null) {
            try {
                player.stop();
            } finally {
                player.dispose();
            }
        }
        MediaView currentMediaView = mediaView;
        if (currentMediaView != null) {
            currentMediaView.setMediaPlayer(null);
        }
    }

    private void playTone(int hz, int millis, double volume) {
        if (!enabled) {
            return;
        }
        effectsWorker.submit(() -> emitTone(hz, millis, volume));
    }

    private void runBackingTrack(Song song) {
        long stepMillis = Math.max(110, Math.round(song.secondsPerBeat() * 1000));
        int step = 0;
        while (enabled && backingTrackRunning.get() && !Thread.currentThread().isInterrupted()) {
            int bass = song.bassAt(step / 2);
            int lead = song.leadAt(step);

            emitTone(bass, (int) Math.min(220, stepMillis), 0.09);
            if (step % 2 == 0) {
                emitTone(lead, (int) Math.min(160, stepMillis * 0.75), step % 4 == 0 ? 0.12 : 0.08);
            }
            if (step % 4 == 3) {
                emitTone(lead + 12, (int) Math.min(120, stepMillis * 0.55), 0.06);
            }

            long sleepMillis = Math.max(25, stepMillis - 140);
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
            step++;
        }
    }

    private boolean tryStartMedia(Song song) {
        if (!song.hasMediaFile()) {
            return false;
        }

        Path path = Paths.get(song.mediaPath());
        if (!path.isAbsolute()) {
            path = Paths.get("").resolve(path).normalize();
        }
        if (!Files.exists(path)) {
            return false;
        }
        Path mediaPath = path;

        Runnable startMedia = () -> {
            try {
                MediaPlayer player = new MediaPlayer(new Media(mediaPath.toUri().toString()));
                MediaView currentMediaView = mediaView;
                videoBackgroundActive = song.hasVideoBackground() && currentMediaView != null;
                player.setOnEndOfMedia(() -> {
                    backingTrackRunning.set(false);
                    videoBackgroundActive = false;
                });
                player.setOnError(() -> videoBackgroundActive = false);
                backingPlayer = player;
                backingTrackRunning.set(true);
                if (currentMediaView != null) {
                    currentMediaView.setMediaPlayer(player);
                }
                player.play();
            } catch (Exception ignored) {
                MediaPlayer player = backingPlayer;
                backingPlayer = null;
                videoBackgroundActive = false;
                if (player != null) {
                    player.dispose();
                }
            }
        };

        if (Platform.isFxApplicationThread()) {
            startMedia.run();
            return backingPlayer != null;
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean started = new AtomicBoolean(false);
        Platform.runLater(() -> {
            try {
                startMedia.run();
                started.set(backingPlayer != null);
            } finally {
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        }
        return started.get();
    }

    private void emitTone(int hz, int millis, double volume) {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        byte[] buffer = new byte[(int) (SAMPLE_RATE * millis / 1000.0) * 2];
        for (int i = 0; i < buffer.length / 2; i++) {
            double angle = i / (SAMPLE_RATE / hz) * 2.0 * Math.PI;
            short sample = (short) (Math.sin(angle) * Short.MAX_VALUE * volume);
            buffer[i * 2] = (byte) (sample & 0xff);
            buffer[i * 2 + 1] = (byte) ((sample >> 8) & 0xff);
        }

        try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
            line.open(format);
            line.start();
            line.write(buffer, 0, buffer.length);
            line.drain();
        } catch (IllegalArgumentException | LineUnavailableException ignored) {
            // Audio is optional. The game stays playable without it.
        }
    }
}

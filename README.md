# RhythemGameLevels

This project came about because I wanted a rhythm game that didn't just play the same static patterns every time. I wanted something that felt "alive"—where the difficulty actually changes how the music is mapped, not just how fast the notes fall. It's about getting that perfect "flow" without having to manually map every single second of a song.

## What is RhythemGameLevels?

RhythemGameLevels is a JavaFX-based rhythm engine designed to take base song data and "smart-transform" it into different difficulty profiles. Whether you're just chilling with a level 1 beat or sweating through a level 5 gauntlet with chord accents and gap fills, the app handles the heavy lifting so you can just focus on the rhythm.

### Key Features:
- **Dynamic Chart Transformation:** Levels 1 through 5 aren't just faster; they're structurally different. The `ChartTransformer` adds chords and fills as you crank up the heat.
- **Video Integration:** It's not just a black screen. The game supports video backgrounds (like the included `.mp4` files) to keep the vibe right while you play.
- **Custom Sound Engine:** Built on JavaFX Media, it handles the synchronization between the audio, video, and the note lanes.
- **Real-time Feedback:** Combo counters, miss tracking, and graded judgements (Perfect/Great/etc.) to let you know exactly how you're doing.
- **Song Library:** Easily expandable song list with built-in support for multiple lanes and varying BPMs.

---

## How to Replicate (Development Setup)

If you want to run this yourself or build upon it, here is how you can set it up. Just like arranging things before a club meeting, having the right setup ensures everything runs smoothly.

### 1. Prerequisites
- **Java 21 or higher:** We’re using modern Java features here, so make sure your JDK is up to date.
- **Maven:** For managing all the JavaFX dependencies (the Maven Wrapper `mvnw` is included if you don't have it installed globally).

### 2. Get the Code
```bash
git clone https://github.com/herbertwu071214-cmd/RhythemGameLevels.git
cd RhythemGameLevels
```

### 3. Running the Game
You have two main ways to get the music started:

**Option A: Using Maven (The Developer Way)**
If you're already in your terminal, just run:
```bash
./mvnw clean javafx:run
```

**Option B: The Executable Script (The Quick Way)**
If you're on macOS/Linux, you can use the provided command script:
```bash
chmod +x run-game.command
./run-game.command
```

### 4. Project Structure
- `src/main/java/...`: Where the logic lives. Check out `RhythmGame.java` for the engine and `ChartTransformer.java` for the level-scaling magic.
- `music/`: Drop your `.mp4` and `.mp3` files here to expand the library.
- `pom.xml`: All the JavaFX 21 modules are wired up here.

---

## Controls
- **Menu:** Use the Arrow Keys to navigate and Enter to select your song and difficulty.
- **Gameplay:** Match the keys shown in the lanes (defaults usually follow standard rhythm game layouts like D-F-J-K).
- **Exit:** ESC or close the window when you're done.

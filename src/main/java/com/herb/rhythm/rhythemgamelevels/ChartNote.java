package com.herb.rhythm.rhythemgamelevels;

record ChartNote(double beat, int laneSeed, double holdBeats) {
    boolean isHold() {
        return holdBeats > 0;
    }
}

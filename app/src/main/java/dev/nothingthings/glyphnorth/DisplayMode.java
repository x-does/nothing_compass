package dev.nothingthings.glyphnorth;

public final class DisplayMode {
    public static final int OFF = 0, TIME = 1, COMPASS = 2;
    private DisplayMode() {}
    public static int next(int mode) { return (mode + 1) % 3; }
    public static String label(int mode) { return mode == TIME ? "Time" : mode == COMPASS ? "Compass" : "Off"; }
}

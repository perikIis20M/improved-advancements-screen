package betteradvancements.common.gui;

/** The same saved percentage range for both advancement windows. */
public final class ScreenSize {
    public static final int MIN = 60, MAX = 100, DEFAULT = 100;
    private ScreenSize() { }
    public static int clamp(int value) { return Math.max(MIN, Math.min(MAX, value)); }
}

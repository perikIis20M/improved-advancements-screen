package betteradvancements.common.gui;

/** Shared window dimensions for the tabbed view and its search view. */
record ModernScreenGeometry(int internalWidth, int internalHeight, int left, int top, int right, int bottom) {
    static final int SIDE = 30, TOP = 40, BOTTOM = 30, PADDING = 9;

    static ModernScreenGeometry create(int width, int height, int percent) {
        int scale = ScreenSize.clamp(percent);
        int innerWidth = Math.min(width, Math.max(252, width * scale * 80 / 10000));
        int innerHeight = Math.min(height, Math.max(140, height * scale / 100));
        int left = SIDE + (width - innerWidth) / 2;
        int top = TOP + (height - innerHeight) / 2;
        return new ModernScreenGeometry(innerWidth, innerHeight, left, top,
            left + innerWidth - 2 * SIDE, top + innerHeight - TOP - BOTTOM);
    }
}

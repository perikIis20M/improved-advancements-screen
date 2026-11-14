package betteradvancements.common.gui;

/** Resizes the window; frame pixels, text, items and mouse coordinates keep Minecraft's GUI scale. */
public record LegacyScreenGeometry(int left, int top, int width, int height) {
    public static final int FRAME_WIDTH = 256, FRAME_HEIGHT = 202;
    public static final int MAP_LEFT = 16, MAP_TOP = 17, MAP_WIDTH = 224, MAP_HEIGHT = 155;
    public static final int MIN_SIZE = ScreenSize.MIN, MAX_SIZE = ScreenSize.MAX, DEFAULT_SIZE = ScreenSize.DEFAULT;

    public static int clampSize(int percent) { return ScreenSize.clamp(percent); }

    public static LegacyScreenGeometry create(int width, int height, int percent) {
        return create(width, height, percent, false);
    }

    public static LegacyScreenGeometry create(int width, int height, int percent, boolean navigationTab) {
        int header = navigationTab ? NavigationTabLayout.TOP_SPACE : 0;
        double availableWidth = Math.max(1, width - 8), availableHeight = Math.max(1, height - 8 - header);
        double largestScale = Math.min(availableWidth / FRAME_WIDTH, availableHeight / FRAME_HEIGHT);
        // As with the modern window, stop shrinking at the size needed by fixed-size controls.
        double size = Math.max(Math.min(1, largestScale), largestScale * clampSize(percent) / 100.0);
        int frameWidth = Math.min((int) availableWidth, (int) Math.floor(FRAME_WIDTH * size + 1.0E-7));
        int frameHeight = Math.min((int) availableHeight, (int) Math.floor(FRAME_HEIGHT * size + 1.0E-7));
        return new LegacyScreenGeometry((width - frameWidth) / 2, header + (height - header - frameHeight) / 2, frameWidth, frameHeight);
    }

    public int mapWidth() { return width - (FRAME_WIDTH - MAP_WIDTH); }
    public int mapHeight() { return height - (FRAME_HEIGHT - MAP_HEIGHT); }
    public int footerY() { return top + height - 27; }
    public int buttonWidth() { return Math.min(80, (width - 48) / 2); }
    public int leftButtonX() { return left + 20; }
    public int rightButtonX(int buttonWidth) { return left + width - 24 - buttonWidth; }
    public float screenX(float localX) { return left + localX; }
    public float screenY(float localY) { return top + localY; }
    public double mapX(double screenX) { return screenX - clipLeft(); }
    public double mapY(double screenY) { return screenY - clipTop(); }
    public double dragDistance(double screenDelta, float zoom) { return screenDelta * zoom; }
    public int clipLeft() { return left + MAP_LEFT; }
    public int clipTop() { return top + MAP_TOP; }
    public int clipRight() { return clipLeft() + mapWidth(); }
    public int clipBottom() { return clipTop() + mapHeight(); }
    public boolean insideMap(double screenX, double screenY) {
        double x = mapX(screenX), y = mapY(screenY);
        return x >= 0 && x < mapWidth() && y >= 0 && y < mapHeight();
    }
}

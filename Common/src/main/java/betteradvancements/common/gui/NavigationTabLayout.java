package betteradvancements.common.gui;

import java.util.List;

/** Fixed GUI-pixel tabs attached to the window, without moving or replacing other controls. */
public final class NavigationTabLayout {
    public static final int WIDTH = 28, HEIGHT = 32, TOP_SPACE = 28;
    private NavigationTabLayout() { }
    public record Rect(int x, int y, int width, int height) {
        public int right() { return x + width; }
        public int bottom() { return y + height; }
        public boolean intersects(Rect other) {
            return x < other.right() && right() > other.x && y < other.bottom() && bottom() > other.y;
        }
    }
    public static Rect find(Rect frame, int screenWidth, int screenHeight, List<Rect> occupied) {
        for (int x = frame.right() - WIDTH; x >= frame.x(); x -= WIDTH + 4) {
            Rect tab = new Rect(x, frame.y() - TOP_SPACE, WIDTH, HEIGHT);
            if (fits(tab, screenWidth, screenHeight, occupied)) return tab;
        }
        // A full top row can occur with category tabs or another inventory extension.
        for (int y = frame.y(); y + HEIGHT <= frame.bottom(); y += HEIGHT + 4) {
            Rect right = new Rect(frame.right() - 4, y, WIDTH, HEIGHT);
            if (fits(right, screenWidth, screenHeight, occupied)) return right;
            Rect left = new Rect(frame.x() - WIDTH + 4, y, WIDTH, HEIGHT);
            if (fits(left, screenWidth, screenHeight, occupied)) return left;
        }
        return null; // No safe slot: leave all existing controls intact.
    }
    private static boolean fits(Rect tab, int width, int height, List<Rect> occupied) {
        return tab.x() >= 0 && tab.y() >= 0 && tab.right() <= width && tab.bottom() <= height
            && occupied.stream().noneMatch(tab::intersects);
    }
}

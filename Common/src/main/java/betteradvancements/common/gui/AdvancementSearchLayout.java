package betteradvancements.common.gui;

/** Search stays within the parent advancement canvas. Coordinates are GUI pixels. */
public record AdvancementSearchLayout(int left, int top, int width, int height, int footerY) {
    public static AdvancementSearchLayout classic(LegacyScreenGeometry frame) {
        return new AdvancementSearchLayout(frame.clipLeft(), frame.clipTop(), frame.mapWidth(), frame.mapHeight(), frame.footerY());
    }

    static AdvancementSearchLayout modern(ModernScreenGeometry frame) {
        int pad = ModernScreenGeometry.PADDING;
        return new AdvancementSearchLayout(frame.left() + pad, frame.top() + 2 * pad,
            frame.right() - frame.left() - 2 * pad, frame.bottom() - frame.top() - 3 * pad, frame.bottom() + 4);
    }

    public int fieldX() { return left + 3; }
    public int fieldY() { return top + 3; }
    public int fieldWidth() { return width - 28; }
    public int clearX() { return left + width - 21; }
    public int gridTop() { return top + 23; }
    public int gridBottom() { return top + height - 2; }
    public int cellHeight() { return Math.min(42, gridBottom() - gridTop()); }
    public boolean compact() { return cellHeight() < 40; }
    public int columns() { return Math.max(1, (width - 2) / (compact() ? 140 : 72)); }
    public int rows() { return Math.max(1, (gridBottom() - gridTop()) / (cellHeight() + 2)); }
    public int cellWidth() { return Math.min(compact() ? 220 : 96, (width - 4 - (columns() - 1) * 4) / columns()); }
    public int cellX(int slot) {
        return left + (width - (columns() * (cellWidth() + 4) - 4)) / 2 + slot % columns() * (cellWidth() + 4);
    }
    public int cellY(int slot) { return gridTop() + slot / columns() * (cellHeight() + 2); }
    public int pageSize() { return columns() * rows(); }
    public int pageCount(int entries) { return Math.max(1, (entries + pageSize() - 1) / pageSize()); }
    public boolean insideResults(double x, double y) { return x >= left && x < left + width && y >= gridTop() && y < gridBottom(); }
}

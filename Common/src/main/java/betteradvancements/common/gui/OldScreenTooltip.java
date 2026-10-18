package betteradvancements.common.gui;

/** Fits a zoomed tooltip to the map without changing the camera or hover hitbox. */
public final class OldScreenTooltip {
    private static final int MARGIN = 4;
    private static final int ICON_HEIGHT = 26, LEFT_HEADER_END = 32;

    public record Placement(float headerX, float headerY, float scale, boolean drawLeft, boolean drawTop,
                            float boxX, float boxY) { }

    private OldScreenTooltip() { }

    public static Placement place(double anchorX, double anchorY, int contentWidth, int contentHeight,
                                  float zoom, int left, int top, int width, int height) {
        int margin = Math.min(MARGIN, Math.max(0, (Math.min(width, height) - 1) / 2));
        double minX = left + margin, minY = top + margin;
        double availableWidth = Math.max(1, width - 2 * margin);
        double availableHeight = Math.max(1, height - 2 * margin);
        // Round the fit scale down, so float conversion cannot push a large tooltip outside the edge.
        float scale = (float) Math.min(zoom, Math.min(availableWidth / contentWidth, availableHeight / contentHeight));
        if (contentWidth * (double) scale > availableWidth || contentHeight * (double) scale > availableHeight)
            scale = Math.nextDown(scale);
        boolean drawLeft = anchorX + contentWidth * scale > minX + availableWidth;
        boolean drawTop = anchorY + contentHeight * scale > minY + availableHeight;
        int offsetX = drawLeft ? LEFT_HEADER_END - contentWidth : 0;
        int offsetY = drawTop ? ICON_HEIGHT - contentHeight : 0;
        double boxX = Math.max(minX, Math.min(anchorX + offsetX * scale, minX + availableWidth - contentWidth * scale));
        double boxY = Math.max(minY, Math.min(anchorY + offsetY * scale, minY + availableHeight - contentHeight * scale));
        return new Placement((float) (boxX - offsetX * scale), (float) (boxY - offsetY * scale), scale,
            drawLeft, drawTop, (float) boxX, (float) boxY);
    }
}

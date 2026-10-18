package betteradvancements.common.gui;

/** One transform shared by rendering, dragging and hit testing on the old map. */
public final class OldScreenCamera {
    public static final int MIN_ZOOM = 50, MAX_ZOOM = 200, ZOOM_STEP = 25;
    private double centerX, centerY;
    private int zoomPercent = 100;

    public double centerX() { return centerX; }
    public double centerY() { return centerY; }
    public int zoomPercent() { return zoomPercent; }
    public float scale() { return zoomPercent / 100.0F; }
    public boolean canZoomIn() { return zoomPercent < MAX_ZOOM; }
    public boolean canZoomOut() { return zoomPercent > MIN_ZOOM; }

    public void setZoomPercent(int value) {
        zoomPercent = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, Math.round(value / (float) ZOOM_STEP) * ZOOM_STEP));
    }

    public void changeZoom(int steps) {
        setZoomPercent(zoomPercent + Math.max(-1, Math.min(1, steps)) * ZOOM_STEP);
    }

    public void centerOn(double x, double y) {
        centerX = Double.isFinite(x) ? x : 0;
        centerY = Double.isFinite(y) ? y : 0;
    }

    public double screenX(double worldX, int viewportWidth) { return viewportWidth / 2.0 + (worldX - centerX) * scale(); }
    public double screenY(double worldY, int viewportHeight) { return viewportHeight / 2.0 + (worldY - centerY) * scale(); }
    public double worldX(double screenX, int viewportWidth) { return centerX + (screenX - viewportWidth / 2.0) / scale(); }
    public double worldY(double screenY, int viewportHeight) { return centerY + (screenY - viewportHeight / 2.0) / scale(); }

    public void pan(double dx, double dy) {
        centerX -= dx / scale();
        centerY -= dy / scale();
    }

    public void clamp(int minX, int minY, int maxX, int maxY, int width, int height) {
        // Keep at least 32 screen pixels of the revealed area reachable at the edges.
        // Expanding the revealed bounds cannot recenter an already visible map.
        double reachX = Math.max(0, width / 2.0 - 32) / scale();
        double reachY = Math.max(0, height / 2.0 - 32) / scale();
        centerX = Math.max(minX - reachX, Math.min(maxX + reachX, centerX));
        centerY = Math.max(minY - reachY, Math.min(maxY + reachY, centerY));
    }
}

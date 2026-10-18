import betteradvancements.common.gui.OldScreenTooltip;

/** Regressions for zoomed headers and oversized criteria tooltips near every map edge. */
public final class OldScreenTooltipCheck {
    private static int assertions;
    public static void main(String[] args) {
        for (int zoom = 50; zoom <= 200; zoom += 25)
            for (int[] viewport : new int[][]{{1, 1}, {173, 147}, {320, 200}, {641, 479}, {1280, 721}})
                for (int[] content : new int[][]{{40, 32}, {80, 80}, {240, 160}, {500, 700}, {1400, 90}})
                    for (double[] anchor : new double[][]{{0, 0}, {viewport[0] - 1, 0}, {0, viewport[1] - 1},
                            {viewport[0] - 1, viewport[1] - 1}, {viewport[0] / 2.0, viewport[1] / 2.0}, {-20.5, 30.25}}) {
                        int left = 37, top = 29;
                        var p = OldScreenTooltip.place(left + anchor[0], top + anchor[1], content[0], content[1],
                            zoom / 100.0F, left, top, viewport[0], viewport[1]);
                        require(p.scale() > 0 && p.scale() <= zoom / 100.0F, "Follows zoom or shrinks to fit");
                        require(p.boxX() >= left - 0.001 && p.boxY() >= top - 0.001, "Top/left edges inside map");
                        require(p.boxX() + content[0] * p.scale() <= left + viewport[0] + 0.001, "Right edge inside map");
                        require(p.boxY() + content[1] * p.scale() <= top + viewport[1] + 0.001, "Bottom edge inside map");
                        int offsetX = p.drawLeft() ? 32 - content[0] : 0, offsetY = p.drawTop() ? 26 - content[1] : 0;
                        require(Math.abs(p.headerX() + offsetX * p.scale() - p.boxX()) < 0.001, "Header/body X aligned");
                        require(Math.abs(p.headerY() + offsetY * p.scale() - p.boxY()) < 0.001, "Header/body Y aligned");
                    }
        var normal = OldScreenTooltip.place(100, 100, 80, 60, 1.5F, 0, 0, 640, 480);
        require(normal.scale() == 1.5F && normal.headerX() == 100 && normal.headerY() == 100, "Fitting tooltip retains zoom and anchor");
        System.out.println("PASS: " + assertions + " assertions; aligned tooltip headers/body within resized maps at every 50–200% zoom step.");
    }
    private static void require(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}

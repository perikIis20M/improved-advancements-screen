package betteradvancements.common.gui;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Button;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import static betteradvancements.common.gui.LegacyScreenGeometry.*;

/** Window size must not change content pixels, hover transforms, drag speed or button size. */
public final class LegacyScreenSizeCheck {
    private static int assertions;
    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        require(BetterAdvancementsScreen.oldScreenSize == 100, "Fresh default");
        require(ScreenSize.MIN == 60 && ScreenSize.MAX == 100, "Shared limits");
        require(clampSize(Integer.MIN_VALUE) == 60 && clampSize(Integer.MAX_VALUE) == 100, "Config bounds");
        var largest = LegacyScreenGeometry.create(640, 480, 100);
        require(largest.height() == 472 && largest.top() == 4, "Maximum fills limiting dimension");
        require(LegacyScreenGeometry.create(640, 480, 200).equals(largest), "Clamp old oversized value");
        var smaller = LegacyScreenGeometry.create(640, 480, 60);
        require(smaller.mapWidth() < largest.mapWidth() && smaller.mapHeight() < largest.mapHeight(), "Resize visible area");
        for (int[] viewport : new int[][]{{160, 120}, {320, 240}, {427, 241}, {640, 360}, {853, 479}, {1280, 720}}) {
            LegacyScreenGeometry prior = null;
            for (int size = 60; size <= 100; size++) {
                var g = LegacyScreenGeometry.create(viewport[0], viewport[1], size);
                require(g.left() >= 4 && g.top() >= 4, "Frame clips top/left");
                require(g.left() + g.width() <= viewport[0] - 4 && g.top() + g.height() <= viewport[1] - 4, "Frame clips bottom/right");
                require(Math.abs(g.left() * 2 + g.width() - viewport[0]) <= 1, "Not centered horizontally");
                require(Math.abs(g.top() * 2 + g.height() - viewport[1]) <= 1, "Not centered vertically");
                if (prior != null) require(g.width() >= prior.width() && g.height() >= prior.height(), "Larger setting shrinks window");
                prior = g;
                require(g.clipLeft() - g.left() == 16 && g.clipTop() - g.top() == 17, "Top/left chrome scaled");
                require(g.left() + g.width() - g.clipRight() == 16 && g.top() + g.height() - g.clipBottom() == 30, "Bottom/right chrome scaled");
                require(g.clipRight() - g.clipLeft() == g.mapWidth() && g.clipBottom() - g.clipTop() == g.mapHeight(), "Scissor mismatch");
                require(g.insideMap(g.clipLeft(), g.clipTop()) && g.insideMap(g.clipRight() - 0.01, g.clipBottom() - 0.01), "Canvas pixel excluded");
                require(!g.insideMap(g.clipRight(), g.clipTop()) && !g.insideMap(g.clipLeft(), g.clipBottom()), "Border starts drag");
                require(!g.insideMap(g.leftButtonX(), g.footerY()), "Footer starts drag");
                var done = Button.builder(Component.literal("Done"), ignored -> {})
                    .bounds(g.rightButtonX(g.buttonWidth()), g.footerY(), g.buttonWidth(), 20).build();
                require(done.getHeight() == 20, "Window size changed button height");
                if (viewport[0] >= 320 && viewport[1] >= 240) require(done.getWidth() == 80, "Window size changed button width");
                require(done.isMouseOver(done.getX() + done.getWidth() / 2.0, done.getY() + 10), "Done hitbox misaligned");
                require(!done.isMouseOver(done.getX() - 1, done.getY()), "Done hitbox extends outside button");
                require(done.getX() + done.getWidth() < g.left() + g.width() && done.getY() + 20 < g.top() + g.height(), "Button outside frame");
                require(g.leftButtonX() + g.buttonWidth() < done.getX(), "Footer buttons overlap");
                for (float zoom = 1; zoom <= 2; zoom += 0.25F) {
                    var mapMatrix = new Matrix3x2f().translate(g.clipLeft(), g.clipTop()).scale(1 / zoom);
                    for (float[] point : new float[][]{{0, 0}, {17.5F, 9.25F}, {220, 150}, {-350, 311}, {423, -150}}) {
                        var rendered = mapMatrix.transformPosition(new Vector2f(point[0], point[1]));
                        near(g.mapX(rendered.x) * zoom, point[0], "Hover X");
                        near(g.mapY(rendered.y) * zoom, point[1], "Hover Y");
                        for (double delta : new double[]{-17.25, -0.5, 0.5, 31.75})
                            near(g.dragDistance(delta, zoom) / zoom, delta, "Drag must follow mouse pixels");
                    }
                    var tile = mapMatrix.transformDirection(new Vector2f(16, 16));
                    near(tile.x, 16 / zoom, "Window size enlarges terrain");
                    near(tile.y, 16 / zoom, "Window size stretches terrain");
                    var icon = mapMatrix.transformDirection(new Vector2f(26, 26));
                    near(icon.x, 26 / zoom, "Window size enlarges icons");
                    for (int offset = 0; offset < 16; offset++) {
                        int columns = (int) Math.ceil((g.mapWidth() * zoom + offset) / 16.0);
                        int rows = (int) Math.ceil((g.mapHeight() * zoom + offset) / 16.0);
                        require((columns * 16 - offset) / zoom >= g.mapWidth(), "Uncovered right terrain edge");
                        require((rows * 16 - offset) / zoom >= g.mapHeight(), "Uncovered bottom terrain edge");
                    }
                }
            }
        }
        System.out.println("PASS: " + assertions + " checks: all 60-100% sizes, six viewports, five map zooms, fixed content/chrome/buttons, clipping, hover, drag and terrain coverage.");
    }
    private static void near(double actual, double expected, String label) {
        require(Math.abs(actual - expected) < 0.001, label + ": " + actual + " vs " + expected);
    }
    private static void require(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}

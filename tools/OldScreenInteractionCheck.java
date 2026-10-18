import betteradvancements.common.gui.OldScreenCamera;
import betteradvancements.common.gui.OldScreenVisibility;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Standalone regressions for old-screen transforms and gradual discovery. */
public final class OldScreenInteractionCheck {
    private static int assertions;

    public static void main(String[] args) {
        cameraTransforms();
        cameraLimits();
        gradualDiscovery();
        System.out.println("PASS: " + assertions + " assertions; all 50–200% zoom steps, fractional drag/hit-test coordinates, odd/even viewports, pan limits, stable expansion, progressive discovery, hidden/completed nodes and cycles.");
    }

    private static void cameraTransforms() {
        int[][] viewports = {{1, 1}, {32, 31}, {63, 64}, {173, 174}, {320, 240}, {641, 479}, {1280, 721}};
        double[][] centers = {{0, 0}, {-123.375, 87.125}, {1567.75, -2143.625}};
        double[][] nodes = {{0, 0}, {-18.5, 26.25}, {512.875, -768.125}, {-983.75, -432.5}};
        double[][] drags = {{0.25, -0.75}, {-31.5, 17.25}, {64, -96}};
        for (int zoom = 50; zoom <= 200; zoom += 25) {
            for (int[] viewport : viewports) for (double[] center : centers) {
                var camera = new OldScreenCamera();
                camera.setZoomPercent(zoom);
                camera.centerOn(center[0], center[1]);
                near(zoom / 100.0, camera.scale(), "Zoom scale");
                near(viewport[0] / 2.0, camera.screenX(center[0], viewport[0]), "Center X must anchor to viewport midpoint");
                near(viewport[1] / 2.0, camera.screenY(center[1], viewport[1]), "Center Y must anchor to viewport midpoint");
                for (double[] node : nodes) {
                    near(node[0], camera.worldX(camera.screenX(node[0], viewport[0]), viewport[0]), "X hit testing must invert rendering");
                    near(node[1], camera.worldY(camera.screenY(node[1], viewport[1]), viewport[1]), "Y hit testing must invert rendering");
                    for (double[] drag : drags) {
                        camera.centerOn(center[0], center[1]);
                        double oldX = camera.screenX(node[0], viewport[0]);
                        double oldY = camera.screenY(node[1], viewport[1]);
                        camera.pan(drag[0], drag[1]);
                        near(oldX + drag[0], camera.screenX(node[0], viewport[0]), "Dragged X must follow mouse pixels at every scale");
                        near(oldY + drag[1], camera.screenY(node[1], viewport[1]), "Dragged Y must follow mouse pixels at every scale");
                    }
                }
                camera.centerOn(center[0], center[1]);
                camera.changeZoom(1);
                near(center[0], camera.worldX(viewport[0] / 2.0, viewport[0]), "Zoom in must preserve central world X");
                near(center[1], camera.worldY(viewport[1] / 2.0, viewport[1]), "Zoom in must preserve central world Y");
                camera.changeZoom(-1);
                near(center[0], camera.worldX(viewport[0] / 2.0, viewport[0]), "Zoom out must preserve central world X");
                near(center[1], camera.worldY(viewport[1] / 2.0, viewport[1]), "Zoom out must preserve central world Y");

                camera.setZoomPercent(zoom);
                camera.centerOn(-1e9, -1e9);
                camera.clamp(-500, -800, 1500, 2000, viewport[0], viewport[1]);
                double lowX = camera.centerX(), lowY = camera.centerY();
                near(-500 - Math.max(0, viewport[0] / 2.0 - 32) / camera.scale(), lowX, "Minimum X pan bound");
                near(-800 - Math.max(0, viewport[1] / 2.0 - 32) / camera.scale(), lowY, "Minimum Y pan bound");
                camera.centerOn(1e9, 1e9);
                camera.clamp(-500, -800, 1500, 2000, viewport[0], viewport[1]);
                double highX = camera.centerX(), highY = camera.centerY();
                near(1500 + Math.max(0, viewport[0] / 2.0 - 32) / camera.scale(), highX, "Maximum X pan bound");
                near(2000 + Math.max(0, viewport[1] / 2.0 - 32) / camera.scale(), highY, "Maximum Y pan bound");

                for (double[] prior : new double[][]{{lowX, lowY}, {highX, highY}, {111.375, 723.625}}) {
                    camera.centerOn(prior[0], prior[1]);
                    camera.clamp(-1500, -1800, 2500, 3000, viewport[0], viewport[1]);
                    near(prior[0], camera.centerX(), "Revealing nodes must not recenter X");
                    near(prior[1], camera.centerY(), "Revealing nodes must not recenter Y");
                }
            }
        }
    }

    private static void cameraLimits() {
        var camera = new OldScreenCamera();
        for (int input : new int[]{-10000, -1, 0, 49, 50, Integer.MIN_VALUE}) {
            camera.setZoomPercent(input);
            require(camera.zoomPercent() == 50, "Zoom below minimum must clamp to 50%: " + input);
            require(!camera.canZoomOut() && camera.canZoomIn(), "Minimum zoom button availability");
            camera.changeZoom(-1);
            require(camera.zoomPercent() == 50, "Minus button must stop at minimum");
        }
        for (int input : new int[]{200, 201, 10000, Integer.MAX_VALUE}) {
            camera.setZoomPercent(input);
            require(camera.zoomPercent() == 200, "Zoom above maximum must clamp to 200%: " + input);
            require(camera.canZoomOut() && !camera.canZoomIn(), "Maximum zoom button availability");
            camera.changeZoom(1);
            require(camera.zoomPercent() == 200, "Plus button must stop at maximum");
        }
        camera.setZoomPercent(112);
        require(camera.zoomPercent() == 100, "Saved scale rounds to nearest supported step");
        camera.setZoomPercent(113);
        require(camera.zoomPercent() == 125, "Saved scale rounds to nearest supported step");
        camera.changeZoom(0);
        require(camera.zoomPercent() == 125, "Zero steps must not change zoom");
        camera.changeZoom(99);
        require(camera.zoomPercent() == 150, "One plus press changes one step");
        camera.changeZoom(-99);
        require(camera.zoomPercent() == 125, "One minus press changes one step");
        camera.centerOn(Double.NaN, Double.POSITIVE_INFINITY);
        near(0, camera.centerX(), "Invalid saved X must recover");
        near(0, camera.centerY(), "Invalid saved Y must recover");
    }

    private static void gradualDiscovery() {
        Map<String, String> parents = new HashMap<>();
        parents.put("root", null);
        for (int i = 1; i <= 8; i++) parents.put("node" + i, i == 1 ? "root" : "node" + (i - 1));
        parents.put("branch", "node3");
        Set<String> completed = new HashSet<>();
        require(OldScreenVisibility.unlockDepth("root", parents, completed) == 0, "An unfinished root is available");
        for (int i = 1; i <= 5; i++) {
            int depth = OldScreenVisibility.unlockDepth("node" + i, parents, completed);
            require(depth == i, "Depth counts unfinished ancestors: " + i);
            require(OldScreenVisibility.isVisible(depth, false, false) == (i <= 4), "Only first four locked levels are revealed");
            require(OldScreenVisibility.hasTooltip(depth) == (i <= 3), "Level four has no tooltip; level three does");
        }
        completed.add("root");
        require(OldScreenVisibility.unlockDepth("node1", parents, completed) == 0, "Completing a parent makes its child available");
        int fifthDepth = OldScreenVisibility.unlockDepth("node5", parents, completed);
        require(fifthDepth == 4 && OldScreenVisibility.isVisible(fifthDepth, false, false), "Completing an ancestor reveals the next node");
        completed.add("node1");
        fifthDepth = OldScreenVisibility.unlockDepth("node5", parents, completed);
        require(fifthDepth == 3 && OldScreenVisibility.hasTooltip(fifthDepth), "Continued progress reveals the new tooltip");
        completed.add("node3");
        require(OldScreenVisibility.unlockDepth("branch", parents, completed) == 0, "Progress unlocks branching children");
        require(OldScreenVisibility.unlockDepth("node4", parents, completed) == 0, "Progress unlocks the continuing branch");

        completed.clear();
        completed.add("node8");
        require(OldScreenVisibility.unlockDepth("node8", parents, completed) == 0, "Completed nodes stay visible when ancestors are incomplete");
        require(OldScreenVisibility.isVisible(0, true, true), "A completed hidden node must be visible");
        require(OldScreenVisibility.isVisible(9, true, true), "Completed status overrides discovery distance");
        for (int depth = 0; depth <= 8; depth++) require(!OldScreenVisibility.isVisible(depth, true, false), "Hidden nodes must stay hidden until completion");
        require(OldScreenVisibility.unlockDepth("missing", parents, completed) == 0, "A node without an available parent is treated as a root");

        parents.put("loopA", "loopB");
        parents.put("loopB", "loopA");
        parents.put("self", "self");
        require(OldScreenVisibility.unlockDepth("loopA", parents, completed) > 4, "A cycle must terminate and stay undiscovered");
        require(OldScreenVisibility.unlockDepth("self", parents, completed) > 4, "A self-cycle must terminate");
        completed.add("loopA");
        require(OldScreenVisibility.unlockDepth("loopA", parents, completed) == 0, "Completed cyclic nodes remain visible");
        require(OldScreenVisibility.unlockDepth("loopB", parents, completed) == 0, "A completed ancestor terminates cyclic traversal");
    }

    private static void near(double expected, double actual, String label) {
        require(Double.isFinite(actual) && Math.abs(expected - actual) <= 1e-9, label + ": expected " + expected + ", got " + actual);
    }

    private static void require(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}

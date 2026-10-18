package betteradvancements.common.gui;

/** Regressions for search leaving a fitting tree stuck away from the normal center. */
public final class AdvancementScrollCheck {
    private static int assertions;
    public static void main(String[] args) {
        equal(150, AdvancementScrollBounds.reveal(-50, 172, 200, 0, 200, 500), "Fitting tree must remain centered");
        equal(150, AdvancementScrollBounds.constrain(-10000, 0, 200, 500), "Dragging cannot dislodge a fitting tree");
        equal(0, AdvancementScrollBounds.reveal(-100, 0, 28, 0, 300, 300), "Exact-fit tree must remain at zero");
        equal(-200, AdvancementScrollBounds.reveal(-200, 220, 248, 0, 900, 300), "Visible result should not move");
        equal(-50, AdvancementScrollBounds.reveal(-200, 50, 78, 0, 900, 300), "Reveal clipped left result with minimum movement");
        equal(-228, AdvancementScrollBounds.reveal(-200, 500, 528, 0, 900, 300), "Reveal clipped right result with minimum movement");
        int lastResult = AdvancementScrollBounds.reveal(0, 872, 900, 0, 900, 300);
        equal(-600, lastResult, "Last result stops at right boundary");
        equal(-550, AdvancementScrollBounds.constrain(lastResult + 50, 0, 900, 300), "Axis remains draggable after search");
        // Horizontal and vertical movement use this rule independently, including negative coordinates.
        for (int min : new int[]{-300, 0, 170}) for (int span : new int[]{28, 200, 300, 901})
            for (int viewport : new int[]{174, 300, 500}) for (int offset : new int[]{-10000, -500, 0, 500, 10000}) {
                int max = min + span;
                for (int target = min; target <= max - 28; target += 7) {
                    int result = AdvancementScrollBounds.reveal(offset, target, target + 28, min, max, viewport);
                    require(target + result >= 0 && target + 28 + result <= viewport, "Result remains clipped");
                    equal(result, AdvancementScrollBounds.constrain(result, min, max, viewport), "Search bypassed normal limits");
                    equal(result, AdvancementScrollBounds.reveal(result, target, target + 28, min, max, viewport), "Repeated search drifts");
                    if (span <= viewport) equal(AdvancementScrollBounds.center(min, max, viewport), result, "Fixed axis moved");
                }
            }
        System.out.println("PASS: " + assertions + " checks: fitting/scrollable axes, clipped/visible targets, negative positions, bounds and repeated search.");
    }
    private static void equal(int expected, int actual, String message) { require(expected == actual, message + ": " + actual + " vs " + expected); }
    private static void require(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}

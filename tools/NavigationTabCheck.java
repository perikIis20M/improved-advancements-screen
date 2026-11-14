package betteradvancements.common.gui;

import java.util.ArrayList;
import java.util.List;
import static betteradvancements.common.gui.NavigationTabLayout.*;

/** Placement regressions for recipe-book movement, other mods' tabs, resizing and full windows. */
public final class NavigationTabCheck {
    private static int checks;
    public static void main(String[] args) {
        Rect inventory = new Rect(72, 37, 176, 166);
        Rect first = find(inventory, 320, 240, List.of());
        require(first.equals(new Rect(220, 9, 28, 32)), "Attach to the inventory's top-right corner");
        var neighbors = new ArrayList<Rect>();
        neighbors.add(first);
        Rect second = find(inventory, 320, 240, neighbors);
        require(second.equals(new Rect(188, 9, 28, 32)), "Existing mod tab gets the first slot");
        neighbors.add(second);
        Rect third = find(inventory, 320, 240, neighbors);
        require(third != null && neighbors.stream().noneMatch(third::intersects), "Two neighboring tabs remain untouched");
        require(first.equals(neighbors.getFirst()), "Placement must not move another mod's tab");
        Rect recipeOpen = new Rect(142, 37, 176, 166);
        Rect moved = find(recipeOpen, 400, 240, List.of());
        require(moved.x() - first.x() == recipeOpen.x() - inventory.x(), "Track recipe-book movement");
        require(find(inventory, 320, 240, List.of(new Rect(0, 0, 320, 240))) == null, "Hide when no safe space exists");
        require(!first.intersects(new Rect(first.right(), first.y(), 28, 32)), "Adjacent widgets may share an edge");
        for (int[] viewport : new int[][]{{320, 240}, {427, 241}, {640, 360}, {853, 479}, {1280, 720}})
            for (int percent = 60; percent <= 100; percent++) {
                var classic = LegacyScreenGeometry.create(viewport[0], viewport[1], percent, true);
                var frame = new Rect(classic.left(), classic.top(), classic.width(), classic.height());
                check(frame, viewport, List.of());
                require(classic.top() >= TOP_SPACE, "Large Old window leaves room above");
                require(classic.top() + classic.height() <= viewport[1] - 4, "Old window still fits below");
                var modern = ModernScreenGeometry.create(viewport[0], viewport[1], percent);
                var modernFrame = new Rect(modern.left(), modern.top(), modern.right() - modern.left(), modern.bottom() - modern.top());
                var categories = new ArrayList<Rect>();
                for (int x = modernFrame.x(); x + 28 < modernFrame.right() - 32; x += 32)
                    categories.add(new Rect(x, modernFrame.y() - TOP_SPACE, 28, 32));
                check(modernFrame, viewport, categories);
            }
        System.out.println("PASS: " + checks + " navigation checks: existing tabs, full screens, inventory movement, fixed size and both view geometries at 60-100%.");
    }
    private static void check(Rect frame, int[] viewport, List<Rect> occupied) {
        Rect result = find(frame, viewport[0], viewport[1], occupied);
        require(result != null, "A free corner must be reachable");
        require(result.width() == 28 && result.height() == 32, "Window size must not enlarge the tab");
        require(result.x() >= 0 && result.y() >= 0 && result.right() <= viewport[0] && result.bottom() <= viewport[1], "Tab clips outside screen");
        require(occupied.stream().noneMatch(result::intersects), "Tab overlaps an existing control");
        require(result.y() == frame.y() - TOP_SPACE, "Same relative position on each view");
    }
    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}

package betteradvancements.common.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pixel positions for a compass-shaped achievement map, independent of server tree coordinates. */
public final class OldScreenLayout {
    public record Point(int x, int y) {
        Point move(int dx, int dy) { return new Point(x + dx, y + dy); }
    }

    private OldScreenLayout() { }

    public static Map<String, Point> create(Map<String, String> parents) {
        Map<String, List<String>> children = new HashMap<>();
        parents.keySet().forEach(id -> children.put(id, new ArrayList<>()));
        List<String> roots = new ArrayList<>();
        parents.forEach((id, parent) -> {
            if (parent == null || !parents.containsKey(parent)) roots.add(id);
            else children.get(parent).add(id);
        });
        children.values().forEach(list -> list.sort(String::compareTo));
        List<String> vanillaOrder = List.of("minecraft:story/root", "minecraft:husbandry/root",
            "minecraft:adventure/root", "minecraft:nether/root", "minecraft:end/root");
        roots.sort(Comparator.<String>comparingInt(id -> {
            int index = vanillaOrder.indexOf(id);
            return index < 0 ? vanillaOrder.size() : index;
        }).thenComparing(id -> id));

        Map<String, Point> result = new LinkedHashMap<>();
        int minX = 0, maxX = 0, maxY = 0;
        for (int index = 0; index < roots.size(); index++) {
            String root = roots.get(index);
            Map<String, Point> group = new LinkedHashMap<>();
            group.put(root, new Point(0, 0));
            int[] lanes = {0, 0, 0, 0};
            for (String child : children.get(root)) {
                int direction = 0;
                for (int i = 1; i < 4; i++) if (lanes[i] < lanes[direction]) direction = i;
                int leaves = layoutBranch(child, children, group, direction, 0, lanes[direction]);
                lanes[direction] += leaves + 1;
            }
            int groupMinX = group.values().stream().mapToInt(Point::x).min().orElse(0);
            int groupMaxX = group.values().stream().mapToInt(Point::x).max().orElse(0);
            int groupMinY = group.values().stream().mapToInt(Point::y).min().orElse(0);
            int groupMaxY = group.values().stream().mapToInt(Point::y).max().orElse(0);
            int dx = 0, dy = 0;
            if (index == 1) dx = maxX + 112 - groupMinX;
            else if (index == 2) dx = minX - 112 - groupMaxX;
            else if (index > 2) dy = maxY + 112 - groupMinY;
            for (Map.Entry<String, Point> entry : group.entrySet()) {
                result.put(entry.getKey(), entry.getValue().move(dx, dy));
            }
            minX = Math.min(minX, groupMinX + dx);
            maxX = Math.max(maxX, groupMaxX + dx);
            maxY = Math.max(maxY, groupMaxY + dy);
        }
        return result;
    }

    private static int layoutBranch(String id, Map<String, List<String>> children, Map<String, Point> points,
                                    int direction, int depth, int lane) {
        int count = 0;
        for (String child : children.get(id)) {
            count += layoutBranch(child, children, points, direction, depth + 1, lane + count);
        }
        count = Math.max(1, count);
        int forward = 72 + depth * 64;
        int sideways = 48 + lane * 48 + (count - 1) * 24;
        Point point = switch (direction) {
            case 0 -> new Point(forward, sideways);
            case 1 -> new Point(-sideways, forward);
            case 2 -> new Point(-forward, -sideways);
            default -> new Point(sideways, -forward);
        };
        points.put(id, point);
        return count;
    }

    /** New datapack/mod entries occupy free space without moving any existing achievement. */
    public static void addMissing(Map<String, Point> points, Map<String, String> parents) {
        List<String> missing = parents.keySet().stream().filter(id -> !points.containsKey(id)).sorted().toList();
        while (!missing.isEmpty()) {
            List<String> deferred = new ArrayList<>();
            int before = points.size();
            for (String id : missing) {
                String parent = parents.get(id);
                if (parent != null && parents.containsKey(parent) && !points.containsKey(parent)) {
                    deferred.add(id);
                    continue;
                }
                placeNear(points, id, points.getOrDefault(parent, new Point(0, 0)));
            }
            if (before == points.size()) {
                // Tolerate malformed/cyclic custom data without hanging the screen.
                String id = deferred.removeFirst();
                placeNear(points, id, new Point(0, 0));
            }
            missing = deferred;
        }
    }

    private static void placeNear(Map<String, Point> points, String id, Point parent) {
        for (int radius = 1; ; radius++) {
            for (int side = 0; side < 4; side++) {
                for (int step = -radius; step < radius; step++) {
                    Point candidate = switch (side) {
                        case 0 -> parent.move(radius * 56, step * 56);
                        case 1 -> parent.move(-step * 56, radius * 56);
                        case 2 -> parent.move(-radius * 56, -step * 56);
                        default -> parent.move(step * 56, -radius * 56);
                    };
                    if (points.values().stream().noneMatch(p -> Math.abs(p.x - candidate.x) < 40 && Math.abs(p.y - candidate.y) < 40)) {
                        points.put(id, candidate);
                        return;
                    }
                }
            }
        }
    }

    public static double clampCenter(double center, int minimum, int maximum, int viewport) {
        double half = viewport / 2.0;
        if (maximum - minimum + 64 <= viewport) return (minimum + maximum) / 2.0;
        return Math.max(minimum - 32 + half, Math.min(maximum + 32 - half, center));
    }
}

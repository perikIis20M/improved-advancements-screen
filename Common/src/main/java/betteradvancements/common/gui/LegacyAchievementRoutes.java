package betteradvancements.common.gui;

import java.util.*;

/** Orthogonal classic-style arrows routed around frames, without relocating authored positions. */
public final class LegacyAchievementRoutes {
    private static final int STEP = 12;
    private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
    private record Cell(int x, int y) { }
    private record State(Cell cell, int direction) { }
    private record Search(State state, int cost, int score, long order) { }
    private LegacyAchievementRoutes() { }

    public static Map<String, List<OldScreenLayout.Point>> create(Map<String, OldScreenLayout.Point> points, Map<String, String> parents) {
        Map<String, List<OldScreenLayout.Point>> result = new LinkedHashMap<>();
        Map<Cell, Set<String>> used = new HashMap<>();
        List<String> children = parents.keySet().stream().filter(id -> points.containsKey(id) && points.containsKey(parents.get(id)))
            .sorted(Comparator.<String>comparingInt(id -> distance(points.get(id), points.get(parents.get(id)))).thenComparing(id -> id)).toList();
        int lowX = points.values().stream().mapToInt(OldScreenLayout.Point::x).min().orElse(0) / STEP - 4;
        int highX = points.values().stream().mapToInt(OldScreenLayout.Point::x).max().orElse(0) / STEP + 4;
        int lowY = points.values().stream().mapToInt(OldScreenLayout.Point::y).min().orElse(0) / STEP - 4;
        int highY = points.values().stream().mapToInt(OldScreenLayout.Point::y).max().orElse(0) / STEP + 4;
        for (String child : children) {
            String parent = parents.get(child);
            Set<Cell> blocked = new HashSet<>();
            points.forEach((id, point) -> {
                if (id.equals(child) || id.equals(parent)) return;
                Cell center = cell(point);
                for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++)
                    blocked.add(new Cell(center.x() + dx, center.y() + dy));
            });
            List<Cell> path = search(cell(points.get(child)), cell(points.get(parent)), parent, blocked, used, lowX, highX, lowY, highY);
            for (Cell c : path) used.computeIfAbsent(c, unused -> new HashSet<>()).add(parent);
            result.put(child, simplify(path));
        }
        return result;
    }

    private static int distance(OldScreenLayout.Point a, OldScreenLayout.Point b) {
        return Math.abs(a.x() - b.x()) + Math.abs(a.y() - b.y());
    }
    private static Cell cell(OldScreenLayout.Point point) {
        return new Cell(Math.floorDiv(point.x(), STEP), Math.floorDiv(point.y(), STEP));
    }

    private static List<Cell> search(Cell start, Cell goal, String parent, Set<Cell> blocked, Map<Cell, Set<String>> used,
                                     int lowX, int highX, int lowY, int highY) {
        var queue = new PriorityQueue<Search>(Comparator.comparingInt(Search::score).thenComparingLong(Search::order));
        Map<State, Integer> costs = new HashMap<>();
        Map<State, State> previous = new HashMap<>();
        State initial = new State(start, -1);
        costs.put(initial, 0);
        long order = 0;
        queue.add(new Search(initial, 0, 0, order++));
        while (!queue.isEmpty()) {
            Search next = queue.remove();
            State current = next.state();
            if (next.cost() != costs.getOrDefault(current, Integer.MAX_VALUE)) continue;
            if (current.cell().equals(goal)) {
                List<Cell> path = new ArrayList<>();
                for (State state = current; state != null; state = previous.get(state)) path.add(state.cell());
                Collections.reverse(path);
                return path;
            }
            for (int direction = 0; direction < DIRECTIONS.length; direction++) {
                int fromStart = Math.abs(current.cell().x() - start.x()) + Math.abs(current.cell().y() - start.y());
                if (current.direction() >= 0 && fromStart < 2 && direction != current.direction()) continue;
                Cell target = new Cell(current.cell().x() + DIRECTIONS[direction][0], current.cell().y() + DIRECTIONS[direction][1]);
                if (target.x() < lowX || target.x() > highX || target.y() < lowY || target.y() > highY || blocked.contains(target)) continue;
                if (Math.abs(target.x() - goal.x()) + Math.abs(target.y() - goal.y()) == 1
                    && (target.x() + DIRECTIONS[direction][0] != goal.x() || target.y() + DIRECTIONS[direction][1] != goal.y())) continue;
                Set<String> owners = used.getOrDefault(target, Set.of());
                int crossing = owners.isEmpty() || owners.contains(parent) || target.equals(goal) ? 0 : 24;
                int turning = current.direction() < 0 || current.direction() == direction ? 0 : 14;
                int cost = next.cost() + 10 + turning + crossing;
                State candidate = new State(target, direction);
                if (cost >= costs.getOrDefault(candidate, Integer.MAX_VALUE)) continue;
                costs.put(candidate, cost);
                previous.put(candidate, current);
                int heuristic = 10 * (Math.abs(target.x() - goal.x()) + Math.abs(target.y() - goal.y()));
                queue.add(new Search(candidate, cost, cost + heuristic, order++));
            }
        }
        // Do not draw a misleading line through frames if custom data blocks every route.
        return List.of();
    }

    private static List<OldScreenLayout.Point> simplify(List<Cell> cells) {
        List<OldScreenLayout.Point> result = new ArrayList<>();
        for (int i = 0; i < cells.size(); i++) {
            Cell p = cells.get(i);
            if (i > 0 && i + 1 < cells.size()) {
                Cell before = cells.get(i - 1), after = cells.get(i + 1);
                if ((before.x() == p.x() && after.x() == p.x()) || (before.y() == p.y() && after.y() == p.y())) continue;
            }
            result.add(new OldScreenLayout.Point(p.x() * STEP, p.y() * STEP));
        }
        return List.copyOf(result);
    }
}

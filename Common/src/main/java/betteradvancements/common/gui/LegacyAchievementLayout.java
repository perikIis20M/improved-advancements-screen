package betteradvancements.common.gui;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** One authored map. Its presentation prerequisites do not replace the server's completion triggers. */
public final class LegacyAchievementLayout {
    public static final String ROOT = "minecraft:story/root";
    private record Entry(OldScreenLayout.Point point, String parent) { }
    private static final Map<String, Entry> MAP = readMap();
    private LegacyAchievementLayout() { }

    private static Map<String, Entry> readMap() {
        try (var input = LegacyAchievementLayout.class.getResourceAsStream("/assets/betteradvancements/old_screen_map.json")) {
            if (input == null) throw new IllegalStateException("Missing authored achievement map");
            try (var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                Map<String, Entry> entries = new LinkedHashMap<>();
                JsonParser.parseReader(reader).getAsJsonObject().entrySet().forEach(item -> {
                    var value = item.getValue().getAsJsonObject();
                    entries.put(item.getKey(), new Entry(new OldScreenLayout.Point(value.get("x").getAsInt(), value.get("y").getAsInt()),
                        value.get("parent").isJsonNull() ? null : value.get("parent").getAsString()));
                });
                return Collections.unmodifiableMap(entries);
            }
        } catch (Exception error) {
            throw new IllegalStateException("Could not read the authored achievement map", error);
        }
    }

    public static Map<String, OldScreenLayout.Point> create(Map<String, String> graph) {
        Map<String, OldScreenLayout.Point> points = new LinkedHashMap<>();
        MAP.forEach((id, entry) -> points.put(id, entry.point()));
        addMissing(points, parents(graph));
        return points;
    }

    /** Complete discovery graph, even when the server has sent only a subset of its goals. */
    public static Map<String, String> parents(Map<String, String> serverGraph) {
        Map<String, String> result = new LinkedHashMap<>();
        MAP.forEach((id, entry) -> result.put(id, entry.parent()));
        serverGraph.keySet().stream().sorted().filter(id -> !result.containsKey(id)).forEach(id ->
            result.put(id, serverGraph.get(id) == null ? ROOT : serverGraph.get(id)));
        for (String id : new ArrayList<>(result.keySet())) {
            Set<String> visited = new HashSet<>();
            String current = id;
            while (current != null) {
                if (!visited.add(current)) { result.put(id, ROOT); break; }
                current = result.get(current);
            }
        }
        return result;
    }

    public static void addMissing(Map<String, OldScreenLayout.Point> points, Map<String, String> graph) {
        var pending = new TreeSet<String>();
        graph.keySet().stream().filter(id -> !points.containsKey(id)).forEach(pending::add);
        while (!pending.isEmpty()) {
            String id = pending.stream().filter(key -> !pending.contains(graph.get(key))).findFirst().orElse(pending.first());
            var anchor = points.getOrDefault(graph.get(id), points.getOrDefault(ROOT, new OldScreenLayout.Point(0, 0)));
            points.put(id, freePoint(anchor, points.values()));
            pending.remove(id);
        }
    }

    private static OldScreenLayout.Point freePoint(OldScreenLayout.Point anchor, Collection<OldScreenLayout.Point> points) {
        for (int distance = 1; distance < points.size() + 16; distance++) {
            for (int dx = -distance; dx <= distance; dx++) {
                int dy = distance - Math.abs(dx);
                for (int sign : new int[]{1, -1}) {
                    var candidate = new OldScreenLayout.Point(anchor.x() + dx * 48, anchor.y() + dy * 48 * sign);
                    if (candidate.y() < -120 || candidate.y() > 360) continue;
                    if (points.stream().noneMatch(p -> Math.abs(p.x() - candidate.x()) < 36 && Math.abs(p.y() - candidate.y()) < 36))
                        return candidate;
                }
            }
        }
        throw new IllegalStateException("Could not extend the achievement map");
    }
}

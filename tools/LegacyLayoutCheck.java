import betteradvancements.common.gui.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** Authored topology, discovery and actual routed-frame geometry checks. */
public final class LegacyLayoutCheck {
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]);
        Map<String, String> vanilla = new LinkedHashMap<>();
        try (var reader = Files.newBufferedReader(root.resolve("Common/src/main/resources/assets/betteradvancements/old_screen_vanilla.json"))) {
            JsonParser.parseReader(reader).getAsJsonObject().entrySet().forEach(e ->
                vanilla.put(e.getKey(), e.getValue().isJsonNull() ? null : e.getValue().getAsString()));
        }
        var positions = LegacyAchievementLayout.create(vanilla);
        var parents = LegacyAchievementLayout.parents(vanilla);
        require(positions.keySet().equals(vanilla.keySet()), "Missing/extra vanilla advancements");
        require(positions.get(LegacyAchievementLayout.ROOT).equals(new OldScreenLayout.Point(0, 0)), "Opening root moved");
        require(parents.values().stream().filter(Objects::isNull).count() == 1, "Map still has independent category roots");
        int bridges = 0;
        for (String id : parents.keySet()) {
            String current = id;
            Set<String> visited = new HashSet<>();
            while (current != null) {
                require(visited.add(current), "Cycle in authored path: " + id);
                require(parents.containsKey(current), "Missing prerequisite: " + current);
                if (parents.get(current) == null) require(current.equals(LegacyAchievementLayout.ROOT), "Disconnected map");
                current = parents.get(current);
            }
            String parent = parents.get(id);
            if (parent != null && !id.split("/")[0].equals(parent.split("/")[0])) bridges++;
        }
        require(bridges >= 20, "Category trees were only joined at their roots");
        var ids = new ArrayList<>(positions.keySet());
        for (int i = 0; i < ids.size(); i++) for (int j = 0; j < i; j++) {
            var a = positions.get(ids.get(i)); var b = positions.get(ids.get(j));
            require(Math.abs(a.x() - b.x()) >= 30 || Math.abs(a.y() - b.y()) >= 30,
                "Overlapping/too-close frames: " + ids.get(i) + " and " + ids.get(j));
        }
        long start = System.nanoTime();
        var routes = LegacyAchievementRoutes.create(positions, parents);
        long routeMillis = (System.nanoTime() - start) / 1_000_000;
        for (var entry : parents.entrySet()) {
            String child = entry.getKey(), parent = entry.getValue();
            if (parent == null) continue;
            var path = routes.get(child);
            require(path != null && path.size() >= 2, "No route for " + child);
            require(path.getFirst().equals(positions.get(child)) && path.getLast().equals(positions.get(parent)), "Wrong route endpoints");
            require(distance(path.get(0), path.get(1)) >= 24, "Arrow bends inside its frame: " + child);
            for (int i = 1; i < path.size(); i++) {
                var a = path.get(i - 1); var b = path.get(i);
                require((a.x() == b.x()) != (a.y() == b.y()), "Non-orthogonal/empty link");
                for (var other : positions.entrySet()) {
                    if (other.getKey().equals(child) || other.getKey().equals(parent)) continue;
                    var p = other.getValue();
                    boolean hit = a.y() == b.y()
                        ? Math.abs(a.y() - p.y()) <= 13 && Math.min(a.x(), b.x()) < p.x() + 13 && Math.max(a.x(), b.x()) > p.x() - 13
                        : Math.abs(a.x() - p.x()) <= 13 && Math.min(a.y(), b.y()) < p.y() + 13 && Math.max(a.y(), b.y()) > p.y() - 13;
                    require(!hit, child + " route crosses " + other.getKey());
                }
            }
        }
        for (int seed = 0; seed < 30; seed++) {
            Collections.shuffle(ids, new Random(seed));
            var partial = new LinkedHashMap<String, String>();
            ids.subList(0, 1 + seed * 3).forEach(id -> partial.put(id, vanilla.get(id)));
            require(positions.equals(LegacyAchievementLayout.create(partial)), "Packet/world progress moved a node");
            require(parents.equals(LegacyAchievementLayout.parents(partial)), "Partial data broke discovery");
        }
        Set<String> completed = new HashSet<>();
        require(OldScreenVisibility.canUnlock(LegacyAchievementLayout.ROOT, parents, completed), "Root not available");
        require(parents.keySet().stream().filter(id -> OldScreenVisibility.canUnlock(id, parents, completed)).count() == 1,
            "Multiple categories available before the start");
        require(OldScreenVisibility.unlockDepth("minecraft:end/elytra", parents, completed) > 4, "Late game exposed immediately");
        for (String id : List.of("minecraft:story/enter_the_nether", "minecraft:nether/obtain_blaze_rod", "minecraft:end/kill_dragon")) {
            parents.forEach((child, parent) -> {
                if (!id.equals(parent)) return;
                require(!OldScreenVisibility.canUnlock(child, parents, Set.of()), "Gate exposed without prerequisite");
                require(OldScreenVisibility.canUnlock(child, parents, Set.of(id)), "Gate did not unlock child");
            });
        }
        completed.addAll(parents.keySet());
        for (String id : parents.keySet()) require(OldScreenVisibility.unlockDepth(id, parents, completed) == 0
            && OldScreenVisibility.canUnlock(id, parents, completed), "Completed map is not fully readable");
        var custom = new LinkedHashMap<>(vanilla);
        custom.put("example:a", "example:b"); custom.put("example:b", "example:a");
        var customParents = LegacyAchievementLayout.parents(custom);
        var expanded = LegacyAchievementLayout.create(custom);
        positions.forEach((id, point) -> require(point.equals(expanded.get(id)), "Custom map moved vanilla"));
        require(expanded.size() == 128 && customParents.size() == 128, "Custom cycle handling");
        Path output = root.resolve("build/legacy-layout-check/map-data.json");
        Files.createDirectories(output.getParent());
        var exported = new JsonObject();
        exported.add("positions", new Gson().toJsonTree(positions));
        exported.add("parents", new Gson().toJsonTree(parents));
        exported.add("routes", new Gson().toJsonTree(routes));
        Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(exported));
        System.out.println("PASS: 126 authored nodes, ONE connected root, " + bridges + " links across former categories, 125 unobstructed routes (" + routeMillis + " ms), 30 partial/world orders, original reveal gates and custom-cycle safety.");
    }
    private static int distance(OldScreenLayout.Point a, OldScreenLayout.Point b) { return Math.abs(a.x() - b.x()) + Math.abs(a.y() - b.y()); }
}

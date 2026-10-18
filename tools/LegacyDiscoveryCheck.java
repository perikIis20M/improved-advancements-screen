import betteradvancements.common.gui.*;
import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Discovery checks against the actual shipped map and display catalogue. */
public final class LegacyDiscoveryCheck {
    public static void main(String[] args) throws Exception {
        var parents = LegacyAchievementLayout.parents(Map.of());
        Set<String> hidden = new HashSet<>();
        try (var reader = new InputStreamReader(LegacyAchievementLayout.class.getResourceAsStream(
                "/assets/betteradvancements/old_screen_titles.json"), StandardCharsets.UTF_8)) {
            JsonParser.parseReader(reader).getAsJsonObject().entrySet().forEach(entry -> {
                var display = entry.getValue().getAsJsonObject();
                if (display.has("hidden") && display.get("hidden").getAsBoolean()) hidden.add(entry.getKey());
            });
        }
        int limit = OldScreenVisibility.DEFAULT_REVEAL_DEPTH;
        Set<String> completed = new HashSet<>();
        var fresh = visible(parents, hidden, completed, limit);
        require(fresh.size() == 15, "Fresh start must preview just 15 of 126 goals");
        require(!fresh.contains("minecraft:story/enter_the_nether") && !fresh.contains("minecraft:end/elytra"),
            "Late milestones leaked into the starting view");
        var previous = fresh;
        List<Integer> counts = new ArrayList<>(List.of(fresh.size()));
        for (String id : List.of("minecraft:story/root", "minecraft:story/mine_stone", "minecraft:story/upgrade_tools")) {
            completed.add(id);
            var next = visible(parents, hidden, completed, limit);
            require(next.containsAll(previous) && next.size() > previous.size(), "Progress did not expand discovery");
            require(next.size() < parents.size(), "Early progress revealed the whole map");
            require(Collections.disjoint(next, hidden), "Uncompleted secret exposed");
            previous = next;
            counts.add(next.size());
        }
        completed.addAll(parents.keySet());
        require(visible(parents, hidden, completed, limit).equals(parents.keySet()), "Earned goals disappeared");
        completed.clear();
        require(visible(parents, hidden, completed, limit).equals(fresh), "Reset/world change kept old discovery");
        completed.add("minecraft:end/elytra");
        require(visible(parents, hidden, completed, limit).contains("minecraft:end/elytra"), "Isolated earned goal hidden");
        for (String id : hidden) require(visible(parents, hidden, Set.of(id), limit).contains(id), "Earned secret hidden");
        require(visible(parents, hidden, Set.of(), 0).equals(Set.of(LegacyAchievementLayout.ROOT)), "Zero preview exposed locked goals");
        require(visible(parents, hidden, Set.of(), 4).size() == 51, "Original distance option changed");
        require(visible(parents, hidden, Set.of(), -99).equals(visible(parents, hidden, Set.of(), 0)), "Low config bound");
        require(visible(parents, hidden, Set.of(), 99).equals(visible(parents, hidden, Set.of(), 4)), "High config bound");
        for (int depth = 0; depth <= 5; depth++) for (boolean secret : List.of(false, true))
            for (boolean done : List.of(false, true))
                require(OldScreenVisibility.isVisible(depth, secret, done, 4) == OldScreenVisibility.isVisible(depth, secret, done),
                    "Original visibility compatibility changed");
        System.out.println("PASS: default progress reveals " + counts + " of 126 goals; full/reset, earned secrets, isolated completion and config 0-4 passed.");
    }

    private static Set<String> visible(Map<String, String> parents, Set<String> hidden, Set<String> completed, int limit) {
        Set<String> result = new HashSet<>();
        for (String id : parents.keySet()) {
            int depth = OldScreenVisibility.unlockDepth(id, parents, completed);
            if (OldScreenVisibility.isVisible(depth, hidden.contains(id), completed.contains(id), limit)) result.add(id);
        }
        return result;
    }

    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}

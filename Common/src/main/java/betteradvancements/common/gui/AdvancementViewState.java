package betteradvancements.common.gui;

import betteradvancements.common.platform.Services;
import betteradvancements.common.reference.Constants;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/** Local view state, keyed by advancement ID so it survives reconnecting to a world. */
public final class AdvancementViewState {
    public static final Map<String, Pair<Integer, Integer>> scrollByTab = new LinkedHashMap<>();
    public static final Map<String, Map<String, Pair<Integer, Integer>>> positionsByLayout = new LinkedHashMap<>();
    public static String selectedTab;
    public static boolean hasOldScreenPosition;
    public static double oldScreenCenterX, oldScreenCenterY;
    public static int oldScreenZoom = 100;
    public static boolean hasLegacyScreenPosition;
    public static double legacyScreenScrollX, legacyScreenScrollY;
    public static float legacyScreenZoom = 1.0F;

    private AdvancementViewState() { }

    private static Path file() {
        return Services.PLATFORM.getConfigDirectory().resolve("improved-advancements-view.json");
    }

    public static void load() {
        scrollByTab.clear();
        positionsByLayout.clear();
        try {
            if (!Files.exists(file())) return;
            try (Reader reader = Files.newBufferedReader(file())) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                selectedTab = root.has("selectedTab") ? root.get("selectedTab").getAsString() : null;
                hasLegacyScreenPosition = root.has("legacyScreenScrollX") && root.has("legacyScreenScrollY");
                if (hasLegacyScreenPosition) {
                    legacyScreenScrollX = root.get("legacyScreenScrollX").getAsDouble();
                    legacyScreenScrollY = root.get("legacyScreenScrollY").getAsDouble();
                    hasLegacyScreenPosition = Double.isFinite(legacyScreenScrollX) && Double.isFinite(legacyScreenScrollY);
                }
                if (root.has("legacyScreenZoom")) {
                    float savedZoom = root.get("legacyScreenZoom").getAsFloat();
                    legacyScreenZoom = Float.isFinite(savedZoom) ? Math.max(1.0F, Math.min(2.0F, savedZoom)) : 1.0F;
                }
                if (root.has("oldScreenZoom")) oldScreenZoom = Math.max(OldScreenCamera.MIN_ZOOM,
                    Math.min(OldScreenCamera.MAX_ZOOM, root.get("oldScreenZoom").getAsInt()));
                hasOldScreenPosition = root.has("oldScreenCenterX") && root.has("oldScreenCenterY");
                if (hasOldScreenPosition) {
                    oldScreenCenterX = root.get("oldScreenCenterX").getAsDouble();
                    oldScreenCenterY = root.get("oldScreenCenterY").getAsDouble();
                    hasOldScreenPosition = Double.isFinite(oldScreenCenterX) && Double.isFinite(oldScreenCenterY);
                }
                if (root.has("tabs")) {
                    root.getAsJsonObject("tabs").entrySet().forEach(entry -> {
                        try {
                            JsonObject point = entry.getValue().getAsJsonObject();
                            scrollByTab.put(entry.getKey(), Pair.of(point.get("x").getAsInt(), point.get("y").getAsInt()));
                        } catch (RuntimeException ignored) {
                            // One old or malformed tab entry must not discard the others.
                        }
                    });
                }
                if (root.has("layouts")) {
                    root.getAsJsonObject("layouts").entrySet().forEach(layout -> {
                        try {
                            Map<String, Pair<Integer, Integer>> positions = new LinkedHashMap<>();
                            layout.getValue().getAsJsonObject().entrySet().forEach(entry -> {
                                JsonObject point = entry.getValue().getAsJsonObject();
                                positions.put(entry.getKey(), Pair.of(point.get("x").getAsInt(), point.get("y").getAsInt()));
                            });
                            positionsByLayout.put(layout.getKey(), positions);
                        } catch (RuntimeException ignored) {
                            // Keep other saved layouts if one entry is invalid.
                        }
                    });
                }
            }
        } catch (Exception e) {
            Constants.log.warn("Could not read saved advancement view", e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(file().getParent());
            JsonObject root = new JsonObject();
            if (selectedTab != null) root.addProperty("selectedTab", selectedTab);
            root.addProperty("legacyScreenZoom", legacyScreenZoom);
            if (hasLegacyScreenPosition) {
                root.addProperty("legacyScreenScrollX", legacyScreenScrollX);
                root.addProperty("legacyScreenScrollY", legacyScreenScrollY);
            }
            root.addProperty("oldScreenZoom", oldScreenZoom);
            if (hasOldScreenPosition) {
                root.addProperty("oldScreenCenterX", oldScreenCenterX);
                root.addProperty("oldScreenCenterY", oldScreenCenterY);
            }
            JsonObject tabs = new JsonObject();
            scrollByTab.forEach((id, point) -> {
                JsonObject value = new JsonObject();
                value.addProperty("x", point.getFirst());
                value.addProperty("y", point.getSecond());
                tabs.add(id, value);
            });
            root.add("tabs", tabs);
            JsonObject layouts = new JsonObject();
            positionsByLayout.forEach((key, positions) -> {
                JsonObject layout = new JsonObject();
                positions.forEach((id, point) -> {
                    JsonObject value = new JsonObject();
                    value.addProperty("x", point.getFirst());
                    value.addProperty("y", point.getSecond());
                    layout.add(id, value);
                });
                layouts.add(key, layout);
            });
            root.add("layouts", layouts);
            Path temporary = file().resolveSibling(file().getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
            }
            Files.move(temporary, file(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            Constants.log.warn("Could not save advancement view", e);
        }
    }
}

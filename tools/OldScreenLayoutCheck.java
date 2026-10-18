import betteradvancements.common.gui.OldScreenLayout;
import com.google.gson.JsonParser;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import javax.imageio.ImageIO;

/** Standalone regression checks using the actual 26.3 advancement graph. */
public class OldScreenLayoutCheck {
    public static void main(String[] args) throws Exception {
        Map<String, String> graph = new LinkedHashMap<>();
        try (var reader = Files.newBufferedReader(Path.of(args[0]))) {
            JsonParser.parseReader(reader).getAsJsonObject().entrySet().forEach(e ->
                graph.put(e.getKey(), e.getValue().isJsonNull() ? null : e.getValue().getAsString()));
        }
        var reference = OldScreenLayout.create(graph);
        require(reference.size() == graph.size(), "Every vanilla advancement must receive a position");
        require(reference.containsKey("minecraft:story/root"), "Story root must be present");
        noOverlaps(reference);
        var ids = new ArrayList<>(graph.keySet());
        for (int seed = 0; seed < 30; seed++) {
            Collections.shuffle(ids, new Random(seed));
            Map<String, String> shuffled = new LinkedHashMap<>();
            ids.forEach(id -> shuffled.put(id, graph.get(id)));
            require(reference.equals(OldScreenLayout.create(shuffled)), "Packet order must not change positions");
            Map<String, OldScreenLayout.Point> partialWorld = new LinkedHashMap<>(reference);
            Map<String, String> visible = new LinkedHashMap<>();
            ids.subList(0, 1 + seed * 3).forEach(id -> visible.put(id, graph.get(id)));
            OldScreenLayout.addMissing(partialWorld, visible);
            require(reference.equals(partialWorld), "World progress must not change vanilla positions");
        }
        Map<String, OldScreenLayout.Point> expanded = new LinkedHashMap<>(reference);
        Map<String, String> additions = new LinkedHashMap<>();
        additions.put("example:root", null);
        for (int i = 0; i < 30; i++) {
            var previous = Map.copyOf(expanded);
            additions.put("example:child_" + i, i == 0 ? "example:root" : "example:child_" + (i - 1));
            OldScreenLayout.addMissing(expanded, additions);
            previous.forEach((id, point) -> require(point.equals(expanded.get(id)), "Revealing custom advancements moved an existing icon"));
        }
        noOverlaps(expanded);
        Map<String, String> cycle = Map.of("cycle:a", "cycle:b", "cycle:b", "cycle:a");
        OldScreenLayout.addMissing(expanded, cycle);
        require(expanded.containsKey("cycle:a") && expanded.containsKey("cycle:b"), "Cyclic data must not hang placement");
        for (int viewport : new int[]{72, 174, 320, 640, 1280}) {
            double low = OldScreenLayout.clampCenter(-1e6, -500, 1500, viewport);
            double high = OldScreenLayout.clampCenter(1e6, -500, 1500, viewport);
            require(Math.abs(low - (-532 + viewport / 2.0)) < 0.001, "Left edge is unreachable");
            require(Math.abs(high - (1532 - viewport / 2.0)) < 0.001, "Right edge is unreachable");
            require(OldScreenLayout.clampCenter(500, -500, 1500, viewport) == 500, "Resizing moved an unclamped camera");
        }
        require(OldScreenLayout.clampCenter(900, 0, 26, 320) == 13, "Small maps must be centered");
        renderPreview(reference, graph, Path.of(args[1]));
        System.out.println("PASS: " + graph.size() + " vanilla nodes, no overlaps, 30 packet orders/world subsets, incremental custom nodes, cyclic-data fallback, and viewport bounds.");
    }

    private static void noOverlaps(Map<String, OldScreenLayout.Point> positions) {
        var ids = new ArrayList<>(positions.keySet());
        for (int i = 0; i < ids.size(); i++) for (int j = i + 1; j < ids.size(); j++) {
            var a = positions.get(ids.get(i)); var b = positions.get(ids.get(j));
            require(Math.abs(a.x() - b.x()) >= 29 || Math.abs(a.y() - b.y()) >= 26,
                "Overlapping icons: " + ids.get(i) + " and " + ids.get(j));
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void renderPreview(Map<String, OldScreenLayout.Point> positions, Map<String, String> graph, Path file) throws Exception {
        int minX = positions.values().stream().mapToInt(OldScreenLayout.Point::x).min().orElse(0) - 30;
        int minY = positions.values().stream().mapToInt(OldScreenLayout.Point::y).min().orElse(0) - 30;
        int maxX = positions.values().stream().mapToInt(OldScreenLayout.Point::x).max().orElse(0) + 60;
        int maxY = positions.values().stream().mapToInt(OldScreenLayout.Point::y).max().orElse(0) + 60;
        var image = new BufferedImage(maxX - minX, maxY - minY, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(new Color(35, 35, 35)); graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.translate(-minX, -minY);
        graph.forEach((id, parent) -> {
            var a = positions.get(parent); var b = positions.get(id);
            if (a == null || b == null) return;
            graphics.setColor(Color.GRAY);
            boolean horizontal = Math.abs(b.x() - a.x()) >= Math.abs(b.y() - a.y());
            int elbowX = horizontal ? a.x() : b.x(), elbowY = horizontal ? b.y() : a.y();
            graphics.drawLine(a.x() + 16, a.y() + 13, elbowX + 16, elbowY + 13);
            graphics.drawLine(elbowX + 16, elbowY + 13, b.x() + 16, b.y() + 13);
        });
        positions.forEach((id, p) -> {
            graphics.setColor(id.endsWith("/root") ? Color.ORANGE : Color.LIGHT_GRAY);
            graphics.fillRect(p.x() + 3, p.y(), 26, 26);
            graphics.setColor(Color.DARK_GRAY); graphics.fillRect(p.x() + 6, p.y() + 3, 20, 20);
            if (id.endsWith("/root")) {
                graphics.setColor(Color.WHITE); graphics.drawString(id, p.x() + 34, p.y() + 16);
            }
        });
        graphics.dispose();
        Files.createDirectories(file.getParent());
        ImageIO.write(image, "png", file.toFile());
    }
}

package betteradvancements.common.gui;

import betteradvancements.common.reference.Constants;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.List;

/** Classic achievement canvas. Its coordinates never modify the normal advancement tabs. */
final class OldAdvancementsView {
    private static final String LAYOUT_KEY = "old-screen-v1";
    private static final Map<String, OldScreenLayout.Point> VANILLA = loadVanilla();
    private static final Identifier[] TERRAIN = {texture("sand"), texture("dirt"), texture("stone"),
        texture("coal_ore"), texture("iron_ore"), texture("diamond_ore"), texture("redstone_ore"), texture("bedrock")};
    private final Map<String, OldScreenLayout.Point> positions = new LinkedHashMap<>(VANILLA);
    private final Map<String, BetterAdvancementWidget> widgets = new LinkedHashMap<>();
    private final Map<String, String> parents = new LinkedHashMap<>();
    private final Map<String, Integer> depths = new LinkedHashMap<>();
    private final OldScreenCamera camera = new OldScreenCamera();
    private boolean positioned;
    private int minX, minY, maxX, maxY;
    private final int terrainSeed = Minecraft.getInstance().getUser().getProfileId().toString().hashCode();

    OldAdvancementsView() {
        Map<String, Pair<Integer, Integer>> saved = AdvancementViewState.positionsByLayout.get(LAYOUT_KEY);
        if (saved != null) saved.forEach((id, p) -> positions.putIfAbsent(id, new OldScreenLayout.Point(p.getFirst(), p.getSecond())));
        if (BetterAdvancementsScreen.rememberPosition) camera.setZoomPercent(AdvancementViewState.oldScreenZoom);
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath("betteradvancements", "textures/classic/" + name + ".png");
    }

    private static Map<String, OldScreenLayout.Point> loadVanilla() {
        Map<String, String> parents = new LinkedHashMap<>();
        try (var stream = OldAdvancementsView.class.getResourceAsStream("/assets/betteradvancements/old_screen_vanilla.json")) {
            if (stream == null) throw new IllegalStateException("Missing old screen layout data");
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonParser.parseReader(reader).getAsJsonObject().entrySet().forEach(entry ->
                    parents.put(entry.getKey(), entry.getValue().isJsonNull() ? null : entry.getValue().getAsString()));
            }
            return OldScreenLayout.create(parents);
        } catch (Exception e) {
            Constants.log.warn("Could not load classic advancement layout", e);
            return Map.of();
        }
    }

    void rebuild(Collection<BetterAdvancementTab> tabs) {
        widgets.clear();
        parents.clear();
        depths.clear();
        Set<String> completed = new HashSet<>();
        for (BetterAdvancementTab tab : tabs) {
            tab.widgets.forEach((holder, widget) -> {
                String id = holder.id().toString();
                widgets.put(id, widget);
                if (widget.isCompleted()) completed.add(id);
                AdvancementNode parent = widget.getAdvancement().parent();
                while (parent != null && parent.advancement().display().isEmpty()) parent = parent.parent();
                parents.put(id, parent == null ? null : parent.holder().id().toString());
            });
        }
        OldScreenLayout.addMissing(positions, parents);
        widgets.forEach((id, widget) -> {
            int depth = OldScreenVisibility.unlockDepth(id, parents, completed);
            if (widget.isVisibleOnMap() && depth <= OldScreenVisibility.MAX_DEPTH) depths.put(id, depth);
        });
        minX = minY = Integer.MAX_VALUE;
        maxX = maxY = Integer.MIN_VALUE;
        depths.keySet().forEach(id -> {
            var point = positions.get(id);
            minX = Math.min(minX, point.x());
            minY = Math.min(minY, point.y());
            maxX = Math.max(maxX, point.x() + 29);
            maxY = Math.max(maxY, point.y() + 26);
        });
        if (depths.isEmpty()) minX = minY = maxX = maxY = 0;
    }

    void prepare(int width, int height) {
        if (!positioned && !depths.isEmpty()) {
            var first = positions.get(depths.containsKey("minecraft:story/root") ? "minecraft:story/root" : depths.keySet().iterator().next());
            camera.centerOn(first.x() + 16, first.y() + 13);
            if (BetterAdvancementsScreen.rememberPosition && AdvancementViewState.hasOldScreenPosition) {
                camera.centerOn(AdvancementViewState.oldScreenCenterX, AdvancementViewState.oldScreenCenterY);
                boolean anythingInView = depths.keySet().stream().map(positions::get).anyMatch(p ->
                    camera.screenX(p.x() + 16, width) >= 0 && camera.screenX(p.x() + 16, width) < width &&
                    camera.screenY(p.y() + 13, height) >= 0 && camera.screenY(p.y() + 13, height) < height);
                if (!anythingInView) camera.centerOn(first.x() + 16, first.y() + 13);
            }
            positioned = true;
        }
        if (!depths.isEmpty()) camera.clamp(minX, minY, maxX, maxY, width, height);
    }

    void pan(double dx, double dy, int width, int height) {
        prepare(width, height);
        camera.pan(dx, dy);
        prepare(width, height);
    }

    int zoomPercent() { return camera.zoomPercent(); }
    boolean canZoomIn() { return camera.canZoomIn(); }
    boolean canZoomOut() { return camera.canZoomOut(); }

    void changeZoom(int steps, int width, int height) {
        prepare(width, height);
        camera.changeZoom(steps);
        prepare(width, height);
    }

    void store() {
        Map<String, Pair<Integer, Integer>> saved = new LinkedHashMap<>();
        positions.forEach((id, point) -> { if (!VANILLA.containsKey(id)) saved.put(id, Pair.of(point.x(), point.y())); });
        AdvancementViewState.positionsByLayout.put(LAYOUT_KEY, saved);
        if (positioned && BetterAdvancementsScreen.rememberPosition) {
            AdvancementViewState.hasOldScreenPosition = true;
            AdvancementViewState.oldScreenCenterX = camera.centerX();
            AdvancementViewState.oldScreenCenterY = camera.centerY();
            AdvancementViewState.oldScreenZoom = camera.zoomPercent();
        }
    }

    void draw(GuiGraphicsExtractor graphics, int left, int top, int width, int height) {
        prepare(width, height);
        graphics.enableScissor(left, top, left + width, top + height);
        drawTerrain(graphics, left, top, width, height);
        graphics.pose().pushMatrix();
        graphics.pose().translate(left + width / 2.0F, top + height / 2.0F);
        graphics.pose().scale(camera.scale());
        graphics.pose().translate((float) -camera.centerX(), (float) -camera.centerY());
        for (var entry : depths.entrySet()) {
            BetterAdvancementWidget widget = widgets.get(entry.getKey());
            if (widget.betterDisplayInfo.hideLines()) continue;
            String parent = parents.get(entry.getKey());
            if (parent == null || !depths.containsKey(parent)) continue;
            var a = positions.get(parent);
            var b = positions.get(entry.getKey());
            drawConnection(graphics, a.x() + 16, a.y() + 13, b.x() + 16, b.y() + 13,
                widget.isCompleted() ? 0xFFB0B0B0 : entry.getValue() == 0 ? 0xFF50A040 : 0xFF444444);
        }
        for (var entry : depths.entrySet()) {
            var p = positions.get(entry.getKey());
            if (intersectsViewport(p, width, height)) widgets.get(entry.getKey()).drawOldIcon(graphics, p.x(), p.y(), entry.getValue());
        }
        // Item render states must be submitted before these overlays, otherwise locked icons stay bright.
        graphics.nextStratum();
        for (var entry : depths.entrySet()) {
            var p = positions.get(entry.getKey());
            if (entry.getValue() > 0 && intersectsViewport(p, width, height)) {
                int alpha = switch (entry.getValue()) { case 1 -> 65; case 2 -> 95; case 3 -> 130; default -> 175; };
                graphics.fill(p.x() + 8, p.y() + 5, p.x() + 24, p.y() + 21, alpha << 24);
            }
        }
        graphics.pose().popMatrix();
        graphics.disableScissor();
    }

    void drawTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int left, int top, int width, int height) {
        if (mouseX < left || mouseY < top || mouseX >= left + width || mouseY >= top + height) return;
        double wx = camera.worldX(mouseX - left, width), wy = camera.worldY(mouseY - top, height);
        for (var entry : depths.entrySet()) {
            if (!OldScreenVisibility.hasTooltip(entry.getValue())) continue;
            var p = positions.get(entry.getKey());
            BetterAdvancementWidget widget = widgets.get(entry.getKey());
            if (wx >= p.x() + 3 && wx < p.x() + 29 && wy >= p.y() && wy < p.y() + 26) {
                if (entry.getValue() == 0) {
                    widget.drawOldHover(graphics, left + camera.screenX(p.x(), width),
                        top + camera.screenY(p.y(), height), camera.scale(), left, top, width, height);
                } else {
                    Component title = entry.getValue() == 3 ? Component.translatable("improvedadvancements.unknown")
                        : widget.getAdvancement().advancement().display().orElseThrow().title();
                    BetterAdvancementWidget parent = widgets.get(parents.get(entry.getKey()));
                    Component prerequisite = parent == null ? Component.empty()
                        : parent.getAdvancement().advancement().display().orElseThrow().title();
                    widget.drawOldLockedHover(graphics,
                        List.of(title.copy().withStyle(ChatFormatting.GRAY),
                            Component.translatable("improvedadvancements.requires", prerequisite).withStyle(ChatFormatting.DARK_GRAY)),
                        mouseX + 12 * camera.scale(), mouseY, camera.scale(), left, top, width, height);
                }
                return;
            }
        }
    }

    private boolean intersectsViewport(OldScreenLayout.Point p, int width, int height) {
        return camera.screenX(p.x() + 29, width) >= 0 && camera.screenY(p.y() + 26, height) >= 0
            && camera.screenX(p.x(), width) < width && camera.screenY(p.y(), height) < height;
    }

    private void drawTerrain(GuiGraphicsExtractor graphics, int left, int top, int width, int height) {
        int startX = Math.floorDiv((int) Math.floor(camera.worldX(0, width)), 16) * 16;
        int startY = Math.floorDiv((int) Math.floor(camera.worldY(0, height)), 16) * 16;
        double endX = camera.worldX(width, width), endY = camera.worldY(height, height);
        for (int y = startY; y < endY; y += 16) {
            int tileTop = top + (int) Math.round(camera.screenY(y, height));
            int tileBottom = top + (int) Math.round(camera.screenY(y + 16, height));
            int row = Math.floorDiv(y + OldScreenTerrain.COORDINATE_OFFSET, OldScreenTerrain.TILE_SIZE);
            int tint = OldScreenTerrain.tintAt(row);
            for (int x = startX; x < endX; x += 16) {
                int tileLeft = left + (int) Math.round(camera.screenX(x, width));
                int tileRight = left + (int) Math.round(camera.screenX(x + 16, width));
                int column = Math.floorDiv(x + OldScreenTerrain.COORDINATE_OFFSET, OldScreenTerrain.TILE_SIZE);
                int tile = OldScreenTerrain.tileAt(column, row, terrainSeed);
                // Shared rounded edges prevent cracks at fractional zoom/pan. Source UVs always cover
                // one complete 16x16 tile, independently of its scaled destination dimensions.
                graphics.blit(RenderPipelines.GUI_TEXTURED, TERRAIN[tile], tileLeft, tileTop, 0, 0,
                    tileRight - tileLeft, tileBottom - tileTop, 16, 16, 16, 16, tint);
            }
        }
    }

    private static void drawConnection(GuiGraphicsExtractor graphics, int ax, int ay, int bx, int by, int color) {
        // Orthogonal prerequisite line and an arrow immediately outside the child frame.
        boolean horizontal = Math.abs(bx - ax) >= Math.abs(by - ay);
        int direction = horizontal ? Integer.signum(bx - ax) : Integer.signum(by - ay);
        int endX = horizontal ? bx - direction * 15 : bx;
        int endY = horizontal ? by : by - direction * 15;
        if (horizontal) {
            segment(graphics, ax, ay, ax, by, color);
            segment(graphics, ax, by, endX, by, color);
        } else {
            segment(graphics, ax, ay, bx, ay, color);
            segment(graphics, bx, ay, bx, endY, color);
        }
        for (int i = 0; i < 4; i++) {
            if (horizontal) graphics.fill(endX - direction * i, endY - i, endX - direction * i + 1, endY + i + 1, color);
            else graphics.fill(endX - i, endY - direction * i, endX + i + 1, endY - direction * i + 1, color);
        }
    }

    private static void segment(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
        int left = Math.min(x1, x2), right = Math.max(x1, x2) + 1;
        int top = Math.min(y1, y2), bottom = Math.max(y1, y2) + 1;
        graphics.fill(left - 1, top - 1, right + 1, bottom + 1, 0xFF171717);
        graphics.fill(left, top, right, bottom, color);
    }
}

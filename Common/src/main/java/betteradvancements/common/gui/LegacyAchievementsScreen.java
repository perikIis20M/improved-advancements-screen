package betteradvancements.common.gui;

import betteradvancements.common.reference.Constants;
import betteradvancements.common.util.CriterionGrid;
import betteradvancements.common.util.RenderUtil;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static betteradvancements.common.gui.LegacyScreenGeometry.*;

/**
 * Port of the pre-1.12 GuiAchievements screen to the 26.3 GUI/data APIs.
 * Reference: https://github.com/Marcelektro/MavenMCP-1.8.9/blob/master/src/main/java/net/minecraft/client/gui/achievement/GuiAchievements.java
 * Original Minecraft screen/artwork: Mojang. An authored single-root map replaces AchievementList.
 */
public final class LegacyAchievementsScreen extends Screen implements ClientAdvancements.Listener {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath("betteradvancements", "textures/classic/achievement_background.png");
    private static final Identifier[] TERRAIN = Arrays.stream(new String[]{"sand", "dirt", "stone", "coal_ore", "iron_ore", "diamond_ore", "redstone_ore", "bedrock"})
        .map(name -> Identifier.fromNamespaceAndPath("betteradvancements", "textures/classic/" + name + ".png")).toArray(Identifier[]::new);
    private static final String LAYOUT_KEY = "legacy-single-map-v5";
    private final ClientAdvancements advancements;
    private final Screen parentScreen;
    private final Map<String, AdvancementNode> nodes = new LinkedHashMap<>();
    private final Map<String, DisplayInfo> catalogue = new LinkedHashMap<>();
    private final Map<String, DisplayInfo> displays = new LinkedHashMap<>();
    private final Set<String> completedIds = new HashSet<>();
    private final Map<String, String> parents = new LinkedHashMap<>();
    private final Map<String, OldScreenLayout.Point> positions = vanillaPositions();
    private final Map<String, Integer> depths = new LinkedHashMap<>();
    private final Set<String> available = new HashSet<>();
    private Map<String, List<OldScreenLayout.Point>> routes = Map.of();
    private Map<String, String> routedParents = Map.of();
    private Map<String, OldScreenLayout.Point> routedPositions = Map.of();
    private float zoom = 1.0F;
    private double previousX, previousY, scrollX, scrollY, targetX, targetY;
    private boolean positioned, dragging;
    private int minX, minY, maxX, maxY;
    private LegacyScreenGeometry geometry;
    private String searchHighlight;
    private long searchHighlightUntil;

    public LegacyAchievementsScreen(ClientAdvancements advancements, Screen parentScreen) {
        super(Component.translatable("improvedadvancements.achievements"));
        this.advancements = advancements;
        this.parentScreen = parentScreen;
        var saved = AdvancementViewState.positionsByLayout.get(LAYOUT_KEY);
        if (saved != null) saved.forEach((id, point) -> positions.putIfAbsent(id, new OldScreenLayout.Point(point.getFirst(), point.getSecond())));
    }

    private static Map<String, OldScreenLayout.Point> vanillaPositions() {
        try (var stream = LegacyAchievementsScreen.class.getResourceAsStream("/assets/betteradvancements/old_screen_vanilla.json")) {
            if (stream == null) throw new IllegalStateException("Missing modern advancement graph");
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                Map<String, String> graph = new LinkedHashMap<>();
                JsonParser.parseReader(reader).getAsJsonObject().entrySet().forEach(entry ->
                    graph.put(entry.getKey(), entry.getValue().isJsonNull() ? null : entry.getValue().getAsString()));
                Map<String, OldScreenLayout.Point> result = new LinkedHashMap<>();
                result.putAll(LegacyAchievementLayout.create(graph));
                return result;
            }
        } catch (Exception error) {
            Constants.log.warn("Could not load legacy screen's modern advancement adapter", error);
            return new LinkedHashMap<>();
        }
    }

    @Override
    protected void init() {
        dragging = false;
        var oldGeometry = geometry;
        geometry = LegacyScreenGeometry.create(width, height, BetterAdvancementsScreen.oldScreenSize);
        if (positioned && oldGeometry != null) {
            // Keep the same map center when the available window changes.
            double dx = (oldGeometry.mapWidth() - geometry.mapWidth()) * zoom / 2.0;
            double dy = (oldGeometry.mapHeight() - geometry.mapHeight()) * zoom / 2.0;
            scrollX += dx; previousX += dx; targetX += dx;
            scrollY += dy; previousY += dy; targetY += dy;
        }
        loadCatalogue();
        advancements.setListener(this);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> minecraft.setScreenAndShow(parentScreen))
            .bounds(geometry.rightButtonX(geometry.buttonWidth()), geometry.footerY(), geometry.buttonWidth(), 20).build());
        addRenderableWidget(Button.builder(Component.translatable("improvedadvancements.search.button"), button -> openSearch())
            .bounds(geometry.leftButtonX(), geometry.footerY(), geometry.buttonWidth(), 20).build());
        AdvancementNode root = nodes.get("minecraft:story/root");
        if (root == null && !nodes.isEmpty()) root = nodes.values().iterator().next().root();
        if (root != null) advancements.setSelectedTab(root.holder(), true);
    }

    private void loadCatalogue() {
        if (!catalogue.isEmpty() || minecraft.getConnection() == null) return;
        // Like the old AchievementList, the vanilla map is known before every goal is discovered.
        // Server-supplied display data always takes precedence, and hidden entries stay hidden.
        try (var input = LegacyAchievementsScreen.class.getResourceAsStream("/assets/betteradvancements/old_screen_titles.json")) {
            if (input == null) throw new IllegalStateException("Missing vanilla display catalogue");
            try (var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                var ops = minecraft.getConnection().registryAccess().createSerializationContext(JsonOps.INSTANCE);
                JsonParser.parseReader(reader).getAsJsonObject().entrySet().forEach(entry ->
                    DisplayInfo.CODEC.parse(ops, entry.getValue()).resultOrPartial(error ->
                        Constants.log.warn("Could not read classic map display {}: {}", entry.getKey(), error))
                        .ifPresent(display -> catalogue.put(entry.getKey(), display)));
            }
        } catch (Exception error) {
            Constants.log.warn("Could not load classic map display catalogue", error);
        }
    }

    @Override
    public void onAdvancementsUpdated() {
        nodes.clear();
        parents.clear();
        displays.clear();
        completedIds.clear();
        var tree = advancements.tree();
        List<AdvancementNode> all = new ArrayList<>();
        tree.roots().forEach(all::add);
        tree.tasks().forEach(all::add);
        all.sort(Comparator.comparing(node -> node.holder().id().toString()));
        Map<String, String> serverParents = new LinkedHashMap<>();
        advancements.progress().forEach((holder, progress) -> {
            if (progress.isDone()) completedIds.add(holder.id().toString());
        });
        for (AdvancementNode node : all) {
            if (node.advancement().display().isEmpty()) continue;
            String id = node.holder().id().toString();
            nodes.put(id, node);
            AdvancementNode parent = node.parent();
            while (parent != null && parent.advancement().display().isEmpty()) parent = parent.parent();
            serverParents.put(id, parent == null ? null : parent.holder().id().toString());
        }
        // A reset/new world can have no server-visible advancements at all. The classic root
        // and its nearby locked steps must still exist, just as they did in AchievementList.
        displays.putAll(catalogue);
        nodes.forEach((id, node) -> displays.put(id, node.advancement().display().orElseThrow()));
        parents.putAll(LegacyAchievementLayout.parents(serverParents));
        LegacyAchievementLayout.addMissing(positions, parents);
        if (!parents.equals(routedParents) || !positions.equals(routedPositions)) {
            routes = LegacyAchievementRoutes.create(positions, parents);
            routedParents = new LinkedHashMap<>(parents);
            routedPositions = new LinkedHashMap<>(positions);
        }
        depths.clear();
        available.clear();
        displays.forEach((id, display) -> {
            if (OldScreenVisibility.canUnlock(id, parents, completedIds)) available.add(id);
            int depth = OldScreenVisibility.unlockDepth(id, parents, completedIds);
            if (OldScreenVisibility.isVisible(depth, display.hidden(), completedIds.contains(id),
                BetterAdvancementsScreen.oldScreenRevealDepth))
                depths.put(id, depth);
        });
        // The old screen bounds used the complete achievement list, including undiscovered entries.
        minX = positions.values().stream().mapToInt(OldScreenLayout.Point::x).min().orElse(0) - 112;
        minY = positions.values().stream().mapToInt(OldScreenLayout.Point::y).min().orElse(0) - 112;
        maxX = positions.values().stream().mapToInt(OldScreenLayout.Point::x).max().orElse(0) - 77;
        maxY = positions.values().stream().mapToInt(OldScreenLayout.Point::y).max().orElse(0) - 77;
        if (!positioned && !depths.isEmpty()) {
            var first = positions.get(depths.containsKey("minecraft:story/root") ? "minecraft:story/root" : depths.keySet().iterator().next());
            // Original openInventory start: column*24 - 141/2 - 12, row*24 - 141/2.
            scrollX = first.x() - 82 - (geometry.mapWidth() - MAP_WIDTH) / 2.0;
            scrollY = first.y() - 70 - (geometry.mapHeight() - MAP_HEIGHT) / 2.0;
            if (BetterAdvancementsScreen.rememberPosition && AdvancementViewState.hasLegacyScreenPosition
                && AdvancementViewState.positionsByLayout.containsKey(LAYOUT_KEY)) {
                scrollX = AdvancementViewState.legacyScreenScrollX;
                scrollY = AdvancementViewState.legacyScreenScrollY;
                zoom = Mth.clamp(AdvancementViewState.legacyScreenZoom, 1.0F, 2.0F);
            }
            targetX = previousX = scrollX;
            targetY = previousY = scrollY;
            positioned = true;
        }
        clampTargets();
    }

    private void clampTargets() {
        targetX = Mth.clamp(targetX, minX, Math.max(minX, maxX - 1));
        targetY = Mth.clamp(targetY, minY, Math.max(minY, maxY - 1));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        // Pre-1.12 drawDefaultBackground darkens the world; it does not blur it.
        graphics.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);
    }

    @Override
    public void tick() {
        previousX = scrollX;
        previousY = scrollY;
        double dx = targetX - scrollX, dy = targetY - scrollY;
        double fraction = dx * dx + dy * dy < 4.0D ? 1.0D : 0.85D;
        scrollX += dx * fraction;
        scrollY += dy * fraction;
    }

    private void changeZoom(float delta) {
        float oldZoom = zoom;
        zoom = Mth.clamp(zoom + delta, 1.0F, 2.0F);
        if (oldZoom != zoom) {
            scrollX -= (zoom - oldZoom) * geometry.mapWidth() * 0.5F;
            scrollY -= (zoom - oldZoom) * geometry.mapHeight() * 0.5F;
            targetX = previousX = scrollX;
            targetY = previousY = scrollY;
            clampTargets();
        }
    }

    private boolean insideMap(double x, double y) {
        return geometry.insideMap(x, y);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (event.button() == 1 && insideMap(event.x(), event.y())) {
            dragging = true;
            setDragging(true);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!dragging || event.button() != 1) return false;
        scrollX -= geometry.dragDistance(dx, zoom);
        scrollY -= geometry.dragDistance(dy, zoom);
        targetX = previousX = scrollX;
        targetY = previousY = scrollY;
        clampTargets();
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean wasDragging = dragging;
        dragging = false;
        setDragging(false);
        return super.mouseReleased(event) || wasDragging;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        if (dy == 0 || !insideMap(x, y)) return false;
        changeZoom(dy > 0 ? -0.25F : 0.25F);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (AdvancementSearchScreen.isSearchShortcut(event)) { openSearch(); return true; }
        if (minecraft.options.keyInventory.matches(event) || minecraft.options.keyAdvancements.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private void openSearch() {
        List<AdvancementSearchScreen.Result> results = new ArrayList<>();
        for (var entry : depths.entrySet()) {
            if (entry.getValue() >= 3) continue; // Never reveal ??? entries through search.
            String id = entry.getKey();
            var display = displays.get(id);
            var node = nodes.get(id);
            var progress = node == null ? null : advancements.progress().get(node.holder());
            boolean unlocked = available.contains(id);
            ItemStack icon = id.equals(LegacyAchievementLayout.ROOT) ? new ItemStack(Items.BOOK) : display.icon().create();
            var searchEntry = new AdvancementSearchIndex.Entry(id, display.title().getString(), display.description().getString(),
                icon.getHoverName().getString(), true, unlocked, completed(id), progress == null ? 0 : progress.getPercent());
            var parent = displays.get(parents.get(id));
            Component details = unlocked ? display.description() : Component.translatable("improvedadvancements.requires",
                parent == null ? Component.literal(Objects.toString(parents.get(id), "")) : parent.title());
            results.add(new AdvancementSearchScreen.Result(searchEntry, icon, display.type(), AdvancementSearchScreen.tooltip(searchEntry, details)));
        }
        minecraft.setScreenAndShow(new AdvancementSearchScreen(this, results, this::focusSearchResult));
    }

    private void focusSearchResult(String id) {
        if (!depths.containsKey(id) || depths.get(id) >= 3) return;
        var point = positions.get(id);
        targetX = point.x() + 11 - geometry.mapWidth() * zoom / 2;
        targetY = point.y() + 11 - geometry.mapHeight() * zoom / 2;
        clampTargets();
        searchHighlight = id;
        searchHighlightUntil = System.currentTimeMillis() + 4000;
    }

    private boolean completed(String id) {
        return completedIds.contains(id);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        int xScroll = Mth.clamp(Mth.floor(previousX + (scrollX - previousX) * partialTicks), minX, Math.max(minX, maxX - 1));
        int yScroll = Mth.clamp(Mth.floor(previousY + (scrollY - previousY) * partialTicks), minY, Math.max(minY, maxY - 1));
        // Window sizing uses GUI pixels; only the map's separate wheel zoom has a transform.
        graphics.enableScissor(geometry.clipLeft(), geometry.clipTop(), geometry.clipRight(), geometry.clipBottom());
        graphics.pose().pushMatrix();
        graphics.pose().translate(geometry.left(), geometry.top());
        graphics.pose().pushMatrix();
        graphics.pose().translate(MAP_LEFT, MAP_TOP);
        graphics.pose().scale(1.0F / zoom);
        drawTerrain(graphics, xScroll, yScroll, geometry.mapWidth(), geometry.mapHeight());
        for (var entry : depths.entrySet()) {
            // A completed child can precede its map parent. Do not expose a hidden endpoint.
            if (!depths.containsKey(parents.get(entry.getKey()))) continue;
            var path = routes.getOrDefault(entry.getKey(), List.of());
            if (path.size() < 2) continue;
            int cx = path.getFirst().x() - xScroll + 11, cy = path.getFirst().y() - yScroll + 11;
            int px = path.get(1).x() - xScroll + 11, py = path.get(1).y() - yScroll + 11;
            int color = completed(entry.getKey()) ? 0xFFA0A0A0 : available.contains(entry.getKey()) ? 0xFF00FF00 : 0xFF000000;
            for (int i = 1; i < path.size(); i++) {
                var a = path.get(i - 1); var b = path.get(i);
                int ax = a.x() - xScroll + 11, ay = a.y() - yScroll + 11;
                int bx = b.x() - xScroll + 11, by = b.y() - yScroll + 11;
                if (ay == by) graphics.horizontalLine(Math.min(ax, bx), Math.max(ax, bx), ay, color);
                else graphics.verticalLine(ax, Math.min(ay, by), Math.max(ay, by), color);
            }
            if (cx > px) sprite(graphics, cx - 18, cy - 5, 114, 234, 7, 11, color);
            else if (cx < px) sprite(graphics, cx + 11, cy - 5, 107, 234, 7, 11, color);
            else if (cy > py) sprite(graphics, cx - 5, cy - 18, 96, 234, 11, 7, color);
            else if (cy < py) sprite(graphics, cx - 5, cy + 11, 96, 241, 11, 7, color);
        }
        String hovered = null;
        double hoverX = geometry.mapX(mouseX) * zoom, hoverY = geometry.mapY(mouseY) * zoom;
        for (var entry : depths.entrySet()) {
            var point = positions.get(entry.getKey());
            int x = point.x() - xScroll, y = point.y() - yScroll;
            if (x < -24 || y < -24 || x > geometry.mapWidth() * zoom || y > geometry.mapHeight() * zoom) continue;
            boolean done = completed(entry.getKey()), unlocked = available.contains(entry.getKey());
            var display = displays.get(entry.getKey());
            ItemStack icon = entry.getKey().equals("minecraft:story/root") ? new ItemStack(Items.BOOK) : display.icon().create();
            drawAchievementIcon(graphics, x - 2, y - 2, display.type(), icon, done, unlocked, entry.getValue());
            if (entry.getKey().equals(searchHighlight)) AdvancementSearchScreen.highlight(graphics, x - 3, y - 3, 28, searchHighlightUntil);
            if (insideMap(mouseX, mouseY) && hoverX >= x && hoverX <= x + 22 && hoverY >= y && hoverY <= y + 22)
                hovered = entry.getKey();
        }
        graphics.pose().popMatrix();
        graphics.disableScissor();
        graphics.nextStratum();
        drawWindowFrame(graphics, geometry);
        graphics.text(font, title, 15, 5, 0xFF404040, false);
        graphics.pose().popMatrix();
        graphics.nextStratum();
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        graphics.nextStratum();
        if (hovered != null) drawTooltip(graphics, hovered, mouseX, mouseY);
        if (dragging) requestDragCursor(graphics);
    }

    private void requestDragCursor(GuiGraphicsExtractor graphics) {
        boolean horizontal = maxX - 1 > minX, vertical = maxY - 1 > minY;
        if (horizontal && vertical) graphics.requestCursor(CursorTypes.RESIZE_ALL);
        else if (horizontal) graphics.requestCursor(CursorTypes.RESIZE_EW);
        else if (vertical) graphics.requestCursor(CursorTypes.RESIZE_NS);
    }

    void drawSearchWindow(GuiGraphicsExtractor graphics, LegacyScreenGeometry frame, Component heading) {
        graphics.enableScissor(frame.clipLeft(), frame.clipTop(), frame.clipRight(), frame.clipBottom());
        graphics.pose().pushMatrix();
        graphics.pose().translate(frame.left(), frame.top());
        graphics.pose().translate(MAP_LEFT, MAP_TOP);
        graphics.pose().scale(1.0F / zoom);
        drawTerrain(graphics, Mth.floor(scrollX), Mth.floor(scrollY), frame.mapWidth(), frame.mapHeight());
        graphics.pose().popMatrix();
        graphics.disableScissor();
        graphics.nextStratum();
        graphics.pose().pushMatrix();
        graphics.pose().translate(frame.left(), frame.top());
        drawWindowFrame(graphics, frame);
        graphics.text(font, heading, 15, 5, 0xFF404040, false);
        graphics.pose().popMatrix();
    }

    static void drawAchievementIcon(GuiGraphicsExtractor graphics, int x, int y, AdvancementType type,
                                   ItemStack icon, boolean done, boolean unlocked, int depth) {
        int shade = done ? 191 : unlocked ? 255 : depth < 3 ? 76 : depth == 3 ? 51 : 25;
        sprite(graphics, x, y, type == AdvancementType.CHALLENGE ? 26 : 0, 202, 26, 26, 0xFF000000 | shade * 0x010101);
        graphics.fakeItem(icon, x + 5, y + 5, unlocked ? 0xFF000000 | shade * 0x010101 : 0xFF191919);
    }

    private static void drawWindowFrame(GuiGraphicsExtractor graphics, LegacyScreenGeometry frame) {
        int right = MAP_LEFT + frame.mapWidth(), bottom = MAP_TOP + frame.mapHeight();
        int borderRight = FRAME_WIDTH - MAP_LEFT - MAP_WIDTH;
        int borderBottom = FRAME_HEIGHT - MAP_TOP - MAP_HEIGHT;
        // Tile the original edge strips and keep all four corners at their original pixel size.
        sprite(graphics, 0, 0, 0, 0, MAP_LEFT, MAP_TOP, -1);
        sprite(graphics, right, 0, FRAME_WIDTH - borderRight, 0, borderRight, MAP_TOP, -1);
        sprite(graphics, 0, bottom, 0, FRAME_HEIGHT - borderBottom, MAP_LEFT, borderBottom, -1);
        sprite(graphics, right, bottom, FRAME_WIDTH - borderRight, FRAME_HEIGHT - borderBottom, borderRight, borderBottom, -1);
        RenderUtil.renderRepeating(BACKGROUND, graphics, MAP_LEFT, 0, frame.mapWidth(), MAP_TOP, MAP_LEFT, 0, MAP_WIDTH, MAP_TOP);
        RenderUtil.renderRepeating(BACKGROUND, graphics, MAP_LEFT, bottom, frame.mapWidth(), borderBottom,
            MAP_LEFT, FRAME_HEIGHT - borderBottom, MAP_WIDTH, borderBottom);
        RenderUtil.renderRepeating(BACKGROUND, graphics, 0, MAP_TOP, MAP_LEFT, frame.mapHeight(), 0, MAP_TOP, MAP_LEFT, MAP_HEIGHT);
        RenderUtil.renderRepeating(BACKGROUND, graphics, right, MAP_TOP, borderRight, frame.mapHeight(),
            FRAME_WIDTH - borderRight, MAP_TOP, borderRight, MAP_HEIGHT);
    }

    private void drawTerrain(GuiGraphicsExtractor graphics, int scrollX, int scrollY, int mapWidth, int mapHeight) {
        int column = (scrollX + 288) >> 4, row = (scrollY + 288) >> 4;
        // floorMod also covers modern advancement coordinates above the old map's legal range.
        int offsetX = Math.floorMod(scrollX + 288, 16), offsetY = Math.floorMod(scrollY + 288, 16);
        int seed = minecraft.getUser().getProfileId().toString().hashCode();
        for (int y = 0; y * 16 - offsetY < mapHeight * zoom; y++)
            for (int x = 0; x * 16 - offsetX < mapWidth * zoom; x++) {
                int tile = OldScreenTerrain.tileAt(column + x, row + y, seed);
                graphics.blit(RenderPipelines.GUI_TEXTURED, TERRAIN[tile], x * 16 - offsetX, y * 16 - offsetY,
                    0, 0, 16, 16, 16, 16, OldScreenTerrain.tintAt(row + y));
            }
    }

    private static void sprite(GuiGraphicsExtractor graphics, int x, int y, int u, int v, int width, int height, int color) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, u, v, width, height, 256, 256, color);
    }

    private void drawTooltip(GuiGraphicsExtractor graphics, String id, int mouseX, int mouseY) {
        int depth = depths.get(id);
        if (depth > 3) return;
        boolean unlocked = available.contains(id);
        var display = displays.get(id);
        if (drawProgressTooltip(graphics, id, display, unlocked, mouseX, mouseY)) return;
        Component heading = depth == 3 ? Component.translatable("improvedadvancements.unknown") : Component.literal(display.title().getString());
        int x = mouseX + 12, y = mouseY - 4, textWidth = Math.max(font.width(heading), 120);
        Component body;
        if (unlocked) body = Component.literal(display.description().getString());
        else {
            var parent = displays.get(parents.get(id));
            body = Component.translatable("improvedadvancements.requires", parent == null
                ? Component.literal(parents.get(id)) : Component.literal(parent.title().getString()));
        }
        List<FormattedCharSequence> lines = font.split(body, textWidth);
        boolean taken = completed(id);
        int bodyHeight = lines.size() * font.lineHeight + (unlocked && taken ? 12 : 0);
        graphics.fill(x - 3, y - 3, x + textWidth + 3, y + bodyHeight + 15, 0xC0000000);
        for (int index = 0; index < lines.size(); index++)
            graphics.text(font, lines.get(index), x, y + 12 + index * font.lineHeight, unlocked ? 0xFFA0A0A0 : 0xFF707070, false);
        if (unlocked && taken)
            graphics.text(font, Component.translatable("improvedadvancements.classic.taken"), x, y + bodyHeight + 4, 0xFF9090FF);
        boolean special = display.type() == AdvancementType.CHALLENGE;
        int color = unlocked ? (special ? 0xFFFFFF80 : 0xFFFFFFFF) : (special ? 0xFF808000 : 0xFF808080);
        graphics.text(font, heading, x, y, color);
    }

    private boolean drawProgressTooltip(GuiGraphicsExtractor graphics, String id, DisplayInfo display,
                                        boolean unlocked, int mouseX, int mouseY) {
        var node = nodes.get(id);
        if (node == null) return false; // Catalogue-only entries have no server progress to report.
        var progress = advancements.progress().get(node.holder());
        var summary = LegacyTooltipProgress.from(BetterAdvancementsScreen.oldScreenTooltipProgress,
            unlocked, node.advancement().requirements(), progress);
        if (summary == null) return false;

        int maxTextWidth = Math.max(80, Math.min(280, width - 16));
        var description = font.split(display.description(), maxTextWidth);
        int descriptionHeight = description.size() * font.lineHeight;
        int maxGridHeight = Math.max(font.lineHeight, Math.min(height / 2, height - descriptionHeight - 66));
        CriterionGrid grid = !CriterionGrid.requiresShift || minecraft.hasShiftDown()
            ? CriterionGrid.findOptimalCriterionGrid(node.holder(), node.advancement(), progress,
                maxTextWidth, maxGridHeight, font) : null;
        var heading = font.split(display.title(), maxTextWidth);
        Component counter = summary.label();
        int textWidth = Math.max(120, font.width(counter));
        for (var line : heading) textWidth = Math.max(textWidth, font.width(line));
        for (var line : description) textWidth = Math.max(textWidth, font.width(line));
        if (grid != null) textWidth = Math.max(textWidth, grid.width);
        int gridHeight = grid == null || !grid.hasPages() ? 0 : grid.height + 4;
        boolean taken = completed(id);
        int contentHeight = heading.size() * font.lineHeight + font.lineHeight + 16
            + descriptionHeight + gridHeight + (taken ? font.lineHeight + 4 : 0);

        // Tooltip coordinates stay in screen space, independent of the map's zoom. The existing
        // fit helper also keeps exceptionally large translated/custom tooltips inside the window.
        var placement = OldScreenTooltip.place(mouseX + 12, mouseY - 4, textWidth + 8, contentHeight + 8,
            1.0F, 0, 0, width, height);
        graphics.pose().pushMatrix();
        graphics.pose().translate(placement.boxX(), placement.boxY());
        graphics.pose().scale(placement.scale());
        graphics.fill(0, 0, textWidth + 8, contentHeight + 8, 0xE0000000);
        int y = 4;
        int titleColor = display.type() == AdvancementType.CHALLENGE ? 0xFFFFFF80 : 0xFFFFFFFF;
        for (var line : heading) {
            graphics.text(font, line, 4, y, titleColor, false);
            y += font.lineHeight;
        }
        y += 4;
        graphics.text(font, counter, 4, y, 0xFFDDDDDD, false);
        y += font.lineHeight + 3;
        graphics.fill(4, y, 4 + textWidth, y + 4, 0xFF404040);
        int fillWidth = Mth.floor(textWidth * summary.fraction());
        if (fillWidth > 0) graphics.fill(4, y, 4 + fillWidth, y + 4, taken ? 0xFFD5AA32 : 0xFF80B83C);
        y += 9;
        for (var line : description) {
            graphics.text(font, line, 4, y, 0xFFA0A0A0, false);
            y += font.lineHeight;
        }
        if (grid != null && grid.hasPages()) {
            y += 4;
            int x = 4;
            for (var column : grid.getCurrentPage()) {
                for (int row = 0; row < column.cells().size(); row++)
                    graphics.text(font, column.cells().get(row), x, y + row * font.lineHeight, 0xFFFFFFFF, false);
                x += column.width();
            }
            y += grid.height;
        }
        if (taken) graphics.text(font, Component.translatable("improvedadvancements.classic.taken"),
            4, y + 4, 0xFF9090FF, false);
        graphics.pose().popMatrix();
        return true;
    }

    @Override
    public void removed() {
        if (BetterAdvancementsScreen.rememberPosition && positioned) {
            AdvancementViewState.hasLegacyScreenPosition = true;
            AdvancementViewState.legacyScreenScrollX = targetX;
            AdvancementViewState.legacyScreenScrollY = targetY;
            AdvancementViewState.legacyScreenZoom = zoom;
            Map<String, Pair<Integer, Integer>> saved = new LinkedHashMap<>();
            positions.forEach((id, point) -> saved.put(id, Pair.of(point.x(), point.y())));
            AdvancementViewState.positionsByLayout.put(LAYOUT_KEY, saved);
            AdvancementViewState.save();
        }
        advancements.setListener(null);
        if (minecraft.getConnection() != null)
            minecraft.getConnection().send(ServerboundSeenAdvancementsPacket.closedScreen());
        super.removed();
    }

    @Override public void onSelectedTabChanged(AdvancementHolder tab) { }
    @Override public void onAdvancementsCleared() {
        nodes.clear(); displays.clear(); completedIds.clear(); parents.clear(); depths.clear(); available.clear();
    }
}

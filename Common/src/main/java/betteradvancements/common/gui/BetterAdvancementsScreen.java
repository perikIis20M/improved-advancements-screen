package betteradvancements.common.gui;

import betteradvancements.common.platform.Services;
import betteradvancements.common.reference.Resources;
import betteradvancements.common.util.RenderUtil;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.util.FormattedCharSequence;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.StreamSupport;

public class BetterAdvancementsScreen extends Screen implements ClientAdvancements.Listener {
    private static final Component VERY_SAD_LABEL = Component.translatable("advancements.sad_label");
    private static final Component NO_ADVANCEMENTS_LABEL = Component.translatable("advancements.empty");
    private static final Component TITLE =Component.translatable("gui.advancements");
    private static final int WIDTH = 252, HEIGHT = 140, CORNER_SIZE = 30;
    private static final int SIDE = ModernScreenGeometry.SIDE, TOP = ModernScreenGeometry.TOP,
        BOTTOM = ModernScreenGeometry.BOTTOM, PADDING = ModernScreenGeometry.PADDING;
    private static final int OLD_FOOTER_HEIGHT = 28;
    private static final int OLD_VERTICAL_OFFSET = (TOP - BOTTOM + OLD_FOOTER_HEIGHT) / 2;
    private static final float TREE_SCALE = 1.0F;
    private final ClientAdvancements clientAdvancements;
    private final Map<AdvancementHolder, BetterAdvancementTab> tabs = Maps.newLinkedHashMap();
    private BetterAdvancementTab selectedTab;
    private static int tabPage, maxPages;
    private boolean isScrolling;
    protected int internalWidth, internalHeight;
    public static boolean combinedMode = false;
    public static int oldScreenSize = LegacyScreenGeometry.DEFAULT_SIZE;
    public static int oldScreenRevealDepth = OldScreenVisibility.DEFAULT_REVEAL_DEPTH;
    public static boolean oldScreenTooltipProgress = false;
    public static int windowScale = ScreenSize.DEFAULT;
    public static boolean showProgress = true;
    public static boolean rememberPosition = true;
    public static boolean rememberOldPosition = true;
    public static boolean stableLayout = true;
    private final OldAdvancementsView oldView = new OldAdvancementsView();
    private boolean draggingOldMap;
    private Button zoomOutButton;
    private Button zoomInButton;
    public static boolean showDebugCoordinates = false;
    public static boolean orderTabsAlphabetically = false;
    public static boolean keepAdvancementsTitle = true;
    private BetterAdvancementWidget advConnectedToMouse = null;

    public BetterAdvancementsScreen(ClientAdvancements clientAdvancements) {
        super(GameNarrator.NO_TITLE);
        this.clientAdvancements = clientAdvancements;
    }

    public static Screen create(ClientAdvancements advancements) {
        return create(advancements, net.minecraft.client.Minecraft.getInstance().gui.screen());
    }

    public static Screen create(ClientAdvancements advancements, Screen parentScreen) {
        return combinedMode ? new LegacyAchievementsScreen(advancements, parentScreen) : new BetterAdvancementsScreen(advancements);
    }

    /**
     * Adds the buttons (and other controls) to the screen in question. Called when the GUI is displayed and when the
     * window resizes, the buttonList is cleared beforehand.
     */
    @Override
    protected void init() {
        var geometry = ModernScreenGeometry.create(width, height, windowScale);
        this.internalHeight = geometry.internalHeight();
        this.internalWidth = geometry.internalWidth();
        this.draggingOldMap = false;
        this.isScrolling = false;
        this.zoomOutButton = null;
        this.zoomInButton = null;

        // Keep the current selection for the listener's rebuild when returning from search.
        this.clientAdvancements.setListener(this);

        if (this.selectedTab == null && !this.tabs.isEmpty()) {
            BetterAdvancementTab advancementTab = this.tabs.values().iterator().next();
            this.clientAdvancements.setSelectedTab(advancementTab.getRootNode().holder(), true);
        } else {
            this.clientAdvancements.setSelectedTab(this.selectedTab == null ? null : this.selectedTab.getRootNode().holder(), true);
        }

        int left = SIDE + (width - internalWidth) / 2;
        int top = windowTop();

        int right = internalWidth - SIDE + (width - internalWidth) / 2;
        int bottom = windowBottom();

        int width = right - left;
        int height = bottom - top;

        int maxTabs = BetterAdvancementTabType.getMaxTabs(width, height);

        maxPages = Math.max(0, (this.tabs.size() - 1) / Math.max(1, maxTabs));
        tabPage = Math.min(tabPage, maxPages);
        if (!combinedMode) addRenderableWidget(Button.builder(Component.translatable("improvedadvancements.search.button"), button -> openSearch())
            .pos(right - 84, bottom + 4).size(80, 20).build());
        if (maxPages > 0 && !combinedMode) {

            addRenderableWidget(Button.builder(Component.literal("<"), b -> tabPage = Math.max(tabPage - 1, 0)).pos(left + 4, bottom + 4).size(20, 20).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> tabPage = Math.min(tabPage + 1, maxPages)).pos(left + 28, bottom + 4).size(20, 20).build());
        }
        if (combinedMode) {
            this.zoomOutButton = addRenderableWidget(Button.builder(Component.literal("-"), button -> changeOldZoom(-1))
                .pos(left + 4, bottom + 4).size(20, 20)
                .createNarration(message -> Component.translatable("improvedadvancements.zoom.out", this.oldView.zoomPercent())).build());
            this.zoomInButton = addRenderableWidget(Button.builder(Component.literal("+"), button -> changeOldZoom(1))
                .pos(left + 28, bottom + 4).size(20, 20)
                .createNarration(message -> Component.translatable("improvedadvancements.zoom.in", this.oldView.zoomPercent())).build());
            updateOldZoomButtons();
            addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose())
                .pos(right - 84, bottom + 4).size(80, 20).build());
        }
    }

    private void changeOldZoom(int steps) {
        this.oldView.changeZoom(steps, mapWidth(), mapHeight());
        updateOldZoomButtons();
    }

    private void updateOldZoomButtons() {
        if (this.zoomOutButton != null) {
            this.zoomOutButton.active = this.oldView.canZoomOut();
            this.zoomOutButton.setTooltip(Tooltip.create(Component.translatable("improvedadvancements.zoom.out", this.oldView.zoomPercent())));
        }
        if (this.zoomInButton != null) {
            this.zoomInButton.active = this.oldView.canZoomIn();
            this.zoomInButton.setTooltip(Tooltip.create(Component.translatable("improvedadvancements.zoom.in", this.oldView.zoomPercent())));
        }
    }

    /**
     * Called when the screen is unloaded. Used to disable keyboard repeat events
     */
    @Override
    public void onClose() {
        this.oldView.store();
        if (stableLayout) this.tabs.values().forEach(BetterAdvancementTab::rememberStableLayout);
        if (this.selectedTab != null && rememberPosition) {
            this.selectedTab.storeScroll();
            AdvancementViewState.selectedTab = this.selectedTab.getRootNode().holder().id().toString();
        }
        if (rememberPosition || stableLayout) AdvancementViewState.save();
        this.clientAdvancements.setListener(null);
        ClientPacketListener clientpacketlistener = this.minecraft.getConnection();
        if (clientpacketlistener != null) {
            clientpacketlistener.send(ServerboundSeenAdvancementsPacket.closedScreen());
        }
        super.onClose();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (super.mouseClicked(event, isDoubleClick)) return true;
        if (combinedMode && event.button() == 1 && isInsideMap(event.x(), event.y())) {
            this.draggingOldMap = true;
            this.setDragging(true);
            return true;
        }
        if (!combinedMode) {
            int left = SIDE + (width - internalWidth) / 2;
            int top = windowTop();

            int right = internalWidth - SIDE + (width - internalWidth) / 2;
            int bottom = windowBottom();

            int width = right - left;
            int height = bottom - top;

            int maxTabs = BetterAdvancementTabType.getMaxTabs(width, height);
            int skip = tabPage * maxTabs;

            for (BetterAdvancementTab tab : this.tabs.values().stream().skip(skip).limit(maxTabs).toList()) {
                if (event.button() == 1 && tab.isMouseOver(left, top, width, height, event.x(), event.y())) {
                    this.onSelectedTabChanged(tab.getRootNode().holder());
                    this.clientAdvancements.setSelectedTab(tab.getRootNode().holder(), true);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (combinedMode) {
            if (!isInsideMap(mouseX, mouseY)) return false;
            this.oldView.pan(scrollX * 16, scrollY * 16, mapWidth(), mapHeight());
            return true;
        }
        if (this.selectedTab != null) {
            this.selectedTab.scroll(scrollX * 16, scrollY * 16, internalWidth - 2 * SIDE - 2 * PADDING, internalHeight - TOP - BOTTOM - 3 * PADDING);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (AdvancementSearchScreen.isSearchShortcut(event)) { openSearch(); return true; }
        if (this.minecraft.options.keyAdvancements.matches(event)) {
            this.onClose();
            return true;
        } else {
            return super.keyPressed(event);
        }
    }

    private void openSearch() {
        List<AdvancementSearchScreen.Result> results = new ArrayList<>();
        for (var tab : tabs.values()) for (var widget : tab.widgets.values()) {
            if (!widget.isVisibleOnMap()) continue;
            var node = widget.getAdvancement();
            var display = node.advancement().display().orElseThrow();
            var progress = clientAdvancements.progress().get(node.holder());
            var icon = display.icon().create();
            var entry = new AdvancementSearchIndex.Entry(node.holder().id().toString(), display.title().getString(),
                display.description().getString(), icon.getHoverName().getString(), true, true,
                widget.isCompleted(), progress == null ? 0 : progress.getPercent());
            results.add(new AdvancementSearchScreen.Result(entry, icon, display.type(), AdvancementSearchScreen.tooltip(entry, display.description())));
        }
        if (selectedTab != null) selectedTab.storeScroll();
        clientAdvancements.setListener(null);
        minecraft.setScreenAndShow(new AdvancementSearchScreen(this, results, this::focusSearchResult));
    }

    private void focusSearchResult(String id) {
        for (var tab : tabs.values()) for (var widget : tab.widgets.values()) {
            if (!widget.isVisibleOnMap() || !widget.getAdvancement().holder().id().toString().equals(id)) continue;
            onSelectedTabChanged(tab.getRootNode().holder());
            clientAdvancements.setSelectedTab(tab.getRootNode().holder(), true);
            int maxTabs = Math.max(1, BetterAdvancementTabType.getMaxTabs(internalWidth - 2 * SIDE, internalHeight - TOP - BOTTOM));
            tabPage = tab.getIndex() / maxTabs;
            tab.focusSearchResult(widget, mapWidth(), mapHeight());
            return;
        }
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double mouseDeltaX, double mouseDeltaY) {
        if (combinedMode) {
            if (event.button() == 1 && this.draggingOldMap) {
                this.oldView.pan(mouseDeltaX, mouseDeltaY, mapWidth(), mapHeight());
                return true;
            }
            return false;
        }
        int left = SIDE + (width - internalWidth) / 2;
        int top = windowTop();

        if (event.button() != 1) {
            this.isScrolling = false;
            return false;
        }

        if (!this.isScrolling) {
            if (this.advConnectedToMouse == null) {
                boolean inGui = event.x() < left + internalWidth - 2*SIDE - PADDING && event.x() > left + PADDING
                    && event.y() < top + internalHeight - TOP - SIDE - PADDING && event.y() > top + 2*PADDING;
                if (this.selectedTab != null && inGui) {
                    for (BetterAdvancementWidget betterAdvancementEntryScreen : this.selectedTab.widgets.values()) {
                        if (betterAdvancementEntryScreen.isMouseOver(this.selectedTab.scrollX, this.selectedTab.scrollY, event.x() - left - PADDING, event.y() - top - 2*PADDING, TREE_SCALE)) {

                            if (betterAdvancementEntryScreen.betterDisplayInfo.allowDragging())
                            {
                                this.advConnectedToMouse = betterAdvancementEntryScreen;
                                break;
                            }
                        }
                    }
                }
            }
            else {
                this.advConnectedToMouse.x = (int)Math.round(this.advConnectedToMouse.x + mouseDeltaX);
                this.advConnectedToMouse.y = (int)Math.round(this.advConnectedToMouse.y + mouseDeltaY);
            }
        }
        else {
            if (this.advConnectedToMouse != null) {
                Services.PLATFORM.getEventHelper().postAdvancementMovementEvent(advConnectedToMouse);
            }
            this.advConnectedToMouse = null;
        }

        if (this.advConnectedToMouse == null) {
            if (!this.isScrolling) {
                this.isScrolling = true;
            } else if (this.selectedTab != null) {
                this.selectedTab.scroll(mouseDeltaX , mouseDeltaY, internalWidth - 2 * SIDE - 2 * PADDING, internalHeight - TOP - BOTTOM - 3 * PADDING);
            }
        }

        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean wasDraggingOldMap = this.draggingOldMap;
        this.draggingOldMap = false;
        this.setDragging(false);
        this.isScrolling = false;
        if (this.advConnectedToMouse != null) {
            Services.PLATFORM.getEventHelper().postAdvancementMovementEvent(this.advConnectedToMouse);
            this.advConnectedToMouse = null;
        }
        return super.mouseReleased(event) || wasDraggingOldMap;
    }

    /**
     * Draws the screen and all the components in it.
     */
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {

        int left = SIDE + (width - internalWidth) / 2;
        int top = windowTop();

        int right = internalWidth - SIDE + (width - internalWidth) / 2;
        int bottom = windowBottom();

        int width = right - left;
        int height = bottom - top;

        int maxTabs = BetterAdvancementTabType.getMaxTabs(width, height);
        int skip = tabPage * maxTabs;

        this.renderInside(guiGraphics, mouseX, mouseY, left, top, right, bottom, maxTabs, skip);
        guiGraphics.nextStratum();
        this.renderWindow(guiGraphics, left, top, right, bottom, maxTabs, skip);
        guiGraphics.nextStratum();
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
        guiGraphics.nextStratum();
        //Don't draw tool tips if dragging an advancement
        if (this.advConnectedToMouse == null && !this.draggingOldMap) {
            this.renderToolTips(guiGraphics, mouseX, mouseY, left, top, right, bottom, maxTabs, skip);
        }
        if (!combinedMode && isScrolling && selectedTab != null) {
            boolean horizontal = selectedTab.treeWidth() > mapWidth();
            boolean vertical = selectedTab.treeHeight() > mapHeight();
            if (horizontal && vertical) guiGraphics.requestCursor(CursorTypes.RESIZE_ALL);
            else if (horizontal) guiGraphics.requestCursor(CursorTypes.RESIZE_EW);
            else if (vertical) guiGraphics.requestCursor(CursorTypes.RESIZE_NS);
        }
        
        //Draw guide lines to all advancements at 45 or 90 degree angles.
        if (this.advConnectedToMouse != null)
        {
            for (BetterAdvancementWidget betterAdvancementEntryScreen : this.selectedTab.widgets.values()) {
                if (betterAdvancementEntryScreen != this.advConnectedToMouse)
                {
                    int x1 = betterAdvancementEntryScreen.x + left + PADDING + this.selectedTab.scrollX + 3;
                    int x2 = this.advConnectedToMouse.x + left + PADDING + this.selectedTab.scrollX + 3;
                    int y1 = betterAdvancementEntryScreen.y + top + 2 * PADDING + this.selectedTab.scrollY;
                    int y2 = this.advConnectedToMouse.y + top + 2 * PADDING + this.selectedTab.scrollY;
                    int centerX1 = betterAdvancementEntryScreen.x + left + PADDING + this.selectedTab.scrollX + 3 + BetterAdvancementWidget.ADVANCEMENT_SIZE / 2;
                    int centerX2 = this.advConnectedToMouse.x + left + PADDING + this.selectedTab.scrollX + 3 + BetterAdvancementWidget.ADVANCEMENT_SIZE / 2;
                    int centerY1 = betterAdvancementEntryScreen.y + top + 2 * PADDING + this.selectedTab.scrollY + BetterAdvancementWidget.ADVANCEMENT_SIZE / 2;
                    int centerY2 = this.advConnectedToMouse.y + top + 2 * PADDING + this.selectedTab.scrollY + BetterAdvancementWidget.ADVANCEMENT_SIZE / 2;
                    double degrees = Math.toDegrees(Math.atan2(centerX1 - centerX2, centerY1 - centerY2));
                    if (degrees < 0)
                    {
                        degrees += 360;
                    }
                    
                    if (betterAdvancementEntryScreen.x == this.advConnectedToMouse.x)
                    {
                        if (y1 > y2)
                        {
                            //Draw right
                            RenderUtil.drawRect(guiGraphics, x1, y1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, x2, y2, 1, 0x00FF00);
                            //Draw bottom for bottom
                            RenderUtil.drawRect(guiGraphics, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, x2, y1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            //Draw top for bottom
                            RenderUtil.drawRect(guiGraphics, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x2, y1, 1, 0x00FF00);
                            //Draw bottom for top
                            RenderUtil.drawRect(guiGraphics, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, x2, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            //Draw top for top
                            RenderUtil.drawRect(guiGraphics, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2, x2, y2, 1, 0x00FF00);
                            //Draw left
                            RenderUtil.drawRect(guiGraphics, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, x2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2, 1, 0x00FF00);
                        }
                        else
                        {
                            //Draw right
                            RenderUtil.drawRect(guiGraphics, x1, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, x2, y1, 1, 0x00FF00);
                            //Draw bottom for bottom
                            RenderUtil.drawRect(guiGraphics, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, x2, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            //Draw top for bottom
                            RenderUtil.drawRect(guiGraphics, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2, x2, y2, 1, 0x00FF00);
                            //Draw bottom for top
                            RenderUtil.drawRect(guiGraphics, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, x2, y1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            //Draw top for top
                            RenderUtil.drawRect(guiGraphics, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x2, y1, 1, 0x00FF00);
                            //Draw left
                            RenderUtil.drawRect(guiGraphics, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, x2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, 1, 0x00FF00);
                        }
                    }
                    if (betterAdvancementEntryScreen.y == this.advConnectedToMouse.y)
                    {
                        if (x1 > x2)
                        {
                            //Draw top
                            RenderUtil.drawRect(guiGraphics, x2, y1, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2, 1, 0x00FF00);
                            //Draw left for right
                            RenderUtil.drawRect(guiGraphics, x1, y1, x1, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            //Draw right for right
                            RenderUtil.drawRect(guiGraphics, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            //Draw left for left
                            RenderUtil.drawRect(guiGraphics, x2, y1, x2, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            //Draw right for left
                            RenderUtil.drawRect(guiGraphics, x2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            //Draw bottom
                            RenderUtil.drawRect(guiGraphics, x2, y1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                        }
                        else
                        {
                            //Draw left
                            RenderUtil.drawRect(guiGraphics, x2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x1, y2, 1, 0x00FF00);
                            //Draw left for right
                            RenderUtil.drawRect(guiGraphics, x2, y1, x2, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            //Draw right for right
                            RenderUtil.drawRect(guiGraphics, x2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            //Draw left for left
                            RenderUtil.drawRect(guiGraphics, x1, y1, x1, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            //Draw right for left
                            RenderUtil.drawRect(guiGraphics, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            //Draw right
                            RenderUtil.drawRect(guiGraphics, x2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, x1, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                        }
                    }
                    if (degrees == 45 || degrees == 135 || degrees == 225 || degrees == 315)
                    {
                        //Draw lines around each advancement
                        //First
                        //Top
                        RenderUtil.drawRect(guiGraphics, x1, y1, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, 1, 0x00FF00);
                        //Bottom
                        RenderUtil.drawRect(guiGraphics, x1, y1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                        //Left
                        RenderUtil.drawRect(guiGraphics, x1, y1, x1, y1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                        //Right
                        RenderUtil.drawRect(guiGraphics, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                        //Second
                        //Top
                        RenderUtil.drawRect(guiGraphics, x2, y2, x2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2, 1, 0x00FF00);
                        //Bottom
                        RenderUtil.drawRect(guiGraphics, x2, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, x2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                        //Left
                        RenderUtil.drawRect(guiGraphics, x2, y2, x2, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                        //Right
                        RenderUtil.drawRect(guiGraphics, x2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2, x2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                        
                        if (degrees == 45 || degrees == 225)
                        {
                            RenderUtil.drawRect(guiGraphics, x1, y1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, x2, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2, 1, 0x00FF00);
                        }
                        else if (degrees == 135 || degrees == 315)
                        {
                            RenderUtil.drawRect(guiGraphics, x1, y1, x2, y2, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, x2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                        }
                    }
                }
            }
        }

        if (!combinedMode && BetterAdvancementsScreen.showDebugCoordinates && this.selectedTab != null && mouseX < internalWidth - SIDE - PADDING && mouseX > SIDE + PADDING && mouseY < internalHeight - top + 1 && mouseY > top + PADDING * 2) {
            //If dragging an advancement, draw coordinates of advancement being moved instead of mouse coordinates
            if (this.advConnectedToMouse != null) {
                //-3 and -1 are needed to have the coordinates be rendered where the advancement starts being rendered, rather than its real position.
                int currentX = this.advConnectedToMouse.x + left + PADDING + this.selectedTab.scrollX + 3 + 1;
                int currentY = this.advConnectedToMouse.y + top + 2 * PADDING + this.selectedTab.scrollY - font.lineHeight + 1;

                guiGraphics.text(font, this.advConnectedToMouse.x + "," + this.advConnectedToMouse.y, currentX, currentY, 0x000000);
            } else {
                //Draws a string containing the current position above the mouse. Locked to inside the advancement window.
                int xMouse = mouseX - left - PADDING;
                int yMouse = mouseY - top - 2 * PADDING;
                //-3 and -1 are needed to have the position be where the advancement starts being rendered, rather than its real position.
                int currentX = xMouse - this.selectedTab.scrollX - 3 - 1;
                int currentY = yMouse - this.selectedTab.scrollY - 1;

                guiGraphics.text(font, currentX + "," + currentY, mouseX, mouseY - font.lineHeight, 0x000000);
            }
        }
    }

    private void renderInside(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, int left, int top, int right, int bottom, int maxTabs, int skip) {
        BetterAdvancementTab betterAdvancementTab = this.selectedTab;
        int boxLeft = left + PADDING;
        int boxTop = top + 2*PADDING;
        int boxRight = right - PADDING;
        int boxBottom = bottom - PADDING;

        int width = boxRight - boxLeft;
        int height = boxBottom - boxTop;

        if (combinedMode) {
            this.oldView.draw(guiGraphics, boxLeft, boxTop, width, height);
            if (this.tabs.isEmpty()) guiGraphics.centeredText(this.font, NO_ADVANCEMENTS_LABEL, boxLeft + width / 2, boxTop + height / 2, -1);
            return;
        }

        if (betterAdvancementTab == null) {
            guiGraphics.fill(boxLeft, boxTop, boxRight, boxBottom, -16777216);
            guiGraphics.text(this.font, NO_ADVANCEMENTS_LABEL, boxLeft + (width - this.font.width(NO_ADVANCEMENTS_LABEL)) / 2, boxTop + height / 2 - this.font.lineHeight, -1);
            guiGraphics.text(this.font, VERY_SAD_LABEL, boxLeft + (width - this.font.width(VERY_SAD_LABEL)) / 2, boxTop + height / 2 + this.font.lineHeight, -1);
        } else {
            betterAdvancementTab.drawContents(guiGraphics, boxLeft, boxTop, width, height, TREE_SCALE);
        }
    }

    public void renderWindow(GuiGraphicsExtractor guiGraphics, int left, int top, int right, int bottom, int maxTabs, int skip) {
        if (combinedMode) {
            renderOldFrame(guiGraphics, left, top, right, bottom);
        } else {
            drawWindowFrame(guiGraphics, left, top, right, bottom);
        }

        int width = right - left;
        int height = bottom - top;

        if (!combinedMode && this.tabs.size() > 1) {
            for (BetterAdvancementTab tab : this.tabs.values().stream().skip(skip).limit(maxTabs).toList()) {
                tab.drawTab(guiGraphics, left, top, width, height, tab == this.selectedTab);
            }

            for (BetterAdvancementTab tab : this.tabs.values().stream().skip(skip).limit(maxTabs).toList()) {
                tab.drawIcon(guiGraphics, left, top, width, height);
            }
        }

        FormattedCharSequence windowTitle = combinedMode
            ? Component.translatable("improvedadvancements.achievements").getVisualOrderText()
            : TITLE.getVisualOrderText();
        if (!combinedMode && selectedTab != null) {
            if (keepAdvancementsTitle) {
                windowTitle = FormattedCharSequence.composite(
                    windowTitle,
                    Component.translatableWithFallback("betteradvancements.title_separator"," - ").getVisualOrderText(),
                    selectedTab.getTitle().getVisualOrderText()
                );
            } else {
                windowTitle = selectedTab.getTitle().getVisualOrderText();
            }

        }
        guiGraphics.text(this.font, windowTitle, left + 8, top + 6, -12566464, false);
        if (maxPages != 0 && !combinedMode) {
            Component page = Component.literal(String.format("%d / %d", tabPage + 1, maxPages + 1));
            guiGraphics.text(this.font, page, left + 8, bottom - 15, -12566464, false);
        }
        Component zoom = Component.literal(this.oldView.zoomPercent() + "%");
        int footerTextLeft = left + 56 + this.font.width(zoom) + 8;
        if (combinedMode && footerTextLeft < right - 88) {
            guiGraphics.text(this.font, zoom, left + 56, bottom + 10, -12566464, false);
        }
        if (showProgress && (combinedMode || this.selectedTab != null)) {
            int completed = 0;
            int total = combinedMode ? this.tabs.values().stream().mapToInt(tab -> tab.widgets.size()).sum() : this.selectedTab.widgets.size();
            Iterable<AdvancementHolder> holders = combinedMode
                ? this.tabs.values().stream().flatMap(tab -> tab.widgets.keySet().stream()).toList()
                : this.selectedTab.widgets.keySet();
            for (AdvancementHolder holder : holders) {
                AdvancementProgress progress = this.clientAdvancements.progress().get(holder);
                if (progress != null && progress.isDone()) completed++;
            }
            Component summary = Component.translatable("improvedadvancements.progress", completed, total,
                total == 0 ? 0 : Math.round(100.0F * completed / total));
            int summaryX = right - this.font.width(summary) - 8;
            if (summaryX > left + this.font.width(windowTitle) + 16) {
                guiGraphics.text(this.font, summary, summaryX, top + 6, -12566464, false);
            } else {
                if (!combinedMode) guiGraphics.text(this.font, summary, summaryX, bottom - 15, -12566464, false);
                else {
                    Component compact = Component.literal(completed + "/" + total);
                    int x = left + (right - left - this.font.width(compact)) / 2;
                    if (x >= footerTextLeft && x + this.font.width(compact) < right - 88)
                        guiGraphics.text(this.font, compact, x, bottom + 10, -12566464, false);
                }
            }
        }
    }

    private static void drawWindowFrame(GuiGraphicsExtractor g, int left, int top, int right, int bottom) {
        g.blit(RenderPipelines.GUI_TEXTURED, Resources.Gui.WINDOW, left, top, 0, 0, CORNER_SIZE, CORNER_SIZE, 256, 256);
        RenderUtil.renderRepeating(Resources.Gui.WINDOW, g, left + CORNER_SIZE, top, right - left - 2 * CORNER_SIZE, CORNER_SIZE, CORNER_SIZE, 0, WIDTH - 2 * CORNER_SIZE, CORNER_SIZE);
        g.blit(RenderPipelines.GUI_TEXTURED, Resources.Gui.WINDOW, right - CORNER_SIZE, top, WIDTH - CORNER_SIZE, 0, CORNER_SIZE, CORNER_SIZE, 256, 256);
        RenderUtil.renderRepeating(Resources.Gui.WINDOW, g, left, top + CORNER_SIZE, CORNER_SIZE, bottom - top - 2 * CORNER_SIZE, 0, CORNER_SIZE, CORNER_SIZE, HEIGHT - 2 * CORNER_SIZE);
        RenderUtil.renderRepeating(Resources.Gui.WINDOW, g, right - CORNER_SIZE, top + CORNER_SIZE, CORNER_SIZE, bottom - top - 2 * CORNER_SIZE, WIDTH - CORNER_SIZE, CORNER_SIZE, CORNER_SIZE, HEIGHT - 2 * CORNER_SIZE);
        g.blit(RenderPipelines.GUI_TEXTURED, Resources.Gui.WINDOW, left, bottom - CORNER_SIZE, 0, HEIGHT - CORNER_SIZE, CORNER_SIZE, CORNER_SIZE, 256, 256);
        RenderUtil.renderRepeating(Resources.Gui.WINDOW, g, left + CORNER_SIZE, bottom - CORNER_SIZE, right - left - 2 * CORNER_SIZE, CORNER_SIZE, CORNER_SIZE, HEIGHT - CORNER_SIZE, WIDTH - 2 * CORNER_SIZE, CORNER_SIZE);
        g.blit(RenderPipelines.GUI_TEXTURED, Resources.Gui.WINDOW, right - CORNER_SIZE, bottom - CORNER_SIZE, WIDTH - CORNER_SIZE, HEIGHT - CORNER_SIZE, CORNER_SIZE, CORNER_SIZE, 256, 256);
    }

    void drawSearchWindow(GuiGraphicsExtractor g, ModernScreenGeometry frame, Component heading) {
        var area = AdvancementSearchLayout.modern(frame);
        g.enableScissor(area.left(), area.top(), area.left() + area.width(), area.top() + area.height());
        g.pose().pushMatrix();
        g.pose().translate(area.left(), area.top());
        if (selectedTab != null) selectedTab.drawBackground(g, area.width(), area.height(), selectedTab.scrollX, selectedTab.scrollY);
        else g.fill(0, 0, area.width(), area.height(), 0xFF202020);
        g.pose().popMatrix();
        g.disableScissor();
        g.nextStratum();
        drawWindowFrame(g, frame.left(), frame.top(), frame.right(), frame.bottom());
        g.text(font, font.plainSubstrByWidth(heading.getString(), frame.right() - frame.left() - 16), frame.left() + 8, frame.top() + 6, 0xFF404040, false);
    }

    private void renderToolTips(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, int left, int top, int right, int bottom, int maxTabs, int skip) {
        if (combinedMode) {
            this.oldView.drawTooltip(guiGraphics, mouseX, mouseY, left + PADDING, top + 2 * PADDING, mapWidth(), mapHeight());
            return;
        }
        if (this.selectedTab != null) {
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(left + PADDING, top + 2*PADDING);
            this.selectedTab.drawToolTips(guiGraphics,mouseX - left - PADDING, mouseY - top - 2*PADDING, left, top, right - left - 2*PADDING, bottom - top - 3*PADDING, TREE_SCALE);
            guiGraphics.pose().popMatrix();
        }

        int width = right - left;
        int height = bottom - top;

        if (this.tabs.size() > 1) {
            for (BetterAdvancementTab tab : this.tabs.values().stream().skip(skip).limit(maxTabs).toList()) {
                if (tab.isMouseOver(left, top, width, height, mouseX, mouseY)) {
                    guiGraphics.setTooltipForNextFrame(this.font, tab.getTitle(), mouseX, mouseY);
                    if (tab != selectedTab) guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
                }
            }
        }
    }

    public NavigationTabLayout.Rect navigationFrame() {
        int left = SIDE + (width - internalWidth) / 2;
        return new NavigationTabLayout.Rect(left, windowTop(), internalWidth - 2 * SIDE, windowBottom() - windowTop());
    }

    public List<NavigationTabLayout.Rect> navigationObstacles() {
        var frame = navigationFrame();
        int maxTabs = BetterAdvancementTabType.getMaxTabs(frame.width(), frame.height());
        return tabs.size() <= 1 ? List.of() : tabs.values().stream().skip((long) tabPage * maxTabs).limit(maxTabs)
            .map(tab -> tab.tabBounds(frame.x(), frame.y(), frame.width(), frame.height())).toList();
    }

    private int mapWidth() {
        return Math.max(1, this.internalWidth - 2 * SIDE - 2 * PADDING);
    }

    private int windowTop() {
        return TOP + (this.height - this.internalHeight) / 2 - (combinedMode ? OLD_VERTICAL_OFFSET : 0);
    }

    private int windowBottom() {
        return this.internalHeight - BOTTOM + (this.height - this.internalHeight) / 2 - (combinedMode ? OLD_VERTICAL_OFFSET : 0);
    }

    private int mapHeight() {
        return Math.max(1, this.internalHeight - TOP - BOTTOM - 3 * PADDING);
    }

    private boolean isInsideMap(double x, double y) {
        int left = SIDE + (this.width - internalWidth) / 2 + PADDING;
        int top = windowTop() + 2 * PADDING;
        return x >= left && y >= top && x < left + mapWidth() && y < top + mapHeight();
    }

    private static void renderOldFrame(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom) {
        int frameBottom = bottom + OLD_FOOTER_HEIGHT;
        int gray = 0xFFC6C6C6;
        // Leave the outer corner pixels transparent, matching vanilla's stepped bevel.
        graphics.fill(left + 4, top, right - 4, top + 2 * PADDING, gray);
        graphics.fill(left, top + 4, left + PADDING, frameBottom - 4, gray);
        graphics.fill(right - PADDING, top + 4, right, frameBottom - 4, gray);
        graphics.fill(left + 4, bottom - PADDING, right - 4, frameBottom, gray);
        graphics.fill(left + 2, top + 1, left + 4, top + 4, gray);
        graphics.fill(left + 1, top + 2, left + 2, top + 4, gray);
        graphics.fill(right - 4, top + 1, right - 2, top + 4, gray);
        graphics.fill(right - 2, top + 2, right - 1, top + 4, gray);
        graphics.fill(left + 2, frameBottom - 4, left + 4, frameBottom - 1, gray);
        graphics.fill(left + 1, frameBottom - 4, left + 2, frameBottom - 2, gray);
        graphics.fill(right - 4, frameBottom - 4, right - 2, frameBottom - 1, gray);
        graphics.fill(right - 2, frameBottom - 4, right - 1, frameBottom - 2, gray);
        graphics.fill(left + 4, top, right - 4, top + 2, 0xFFFFFFFF);
        graphics.fill(left + 2, top + 1, left + 4, top + 3, 0xFFFFFFFF);
        graphics.fill(left + 1, top + 2, left + 3, top + 4, 0xFFFFFFFF);
        graphics.fill(left, top + 4, left + 2, frameBottom - 4, 0xFFFFFFFF);
        graphics.fill(right - 4, top + 1, right - 2, top + 3, 0xFF555555);
        graphics.fill(right - 3, top + 2, right - 1, top + 4, 0xFF555555);
        graphics.fill(right - 2, top + 4, right, frameBottom - 4, 0xFF555555);
        graphics.fill(left + 1, frameBottom - 4, left + 3, frameBottom - 2, 0xFF555555);
        graphics.fill(left + 2, frameBottom - 3, left + 4, frameBottom - 1, 0xFF555555);
        graphics.fill(right - 3, frameBottom - 4, right - 1, frameBottom - 2, 0xFF555555);
        graphics.fill(right - 4, frameBottom - 3, right - 2, frameBottom - 1, 0xFF555555);
        graphics.fill(left + 4, frameBottom - 2, right - 4, frameBottom, 0xFF555555);
        graphics.outline(left + PADDING - 1, top + 2 * PADDING - 1,
            right - left - 2 * PADDING + 2, bottom - top - 3 * PADDING + 2, 0xFF292929);
    }

    @Override
    public void onAdvancementsUpdated() {
        AdvancementHolder previouslySelected = this.selectedTab == null ? null : this.selectedTab.getRootNode().holder();
        if (this.selectedTab != null) {
            this.selectedTab.storeScroll();
        }

        this.tabs.clear();
        AdvancementTree tree = this.clientAdvancements.tree();
        for (AdvancementNode advancement : StreamSupport.stream(tree.roots().spliterator(), false)
            .sorted(Comparator.comparing(node -> node.holder().id().toString())).toList()) {
            BetterAdvancementTab tab = BetterAdvancementTab.create(this.minecraft, this, this.tabs.size(), advancement, internalWidth - 2 * SIDE, internalHeight - TOP - SIDE);
            if (tab != null) {
                this.tabs.put(advancement.holder(), tab);
            }
        }

        for (AdvancementNode advancement : StreamSupport.stream(tree.tasks().spliterator(), false)
            .sorted(Comparator.comparing(node -> node.holder().id().toString())).toList()) {
            BetterAdvancementTab tab = this.getTab(advancement);
            if (tab != null) {
                tab.addAdvancement(advancement);
            }
        }
        if (stableLayout) this.tabs.values().forEach(BetterAdvancementTab::applyStableLayout);
        this.clientAdvancements.progress().forEach((holder, progress) -> {
            AdvancementNode advancement = tree.get(holder);
            if (advancement != null) {
                BetterAdvancementWidget widget = this.getAdvancementWidget(advancement);
                if (widget != null) {
                    widget.getAdvancementProgress(progress);
                }
            }
        });
        this.oldView.rebuild(this.tabs.values());

        this.selectedTab = this.tabs.get(previouslySelected);
        if (this.selectedTab == null && rememberPosition && AdvancementViewState.selectedTab != null) {
            for (BetterAdvancementTab tab : this.tabs.values()) {
                if (tab.getRootNode().holder().id().toString().equals(AdvancementViewState.selectedTab)) {
                    this.selectedTab = tab;
                    break;
                }
            }
        }
        if (this.selectedTab == null && !this.tabs.isEmpty()) {
            this.selectedTab = this.tabs.values().iterator().next();
            this.clientAdvancements.setSelectedTab(this.selectedTab.getRootNode().holder(), true);
        }
        if (this.selectedTab != null) {
            this.selectedTab.loadScroll(internalWidth - 2 * SIDE - 2 * PADDING, internalHeight - TOP - BOTTOM - 3 * PADDING);
            int maxTabs = Math.max(1, BetterAdvancementTabType.getMaxTabs(internalWidth - 2 * SIDE, internalHeight - TOP - BOTTOM));
            tabPage = this.selectedTab.getIndex() / maxTabs;
            this.clientAdvancements.setSelectedTab(this.selectedTab.getRootNode().holder(), false);
        }
    }

    @Override
    public void onSelectedTabChanged(AdvancementHolder advancement) {
        BetterAdvancementTab nextTab = this.tabs.get(advancement);
        if (this.selectedTab == nextTab) {
            return;
        }
        if (this.selectedTab != null) {
            this.selectedTab.storeScroll();
        }
        this.selectedTab = nextTab;
        if (this.selectedTab != null) {
            this.selectedTab.loadScroll(internalWidth - 2 * SIDE - 2 * PADDING, internalHeight - TOP - BOTTOM - 3 * PADDING);
            if (rememberPosition) AdvancementViewState.selectedTab = this.selectedTab.getRootNode().holder().id().toString();
        }
    }

    @Override
    public void onAdvancementsCleared() {
        this.tabs.clear();
        this.oldView.rebuild(this.tabs.values());
        this.selectedTab = null;
    }

    public BetterAdvancementWidget getAdvancementWidget(AdvancementNode advancement) {
        BetterAdvancementTab betterAdvancementTab = this.getTab(advancement);
        return betterAdvancementTab == null ? null : betterAdvancementTab.getWidget(advancement.holder());
    }

    private BetterAdvancementTab getTab(AdvancementNode advancement) {
        AdvancementNode advancementNode = advancement.root();
        return this.tabs.get(advancementNode.holder());
    }
}

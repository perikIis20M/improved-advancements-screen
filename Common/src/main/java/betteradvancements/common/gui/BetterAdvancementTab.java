package betteradvancements.common.gui;

import betteradvancements.common.advancements.BetterDisplayInfo;
import betteradvancements.common.advancements.BetterDisplayInfoRegistry;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.ClientAsset;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Optional;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

public class BetterAdvancementTab {
    public static boolean doFade = true;

    private final Minecraft minecraft;
    private final BetterAdvancementsScreen screen;
    private final BetterAdvancementTabType type;
    private final int index;
    private final AdvancementNode rootNode;
    private final DisplayInfo display;
    private final ItemStack icon;
    private final Component title;
    private final BetterAdvancementWidget root;
    protected final Map<AdvancementHolder, BetterAdvancementWidget> widgets = Maps.newLinkedHashMap();
    private final BetterDisplayInfoRegistry betterDisplayInfos;

    protected int scrollX, scrollY;
    private int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
    private int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
    private float fade;
    private boolean centered;
    private BetterAdvancementWidget searchHighlight;
    private long searchHighlightUntil;

    public BetterAdvancementTab(Minecraft mc, BetterAdvancementsScreen betterAdvancementsScreen, BetterAdvancementTabType type, int index, AdvancementNode advancementNode, DisplayInfo displayInfo) {
        this.minecraft = mc;
        this.screen = betterAdvancementsScreen;
        this.type = type;
        this.index = index;
        this.rootNode = advancementNode;
        this.display = displayInfo;
        this.icon = displayInfo.icon().create();
        this.title = displayInfo.title();
        this.betterDisplayInfos = new BetterDisplayInfoRegistry(advancementNode);
        this.root = new BetterAdvancementWidget(this, mc, advancementNode, displayInfo);
        this.addWidget(this.root, advancementNode.holder());
    }

    public AdvancementNode getRootNode() {
        return this.rootNode;
    }

    public Component getTitle() {
        return this.title;
    }

    public int getIndex() {
        return this.index;
    }

    public int minX() { return this.minX; }
    public int minY() { return this.minY; }
    public int treeWidth() { return this.maxX - this.minX; }
    public int treeHeight() { return this.maxY - this.minY; }

    NavigationTabLayout.Rect tabBounds(int left, int top, int width, int height) {
        return type.bounds(left, top, width, height, index);
    }

    public void drawTab(GuiGraphicsExtractor guiGraphics, int left, int top, int width, int height, boolean selected) {
        this.type.draw(guiGraphics, left, top, width, height, selected, this.index);
    }

    public void drawIcon(GuiGraphicsExtractor guiGraphics, int left, int top,int width, int height) {
        this.type.drawIcon(guiGraphics, left, top, width, height, this.index, this.icon);
    }

    public void drawContents(GuiGraphicsExtractor guiGraphics, int left, int top, int width, int height, float zoom) {
        if (!this.centered) {
            this.scrollX = (width - (this.maxX + this.minX)) / 2;
            this.scrollY = (height - (this.maxY + this.minY)) / 2;
            this.centered = true;
        }
        scroll(0, 0, width, height);

        guiGraphics.enableScissor(left, top, left + width, top + height);
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(left, top);
        this.drawBackground(guiGraphics, width, height, this.scrollX, this.scrollY);
        guiGraphics.pose().scale(zoom, zoom);
        this.drawTree(guiGraphics, this.scrollX, this.scrollY);
        guiGraphics.pose().popMatrix();
        guiGraphics.disableScissor();
    }

    public void drawBackground(GuiGraphicsExtractor guiGraphics, int width, int height, int offsetX, int offsetY) {
        Identifier resourcelocation = this.display.background().map(ClientAsset.ResourceTexture::texturePath).orElse(MissingTextureAtlasSprite.getLocation());

        int i = offsetX % 16;
        int j = offsetY % 16;

        int k = -1;
        for (; k <= 1 + width / 16; k++) {
            int l = -1;
            for (;l <= height / 16; l++) {
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, resourcelocation, i + 16 * k, j + 16 * l, 0.0F, 0.0F, 16, 16, 16, 16);
            }
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, resourcelocation, i + 16 * k, j + 16 * l, 0.0F, 0.0F, 16, height % 16, 16, 16);
        }

    }

    public void drawTree(GuiGraphicsExtractor guiGraphics, int offsetX, int offsetY) {
        this.root.drawConnectivity(guiGraphics, offsetX, offsetY, true);
        this.root.drawConnectivity(guiGraphics, offsetX, offsetY, false);
        this.root.draw(guiGraphics, offsetX, offsetY);
        if (searchHighlight != null) AdvancementSearchScreen.highlight(guiGraphics,
            offsetX + searchHighlight.x + 2, offsetY + searchHighlight.y - 1, 28, searchHighlightUntil);
    }

    void focusSearchResult(BetterAdvancementWidget widget, int width, int height) {
        if (!centered) {
            scrollX = AdvancementScrollBounds.center(minX, maxX, width);
            scrollY = AdvancementScrollBounds.center(minY, maxY, height);
        }
        centered = true;
        scrollX = AdvancementScrollBounds.reveal(scrollX, widget.x, widget.x + 28, minX, maxX, width);
        scrollY = AdvancementScrollBounds.reveal(scrollY, widget.y, widget.y + 27, minY, maxY, height);
        searchHighlight = widget;
        searchHighlightUntil = System.currentTimeMillis() + 4000;
    }

    public boolean drawCombinedToolTip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY,
                                       int left, int top, int offsetX, int offsetY) {
        for (BetterAdvancementWidget widget : this.widgets.values()) {
            if (widget.isMouseOver(offsetX, offsetY, mouseX, mouseY, 1.0F)) {
                widget.drawHover(guiGraphics, offsetX, offsetY, 1.0F, left, top, 1.0F);
                return true;
            }
        }
        return false;
    }

    public void drawToolTips(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, int left, int top, int width, int height, float zoom) {
        guiGraphics.fill(0, 0, width, height, Mth.floor(this.fade * 255.0F) << 24);
        boolean flag = false;

        if (mouseX > 0 && mouseX < width && mouseY > 0 && mouseY < height) {
            for (BetterAdvancementWidget betterAdvancementWidget : this.widgets.values()) {
                if (betterAdvancementWidget.isMouseOver(this.scrollX, this.scrollY, mouseX, mouseY, zoom)) {
                    flag = true;
                    betterAdvancementWidget.drawHover(guiGraphics, this.scrollX, this.scrollY, this.fade, left, top, zoom);
                    break;
                }
            }
        }

        if (doFade && flag) {
            this.fade = Mth.clamp(this.fade + 0.02F, 0.0F, 0.3F);
        } else {
            this.fade = Mth.clamp(this.fade - 0.04F, 0.0F, 1.0F);
        }
    }

    public boolean isMouseOver(int left, int top, int width, int height, double mouseX, double mouseY) {
        return this.type.isMouseOver(left, top, width, height, this.index, mouseX, mouseY);
    }

    public static BetterAdvancementTab create(Minecraft mc, BetterAdvancementsScreen betterAdvancementsScreen, int index, AdvancementNode advancementNode, int width, int height) {
        Optional<DisplayInfo> optional = advancementNode.advancement().display();
        if (optional.isEmpty()) {
            return null;
        } else {
            BetterAdvancementTabType advancementTabType = BetterAdvancementTabType.getTabType(width, height, index);
            if (advancementTabType == null) {
                return null;
            } else {
                return new BetterAdvancementTab(mc, betterAdvancementsScreen, advancementTabType, index, advancementNode, optional.get());
            }

        }
    }

    public void scroll(double scrollX, double scrollY, int width, int height) {
        this.scrollX = AdvancementScrollBounds.constrain(this.scrollX + scrollX, minX, maxX, width);
        this.scrollY = AdvancementScrollBounds.constrain(this.scrollY + scrollY, minY, maxY, height);
    }

    public void addAdvancement(AdvancementNode advancementNode) {
        Optional<DisplayInfo> optional = advancementNode.advancement().display();
        if (optional.isPresent()) {
            BetterAdvancementWidget betterAdvancementEntryScreen = new BetterAdvancementWidget(this, this.minecraft, advancementNode, optional.get());
            this.addWidget(betterAdvancementEntryScreen, advancementNode.holder());
        }
    }

    private void addWidget(BetterAdvancementWidget betterAdvancementEntryScreen, AdvancementHolder advancementHolder) {
        this.widgets.put(advancementHolder, betterAdvancementEntryScreen);
        int left = betterAdvancementEntryScreen.getX();
        int right = left + 28;
        int top = betterAdvancementEntryScreen.getY();
        int bottom = top + 27;
        this.minX = Math.min(this.minX, left);
        this.maxX = Math.max(this.maxX, right);
        this.minY = Math.min(this.minY, top);
        this.maxY = Math.max(this.maxY, bottom);

        for (BetterAdvancementWidget gui : this.widgets.values()) {
            gui.attachToParent();
        }
    }

    public BetterAdvancementWidget getWidget(AdvancementHolder advancementHolder) {
        return this.widgets.get(advancementHolder);
    }

    public BetterAdvancementsScreen getScreen() {
        return this.screen;
    }

    public BetterDisplayInfo getBetterDisplayInfo(AdvancementNode advancementNode) {
        return betterDisplayInfos.get(advancementNode.holder());
    }

    public void storeScroll() {
        if (this.centered && BetterAdvancementsScreen.rememberPosition) {
            AdvancementViewState.scrollByTab.put(this.rootNode.holder().id().toString(), Pair.of(scrollX, scrollY));
        }
    }

    public void loadScroll(int width, int height) {
        if (!BetterAdvancementsScreen.rememberPosition) return;
        Pair<Integer, Integer> scroll = AdvancementViewState.scrollByTab.get(this.rootNode.holder().id().toString());
        if (scroll != null) {
            this.centered = true;
            this.scrollX = scroll.getFirst();
            this.scrollY = scroll.getSecond();

            scroll(0, 0, width, height);
        }
    }

    private String layoutKey() {
        return this.rootNode.holder().id() + ":" + this.widgets.keySet().stream()
            .map(holder -> holder.id().toString()).sorted().collect(Collectors.joining("|"));
    }

    public void applyStableLayout() {
        if (!BetterAdvancementsScreen.stableLayout) return;
        Map<String, Pair<Integer, Integer>> saved = AdvancementViewState.positionsByLayout.get(layoutKey());
        if (saved == null || saved.size() != this.widgets.size()) {
            rememberStableLayout();
            return;
        }
        this.minX = this.minY = Integer.MAX_VALUE;
        this.maxX = this.maxY = Integer.MIN_VALUE;
        for (Map.Entry<AdvancementHolder, BetterAdvancementWidget> entry : this.widgets.entrySet()) {
            Pair<Integer, Integer> position = saved.get(entry.getKey().id().toString());
            BetterAdvancementWidget widget = entry.getValue();
            if (position != null) {
                widget.x = position.getFirst();
                widget.y = position.getSecond();
            }
            this.minX = Math.min(this.minX, widget.x);
            this.maxX = Math.max(this.maxX, widget.x + 28);
            this.minY = Math.min(this.minY, widget.y);
            this.maxY = Math.max(this.maxY, widget.y + 27);
        }
    }

    public void rememberStableLayout() {
        if (!BetterAdvancementsScreen.stableLayout) return;
        Map<String, Pair<Integer, Integer>> positions = new LinkedHashMap<>();
        this.widgets.forEach((holder, widget) -> positions.put(holder.id().toString(), Pair.of(widget.x, widget.y)));
        AdvancementViewState.positionsByLayout.put(layoutKey(), positions);
    }
}

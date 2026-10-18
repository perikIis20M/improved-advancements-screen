package betteradvancements.common.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementWidgetType;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import java.util.*;
import java.util.function.Consumer;

/** Native Minecraft widgets; independent implementation of advancement search and jump-to-result. */
public final class AdvancementSearchScreen extends Screen {
    public record Result(AdvancementSearchIndex.Entry entry, ItemStack icon, AdvancementType frame, Component tooltip) { }
    private final Screen parent;
    private final Map<String, Result> catalogue = new LinkedHashMap<>();
    private final Consumer<String> select;
    private final List<Button> resultButtons = new ArrayList<>();
    private List<AdvancementSearchIndex.Entry> matches = List.of();
    private EditBox input;
    private Button previous, next;
    private AdvancementSearchLayout layout;
    private LegacyScreenGeometry classicFrame;
    private ModernScreenGeometry modernFrame;
    private String query = "";
    private int page;

    public AdvancementSearchScreen(Screen parent, List<Result> entries, Consumer<String> select) {
        super(Component.translatable("improvedadvancements.search.title"));
        this.parent = parent;
        entries.forEach(result -> { if (result.entry().revealed()) catalogue.put(result.entry().id(), result); });
        this.select = select;
    }

    @Override protected void init() {
        if (parent instanceof LegacyAchievementsScreen) {
            classicFrame = LegacyScreenGeometry.create(width, height, BetterAdvancementsScreen.oldScreenSize);
            layout = AdvancementSearchLayout.classic(classicFrame);
        } else {
            modernFrame = ModernScreenGeometry.create(width, height, BetterAdvancementsScreen.windowScale);
            layout = AdvancementSearchLayout.modern(modernFrame);
        }
        resultButtons.clear();
        input = addRenderableWidget(new EditBox(font, layout.fieldX(), layout.fieldY(), layout.fieldWidth(), 16,
            Component.translatable("improvedadvancements.search.title")));
        input.setHint(Component.translatable("improvedadvancements.search.hint"));
        input.setMaxLength(128);
        input.setValue(query);
        addRenderableWidget(Button.builder(Component.literal("×"), button -> {
            input.setValue(""); setFocused(input);
        }).bounds(layout.clearX(), layout.fieldY(), 18, 16)
            .tooltip(Tooltip.create(Component.translatable("improvedadvancements.search.clear"))).build());
        previous = addRenderableWidget(footerButton(20, 24, layout.left(), 24, Component.literal("<"), button -> changePage(-1)));
        next = addRenderableWidget(footerButton(48, 24, layout.left() + 28, 24, Component.literal(">"), button -> changePage(1)));
        int backWidth = classicFrame == null ? 80 : Math.min(80, classicFrame.width() - 108);
        int backX = classicFrame == null ? 152 : classicFrame.rightButtonX(backWidth) - classicFrame.left();
        addRenderableWidget(footerButton(backX, backWidth, layout.left() + layout.width() - 60, 60,
            Component.translatable("gui.back"), button -> onClose()));
        input.setResponder(value -> { query = value; page = 0; refresh(); });
        refresh();
        setInitialFocus(input);
    }

    private Button footerButton(int classicX, int classicWidth, int x, int width, Component label, Button.OnPress press) {
        int buttonX = classicFrame == null ? x : Math.round(classicFrame.screenX(classicX));
        int buttonWidth = classicFrame == null ? width : classicWidth;
        return Button.builder(label, press).bounds(buttonX, layout.footerY(), buttonWidth, 20).build();
    }

    private void refresh() {
        resultButtons.forEach(this::removeWidget);
        resultButtons.clear();
        matches = AdvancementSearchIndex.search(catalogue.values().stream().map(Result::entry).toList(), query);
        page = Math.max(0, Math.min(page, layout.pageCount(matches.size()) - 1));
        int start = page * layout.pageSize();
        for (int i = start; i < Math.min(matches.size(), start + layout.pageSize()); i++) {
            var result = catalogue.get(matches.get(i).id());
            resultButtons.add(addRenderableWidget(new ResultButton(layout.cellX(i - start), layout.cellY(i - start),
                layout.cellWidth(), layout.cellHeight(), result, button -> choose(result.entry().id()))));
        }
        previous.active = page > 0;
        next.active = page + 1 < layout.pageCount(matches.size());
        var summary = Tooltip.create(Component.translatable("improvedadvancements.search.results", matches.size(), page + 1,
            layout.pageCount(matches.size())));
        previous.setTooltip(summary);
        next.setTooltip(summary);
    }

    private void choose(String id) {
        minecraft.setScreenAndShow(parent);
        select.accept(id); // Parent has rebuilt its current server data before the camera moves.
    }

    private void changePage(int delta) {
        page = Math.max(0, Math.min(page + delta, layout.pageCount(matches.size()) - 1));
        refresh();
    }

    @Override public void onClose() { minecraft.setScreenAndShow(parent); }

    @Override public boolean keyPressed(KeyEvent event) {
        if (isSearchShortcut(event)) { setFocused(input); return true; }
        if (event.isConfirmation() && input.isFocused() && !matches.isEmpty()) {
            choose(matches.get(page * layout.pageSize()).id()); return true;
        }
        return super.keyPressed(event);
    }

    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
        if (dy == 0 || !layout.insideResults(x, y)) return false;
        changePage(dy < 0 ? 1 : -1);
        return true;
    }

    public static boolean isSearchShortcut(KeyEvent event) {
        return event.hasControlDownWithQuirk() && !event.hasAltDown() && event.shortcutKey() == 'f';
    }

    public static void highlight(GuiGraphicsExtractor graphics, int x, int y, int size, long until) {
        long remaining = until - System.currentTimeMillis();
        if (remaining <= 0) return;
        int alpha = (int) (175 + 80 * (0.5 + 0.5 * Math.sin(remaining / 180.0)));
        graphics.outline(x, y, size, size, (alpha << 24) | 0xFFFF80);
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        if (parent instanceof LegacyAchievementsScreen) graphics.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);
        else super.extractBackground(graphics, mouseX, mouseY, partialTicks);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        if (parent instanceof LegacyAchievementsScreen old) old.drawSearchWindow(graphics, classicFrame, title);
        else ((BetterAdvancementsScreen) parent).drawSearchWindow(graphics, modernFrame, title);
        graphics.nextStratum();
        // Darken only the canvas, leaving the original window and terrain visible behind search.
        graphics.fill(layout.left(), layout.top(), layout.left() + layout.width(), layout.top() + layout.height(), 0x38000000);
        graphics.nextStratum();
        if (matches.isEmpty()) {
            int y = layout.gridTop() + 2;
            for (var line : font.split(Component.translatable("improvedadvancements.search.none"), layout.width() - 8)) {
                if (y + font.lineHeight > layout.gridBottom()) break;
                graphics.text(font, line, layout.left() + 4, y, 0xFFFFFFFF, true);
                y += font.lineHeight;
            }
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        String pages = (page + 1) + "/" + layout.pageCount(matches.size());
        if (classicFrame != null) {
            int left = classicFrame.left() + 76;
            int right = classicFrame.rightButtonX(Math.min(80, classicFrame.width() - 108)) - 4;
            if (right - left >= font.width(pages))
                graphics.centeredText(font, pages, (left + right) / 2, layout.footerY() + 6, 0xFF404040);
        } else {
            int left = layout.left() + 56, right = layout.left() + layout.width() - 64;
            graphics.centeredText(font, fitLabel(pages, right - left), (left + right) / 2, layout.footerY() + 6, 0xFFFFFFFF);
        }
    }

    private String fitLabel(String name, int width) {
        if (font.width(name) <= width) return name;
        return font.plainSubstrByWidth(name, Math.max(0, width - font.width("..."))) + "...";
    }

    private final class ResultButton extends Button {
        private final Result result;
        ResultButton(int x, int y, int width, int height, Result result, OnPress press) {
            super(x, y, width, height, Component.literal(result.entry().title()), press, DEFAULT_NARRATION);
            this.result = result;
            setTooltip(Tooltip.create(result.tooltip()));
        }
        @Override protected MutableComponent createNarrationMessage() { return result.tooltip().copy(); }
        @Override protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
            boolean compact = layout.compact();
            int size = compact ? Math.min(26, getHeight() - 2) : 26;
            int x = compact ? getX() + 1 : getX() + (getWidth() - size) / 2;
            int y = getY() + 1;
            graphics.pose().pushMatrix();
            graphics.pose().translate(x, y);
            graphics.pose().scale(size / 26.0F);
            if (classicFrame != null) LegacyAchievementsScreen.drawAchievementIcon(graphics, 0, 0, result.frame(),
                result.icon(), result.entry().completed(), result.entry().detailsVisible(), 1);
            else {
                var state = result.entry().completed() ? AdvancementWidgetType.OBTAINED : AdvancementWidgetType.UNOBTAINED;
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, state.frameSprite(result.frame()), 0, 0, 26, 26);
                graphics.fakeItem(result.icon(), 5, 5);
            }
            graphics.pose().popMatrix();
            if (isHoveredOrFocused()) graphics.outline(x - 1, y - 1, size + 2, size + 2, 0xFFFFFFA0);
            int labelX = compact ? x + size + 4 : getX() + 2;
            int labelY = compact ? getY() + (getHeight() - font.lineHeight) / 2 : getY() + 30;
            int labelWidth = getX() + getWidth() - labelX - 2;
            String label = fitLabel(result.entry().title(), labelWidth);
            if (!compact) labelX += (labelWidth - font.width(label)) / 2;
            graphics.fill(labelX - 1, labelY - 1, labelX + font.width(label) + 1, labelY + font.lineHeight + 1, 0x70000000);
            int color = !result.entry().detailsVisible() ? 0xFFAAAAAA : result.frame() == AdvancementType.CHALLENGE ? 0xFFFFFF80 : 0xFFFFFFFF;
            graphics.text(font, label, labelX, labelY, color, true);
        }
    }

    public static Component tooltip(AdvancementSearchIndex.Entry entry, Component details) {
        var text = Component.literal(entry.title()).withStyle(ChatFormatting.WHITE);
        if (!details.getString().isBlank()) text.append(Component.literal("\n").append(details.copy().withStyle(ChatFormatting.GRAY)));
        if (entry.completed()) text.append(Component.literal("\n").append(Component.translatable("improvedadvancements.classic.taken")
            .withStyle(ChatFormatting.GREEN)));
        else if (entry.detailsVisible() && entry.progress() > 0) text.append(Component.literal("\n" + (int) (entry.progress() * 100) + "%")
            .withStyle(ChatFormatting.GRAY));
        return text;
    }
}

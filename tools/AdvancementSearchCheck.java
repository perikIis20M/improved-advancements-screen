package betteradvancements.common.gui;

import java.util.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.InputQuirks;
import net.minecraft.network.chat.Component;
import betteradvancements.common.gui.AdvancementSearchIndex.Entry;

/** Exercises the shipped search, native shortcut events, spoiler policy and result geometry. */
public final class AdvancementSearchCheck {
    private static int assertions;
    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Entry diamond = new Entry("diamond", "Diamonds!", "Acquire diamonds", "Diamond", true, true, false, 0);
        Entry adventure = new Entry("adventure", "Adventuring Time", "Discover every biome", "Diamond Boots", true, true, false, 0.8F);
        Entry locked = new Entry("locked", "A Locked Goal", "secret description", "Golden Apple", true, false, false, 0);
        Entry hidden = new Entry("hidden", "Secret hidden title", "secret", "Diamond", false, false, false, 0);
        Entry complete = new Entry("complete", "Diamonds!", "Acquire diamonds", "Diamond", true, true, true, 1);
        Entry greek = new Entry("greek", "Περιπέτεια", "Εξερεύνηση", "Χάρτης", true, true, false, 0);
        var all = List.of(diamond, adventure, locked, hidden, complete, greek);
        require(ids(all, "DIAMONDS!").equals(List.of("diamond", "complete")), "Case-insensitive title and completed ordering");
        require(ids(all, "biome").equals(List.of("adventure")), "Description search");
        require(ids(all, "boots").equals(List.of("adventure")), "Icon name search");
        require(ids(all, "  discover   boots ").equals(List.of("adventure")), "Multiple words across fields");
        require(ids(all, "discover missing").isEmpty(), "All query words must match");
        require(ids(all, "secret").isEmpty(), "Hidden title or locked description leaked");
        require(ids(all, "apple").equals(List.of("locked")), "Known locked icon must remain searchable");
        require(ids(all, "ΠΕΡΙΠΕΤΕΙΑ").equals(List.of("greek")), "Greek case and accents");
        require(ids(all, "diamonds!").indexOf("complete") > ids(all, "diamonds!").indexOf("diamond"), "Completed not last");
        require(ids(all, " ").size() == 5 && !ids(all, " ").contains("hidden"), "Blank search reveals hidden goal");
        require(ids(all, "").getFirst().equals("adventure"), "Blank search prefers progress");
        require(ids(all, "diamond").getFirst().equals("diamond"), "Title match precedes item-only match");
        for (int seed = 0; seed < 30; seed++) {
            var shuffled = new ArrayList<>(all);
            Collections.shuffle(shuffled, new Random(seed));
            require(ids(all, "").equals(ids(shuffled, "")), "Server order changes search order");
        }
        var locale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            require(ids(all, "DIAMONDS!").equals(List.of("diamond", "complete")), "Locale-dependent matching");
        } finally { Locale.setDefault(locale); }
        var badProgress = List.of(new Entry("nan", "Example", "", "", true, true, false, Float.NaN),
            new Entry("normal", "Example", "", "", true, true, false, 0.1F));
        require(ids(badProgress, "").getFirst().equals("normal"), "Invalid progress ranks first");
        String lockedTip = AdvancementSearchScreen.tooltip(locked, Component.literal("Requires prerequisite")).getString();
        require(lockedTip.contains("Requires prerequisite") && !lockedTip.contains("secret description"), "Locked tooltip leaked details");
        String partialTip = AdvancementSearchScreen.tooltip(adventure, Component.literal(adventure.description())).getString();
        require(partialTip.contains("80%") && partialTip.contains("Discover every biome"), "Partial result tooltip missing details");
        int ctrl = InputQuirks.EDIT_SHORTCUT_KEY_MODIFIER;
        require(AdvancementSearchScreen.isSearchShortcut(new KeyEvent(9, 'f', ctrl)), "Native Ctrl+F not recognized");
        require(!AdvancementSearchScreen.isSearchShortcut(new KeyEvent(9, 'f', 0)), "Plain F opens search");
        require(!AdvancementSearchScreen.isSearchShortcut(new KeyEvent(9, 'f', ctrl | 0x100)), "AltGr opens search");
        require(!AdvancementSearchScreen.isSearchShortcut(new KeyEvent(15, 'l', ctrl)), "Other key opens search");
        require(new KeyEvent(40, 13, 0).isConfirmation(), "SDL Enter mapping");
        require(new KeyEvent(41, 27, 0).isEscape(), "SDL Escape mapping");
        for (int[] viewport : new int[][]{{160, 120}, {320, 240}, {427, 241}, {640, 360}, {853, 479}, {1280, 720}}) {
            for (int size = 60; size <= 100; size++) {
                var frame = LegacyScreenGeometry.create(viewport[0], viewport[1], size);
                var layout = AdvancementSearchLayout.classic(frame);
                require(Math.abs(layout.left() - frame.screenX(LegacyScreenGeometry.MAP_LEFT)) < 1.01, "Classic search moved horizontally");
                require(Math.abs(layout.top() - frame.screenY(LegacyScreenGeometry.MAP_TOP)) < 1.01, "Classic search moved vertically");
                checkLayout(layout, viewport);
            }
            if (viewport[0] >= 320 && viewport[1] >= 240) for (int size = 60; size <= 100; size++) {
                var frame = ModernScreenGeometry.create(viewport[0], viewport[1], size);
                require(frame.internalWidth() == Math.min(viewport[0], Math.max(252, viewport[0] * size * 80 / 10000)), "Modern window width changed");
                require(frame.internalHeight() == Math.min(viewport[1], Math.max(140, viewport[1] * size / 100)), "Modern window height changed");
                checkLayout(AdvancementSearchLayout.modern(frame), viewport);
            }
        }
        System.out.println("PASS: " + assertions + " search checks: matching, spoilers, ordering, locale, native keys, tooltips and labels/grid/input bounds inside both parent frames at every configured size.");
    }

    private static void checkLayout(AdvancementSearchLayout layout, int[] viewport) {
            require(layout.left() >= 0 && layout.left() + layout.width() <= viewport[0], "Search width clips");
            require(layout.top() >= 0 && layout.top() + layout.height() <= viewport[1], "Search height clips");
            require(layout.footerY() < viewport[1], "Footer clips");
            require(layout.fieldX() >= layout.left() && layout.clearX() + 18 <= layout.left() + layout.width(), "Field or clear outside window");
            require(layout.fieldWidth() >= 20 && layout.fieldY() + 16 < layout.gridTop(), "Field overlaps results");
            require(layout.fieldX() + layout.fieldWidth() < layout.clearX(), "Input overlaps clear button");
            require(layout.cellHeight() >= 20 && layout.columns() > 0 && layout.rows() > 0, "Invalid result size");
            for (int i = 0; i < layout.pageSize(); i++) {
                int x = layout.cellX(i), y = layout.cellY(i);
                require(x >= layout.left() && x + layout.cellWidth() <= layout.left() + layout.width(), "Result outside grid");
                require(y >= layout.gridTop() && y + layout.cellHeight() <= layout.gridBottom(), "Result overlaps field or frame");
                if (i % layout.columns() != 0) require(layout.cellX(i - 1) + layout.cellWidth() < x, "Result columns overlap");
                int iconSize = layout.compact() ? Math.min(26, layout.cellHeight() - 2) : 26;
                require(iconSize >= 18 && iconSize + 2 <= layout.cellHeight(), "Advancement frame clips");
                if (!layout.compact()) require(30 + 9 + 1 <= layout.cellHeight(), "Name label clips below icon");
                else require(layout.cellWidth() - iconSize - 7 >= 20, "No room for compact name label");
                require(layout.insideResults(x + layout.cellWidth() / 2.0, y + layout.cellHeight() / 2.0), "Result wheel target mismatch");
            }
            for (int count : new int[]{0, 1, 126, 300, 5000}) {
                int pages = layout.pageCount(count);
                require(pages >= 1 && (long) pages * layout.pageSize() >= count, "Missing result pages");
                require(count == 0 || (long) (pages - 1) * layout.pageSize() < count, "Empty final page");
            }
    }
    private static List<String> ids(Collection<Entry> entries, String term) {
        return AdvancementSearchIndex.search(entries, term).stream().map(Entry::id).toList();
    }
    private static void require(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}

package betteradvancements.fabric.config;

import betteradvancements.common.gui.BetterAdvancementsScreen;
import betteradvancements.common.gui.BetterAdvancementsScreenButton;
import betteradvancements.common.gui.OldScreenVisibility;
import betteradvancements.common.gui.ScreenSize;
import betteradvancements.common.util.CriteriaDetail;
import betteradvancements.common.util.CriterionGrid;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

public final class ConfigValues {
    private ConfigValues() { }

    public static void buildModern(ConfigCategory category, ConfigEntryBuilder builder) {
        category.addEntry(builder.startBooleanToggle(label("navigation_tabs"), BetterAdvancementsScreenButton.addToInventory)
            .setDefaultValue(true).setTooltip(label("navigation_tabs.tip"))
            .setSaveConsumer(value -> BetterAdvancementsScreenButton.addToInventory = value).build());
        category.addEntry(builder.startIntSlider(label("scale"), ScreenSize.clamp(BetterAdvancementsScreen.windowScale), ScreenSize.MIN, ScreenSize.MAX)
            .setDefaultValue(ScreenSize.DEFAULT).setTooltip(label("scale.tip"))
            .setSaveConsumer(value -> BetterAdvancementsScreen.windowScale = ScreenSize.clamp(value)).build());
        category.addEntry(builder.startBooleanToggle(label("progress"), BetterAdvancementsScreen.showProgress)
            .setDefaultValue(true).setTooltip(label("progress.tip"))
            .setSaveConsumer(value -> BetterAdvancementsScreen.showProgress = value).build());
        category.addEntry(builder.startBooleanToggle(label("remember"), BetterAdvancementsScreen.rememberPosition)
            .setDefaultValue(true).setTooltip(label("remember.tip"))
            .setSaveConsumer(value -> BetterAdvancementsScreen.rememberPosition = value).build());
        category.addEntry(builder.startBooleanToggle(label("stable"), BetterAdvancementsScreen.stableLayout)
            .setDefaultValue(true).setTooltip(label("stable.tip"))
            .setSaveConsumer(value -> BetterAdvancementsScreen.stableLayout = value).build());
    }

    public static void buildOld(ConfigCategory category, ConfigEntryBuilder builder) {
        category.addEntry(builder.startBooleanToggle(label("old_screen"), BetterAdvancementsScreen.combinedMode)
            .setDefaultValue(false).setTooltip(label("old_screen.tip"))
            .setSaveConsumer(value -> BetterAdvancementsScreen.combinedMode = value).build());
        category.addEntry(builder.startIntSlider(label("old_screen_size"), ScreenSize.clamp(BetterAdvancementsScreen.oldScreenSize), ScreenSize.MIN, ScreenSize.MAX)
            .setDefaultValue(ScreenSize.DEFAULT).setTooltip(label("old_screen_size.tip"))
            .setSaveConsumer(value -> BetterAdvancementsScreen.oldScreenSize = ScreenSize.clamp(value)).build());
        category.addEntry(builder.startIntSlider(label("discovery_distance"), BetterAdvancementsScreen.oldScreenRevealDepth, 0, OldScreenVisibility.MAX_DEPTH)
            .setDefaultValue(OldScreenVisibility.DEFAULT_REVEAL_DEPTH).setTooltip(label("discovery_distance.tip"))
            .setSaveConsumer(value -> BetterAdvancementsScreen.oldScreenRevealDepth = value).build());
        category.addEntry(builder.startBooleanToggle(label("old_remember"), BetterAdvancementsScreen.rememberOldPosition)
            .setDefaultValue(true).setTooltip(label("old_remember.tip"))
            .setSaveConsumer(value -> BetterAdvancementsScreen.rememberOldPosition = value).build());
    }

    public static void buildDetails(ConfigCategory category, ConfigEntryBuilder builder) {
        category.addEntry(builder.startBooleanToggle(label("criteria"), CriterionGrid.detailLevel != CriteriaDetail.OFF)
            .setDefaultValue(true).setTooltip(label("criteria.tip"))
            .setSaveConsumer(ConfigValues::setRequirements).build());
        category.addEntry(builder.startBooleanToggle(label("shift"), CriterionGrid.requiresShift)
            .setDefaultValue(false).setTooltip(label("shift.tip"))
            .setSaveConsumer(value -> CriterionGrid.requiresShift = value).build());
        category.addEntry(builder.startIntSlider(label("criteria_speed"), CriterionGrid.criteriaRotationSpeed, 1, 10)
            .setDefaultValue(3).setTooltip(label("criteria_speed.tip"))
            .setSaveConsumer(value -> CriterionGrid.criteriaRotationSpeed = value).build());
    }

    static void setRequirements(boolean enabled) {
        CriterionGrid.detailLevel = enabled ? CriteriaDetail.DEFAULT : CriteriaDetail.OFF;
        BetterAdvancementsScreen.oldScreenTooltipProgress = enabled;
    }

    private static Component label(String name) {
        return Component.translatable("option.improvedadvancements." + name);
    }
}

package betteradvancements.fabric.config;

import betteradvancements.common.advancements.BetterDisplayInfo;
import betteradvancements.common.gui.BetterAdvancementTab;
import betteradvancements.common.gui.BetterAdvancementTabType;
import betteradvancements.common.gui.BetterAdvancementsScreen;
import betteradvancements.common.gui.BetterAdvancementsScreenButton;
import betteradvancements.common.gui.OldScreenVisibility;
import betteradvancements.common.gui.ScreenSize;
import betteradvancements.common.util.CriteriaDetail;
import betteradvancements.common.util.ColorHelper;
import betteradvancements.common.util.CriterionGrid;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

public final class ConfigValues {
    private ConfigValues() { }

    public static void buildScreen(ConfigCategory category, ConfigEntryBuilder builder) {
        category.addEntry(builder.startBooleanToggle(label("old_screen"), BetterAdvancementsScreen.combinedMode)
            .setDefaultValue(false)
            .setTooltip(label("old_screen.tip"))
            .setSaveConsumer(value -> BetterAdvancementsScreen.combinedMode = value).build());
        category.addEntry(builder.startIntSlider(label("old_screen_size"), ScreenSize.clamp(BetterAdvancementsScreen.oldScreenSize),
                ScreenSize.MIN, ScreenSize.MAX)
            .setDefaultValue(ScreenSize.DEFAULT)
            .setTooltip(label("old_screen_size.tip"))
            .setSaveConsumer(value -> BetterAdvancementsScreen.oldScreenSize = ScreenSize.clamp(value)).build());
        category.addEntry(builder.startIntSlider(label("discovery_distance"), BetterAdvancementsScreen.oldScreenRevealDepth,
                0, OldScreenVisibility.MAX_DEPTH)
            .setDefaultValue(OldScreenVisibility.DEFAULT_REVEAL_DEPTH)
            .setTooltip(label("discovery_distance.tip"))
            .setSaveConsumer(value -> BetterAdvancementsScreen.oldScreenRevealDepth = value).build());
        category.addEntry(builder.startIntSlider(label("scale"), ScreenSize.clamp(BetterAdvancementsScreen.windowScale), ScreenSize.MIN, ScreenSize.MAX)
            .setDefaultValue(ScreenSize.DEFAULT)
            .setTooltip(label("scale.tip"))
            .setSaveConsumer(value -> BetterAdvancementsScreen.windowScale = ScreenSize.clamp(value)).build());
        category.addEntry(builder.startBooleanToggle(label("progress"), BetterAdvancementsScreen.showProgress)
            .setDefaultValue(true)
            .setSaveConsumer(value -> BetterAdvancementsScreen.showProgress = value).build());
        category.addEntry(builder.startBooleanToggle(label("remember"), BetterAdvancementsScreen.rememberPosition)
            .setDefaultValue(true)
            .setSaveConsumer(value -> BetterAdvancementsScreen.rememberPosition = value).build());
        category.addEntry(builder.startBooleanToggle(label("stable"), BetterAdvancementsScreen.stableLayout)
            .setDefaultValue(true)
            .setTooltip(label("stable.tip"))
            .setSaveConsumer(value -> BetterAdvancementsScreen.stableLayout = value).build());
    }

    public static void buildDetails(ConfigCategory category, ConfigEntryBuilder builder) {
        category.addEntry(builder.startBooleanToggle(label("old_tooltip_progress"), BetterAdvancementsScreen.oldScreenTooltipProgress)
            .setDefaultValue(false)
            .setTooltip(label("old_tooltip_progress.tip"))
            .setSaveConsumer(value -> BetterAdvancementsScreen.oldScreenTooltipProgress = value).build());
        category.addEntry(builder.startBooleanToggle(label("criteria"), CriterionGrid.detailLevel != CriteriaDetail.OFF)
            .setDefaultValue(true)
            .setSaveConsumer(value -> CriterionGrid.detailLevel = value ? CriteriaDetail.DEFAULT : CriteriaDetail.OFF).build());
        category.addEntry(builder.startBooleanToggle(label("shift"), CriterionGrid.requiresShift)
            .setDefaultValue(false)
            .setSaveConsumer(value -> CriterionGrid.requiresShift = value).build());
        category.addEntry(builder.startBooleanToggle(label("criteria_color"), CriterionGrid.colorWholeCriteriaText)
            .setDefaultValue(false)
            .setSaveConsumer(value -> CriterionGrid.colorWholeCriteriaText = value).build());
        category.addEntry(builder.startIntSlider(label("criteria_speed"), CriterionGrid.criteriaRotationSpeed, 1, 10)
            .setDefaultValue(3)
            .setSaveConsumer(value -> CriterionGrid.criteriaRotationSpeed = value).build());
    }

    public static void buildAppearance(ConfigCategory category, ConfigEntryBuilder builder) {
        category.addEntry(builder.startBooleanToggle(label("fade"), BetterAdvancementTab.doFade)
            .setDefaultValue(true)
            .setSaveConsumer(value -> BetterAdvancementTab.doFade = value).build());
        category.addEntry(builder.startBooleanToggle(label("top_tabs"), BetterAdvancementTabType.onlyUseAbove)
            .setDefaultValue(false)
            .setSaveConsumer(value -> BetterAdvancementTabType.onlyUseAbove = value).build());
        category.addEntry(builder.startBooleanToggle(label("title"), BetterAdvancementsScreen.keepAdvancementsTitle)
            .setDefaultValue(true)
            .setSaveConsumer(value -> BetterAdvancementsScreen.keepAdvancementsTitle = value).build());
        category.addEntry(builder.startBooleanToggle(label("inventory"), BetterAdvancementsScreenButton.addToInventory)
            .setDefaultValue(false)
            .setSaveConsumer(value -> BetterAdvancementsScreenButton.addToInventory = value).build());
    }

    public static void buildAdvanced(ConfigCategory category, ConfigEntryBuilder builder) {
        category.addEntry(builder.startBooleanToggle(label("debug"), BetterAdvancementsScreen.showDebugCoordinates)
            .setDefaultValue(false)
            .setSaveConsumer(value -> BetterAdvancementsScreen.showDebugCoordinates = value).build());
        category.addEntry(builder.startBooleanToggle(label("direct_lines"), BetterDisplayInfo.defaultDrawDirectLines)
            .setDefaultValue(false)
            .setSaveConsumer(value -> BetterDisplayInfo.defaultDrawDirectLines = value).build());
        category.addEntry(builder.startBooleanToggle(label("hide_lines"), BetterDisplayInfo.defaultHideLines)
            .setDefaultValue(false)
            .setSaveConsumer(value -> BetterDisplayInfo.defaultHideLines = value).build());
        category.addEntry(builder.startAlphaColorField(label("completed_icon"), BetterDisplayInfo.defaultCompletedIconColor)
            .setDefaultValue(ColorHelper.RGB(BetterDisplayInfo.defaultMinecraftCompletedIconColor))
            .setSaveConsumer(value -> BetterDisplayInfo.defaultCompletedIconColor = value).build());
        category.addEntry(builder.startAlphaColorField(label("pending_icon"), BetterDisplayInfo.defaultUncompletedIconColor)
            .setDefaultValue(ColorHelper.RGB(BetterDisplayInfo.defaultMinecraftUncompletedIconColor))
            .setSaveConsumer(value -> BetterDisplayInfo.defaultUncompletedIconColor = value).build());
        category.addEntry(builder.startAlphaColorField(label("completed_title"), BetterDisplayInfo.defaultCompletedTitleColor)
            .setDefaultValue(ColorHelper.RGB(BetterDisplayInfo.defaultMinecraftCompletedTitleColor))
            .setSaveConsumer(value -> BetterDisplayInfo.defaultCompletedTitleColor = value).build());
        category.addEntry(builder.startAlphaColorField(label("pending_title"), BetterDisplayInfo.defaultUncompletedTitleColor)
            .setDefaultValue(ColorHelper.RGB(BetterDisplayInfo.defaultMinecraftUncompletedTitleColor))
            .setSaveConsumer(value -> BetterDisplayInfo.defaultUncompletedTitleColor = value).build());
        category.addEntry(builder.startAlphaColorField(label("completed_line"), BetterDisplayInfo.defaultCompletedLineColor)
            .setDefaultValue(ColorHelper.RGB("#FFFFFF"))
            .setSaveConsumer(value -> BetterDisplayInfo.defaultCompletedLineColor = value).build());
        category.addEntry(builder.startAlphaColorField(label("pending_line"), BetterDisplayInfo.defaultUncompletedLineColor)
            .setDefaultValue(ColorHelper.RGB("#FFFFFF"))
            .setSaveConsumer(value -> BetterDisplayInfo.defaultUncompletedLineColor = value).build());
    }

    private static Component label(String name) {
        return Component.translatable("option.improvedadvancements." + name);
    }
}

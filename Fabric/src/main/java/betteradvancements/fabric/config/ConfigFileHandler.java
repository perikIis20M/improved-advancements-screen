package betteradvancements.fabric.config;

import betteradvancements.common.advancements.BetterDisplayInfo;
import betteradvancements.common.gui.*;
import betteradvancements.common.reference.Constants;
import betteradvancements.common.util.ColorHelper;
import betteradvancements.common.util.CriteriaDetail;
import betteradvancements.common.util.CriterionGrid;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import java.io.*;

public class ConfigFileHandler {
    public static void readFromConfig() {
        JsonObject root = new JsonObject();
        try {
            File file = getConfigFile();
            if (file.exists()) try (FileReader reader = new FileReader(file)) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (IOException | RuntimeException error) { Constants.log.error(error); }
        applyConfig(root);
    }

    // Pure data boundary: legacy settings migrate without touching the user's file on startup.
    static void applyConfig(JsonObject root) {
        BetterAdvancementsScreen.windowScale = ScreenSize.clamp(integer(root, "windowScale", ScreenSize.DEFAULT));
        BetterAdvancementsScreen.combinedMode = bool(root, "oldScreen", false);
        BetterAdvancementsScreen.oldScreenSize = ScreenSize.clamp(integer(root, "oldScreenSize", ScreenSize.DEFAULT));
        BetterAdvancementsScreen.oldScreenRevealDepth = OldScreenVisibility.clampRevealDepth(integer(root, "oldScreenRevealDepth", OldScreenVisibility.DEFAULT_REVEAL_DEPTH));
        BetterAdvancementsScreen.showProgress = bool(root, "showProgress", true);
        BetterAdvancementsScreen.rememberPosition = bool(root, "rememberPosition", true);
        BetterAdvancementsScreen.rememberOldPosition = bool(root, "rememberOldPosition", BetterAdvancementsScreen.rememberPosition);
        BetterAdvancementsScreen.stableLayout = bool(root, "stableLayout", true);
        BetterAdvancementsScreenButton.addToInventory = bool(root, "navigationTabs", true);
        boolean requirements = root.has("showRequirements") ? root.get("showRequirements").getAsBoolean()
            : !root.has("criteriaDetail") || CriteriaDetail.fromName(root.get("criteriaDetail").getAsString()) != CriteriaDetail.OFF;
        ConfigValues.setRequirements(requirements);
        CriterionGrid.requiresShift = bool(root, "criteriaDetailRequiresShift", false);
        CriterionGrid.criteriaRotationSpeed = Math.clamp(integer(root, "criteriaRotationSpeed", 3), 1, 10);

        // Retired appearance/debug/color overrides no longer affect the screen invisibly.
        CriterionGrid.colorWholeCriteriaText = false;
        BetterAdvancementTab.doFade = true;
        BetterAdvancementTabType.onlyUseAbove = false;
        BetterAdvancementsScreen.keepAdvancementsTitle = true;
        BetterAdvancementsScreen.showDebugCoordinates = false;
        BetterAdvancementsScreen.orderTabsAlphabetically = false;
        BetterDisplayInfo.defaultDrawDirectLines = false;
        BetterDisplayInfo.defaultHideLines = false;
        BetterDisplayInfo.defaultCompletedIconColor = ColorHelper.RGB(BetterDisplayInfo.defaultMinecraftCompletedIconColor);
        BetterDisplayInfo.defaultUncompletedIconColor = ColorHelper.RGB(BetterDisplayInfo.defaultMinecraftUncompletedIconColor);
        BetterDisplayInfo.defaultCompletedTitleColor = ColorHelper.RGB(BetterDisplayInfo.defaultMinecraftCompletedTitleColor);
        BetterDisplayInfo.defaultUncompletedTitleColor = ColorHelper.RGB(BetterDisplayInfo.defaultMinecraftUncompletedTitleColor);
        BetterDisplayInfo.defaultCompletedLineColor = BetterDisplayInfo.defaultUncompletedLineColor = ColorHelper.RGB("#FFFFFF");
    }

    static JsonObject configSnapshot() {
        JsonObject root = new JsonObject();
        root.addProperty("windowScale", ScreenSize.clamp(BetterAdvancementsScreen.windowScale));
        root.addProperty("oldScreen", BetterAdvancementsScreen.combinedMode);
        root.addProperty("oldScreenSize", ScreenSize.clamp(BetterAdvancementsScreen.oldScreenSize));
        root.addProperty("oldScreenRevealDepth", BetterAdvancementsScreen.oldScreenRevealDepth);
        root.addProperty("showProgress", BetterAdvancementsScreen.showProgress);
        root.addProperty("rememberPosition", BetterAdvancementsScreen.rememberPosition);
        root.addProperty("rememberOldPosition", BetterAdvancementsScreen.rememberOldPosition);
        root.addProperty("stableLayout", BetterAdvancementsScreen.stableLayout);
        root.addProperty("navigationTabs", BetterAdvancementsScreenButton.addToInventory);
        root.addProperty("showRequirements", CriterionGrid.detailLevel != CriteriaDetail.OFF);
        root.addProperty("criteriaDetailRequiresShift", CriterionGrid.requiresShift);
        root.addProperty("criteriaRotationSpeed", CriterionGrid.criteriaRotationSpeed);
        return root;
    }

    public static void writeToConfig() {
        try (FileWriter file = new FileWriter(getConfigFile())) {
            file.write(new GsonBuilder().setPrettyPrinting().create().toJson(configSnapshot()));
        } catch (IOException error) { Constants.log.error(error); }
    }
    private static boolean bool(JsonObject root, String key, boolean fallback) {
        return root.has(key) ? root.get(key).getAsBoolean() : fallback;
    }
    private static int integer(JsonObject root, String key, int fallback) {
        return root.has(key) ? root.get(key).getAsInt() : fallback;
    }
    public static File getConfigFile() throws IOException {
        return FabricLoader.getInstance().getConfigDir().resolve("betteradvancements.json").toFile();
    }
}

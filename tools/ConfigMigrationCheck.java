package betteradvancements.fabric.config;

import betteradvancements.common.gui.BetterAdvancementsScreen;
import betteradvancements.common.util.CriteriaDetail;
import betteradvancements.common.util.CriterionGrid;
import com.google.gson.JsonObject;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

/** Exercises the packaged config data boundary without reading or changing a player's config. */
public final class ConfigMigrationCheck {
    private static int checks;
    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        ConfigFileHandler.applyConfig(new JsonObject());
        require(CriterionGrid.detailLevel == CriteriaDetail.DEFAULT && BetterAdvancementsScreen.oldScreenTooltipProgress, "Fresh shared details enabled in both views");
        JsonObject old = new JsonObject();
        old.addProperty("rememberPosition", false);
        old.addProperty("stableLayout", true);
        old.addProperty("criteriaDetail", "Off");
        old.addProperty("oldScreenTooltipProgress", true);
        old.addProperty("showDebugCoordinates", true);
        old.addProperty("colorWholeCriteriaText", true);
        old.addProperty("windowScale", 200);
        old.addProperty("oldScreenSize", 40);
        old.addProperty("criteriaRotationSpeed", 99);
        ConfigFileHandler.applyConfig(old);
        require(!BetterAdvancementsScreen.rememberPosition && !BetterAdvancementsScreen.rememberOldPosition, "Legacy view preference migrates to both views");
        require(BetterAdvancementsScreen.stableLayout, "Layout remains independent of view memory");
        require(CriterionGrid.detailLevel == CriteriaDetail.OFF && !BetterAdvancementsScreen.oldScreenTooltipProgress, "Legacy criteria preference controls both views");
        require(!BetterAdvancementsScreen.showDebugCoordinates && !CriterionGrid.colorWholeCriteriaText, "Retired overrides ignored");
        require(BetterAdvancementsScreen.windowScale == 100 && BetterAdvancementsScreen.oldScreenSize == 60 && CriterionGrid.criteriaRotationSpeed == 10, "Bounds preserved during migration");
        for (boolean requirements : new boolean[]{true, false}) for (boolean modern : new boolean[]{true, false}) for (boolean classic : new boolean[]{true, false}) {
            ConfigValues.setRequirements(requirements);
            BetterAdvancementsScreen.rememberPosition = modern;
            BetterAdvancementsScreen.rememberOldPosition = classic;
            JsonObject saved = ConfigFileHandler.configSnapshot();
            require(saved.size() == 11, "Only supported settings serialized");
            ConfigValues.setRequirements(!requirements);
            BetterAdvancementsScreen.rememberPosition = !modern;
            BetterAdvancementsScreen.rememberOldPosition = !classic;
            ConfigFileHandler.applyConfig(saved);
            require((CriterionGrid.detailLevel != CriteriaDetail.OFF) == requirements && BetterAdvancementsScreen.oldScreenTooltipProgress == requirements, "Shared toggle survives save/reload");
            require(BetterAdvancementsScreen.rememberPosition == modern && BetterAdvancementsScreen.rememberOldPosition == classic, "Independent view preferences survive save/reload");
        }
        JsonObject precedence = new JsonObject();
        precedence.addProperty("showRequirements", true);
        precedence.addProperty("criteriaDetail", "Off");
        ConfigFileHandler.applyConfig(precedence);
        require(BetterAdvancementsScreen.oldScreenTooltipProgress && CriterionGrid.detailLevel == CriteriaDetail.DEFAULT, "New shared key takes precedence over retired keys");
        System.out.println("PASS: " + checks + " config migration, shared toggle, independent memory, defaults and round-trip checks; no player file accessed.");
    }
    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}

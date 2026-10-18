package betteradvancements.common.gui;

import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.network.chat.Component;

/** Counts requirement groups, so alternative criteria do not inflate the progress counter. */
public record LegacyTooltipProgress(int completed, int total) {
    public static LegacyTooltipProgress from(boolean enabled, boolean available,
                                             AdvancementRequirements requirements, AdvancementProgress progress) {
        if (!enabled || !available || progress == null || requirements.isEmpty()) return null;
        int completed = requirements.count(name -> {
            var criterion = progress.getCriterion(name);
            return criterion != null && criterion.isDone();
        });
        return new LegacyTooltipProgress(completed, requirements.size());
    }

    public float fraction() { return (float) completed / total; }

    public Component label() {
        return Component.translatable("improvedadvancements.tooltip_progress", completed, total,
            (int) Math.floor(100.0 * completed / total));
    }
}

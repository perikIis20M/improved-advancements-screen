import betteradvancements.common.gui.LegacyTooltipProgress;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.advancements.*;
import java.util.*;
import java.util.stream.IntStream;

/** Exercises the tooltip counter using Minecraft's actual advancement progress objects. */
public final class LegacyTooltipProgressCheck {
    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var names = IntStream.range(0, 55).mapToObj(i -> "biome_" + i).toList();
        var all = AdvancementRequirements.allOf(names);
        var progress = new AdvancementProgress();
        progress.update(all);
        check(all, progress, 0, 55);
        for (int i = 0; i < 52; i++) progress.grantProgress(names.get(i));
        check(all, progress, 52, 55);
        for (var name : names) progress.grantProgress(name);
        check(all, progress, 55, 55);
        for (var name : names) progress.revokeProgress(name);
        check(all, progress, 0, 55);
        require(LegacyTooltipProgress.from(false, true, all, progress) == null, "Disabled option still shows progress");
        require(LegacyTooltipProgress.from(true, false, all, progress) == null, "Locked goal leaks modern details");
        require(LegacyTooltipProgress.from(true, true, all, null) == null, "Unknown server progress invented");
        require(LegacyTooltipProgress.from(true, true, AdvancementRequirements.EMPTY, progress) == null, "Empty goal divides by zero");

        var alternatives = new AdvancementRequirements(List.of(List.of("oak", "birch"), List.of("stone")));
        progress = new AdvancementProgress();
        progress.update(alternatives);
        check(alternatives, progress, 0, 2);
        progress.grantProgress("oak");
        check(alternatives, progress, 1, 2);
        progress.grantProgress("birch");
        check(alternatives, progress, 1, 2);
        progress.grantProgress("stone");
        check(alternatives, progress, 2, 2);
        progress.revokeProgress("oak");
        check(alternatives, progress, 2, 2);
        progress.revokeProgress("birch");
        check(alternatives, progress, 1, 2);
        var single = AdvancementRequirements.anyOf(List.of("one", "two"));
        progress = new AdvancementProgress();
        progress.update(single);
        check(single, progress, 0, 1);
        progress.grantProgress("two");
        check(single, progress, 1, 1);
        System.out.println("PASS: 0/55, 52/55, 55/55, reset, alternative groups, single requirements, locked/disabled/unknown progress.");
    }

    private static void check(AdvancementRequirements requirements, AdvancementProgress progress, int count, int total) {
        var summary = LegacyTooltipProgress.from(true, true, requirements, progress);
        require(summary != null && summary.completed() == count && summary.total() == total, "Incorrect requirement count");
        require(Math.abs(summary.fraction() - progress.getPercent()) < 0.00001, "Bar differs from Minecraft progress");
        require(summary.label() != null, "Missing progress label");
    }

    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}

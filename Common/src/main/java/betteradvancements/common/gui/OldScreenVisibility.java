package betteradvancements.common.gui;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Pre-1.12 discovery distance, applied only to presentation, never completion rules. */
public final class OldScreenVisibility {
    public static final int MAX_DEPTH = 4;
    public static final int DEFAULT_REVEAL_DEPTH = 2;
    private OldScreenVisibility() { }

    /** StatFileWriter.canUnlockAchievement is independent of discovery depth and own completion. */
    public static boolean canUnlock(String id, Map<String, String> parents, Set<String> completed) {
        String parent = parents.get(id);
        return parent == null || completed.contains(parent);
    }

    public static int unlockDepth(String id, Map<String, String> parents, Set<String> completed) {
        if (completed.contains(id)) return 0;
        Set<String> visited = new HashSet<>();
        visited.add(id);
        int depth = 0;
        String parent = parents.get(id);
        while (parent != null && !completed.contains(parent)) {
            if (!visited.add(parent)) return MAX_DEPTH + 1;
            if (++depth > MAX_DEPTH) return depth;
            parent = parents.get(parent);
        }
        return depth;
    }

    public static boolean isVisible(int depth, boolean hidden, boolean completed) {
        return isVisible(depth, hidden, completed, MAX_DEPTH);
    }

    /** The larger modern map defaults to a shorter preview; four preserves the original rule. */
    public static boolean isVisible(int depth, boolean hidden, boolean completed, int revealDepth) {
        return completed || (!hidden && depth <= clampRevealDepth(revealDepth));
    }

    public static int clampRevealDepth(int depth) { return Math.max(0, Math.min(MAX_DEPTH, depth)); }

    public static boolean hasTooltip(int depth) { return depth <= 3; }
}

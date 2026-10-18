import betteradvancements.common.gui.OldScreenVisibility;
import java.util.*;

/** Matches the independent canUnlockAchievement and discovery-distance rules in StatFileWriter. */
public final class LegacyAvailabilityCheck {
    public static void main(String[] args) {
        Map<String, String> parents = new LinkedHashMap<>();
        parents.put("root", null);
        for (int i = 1; i <= 6; i++) parents.put("step" + i, i == 1 ? "root" : "step" + (i - 1));
        Set<String> completed = new HashSet<>();
        require(OldScreenVisibility.canUnlock("root", parents, completed), "An unfinished root is available");
        for (int i = 1; i <= 6; i++) require(!OldScreenVisibility.canUnlock("step" + i, parents, completed), "Locked node exposed a description");
        completed.add("step4");
        require(OldScreenVisibility.unlockDepth("step4", parents, completed) == 0, "Completed entries stay visible");
        require(!OldScreenVisibility.canUnlock("step4", parents, completed), "Own completion incorrectly unlocks the description without its parent");
        require(OldScreenVisibility.canUnlock("step5", parents, completed), "Immediate completed parent unlocks its child even if earlier steps were revoked");
        completed.add("root");
        require(OldScreenVisibility.canUnlock("step1", parents, completed), "Root progress did not unlock first step");
        require(!OldScreenVisibility.canUnlock("step2", parents, completed), "Root progress incorrectly unlocked the second step");
        require(OldScreenVisibility.unlockDepth("step4", parents, completed) == 0, "Child completion must not change depth based on ancestor state");
        completed.add("step3");
        require(OldScreenVisibility.canUnlock("step4", parents, completed), "Completing the prerequisite did not expose the description");
        completed.remove("step3");
        require(!OldScreenVisibility.canUnlock("step4", parents, completed), "Revoking the prerequisite left the description exposed");
        completed.clear();
        for (String id : parents.keySet()) completed.add(id);
        for (String id : parents.keySet()) require(OldScreenVisibility.canUnlock(id, parents, completed), "Complete map has locked descriptions");
        completed.clear();
        for (int depth = 1; depth <= 4; depth++) {
            require(OldScreenVisibility.unlockDepth("step" + depth, parents, completed) == depth, "Wrong unfinished ancestor depth");
            require(OldScreenVisibility.hasTooltip(depth) == (depth < 4), "Wrong tooltip reveal threshold");
        }
        System.out.println("PASS: original availability, independent discovery depth, child-without-parent completion, grant/revoke transitions and full/reset progress.");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}

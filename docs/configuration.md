# Configuration

Open Mod Menu → Improved Advancements Screen → Configure.

## Modern screen

- Screen size: size of the tabbed window.
- Show progress overview: completed count, total and percentage in the selected tab's title.
- Remember tab and position: restores the selected tab and its scroll position.
- Keep advancement layout stable: reuses individual advancement coordinates for the same tree across worlds.

View memory and stable layout are separate: one remembers where you were looking, the other remembers where advancements are placed.

## Old screen

- Old screen: enables the classic connected map.
- Screen size (old): resizes its window without enlarging its contents.
- Old screen discovery distance: how far ahead locked goals are revealed.
- Remember map position and zoom: restores the classic camera independently of modern view memory.

## Advancement Details

These settings apply to both views.

- Show remaining requirements: shows completed/missing requirements and enables the Old screen's progress count and bar. Disabling it restores the classic tooltip in the Old screen.
- Hold Shift for requirements: hides the checklist until Shift is held. Progress counts and bars remain visible.
- Requirement rotation speed: seconds to display each checklist page.

Locked/unknown goals retain their prerequisite hints; Minecraft determines what progress information the client receives. Every setting has a hover tooltip.

The retired Appearance, Advanced and whole-text-color controls are removed. Old color/debug overrides are ignored and no longer saved. Existing window sizes and supported preferences migrate; the previous requirement-detail setting becomes the shared toggle. Old-screen camera memory initially inherits the previous remember-position preference.

## Manual checks

1. Confirm exactly three tabs: Modern screen, Old screen, Advancement Details. Hover each option to read its explanation.
2. Disable modern view memory and enable classic memory. Reopen each view and verify the settings operate independently.
3. With a partially completed advancement, enable shared requirements and verify both views show details. Disable it and verify the classic tooltip returns.
4. Enable Hold Shift; check each view with Shift released and held.
5. Save, restart Minecraft and verify preferences persist. Check locked and unknown classic goals retain prerequisite hints.

Automated migration tests do not replace this in-game visual check.

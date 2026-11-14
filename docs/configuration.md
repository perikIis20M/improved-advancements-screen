# Configuration

Open Mod Menu → Improved Advancements Screen → Configure.

The title, category spacing and arrow-free tab bar match Improved Statistics Screen. This layout is enabled only for this mod's configuration screen; other Cloth Config menus keep their own layout.

## General

These settings apply to both views.

- Inventory and advancement tabs: enables diamond navigation tabs between Survival inventory and either advancement view. On by default; this single setting applies to both styles. Tabs keep their normal GUI size, follow the window and avoid occupied widget/tab slots. If there is no safe slot, the navigation tab is hidden rather than covering another control.
- Show remaining requirements: shows completed/missing requirements and enables the Old screen's progress count and bar. Disabling it restores the classic tooltip in the Old screen.
- Hold Shift for requirements: hides the checklist until Shift is held. Progress counts and bars remain visible.
- Requirement rotation speed: seconds to display each checklist page.

Locked/unknown goals retain their prerequisite hints; Minecraft determines what progress information the client receives. Every setting has a hover tooltip.

## Modern screen

- Screen size: size of the tabbed window.
- Show progress overview: completed count, total and percentage in the selected tab's title.
- Remember tab and position: restores the selected tab and its scroll position.
- Keep advancement layout stable: reuses individual advancement coordinates for the same tree across worlds.

View memory and stable layout are separate: one remembers where you were looking, the other remembers where advancements are placed.

## Old screen

- Toggle: enables the classic connected map.
- Screen size: resizes its window without enlarging its contents.
- Discovery distance: how far ahead locked goals are revealed.
- Remember map position and zoom: restores the classic camera independently of modern view memory.

The retired Appearance, Advanced and whole-text-color controls are removed. Old color/debug overrides are ignored and no longer saved. Existing window sizes and supported preferences migrate; the previous requirement-detail setting becomes the shared toggle. Old-screen camera memory initially inherits the previous remember-position preference.

## Manual checks

1. Confirm exactly three tabs in order: General, Modern screen, Old screen. General contains the inventory/advancement tabs toggle and all shared requirement settings. Hover each option to read its explanation.
2. Disable modern view memory and enable classic memory. Reopen each view and verify the settings operate independently.
3. With a partially completed advancement, enable shared requirements and verify both views show details. Disable it and verify the classic tooltip returns.
4. Enable Hold Shift; check each view with Shift released and held.
5. Save, restart Minecraft and verify preferences persist. Check locked and unknown classic goals retain prerequisite hints.
6. In Survival, use the diamond tab to open advancements from inventory and return from each advancement style. Open/close the recipe book and resize the window; the tab should follow the frame at normal size.
7. Check alongside another mod's inventory tabs. Existing controls should keep their positions and remain clickable. Disable Inventory and advancement tabs, reopen the screens, and verify this mod's tabs disappear. Creative, Adventure and Spectator should never show them.

Automated migration tests do not replace this in-game visual check.

Navigation placement checks cover occupied slots, a completely occupied screen, inventory movement and both advancement frames at every configured size from 60–100%. Configuration checks cover the enabled default and persistence when disabled. These checks passed without opening a Minecraft window; navigation still needs an in-game visual check with the intended inventory extensions.

For 1.0+26.3, the Fabric shadow JAR builds successfully. A no-window Fabric/Knot check applies the actual Cloth Config screen and tab mixins. The same check also passes with a fixture compiled from the supplied Statistics Screen's original layout mixins, confirming those transformations can coexist. This validates transformation compatibility, not a complete in-game comparison of both mods. No Minecraft window was opened or controlled for this change.

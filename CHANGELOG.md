# Changelog

## 1.0+26.3

- Added diamond navigation tabs between Survival inventory and both advancement views, enabled by default with a configuration toggle.
- Matched Improved Statistics Screen's configuration header and category-bar proportions.
- Removed the category navigation arrows and expanded the clickable tab area.
- Put General first, followed by Modern screen and Old screen. General contains the shared navigation-tab toggle and requirement settings, with existing preferences preserved.
- Added the MIT attribution for the adapted configuration layout code.

## 1.0

Changes since 0.7.8:

- Reorganized configuration into Modern screen, Old screen and shared Advancement Details.
- Added explanatory tooltips to every setting.
- Combined Show remaining requirements and the Old screen progress tooltip into one setting for both views.
- Added independent Old screen map-position and zoom memory.
- Kept view memory and stable advancement layouts as separate settings, with clearer explanations.
- Removed the Appearance and Advanced categories and their color/debug options, including whole-requirement-text coloring.
- Shortened Old screen option labels to Toggle, Screen size and Discovery distance.

Existing supported settings load automatically. Old screen memory initially inherits the previous remember-position preference. The previous requirement-detail setting becomes the shared toggle. Existing saved map positions and layouts remain available; retired color/debug overrides return to the standard appearance.

Minecraft 26.3 · Fabric · Client-side

## 0.7.8 — First public beta

Based on Better Advancements by way2muchnoise. Changes from the original mod:

- Added an optional Old screen inspired by Minecraft's pre-1.12 achievements menu, with one connected map, an underground background, prerequisite arrows and gradual discovery.
- Added advancement search to both views, with Ctrl+F and jump-to-result navigation.
- Added progress counts and percentages for the selected advancement tab.
- Added remembered tab selection and map positions.
- Added saved layouts to help keep advancement positions consistent.
- Added optional progress details and remaining requirements to Old screen tooltips.
- Reworked the Mod Menu configuration.
- Fixed the remaining counter displaying `%d` instead of a number, and placed missing requirements after completed ones.

Modified by periklis20M and JohnFG. Original Better Advancements by way2muchnoise.

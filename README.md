# Improved Advancements Screen

A client-side advancement screen for **Minecraft 26.3 and Fabric**, with progress tracking and an optional achievement map inspired by Minecraft before 1.12.

## Features

- Completed and remaining requirements in advancement tooltips, where the server provides that information.
- Progress counts and percentages for the selected tab.
- Remembered tabs, map positions and stable advancement layouts.
- An optional Old screen with one draggable map, an underground background, prerequisite arrows and gradual discovery.
- Search by advancement name, description or item in either view. Open Search or press **Ctrl+F**, then choose a result to find it on the map.
- Optional progress details in Old screen tooltips.
- Configuration through Mod Menu and Cloth Config.

Minecraft still controls advancement completion and the information sent to the client. The Old screen's map connections guide its presentation and discovery; they do not change Minecraft's award requirements.

## Installation

Install the mod on your Fabric 26.3 client with **Fabric API**. Install **Mod Menu** and **Cloth Config** to access its configuration.

To enable the Old screen, open **Mods → Improved Advancements Screen → Configure → Old screen**.

## Build from source

Use **JDK 25**. The Gradle wrapper is included; a separate Gradle installation is unnecessary.

```sh
git clone https://github.com/perikIis20M/improved-advancements-screen.git
cd improved-advancements-screen
./gradlew :Fabric:shadowJar
```

On Windows, use `gradlew.bat :Fabric:shadowJar`. The first build downloads the required dependencies. The finished mod JAR is written to `output/`.

To launch a separate development client, run `./gradlew :Fabric:runClient` or `gradlew.bat :Fabric:runClient`. Its game files are stored in `Fabric/run/`.

This source tree builds the Fabric version. Build output, game files and local backups are excluded from Git.

## Credits and license

Modified by **periklis20M** and **JohnFG**.

Based on [Better Advancements](https://github.com/way2muchnoise/BetterAdvancements) by **way2muchnoise**. The starting upstream revision is `314b4e7`.

Classic Minecraft artwork belongs to **Mojang**. This is an unofficial project and is not affiliated with Mojang or Microsoft.

The configuration header and arrow-free category bar are adapted from **Improved Statistics Screen** by CoolAid48 and Johnfrag. The adapted code retains its [MIT notice](Fabric/src/main/resources/licenses/ImprovedStatisticsScreen-MIT.txt).

The original [Don't Be a Jerk non-commercial license](LICENSE.md) is preserved. It applies to this derivative; please read it before using or redistributing the code.

Report problems through this repository's [issue tracker](https://github.com/perikIis20M/improved-advancements-screen/issues).

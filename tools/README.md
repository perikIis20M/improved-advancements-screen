# Development checks

The Java files in this folder are standalone regression checks used while developing the mod. They are not packaged with the mod.

Run them with JDK 25 and the Fabric development runtime classpath after building the project. Checks that accept a path expect the repository root, a catalogue JSON file, or another file named in their source.

`LegacyReferenceCheck`, `BuildModIcon` and `LegacyMapPreview` also use historical reference files or generated preview data under `build/`. Those local reference files are not included in this repository and are not needed to build or run the mod.

Headless geometry and data checks do not replace checking the interface in Minecraft. Launch a development client with `gradlew.bat :Fabric:runClient` (Windows) or `./gradlew :Fabric:runClient` (macOS/Linux).

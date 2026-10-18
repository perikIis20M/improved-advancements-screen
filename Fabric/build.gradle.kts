// gradle.properties
val fabricVersion: String by extra
val fabricLoaderVersion: String by extra
val clothVersion: String by extra
val modMenuVersion: String by extra
val minecraftVersion: String by extra
val modId: String by extra
val modFileName: String by extra
val modJavaVersion: String by extra

val baseArchivesName = "${modFileName}-Fabric-${minecraftVersion}"
base {
	archivesName.set(baseArchivesName)
}

architectury {
	// Create the IDE launch configurations for this subproject.
	platformSetupLoomIde()
	// Set up Architectury for Fabric.
	fabric()
}

loom {
	accessWidenerPath.set(project(":Common").file("src/main/resources/betteradvancements.accesswidener"))
}

dependencies {
	implementation("net.fabricmc:fabric-loader:${fabricLoaderVersion}")
	implementation("net.fabricmc.fabric-api:fabric-api:${fabricVersion}")
	api("me.shedaniel.cloth:cloth-config-fabric:${clothVersion}") {
		exclude("net.fabricmc.fabric-api")
	}
	implementation("com.terraformersmc:modmenu:${modMenuVersion}")

	implementation(project(":Common")) { isTransitive = false }
	shadowImplementation(project(":Common", configuration = "transformProductionFabric")) { isTransitive = false }

	implementation(project(":FabricApi"))
	shadowImplementation(project(":CommonApi")) { isTransitive = false }
	shadowImplementation(project(":FabricApi")) { isTransitive = false }
}

val apiJar = tasks.register<Jar>("apiJar") {
	from(project(":CommonApi").sourceSets.main.get().output)
	from(project(":FabricApi").sourceSets.main.get().output)

	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
	archiveClassifier.set("api")
}

artifacts {
	archives(apiJar.get())
	archives(tasks.shadowJar.get())
}

tasks.withType<Jar> {
	destinationDirectory.set(file(rootProject.rootDir.path + "/output"))
}

tasks.withType<ProcessResources> {
	from(project(":Common").sourceSets.main.get().resources)
}

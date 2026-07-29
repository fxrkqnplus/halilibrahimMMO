plugins {
    // Version comes from pluginManagement in settings.gradle.kts.
    id("net.fabricmc.fabric-loom")
}

version = providers.gradleProperty("mod_version").get()
group = providers.gradleProperty("maven_group").get()

repositories {
    // Deliberately empty: Loom adds the repositories needed to download
    // Minecraft and the Fabric libraries by itself. Only add entries here when
    // depending on a third-party mod.
}

dependencies {
    // There is no `mappings(...)` line, and that is correct rather than an
    // oversight. Minecraft 26.x ships unobfuscated, so there is nothing to
    // remap: `loom.officialMojangMappings()` fails outright ("Failed to find
    // official mojang mappings for 26.2"), and Fabric's own 26.2 template
    // declares no mappings either.
    //
    // For the same reason these are plain `implementation` dependencies, not
    // `modImplementation` - remapping is no longer part of the build.
    minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
    implementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")
    implementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
}

tasks.processResources {
    val version = version
    val modId = providers.gradleProperty("mod_id").get()
    val minecraftVersion = providers.gradleProperty("minecraft_version").get()
    val loaderVersion = providers.gradleProperty("loader_version").get()

    inputs.property("version", version)
    inputs.property("mod_id", modId)
    inputs.property("minecraft_version", minecraftVersion)
    inputs.property("loader_version", loaderVersion)

    filesMatching("fabric.mod.json") {
        expand(
            "version" to version,
            "mod_id" to modId,
            "minecraft_version" to minecraftVersion,
            "loader_version" to loaderVersion
        )
    }
}

// Minecraft 26.2 runs on Java 25 (its version manifest asks for major version
// 25), so the mod targets 25 even though the server plugin targets 26.
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 25
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

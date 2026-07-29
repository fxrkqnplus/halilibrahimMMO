pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        mavenCentral()
        gradlePluginPortal()
    }

    // The plugin id is `net.fabricmc.fabric-loom`, not the legacy `fabric-loom`.
    // Declaring the version here (rather than in build.gradle.kts) keeps
    // gradle.properties the single source of truth, since `providers` is not
    // available inside a `plugins {}` block.
    plugins {
        id("net.fabricmc.fabric-loom") version providers.gradleProperty("loom_version")
    }
}

// Should match the mod id.
rootProject.name = "hmmo-client"

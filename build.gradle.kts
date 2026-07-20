import org.gradle.jvm.toolchain.JavaLanguageVersion

val javaVersion = providers.gradleProperty("javaVersion").get().toInt()

allprojects {
    group = providers.gradleProperty("pluginGroup").get()
    version = providers.gradleProperty("pluginVersion").get()

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/") {
            name = "papermc"
        }
        maven("https://repo.helpch.at/releases/") {
            name = "placeholderapi"
        }
    }
}

subprojects {
    apply(plugin = "java")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(javaVersion))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(javaVersion)
    }

    tasks.withType<ProcessResources>().configureEach {
        filteringCharset = "UTF-8"
    }
}

/*
 * HalilIbrahimMMO :: core
 *
 * The actual plugin implementation.
 *
 * HikariCP and every JDBC driver below are declared `compileOnly`: javac needs them
 * on the classpath to compile HikariDatabaseService and friends, but they must NOT
 * be bundled into the shipped jar. At runtime, HalilIbrahimMMOLoader (a Paper
 * PluginLoader) resolves the exact same coordinates - read from
 * runtime-libraries.txt, itself generated from this same version catalog - through
 * Paper's MavenLibraryResolver and hands them to the plugin's runtime classloader.
 * Since `compileOnly` dependencies are excluded from the `runtimeClasspath`
 * configuration, Shadow never has a chance to accidentally bundle them; it only
 * ever merges the :api module's own compiled classes into this module's final jar.
 */

plugins {
    id("com.gradleup.shadow") version "9.6.0"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

val paperApiVersion = providers.gradleProperty("paperApiVersion").get()
val targetMinecraftVersion = providers.gradleProperty("minecraftVersion").get()
val hikariCpVersion = providers.gradleProperty("hikariCpVersion").get()
val sqliteJdbcVersion = providers.gradleProperty("sqliteJdbcVersion").get()
val mysqlConnectorVersion = providers.gradleProperty("mysqlConnectorVersion").get()
val mariadbConnectorVersion = providers.gradleProperty("mariadbConnectorVersion").get()
val postgresqlVersion = providers.gradleProperty("postgresqlVersion").get()
val placeholderApiVersion = providers.gradleProperty("placeholderApiVersion").get()
val luckPermsApiVersion = providers.gradleProperty("luckPermsApiVersion").get()
val testcontainersVersion = providers.gradleProperty("testcontainersVersion").get()

base {
    archivesName.set("HalilIbrahimMMO")
}

// Cross-engine repository integration tests (MySQL/MariaDB/PostgreSQL via
// Testcontainers) live in their own source set/task, deliberately excluded
// from `test`/`check`/`build` since they require a running Docker daemon -
// see the `integrationTest` task below and PROJECT_MEMORY.md §21.
sourceSets {
    create("integrationTest") {
        java.srcDir("src/integrationTest/java")
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

configurations.getByName("integrationTestImplementation") {
    extendsFrom(configurations.testImplementation.get())
}
configurations.getByName("integrationTestRuntimeOnly") {
    extendsFrom(configurations.testRuntimeOnly.get())
}

dependencies {
    implementation(project(":api"))
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")

    // Optional soft dependencies (see paper-plugin.yml's dependencies.server block) -
    // present on the server's classpath only when that plugin is installed, never
    // resolved through HalilIbrahimMMOLoader since they aren't shared runtime libraries.
    compileOnly("me.clip:placeholderapi:$placeholderApiVersion")
    compileOnly("net.luckperms:api:$luckPermsApiVersion")

    // Runtime-supplied by HalilIbrahimMMOLoader - see file header comment above.
    compileOnly("com.zaxxer:HikariCP:$hikariCpVersion")
    compileOnly("org.xerial:sqlite-jdbc:$sqliteJdbcVersion")
    compileOnly("com.mysql:mysql-connector-j:$mysqlConnectorVersion")
    compileOnly("org.mariadb.jdbc:mariadb-java-client:$mariadbConnectorVersion")
    compileOnly("org.postgresql:postgresql:$postgresqlVersion")

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.papermc.paper:paper-api:$paperApiVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Needed by SqliteMigrationContractTest, the one migration-execution test
    // that runs in the fast suite (no Docker required, unlike the other 3 engines).
    testImplementation("com.zaxxer:HikariCP:$hikariCpVersion")
    testImplementation("org.xerial:sqlite-jdbc:$sqliteJdbcVersion")

    "integrationTestImplementation"("org.testcontainers:testcontainers-junit-jupiter:$testcontainersVersion")
    "integrationTestImplementation"("org.testcontainers:testcontainers-mysql:$testcontainersVersion")
    "integrationTestImplementation"("org.testcontainers:testcontainers-mariadb:$testcontainersVersion")
    "integrationTestImplementation"("org.testcontainers:testcontainers-postgresql:$testcontainersVersion")
    "integrationTestImplementation"("com.zaxxer:HikariCP:$hikariCpVersion")
    "integrationTestImplementation"("com.mysql:mysql-connector-j:$mysqlConnectorVersion")
    "integrationTestImplementation"("org.mariadb.jdbc:mariadb-java-client:$mariadbConnectorVersion")
    "integrationTestImplementation"("org.postgresql:postgresql:$postgresqlVersion")
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    runServer {
        minecraftVersion(targetMinecraftVersion)
    }

    processResources {
        val substitutions = mapOf(
            "version" to project.version.toString(),
            "apiVersion" to targetMinecraftVersion,
            "hikariCpVersion" to hikariCpVersion,
            "sqliteJdbcVersion" to sqliteJdbcVersion,
            "mysqlConnectorVersion" to mysqlConnectorVersion,
            "mariadbConnectorVersion" to mariadbConnectorVersion,
            "postgresqlVersion" to postgresqlVersion
        )
        inputs.properties(substitutions)
        filesMatching(listOf("paper-plugin.yml", "runtime-libraries.txt")) {
            expand(substitutions)
        }
    }

    test {
        useJUnitPlatform()
    }

    register<Test>("integrationTest") {
        description = "Runs cross-engine repository tests against real MySQL/MariaDB/PostgreSQL via Testcontainers. Requires a running Docker daemon; not part of build/check/test."
        group = "verification"
        testClassesDirs = sourceSets["integrationTest"].output.classesDirs
        classpath = sourceSets["integrationTest"].runtimeClasspath
        useJUnitPlatform()
        shouldRunAfter(test)
    }
}

java {
    withSourcesJar()
}

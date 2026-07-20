/*
 * HalilIbrahimMMO :: api
 *
 * Pure public developer API. This module has no dependency on any
 * HalilIbrahimMMO internal class and no external runtime libraries -
 * third-party plugins should only ever need to `compileOnly` this jar
 * plus paper-api itself.
 */

dependencies {
    compileOnly("io.papermc.paper:paper-api:${providers.gradleProperty("paperApiVersion").get()}")
}

java {
    withSourcesJar()
    withJavadocJar()
}

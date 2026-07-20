package com.halilibrahim.halilibrahimmmo;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Declares HalilIbrahimMMO's third-party runtime libraries (HikariCP + every supported
 * JDBC driver) to Paper's plugin classloader. None of these libraries are shaded into
 * the plugin jar; Paper downloads them into the server's shared {@code libraries/}
 * cache on first boot and reuses them across restarts.
 *
 * <p>The actual list of coordinates lives in {@code runtime-libraries.txt}, generated
 * at build time from the version catalog in {@code gradle.properties} - this class
 * never hardcodes a library version.</p>
 */
public final class HalilIbrahimMMOLoader implements PluginLoader {

    private static final String RUNTIME_LIBRARIES_RESOURCE = "/runtime-libraries.txt";

    @Override
    public void classloader(PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();
        resolver.addRepository(new RemoteRepository.Builder(
                "central", "default", MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR
        ).build());

        for (String coordinate : readRuntimeLibraryCoordinates()) {
            resolver.addDependency(new Dependency(new DefaultArtifact(coordinate), null));
        }

        classpathBuilder.addLibrary(resolver);
    }

    private Iterable<String> readRuntimeLibraryCoordinates() {
        try (InputStream in = HalilIbrahimMMOLoader.class.getResourceAsStream(RUNTIME_LIBRARIES_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException(
                        "Missing bundled resource " + RUNTIME_LIBRARIES_RESOURCE + " - the plugin jar is corrupt."
                );
            }

            List<String> coordinates = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        continue;
                    }
                    coordinates.add(trimmed);
                }
            }
            return coordinates;
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read " + RUNTIME_LIBRARIES_RESOURCE, exception);
        }
    }
}

package net.sortcraft.test;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameTestParityTest {
    private static final Pattern TEST_METHOD_PATTERN =
            Pattern.compile("public\\s+void\\s+(\\w+)\\s*\\(\\s*GameTestHelper\\s+\\w+\\s*\\)");
    private static final Pattern ENTRYPOINT_PATTERN =
            Pattern.compile("\"net\\.sortcraft\\.gametest\\.([A-Za-z0-9_]+)\"");
    private static final Pattern CATALOG_CLASS_PATTERN =
            Pattern.compile("\\b([A-Za-z0-9_]+GameTest)\\.class\\b");

    @Test
    void mirroredFabricAndNeoForgeGameTestMethodsStayInSync() throws IOException {
        Path root = findRepoRoot();
        Path fabricDir = root.resolve("fabric/src/gametest/java/net/sortcraft/gametest");
        Path neoForgeDir = root.resolve("neoforge/src/gametest/java/net/sortcraft/gametest");

        Set<String> fabric = discoverGameTestMethods(fabricDir);
        Set<String> neoForge = discoverGameTestMethods(neoForgeDir);

        Set<String> sharedFabric = filterSharedClasses(fabric, neoForge);
        Set<String> sharedNeoForge = filterSharedClasses(neoForge, fabric);

        assertEquals(new TreeSet<>(sharedFabric), new TreeSet<>(sharedNeoForge),
                "Fabric/NeoForge mirrored GameTest methods drifted");
    }

    @Test
    void fabricGameTestEntrypointsCoverAllFabricGameTestClasses() throws IOException {
        Path root = findRepoRoot();
        Path fabricGameTestDir = root.resolve("fabric/src/gametest/java/net/sortcraft/gametest");
        Path fabricModJson = root.resolve("fabric/src/gametest/resources/fabric.mod.json");

        Set<String> sourceGameTestClasses = discoverGameTestMethods(fabricGameTestDir).stream()
                .map(s -> s.substring(0, s.indexOf('.')))
                .collect(Collectors.toCollection(TreeSet::new));
        Set<String> entrypointClasses = parseEntrypointClasses(fabricModJson);

        assertTrue(entrypointClasses.containsAll(sourceGameTestClasses),
                "fabric.mod.json missing GameTest entrypoints: "
                        + diff(sourceGameTestClasses, entrypointClasses));
    }

    @Test
    void neoForgeCatalogCoversNeoForgeGameTestClasses() throws IOException {
        Path root = findRepoRoot();
        Path neoForgeGameTestDir = root.resolve("neoforge/src/gametest/java/net/sortcraft/gametest");
        Path catalog = root.resolve("neoforge/src/gametest/java/net/sortcraft/gametest/neoforge/SortCraftNeoForgeGameTestCatalog.java");

        Set<String> sourceGameTestClasses = discoverGameTestMethods(neoForgeGameTestDir).stream()
                .map(s -> s.substring(0, s.indexOf('.')))
                .collect(Collectors.toCollection(TreeSet::new));
        Set<String> catalogClasses = parseCatalogClasses(catalog);

        assertTrue(catalogClasses.containsAll(sourceGameTestClasses),
                "NeoForge registrar catalog missing GameTest classes: "
                        + diff(sourceGameTestClasses, catalogClasses));
    }

    private static Set<String> discoverGameTestMethods(Path gametestDir) throws IOException {
        try (Stream<Path> stream = Files.list(gametestDir)) {
            return stream
                    .filter(p -> p.getFileName().toString().endsWith("GameTest.java"))
                    .filter(p -> !p.getFileName().toString().contains("Parity"))
                    .sorted()
                    .flatMap(GameTestParityTest::methodsForFileUnchecked)
                    .collect(Collectors.toCollection(TreeSet::new));
        }
    }

    private static Stream<String> methodsForFileUnchecked(Path path) {
        try {
            return methodsForFile(path).stream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<String> methodsForFile(Path path) throws IOException {
        String fileName = path.getFileName().toString();
        String className = fileName.substring(0, fileName.length() - ".java".length());
        String src = Files.readString(path, StandardCharsets.UTF_8);

        Matcher matcher = TEST_METHOD_PATTERN.matcher(src);
        List<String> methods = new java.util.ArrayList<>();
        while (matcher.find()) {
            methods.add(className + "." + matcher.group(1));
        }
        return methods;
    }

    private static Set<String> parseEntrypointClasses(Path fabricModJson) throws IOException {
        String json = Files.readString(fabricModJson, StandardCharsets.UTF_8);
        Matcher matcher = ENTRYPOINT_PATTERN.matcher(json);
        Set<String> result = new TreeSet<>();
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

    private static Set<String> parseCatalogClasses(Path catalogSource) throws IOException {
        String src = Files.readString(catalogSource, StandardCharsets.UTF_8);
        Matcher matcher = CATALOG_CLASS_PATTERN.matcher(src);
        Set<String> result = new TreeSet<>();
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

    private static Set<String> filterSharedClasses(Set<String> left, Set<String> right) {
        Set<String> rightClasses = right.stream()
                .map(s -> s.substring(0, s.indexOf('.')))
                .collect(Collectors.toSet());
        return left.stream()
                .filter(s -> rightClasses.contains(s.substring(0, s.indexOf('.'))))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static Set<String> diff(Set<String> expected, Set<String> actual) {
        Set<String> result = new LinkedHashSet<>(expected);
        result.removeAll(actual);
        return result;
    }

    private static Path findRepoRoot() {
        Path start = Path.of("").toAbsolutePath().normalize();
        for (Path p = start; p != null; p = p.getParent()) {
            if (Files.exists(p.resolve("fabric/build.gradle")) && Files.exists(p.resolve("neoforge/build.gradle"))) {
                return p;
            }
        }
        throw new IllegalStateException("Could not locate repo root from " + start);
    }
}

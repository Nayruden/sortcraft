package net.sortcraft.gametest.neoforge;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.sortcraft.compat.Id;
import net.sortcraft.gametest.AuditGameTest;
import net.sortcraft.gametest.CategoryMatchingGameTest;
import net.sortcraft.gametest.ChestLayoutGameTest;
import net.sortcraft.gametest.ContainerTypeGameTest;
import net.sortcraft.gametest.FilterFeatureGameTest;
import net.sortcraft.gametest.InputStackGameTest;
import net.sortcraft.gametest.ItemDistributionGameTest;
import net.sortcraft.gametest.NestedContainerGameTest;
import net.sortcraft.gametest.OverflowGameTest;
import net.sortcraft.gametest.PerformanceGameTest;
import net.sortcraft.gametest.ShareConfigSortGameTest;
import net.sortcraft.gametest.SignTextGameTest;
import net.sortcraft.gametest.SophisticatedStorageGameTest;
import net.sortcraft.gametest.SortCraftGameTest;
import net.sortcraft.gametest.StorageDrawersGameTest;
import net.sortcraft.gametest.StorageLayoutGameTest;
import net.sortcraft.gametest.VerticalStackGameTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

final class SortCraftNeoForgeGameTestCatalog {
    private static final Logger LOGGER = LoggerFactory.getLogger("sortcraft-gametest");
    static final String NAMESPACE = "sortcraft-gametest";
    static final Id DEFAULT_STRUCTURE = Id.parse("sortcraft-gametest:empty_32x32");

    private static final List<Class<?>> TEST_CLASSES = buildTestClasses();

    private static final Map<String, Id> STRUCTURE_OVERRIDES = Map.ofEntries(
            Map.entry("PerformanceGameTest.sortWithLargeSearchRadius", Id.parse("sortcraft-gametest:empty_32x32")),
            Map.entry("PerformanceGameTest.categoryOutsideRadiusNotFound", Id.parse("sortcraft-gametest:empty_32x32")),
            Map.entry("PerformanceGameTest.chestAtExactRadiusIncluded", Id.parse("sortcraft-gametest:empty_32x32")),
            Map.entry("PerformanceGameTest.chestBeyondRadiusExcluded", Id.parse("sortcraft-gametest:empty_32x32")),
            Map.entry("PerformanceGameTest.diagonalDistanceCalculation", Id.parse("sortcraft-gametest:empty_32x32")),
            Map.entry("PerformanceGameTest.verticalDistanceIncludedInRadius", Id.parse("sortcraft-gametest:empty_32x32"))
    );

    private static final List<TestMethodSpec> TESTS = discoverTests();
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, NAMESPACE);
    private static final AtomicBoolean FUNCTIONS_REGISTERED = new AtomicBoolean(false);

    private SortCraftNeoForgeGameTestCatalog() {}

    private static List<Class<?>> buildTestClasses() {
        List<Class<?>> classes = new ArrayList<>(List.of(
                AuditGameTest.class,
                CategoryMatchingGameTest.class,
                ChestLayoutGameTest.class,
                ContainerTypeGameTest.class,
                FilterFeatureGameTest.class,
                InputStackGameTest.class,
                ItemDistributionGameTest.class,
                NestedContainerGameTest.class,
                OverflowGameTest.class,
                PerformanceGameTest.class,
                ShareConfigSortGameTest.class,
                SignTextGameTest.class,
                SortCraftGameTest.class,
                StorageDrawersGameTest.class,
                StorageLayoutGameTest.class,
                VerticalStackGameTest.class
        ));

        if (Boolean.getBoolean("sortcraft.gametest.enableThirdParty")) {
            classes.add(SophisticatedStorageGameTest.class);
        }

        return List.copyOf(classes);
    }

    static void registerTestFunctions(IEventBus modBus) {
        if (!FUNCTIONS_REGISTERED.compareAndSet(false, true)) {
            return;
        }

        for (TestMethodSpec spec : TESTS) {
            TEST_FUNCTIONS.register(spec.functionId().path(), () -> spec::invoke);
        }

        TEST_FUNCTIONS.register(modBus);
        LOGGER.info("Registered {} SortCraft NeoForge GameTest functions", TESTS.size());
    }

    static List<TestMethodSpec> tests() {
        return TESTS;
    }

    private static List<TestMethodSpec> discoverTests() {
        List<TestMethodSpec> result = new ArrayList<>();
        Map<String, TestMethodSpec> byTestId = new LinkedHashMap<>();

        for (Class<?> testClass : TEST_CLASSES) {
            for (Method method : testClass.getDeclaredMethods()) {
                if (!isGameTestMethod(method)) {
                    continue;
                }

                method.setAccessible(true);
                String key = testClass.getSimpleName() + "." + method.getName();
                Id structure = STRUCTURE_OVERRIDES.getOrDefault(key, DEFAULT_STRUCTURE);

                String classSlug = toSnake(testClass.getSimpleName().replaceFirst("GameTest$", ""));
                String methodSlug = toSnake(method.getName());
                Id testId = id("generated/" + classSlug + "/" + methodSlug);
                Id functionId = id("generated_fn/" + classSlug + "/" + methodSlug);
                ResourceKey<Consumer<GameTestHelper>> functionKey =
                        ResourceKey.create(Registries.TEST_FUNCTION, functionId.unwrap());

                TestMethodSpec spec = new TestMethodSpec(testClass, method, testId, functionId, functionKey, structure);
                TestMethodSpec prior = byTestId.put(testId.toString(), spec);
                if (prior != null) {
                    throw new IllegalStateException("Duplicate GameTest id: " + testId + " for "
                            + prior.testClass().getName() + "#" + prior.method().getName()
                            + " and " + testClass.getName() + "#" + method.getName());
                }
                result.add(spec);
            }
        }

        LOGGER.info("Discovered {} NeoForge GameTest methods across {} classes", result.size(), TEST_CLASSES.size());
        return List.copyOf(result);
    }

    private static boolean isGameTestMethod(Method method) {
        return Modifier.isPublic(method.getModifiers())
                && !Modifier.isStatic(method.getModifiers())
                && method.getReturnType() == Void.TYPE
                && method.getParameterCount() == 1
                && method.getParameterTypes()[0] == GameTestHelper.class;
    }

    private static Id id(String path) {
        return Id.parse(NAMESPACE + ":" + path);
    }

    private static String toSnake(String value) {
        String snake = value
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
                .replace('-', '_')
                .toLowerCase(Locale.ROOT);
        return snake;
    }

    static final class TestMethodSpec {
        private final Class<?> testClass;
        private final Method method;
        private final Id testId;
        private final Id functionId;
        private final ResourceKey<Consumer<GameTestHelper>> functionKey;
        private final Id structureId;

        TestMethodSpec(Class<?> testClass,
                       Method method,
                       Id testId,
                       Id functionId,
                       ResourceKey<Consumer<GameTestHelper>> functionKey,
                       Id structureId) {
            this.testClass = testClass;
            this.method = method;
            this.testId = testId;
            this.functionId = functionId;
            this.functionKey = functionKey;
            this.structureId = structureId;
        }

        Class<?> testClass() {
            return testClass;
        }

        Method method() {
            return method;
        }

        Id testId() {
            return testId;
        }

        Id functionId() {
            return functionId;
        }

        ResourceKey<Consumer<GameTestHelper>> functionKey() {
            return functionKey;
        }

        Id structureId() {
            return structureId;
        }

        void invoke(GameTestHelper helper) {
            try {
                Object instance = testClass.getDeclaredConstructor().newInstance();
                method.invoke(instance, helper);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                throw new RuntimeException("GameTest method failed: " + testClass.getName() + "#" + method.getName(), cause);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Failed to invoke GameTest method: " + testClass.getName() + "#" + method.getName(), e);
            }
        }
    }
}

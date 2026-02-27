package net.sortcraft.gametest.neoforge;

import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

final class NeoForgeGameTestRegistrationCompat {
    private static final int DEFAULT_MAX_TICKS = 100;
    private static final int DEFAULT_SETUP_TICKS = 0;

    private NeoForgeGameTestRegistrationCompat() {}

    static void register(RegisterGameTestsEvent event,
                         Holder<TestEnvironmentDefinition> environment,
                         SortCraftNeoForgeGameTestCatalog.TestMethodSpec spec) {
        TestData<Holder<TestEnvironmentDefinition>> testData = new TestData<>(
                environment,
                spec.structureId(),
                DEFAULT_MAX_TICKS,
                DEFAULT_SETUP_TICKS,
                true,
                Rotation.NONE
        );

        event.registerTest(spec.testId(), new FunctionGameTestInstance(spec.functionKey(), testData));
    }
}

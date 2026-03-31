package net.sortcraft.gametest.neoforge;

import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestEnvironments;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public final class SortCraftNeoForgeGameTestRegistrar {
    private static final Logger LOGGER = LoggerFactory.getLogger("sortcraft-gametest");
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private SortCraftNeoForgeGameTestRegistrar() {}

    public static void registerAll(RegisterGameTestsEvent event) {
        if (!REGISTERED.compareAndSet(false, true)) {
            LOGGER.debug("Skipping duplicate RegisterGameTestsEvent for SortCraft NeoForge GameTests");
            return;
        }

        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(Identifier.parse("sortcraft-gametest:sortcraft"));

        int registered = 0;
        for (SortCraftNeoForgeGameTestCatalog.TestMethodSpec spec : SortCraftNeoForgeGameTestCatalog.tests()) {
            NeoForgeGameTestRegistrationCompat.register(event, environment, spec);
            registered++;
        }

        LOGGER.info("Registered {} SortCraft NeoForge GameTests", registered);
    }
}

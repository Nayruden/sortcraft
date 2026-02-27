package net.sortcraft.gametest.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@Mod("sortcraft_gametest")
public final class SortCraftNeoForgeGameTestMod {
    public SortCraftNeoForgeGameTestMod(IEventBus modBus) {
        SortCraftNeoForgeGameTestCatalog.registerTestFunctions(modBus);
        modBus.addListener(this::registerGameTests);
    }

    private void registerGameTests(RegisterGameTestsEvent event) {
        SortCraftNeoForgeGameTestRegistrar.registerAll(event);
    }
}

package net.sortcraft.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.sortcraft.Sortcraft;

@Mod(Sortcraft.MODID)
public class SortcraftNeoForge {
    public SortcraftNeoForge(IEventBus modEventBus) {
        Sortcraft.init();
    }
}


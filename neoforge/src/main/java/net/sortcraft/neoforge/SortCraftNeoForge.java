package net.sortcraft.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.sortcraft.SortCraft;

@Mod(SortCraft.MODID)
public class SortCraftNeoForge {
    public SortCraftNeoForge(IEventBus modEventBus) {
        SortCraft.init();
    }
}


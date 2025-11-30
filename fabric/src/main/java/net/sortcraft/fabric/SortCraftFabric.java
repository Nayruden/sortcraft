package net.sortcraft.fabric;

import net.fabricmc.api.ModInitializer;
import net.sortcraft.SortCraft;

public class SortCraftFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SortCraft.init();
    }
}


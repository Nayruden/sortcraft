package net.sortcraft.fabric;

import net.fabricmc.api.ModInitializer;
import net.sortcraft.Sortcraft;

public class SortcraftFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Sortcraft.init();
    }
}


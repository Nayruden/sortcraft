package net.sortcraft.compat;

import net.minecraft.world.item.alchemy.PotionContents;

import java.util.Optional;

/**
 * Potion helper for Minecraft 1.21.4+.
 * In 1.21.4+, PotionContents has a customName() method that returns Optional<String>.
 */
public final class PotionHelper {
    private PotionHelper() {}

    /**
     * Get the custom name from PotionContents.
     * In 1.21.4+, custom potions have a custom_name field accessible via customName().
     *
     * @param contents The PotionContents to extract the custom name from
     * @return The custom name, or null if not present
     */
    public static String getCustomName(PotionContents contents) {
        if (contents == null) {
            return null;
        }
        return contents.customName().orElse(null);
    }
}


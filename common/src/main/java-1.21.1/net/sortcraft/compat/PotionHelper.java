package net.sortcraft.compat;

import net.minecraft.world.item.alchemy.PotionContents;

/**
 * Potion helper for Minecraft 1.21.1.
 * In 1.21.1, PotionContents does NOT have a customName() method.
 * Custom potions with custom_name are not supported on this version.
 */
public final class PotionHelper {
    private PotionHelper() {}

    /**
     * Get the custom name from PotionContents.
     * In 1.21.1, the customName() method doesn't exist, so we always return null.
     * Custom potion names are only supported in 1.21.4+.
     *
     * @param contents The PotionContents (unused in 1.21.1)
     * @return Always null in 1.21.1
     */
    public static String getCustomName(PotionContents contents) {
        // customName() method doesn't exist in 1.21.1
        return null;
    }
}


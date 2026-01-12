package net.sortcraft.compat;

import net.minecraft.commands.CommandSourceStack;

/**
 * Helper class for permission checks.
 * In MC 1.21.1-1.21.10, this uses the simple hasPermission(int) method.
 * In MC 1.21.11+, the permission system was reworked.
 */
public final class PermissionHelper {
    private PermissionHelper() {}

    /**
     * Check if the command source has operator permission level.
     * @param source The command source to check
     * @param level The permission level (0-4, where 2 is typical for operator commands)
     * @return true if the source has the required permission level
     */
    public static boolean hasOpLevel(CommandSourceStack source, int level) {
        return source.hasPermission(level);
    }
}


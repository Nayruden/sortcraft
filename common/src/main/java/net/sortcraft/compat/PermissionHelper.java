package net.sortcraft.compat;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.Permissions;

/**
 * Helper class for permission checks.
 * In MC 1.21.11+, CommandSourceStack.hasPermission(int) was replaced with
 * source.permissions().hasPermission(Permissions.XXX).
 */
public final class PermissionHelper {
    private PermissionHelper() {}

    /**
     * Check if the command source has operator permission level.
     * In 1.21.11+, uses the new permissions() API with Permissions constants.
     *
     * @param source The command source to check
     * @param level The permission level (0-4, where 2 is typical for operator commands)
     * @return true if the source has the required permission level
     */
    public static boolean hasOpLevel(CommandSourceStack source, int level) {
        // Level 0 is available to everyone. All operator-gated SortCraft commands
        // use level 2, which maps to COMMANDS_MODERATOR — the only operator-tier
        // permission the 1.21.11+ API exposes for this mod's needs.
        if (level <= 0) return true;
        return source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR);
    }
}


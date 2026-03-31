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
        // In 1.21.11+, use the new permissions API
        // Map legacy int levels to Permissions constants
        // Note: Only COMMANDS_MODERATOR is commonly used; for other levels we approximate
        return switch (level) {
            case 0 -> true; // Everyone has level 0
            case 1, 2 -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR);
            case 3, 4 -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR);
            default -> level <= 0 || source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR);
        };
    }

    /**
     * Check if the command source has moderator permissions (level 2).
     * This is the most common permission check for operator commands.
     *
     * @param source The command source to check
     * @return true if the source has moderator permissions
     */
    public static boolean isModerator(CommandSourceStack source) {
        return source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR);
    }

    /**
     * Check if the command source has admin permissions (level 3).
     * In 1.21.11+, we use COMMANDS_MODERATOR as the closest equivalent.
     *
     * @param source The command source to check
     * @return true if the source has admin permissions
     */
    public static boolean isAdmin(CommandSourceStack source) {
        // Use COMMANDS_MODERATOR as the closest available permission level
        return source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR);
    }

    /**
     * Check if the command source has owner permissions (level 4).
     * In 1.21.11+, we use COMMANDS_MODERATOR as the closest equivalent.
     *
     * @param source The command source to check
     * @return true if the source has owner permissions
     */
    public static boolean isOwner(CommandSourceStack source) {
        // Use COMMANDS_MODERATOR as the closest available permission level
        return source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR);
    }
}


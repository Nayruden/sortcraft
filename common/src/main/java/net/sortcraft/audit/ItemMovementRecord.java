package net.sortcraft.audit;

import net.minecraft.core.BlockPos;

/**
 * Immutable record representing a single item movement during a sort operation.
 *
 * @param itemId          The item identifier (e.g., "minecraft:iron_ingot")
 * @param quantity        Number of items moved
 * @param category        The category the item was sorted into
 * @param destinationPos  The position of the destination chest
 * @param partial         True if only part of the stack could be moved
 */
public record ItemMovementRecord(
        String itemId,
        int quantity,
        String category,
        BlockPos destinationPos,
        boolean partial
) {
    /**
     * Creates a JSON representation of this movement record.
     */
    public String toJson() {
        String destPosJson = destinationPos != null
                ? String.format("{\"x\":%d,\"y\":%d,\"z\":%d}", destinationPos.getX(), destinationPos.getY(), destinationPos.getZ())
                : "null";
        return String.format(
                "{\"itemId\":\"%s\",\"quantity\":%d,\"category\":\"%s\",\"destinationPos\":%s,\"partial\":%s}",
                escapeJson(itemId),
                quantity,
                escapeJson(category),
                destPosJson,
                partial
        );
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}


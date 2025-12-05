package net.sortcraft.audit;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable record representing a complete sort operation audit entry.
 *
 * @param operationId         Unique identifier for this operation
 * @param timestamp           When the operation occurred
 * @param playerName          Name of the player who initiated the sort
 * @param playerUuid          UUID of the player
 * @param dimension           The dimension where the sort occurred
 * @param operationType       SORT or PREVIEW
 * @param inputChestPos       Position of the input chest
 * @param searchRadius        Search radius used for finding category signs
 * @param totalItemsProcessed Total number of items that were in the input
 * @param totalItemsSorted    Number of items successfully sorted
 * @param durationMs          Duration of the operation in milliseconds
 * @param status              SUCCESS, PARTIAL_SUCCESS, or FAILED
 * @param errorMessage        Error message if status is FAILED
 * @param movements           List of individual item movements (for FULL detail)
 * @param categorySummary     Summary of items per category (for SUMMARY detail)
 * @param unknownItems        Items that had no matching category
 * @param overflowCategories  Categories that ran out of storage space
 */
public record SortAuditEntry(
        UUID operationId,
        Instant timestamp,
        String playerName,
        UUID playerUuid,
        String dimension,
        OperationType operationType,
        BlockPos inputChestPos,
        int searchRadius,
        int totalItemsProcessed,
        int totalItemsSorted,
        long durationMs,
        OperationStatus status,
        String errorMessage,
        List<ItemMovementRecord> movements,
        Map<String, Integer> categorySummary,
        Set<String> unknownItems,
        Set<String> overflowCategories
) {
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    /**
     * Converts this entry to a JSON string.
     */
    public String toJson(AuditConfig.DetailLevel detailLevel) {
        JsonObject json = new JsonObject();

        // Core fields (always included)
        json.addProperty("operationId", operationId.toString());
        json.addProperty("timestamp", ISO_FORMATTER.format(timestamp));
        json.addProperty("playerName", playerName);
        json.addProperty("playerUuid", playerUuid.toString());
        json.addProperty("dimension", dimension);
        json.addProperty("operationType", operationType.name());
        json.add("inputChestPos", AuditGsonHelper.GSON.toJsonTree(inputChestPos));
        json.addProperty("searchRadius", searchRadius);
        json.addProperty("totalItemsProcessed", totalItemsProcessed);
        json.addProperty("totalItemsSorted", totalItemsSorted);
        json.addProperty("durationMs", durationMs);
        json.addProperty("status", status.name());
        json.addProperty("errorMessage", errorMessage);

        // Detail-level dependent fields
        if (detailLevel == AuditConfig.DetailLevel.FULL && movements != null && !movements.isEmpty()) {
            json.add("movements", AuditGsonHelper.GSON.toJsonTree(movements));
        }

        if ((detailLevel == AuditConfig.DetailLevel.SUMMARY || detailLevel == AuditConfig.DetailLevel.FULL)
                && categorySummary != null && !categorySummary.isEmpty()) {
            json.add("categorySummary", AuditGsonHelper.GSON.toJsonTree(categorySummary));
        }

        // Always include issues
        json.add("unknownItems", toJsonArray(unknownItems));
        json.add("overflowCategories", toJsonArray(overflowCategories));

        return AuditGsonHelper.GSON.toJson(json);
    }

    private static JsonArray toJsonArray(Set<String> set) {
        JsonArray array = new JsonArray();
        if (set != null) {
            for (String s : set) {
                array.add(s);
            }
        }
        return array;
    }
}


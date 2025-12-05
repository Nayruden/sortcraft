package net.sortcraft.audit;

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
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // Core fields
        sb.append("\"operationId\":\"").append(operationId).append("\",");
        sb.append("\"timestamp\":\"").append(ISO_FORMATTER.format(timestamp)).append("\",");
        sb.append("\"playerName\":\"").append(escapeJson(playerName)).append("\",");
        sb.append("\"playerUuid\":\"").append(playerUuid).append("\",");
        sb.append("\"dimension\":\"").append(escapeJson(dimension)).append("\",");
        sb.append("\"operationType\":\"").append(operationType).append("\",");
        sb.append("\"inputChestPos\":");
        appendBlockPos(sb, inputChestPos);
        sb.append(",");
        sb.append("\"searchRadius\":").append(searchRadius).append(",");
        sb.append("\"totalItemsProcessed\":").append(totalItemsProcessed).append(",");
        sb.append("\"totalItemsSorted\":").append(totalItemsSorted).append(",");
        sb.append("\"durationMs\":").append(durationMs).append(",");
        sb.append("\"status\":\"").append(status).append("\",");
        sb.append("\"errorMessage\":").append(errorMessage != null ? "\"" + escapeJson(errorMessage) + "\"" : "null").append(",");

        // Detail-level dependent fields
        if (detailLevel == AuditConfig.DetailLevel.FULL && movements != null && !movements.isEmpty()) {
            sb.append("\"movements\":[");
            for (int i = 0; i < movements.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(movements.get(i).toJson());
            }
            sb.append("],");
        }

        if ((detailLevel == AuditConfig.DetailLevel.SUMMARY || detailLevel == AuditConfig.DetailLevel.FULL)
                && categorySummary != null && !categorySummary.isEmpty()) {
            sb.append("\"categorySummary\":{");
            boolean first = true;
            for (Map.Entry<String, Integer> entry : categorySummary.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(escapeJson(entry.getKey())).append("\":").append(entry.getValue());
                first = false;
            }
            sb.append("},");
        }

        // Always include issues
        sb.append("\"unknownItems\":");
        appendStringSet(sb, unknownItems);
        sb.append(",");
        sb.append("\"overflowCategories\":");
        appendStringSet(sb, overflowCategories);

        sb.append("}");
        return sb.toString();
    }

    private void appendBlockPos(StringBuilder sb, BlockPos pos) {
        if (pos == null) {
            sb.append("null");
        } else {
            sb.append("{\"x\":").append(pos.getX())
              .append(",\"y\":").append(pos.getY())
              .append(",\"z\":").append(pos.getZ()).append("}");
        }
    }

    private void appendStringSet(StringBuilder sb, Set<String> set) {
        sb.append("[");
        if (set != null && !set.isEmpty()) {
            boolean first = true;
            for (String s : set) {
                if (!first) sb.append(",");
                sb.append("\"").append(escapeJson(s)).append("\"");
                first = false;
            }
        }
        sb.append("]");
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


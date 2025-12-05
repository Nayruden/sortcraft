package net.sortcraft.audit;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.sortcraft.sorting.SortingResults;

import java.time.Instant;
import java.util.*;

/**
 * Collector that accumulates audit data during a sort operation.
 * Create at the start of a sort, record movements during, and finalize at completion.
 */
public class SortAuditLog {
    private final UUID operationId;
    private final Instant startTime;
    private final String playerName;
    private final UUID playerUuid;
    private final String dimension;
    private final OperationType operationType;
    private final BlockPos inputChestPos;
    private final int searchRadius;

    private final List<ItemMovementRecord> movements = new ArrayList<>();
    private int totalItemsProcessed = 0;

    private SortAuditLog(ServerPlayer player, ServerLevel world, BlockPos inputChestPos,
                         int searchRadius, boolean preview) {
        this.operationId = UUID.randomUUID();
        this.startTime = Instant.now();
        this.playerName = player.getName().getString();
        this.playerUuid = player.getUUID();
        this.dimension = world.dimension().location().toString();
        this.operationType = preview ? OperationType.PREVIEW : OperationType.SORT;
        this.inputChestPos = inputChestPos;
        this.searchRadius = searchRadius;
    }

    /**
     * Private constructor for test use - allows creating audit log without a real player.
     */
    private SortAuditLog(String playerName, UUID playerUuid, String dimension,
                         BlockPos inputChestPos, int searchRadius, boolean preview) {
        this.operationId = UUID.randomUUID();
        this.startTime = Instant.now();
        this.playerName = playerName;
        this.playerUuid = playerUuid;
        this.dimension = dimension;
        this.operationType = preview ? OperationType.PREVIEW : OperationType.SORT;
        this.inputChestPos = inputChestPos;
        this.searchRadius = searchRadius;
    }

    /**
     * Starts a new audit log for a sort operation.
     *
     * @param player        The player initiating the sort
     * @param world         The world where sorting occurs
     * @param inputChestPos Position of the input chest
     * @param searchRadius  Search radius for category signs
     * @param preview       True if this is a preview operation
     * @return A new SortAuditLog instance
     */
    public static SortAuditLog start(ServerPlayer player, ServerLevel world, BlockPos inputChestPos,
                                     int searchRadius, boolean preview) {
        return new SortAuditLog(player, world, inputChestPos, searchRadius, preview);
    }

    /**
     * Starts a new audit log for testing purposes without requiring a real player.
     * This is intended for game tests where a ServerPlayer is not available.
     *
     * @param playerName    The player name to record
     * @param playerUuid    The player UUID to record
     * @param dimension     The dimension string
     * @param inputChestPos Position of the input chest
     * @param searchRadius  Search radius for category signs
     * @param preview       True if this is a preview operation
     * @return A new SortAuditLog instance
     */
    public static SortAuditLog startForTest(String playerName, UUID playerUuid, String dimension,
                                            BlockPos inputChestPos, int searchRadius, boolean preview) {
        return new SortAuditLog(playerName, playerUuid, dimension, inputChestPos, searchRadius, preview);
    }

    /**
     * Records an item movement.
     *
     * @param itemId         The item identifier
     * @param quantity       Number of items moved
     * @param category       The category sorted into
     * @param destinationPos Position of the destination chest
     * @param partial        True if only part of the stack was moved
     */
    public void recordMovement(String itemId, int quantity, String category,
                               BlockPos destinationPos, boolean partial) {
        movements.add(new ItemMovementRecord(itemId, quantity, category, destinationPos, partial));
    }

    /**
     * Records that items were processed (for tracking total items).
     *
     * @param count Number of items processed
     */
    public void recordItemsProcessed(int count) {
        totalItemsProcessed += count;
    }

    /**
     * Completes the audit and creates the final entry.
     *
     * @param results The sorting results
     * @return The completed SortAuditEntry
     */
    public SortAuditEntry complete(SortingResults results) {
        return complete(results, null);
    }

    /**
     * Completes the audit, logs it, and returns the entry.
     * This is the preferred method for production use.
     *
     * @param results The sorting results
     * @return The completed SortAuditEntry
     */
    public SortAuditEntry completeAndLog(SortingResults results) {
        SortAuditEntry entry = complete(results);
        SortAuditLogger.log(entry);
        return entry;
    }

    /**
     * Completes the audit with an error.
     *
     * @param results      The sorting results (may be partial or null)
     * @param errorMessage The error message
     * @return The completed SortAuditEntry
     */
    public SortAuditEntry complete(SortingResults results, String errorMessage) {
        long durationMs = System.currentTimeMillis() - startTime.toEpochMilli();

        int sorted = results != null ? results.sorted : 0;
        Set<String> unknownItems = results != null ? results.unknownItems : Set.of();
        Set<String> overflowCategories = results != null ? results.overflowCategories : Set.of();
        Map<String, Integer> categoryCounts = results != null ? results.categoryCounts : Map.of();

        // Determine status
        OperationStatus status;
        if (errorMessage != null) {
            status = OperationStatus.FAILED;
        } else if (!unknownItems.isEmpty() || !overflowCategories.isEmpty()) {
            status = OperationStatus.PARTIAL_SUCCESS;
        } else {
            status = OperationStatus.SUCCESS;
        }

        // Use recorded processed count, or fall back to sorted + leftovers
        int processed = totalItemsProcessed > 0 ? totalItemsProcessed : sorted;
        if (results != null && !results.leftovers.isEmpty()) {
            processed = sorted + results.leftovers.stream().mapToInt(s -> s.getCount()).sum();
        }

        return new SortAuditEntry(
                operationId,
                startTime,
                playerName,
                playerUuid,
                dimension,
                operationType,
                inputChestPos,
                searchRadius,
                processed,
                sorted,
                durationMs,
                status,
                errorMessage,
                List.copyOf(movements),
                Map.copyOf(categoryCounts),
                Set.copyOf(unknownItems),
                Set.copyOf(overflowCategories)
        );
    }

    public UUID getOperationId() {
        return operationId;
    }

    public OperationType getOperationType() {
        return operationType;
    }
}


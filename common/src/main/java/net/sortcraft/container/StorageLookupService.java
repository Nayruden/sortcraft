package net.sortcraft.container;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

/**
 * Service interface for platform-specific storage lookup.
 *
 * <p>Each platform (Fabric, NeoForge) provides an implementation that checks for:
 * <ol>
 *   <li>Vanilla Container (via ContainerStorage wrapper)</li>
 *   <li>Platform-specific storage APIs (Fabric Transfer API, NeoForge IItemHandler)</li>
 * </ol>
 *
 * <p>Implementations are discovered via {@link java.util.ServiceLoader}.
 */
public interface StorageLookupService {

    /**
     * Gets the SortCraftStorage at the given position, if any.
     *
     * @param world The server level
     * @param pos The block position to check
     * @return An Optional containing the storage, or empty if no storage found
     */
    Optional<SortCraftStorage> getStorageAt(ServerLevel world, BlockPos pos);

    /**
     * Checks whether the block at the given position is a storage block.
     *
     * @param world The server level
     * @param pos The block position to check
     * @return true if the block has storage capabilities
     */
    boolean isStorageBlock(ServerLevel world, BlockPos pos);
}


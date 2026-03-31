package net.sortcraft.container;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Platform-specific storage lookup.
 *
 * <p>Each platform (Fabric, NeoForge) provides an implementation that checks for:
 * <ol>
 *   <li>Vanilla Container (via ContainerStorage wrapper)</li>
 *   <li>Platform-specific storage APIs (Fabric Transfer API, NeoForge IItemHandler)</li>
 * </ol>
 *
 * <p>Implementations are discovered via {@link ServiceLoader}:
 * <ul>
 *   <li>Fabric: {@code net.sortcraft.container.fabric.StorageLookupImpl}</li>
 *   <li>NeoForge: {@code net.sortcraft.container.neoforge.StorageLookupImpl}</li>
 * </ul>
 */
public final class StorageLookup {

    private static final StorageLookupService INSTANCE = ServiceLoader.load(StorageLookupService.class)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No StorageLookupService implementation found"));

    private StorageLookup() {}

    /**
     * Gets the SortCraftStorage at the given position, if any.
     *
     * <p>Checks vanilla Container first (including ChestBlock double-chest handling),
     * then falls back to platform-specific storage APIs.
     *
     * @param world The server level
     * @param pos The block position to check
     * @return An Optional containing the storage, or empty if no storage found
     */
    public static Optional<SortCraftStorage> getStorageAt(ServerLevel world, BlockPos pos) {
        return INSTANCE.getStorageAt(world, pos);
    }

    /**
     * Checks whether the block at the given position is a storage block.
     *
     * <p>This is a lightweight check used for stack boundary detection.
     * Checks vanilla Container first, then platform-specific APIs.
     *
     * @param world The server level
     * @param pos The block position to check
     * @return true if the block has storage capabilities
     */
    public static boolean isStorageBlock(ServerLevel world, BlockPos pos) {
        return INSTANCE.isStorageBlock(world, pos);
    }
}


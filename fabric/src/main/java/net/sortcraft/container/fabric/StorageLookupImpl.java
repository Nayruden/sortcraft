package net.sortcraft.container.fabric;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.sortcraft.container.ContainerHelper;
import net.sortcraft.container.SortCraftStorage;

import java.util.Optional;

/**
 * Fabric implementation of StorageLookup.
 *
 * <p>Priority order:
 * <ol>
 *   <li>Vanilla Container (ChestBlock double-chest, then generic instanceof)</li>
 *   <li>Fabric Transfer API ({@code ItemStorage.SIDED} — supports Storage Drawers, etc.)</li>
 * </ol>
 */
public class StorageLookupImpl {

    public static Optional<SortCraftStorage> getStorageAt(ServerLevel world, BlockPos pos) {
        // Check vanilla Container first (ChestBlock double-chest, then generic instanceof)
        Optional<SortCraftStorage> vanilla = ContainerHelper.getVanillaStorageAt(world, pos);
        if (vanilla.isPresent()) return vanilla;

        // Fallback: Fabric Transfer API (e.g., Storage Drawers, other modded storage)
        Storage<ItemVariant> transferStorage = ItemStorage.SIDED.find(world, pos, null);
        if (transferStorage != null) {
            return Optional.of(new FabricTransferStorage(transferStorage));
        }

        return Optional.empty();
    }

    public static boolean isStorageBlock(ServerLevel world, BlockPos pos) {
        return getStorageAt(world, pos).isPresent();
    }
}


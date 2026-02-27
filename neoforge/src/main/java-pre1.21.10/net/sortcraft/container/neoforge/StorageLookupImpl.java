package net.sortcraft.container.neoforge;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.sortcraft.container.ContainerHelper;
import net.sortcraft.container.SortCraftStorage;

import java.util.Optional;

/**
 * NeoForge implementation of StorageLookup for NeoForge 21.1-21.8 (MC 1.21.1-1.21.8).
 *
 * <p>Uses the old {@code Capabilities.ItemHandler.BLOCK} capability.
 *
 * <p>Priority order:
 * <ol>
 *   <li>Vanilla Container (ChestBlock double-chest, then generic instanceof)</li>
 *   <li>NeoForge IItemHandler capability (e.g., SophisticatedStorage, other modded storage)</li>
 * </ol>
 */
public class StorageLookupImpl {

    public static Optional<SortCraftStorage> getStorageAt(ServerLevel world, BlockPos pos) {
        // Check vanilla Container first (ChestBlock double-chest, then generic instanceof)
        Optional<SortCraftStorage> vanilla = ContainerHelper.getVanillaStorageAt(world, pos);
        if (vanilla.isPresent()) return vanilla;

        // Fallback: NeoForge IItemHandler capability (e.g., SophisticatedStorage, other modded storage)
        IItemHandler itemHandler = world.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (itemHandler != null) {
            return Optional.of(new NeoForgeItemHandlerStorage(itemHandler));
        }

        return Optional.empty();
    }

    public static boolean isStorageBlock(ServerLevel world, BlockPos pos) {
        return getStorageAt(world, pos).isPresent();
    }
}


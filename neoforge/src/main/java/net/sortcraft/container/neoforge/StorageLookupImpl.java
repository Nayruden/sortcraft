package net.sortcraft.container.neoforge;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.sortcraft.container.ContainerHelper;
import net.sortcraft.container.SortCraftStorage;
import net.sortcraft.container.StorageLookupService;

import java.util.Optional;

/**
 * NeoForge implementation of StorageLookup for NeoForge 21.9+ (MC 1.21.10+).
 *
 * <p>Uses the new {@code Capabilities.Item.BLOCK} capability introduced in the
 * NeoForge Transfer Rework (21.9.1-beta), then wraps via {@code IItemHandler.of()}.
 *
 * <p>Priority order:
 * <ol>
 *   <li>Vanilla Container (ChestBlock double-chest, then generic instanceof)</li>
 *   <li>NeoForge Item capability via IItemHandler wrapper (e.g., SophisticatedStorage, other modded storage)</li>
 * </ol>
 */
public class StorageLookupImpl implements StorageLookupService {

    @Override
    public Optional<SortCraftStorage> getStorageAt(ServerLevel world, BlockPos pos) {
        // Check vanilla Container first (ChestBlock double-chest, then generic instanceof)
        Optional<SortCraftStorage> vanilla = ContainerHelper.getVanillaStorageAt(world, pos);
        if (vanilla.isPresent()) return vanilla;

        // Fallback: NeoForge Item capability (new API in NeoForge 21.9+)
        // Capabilities.Item.BLOCK returns ResourceHandler<ItemResource>, wrap with IItemHandler.of()
        var resourceHandler = world.getCapability(Capabilities.Item.BLOCK, pos, null);
        if (resourceHandler != null) {
            IItemHandler itemHandler = IItemHandler.of(resourceHandler);
            return Optional.of(new NeoForgeItemHandlerStorage(itemHandler));
        }

        return Optional.empty();
    }

    @Override
    public boolean isStorageBlock(ServerLevel world, BlockPos pos) {
        return getStorageAt(world, pos).isPresent();
    }
}


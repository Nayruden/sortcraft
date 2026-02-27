package net.sortcraft.container;

import net.minecraft.core.BlockPos;

/**
 * Reference to a storage block at a specific position with its storage abstraction.
 */
public class ChestRef {
    private final BlockPos pos;
    private final SortCraftStorage storage;

    public ChestRef(BlockPos pos, SortCraftStorage storage) {
        this.pos = pos;
        this.storage = storage;
    }

    public BlockPos getPos() {
        return pos;
    }

    /**
     * Returns the storage abstraction for this block.
     */
    public SortCraftStorage getStorage() {
        return storage;
    }
}


package net.sortcraft.container;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;

/**
 * Reference to a chest at a specific position with its inventory.
 */
public class ChestRef {
    private final BlockPos pos;
    private final Container inventory;

    public ChestRef(BlockPos pos, Container inventory) {
        this.pos = pos;
        this.inventory = inventory;
    }

    public BlockPos getPos() {
        return pos;
    }

    public Container getInventory() {
        return inventory;
    }
}


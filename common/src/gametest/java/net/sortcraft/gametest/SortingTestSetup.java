package net.sortcraft.gametest;

import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * Holds the positions of chests set up for a sorting test.
 * Returned by TestScenarios methods to provide test code with the positions it needs.
 */
public record SortingTestSetup(
    BlockPos inputPos,
    List<BlockPos> categoryPositions
) {
    /**
     * Convenience method to get the first (or only) category chest position.
     * Most tests have a single category chest, so this avoids .get(0) everywhere.
     */
    public BlockPos categoryPos() {
        if (categoryPositions.isEmpty()) {
            throw new IllegalStateException("No category positions in this setup");
        }
        return categoryPositions.get(0);
    }

    /**
     * Returns the category chest position at the given index.
     * For vertical stacks, index 0 is the bottom chest.
     */
    public BlockPos categoryPos(int index) {
        return categoryPositions.get(index);
    }

    /**
     * Returns the number of category chest positions.
     */
    public int categoryCount() {
        return categoryPositions.size();
    }
}


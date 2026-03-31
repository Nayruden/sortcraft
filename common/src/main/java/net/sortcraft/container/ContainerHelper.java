package net.sortcraft.container;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.sortcraft.compat.BundleHelper;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Utility methods for working with chests and containers.
 */
public final class ContainerHelper {
    private static final Pattern CATEGORY_SIGN_PATTERN = Pattern.compile("\\[.+?]");
    private ContainerHelper() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("sortcraft");
    public static final int SHULKER_BOX_SIZE = 27;

    /**
     * Checks for a vanilla Container at the given position.
     * Handles ChestBlock double-chest merging and generic BlockEntity instanceof Container.
     *
     * <p>This is the shared vanilla container detection logic used by all platform-specific
     * {@code StorageLookupImpl} classes before their platform-specific fallback.
     *
     * @param world The server level
     * @param pos The block position to check
     * @return An Optional containing a ContainerStorage wrapper, or empty if no vanilla container found
     */
    public static Optional<SortCraftStorage> getVanillaStorageAt(ServerLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);

        // Special case: ChestBlock has merged double-chest inventory
        if (state.getBlock() instanceof ChestBlock chestBlock) {
            Container container = ChestBlock.getContainer(chestBlock, state, world, pos, true);
            if (container != null) {
                return Optional.of(new ContainerStorage(container));
            }
        }

        // Generic case: any block entity that implements Container
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof Container container) {
            return Optional.of(new ContainerStorage(container));
        }

        return Optional.empty();
    }

    /**
     * Gets all block positions for a container (handles double chests).
     * For double chests, returns both halves. For all other containers, returns just the single position.
     */
    public static List<BlockPos> getContainerBlocks(BlockPos pos, ServerLevel world) {
        BlockState state = world.getBlockState(pos);
        List<BlockPos> blocks = new ArrayList<>();
        blocks.add(pos);

        // Only chest blocks have CHEST_TYPE and HORIZONTAL_FACING properties (double chest support)
        if (!(state.getBlock() instanceof ChestBlock)) return blocks;

        ChestType chestType = state.getValue(BlockStateProperties.CHEST_TYPE);
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);

        if (chestType == ChestType.SINGLE) return blocks;
        if (chestType == ChestType.RIGHT) {
            blocks.add(pos.relative(facing.getCounterClockWise()));
        } else {
            blocks.add(pos.relative(facing.getClockWise()));
        }
        return blocks;
    }

    /**
     * Gets the container position attached to a sign.
     * Supports any block that has a storage (vanilla Container, Fabric Transfer API, NeoForge IItemHandler).
     *
     * @param signPos the position of the sign
     * @param signState the block state of the sign
     * @param world the server level
     * @return the position of the attached storage block, or null if not a wall sign or no storage attached
     */
    public static BlockPos getAttachedContainerPos(BlockPos signPos, BlockState signState, ServerLevel world) {
        // Check if the block state has the HORIZONTAL_FACING property before accessing it
        if (!signState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return null;
        }
        Direction attachedDirection = signState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        attachedDirection = attachedDirection.getOpposite();

        BlockPos containerPos = signPos.relative(attachedDirection);
        if (StorageLookup.isStorageBlock(world, containerPos)) {
            return containerPos;
        }
        return null;
    }

    /**
     * Checks if an ItemStack is a bundle.
     */
    public static boolean isBundle(ItemStack stack) {
        return stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, null) != null;
    }

    /**
     * Gets the contents of a container item (bundle or shulker box).
     * Returns null if the item is not a container.
     */
    public static Iterable<ItemStack> getStacksIfContainer(ItemStack stack) {
        BundleContents bundle = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, null);
        if (bundle != null) {
            return BundleHelper.getItems(bundle);
        }

        ItemContainerContents container = stack.getOrDefault(DataComponents.CONTAINER, null);
        if (container != null) {
            NonNullList<ItemStack> containerStacks = NonNullList.withSize(SHULKER_BOX_SIZE, ItemStack.EMPTY);
            container.copyInto(containerStacks);
            return containerStacks;
        }

        return null;
    }

    /**
     * Maximum number of chests to scan in a vertical stack.
     * Prevents infinite loops in case of corrupted world data.
     */
    private static final int MAX_CHEST_STACK_HEIGHT = 256;

    /**
     * Collects all storage blocks in a vertical stack starting from the given position.
     * Supports any block with a storage (vanilla Container, Fabric Transfer API, NeoForge IItemHandler).
     * Stops when a storage block with a category sign is encountered below, or when
     * the maximum stack height is reached.
     */
    public static List<ChestRef> collectContainerStack(ServerLevel world, BlockPos startPos) {
        List<ChestRef> result = new ArrayList<>();
        BlockPos cur = startPos;
        int iterations = 0;

        while (iterations++ < MAX_CHEST_STACK_HEIGHT) {
            // Try to get the storage at the current position
            Optional<SortCraftStorage> storageOpt = StorageLookup.getStorageAt(world, cur);
            if (storageOpt.isPresent()) {
                result.add(new ChestRef(cur, storageOpt.get()));
                LOGGER.trace("[containerstack] Added storage at {}", cur);
            }

            BlockPos below = cur.below();
            // Check if the block below is any kind of storage
            if (!StorageLookup.isStorageBlock(world, below)) {
                LOGGER.trace("[containerstack] Block below {} is not a storage block. Done.", cur);
                break;
            }

            List<BlockPos> blocksForContainer = getContainerBlocks(below, world);
            boolean foundCategorySign = false;

            for (Direction dir : Direction.Plane.HORIZONTAL) {
                for (BlockPos containerPos : blocksForContainer) {
                    BlockPos signPos = containerPos.relative(dir);
                    BlockState signState = world.getBlockState(signPos);

                    LOGGER.trace("[containerstack] Checking containerPos {} and direction {} - pos {} for a sign.", containerPos, dir, signPos);

                    if (!(signState.getBlock() instanceof WallSignBlock)) continue;
                    if (!signPos.relative(signState.getValue(WallSignBlock.FACING).getOpposite()).equals(containerPos)) continue;

                    BlockEntity signBe = world.getBlockEntity(signPos);
                    if (!(signBe instanceof SignBlockEntity sign)) continue;
                    String line = findCategoryTextOnSign(sign);
                    if (line == null) continue;

                    LOGGER.trace("[containerstack] Found category sign at {} - {}. Stopping stack here.", signPos, line);
                    foundCategorySign = true;
                    break;
                }
                if (foundCategorySign) break;
            }

            if (foundCategorySign) {
                Collections.reverse(result);
                return result;
            }

            cur = below;
        }

        Collections.reverse(result);
        return result;
    }

    /**
     * Finds category text (text matching [something]) on a sign.
     * @return The matching text, or null if not found
     */
    private static String findCategoryTextOnSign(SignBlockEntity sign) {
        for (int i = 0; i < 4; i++) {
            String frontLine = sign.getFrontText().getMessage(i, false).getString().trim();
            String backLine = sign.getBackText().getMessage(i, false).getString().trim();

            if (CATEGORY_SIGN_PATTERN.matcher(frontLine).find()) return frontLine;
            if (CATEGORY_SIGN_PATTERN.matcher(backLine).find()) return backLine;
        }
        return null;
    }

}


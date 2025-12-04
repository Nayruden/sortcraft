package net.sortcraft.container;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
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
     * Gets all block positions for a chest (handles double chests).
     */
    public static List<BlockPos> getChestBlocks(BlockPos pos, ServerLevel world) {
        BlockState state = world.getBlockState(pos);
        List<BlockPos> blocks = new ArrayList<>();
        blocks.add(pos);

        // Only chest blocks have CHEST_TYPE and HORIZONTAL_FACING properties
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
     * Gets the chest position attached to a sign.
     */
    public static BlockPos getAttachedChestPos(BlockPos signPos, BlockState signState, ServerLevel world) {
        Direction attachedDirection = signState.getValue(BlockStateProperties.HORIZONTAL_FACING);

        if (attachedDirection == null) return null;
        attachedDirection = attachedDirection.getOpposite();

        BlockPos chestPos = signPos.relative(attachedDirection);
        BlockEntity be = world.getBlockEntity(chestPos);
        if (be instanceof ChestBlockEntity) {
            return chestPos;
        }
        return null;
    }

    /**
     * Gets the container for a chest at the given position.
     */
    public static Optional<Container> getChestContainer(ServerLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock chestBlock)) return Optional.empty();
        return Optional.ofNullable(ChestBlock.getContainer(chestBlock, state, world, pos, true));
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
            List<ItemStack> bundleItems = new ArrayList<>();
            bundle.items().forEach(bundleItems::add);
            return bundleItems;
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
     * Collects all chests in a vertical stack starting from the given position.
     * Stops when a chest with a category sign is encountered below.
     */
    public static List<ChestRef> collectChestStack(ServerLevel world, BlockPos startPos) {
        List<ChestRef> result = new ArrayList<>();
        BlockPos cur = startPos;

        while (true) {
            BlockState state = world.getBlockState(cur);

            Block block = state.getBlock();
            if (block instanceof ChestBlock chestBlock) {
                Container inv = ChestBlock.getContainer(chestBlock, state, world, cur, true);
                if (inv != null) {
                    result.add(new ChestRef(cur, inv));
                    LOGGER.trace("[cheststack] Added chest at {}", cur);
                }
            }

            BlockPos below = cur.below();
            if (!(world.getBlockEntity(below) instanceof ChestBlockEntity)) {
                LOGGER.trace("[cheststack] Block below {} is not a chest. Done.", cur);
                break;
            }

            List<BlockPos> blocksForChest = getChestBlocks(below, world);
            boolean foundCategorySign = false;

            for (Direction dir : Direction.Plane.HORIZONTAL) {
                for (BlockPos chestPos : blocksForChest) {
                    BlockPos signPos = chestPos.relative(dir);
                    BlockState signState = world.getBlockState(signPos);

                    LOGGER.trace("[cheststack] Checking chestPos {} and direction {} - pos {} for a sign.", chestPos, dir, signPos);

                    if (!(signState.getBlock() instanceof WallSignBlock)) continue;
                    if (!signPos.relative(signState.getValue(WallSignBlock.FACING).getOpposite()).equals(chestPos)) continue;

                    BlockEntity signBe = world.getBlockEntity(signPos);
                    if (!(signBe instanceof SignBlockEntity sign)) continue;
                    String line = findCategoryTextOnSign(sign);
                    if (line == null) continue;

                    LOGGER.trace("[cheststack] Found category sign at {} - {}. Stopping stack here.", signPos, line);
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


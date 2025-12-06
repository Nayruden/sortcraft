package net.sortcraft.sorting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.sortcraft.audit.AuditConfig;
import net.sortcraft.audit.SortAuditLog;
import net.sortcraft.category.CategoryLoader;
import net.sortcraft.config.ConfigManager;
import net.sortcraft.category.CategoryNode;
import net.sortcraft.command.CommandHandler;
import net.sortcraft.container.ChestRef;
import net.sortcraft.container.ContainerHelper;
import net.sortcraft.container.SortContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Core sorting engine that distributes items from input containers to categorized destination chests.
 *
 * <p>The sorting process works as follows:
 * <ol>
 *   <li>Items are taken from the source container (the chest with an [input] sign)</li>
 *   <li>Each item is matched against loaded categories using {@link CategoryLoader#getMatchingCategories}</li>
 *   <li>Items are distributed to destination chests (those with [category] signs) in priority order</li>
 *   <li>Containers (bundles, shulker boxes) are handled recursively - their contents are sorted individually</li>
 * </ol>
 *
 * <p>The engine supports both actual sorting and preview mode, where items are counted but not moved.
 *
 * @see SortingResults for the result structure
 * @see SortContext for the search context
 * @see CategoryLoader for category matching
 */
public final class SortingEngine {
    private SortingEngine() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("sortcraft");
    private static final int UNIFORM_CONTAINER_THRESHOLD = 10;

    /**
     * Sorts all items from the source container into categorized chests.
     *
     * <p>This is the main entry point for sorting operations. It handles:
     * <ul>
     *   <li>Regular items - matched by category and distributed to destination chests</li>
     *   <li>Containers (bundles, shulker boxes) - contents sorted recursively</li>
     *   <li>Uniform containers - if a container has 10+ stacks of the same item, the container itself is sorted</li>
     *   <li>Cleanup - empty stacks are replaced with ItemStack.EMPTY to prevent save errors</li>
     * </ul>
     *
     * @param context The sort context containing position, search radius, and cached signs/containers
     * @param world The server level where sorting takes place
     * @param sourceContainer The container to sort items FROM (typically the [input] chest)
     * @param preview If true, only calculate what would be sorted without actually moving items
     * @return Results containing counts of sorted items, overflow categories, and unknown items
     */
    public static SortingResults sortFromContainer(SortContext context, ServerLevel world, Container sourceContainer, boolean preview) {
        return sortFromContainer(context, world, sourceContainer, preview, null);
    }

    /**
     * Sorts all items from the source container into categorized chests with optional audit logging.
     *
     * <p>Same as {@link #sortFromContainer(SortContext, ServerLevel, Container, boolean)} but with
     * audit logging support for tracking item movements.
     *
     * @param context The sort context containing position, search radius, and cached signs/containers
     * @param world The server level where sorting takes place
     * @param sourceContainer The container to sort items FROM (typically the [input] chest)
     * @param preview If true, only calculate what would be sorted without actually moving items
     * @param audit Optional audit log to record item movements (can be null to disable logging)
     * @return Results containing counts of sorted items, overflow categories, and unknown items
     */
    public static SortingResults sortFromContainer(SortContext context, ServerLevel world, Container sourceContainer,
                                                   boolean preview, SortAuditLog audit) {
        SortingResults results = sortStacks(context, world, ContainerHelper.containerToIterable(sourceContainer), preview, audit);

        // Clean up empty stacks (count=0) left behind by shrink() to prevent chunk save errors
        if (!preview) {
            cleanupContainer(sourceContainer);
        }

        return results;
    }

    /**
     * Internal method that sorts items from an iterable into categorized chests.
     * Handles containers (bundles, shulker boxes) recursively.
     * Note: Callers must handle cleanup of source containers if using containerToIterable().
     */
    private static SortingResults sortStacks(SortContext context, ServerLevel world, Iterable<ItemStack> stacks,
                                             boolean preview, SortAuditLog audit) {
        SortingResults results = new SortingResults();

        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;

            LOGGER.debug("[sortinput] Sorting {} of {}", stack.getCount(), stack.getItem().toString());

            // Track items processed for audit
            if (audit != null) {
                audit.recordItemsProcessed(stack.getCount());
            }

            // Check if this is a container (bundle or shulker box)
            Iterable<ItemStack> innerStacks = ContainerHelper.getStacksIfContainer(stack);
            if (innerStacks != null) {
                UniformContainerCheckResult uniformCheck = checkUniformContainerContents(innerStacks, UNIFORM_CONTAINER_THRESHOLD);
                if (uniformCheck.meetsThreshold()) {
                    LOGGER.debug("[sortinput] Container has >={} stacks of same item '{}'. Sorting container itself.",
                            UNIFORM_CONTAINER_THRESHOLD, uniformCheck.uniformItemId());

                    // Use the uniform item's categories, but record the actual container in the audit
                    List<CategoryNode> cats = CategoryLoader.getMatchingCategoriesNoFilter(uniformCheck.uniformItemId());
                    ResourceLocation containerItemId = BuiltInRegistries.ITEM.getKey(stack.getItem());

                    // Create uniform contents info for audit
                    net.sortcraft.audit.UniformContainerContents uniformContents =
                            new net.sortcraft.audit.UniformContainerContents(
                                    uniformCheck.uniformItemId().toString(),
                                    uniformCheck.stackCount(),
                                    uniformCheck.totalItemCount()
                            );

                    sortSingleStack(context, world, preview, stack, cats, containerItemId, results, audit, uniformContents);
                    continue;
                }

                LOGGER.debug("[sortinput] Item is a container. Sorting contents of container.");
                SortingResults innerResults = sortStacks(context, world, innerStacks, preview, audit);
                results.sorted += innerResults.sorted;
                results.overflowCategories.addAll(innerResults.overflowCategories);
                results.unknownItems.addAll(innerResults.unknownItems);
                innerResults.categoryCounts.forEach((category, cnt) ->
                        results.categoryCounts.merge(category, cnt, Integer::sum)
                );

                if (!preview) {
                    if (ContainerHelper.isBundle(stack)) {
                        BundleContents bundleContents;
                        if (innerResults.leftovers.isEmpty()) bundleContents = new BundleContents(List.of());
                        else bundleContents = new BundleContents(innerResults.leftovers);
                        stack.set(DataComponents.BUNDLE_CONTENTS, bundleContents);
                    } else {
                        NonNullList<ItemStack> restored = NonNullList.withSize(ContainerHelper.SHULKER_BOX_SIZE, ItemStack.EMPTY);
                        for (int i = 0; i < innerResults.leftovers.size() && i < ContainerHelper.SHULKER_BOX_SIZE; i++) {
                            restored.set(i, innerResults.leftovers.get(i));
                        }
                        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(restored));
                    }
                }

                if (!innerResults.leftovers.isEmpty()) {
                    LOGGER.debug("[sortinput] Inner container not completely emptied, giving up further sorting.");
                    results.leftovers.add(stack);
                    continue;
                }
            }

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            List<CategoryNode> cats = CategoryLoader.getMatchingCategories(stack);
            sortSingleStack(context, world, preview, stack, cats, itemId, results, audit);
        }
        return results;
    }

    /**
     * Result of checking if a container has uniform contents meeting the threshold.
     *
     * @param uniformItemId   The item ID if uniform and meets threshold, null otherwise
     * @param stackCount      Number of non-empty stacks in the container
     * @param totalItemCount  Total number of items across all stacks
     */
    private record UniformContainerCheckResult(
            ResourceLocation uniformItemId,
            int stackCount,
            int totalItemCount
    ) {
        boolean meetsThreshold() {
            return uniformItemId != null;
        }
    }

    /**
     * Checks if a container has uniform contents (all same item type) meeting the threshold.
     * Returns detailed information about the contents for audit purposes.
     */
    private static UniformContainerCheckResult checkUniformContainerContents(Iterable<ItemStack> stacks, int threshold) {
        ResourceLocation singleItem = null;
        int stackCount = 0;
        int totalItemCount = 0;

        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());

            if (singleItem == null) {
                singleItem = itemId;
            } else if (!singleItem.equals(itemId)) {
                // Mixed items - return result indicating not uniform
                return new UniformContainerCheckResult(null, 0, 0);
            }
            stackCount++;
            totalItemCount += stack.getCount();
        }

        if (stackCount >= threshold) {
            return new UniformContainerCheckResult(singleItem, stackCount, totalItemCount);
        }
        return new UniformContainerCheckResult(null, stackCount, totalItemCount);
    }

    private static void sortSingleStack(SortContext context, ServerLevel world, boolean preview, ItemStack stack,
                                        List<CategoryNode> cats, ResourceLocation itemId, SortingResults results,
                                        SortAuditLog audit) {
        sortSingleStack(context, world, preview, stack, cats, itemId, results, audit, null);
    }

    private static void sortSingleStack(SortContext context, ServerLevel world, boolean preview, ItemStack stack,
                                        List<CategoryNode> cats, ResourceLocation itemId, SortingResults results,
                                        SortAuditLog audit, net.sortcraft.audit.UniformContainerContents uniformContents) {
        if (cats.isEmpty()) {
            LOGGER.debug("[sortinput] No categories found for item: {}", itemId);
            results.unknownItems.add(itemId.toString());
            results.leftovers.add(stack);
            return;
        }

        int stackSize = stack.getCount();
        int totalMoved = 0;
        String categoriesStr = CategoryNode.categoriesToStr(cats);

        // Extract metadata BEFORE distribution (stack may become empty after distribution)
        net.sortcraft.audit.ItemMetadata preExtractedMetadata = null;
        if (audit != null) {
            AuditConfig auditConfig = ConfigManager.getAuditConfig();
            boolean logMetadata = auditConfig != null && auditConfig.isLogItemMetadata();
            if (logMetadata && !stack.isEmpty()) {
                preExtractedMetadata = net.sortcraft.audit.ItemMetadataExtractor.extract(stack);
            }
            // If this is a uniform container, add the uniform contents info to metadata
            if (uniformContents != null) {
                if (preExtractedMetadata != null) {
                    preExtractedMetadata = preExtractedMetadata.withUniformContents(uniformContents);
                } else {
                    preExtractedMetadata = net.sortcraft.audit.ItemMetadata.ofUniformContents(uniformContents);
                }
            }
        }

        for (CategoryNode category : cats) {
            List<ChestRef> categoryChests = findCategoryChests(context, world, category.name);
            if (categoryChests.isEmpty()) continue;

            int moved = distributeToChests(stack, categoryChests, preview);
            totalMoved += moved;
            if (moved > 0) {
                results.sorted += moved;
                results.categoryCounts.merge(category.name, moved, Integer::sum);
                LOGGER.debug("[sortinput] Moved {} of item {}", moved, itemId);

                // Record movement in audit log with pre-extracted metadata
                if (audit != null) {
                    BlockPos destPos = categoryChests.isEmpty() ? null : categoryChests.get(0).getPos();
                    boolean partial = totalMoved < stackSize;
                    audit.recordMovement(itemId.toString(), moved, category.name, destPos, partial, preExtractedMetadata);
                }

                if (preview && totalMoved >= stackSize) break;
            }
        }

        if (totalMoved < stackSize) {
            LOGGER.debug("[sortinput] Overflow: Could not store (all of) item '{}' -> categories '{}'", itemId, categoriesStr);
            results.overflowCategories.add(cats.getFirst().name);
            results.leftovers.add(stack);
        }
    }

    /**
     * Finds all chests associated with a category sign.
     *
     * <p>Searches for a wall sign with text matching "[categoryName]" and returns
     * all chests in the vertical stack attached to that sign. Chests are returned
     * in bottom-to-top order for filling.
     *
     * @param context The sort context with cached sign positions
     * @param world The server level
     * @param categoryName The category name to search for (without brackets)
     * @return List of ChestRef objects for the category's chests, or empty list if not found
     */
    public static List<ChestRef> findCategoryChests(SortContext context, ServerLevel world, String categoryName) {
        String signText = CommandHandler.formatSignText(categoryName);
        SignBlockEntity sign = context.findSign(signText);

        if (sign == null) {
            LOGGER.trace("[findchests] No sign found for category '{}'", categoryName);
            return Collections.emptyList();
        }

        BlockPos signPos = sign.getBlockPos();
        BlockState signState = world.getBlockState(signPos);

        if (!(signState.getBlock() instanceof WallSignBlock)) {
            LOGGER.trace("[findchests] Sign at {} is not a wall sign", signPos);
            return Collections.emptyList();
        }

        BlockPos chestPos = ContainerHelper.getAttachedChestPos(signPos, signState, world);
        if (chestPos == null) {
            LOGGER.trace("[findchests] Sign at {} is not attached to a chest", signPos);
            return Collections.emptyList();
        }

        return ContainerHelper.collectChestStack(world, chestPos);
    }

    /**
     * Distributes items from a stack to the given chests.
     *
     * <p>Items are distributed using a two-pass algorithm:
     * <ol>
     *   <li>First pass: merge with existing stacks of the same item type</li>
     *   <li>Second pass: fill empty slots with new stacks</li>
     * </ol>
     *
     * <p>Chests are processed in order (typically bottom-to-top for vertical stacks).
     * In preview mode, the stack is not modified.
     *
     * @param stack The item stack to distribute (will be shrunk by the amount moved unless preview)
     * @param chests The destination chests to fill
     * @param preview If true, calculate space without actually moving items
     * @return The number of items that were (or would be) moved
     */
    public static int distributeToChests(ItemStack stack, List<ChestRef> chests, boolean preview) {
        int originalCount = stack.getCount();
        int toSort = originalCount;
        int maxStackSize = Math.min(stack.getMaxStackSize(), 64);

        for (ChestRef ref : chests) {
            Container inv = ref.getInventory();

            // First pass: merge with existing stacks
            for (int slot = 0; slot < inv.getContainerSize() && toSort > 0; slot++) {
                ItemStack target = inv.getItem(slot);
                if (!target.isEmpty() && ItemStack.isSameItemSameComponents(stack, target) && target.getCount() < target.getMaxStackSize()) {
                    int space = target.getMaxStackSize() - target.getCount();
                    int move = Math.min(space, toSort);
                    if (!preview) target.grow(move);
                    toSort -= move;
                }
            }

            // Second pass: fill empty slots
            for (int slot = 0; slot < inv.getContainerSize() && toSort > 0; slot++) {
                if (inv.getItem(slot).isEmpty()) {
                    int move = Math.min(toSort, maxStackSize);
                    if (!preview) {
                        ItemStack toPut = stack.copy();
                        toPut.setCount(move);
                        inv.setItem(slot, toPut);
                    }
                    toSort -= move;
                }
            }

            if (toSort == 0) break;
        }

        int moved = originalCount - toSort;
        if (!preview) stack.shrink(moved);
        return moved;
    }

    /**
     * Cleans up a container after sorting by replacing empty stacks with ItemStack.EMPTY.
     *
     * <p>This is necessary because {@link ItemStack#shrink(int)} can leave stacks with count=0
     * in the container, which causes errors when Minecraft tries to save the chunk.
     * This method ensures all empty slots contain the canonical ItemStack.EMPTY instance.
     *
     * @param container The container to clean up
     */
    public static void cleanupContainer(Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                container.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    /**
     * Summarizes a set of items or categories into a formatted message string.
     *
     * <p>Creates a bulleted list with the given header. Used for displaying
     * overflow categories and unknown items in sorting result messages.
     *
     * @param items The set of item/category names to summarize
     * @param header The header text to display before the list
     * @return Formatted string with header and bulleted items, or empty string if items is empty
     */
    public static String summarize(Set<String> items, String header) {
        if (items.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(header).append("\n");
        for (String item : items) {
            sb.append("- ").append(item).append("\n");
        }
        return sb.toString();
    }
}


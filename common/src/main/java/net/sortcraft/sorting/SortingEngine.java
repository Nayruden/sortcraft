package net.sortcraft.sorting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.sortcraft.audit.AuditConfig;
import net.sortcraft.audit.SortAuditLog;
import net.sortcraft.category.CategoryLoader;
import net.sortcraft.category.CategoryNode;
import net.sortcraft.config.ConfigManager;
import net.sortcraft.command.CommandHandler;
import net.sortcraft.compat.Id;
import net.sortcraft.container.ChestRef;
import net.sortcraft.container.ContainerHelper;
import net.sortcraft.container.SortContext;
import net.sortcraft.container.SortCraftStorage;
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
     * Sorts all items from a stack of input chests into categorized chests with optional audit logging.
     *
     * <p>This method processes multiple chests in order (typically bottom-to-top for input stacks),
     * sorting items from each chest into the appropriate category chests.
     *
     * @param context The sort context containing position, search radius, and cached signs/containers
     * @param world The server level where sorting takes place
     * @param inputChests The list of input chests to sort items FROM (in processing order)
     * @param preview If true, only calculate what would be sorted without actually moving items
     * @param audit Optional audit log to record item movements (can be null to disable logging)
     * @return Results containing counts of sorted items, overflow categories, and unknown items
     */
    public static SortingResults sortFromContainers(SortContext context, ServerLevel world, List<ChestRef> inputChests,
                                                    boolean preview, SortAuditLog audit) {
        SortingResults combinedResults = new SortingResults();

        for (ChestRef chestRef : inputChests) {
            SortCraftStorage storage = chestRef.getStorage();
            SortingResults chestResults;
            try {
                chestResults = sortStacks(context, world, storage.allStacks(), preview, audit);
            } finally {
                // Always clean up, even if sorting throws — prevents corrupted zero-count stacks
                if (!preview) {
                    storage.cleanup();
                }
            }

            // Merge results from this chest into combined results
            combinedResults.sorted += chestResults.sorted;
            combinedResults.overflowCategories.addAll(chestResults.overflowCategories);
            combinedResults.unknownItems.addAll(chestResults.unknownItems);
            combinedResults.leftovers.addAll(chestResults.leftovers);
            chestResults.categoryCounts.forEach((category, count) ->
                    combinedResults.categoryCounts.merge(category, count, Integer::sum));
        }

        return combinedResults;
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
                    Id containerItemId = Id.ofItem(stack.getItem());

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

            Id itemId = Id.ofItem(stack.getItem());
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
            Id uniformItemId,
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
        Id singleItem = null;
        int stackCount = 0;
        int totalItemCount = 0;

        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            Id itemId = Id.ofItem(stack.getItem());

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
                                        List<CategoryNode> cats, Id itemId, SortingResults results,
                                        SortAuditLog audit) {
        sortSingleStack(context, world, preview, stack, cats, itemId, results, audit, null);
    }

    private static void sortSingleStack(SortContext context, ServerLevel world, boolean preview, ItemStack stack,
                                        List<CategoryNode> cats, Id itemId, SortingResults results,
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

        BlockPos containerPos = ContainerHelper.getAttachedContainerPos(signPos, signState, world);
        if (containerPos == null) {
            LOGGER.trace("[findchests] Sign at {} is not attached to a container", signPos);
            return Collections.emptyList();
        }

        return ContainerHelper.collectContainerStack(world, containerPos);
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
        int remaining = originalCount;

        for (ChestRef ref : chests) {
            SortCraftStorage storage = ref.getStorage();

            // Create a copy with the remaining count to insert
            ItemStack toInsert = stack.copy();
            toInsert.setCount(remaining);

            int inserted = storage.insert(toInsert, preview);
            remaining -= inserted;

            if (remaining <= 0) break;
        }

        int moved = originalCount - remaining;
        if (!preview) stack.shrink(moved);
        return moved;
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


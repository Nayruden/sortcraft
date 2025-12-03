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
import net.sortcraft.category.CategoryLoader;
import net.sortcraft.category.CategoryNode;
import net.sortcraft.command.CommandHandler;
import net.sortcraft.container.ChestRef;
import net.sortcraft.container.ContainerHelper;
import net.sortcraft.container.SortContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Core sorting engine that distributes items to categorized chests.
 */
public final class SortingEngine {
    private SortingEngine() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("sortcraft");
    private static final int UNIFORM_CONTAINER_THRESHOLD = 10;

    /**
     * Sorts all items from the given iterable into categorized chests.
     * Handles containers (bundles, shulker boxes) recursively.
     */
    public static SortingResults sortStacks(SortContext context, ServerLevel world, Iterable<ItemStack> stacks, boolean preview) {
        SortingResults results = new SortingResults();

        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;

            LOGGER.debug("[sortinput] Sorting {} of {}", stack.getCount(), stack.getItem().toString());

            // Check if this is a container (bundle or shulker box)
            Iterable<ItemStack> innerStacks = ContainerHelper.getStacksIfContainer(stack);
            if (innerStacks != null) {
                ResourceLocation uniformItem = getSingleItemIfUniformAndMeetsThreshold(innerStacks, UNIFORM_CONTAINER_THRESHOLD);
                if (uniformItem != null) {
                    LOGGER.debug("[sortinput] Container has >={} stacks of same item '{}'. Sorting container itself.",
                            UNIFORM_CONTAINER_THRESHOLD, uniformItem);

                    List<CategoryNode> cats = CategoryLoader.getMatchingCategoriesNoFilter(uniformItem);
                    sortSingleStack(context, world, preview, stack, cats, uniformItem, results);
                    continue;
                }

                LOGGER.debug("[sortinput] Item is a container. Sorting contents of container.");
                SortingResults innerResults = sortStacks(context, world, innerStacks, preview);
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
            sortSingleStack(context, world, preview, stack, cats, itemId, results);
        }
        return results;
    }

    private static ResourceLocation getSingleItemIfUniformAndMeetsThreshold(Iterable<ItemStack> stacks, int threshold) {
        ResourceLocation singleItem = null;
        int count = 0;

        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());

            if (singleItem == null) {
                singleItem = itemId;
            } else if (!singleItem.equals(itemId)) {
                return null;
            }
            count++;
        }

        if (count >= threshold) {
            return singleItem;
        }
        return null;
    }

    private static void sortSingleStack(SortContext context, ServerLevel world, boolean preview, ItemStack stack,
                                        List<CategoryNode> cats, ResourceLocation itemId, SortingResults results) {
        if (cats.isEmpty()) {
            LOGGER.debug("[sortinput] No categories found for item: {}", itemId);
            results.unknownItems.add(itemId.toString());
            results.leftovers.add(stack);
            return;
        }

        int stackSize = stack.getCount();
        int totalMoved = 0;
        String categoriesStr = CategoryNode.categoriesToStr(cats);
        for (CategoryNode category : cats) {
            List<ChestRef> categoryChests = findCategoryChests(context, world, category.name);
            if (categoryChests.isEmpty()) continue;

            int moved = distributeToChests(stack, categoryChests, preview);
            totalMoved += moved;
            if (moved > 0) {
                results.sorted += moved;
                results.categoryCounts.merge(category.name, moved, Integer::sum);
                LOGGER.debug("[sortinput] Moved {} of item {}", moved, itemId);

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

        return ContainerHelper.collectChestStack(world, chestPos, SortingEngine::findTextOnSign);
    }

    /**
     * Distributes items from a stack to the given chests.
     * Returns the number of items moved.
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
     * Converts a Container to an Iterable of ItemStacks.
     */
    public static Iterable<ItemStack> containerToIterable(Container container) {
        return () -> new Iterator<>() {
            private int index = 0;
            private final int size = container.getContainerSize();

            @Override
            public boolean hasNext() {
                return index < size;
            }

            @Override
            public ItemStack next() {
                return container.getItem(index++);
            }
        };
    }

    /**
     * Finds text matching a pattern on a sign.
     */
    public static String findTextOnSign(SignBlockEntity sign, String patternStr) {
        Pattern pattern = Pattern.compile(patternStr);
        for (int i = 0; i < 4; i++) {
            String frontLine = sign.getFrontText().getMessage(i, false).getString().trim();
            String backLine = sign.getBackText().getMessage(i, false).getString().trim();

            Matcher frontMatcher = pattern.matcher(frontLine);
            if (frontMatcher.find()) return frontMatcher.group();

            Matcher backMatcher = pattern.matcher(backLine);
            if (backMatcher.find()) return backMatcher.group();
        }
        return null;
    }

    /**
     * Summarizes a set of items/categories into a message string.
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


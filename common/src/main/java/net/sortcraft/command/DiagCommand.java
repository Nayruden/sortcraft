package net.sortcraft.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.sortcraft.category.CategoryLoader;
import net.sortcraft.category.CategoryNode;
import net.sortcraft.compat.RegistryHelper;
import net.sortcraft.config.ConfigManager;
import net.sortcraft.container.ChestRef;
import net.sortcraft.container.SortContext;
import net.sortcraft.sorting.SortingEngine;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Handles /sort diagnostics command.
 */
public final class DiagCommand {
    private DiagCommand() {}

    private static final int CHEST_SLOT_COUNT = 27;

    public static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be run by a player."));
            return 0;
        }
        ServerLevel world = source.getLevel();
        BlockPos playerPos = player.blockPosition();
        Map<String, Map<String, Object>> categoryData = new TreeMap<>();

        SortContext sortContext = new SortContext(world, playerPos, ConfigManager.getSearchRadius());

        for (Map.Entry<String, CategoryNode> entry : CategoryLoader.getCategories().entrySet()) {
            String categoryName = entry.getKey();
            CategoryNode categoryNode = entry.getValue();
            Set<ResourceLocation> items = categoryNode.flattenedItemIds;

            if (items == null || items.isEmpty()) continue;

            List<ChestRef> chests = SortingEngine.findCategoryChests(sortContext, world, categoryName);
            if (chests.isEmpty()) continue;

            Map<String, Object> itemData = new TreeMap<>();
            int totalSlots = chests.size() * CHEST_SLOT_COUNT;
            int usedSlots = 0;

            for (ResourceLocation id : items) {
                Item item = RegistryHelper.getItemByKey(id);
                if (item == null) continue;

                int total = 0;
                Map<String, Integer> locationCounts = new LinkedHashMap<>();

                for (ChestRef ref : chests) {
                    Container inv = ref.getInventory();
                    int count = 0;
                    for (int slot = 0; slot < inv.getContainerSize(); slot++) {
                        ItemStack stack = inv.getItem(slot);
                        if (stack.getItem() == item) {
                            count += stack.getCount();
                            if (!stack.isEmpty()) usedSlots++;
                        }
                    }
                    if (count > 0) {
                        String loc = ref.getPos().getX() + " " + ref.getPos().getY() + " " + ref.getPos().getZ();
                        locationCounts.merge(loc, count, Integer::sum);
                        total += count;
                    }
                }

                if (total > 0) {
                    Map<String, Object> itemEntry = new LinkedHashMap<>();
                    itemEntry.put("total_quantity", total);
                    if (!locationCounts.isEmpty()) {
                        List<Map<String, Object>> chestLocations = new ArrayList<>();
                        for (Map.Entry<String, Integer> e : locationCounts.entrySet()) {
                            Map<String, Object> loc = new LinkedHashMap<>();
                            loc.put("location", e.getKey());
                            loc.put("quantity", e.getValue());
                            chestLocations.add(loc);
                        }
                        itemEntry.put("chest_locations", chestLocations);
                    }
                    itemData.put(id.toString(), itemEntry);
                }
            }

            if (!itemData.isEmpty()) {
                float spaceUsed = (totalSlots > 0) ? ((float) usedSlots / totalSlots) * 100f : 0f;
                itemData.put("space_used", String.format("%.0f%%", spaceUsed));
                categoryData.put(categoryName, itemData);
            }
        }

        Map<String, Object> finalYaml = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> e : categoryData.entrySet()) {
            finalYaml.put(e.getKey(), e.getValue());
        }

        try {
            File file = ConfigManager.getConfigPath("sortdiag.yaml").toFile();
            file.getParentFile().mkdirs();

            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);

            Yaml yaml = new Yaml(options);
            try (FileWriter writer = new FileWriter(file)) {
                yaml.dump(finalYaml, writer);
            }
        } catch (IOException e) {
            source.sendFailure(Component.literal("Failed to write sortcraft/sortdiag.yaml: " + e.getMessage()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Sorter diagnostic written to sortcraft/sortdiag.yaml"), false);
        return 1;
    }
}


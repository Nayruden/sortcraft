package net.sortcraft.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.sortcraft.compat.RegistryHelper;
import net.sortcraft.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.nio.file.Path;
import java.util.*;

/**
 * Handles /sort dump command.
 */
public final class DumpCommand {
    private DumpCommand() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("sortcraft");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static int execute(CommandContext<CommandSourceStack> context) {
        try {
            Map<String, List<String>> itemsToTags = new HashMap<>();
            Map<String, List<String>> tagsToItems = new HashMap<>();

            for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
                Item item = RegistryHelper.getItemByKey(id);
                Holder<Item> entry = BuiltInRegistries.ITEM.wrapAsHolder(item);

                Collection<TagKey<Item>> tags = entry.tags().toList();
                List<String> tagList = new ArrayList<>();

                for (TagKey<Item> tag : tags) {
                    String tagStr = tag.location().toString();
                    tagList.add(tagStr);
                    tagsToItems.computeIfAbsent(tagStr, k -> new ArrayList<>()).add(id.toString());
                }

                itemsToTags.put(id.toString(), tagList);
            }

            Path itemsToTagsPath = ConfigManager.getConfigPath("items_to_tags.json");
            try (FileWriter writer = new FileWriter(itemsToTagsPath.toFile())) {
                writer.write(GSON.toJson(itemsToTags));
            }

            Path tagsToItemsPath = ConfigManager.getConfigPath("tags_to_items.json");
            try (FileWriter writer = new FileWriter(tagsToItemsPath.toFile())) {
                writer.write(GSON.toJson(tagsToItems));
            }

            context.getSource().sendSuccess(() -> Component.literal("Dumped items_to_tags.json and tags_to_items.json"), false);
        } catch (Exception e) {
            LOGGER.error("Error dumping item tags", e);
            context.getSource().sendFailure(Component.literal("Error dumping item tags: " + e.getMessage()));
        }
        return 1;
    }
}


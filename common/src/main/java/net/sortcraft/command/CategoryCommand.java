package net.sortcraft.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.sortcraft.category.CategoryLoader;
import net.sortcraft.category.CategoryNode;

import java.util.Set;

/**
 * Handles /sort category command.
 */
public final class CategoryCommand {
    private CategoryCommand() {}

    public static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String itemName = StringArgumentType.getString(context, "item");

        ResourceLocation id = ResourceLocation.tryParse(itemName);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            source.sendFailure(Component.literal("Unknown item: " + itemName));
            return 0;
        }
        Set<CategoryNode> cats = CategoryLoader.getItemCategoryMap().get(id);
        if (!(cats == null || cats.isEmpty())) {
            String categoriesStr = CategoryNode.categoriesToStr(cats);
            source.sendSuccess(() -> Component.literal("Item " + id + " belongs to categories '" + categoriesStr + "'."), false);
        } else {
            source.sendSuccess(() -> Component.literal("Item " + id + " is not assigned to any category."), false);
        }
        return 1;
    }
}


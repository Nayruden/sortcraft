package net.sortcraft.audit;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to extract metadata from an ItemStack for audit logging.
 */
public final class ItemMetadataExtractor {
    
    private ItemMetadataExtractor() {}

    /**
     * Extracts metadata from an ItemStack.
     * Returns null if the stack has no metadata worth logging.
     *
     * @param stack The ItemStack to extract metadata from
     * @return ItemMetadata or null if no metadata present
     */
    public static ItemMetadata extract(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        List<EnchantmentInfo> enchantments = extractEnchantments(stack);
        String customName = extractCustomName(stack);
        String potionType = extractPotionType(stack);

        // Return null if there's no metadata to log
        if ((enchantments == null || enchantments.isEmpty()) 
                && customName == null 
                && potionType == null) {
            return null;
        }

        return new ItemMetadata(enchantments, customName, potionType);
    }

    /**
     * Extracts enchantments from an ItemStack.
     * Handles both regular enchantments and stored enchantments (enchanted books).
     */
    private static List<EnchantmentInfo> extractEnchantments(ItemStack stack) {
        List<EnchantmentInfo> result = new ArrayList<>();

        // Regular enchantments (on weapons, armor, tools)
        ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
        if (enchantments != null) {
            addEnchantmentsToList(enchantments, result);
        }

        // Stored enchantments (on enchanted books)
        ItemEnchantments storedEnchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (storedEnchantments != null) {
            addEnchantmentsToList(storedEnchantments, result);
        }

        return result.isEmpty() ? null : result;
    }

    /**
     * Adds enchantments from an ItemEnchantments component to the result list.
     */
    private static void addEnchantmentsToList(ItemEnchantments component, List<EnchantmentInfo> result) {
        for (Holder<Enchantment> holder : component.keySet()) {
            int level = component.getLevel(holder);
            String enchantmentId = holder.unwrapKey()
                    .map(ResourceKey::location)
                    .map(Object::toString)
                    .orElse("unknown");
            result.add(new EnchantmentInfo(enchantmentId, level));
        }
    }

    /**
     * Extracts the custom name from an ItemStack if present.
     */
    private static String extractCustomName(ItemStack stack) {
        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        if (customName == null) {
            return null;
        }
        String name = customName.getString();
        // Truncate very long names
        if (name.length() > 256) {
            return name.substring(0, 253) + "...";
        }
        return name;
    }

    /**
     * Extracts the potion type from an ItemStack if it's a potion.
     */
    private static String extractPotionType(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return null;
        }
        
        return contents.potion()
                .flatMap(Holder::unwrapKey)
                .map(key -> key.location().toString())
                .orElse(null);
    }
}


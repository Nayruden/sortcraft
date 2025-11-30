package net.sortcraft;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.sortcraft.compat.RegistryHelper;

import java.util.*;


interface FilterRule {
  boolean matches(ItemStack stack);
}

class FilterRuleFactory {
  public static FilterRule fromYaml(MinecraftServer server, String key, String value) {
    if (key.startsWith("!")) {
      key = key.substring(1);
      return new NegatedFilterRule(fromYaml(server, key, value));
    }

    var registries = server.registryAccess();

    return switch (key.toLowerCase()) {
      case "enchantment" ->
        new EnchantmentFilterRule(value, registries);

      case "custom_name" ->
        new NameFilterRule(value);

      case "stackable" ->
        new StackableFilterRule();

      default ->
        throw new IllegalArgumentException("Unknown filter key: " + key);
    };
  }
}

class NegatedFilterRule implements FilterRule {
  private final FilterRule inner;

  public NegatedFilterRule(FilterRule inner) {
      this.inner = inner;
  }

  @Override
  public boolean matches(ItemStack stack) {
      if (stack == null || stack.isEmpty()) return false;
      return !inner.matches(stack);
  }
}

class NameFilterRule implements FilterRule {
  enum MatchType {
    SINGLE,
    ANY,
  }
  private final NameFilterRule.MatchType matchType;
  private final String expectedName;

  public NameFilterRule(String name) {
    name = name.toLowerCase();
    if (name.equals("*")) {
      matchType = MatchType.ANY;
      this.expectedName = null;
    } else {
      matchType = MatchType.SINGLE;
      this.expectedName = name;
    }
  }

  @Override
  public boolean matches(ItemStack stack) {
    if (stack == null || stack.isEmpty()) return false;
    Component name = stack.get(DataComponents.CUSTOM_NAME);
    if (name == null) return false;
    else if (matchType == MatchType.ANY) return true;

    String displayName = stack.getHoverName().getString();
    return displayName.equalsIgnoreCase(expectedName);
  }
}


class EnchantmentFilterRule implements FilterRule {
  enum MatchType {
    SINGLE,
    ANY,
    MAX,
  }
  private final MatchType matchType;
  private final Enchantment singleEnchantment; // Only used if matching a specific enchantment

  public EnchantmentFilterRule(String configValue, net.minecraft.core.RegistryAccess registries) {
    configValue = configValue.toLowerCase();
    switch(configValue) {
      case "*":
        matchType = MatchType.ANY;
        break;
      case "max":
        matchType = MatchType.MAX;
        break;
      default:
        matchType = MatchType.SINGLE;
        break;
    }

    if (matchType == MatchType.SINGLE) {
      ResourceLocation id = ResourceLocation.tryParse(configValue);
      if (id == null) {
        throw new IllegalArgumentException("Invalid enchantment id: " + configValue);
      }
      singleEnchantment = RegistryHelper.getEnchantmentOrThrow(registries, id);
    } else {
      singleEnchantment = null;
    }
  }

  @Override
  public boolean matches(ItemStack stack) {
    if (stack == null || stack.isEmpty()) return false;
    // Get both ENCHANTMENTS and STORED_ENCHANTMENTS
    ItemEnchantments enchantmentsComponent = stack.get(DataComponents.ENCHANTMENTS);
    ItemEnchantments storedEnchantmentsComponent = stack.get(DataComponents.STORED_ENCHANTMENTS);

    List<ItemEnchantments> components = new ArrayList<>();
    if (enchantmentsComponent != null) components.add(enchantmentsComponent);
    if (storedEnchantmentsComponent != null) components.add(storedEnchantmentsComponent);
    if (components.isEmpty()) return false;

    return switch (matchType) {
      case ANY -> true;
      case MAX -> {
        for (ItemEnchantments component : components) {
          if (component != null) {
            for (Holder<Enchantment> entry : component.keySet()) {
              int level = component.getLevel(entry);
              if (level == entry.value().getMaxLevel()) yield true;
            }
          }
        }
        yield false;
      }
      case SINGLE -> {
        for (ItemEnchantments component : components) {
          for (Holder<Enchantment> entry : component.keySet()) {
            if (entry.value() == singleEnchantment) {
              yield true;
            }
          }
        }
        yield false;
      }
    };
  }
}


class StackableFilterRule implements FilterRule {
  @Override
  public boolean matches(ItemStack stack) {
    return stack != null && !stack.isEmpty() && stack.getItem().getDefaultMaxStackSize() != 1;
  }
}


package net.sortcraft;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryLoader;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Set;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;


interface FilterRule {
  boolean matches(ItemStack stack);
}

class FilterRuleFactory {
  public static FilterRule fromYaml(MinecraftServer server, String key, String value) {
    if (key.startsWith("!")) {
      key = key.substring(1);
      return new NegatedFilterRule(fromYaml(server, key, value));
    }

    var registries = server.getRegistryManager();

    switch (key.toLowerCase()) {
      case "enchantment" -> {
        return new EnchantmentFilterRule(value, registries.getOrThrow(RegistryKeys.ENCHANTMENT));
      }
      case "custom_name" -> {
        return new NameFilterRule(value);
      }
      // types of potions
      default -> throw new IllegalArgumentException("Unknown filter key: " + key);
    }
  }
}

class NegatedFilterRule implements FilterRule {
  private final FilterRule inner;

  public NegatedFilterRule(FilterRule inner) {
      this.inner = inner;
  }

  @Override
  public boolean matches(ItemStack stack) {
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
    Text name = stack.get(DataComponentTypes.CUSTOM_NAME);
    if (name == null) return false;
    else if (matchType == MatchType.ANY) return true;

    String displayName = stack.getName().getString();
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

  public EnchantmentFilterRule(String configValue, Registry<Enchantment> enchantmentRegistry) {
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
      Identifier id = Identifier.tryParse(configValue);
      if (id == null || !enchantmentRegistry.containsId(id)) {
        throw new IllegalArgumentException("Unknown enchantment: " + configValue);
      }
      singleEnchantment = enchantmentRegistry.get(id);
    } else {
      singleEnchantment = null;
    }
  }

  @Override
  public boolean matches(ItemStack stack) {
    ItemEnchantmentsComponent enchantmentsComponent = stack.get(DataComponentTypes.ENCHANTMENTS);
    if (enchantmentsComponent == null || enchantmentsComponent.getEnchantments().isEmpty()) {
      return false;
    }

    Set<RegistryEntry<Enchantment>> enchantments = enchantmentsComponent.getEnchantments();

    return switch (matchType) {
      case ANY -> true;
      case MAX -> {
        for (RegistryEntry<Enchantment> entry : enchantments) {
          int level = enchantmentsComponent.getLevel(entry);
          if (level == entry.value().getMaxLevel()) yield true;
        }
        yield false;
      }
      case SINGLE -> {
        for (RegistryEntry<Enchantment> entry : enchantments) {
          if (entry.value() == singleEnchantment) yield true;
        }
        yield false;
      }
    };
  }
}


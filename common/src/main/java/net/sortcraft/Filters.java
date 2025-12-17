package net.sortcraft;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.sortcraft.compat.RegistryHelper;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Contains filter rule implementations.
 * The public interface and factory are in separate files.
 */
final class Filters {
    private Filters() {}

    /**
     * Creates a filter rule from YAML configuration.
     * Called by FilterRuleFactory.
     *
     * @param registries the registry access (required for enchantment filter)
     * @param key the filter key
     * @param value the filter value
     * @return the created FilterRule
     */
    static FilterRule createFilterRule(RegistryAccess registries, String key, String value) {
        if (key.startsWith("!")) {
            key = key.substring(1);
            return new NegatedFilterRule(createFilterRule(registries, key, value));
        }

        return switch (key.toLowerCase()) {
            case "enchantment" -> {
                if (value == null || value.isEmpty()) {
                    throw new IllegalArgumentException("Enchantment filter requires a value");
                }
                if (registries == null) {
                    throw new IllegalArgumentException("Enchantment filter requires registry access");
                }
                yield new EnchantmentFilterRule(value, registries);
            }

            case "custom_name" -> {
                if (value == null || value.isEmpty()) {
                    throw new IllegalArgumentException("Custom name filter requires a value");
                }
                yield new NameFilterRule(value);
            }

            case "stackable" ->
                new StackableFilterRule();

            case "durability" -> {
                if (value == null || value.isEmpty()) {
                    throw new IllegalArgumentException("Durability filter requires a value (e.g., '<50%', '>=75%', '=100%', or '*')");
                }
                yield new DurabilityFilterRule(value);
            }

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
            if (entry.value().equals(singleEnchantment)) {
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

/**
 * Filter rule that matches items based on their durability percentage.
 * Supports comparison operators (<, <=, >, >=, =) with percentage values,
 * or '*' to match any damageable item.
 *
 * <p>Examples:
 * <ul>
 *   <li>{@code durability: "<50%"} - items with less than 50% durability</li>
 *   <li>{@code durability: ">=75%"} - items with 75% or more durability</li>
 *   <li>{@code durability: "=100%"} - pristine items (no damage)</li>
 *   <li>{@code durability: "*"} - any damageable item (tools, armor, weapons)</li>
 * </ul>
 */
class DurabilityFilterRule implements FilterRule {
    // Matches expressions like "<50%", ">=75%", "=100%"
    private static final Pattern DURABILITY_PATTERN =
            Pattern.compile("^(<=?|>=?|=)(\\d{1,3})%$");

    private final MatchType matchType;
    private final ComparisonOperator operator;
    private final int threshold;

    enum MatchType {
        ANY_DAMAGEABLE,  // "*" - matches any item that has durability
        COMPARISON       // Comparison expression like "<50%"
    }

    enum ComparisonOperator {
        LT("<"),
        LE("<="),
        GT(">"),
        GE(">="),
        EQ("=");

        private final String symbol;

        ComparisonOperator(String symbol) {
            this.symbol = symbol;
        }

        static ComparisonOperator fromSymbol(String symbol) {
            for (ComparisonOperator op : values()) {
                if (op.symbol.equals(symbol)) {
                    return op;
                }
            }
            throw new IllegalArgumentException("Unknown operator: " + symbol);
        }

        boolean test(int actual, int threshold) {
            return switch (this) {
                case LT -> actual < threshold;
                case LE -> actual <= threshold;
                case GT -> actual > threshold;
                case GE -> actual >= threshold;
                case EQ -> actual == threshold;
            };
        }
    }

    public DurabilityFilterRule(String expression) {
        expression = expression.trim();

        if (expression.equals("*")) {
            this.matchType = MatchType.ANY_DAMAGEABLE;
            this.operator = null;
            this.threshold = 0;
            return;
        }

        Matcher matcher = DURABILITY_PATTERN.matcher(expression);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Invalid durability expression: '" + expression + "'. " +
                    "Expected format: <operator><number>% (e.g., '<50%', '>=75%', '=100%') or '*' for any damageable item");
        }

        this.matchType = MatchType.COMPARISON;
        this.operator = ComparisonOperator.fromSymbol(matcher.group(1));
        this.threshold = Integer.parseInt(matcher.group(2));

        if (threshold < 0 || threshold > 100) {
            throw new IllegalArgumentException(
                    "Durability threshold must be 0-100%, got: " + threshold);
        }
    }

    @Override
    public boolean matches(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        int maxDamage = stack.getMaxDamage();

        // Non-damageable items (sticks, blocks, etc.) have maxDamage = 0
        if (maxDamage == 0) {
            // For ANY_DAMAGEABLE, non-damageable items don't match
            if (matchType == MatchType.ANY_DAMAGEABLE) {
                return false;
            }
            // For comparison, treat non-damageable items as 100% durability
            return operator.test(100, threshold);
        }

        // Item is damageable
        if (matchType == MatchType.ANY_DAMAGEABLE) {
            return true;
        }

        // Calculate durability percentage: (remaining / max) * 100
        // Use Math.round for accurate percentage to avoid off-by-one errors at boundaries
        int currentDamage = stack.getDamageValue();
        int remainingDurability = maxDamage - currentDamage;
        int durabilityPercent = Math.round((remainingDurability * 100.0f) / maxDamage);

        return operator.test(durabilityPercent, threshold);
    }
}


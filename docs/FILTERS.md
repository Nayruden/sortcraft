# Filters

Filters allow you to sort items based on their **properties** (enchantments, custom names, durability), not just their item type.

---

## Basic Syntax

```yaml
greater_swords:
  filters:
  - enchantment: max
  includes:
  - swords
```

Filters are applied at the category level. Items must match ALL filters (AND logic).

---

## Filter Negation

Prefix any filter key with `!` to negate it. You must quote the key:

```yaml
filters:
- "!enchantment": max      # NOT max-level enchanted
- "!custom_name": '*'      # NOT renamed
- "!stackable":            # NOT stackable
```

---

## Available Filters

### `enchantment`

Matches items with enchantments (works on both gear and enchanted books).

| Value                 | Description                                          |
|-----------------------|------------------------------------------------------|
| `max`                 | Item has at least one enchantment at its max level   |
| `*`                   | Item has any enchantment                             |
| `minecraft:sharpness` | Item has the specific enchantment (any level)        |

```yaml
# Max-level enchanted books
greater_books:
  priority: 5
  filters:
  - enchantment: max
  items:
  - minecraft:enchanted_book

# Books without max enchantments
lesser_books:
  priority: 4
  filters:
  - "!enchantment": max
  items:
  - minecraft:enchanted_book
```

---

### `custom_name`

Matches items that have been renamed (e.g., in an anvil).

| Value      | Description                                            |
|------------|--------------------------------------------------------|
| `*`        | Item has any custom name                               |
| `My Sword` | Item's display name matches exactly (case-insensitive) |

```yaml
# All renamed gear goes to a special chest
named_items:
  priority: 1
  filters:
  - custom_name: '*'
  includes:
  - gear
```

---

### `stackable`

Matches items that can stack beyond 1. **This filter takes no value.**

```yaml
# Stackable items only
bulk_items:
  filters:
  - stackable:
  items:
  - /.*:.*_ingot/
  - /.*:.*_nugget/

# Non-stackable items (tools, weapons, armor)
equipment:
  filters:
  - "!stackable":
  includes:
  - gear
```

---

### `durability`

Matches items based on their durability percentage.

| Value      | Description                                              |
|------------|----------------------------------------------------------|
| `<50%`     | Less than 50% durability remaining                       |
| `<=25%`    | 25% or less durability remaining                         |
| `>75%`     | More than 75% durability remaining                       |
| `>=50%`    | 50% or more durability remaining                         |
| `=100%`    | Pristine items (no damage taken)                         |
| `*`        | Any damageable item (tools, weapons, armor)              |

**Notes:**
- Non-damageable items are treated as 100% durability for comparisons
- Non-damageable items do NOT match `durability: "*"`
- Durability = `(maxDurability - currentDamage) / maxDurability * 100`

```yaml
# Damaged tools that need repair
damaged_tools:
  priority: 3
  filters:
  - durability: "<50%"
  includes:
  - tools

# Pristine armor only
pristine_armor:
  priority: 2
  filters:
  - durability: "=100%"
  includes:
  - armor

# Mid-range durability (25-75%)
worn_gear:
  filters:
  - durability: ">=25%"
  - durability: "<=75%"
  includes:
  - gear
```

---

## Multiple Filters (AND Logic)

When multiple filters are specified, items must match **ALL** of them:

```yaml
# Must be BOTH max-enchanted AND renamed
prized_weapons:
  priority: 1
  filters:
  - enchantment: max
  - custom_name: '*'
  includes:
  - weapons
```

---

## Filter + Priority Example

Use filters with priority to create tiered sorting:

```yaml
# Priority 1: Named items (most specific)
named_gear:
  priority: 1
  filters:
  - custom_name: '*'
  includes:
  - gear

# Priority 5: Max enchanted items
greater_gear:
  priority: 5
  filters:
  - enchantment: max
  includes:
  - gear

# Priority 10: All other gear
gear:
  priority: 10
  includes:
  - weapons
  - tools
  - armor
```

An enchanted diamond sword named "Excalibur" matches `named_gear` (priority 1) and stops there.


# Configuration

Sortcraft uses YAML config files to define categories, priorities, and filters.

---

## File Structure

```
config/sortcraft/
├── config.yaml          # Main configuration
└── categories/          # Category definition files
    ├── 01_materials.yaml
    ├── 02_tools.yaml
    └── ...              # All .yaml files loaded automatically
```

---

## Main Configuration (config.yaml)

```yaml
# Sortcraft Configuration

# Log level: TRACE, DEBUG, INFO, WARN, ERROR
logLevel: WARN

# Search radius for finding signs (in blocks)
searchRadius: 64
```

| Option         | Default | Description                                                  |
|----------------|---------|--------------------------------------------------------------|
| `logLevel`     | `WARN`  | Controls logging verbosity. Use `DEBUG` for troubleshooting. |
| `searchRadius` | `64`    | Maximum distance (in blocks) to search for category signs.   |

---

## Category Definition

Each YAML file can contain multiple category definitions:

```yaml
swords:
  items:
  - minecraft:diamond_sword
  - minecraft:iron_sword
  - /.*:.*_sword/              # Regex pattern

pickaxes:
  items:
  - /.*:.*_pickaxe/

weapons:
  priority: 12
  includes:
  - swords
  - pickaxes
```

### Category Properties

| Property   | Description                                           |
|------------|-------------------------------------------------------|
| `items`    | List of item IDs, tags (`#tag`), or regex (`/pattern/`) |
| `priority` | Sort order (lower = checked first, default: 10)       |
| `includes` | Include items from other categories                   |
| `filters`  | Match by item properties (see [Filters](FILTERS.md))  |

---

## Priority System

Priority determines the order categories are checked when sorting:

| Priority | Use Case                                              |
|----------|-------------------------------------------------------|
| 1-4      | Special items (named items, specific enchantments)    |
| 5        | "Greater" enchanted items (max-level enchantments)    |
| 10       | Default priority (most categories)                    |
| 12       | Grouping categories (gear, tools, weapons)            |
| 20       | Rollup/catch-all categories                           |

**Lower priority = checked first.** If an item matches a priority 5 category, it won't be checked against priority 10 categories.

> **Note:** These priority values are suggestions. You can use any integer values. The only hard-coded value is the **default priority of 10** when no priority is specified.

---

## Category Includes

Use `includes` to build category hierarchies:

```yaml
# Base categories
swords:
  items:
  - /.*:.*_sword/

pickaxes:
  items:
  - /.*:.*_pickaxe/

# Grouping category
weapons:
  priority: 12
  includes:
  - swords
  - pickaxes
```

An item matching `swords` (priority 10) will go to a `[swords]` chest. If no `[swords]` chest exists, it falls through to `weapons` (priority 12).

---

## Item Tags

Use Minecraft's item tags to match groups of related items. Tags are prefixed with `#`.

```yaml
items:
- "#minecraft:logs"        # All log blocks
- "#minecraft:planks"      # All plank blocks
```

### Common Tags

| Tag                       | Description                          |
|---------------------------|--------------------------------------|
| `#minecraft:logs`         | All log blocks (oak, birch, etc.)    |
| `#minecraft:planks`       | All plank blocks                     |
| `#minecraft:wool`         | All wool colors                      |
| `#minecraft:saplings`     | All sapling types                    |
| `#minecraft:flowers`      | All flower items                     |
| `#minecraft:fishes`       | All fish items                       |
| `#minecraft:coals`        | Coal and charcoal                    |

### Discovering Tags

Use `/sort dump` to generate JSON files listing all item tags in the game.

---

## Combining Match Types

Tags, explicit items, and regex patterns can be mixed:

```yaml
wood_items:
  items:
  - "#minecraft:logs"        # Tag
  - "#minecraft:planks"      # Tag
  - minecraft:stick          # Explicit
  - /.*:.*_wood$/            # Regex
```

---

## See Also

- [Regex Patterns](REGEX.md) - Pattern matching syntax
- [Filters](FILTERS.md) - Match by enchantments, durability, etc.
- [Core Concepts](CONCEPTS.md) - Priority system explained


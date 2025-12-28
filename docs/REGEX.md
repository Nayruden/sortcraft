# Regex Patterns

Regex patterns allow you to match multiple items with a single rule. Patterns must be wrapped in forward slashes (`/pattern/`).

---

## Syntax

```yaml
items:
- /regex_pattern_here/
```

---

## How Matching Works

- Patterns match against the **full item ID** including namespace (e.g., `minecraft:diamond_sword`)
- Uses Java regex with `find()` - matches if pattern is found **anywhere** in the item ID
- Use `^` and `$` anchors for exact matching if needed

---

## Common Patterns

| Pattern                  | Description                      | Matches                                                   |
|--------------------------|----------------------------------|-----------------------------------------------------------|
| `/.*:.*_sword/`          | All swords from any mod          | `minecraft:diamond_sword`, `twilightforest:ironwood_sword`|
| `/minecraft:.*_planks/`  | All vanilla planks               | `minecraft:oak_planks`, `minecraft:spruce_planks`         |
| `/.*_brick.*/`           | Anything with "brick" in name    | `minecraft:bricks`, `minecraft:brick_slab`                |
| `/^minecraft:diamond_/`  | Vanilla diamond items            | `minecraft:diamond_sword`, `minecraft:diamond_pickaxe`    |
| `/.*_ore$/`              | Items ending with "_ore"         | `minecraft:iron_ore`, `minecraft:gold_ore`                |

---

## Examples

### Match all wood variants from a mod

```yaml
wood_items:
  items:
  - /regions_unexplored:(stripped_)?.*_(log|wood|planks|slab|stairs)/
```

### Match all colored blocks

```yaml
colored_glass:
  items:
  - /minecraft:.*_stained_glass$/
  - /minecraft:.*_stained_glass_pane$/
```

### Combine regex with explicit items

```yaml
ranged:
  items:
  - /.*:.*bow$/                    # All bows
  - /.*:.*arrow/                   # All arrows
  - minecraft:trident              # Explicit item
```

---

## Pattern Tips

1. **Start broad, refine later** - Use `/.*:.*_sword/` first, then add exclusions if needed
2. **Test with `/sort category`** - Check if items match your patterns
3. **Anchors matter** - `/sword/` matches `diamond_sword` AND `swordfish`, use `/.*_sword$/` to be specific
4. **Escape special characters** - Use `\.` for literal dots, `\+` for literal plus signs

---

## Regex vs Tags vs Explicit Items

| Method | Best For | Example |
|--------|----------|---------|
| **Explicit** | Specific single items | `minecraft:diamond_sword` |
| **Tags** | Vanilla item groups | `#minecraft:logs` |
| **Regex** | Mod support, bulk matching | `/.*:.*_sword/` |

See [Configuration](CONFIGURATION.md) for more on combining these methods.


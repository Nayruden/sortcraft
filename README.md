# SortCraft

**SortCraft** is a powerful, configurable bulk sorting mod for Minecraft, supporting both Fabric and NeoForge loaders. It organizes your chests with intuitive, customizable categories to keep your world neat and efficient.

---

## 📦 **How It Works**

SortCraft uses **signs to label chests** and sorts items into them based on categories you define in YAML config files.

### 📝 **Chest Labeling**

* **Input Chest:** Place a sign on a chest with `[input]` on any line. This chest acts as the **source of items** to sort.
* **Category Chests:** Place a sign on a chest with the **category name** (matching your YAML) in **square brackets**. For example:

```
[input]
```

```
[gear]
```

```
[armor_helmets]
```

### 🖱️ **Sorting Items**

**Right-click the `[input]` sign** to instantly sort all items from the input chest into their designated category chests. This is the primary way to use SortCraft!

Alternatively, you can use the `/sort input` command if you're near an input chest.

### 📚 **Stacking and Fill Logic**

When you have **multiple chests labeled with the same category**, SortCraft fills them in **stack fashion**:

✅ **Bottom-up fill order.**
If you stack chests vertically with the same category sign, it will fill the **lowest chest first**, then the one above, and so on.

---

## ⚙️ **Commands**

### `/sort help`

Shows help and usage information for all SortCraft commands.

---

### `/sort input`

Triggers the sorting logic for the **nearest `[input]` chest**, scanning its contents and distributing items into destination chests based on your YAML category definitions.

💡 **Tip:** Right-clicking the `[input]` sign is usually faster and more convenient!

---

### `/sort preview`

Previews what items will be sorted where, **without actually moving them**.

✔ **Example usage:**

```
/sort preview
```

Outputs a categorized list showing:

* Which items will go to which category
* Item counts per category

---

### `/sort diagnostics`

Generates a **`sortdiag.yaml` file** in your config folder with:

* Current item counts
* Storage locations
* Slot usage by category

✔ **Use this to audit storage utilization** and plan expansions.

---

### `/sort whereis <item_id>`

Locates where a specific item is stored. Displays the chest locations in chat and shows **particle indicators** in the world for easy navigation.

✔ **Example usage:**

```
/sort whereis minecraft:diamond_sword
```

Finds where your diamond sword is stored.

---

### `/sort category <item_id>`

Outputs the **categories assigned to an item** based on your YAML configuration.

✔ **Example usage:**

```
/sort category minecraft:diamond_sword
```

Returns: `swords, gear, weapons`

---

### `/sort reload`

Reloads all category configurations from the `config/sortcraft/categories/` directory without restarting the server.

✔ **Example usage:**

```
/sort reload
```

Useful when editing category YAML files while the server is running.

---

### `/sort dump`

Generates JSON files containing all item tags from the registry. Useful for debugging and discovering available item tags for your category definitions.

✔ **Example usage:**

```
/sort dump
```

Output files are placed in the `sortcraft-dump/` directory.

---

## 🛠️ **YAML Configuration**

SortCraft uses **YAML config files** to define categories, priorities, and filters. Place your category files in `config/sortcraft/categories/` - all `.yaml` files in this directory are loaded automatically.

### 📁 **Category File Structure**

Each YAML file can contain multiple category definitions:

```yaml
# Priority determines sort order (lower = checked first, default: 10)
swords:
  items:
  - minecraft:diamond_sword
  - minecraft:iron_sword
  - /.*:.*_sword/              # Regex pattern to match all swords

pickaxes:
  items:
  - /.*:.*_pickaxe/            # Match all pickaxes from any mod

weapons:
  priority: 12                  # Higher priority = checked later
  includes:                     # Include items from other categories
  - swords
  - pickaxes

# Filter items by properties (e.g., enchantments)
greater_weapons:
  priority: 5                   # Lower priority = checked first
  filters:
  - enchantment: max            # Only max-level enchanted items
  includes:
  - weapons
```

### 🔢 **Priority System**

Priority determines the order categories are checked when sorting:

| Priority | Use Case                                              |
|----------|-------------------------------------------------------|
| 1-4      | Special items (named items, specific enchantments)    |
| 5        | "Greater" enchanted items (max-level enchantments)    |
| 10       | Default priority (most categories)                    |
| 12       | Grouping categories (gear, tools, weapons)            |
| 20       | Rollup/catch-all categories                           |

**Lower priority = checked first.** If an item matches a priority 5 category, it won't be checked against priority 10 categories.

**Note:** These priority values are suggestions based on the bundled categories. You can use any integer values you want. The only hard-coded value is the **default priority of 10** when no priority is specified.

---

## 🔍 **Regex Patterns**

Regex patterns allow you to match multiple items with a single rule. Patterns must be wrapped in forward slashes (`/pattern/`).

### **Syntax**

```yaml
items:
- /regex_pattern_here/
```

### **How Matching Works**

* Patterns match against the **full item ID** including namespace (e.g., `minecraft:diamond_sword`)
* Uses Java regex with `find()` - matches if pattern is found **anywhere** in the item ID
* Use `^` and `$` anchors for exact matching if needed

### **Common Patterns**

| Pattern                  | Description                      | Matches                                                   |
|--------------------------|----------------------------------|-----------------------------------------------------------|
| `/.*:.*_sword/`          | All swords from any mod          | `minecraft:diamond_sword`, `twilightforest:ironwood_sword`|
| `/minecraft:.*_planks/`  | All vanilla planks               | `minecraft:oak_planks`, `minecraft:spruce_planks`         |
| `/.*_brick.*/`           | Anything with "brick" in name    | `minecraft:bricks`, `minecraft:brick_slab`                |
| `/^minecraft:diamond_/`  | Vanilla diamond items            | `minecraft:diamond_sword`, `minecraft:diamond_pickaxe`    |
| `/.*_ore$/`              | Items ending with "_ore"         | `minecraft:iron_ore`, `minecraft:gold_ore`                |

### **Examples**

```yaml
# Match all wood variants from a mod
wood_items:
  items:
  - /regions_unexplored:(stripped_)?.*_(log|wood|planks|slab|stairs)/

# Match all colored blocks
colored_glass:
  items:
  - /minecraft:.*_stained_glass$/
  - /minecraft:.*_stained_glass_pane$/

# Combine regex with explicit items
ranged:
  items:
  - /.*:.*bow$/                    # All bows
  - /.*:.*arrow/                   # All arrows
  - minecraft:trident              # Explicit item
```

---

## 🎛️ **Filters**

Filters allow you to sort items based on their **properties** (enchantments, custom names, etc.), not just their item type.

### **Filter Syntax**

Filters can be used in two places:

**1. Category-level filters** (apply to all items in the category):
```yaml
greater_swords:
  filters:
  - enchantment: max
  includes:
  - swords
```

**2. Item-level filters** (filter all game items, then add matches):
```yaml
all_enchanted:
  items:
  - enchantment: '*'       # Add ALL enchanted items in the game
```

### **Filter Negation**

Prefix any filter key with `!` to negate it. When using `!` prefix, you must quote the key in YAML:

```yaml
filters:
- "!enchantment": max      # NOT max-level enchanted
- "!custom_name": '*'      # NOT renamed
- "!stackable":            # NOT stackable (max stack size = 1)
```

### **Available Filters**

#### `enchantment`

Matches items with enchantments (works on both equipped items and enchanted books).

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

# Books without max enchantments (using negation)
lesser_books:
  priority: 4
  filters:
  - "!enchantment": max
  items:
  - minecraft:enchanted_book
```

#### `custom_name`

Matches items that have been renamed (e.g., in an anvil).

| Value      | Description                                       |
|------------|---------------------------------------------------|
| `*`        | Item has any custom name                          |
| `My Sword` | Item's display name matches exactly (case-insens) |

```yaml
# All renamed gear goes to a special chest
named_items:
  priority: 1
  filters:
  - custom_name: '*'
  includes:
  - gear
```

#### `stackable`

Matches items that can stack beyond 1. **This filter takes no value** - the value is ignored.

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

### **Multiple Filters**

When multiple filters are specified, items must match **ALL** filters (AND logic):

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

## 🚀 **Example Sorting Flow**

1. Place an `[input]` sign on your input chest.
2. Place category signs like `[gear]`, `[armor_helmets]`, etc. on destination chests.
3. **Right-click the `[input]` sign** to sort!

All items will be sorted into their proper chests, filling from the **bottom up** for stacked chests.

---

## ⚙️ **Configuration**

SortCraft configuration is stored in `config/sortcraft/`:

```
config/sortcraft/
├── config.yaml          # Main configuration
└── categories/          # Category definition files
    ├── 01_materials.yaml
    ├── 02_tools.yaml
    └── ...
```

### **config.yaml**

```yaml
# SortCraft Configuration

# Log level: TRACE, DEBUG, INFO, WARN, ERROR
# Default: WARN
logLevel: WARN

# Search radius for finding signs (in blocks)
# Default: 64
searchRadius: 64
```

| Option         | Default | Description                                                  |
|----------------|---------|--------------------------------------------------------------|
| `logLevel`     | `WARN`  | Controls logging verbosity. Use `DEBUG` for troubleshooting. |
| `searchRadius` | `64`    | Maximum distance (in blocks) to search for category signs.   |

---

## 🚀 **Start Sorting Your World Today**

SortCraft brings **peace and order** to your Minecraft storage chaos. Configure your categories, place your signs, and let SortCraft handle the rest.

---

## 🔧 **Development & Building**

### Multi-Version Support

SortCraft supports Minecraft versions **1.21.1, 1.21.4 through 1.21.10** (8 versions).

**Note:** MC 1.21.2 and 1.21.3 are not supported due to incompatible API changes in Architectury and Minecraft that would require extensive refactoring. These were transitional versions and most servers/clients use either 1.21.1 or 1.21.4+.

To build for all supported versions, run:

```powershell
.\build-all-versions.ps1
```

Built JARs will be placed in `build/libs/all-versions/`.

### Dependency Version Metadata URLs

When updating to new Minecraft versions, use these official metadata sources to find compatible dependency versions:

| Dependency             | Metadata URL                                                                       |
|------------------------|------------------------------------------------------------------------------------|
| **Fabric API**         | `https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml` |
| **Fabric Loader**      | `https://meta.fabricmc.net/v2/versions/loader`                                     |
| **Fabric Yarn**        | `https://meta.fabricmc.net/v2/versions/yarn/{mc_version}`                          |
| **NeoForge**           | `https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge`   |
| **Architectury API**   | `https://maven.architectury.dev/dev/architectury/architectury/maven-metadata.xml`  |
| **Minecraft Versions** | `https://piston-meta.mojang.com/mc/game/version_manifest_v2.json`                  |

---


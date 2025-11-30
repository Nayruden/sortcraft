# SortCraft

**SortCraft** is a powerful, configurable bulk sorting mod for Minecraft, supporting both Fabric and NeoForge loaders. It organizes your chests with intuitive, customizable categories to keep your world neat and efficient.

---

## 📦 **How It Works**

SortCraft uses **signs to label chests** and sorts items into them based on categories you define in a YAML config.

### 📝 **Chest Labeling**

* **Input Chest:** Place a sign on a chest with `[input]` on the first line. This chest acts as the **source of items** to sort.
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

✔ **Example usage:**

```
/sort input
```

Sorts all items in your `[input]` chest into their category chests.

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

SortCraft uses a **YAML config** to define categories, priorities, and filters. Here's a **short example**:

```yaml
# Priority for each category defaults to '10' if unspecified
armor_boots:
  items:
  - minecraft:diamond_boots
  - minecraft:golden_boots
  - minecraft:iron_boots

armor_helmets:
  items:
  - minecraft:diamond_helmet
  - minecraft:golden_helmet
  - minecraft:iron_helmet

armor:
  priority: 20
  includes:
  - armor_boots
  - armor_helmets

bows:
  items:
  - minecraft:bow

swords:
  items:
  - minecraft:diamond_sword
  - minecraft:golden_sword
  - minecraft:iron_sword

weapons:
  priority: 20
  includes:
  - bows
  - swords

gear:
  includes:
  - armor
  - weapons

gear_greater:
  priority: 5
  filters:
  - enchantment: max
  includes:
  - gear
```

### ⚠️ **Notes on Filters**

* **Filters are ANDed together.** Items must match **all filters** to qualify for a category.
* Supported filters include:
  * `enchantment`: `max`, `any`, specific enchantment names, or `!enchantment` to negate
  * `custom_name`: Matches items with custom names. Use `*` to match any custom name, or a specific string to match exactly
  * `stackable`: `true` to match items that can stack beyond 1, `false` to match unstackable items

---

## 🚀 **Example Sorting Flow**

1. Place an `[input]` sign on your input chest.
2. Place category signs like `[gear]`, `[armor_helmets]`, etc. on destination chests.
3. Run:

```
/sort input
```

All items will be sorted into their proper chests, filling from the **bottom up** for stacked chests.

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

| Dependency | Metadata URL |
|------------|--------------|
| **Fabric API** | `https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml` |
| **Fabric Loader** | `https://meta.fabricmc.net/v2/versions/loader` |
| **Fabric Yarn** | `https://meta.fabricmc.net/v2/versions/yarn/{mc_version}` |
| **NeoForge** | `https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge` |
| **Architectury API** | `https://maven.architectury.dev/dev/architectury/architectury/maven-metadata.xml` |
| **Minecraft Versions** | `https://piston-meta.mojang.com/mc/game/version_manifest_v2.json` |

---


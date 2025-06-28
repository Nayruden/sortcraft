# SortCraft

**SortCraft** is a powerful, configurable bulk sorting mod for Minecraft Fabric. It organizes your chests with intuitive, customizable categories to keep your world neat and efficient.

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


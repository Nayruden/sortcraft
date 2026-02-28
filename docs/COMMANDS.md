# SortCraft Commands

All SortCraft commands start with `/sort`.

> **Note:** Some commands (`/sort reload`, `/sort dump`, `/sort diagnostics`) require operator privileges on servers.

---

## `/sort help`

Shows help and usage information for all Sortcraft commands.

---

## `/sort input`

Triggers the sorting logic for the **nearest `[input]` chest**, scanning its contents and distributing items into destination chests based on your YAML category definitions.

💡 **Tip:** Right-clicking the `[input]` sign is usually faster and more convenient!

---

## `/sort preview`

Previews what items will be sorted where, **without actually moving them**. Outputs a categorized list showing which items will go to which category and item counts per category.

Use this to verify your configuration before actually sorting.

💡 **Tip:** You can also **Shift + right-click** the `[input]` sign to trigger a preview!

---

## `/sort diagnostics`

Generates a **`sortdiag.yaml` file** in your config folder with current item counts, storage locations, and slot usage by category. Use this to audit storage utilization and plan expansions.

---

## `/sort whereis <item_id>`

Locates where a specific item is stored. Displays the chest locations in chat and highlights them with **glowing outlines** visible through walls.

**Example:**
```
/sort whereis minecraft:diamond_sword
```

---

## `/sort category <item_id>`

Outputs the **categories assigned to an item** based on your YAML configuration.

**Example:**
```
/sort category minecraft:diamond_sword
```
Returns: `swords, gear, weapons`

---

## `/sort reload`

Reloads all Sortcraft configuration (`config.yaml` and category definitions) without restarting the server. Useful when editing config files while the server is running.

---

## `/sort dump`

Generates JSON files containing all item tags from the registry. Useful for debugging and discovering available item tags for your category definitions. Output files are placed in `config/sortcraft/`.

---

## `/sort shareconfig <id>`

Tests a [CategoryCraft](https://categorycraft.com) share config by ID. Downloads and parses the shared category configuration, then reports how many categories and unique items it contains.

Use this to verify a share ID works before putting it on an `[input]` sign.

**Example:**
```
/sort shareconfig ksJgx-mb
```
Returns: `Share config 'ksJgx-mb' loaded: 14 categories, 312 unique items`

> **Note:** Share configs must be enabled in `config.yaml` (`shareConfigs: true`, which is the default). See [Share Configs](CONFIGURATION.md#share-configs) for setup details.

---

## Command Summary

| Command | Description |
|---------|-------------|
| `/sort help` | Show help |
| `/sort input` | Sort nearest input chest |
| `/sort preview` | Preview sorting without moving items |
| `/sort diagnostics` | Generate storage report |
| `/sort whereis <item>` | Find where an item is stored |
| `/sort category <item>` | Show categories for an item |
| `/sort reload` | Reload configuration |
| `/sort dump` | Export item tags to JSON |
| `/sort shareconfig <id>` | Test a CategoryCraft share config |


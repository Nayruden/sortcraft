# Sortcraft Commands

All Sortcraft commands start with `/sort`.

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


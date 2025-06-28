# Bulk Sorter

Bulk Sorter is a Fabric mod for Minecraft 1.21+ that allows players to automatically sort items into nearby chests based on customizable categories.

## Features

- **Automatic Sorting:** Sorts items from an input chest into destination chests based on item category.
- **Recursive Container Support:** Opens and sorts contents of shulker boxes and bundles, then sorts the empty containers themselves.
- **Smart Chest Stack Detection:** Sorts into vertically stacked chests from bottom to top, unless a chest in the stack belongs to a different category.
- **Diagnostics & Previews:** Provides command-line previews and diagnostics of storage usage.
- **WhereIs Command:** Locates stored items and highlights their chest locations with particle effects.

## Usage

### Setting Up

1. Place a `[input]` sign on a block next to a chest. This chest becomes the input chest.
2. Place signs on blocks next to other chests using `[category]` where `category` is one of the supported item categories (e.g. `[ores]`, `[wood_oak]`, `[tools_swords]`, `[containers]`).
3. Run the `/sort input` command while near the input chest.

### Commands

All commands use the `/sort` base.

- `/sort help` - Show help and usage info.
- `/sort input` - Trigger the sorting logic for the nearest `[input]` chest.
- `/sort preview` - Preview what categories and item counts would be sorted.
- `/sort diagnostics` - Output a `sortdiag.yaml` file with item counts, storage locations, and slot usage.
- `/sort whereis <item_id>` - Locate where a specific item is stored, with particle indicators.
- `/sort category <item_id>` - Output the category of an item.

### Example Categories

- `containers`: Bundles, shulker boxes, chests
- `tools_swords`: All sword types
- `wood_oak`, `wood_spruce`, etc.: Organized by wood type
- `lights`: Torches, lanterns, candles
- `cooked_food`, `raw_food`, `farming`, `ores`, `redstone`, etc.

Over 40 categories are supplied by default. The full list is maintained in a YAML config.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/).
2. Place this mod's JAR file into the `mods/` folder.
3. Launch Minecraft with the Fabric profile.

## Configuration

- Category mappings are defined in a YAML file placed in the `config/sorter/` folder.
- Use `/sort diagnostics` to generate a report showing current item distributions.

## Known Limitations

- Only supports nearby chests (within a radius of 64 blocks from the input sign).
- Only supports wall signs (not standing signs) for category labeling.

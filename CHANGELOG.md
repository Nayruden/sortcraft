# Changelog

All notable changes to SortCraft will be documented in this file.

---

## [1.3.1] - 2026-04-24

### Added

- **Minecraft 26.1 - 26.1.2 support** — SortCraft now works on Minecraft 26.1 - 26.1.2

## [1.3.0] - 2026-02-27

### Added

- **Modded storage support** — SortCraft can now sort into modded storage blocks (Storage Drawers, Sophisticated Storage, etc.) via a new platform-agnostic `SortCraftStorage` abstraction. Fabric uses the Transfer API; NeoForge uses `IItemHandler`
- **Share config support** — Input signs can reference a CategoryCraft (https://categories.craftlabs.nexus/) share ID to use a shared category configuration instead of the global one. Enabled via `share_configs_enabled` in server config
- **`/sort shareconfig <id>` command** — Test and inspect a CategoryCraft share config by ID, showing category and item counts
- **OR logic in filter rules** — Filter rules now support list values to match items against multiple conditions (e.g., `enchantment: [minecraft:sharpness, minecraft:unbreaking]`)

### Changed

- **Shared gametest utilities** — Common gametest helpers (TestHelper, TestScenarios, TestCategories, etc.) moved to `common/src/gametest/java` so they are shared across Fabric and NeoForge loaders

### Fixed

- **SnakeYAML relocated** — SnakeYAML is now relocated to `net.sortcraft.shadow.org.yaml` to prevent JPMS split-package conflicts with other mods
- **Enchantment filter** — Enchantment filters no longer treat items with empty enchantment components as enchanted (items could have a non-null but empty `ENCHANTMENTS` or `STORED_ENCHANTMENTS` component, causing false matches)

---

## [1.2.1] - 2026-01-22

### Fixed

- **Folder case name** — Fixed the config and log folder names to be lowercase `sortcraft` instead of `SortCraft`. Starting with version `1.2.0`, the folder was created as `SortCraft`, which is not consistent with prior versions.

## [1.2.0] - 2026-01-12

### Added

- **Minecraft 1.21.11 support** — SortCraft now works on Minecraft 1.21.11
- **Chest stack sorting** — You can now sort entire vertical stacks of input chests with a single click. Place `[input]` signs on a column of chests, and right-click to sort all of them at once
- **Shift + right-click preview** — Shift + right-click on an `[input]` sign to preview what would be sorted without actually moving items (identical to `/sort preview`)
- **Improved chest highlighting** — Enhanced visual feedback when using `/sort whereis`

### Changed

- **Rebranded to SortCraft** — The mod has been renamed from "Sortcraft" to "SortCraft" (capital C) for better unity with our other projects
- **Operator privileges required** — Sensitive commands (`/sort reload`, `/sort dump`, `/sort diagnostics`) now require operator privileges on servers
- **Improved reload feedback** — The `/sort reload` command now properly notifies the player who ran the command

### Fixed

- **Reload command feedback** — Fixed an issue where `/sort reload` feedback was not being sent to the correct player
- Various bug fixes and stability improvements

### Removed

- **Dropped Minecraft 1.21.7 and 1.21.9 support** — These versions are no longer supported. Please use 1.21.1, 1.21.4–1.21.6, 1.21.8, 1.21.10, or 1.21.11

---

## [1.1.0]

Initial public release

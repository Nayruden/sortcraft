# Changelog

All notable changes to SortCraft will be documented in this file.

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

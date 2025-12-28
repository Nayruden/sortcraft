# Audit Logging

Sortcraft includes an optional audit logging system that records all sorting operations to structured JSONL files. This is useful for server administrators who want to track item movements or debug sorting issues.

---

## Enabling Audit Logging

Add an `audit` section to your `config/sortcraft/config.yaml`:

```yaml
audit:
  enabled: true
  detailLevel: SUMMARY    # FULL, SUMMARY, or MINIMAL
  logPreviews: false      # Whether to log preview operations
  asyncWrite: true        # Write logs asynchronously (recommended)
  maxFileSizeMb: 10       # Rotate log files at this size
  maxFiles: 7             # Keep this many rotated log files
```

---

## Detail Levels

| Level     | Description                                                                 |
|-----------|-----------------------------------------------------------------------------|
| `FULL`    | Complete details including every item movement with source/destination positions |
| `SUMMARY` | Category counts and totals without individual movement records              |
| `MINIMAL` | Only logs failed or partial operations (errors and warnings)                |

---

## Log Location

Audit logs are written to `logs/sortcraft/audit-YYYY-MM-DD.log` in JSONL format (one JSON object per line).

---

## Example Log Entry (FULL level)

```jsonc
{
  "operationId": "a1b2c3d4-...",
  "timestamp": "2024-01-15T10:30:00Z",
  "playerName": "Steve",
  "playerUuid": "f7c77d99-...",
  "dimension": "minecraft:overworld",
  "operationType": "SORT",
  "inputChestPos": {"x": 100, "y": 64, "z": 200},
  "searchRadius": 64,
  "totalItemsProcessed": 2,
  "totalItemsSorted": 2,
  "durationMs": 12,
  "status": "SUCCESS",
  "errorMessage": null,
  "movements": [
    {
      "itemId": "minecraft:diamond_sword",
      "quantity": 1,
      "category": "named_items",
      "destinationPos": {"x": 102, "y": 64, "z": 200},
      "partial": false,
      "metadata": {
        "customName": "Excalibur",
        "enchantments": [
          {"id": "minecraft:sharpness", "level": 5},
          {"id": "minecraft:unbreaking", "level": 3}
        ]
      }
    },
    {
      "itemId": "minecraft:potion",
      "quantity": 1,
      "category": "potions",
      "destinationPos": {"x": 106, "y": 64, "z": 200},
      "partial": false,
      "metadata": {
        "potionType": "minecraft:strong_healing"
      }
    }
  ],
  "categorySummary": {
    "named_items": 1,
    "potions": 1
  },
  "unknownItems": [],
  "overflowCategories": []
}
```

> **Note:** Actual log files use compact JSON (one line per entry). The example above is formatted for readability.

---

## Log Entry Fields

| Field | Description |
|-------|-------------|
| `operationId` | Unique identifier for the operation |
| `timestamp` | ISO 8601 timestamp |
| `playerName` / `playerUuid` | Player who triggered the sort |
| `dimension` | Minecraft dimension |
| `operationType` | `SORT` or `PREVIEW` |
| `inputChestPos` | Coordinates of the input chest |
| `totalItemsProcessed` | Number of items examined |
| `totalItemsSorted` | Number of items moved |
| `durationMs` | Operation duration in milliseconds |
| `status` | `SUCCESS`, `PARTIAL`, or `FAILED` |
| `movements` | Array of item movements (FULL only) |
| `categorySummary` | Count of items per category (SUMMARY+) |
| `unknownItems` | Items that didn't match any category |
| `overflowCategories` | Categories where chests were full |

---

## Configuration Options

| Option | Default | Description |
|--------|---------|-------------|
| `enabled` | `false` | Enable/disable audit logging |
| `detailLevel` | `SUMMARY` | `FULL`, `SUMMARY`, or `MINIMAL` |
| `logPreviews` | `false` | Log `/sort preview` operations |
| `asyncWrite` | `true` | Non-blocking log writes |
| `maxFileSizeMb` | `10` | Rotate logs at this size |
| `maxFiles` | `7` | Number of rotated files to keep |


# Third-Party Test Jars

Runtime-only jars used by the local GameTest setup live in this folder.

Current usage in this repo:

- Fabric `Storage Drawers-fabric-1.21.5-16.0.2.jar`
  - Used for Fabric GameTests on `1.21.5`
  - Also used as `compileOnly` fallback for some other Fabric GameTest versions where runtime loading is skipped
- Fabric `StorageDrawers-fabric-1.21.11-20.0.0.jar`
  - Used for Fabric GameTests on `1.21.10` and `1.21.11`
- NeoForge `neoforge-sophisticatedstorage-1.21.5-1.5.28.1490.jar`
  - Used for NeoForge GameTests on `1.21.5`
  - NeoForge Sophisticated Storage tests should skip gracefully on other MC versions unless a matching jar is added

Guidelines:

- Keep these jars runtime-only in Gradle test wiring when possible.
- Gate loader/version-specific jars by `targetMcVersion` so unsupported versions still compile.
- If you add a new jar, update this file and the relevant `fabric/build.gradle` or `neoforge/build.gradle` GameTest block.

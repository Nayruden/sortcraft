# AGENTS.md

Guidance for AI coding agents working in the SortCraft repository. Keep changes
minimal, verified, and consistent with the surrounding code.

## What this is

SortCraft is a server-side Minecraft mod that sorts items into chests labeled with
signs (e.g. `[ores]`). It is multi-platform (Fabric + NeoForge) with a shared
`common` module.

## Branches

- `26.x` — Minecraft 26.x (this branch). Supported versions are in `versions.json`.
- `1.21.x` — the Minecraft 1.21.x series.

Pick the branch that matches the Minecraft version you are targeting. Do not assume
1.21.x behavior applies on `26.x` or vice versa.

## Layout

- `common/` — platform-agnostic logic (sorting, categories, config, audit, commands).
  Most changes go here.
- `fabric/`, `neoforge/` — thin loader-specific implementations of the service
  interfaces (`PlatformService`, `StorageLookupService`).
- `common/src/main/.../platform` & `container` — abstraction seams; loader modules
  provide the implementations under `META-INF/services`.
- `fabric/src/test/` — JUnit unit tests.
- `*/src/gametest/` — in-game GameTests; shared helpers live in `common/src/gametest/`.
- `docs/` — user documentation. `versions/<mc>/gradle.properties` — per-version deps.

## Build, test, verify

- **Requires JDK 25** (Gradle toolchain). Builds need network access to resolve
  Minecraft / Fabric / NeoForge artifacts and the `foojay-resolver` plugin.
- Build + unit tests for the default version: `./gradlew build`
- Target a specific version: `./gradlew build -Pmc_version=26.1.2`
  (must be listed in `versions.json`, else the build fails fast).
- Collect distributable JARs: `./gradlew buildAndCollect` (output in `build/libs/`).
- GameTests (heavy; need a runtime + third-party jars, see below):
  `./gradlew runGameTests`.
- "Done" = the relevant `./gradlew build` passes for the target version and any new
  behavior has a unit test (and a GameTest if it is in-game behavior).

## Conventions

- Loggers use the name `"sortcraft"`: `LoggerFactory.getLogger("sortcraft")`
  (gametest helpers use `"sortcraft-gametest"`). Don't introduce new logger names.
- Fabric and NeoForge GameTest classes are kept in sync; `GameTestParityTest`
  enforces this. If you add/rename a GameTest in one loader, mirror it in the other
  and register it (fabric: `fabric.mod.json` entrypoints; neoforge: the catalog class).
- YAML is parsed with SnakeYAML's safe default `new Yaml()` — do not switch to an
  unsafe constructor or enable arbitrary type tags.
- Config keys are camelCase (e.g. `searchRadius`, `shareConfigs`).

## Gotchas / do not touch casually

- `versions.json` drives both the build (`build.gradle`) and the CI matrix
  (`.github/workflows/build.yml`). Edit deliberately.
- `third-party/*.jar` are runtime-only test dependencies and are **gitignored**
  (not present in a fresh clone). GameTests that need them skip gracefully when
  absent; do not commit jars. See `third-party/README.md`.
- On-disk config/log paths are lowercase `config/sortcraft/` and `logs/sortcraft/`
  (a 1.2.0 regression to `SortCraft` was fixed in 1.2.1 — keep it lowercase).
- Don't commit build output, run logs, or `run/` artifacts into the repo
  (especially not under `.github/workflows/`).

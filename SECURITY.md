# Security Policy

## Supported Versions

SortCraft is distributed for current Minecraft releases on Fabric and NeoForge.
Security fixes are made against the latest released version (see `gradle.properties`
and [CHANGELOG.md](CHANGELOG.md)). Older Minecraft versions are not guaranteed to
receive backported fixes.

## Reporting a Vulnerability

Please **do not** open a public GitHub issue for security vulnerabilities.

Instead, report privately via one of:

- GitHub's [private vulnerability reporting](https://github.com/nayruden/sortcraft/security/advisories/new)
  ("Report a vulnerability" under the Security tab), or
- A direct message to the maintainers on our [Discord](https://discord.gg/YyutMfPNp4).

When reporting, please include:

- A description of the issue and its impact.
- Steps to reproduce (a minimal config, sign layout, or share ID is ideal).
- The Minecraft version, loader (Fabric/NeoForge), and SortCraft version.

We will acknowledge your report, investigate, and coordinate a fix and disclosure
timeline with you.

## Scope Notes

SortCraft runs server-side and reads:

- Local YAML configuration under `config/sortcraft/` (trusted, server-admin controlled).
- Remote CategoryCraft "share configs" fetched over HTTPS when a player writes a
  share ID on an `[input]` sign. Share IDs are format-validated, responses are size-capped,
  and YAML is parsed with SnakeYAML's safe default loader. Share configs can be disabled
  server-wide via `shareConfigs: false` in `config.yaml`.

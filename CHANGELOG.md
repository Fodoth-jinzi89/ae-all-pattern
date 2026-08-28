# Changelog

## 0.1.6 - 2026-08-28

- Added EMI + TooManyRecipeViewers support to the aggregate-pattern generator, including JEI compatibility recipes exposed through TMRV.
- Preserved candidate ingredients, Mekanism chemicals, and probabilistic output metadata when scanning EMI recipes.
- Added per-pattern safeguards for chance-based main outputs and byproducts.
- Fixed Mekanism recipe retention and oversized custom-payload failures during client-side aggregate scans.
- Standardized the Chinese “样板” terminology across the interface.
- Reworked the aggregate-pattern right-click settings into compact AE-style option rows with inline states and hover help.
- Added an explicit EMI/TMRV development runtime while keeping the release JAR and dedicated server independent of recipe-viewer mods.
- Isolated automated GameTests from the playable test save so minimal dependency runs cannot remove optional-mod machines.

## 0.1.0 - 2026-08-25

- Added the channel- and power-aware All Pattern Linker AE2 node.
- Added server-authoritative two-step binding, ownership validation, world persistence, and owner-only purple target outlines.
- Added virtual AE2 processing patterns backed by deterministic recipe snapshots and stable binding-specific identities.
- Added persistent, all-or-nothing incoming material buffering with safe recovery on unbind or linker removal.
- Added vanilla furnace, blast furnace, and smoker support.
- Added optional Mekanism smelting, crushing, and enriching machine/factory adapters.
- Added optional JEI contextual help without making JEI a server recipe authority.
- Added reload-aware caches, diagnostics commands, a 10,000-fingerprint performance guard, dual dependency-matrix GameTests, and release JAR verification.
- Added original binder/linker pixel art and a mod icon.

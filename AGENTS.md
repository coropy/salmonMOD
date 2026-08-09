# salmonMOD agent contract

This repository is a Fabric mod that recreates Splatoon-style ink gameplay in Minecraft Java Edition.

## Fixed project facts

- Minecraft `26.2`
- Fabric Loader `0.19.3`
- Fabric API `0.155.2+26.2`
- Java `25`, Loom `1.17.17-SNAPSHOT`
- Mojang official mappings
- Mod id `salmon`; main package `yam.salmon`
- Windows workspace; use PowerShell and `gradlew.bat`

## Evidence priority

1. The user's current request and current source/build output.
2. This file and path-conditional rules in `.clinerules/`.
3. The existing Graphify graph for navigation, followed by source verification.
4. `docs/salmon-project-reference.md` for historical orientation only.
5. Official documentation for version-sensitive facts not resolved locally.

Never treat the long reference document or an inferred Graphify edge as stronger evidence than current source.

## Required workflow

1. Run `git status --short` and preserve unrelated or pre-existing changes.
2. For cross-file or architectural work, query the existing graph first:
   `graphify query "<specific task question>" --budget 1200`
3. If the result is broad or truncated, narrow it with a more specific query, `graphify explain`, or `graphify path`. Do not start a repository-wide search.
4. Read the smallest relevant source set and confirm exact behavior before editing.
5. Implement the smallest coherent change. Do not modify generated files under `build/`, `.gradle/`, `bin/`, `run/`, or `graphify-out/`.
6. Run focused verification. For normal Java changes use:
   `./gradlew.bat compileJava compileClientJava`
   Add `test`, `processResources`, or `build` only when the change warrants it.
7. After validated source-code changes, run `graphify update .` once only if `graphify-out/` was clean before the task. If it was already dirty, preserve it and report that the graph update was skipped.
8. Report changed files, verification results, and any remaining risk.

Exact-file edits and documentation/resource-only tasks may skip the initial Graphify query.

## Architecture map

- Server entry point: `src/main/java/yam/salmon/Salmon.java`
- Client entry point: `src/client/java/yam/salmon/client/SalmonClient.java`
- Server packages: `arena`, `block`, `combat`, `command`, `ink`, `item`, `network`, `selection`, `weapon`
- Client packages: `arena`, `ink`, `shot`
- Resources: `src/main/resources/assets/salmon` and `src/main/resources/data`
- Current projectile path centers on `InkProjectileLifecycleManager`; `InkShooterService` and `InkTrailPaintService` are legacy/deprecated paths unless current source proves otherwise.

## Correctness constraints

- Keep logical-side boundaries strict: no client-only class references from common/server code.
- Network payload registration and client receivers must stay symmetric.
- Preserve arena-scoped persistence and monotonic revision behavior when changing ink storage or synchronization.
- Keep arena deletion and cleanup idempotent.
- Do not guess legacy Minecraft/Fabric APIs. For exact 26.2 signatures, follow `.clinerules/minecraft-java.md`.
- Update this `AGENTS.md` only when durable project instructions or the high-level architecture change. Do not append task logs or rewrite it after routine changes.

## Scope discipline

- Do not refactor deprecated code, unrelated packages, formatting, or assets unless required by the task.
- Do not discard user changes or use destructive Git commands.
- If verification fails for a pre-existing reason, distinguish it from regressions introduced by the task.

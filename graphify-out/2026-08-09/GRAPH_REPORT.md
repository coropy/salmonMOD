# Graph Report - salmonMOD  (2026-08-09)

## Corpus Check
- 110 files · ~40,272 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1100 nodes · 2651 edges · 51 communities (45 shown, 6 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 283 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `3e0f2380`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- .getInstance
- 2.1. サーバー側 (`src/main/java/yam/salmon/`)
- InkSurfacePatchId
- FaceBasis
- ClientInkShot
- ArenaDebugRenderer
- ExampleMixin.java
- InkTrajectoryResult
- .checkPaintable
- InkRenderer.java
- InkWeaponConfig
- .useWithoutItem
- PlayerMarkerSelectionManager
- ClientInkTrailDrop
- InkAreaMarkerBlockEntity
- Surface Patch Extraction
- Result
- Salmon.java
- .fire
- ActiveInkShot
- ClientInkCache
- InkArena
- ClientInkShotManager
- Mixin Example
- Paint Rules
- Ink Source Resolver
- Gradle Script
- Mod Documentation
- Setup README
- CI Workflow
- InkShotEffects.java
- InkShotSpawnPayload
- InkCellQuad
- InkArenaClearPayload
- .extractInkState
- InkSyncBeginPayload
- CustomPacketPayload
- InkShotImpactPayload
- InkPlaneCoordinates
- InkTrailDropImpactPayload
- InkTrailDropSpawnPayload
- .distributePaint
- Type
- InkFaceUpdatePayload
- InkCellGeometry
- ClientInkSurface
- ClientInkSurfaceKey
- Minecraft 26.2 and Fabric Java rules
- minecraft-assets.md

## God Nodes (most connected - your core abstractions)
1. `InkArena` - 71 edges
2. `InkSurfacePatchId` - 42 edges
3. `ActiveInkShot` - 42 edges
4. `InkArenaManager` - 40 edges
5. `ActiveTrailDrop` - 31 edges
6. `InkFaceData` - 29 edges
7. `InkStorage` - 26 edges
8. `ArenaDebugRenderer` - 25 edges
9. `InkProjectileLifecycleManager` - 25 edges
10. `InkWeaponConfig` - 25 edges

## Surprising Connections (you probably didn't know these)
- `AGENTS.md - Splatoon Mod Development Guidelines` --references--> `Mod Icon`  [EXTRACTED]
  AGENTS.md → src/main/resources/assets/salmon/icon.png
- `AGENTS.md - Splatoon Mod Development Guidelines` --references--> `Ink Area Marker Block Texture`  [EXTRACTED]
  AGENTS.md → src/main/resources/assets/salmon/textures/block/ink_area_marker.png
- `ClientInkCache` --references--> `ClientInkSurface`  [EXTRACTED]
  src/client/java/yam/salmon/client/ink/ClientInkCache.java → src/client/java/yam/salmon/client/ink/ClientInkSurface.java
- `ClientInkCache` --references--> `ClientInkSurfaceKey`  [EXTRACTED]
  src/client/java/yam/salmon/client/ink/ClientInkCache.java → src/client/java/yam/salmon/client/ink/ClientInkSurfaceKey.java
- `ClientInkSurface` --references--> `ClientInkSurfaceKey`  [EXTRACTED]
  src/client/java/yam/salmon/client/ink/ClientInkSurface.java → src/client/java/yam/salmon/client/ink/ClientInkSurfaceKey.java

## Import Cycles
- None detected.

## Communities (51 total, 6 thin omitted)

### Community 0 - ".getInstance"
Cohesion: 0.09
Nodes (15): CommandBuildContext, CommandContext, CommandDispatcher, CommandSelection, CommandSourceStack, ServerPlayer, BlockPos, ServerLevel (+7 more)

### Community 1 - "2.1. サーバー側 (`src/main/java/yam/salmon/`)"
Cohesion: 0.06
Nodes (30): 1. プロジェクト概要 & 開発環境, 2.1. サーバー側 (`src/main/java/yam/salmon/`), 2.2. クライアント側 (`src/client/java/yam/salmon/client/`), 2.3. リソース (`src/main/resources/`), 2. ディレクトリ構造と主要クラスの役割, 3.1. Phase 1〜5: 基盤システム（完了）, 3.2. Phase 6+: ブキ・戦闘システム, 3. 実装済み機能のステータス (+22 more)

### Community 2 - "InkSurfacePatchId"
Cohesion: 0.14
Nodes (4): InkSurfacePatchId, Deprecated, Direction, FriendlyByteBuf

### Community 3 - "FaceBasis"
Cohesion: 0.10
Nodes (19): Axis, FaceBasis, BlockPos, Direction, Vec3, LocalUV, InkHitResolver, BlockGetter (+11 more)

### Community 5 - "ArenaDebugRenderer"
Cohesion: 0.09
Nodes (23): ArenaDebugRenderer, ArenaRenderState, Box, CachedArena, BlockPos, Draw, ExecuteInfo, Level (+15 more)

### Community 6 - "ExampleMixin.java"
Cohesion: 0.53
Nodes (4): ExampleMixin, CallbackInfo, Inject, Mixin

### Community 7 - "InkTrajectoryResult"
Cohesion: 0.07
Nodes (32): InkCollisionRaycast, BlockHitResult, Entity, Level, Logger, Vec3, InkTrailPaintConfig, RandomSource (+24 more)

### Community 8 - ".checkPaintable"
Cohesion: 0.10
Nodes (21): Block, TagKey, ModBlockTags, InkPaintability, BlockGetter, BlockPos, BlockState, Direction (+13 more)

### Community 9 - "InkRenderer.java"
Cohesion: 0.20
Nodes (11): InkRenderer, Draw, ExecuteInfo, LevelExtractionContext, LevelRenderContext, Matrix4f, Minecraft, RenderPipeline (+3 more)

### Community 10 - "InkWeaponConfig"
Cohesion: 0.06
Nodes (26): InteractionHand, Item, InkShooterItem, InteractionResult, Level, Logger, Override, Player (+18 more)

### Community 11 - ".useWithoutItem"
Cohesion: 0.15
Nodes (13): Block, BlockEntityType, ArenaPermission, Player, InkableBlock, BlockHitResult, BlockPos, BlockState (+5 more)

### Community 12 - "PlayerMarkerSelectionManager"
Cohesion: 0.26
Nodes (5): BlockPos, Level, ResourceKey, MarkerSelection, PlayerMarkerSelectionManager

### Community 13 - "ClientInkTrailDrop"
Cohesion: 0.11
Nodes (12): ClientInkTrailDrop, Vec3, InkShotRenderer, ExecuteInfo, LevelRenderContext, Matrix4f, Matrix4fc, RenderPipeline (+4 more)

### Community 14 - "InkAreaMarkerBlockEntity"
Cohesion: 0.12
Nodes (12): BlockEntity, ClientboundBlockEntityDataPacket, CompoundTag, Provider, InkAreaMarkerBlockEntity, BlockPos, BlockState, Nullable (+4 more)

### Community 15 - "Surface Patch Extraction"
Cohesion: 0.21
Nodes (10): FaceCandidate, FaceCellUV, InkSurfacePatchExtractor, BlockGetter, BlockPos, BlockState, Direction, Logger (+2 more)

### Community 16 - "Result"
Cohesion: 0.30
Nodes (5): InkShotResult, BlockPos, Direction, Vec3, Result

### Community 17 - "Salmon.java"
Cohesion: 0.33
Nodes (6): ClientModInitializer, ModInitializer, SalmonClient, Identifier, Logger, Salmon

### Community 18 - ".fire"
Cohesion: 0.07
Nodes (24): InkCombatService, Entity, Logger, ServerPlayer, InkPaintAccumulator, BlockPos, InkPaintingService, BlockPos (+16 more)

### Community 19 - "ActiveInkShot"
Cohesion: 0.06
Nodes (26): EntityHitResult, ActiveInkShot, Level, RandomSource, ResourceKey, Vec3, ActiveTrailDrop, Level (+18 more)

### Community 20 - "ClientInkCache"
Cohesion: 0.21
Nodes (4): ClientInkCache, BlockPos, Direction, Identifier

### Community 21 - "InkArena"
Cohesion: 0.05
Nodes (35): EntityBlock, SavedData, SavedDataType, InkArena, BlockPos, Codec, Level, Override (+27 more)

### Community 23 - "Mixin Example"
Cohesion: 0.53
Nodes (4): ExampleClientMixin, CallbackInfo, Inject, Mixin

### Community 24 - "Paint Rules"
Cohesion: 0.40
Nodes (4): InkPaintRule, ALLOW, DEFAULT, DENY

### Community 26 - "Gradle Script"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 27 - "Mod Documentation"
Cohesion: 0.67
Nodes (3): AGENTS.md - Splatoon Mod Development Guidelines, Mod Icon, Ink Area Marker Block Texture

### Community 32 - "InkShotEffects.java"
Cohesion: 0.40
Nodes (5): InkShotEffects, Direction, ServerLevel, ServerPlayer, Vec3

### Community 33 - "InkShotSpawnPayload"
Cohesion: 0.39
Nodes (5): InkShotSpawnPayload, FriendlyByteBuf, Override, StreamCodec, Vec3

### Community 34 - "InkCellQuad"
Cohesion: 0.67
Nodes (3): Colored, InkCellQuad, Direction

### Community 35 - "InkArenaClearPayload"
Cohesion: 0.36
Nodes (5): InkArenaClearPayload, FriendlyByteBuf, Identifier, Override, StreamCodec

### Community 36 - ".extractInkState"
Cohesion: 0.16
Nodes (4): ClientInkColors, ColoredQuad, InkRenderState, Override

### Community 37 - "InkSyncBeginPayload"
Cohesion: 0.36
Nodes (5): InkSyncBeginPayload, FriendlyByteBuf, Identifier, Override, StreamCodec

### Community 38 - "CustomPacketPayload"
Cohesion: 0.52
Nodes (4): CustomPacketPayload, InkSyncEndPayload, FriendlyByteBuf, StreamCodec

### Community 43 - "InkShotImpactPayload"
Cohesion: 0.36
Nodes (6): InkShotImpactPayload, Direction, FriendlyByteBuf, Override, StreamCodec, Vec3

### Community 44 - "InkPlaneCoordinates"
Cohesion: 0.33
Nodes (5): InkPlaneCoordinates, BlockPos, Direction, Vec3, LocalUV

### Community 45 - "InkTrailDropImpactPayload"
Cohesion: 0.50
Nodes (5): InkTrailDropImpactPayload, Direction, FriendlyByteBuf, StreamCodec, Vec3

### Community 47 - "InkTrailDropSpawnPayload"
Cohesion: 0.39
Nodes (5): InkTrailDropSpawnPayload, FriendlyByteBuf, Override, StreamCodec, Vec3

### Community 48 - ".distributePaint"
Cohesion: 0.05
Nodes (36): InkFaceCoordinates, Direction, InkFaceData, IntersectionMode, CELL_CENTER, CELL_RECTANGLE_INTERSECTION, Override, InkPaintDistributor (+28 more)

### Community 49 - "Type"
Cohesion: 0.17
Nodes (8): Override, Override, Override, Override, Type, BLOCK_HIT, ENTITY_HIT, MISS

### Community 50 - "InkFaceUpdatePayload"
Cohesion: 0.36
Nodes (6): InkFaceUpdatePayload, BlockPos, Direction, FriendlyByteBuf, Identifier, StreamCodec

### Community 53 - "InkCellGeometry"
Cohesion: 0.38
Nodes (4): InkCellGeometry, BlockPos, Deprecated, Direction

### Community 55 - "ClientInkSurface"
Cohesion: 0.43
Nodes (4): ClientInkSurface, BlockPos, Direction, Identifier

### Community 56 - "ClientInkSurfaceKey"
Cohesion: 0.57
Nodes (3): ClientInkSurfaceKey, BlockPos, Direction

### Community 57 - "Minecraft 26.2 and Fabric Java rules"
Cohesion: 0.40
Nodes (4): API verification, Known 26.2 conventions, Minecraft 26.2 and Fabric Java rules, Verification

## Knowledge Gaps
- **60 isolated node(s):** `Colored`, `CELL_CENTER`, `CELL_RECTANGLE_INTERSECTION`, `DEFAULT`, `ALLOW` (+55 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **6 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `InkSurfacePatchId` connect `InkSurfacePatchId` to `FaceBasis`, `Surface Patch Extraction`, `.distributePaint`, `.fire`, `InkFaceUpdatePayload`, `ClientInkCache`, `InkCellGeometry`, `InkArena`, `ClientInkSurface`, `ClientInkSurfaceKey`?**
  _High betweenness centrality (0.101) - this node is a cross-community bridge._
- **Why does `InkArena` connect `InkArena` to `.checkPaintable`, `.distributePaint`, `.fire`?**
  _High betweenness centrality (0.079) - this node is a cross-community bridge._
- **Why does `InkShotSpawnPayload` connect `InkShotSpawnPayload` to `CustomPacketPayload`, `Salmon.java`, `Type`, `ActiveInkShot`, `ClientInkShotManager`?**
  _High betweenness centrality (0.046) - this node is a cross-community bridge._
- **What connects `Colored`, `CELL_CENTER`, `CELL_RECTANGLE_INTERSECTION` to the rest of the system?**
  _60 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `.getInstance` be split into smaller, more focused modules?**
  _Cohesion score 0.08627450980392157 - nodes in this community are weakly interconnected._
- **Should `2.1. サーバー側 (`src/main/java/yam/salmon/`)` be split into smaller, more focused modules?**
  _Cohesion score 0.06451612903225806 - nodes in this community are weakly interconnected._
- **Should `InkSurfacePatchId` be split into smaller, more focused modules?**
  _Cohesion score 0.14285714285714285 - nodes in this community are weakly interconnected._
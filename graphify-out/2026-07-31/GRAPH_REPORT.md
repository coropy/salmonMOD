# Graph Report - salmonMOD  (2026-07-31)

## Corpus Check
- 99 files · ~36,528 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 934 nodes · 2178 edges · 38 communities (33 shown, 5 thin omitted)
- Extraction: 92% EXTRACTED · 8% INFERRED · 0% AMBIGUOUS · INFERRED: 179 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `1d3b1def`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- InkArena
- Type
- InkSurfacePatchId
- FaceBasis
- .distributePaint
- ArenaDebugRenderer
- ExampleMixin.java
- InkTrajectoryResult
- Block Paintability
- InkRenderer.java
- InkWeaponConfig
- .useWithoutItem
- InkFaceData
- ClientInkShot
- InkAreaMarkerBlockEntity
- Surface Patch Extraction
- Result
- .paintTrail
- InkSyncManager.java
- InkFaceUpdatePayload
- ArenaDebugPayload
- Deprecated
- Identifier
- Mixin Example
- Paint Rules
- Ink Source Resolver
- Gradle Script
- Mod Documentation
- Setup README
- CI Workflow
- InkShotVisualPayload
- Salmon.java
- InkArenaClearPayload
- InkSyncBeginPayload
- CustomPacketPayload
- InkPlaneCoordinates

## God Nodes (most connected - your core abstractions)
1. `InkArena` - 71 edges
2. `InkSurfacePatchId` - 42 edges
3. `InkArenaManager` - 40 edges
4. `InkFaceData` - 29 edges
5. `InkStorage` - 26 edges
6. `ArenaDebugRenderer` - 25 edges
7. `InkTrajectoryResult` - 24 edges
8. `InkWeaponConfig` - 21 edges
9. `FaceBasis` - 21 edges
10. `InkSurfaceKey` - 21 edges

## Surprising Connections (you probably didn't know these)
- `AGENTS.md - Splatoon Mod Development Guidelines` --references--> `Mod Icon`  [EXTRACTED]
  AGENTS.md → src/main/resources/assets/salmon/icon.png
- `AGENTS.md - Splatoon Mod Development Guidelines` --references--> `Ink Area Marker Block Texture`  [EXTRACTED]
  AGENTS.md → src/main/resources/assets/salmon/textures/block/ink_area_marker.png
- `InkWeaponConfig` --references--> `InkTrailPaintConfig`  [EXTRACTED]
  src/main/java/yam/salmon/weapon/InkWeaponConfig.java → src/main/java/yam/salmon/weapon/InkTrailPaintConfig.java
- `PaintabilityResult` --references--> `InkArena`  [EXTRACTED]
  src/main/java/yam/salmon/ink/PaintabilityResult.java → src/main/java/yam/salmon/arena/InkArena.java
- `Entry` --references--> `InkArena`  [EXTRACTED]
  src/main/java/yam/salmon/weapon/InkShotPaintTransaction.java → src/main/java/yam/salmon/arena/InkArena.java

## Import Cycles
- None detected.

## Communities (38 total, 5 thin omitted)

### Community 0 - "InkArena"
Cohesion: 0.06
Nodes (29): CommandBuildContext, CommandContext, CommandDispatcher, CommandSelection, CommandSourceStack, MinecraftServer, SavedData, SavedDataType (+21 more)

### Community 1 - "Type"
Cohesion: 0.17
Nodes (8): Override, Override, Override, Override, Type, BLOCK_HIT, ENTITY_HIT, MISS

### Community 2 - "InkSurfacePatchId"
Cohesion: 0.06
Nodes (19): ClientInkCache, BlockPos, Direction, Identifier, ClientInkSurface, BlockPos, Direction, Identifier (+11 more)

### Community 3 - "FaceBasis"
Cohesion: 0.10
Nodes (19): Axis, FaceBasis, BlockPos, Direction, Vec3, LocalUV, InkHitResolver, BlockGetter (+11 more)

### Community 4 - ".distributePaint"
Cohesion: 0.17
Nodes (14): InkPaintDistributor, BlockPos, Direction, Logger, ServerLevel, Vec3, PatchCandidate, InkPaintingService (+6 more)

### Community 5 - "ArenaDebugRenderer"
Cohesion: 0.09
Nodes (23): ArenaDebugRenderer, ArenaRenderState, Box, CachedArena, BlockPos, Draw, ExecuteInfo, Level (+15 more)

### Community 6 - "ExampleMixin.java"
Cohesion: 0.53
Nodes (4): ExampleMixin, CallbackInfo, Inject, Mixin

### Community 7 - "InkTrajectoryResult"
Cohesion: 0.06
Nodes (33): Deprecated, InkShotPaintTransaction, InkCombatService, Entity, Logger, ServerPlayer, InkShooterService, InkShooterConfig (+25 more)

### Community 8 - "Block Paintability"
Cohesion: 0.10
Nodes (21): Block, TagKey, ModBlockTags, InkPaintability, BlockGetter, BlockPos, BlockState, Direction (+13 more)

### Community 9 - "InkRenderer.java"
Cohesion: 0.09
Nodes (18): ClientInkColors, ColoredQuad, InkRenderer, InkRenderState, Draw, ExecuteInfo, LevelExtractionContext, LevelRenderContext (+10 more)

### Community 10 - "InkWeaponConfig"
Cohesion: 0.06
Nodes (27): Identifier, InteractionHand, Item, InkShooterItem, InteractionResult, Level, Logger, Override (+19 more)

### Community 11 - ".useWithoutItem"
Cohesion: 0.07
Nodes (28): Block, BlockEntityType, EntityBlock, ArenaPermission, Player, InkableBlock, BlockHitResult, BlockPos (+20 more)

### Community 12 - "InkFaceData"
Cohesion: 0.06
Nodes (29): InkFaceCoordinates, Direction, InkFaceData, IntersectionMode, CELL_CENTER, CELL_RECTANGLE_INTERSECTION, Override, InkStorage (+21 more)

### Community 13 - "ClientInkShot"
Cohesion: 0.07
Nodes (17): ClientInkShot, Vec3, ClientInkShotManager, Deprecated, Vec3, ClientInkTrailDrop, Vec3, InkShotRenderer (+9 more)

### Community 14 - "InkAreaMarkerBlockEntity"
Cohesion: 0.12
Nodes (12): BlockEntity, ClientboundBlockEntityDataPacket, CompoundTag, Nullable, Provider, InkAreaMarkerBlockEntity, BlockPos, BlockState (+4 more)

### Community 15 - "Surface Patch Extraction"
Cohesion: 0.21
Nodes (10): FaceCandidate, FaceCellUV, InkSurfacePatchExtractor, BlockGetter, BlockPos, BlockState, Direction, Logger (+2 more)

### Community 16 - "Result"
Cohesion: 0.29
Nodes (5): InkShotResult, BlockPos, Direction, Vec3, Result

### Community 17 - ".paintTrail"
Cohesion: 0.14
Nodes (16): InkCollisionRaycast, BlockHitResult, Entity, Level, Logger, Vec3, InkTrailPaintConfig, RandomSource (+8 more)

### Community 18 - "InkSyncManager.java"
Cohesion: 0.08
Nodes (13): InkPaintAccumulator, BlockPos, BlockPos, Direction, UpdatedInkSurface, InkSyncManager, BlockPos, Direction (+5 more)

### Community 19 - "InkFaceUpdatePayload"
Cohesion: 0.36
Nodes (6): InkFaceUpdatePayload, BlockPos, Direction, FriendlyByteBuf, Identifier, StreamCodec

### Community 20 - "ArenaDebugPayload"
Cohesion: 0.27
Nodes (4): ArenaDebugPayload, BlockPos, FriendlyByteBuf, StreamCodec

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

### Community 32 - "InkShotVisualPayload"
Cohesion: 0.42
Nodes (5): InkShotVisualPayload, InkTrailDropVisual, FriendlyByteBuf, StreamCodec, Vec3

### Community 33 - "Salmon.java"
Cohesion: 0.33
Nodes (6): ClientModInitializer, ModInitializer, SalmonClient, Identifier, Logger, Salmon

### Community 35 - "InkArenaClearPayload"
Cohesion: 0.36
Nodes (5): InkArenaClearPayload, FriendlyByteBuf, Identifier, Override, StreamCodec

### Community 37 - "InkSyncBeginPayload"
Cohesion: 0.36
Nodes (5): InkSyncBeginPayload, FriendlyByteBuf, Identifier, Override, StreamCodec

### Community 38 - "CustomPacketPayload"
Cohesion: 0.52
Nodes (4): CustomPacketPayload, InkSyncEndPayload, FriendlyByteBuf, StreamCodec

### Community 44 - "InkPlaneCoordinates"
Cohesion: 0.33
Nodes (5): InkPlaneCoordinates, BlockPos, Direction, Vec3, LocalUV

## Knowledge Gaps
- **27 isolated node(s):** `MISS`, `BLOCK_HIT`, `ENTITY_HIT`, `Colored`, `CELL_CENTER` (+22 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **5 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `InkSurfacePatchId` connect `InkSurfacePatchId` to `FaceBasis`, `InkFaceData`, `Surface Patch Extraction`, `InkSyncManager.java`, `InkFaceUpdatePayload`?**
  _High betweenness centrality (0.132) - this node is a cross-community bridge._
- **Why does `InkArena` connect `InkArena` to `Block Paintability`, `InkSyncManager.java`, `.distributePaint`, `InkFaceData`?**
  _High betweenness centrality (0.122) - this node is a cross-community bridge._
- **Why does `InkShotVisualPayload` connect `InkShotVisualPayload` to `Salmon.java`, `Type`, `CustomPacketPayload`, `InkWeaponConfig`, `ClientInkShot`?**
  _High betweenness centrality (0.078) - this node is a cross-community bridge._
- **What connects `MISS`, `BLOCK_HIT`, `ENTITY_HIT` to the rest of the system?**
  _27 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `InkArena` be split into smaller, more focused modules?**
  _Cohesion score 0.05690312738367658 - nodes in this community are weakly interconnected._
- **Should `InkSurfacePatchId` be split into smaller, more focused modules?**
  _Cohesion score 0.0625 - nodes in this community are weakly interconnected._
- **Should `FaceBasis` be split into smaller, more focused modules?**
  _Cohesion score 0.09568627450980392 - nodes in this community are weakly interconnected._
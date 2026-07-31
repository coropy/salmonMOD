# Graph Report - salmonMOD  (2026-07-30)

## Corpus Check
- 98 files · ~36,131 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 930 nodes · 2158 edges · 51 communities (39 shown, 12 thin omitted)
- Extraction: 92% EXTRACTED · 8% INFERRED · 0% AMBIGUOUS · INFERRED: 175 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `8b25bed3`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- InkArena
- Type
- InkSurfacePatchId
- FaceBasis
- ClientInkCache
- ArenaDebugRenderer
- ExampleMixin.java
- InkTrajectoryResult
- Block Paintability
- InkRenderer.java
- InkShooterItem.java
- .useWithoutItem
- InkFaceData
- ClientInkShot
- InkAreaMarkerBlockEntity
- Surface Patch Extraction
- Result
- InkShotEffects.java
- .distributePaint
- .extractInkState
- ArenaDebugPayload
- InkCellGeometry
- InkFaceUpdatePayload
- Mixin Example
- Paint Rules
- Ink Source Resolver
- Gradle Script
- Mod Documentation
- Setup README
- CI Workflow
- InkShotVisualPayload
- Salmon.java
- .getInstance
- InkArenaClearPayload
- .fire
- InkSyncBeginPayload
- CustomPacketPayload
- .onInitializeClient
- ClientInkSurface
- ClientInkSurfaceKey
- Level
- Override
- InkPlaneCoordinates
- Player
- Deprecated
- RandomSource
- Vec3
- Identifier
- InteractionResult

## God Nodes (most connected - your core abstractions)
1. `InkArena` - 68 edges
2. `InkSurfacePatchId` - 42 edges
3. `InkArenaManager` - 40 edges
4. `InkFaceData` - 29 edges
5. `InkStorage` - 26 edges
6. `ArenaDebugRenderer` - 25 edges
7. `InkWeaponConfig` - 21 edges
8. `FaceBasis` - 21 edges
9. `InkSurfaceKey` - 21 edges
10. `SalmonCommands` - 20 edges

## Surprising Connections (you probably didn't know these)
- `AGENTS.md - Splatoon Mod Development Guidelines` --references--> `Mod Icon`  [EXTRACTED]
  AGENTS.md → src/main/resources/assets/salmon/icon.png
- `AGENTS.md - Splatoon Mod Development Guidelines` --references--> `Ink Area Marker Block Texture`  [EXTRACTED]
  AGENTS.md → src/main/resources/assets/salmon/textures/block/ink_area_marker.png
- `InkWeaponConfig` --references--> `InkTrailPaintConfig`  [EXTRACTED]
  src/main/java/yam/salmon/weapon/InkWeaponConfig.java → src/main/java/yam/salmon/weapon/InkTrailPaintConfig.java
- `InkTrajectoryResult` --references--> `TrailPaintResult`  [EXTRACTED]
  src/main/java/yam/salmon/weapon/InkTrajectoryResult.java → src/main/java/yam/salmon/weapon/InkTrailPaintService.java
- `ClientInkCache` --references--> `ClientInkSurface`  [EXTRACTED]
  src/client/java/yam/salmon/client/ink/ClientInkCache.java → src/client/java/yam/salmon/client/ink/ClientInkSurface.java

## Import Cycles
- None detected.

## Communities (51 total, 12 thin omitted)

### Community 0 - "InkArena"
Cohesion: 0.07
Nodes (22): MinecraftServer, SavedData, SavedDataType, InkArena, BlockPos, Codec, Level, Override (+14 more)

### Community 1 - "Type"
Cohesion: 0.17
Nodes (8): Override, Override, Override, Override, Type, BLOCK_HIT, ENTITY_HIT, MISS

### Community 2 - "InkSurfacePatchId"
Cohesion: 0.14
Nodes (4): InkSurfacePatchId, Deprecated, Direction, FriendlyByteBuf

### Community 3 - "FaceBasis"
Cohesion: 0.10
Nodes (19): Axis, FaceBasis, BlockPos, Direction, Vec3, LocalUV, InkHitResolver, BlockGetter (+11 more)

### Community 4 - "ClientInkCache"
Cohesion: 0.23
Nodes (4): ClientInkCache, BlockPos, Direction, Identifier

### Community 5 - "ArenaDebugRenderer"
Cohesion: 0.09
Nodes (23): ArenaDebugRenderer, ArenaRenderState, Box, CachedArena, BlockPos, Draw, ExecuteInfo, Level (+15 more)

### Community 6 - "ExampleMixin.java"
Cohesion: 0.53
Nodes (4): ExampleMixin, CallbackInfo, Inject, Mixin

### Community 7 - "InkTrajectoryResult"
Cohesion: 0.13
Nodes (17): TrailSegment, HitType, BLOCK_HIT, ENTITY_HIT, MISS, InkTrajectoryResult, BlockPos, Direction (+9 more)

### Community 8 - "Block Paintability"
Cohesion: 0.10
Nodes (21): Block, TagKey, ModBlockTags, InkPaintability, BlockGetter, BlockPos, BlockState, Direction (+13 more)

### Community 9 - "InkRenderer.java"
Cohesion: 0.18
Nodes (11): InkRenderer, Draw, ExecuteInfo, LevelExtractionContext, LevelRenderContext, Matrix4f, Minecraft, RenderPipeline (+3 more)

### Community 10 - "InkShooterItem.java"
Cohesion: 0.07
Nodes (22): Identifier, InteractionHand, InteractionResult, Item, Level, Override, Player, InkShooterItem (+14 more)

### Community 11 - ".useWithoutItem"
Cohesion: 0.07
Nodes (28): Block, BlockEntityType, EntityBlock, ArenaPermission, Player, InkableBlock, BlockHitResult, BlockPos (+20 more)

### Community 12 - "InkFaceData"
Cohesion: 0.05
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
Cohesion: 0.30
Nodes (5): InkShotResult, BlockPos, Direction, Vec3, Result

### Community 17 - "InkShotEffects.java"
Cohesion: 0.40
Nodes (5): InkShotEffects, Direction, ServerLevel, ServerPlayer, Vec3

### Community 18 - ".distributePaint"
Cohesion: 0.10
Nodes (19): InkPaintAccumulator, BlockPos, InkPaintDistributor, BlockPos, Direction, Logger, ServerLevel, Vec3 (+11 more)

### Community 19 - ".extractInkState"
Cohesion: 0.22
Nodes (6): ClientInkColors, ColoredQuad, InkRenderState, Colored, InkCellQuad, Direction

### Community 20 - "ArenaDebugPayload"
Cohesion: 0.27
Nodes (4): ArenaDebugPayload, BlockPos, FriendlyByteBuf, StreamCodec

### Community 21 - "InkCellGeometry"
Cohesion: 0.38
Nodes (4): InkCellGeometry, BlockPos, Deprecated, Direction

### Community 22 - "InkFaceUpdatePayload"
Cohesion: 0.36
Nodes (6): InkFaceUpdatePayload, BlockPos, Direction, FriendlyByteBuf, Identifier, StreamCodec

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

### Community 34 - ".getInstance"
Cohesion: 0.12
Nodes (13): CommandBuildContext, CommandContext, CommandDispatcher, CommandSelection, CommandSourceStack, ServerPlayer, BlockPos, ServerLevel (+5 more)

### Community 35 - "InkArenaClearPayload"
Cohesion: 0.36
Nodes (5): InkArenaClearPayload, FriendlyByteBuf, Identifier, Override, StreamCodec

### Community 36 - ".fire"
Cohesion: 0.08
Nodes (27): Deprecated, Entity, InkArena, InkPaintAccumulator, RandomSource, Result, InkCombatService, Logger (+19 more)

### Community 37 - "InkSyncBeginPayload"
Cohesion: 0.36
Nodes (5): InkSyncBeginPayload, FriendlyByteBuf, Identifier, Override, StreamCodec

### Community 38 - "CustomPacketPayload"
Cohesion: 0.52
Nodes (4): CustomPacketPayload, InkSyncEndPayload, FriendlyByteBuf, StreamCodec

### Community 40 - "ClientInkSurface"
Cohesion: 0.43
Nodes (4): ClientInkSurface, BlockPos, Direction, Identifier

### Community 41 - "ClientInkSurfaceKey"
Cohesion: 0.73
Nodes (3): ClientInkSurfaceKey, BlockPos, Direction

### Community 44 - "InkPlaneCoordinates"
Cohesion: 0.33
Nodes (5): InkPlaneCoordinates, BlockPos, Direction, Vec3, LocalUV

## Knowledge Gaps
- **27 isolated node(s):** `Colored`, `CELL_CENTER`, `CELL_RECTANGLE_INTERSECTION`, `DEFAULT`, `ALLOW` (+22 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **12 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `InkSurfacePatchId` connect `InkSurfacePatchId` to `InkArena`, `FaceBasis`, `ClientInkCache`, `ClientInkSurface`, `ClientInkSurfaceKey`, `InkFaceData`, `Surface Patch Extraction`, `.distributePaint`, `InkCellGeometry`, `InkFaceUpdatePayload`?**
  _High betweenness centrality (0.137) - this node is a cross-community bridge._
- **Why does `InkArena` connect `InkArena` to `Block Paintability`, `.distributePaint`, `InkFaceData`, `.getInstance`?**
  _High betweenness centrality (0.124) - this node is a cross-community bridge._
- **Why does `InkShotVisualPayload` connect `InkShotVisualPayload` to `Salmon.java`, `ClientInkShot`, `CustomPacketPayload`, `Type`?**
  _High betweenness centrality (0.067) - this node is a cross-community bridge._
- **What connects `Colored`, `CELL_CENTER`, `CELL_RECTANGLE_INTERSECTION` to the rest of the system?**
  _27 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `InkArena` be split into smaller, more focused modules?**
  _Cohesion score 0.065684899485741 - nodes in this community are weakly interconnected._
- **Should `InkSurfacePatchId` be split into smaller, more focused modules?**
  _Cohesion score 0.13852813852813853 - nodes in this community are weakly interconnected._
- **Should `FaceBasis` be split into smaller, more focused modules?**
  _Cohesion score 0.09568627450980392 - nodes in this community are weakly interconnected._
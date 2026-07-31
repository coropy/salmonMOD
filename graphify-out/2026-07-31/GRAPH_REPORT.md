# Graph Report - salmonMOD  (2026-07-31)

## Corpus Check
- 99 files · ~36,414 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 945 nodes · 2177 edges · 46 communities (35 shown, 11 thin omitted)
- Extraction: 92% EXTRACTED · 8% INFERRED · 0% AMBIGUOUS · INFERRED: 174 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `3ecf176e`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- InkArena
- Type
- InkSurfacePatchId
- FaceBasis
- PaintFailureReason
- ArenaDebugRenderer
- ExampleMixin.java
- InkTrajectoryResult
- Block Paintability
- InkRenderer.java
- InkShooterItem.java
- .useWithoutItem
- .distributePaint
- ClientInkShot
- InkAreaMarkerBlockEntity
- Surface Patch Extraction
- Result
- InkShotEffects.java
- .paint
- Level
- ArenaDebugPayload
- BlockPos
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
- Direction
- Override
- InkPlaneCoordinates
- Player
- Deprecated
- Identifier
- InteractionResult

## God Nodes (most connected - your core abstractions)
1. `InkArena` - 68 edges
2. `InkSurfacePatchId` - 42 edges
3. `InkArenaManager` - 40 edges
4. `InkFaceData` - 29 edges
5. `InkStorage` - 26 edges
6. `ArenaDebugRenderer` - 25 edges
7. `FaceBasis` - 21 edges
8. `InkSurfaceKey` - 21 edges
9. `SalmonCommands` - 20 edges
10. `ClientInkCache` - 19 edges

## Surprising Connections (you probably didn't know these)
- `AGENTS.md - Splatoon Mod Development Guidelines` --references--> `Mod Icon`  [EXTRACTED]
  AGENTS.md → src/main/resources/assets/salmon/icon.png
- `AGENTS.md - Splatoon Mod Development Guidelines` --references--> `Ink Area Marker Block Texture`  [EXTRACTED]
  AGENTS.md → src/main/resources/assets/salmon/textures/block/ink_area_marker.png
- `PaintabilityResult` --references--> `InkArena`  [EXTRACTED]
  src/main/java/yam/salmon/ink/PaintabilityResult.java → src/main/java/yam/salmon/arena/InkArena.java
- `InkArenaManager` --references--> `InkStorage`  [EXTRACTED]
  src/main/java/yam/salmon/arena/InkArenaManager.java → src/main/java/yam/salmon/ink/InkStorage.java
- `ArenaSavedData` --references--> `InkFaceData`  [EXTRACTED]
  src/main/java/yam/salmon/arena/InkArenaManager.java → src/main/java/yam/salmon/ink/InkFaceData.java

## Import Cycles
- None detected.

## Communities (46 total, 11 thin omitted)

### Community 0 - "InkArena"
Cohesion: 0.07
Nodes (22): MinecraftServer, SavedData, SavedDataType, InkArena, BlockPos, Codec, Level, Override (+14 more)

### Community 1 - "Type"
Cohesion: 0.17
Nodes (8): Override, Override, Override, Override, Type, BLOCK_HIT, ENTITY_HIT, MISS

### Community 2 - "InkSurfacePatchId"
Cohesion: 0.08
Nodes (15): ClientInkCache, BlockPos, Direction, Identifier, ClientInkSurface, BlockPos, Direction, Identifier (+7 more)

### Community 3 - "FaceBasis"
Cohesion: 0.10
Nodes (19): Axis, FaceBasis, BlockPos, Direction, Vec3, LocalUV, InkHitResolver, BlockGetter (+11 more)

### Community 4 - "PaintFailureReason"
Cohesion: 0.14
Nodes (12): InkFaceCoordinates, Direction, PaintFailureReason, FACE_OCCLUDED, INVALID_TEAM, NO_CHANGE, NO_PERMISSION, NOT_PAINTABLE_BLOCK (+4 more)

### Community 5 - "ArenaDebugRenderer"
Cohesion: 0.09
Nodes (23): ArenaDebugRenderer, ArenaRenderState, Box, CachedArena, BlockPos, Draw, ExecuteInfo, Level (+15 more)

### Community 6 - "ExampleMixin.java"
Cohesion: 0.53
Nodes (4): ExampleMixin, CallbackInfo, Inject, Mixin

### Community 7 - "InkTrajectoryResult"
Cohesion: 0.06
Nodes (38): BlockHitResult, BlockPos, Direction, Entity, InkShotPaintTransaction, RandomSource, InkCombatService, Logger (+30 more)

### Community 8 - "Block Paintability"
Cohesion: 0.10
Nodes (21): Block, TagKey, ModBlockTags, InkPaintability, BlockGetter, BlockPos, BlockState, Direction (+13 more)

### Community 9 - "InkRenderer.java"
Cohesion: 0.07
Nodes (22): ClientInkColors, ColoredQuad, InkRenderer, InkRenderState, Draw, ExecuteInfo, LevelExtractionContext, LevelRenderContext (+14 more)

### Community 10 - "InkShooterItem.java"
Cohesion: 0.11
Nodes (17): InteractionHand, InteractionResult, Item, Override, Player, InkShooterItem, InkTrajectoryResult, InkWeaponConfig (+9 more)

### Community 11 - ".useWithoutItem"
Cohesion: 0.07
Nodes (28): Block, BlockEntityType, EntityBlock, ArenaPermission, Player, InkableBlock, BlockHitResult, BlockPos (+20 more)

### Community 12 - ".distributePaint"
Cohesion: 0.07
Nodes (24): InkFaceData, IntersectionMode, CELL_CENTER, CELL_RECTANGLE_INTERSECTION, Override, InkPaintDistributor, BlockPos, Direction (+16 more)

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

### Community 18 - ".paint"
Cohesion: 0.14
Nodes (12): InkPaintAccumulator, BlockPos, InkPaintingService, BlockPos, Direction, Logger, ServerLevel, Vec3 (+4 more)

### Community 20 - "ArenaDebugPayload"
Cohesion: 0.27
Nodes (4): ArenaDebugPayload, BlockPos, FriendlyByteBuf, StreamCodec

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
Cohesion: 0.07
Nodes (23): Deprecated, Identifier, InkArena, InkPaintAccumulator, Result, InkShooterTickHandler, InkShooterService, InkShooterConfig (+15 more)

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
- **27 isolated node(s):** `Colored`, `CELL_CENTER`, `CELL_RECTANGLE_INTERSECTION`, `DEFAULT`, `ALLOW` (+22 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **11 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `InkSurfacePatchId` connect `InkSurfacePatchId` to `InkArena`, `FaceBasis`, `InkRenderer.java`, `.distributePaint`, `Surface Patch Extraction`, `.paint`, `InkFaceUpdatePayload`?**
  _High betweenness centrality (0.134) - this node is a cross-community bridge._
- **Why does `InkArena` connect `InkArena` to `Block Paintability`, `.paint`, `.distributePaint`, `.getInstance`?**
  _High betweenness centrality (0.122) - this node is a cross-community bridge._
- **Why does `InkShotVisualPayload` connect `InkShotVisualPayload` to `Salmon.java`, `ClientInkShot`, `CustomPacketPayload`, `Type`?**
  _High betweenness centrality (0.067) - this node is a cross-community bridge._
- **What connects `Colored`, `CELL_CENTER`, `CELL_RECTANGLE_INTERSECTION` to the rest of the system?**
  _27 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `InkArena` be split into smaller, more focused modules?**
  _Cohesion score 0.06894790602655772 - nodes in this community are weakly interconnected._
- **Should `InkSurfacePatchId` be split into smaller, more focused modules?**
  _Cohesion score 0.07617051013277429 - nodes in this community are weakly interconnected._
- **Should `FaceBasis` be split into smaller, more focused modules?**
  _Cohesion score 0.09568627450980392 - nodes in this community are weakly interconnected._
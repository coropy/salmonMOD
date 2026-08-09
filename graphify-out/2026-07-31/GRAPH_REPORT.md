# Graph Report - salmonMOD  (2026-07-31)

## Corpus Check
- 107 files · ~39,341 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1062 nodes · 2615 edges · 50 communities (47 shown, 3 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 283 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `2dfbb8e4`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- .getInstance
- Type
- InkSurfacePatchId
- FaceBasis
- InkArenaManager
- ArenaDebugRenderer
- ExampleMixin.java
- InkTrajectoryResult
- .paintTrail
- InkRenderer.java
- InkWeaponConfig
- InkAreaMarkerBlock.java
- InkFaceData
- ClientInkTrailDrop
- InkAreaMarkerBlockEntity
- Surface Patch Extraction
- Result
- InkShotEffects.java
- .fire
- ActiveInkShot
- ArenaDebugPayload
- InkArena
- ClientInkShot
- Mixin Example
- Paint Rules
- Ink Source Resolver
- Gradle Script
- Mod Documentation
- Setup README
- CI Workflow
- .getArenaNumber
- Salmon.java
- InkStorage
- InkArenaClearPayload
- InkPaintabilityService.java
- CustomPacketPayload
- SalmonClient.java
- .paint
- PlayerMarkerSelectionManager
- .distributePaint
- InkFaceUpdatePayload
- InkShotImpactPayload
- InkPlaneCoordinates
- InkTrailDropImpactPayload
- PaintFailureReason
- InkTrailDropSpawnPayload
- InkSurfaceKey
- ArenaDebugSync.java

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
- `ClientInkShotManager` --references--> `ClientInkTrailDrop`  [EXTRACTED]
  src/client/java/yam/salmon/client/shot/ClientInkShotManager.java → src/client/java/yam/salmon/client/shot/ClientInkTrailDrop.java
- `ArenaCreateResult` --references--> `InkArena`  [EXTRACTED]
  src/main/java/yam/salmon/arena/InkArenaManager.java → src/main/java/yam/salmon/arena/InkArena.java
- `ArenaSavedData` --references--> `InkArena`  [EXTRACTED]
  src/main/java/yam/salmon/arena/InkArenaManager.java → src/main/java/yam/salmon/arena/InkArena.java

## Import Cycles
- None detected.

## Communities (50 total, 3 thin omitted)

### Community 0 - ".getInstance"
Cohesion: 0.16
Nodes (9): CommandBuildContext, CommandContext, CommandDispatcher, CommandSelection, CommandSourceStack, ServerPlayer, BlockPos, ServerLevel (+1 more)

### Community 1 - "Type"
Cohesion: 0.14
Nodes (9): Override, Override, Override, Override, Override, Type, BLOCK_HIT, ENTITY_HIT (+1 more)

### Community 2 - "InkSurfacePatchId"
Cohesion: 0.07
Nodes (15): ClientInkCache, BlockPos, Direction, Identifier, ClientInkSurface, BlockPos, Direction, Identifier (+7 more)

### Community 3 - "FaceBasis"
Cohesion: 0.10
Nodes (19): Axis, FaceBasis, BlockPos, Direction, Vec3, LocalUV, InkHitResolver, BlockGetter (+11 more)

### Community 4 - "InkArenaManager"
Cohesion: 0.13
Nodes (11): SavedData, SavedDataType, ArenaCreateResult, ArenaSavedData, InkArenaManager, BlockPos, Codec, Level (+3 more)

### Community 5 - "ArenaDebugRenderer"
Cohesion: 0.09
Nodes (23): ArenaDebugRenderer, ArenaRenderState, Box, CachedArena, BlockPos, Draw, ExecuteInfo, Level (+15 more)

### Community 6 - "ExampleMixin.java"
Cohesion: 0.53
Nodes (4): ExampleMixin, CallbackInfo, Inject, Mixin

### Community 7 - "InkTrajectoryResult"
Cohesion: 0.09
Nodes (23): InkCollisionRaycast, BlockHitResult, Entity, Level, Logger, Vec3, TrailSegment, HitType (+15 more)

### Community 8 - ".paintTrail"
Cohesion: 0.10
Nodes (20): InkPaintability, BlockGetter, BlockPos, BlockState, Direction, Logger, PaintabilityFailureReason, FACE_OCCLUDED (+12 more)

### Community 9 - "InkRenderer.java"
Cohesion: 0.07
Nodes (22): ClientInkColors, ColoredQuad, InkRenderer, InkRenderState, Draw, ExecuteInfo, LevelExtractionContext, LevelRenderContext (+14 more)

### Community 10 - "InkWeaponConfig"
Cohesion: 0.06
Nodes (27): InteractionHand, Item, InkShooterItem, InteractionResult, Level, Logger, Override, Player (+19 more)

### Community 11 - "InkAreaMarkerBlock.java"
Cohesion: 0.09
Nodes (23): Block, BlockEntityType, EntityBlock, ArenaPermission, Player, InkableBlock, BlockHitResult, BlockPos (+15 more)

### Community 12 - "InkFaceData"
Cohesion: 0.13
Nodes (6): InkFaceData, IntersectionMode, CELL_CENTER, CELL_RECTANGLE_INTERSECTION, Override, InkTeam

### Community 13 - "ClientInkTrailDrop"
Cohesion: 0.10
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

### Community 17 - "InkShotEffects.java"
Cohesion: 0.40
Nodes (5): InkShotEffects, Direction, ServerLevel, ServerPlayer, Vec3

### Community 18 - ".fire"
Cohesion: 0.07
Nodes (24): InkCombatService, Entity, Logger, ServerPlayer, InkPaintAccumulator, BlockPos, InkPaintingService, BlockPos (+16 more)

### Community 19 - "ActiveInkShot"
Cohesion: 0.06
Nodes (26): EntityHitResult, ActiveInkShot, Level, RandomSource, ResourceKey, Vec3, ActiveTrailDrop, Level (+18 more)

### Community 20 - "ArenaDebugPayload"
Cohesion: 0.27
Nodes (4): ArenaDebugPayload, BlockPos, FriendlyByteBuf, StreamCodec

### Community 21 - "InkArena"
Cohesion: 0.17
Nodes (6): InkArena, BlockPos, Codec, Level, Override, ResourceKey

### Community 22 - "ClientInkShot"
Cohesion: 0.12
Nodes (4): ClientInkShot, Vec3, ClientInkShotManager, Vec3

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

### Community 32 - ".getArenaNumber"
Cohesion: 0.20
Nodes (5): InkSyncManager, BlockPos, Direction, ServerLevel, ServerPlayer

### Community 33 - "Salmon.java"
Cohesion: 0.28
Nodes (8): ModInitializer, InkShotSpawnPayload, FriendlyByteBuf, StreamCodec, Vec3, Identifier, Logger, Salmon

### Community 34 - "InkStorage"
Cohesion: 0.22
Nodes (8): InkStorage, BlockPos, Codec, Direction, Logger, ServerLevel, SavedArenaInk, SavedSurface

### Community 35 - "InkArenaClearPayload"
Cohesion: 0.36
Nodes (5): InkArenaClearPayload, FriendlyByteBuf, Identifier, Override, StreamCodec

### Community 36 - "InkPaintabilityService.java"
Cohesion: 0.24
Nodes (10): Block, TagKey, ModBlockTags, InkPaintabilityService, Block, BlockPos, BlockState, Direction (+2 more)

### Community 37 - "CustomPacketPayload"
Cohesion: 0.46
Nodes (5): CustomPacketPayload, InkSyncBeginPayload, FriendlyByteBuf, Identifier, StreamCodec

### Community 38 - "SalmonClient.java"
Cohesion: 0.36
Nodes (5): ClientModInitializer, SalmonClient, InkSyncEndPayload, FriendlyByteBuf, StreamCodec

### Community 39 - ".paint"
Cohesion: 0.31
Nodes (5): InkFaceCoordinates, Direction, BlockPos, Direction, PaintResult

### Community 40 - "PlayerMarkerSelectionManager"
Cohesion: 0.26
Nodes (5): BlockPos, Level, ResourceKey, MarkerSelection, PlayerMarkerSelectionManager

### Community 41 - ".distributePaint"
Cohesion: 0.36
Nodes (7): InkPaintDistributor, BlockPos, Direction, Logger, ServerLevel, Vec3, PatchCandidate

### Community 42 - "InkFaceUpdatePayload"
Cohesion: 0.27
Nodes (7): InkFaceUpdatePayload, BlockPos, Direction, FriendlyByteBuf, Identifier, Override, StreamCodec

### Community 43 - "InkShotImpactPayload"
Cohesion: 0.36
Nodes (6): InkShotImpactPayload, Direction, FriendlyByteBuf, Override, StreamCodec, Vec3

### Community 44 - "InkPlaneCoordinates"
Cohesion: 0.33
Nodes (5): InkPlaneCoordinates, BlockPos, Direction, Vec3, LocalUV

### Community 45 - "InkTrailDropImpactPayload"
Cohesion: 0.36
Nodes (6): InkTrailDropImpactPayload, Direction, FriendlyByteBuf, Override, StreamCodec, Vec3

### Community 46 - "PaintFailureReason"
Cohesion: 0.22
Nodes (7): PaintFailureReason, FACE_OCCLUDED, INVALID_TEAM, NO_CHANGE, NO_PERMISSION, NOT_PAINTABLE_BLOCK, OUTSIDE_ARENA

### Community 47 - "InkTrailDropSpawnPayload"
Cohesion: 0.39
Nodes (5): InkTrailDropSpawnPayload, FriendlyByteBuf, Override, StreamCodec, Vec3

### Community 48 - "InkSurfaceKey"
Cohesion: 0.39
Nodes (3): InkSurfaceKey, BlockPos, Direction

### Community 49 - "ArenaDebugSync.java"
Cohesion: 0.47
Nodes (3): ArenaDebugSync, ServerLevel, ServerPlayer

## Knowledge Gaps
- **32 isolated node(s):** `Colored`, `CELL_CENTER`, `CELL_RECTANGLE_INTERSECTION`, `DEFAULT`, `ALLOW` (+27 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **3 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `InkSurfacePatchId` connect `InkSurfacePatchId` to `.getArenaNumber`, `InkStorage`, `FaceBasis`, `InkRenderer.java`, `InkFaceUpdatePayload`, `Surface Patch Extraction`, `InkSurfaceKey`, `.fire`?**
  _High betweenness centrality (0.115) - this node is a cross-community bridge._
- **Why does `InkArena` connect `InkArena` to `.getArenaNumber`, `.getInstance`, `InkStorage`, `InkArenaManager`, `.paint`, `.paintTrail`, `.distributePaint`, `ArenaDebugSync.java`, `.fire`?**
  _High betweenness centrality (0.083) - this node is a cross-community bridge._
- **Why does `InkShotSpawnPayload` connect `Salmon.java` to `Type`, `CustomPacketPayload`, `SalmonClient.java`, `ActiveInkShot`, `ClientInkShot`?**
  _High betweenness centrality (0.048) - this node is a cross-community bridge._
- **What connects `Colored`, `CELL_CENTER`, `CELL_RECTANGLE_INTERSECTION` to the rest of the system?**
  _32 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Type` be split into smaller, more focused modules?**
  _Cohesion score 0.14285714285714285 - nodes in this community are weakly interconnected._
- **Should `InkSurfacePatchId` be split into smaller, more focused modules?**
  _Cohesion score 0.07477288609364081 - nodes in this community are weakly interconnected._
- **Should `FaceBasis` be split into smaller, more focused modules?**
  _Cohesion score 0.09568627450980392 - nodes in this community are weakly interconnected._
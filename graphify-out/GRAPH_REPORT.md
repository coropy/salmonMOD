# Graph Report - salmonMOD  (2026-07-31)

## Corpus Check
- 99 files · ~37,231 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 940 nodes · 2253 edges · 48 communities (43 shown, 5 thin omitted)
- Extraction: 92% EXTRACTED · 8% INFERRED · 0% AMBIGUOUS · INFERRED: 188 edges (avg confidence: 0.8)
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
- InkArenaManager
- InkTrajectoryResult
- Block Paintability
- InkRenderer.java
- InkWeaponConfig
- InkAreaMarkerBlock.java
- InkFaceData
- ClientInkShot
- InkAreaMarkerBlockEntity
- Surface Patch Extraction
- Result
- .paintTrail
- .fire
- InkFaceUpdatePayload
- ArenaDebugPayload
- ClientInkCache
- InkStorage
- Mixin Example
- Paint Rules
- Ink Source Resolver
- Gradle Script
- Mod Documentation
- Setup README
- CI Workflow
- InkShotVisualPayload
- Salmon.java
- .paint
- InkArenaClearPayload
- .extractInkState
- CustomPacketPayload
- InkSyncEndPayload
- PlayerMarkerSelectionManager
- InkCellGeometry
- PaintFailureReason
- InkSurfaceKey
- ClientInkSurface
- InkPlaneCoordinates
- .onInitializeClient
- ClientInkSurfaceKey
- .sendFullSync

## God Nodes (most connected - your core abstractions)
1. `InkArena` - 71 edges
2. `InkSurfacePatchId` - 42 edges
3. `InkArenaManager` - 40 edges
4. `InkFaceData` - 29 edges
5. `InkTrajectoryResult` - 27 edges
6. `InkStorage` - 26 edges
7. `ArenaDebugRenderer` - 25 edges
8. `FaceBasis` - 21 edges
9. `InkSurfaceKey` - 21 edges
10. `InkWeaponConfig` - 21 edges

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

## Communities (48 total, 5 thin omitted)

### Community 0 - "InkArena"
Cohesion: 0.07
Nodes (22): CommandBuildContext, CommandContext, CommandDispatcher, CommandSelection, CommandSourceStack, InkArena, BlockPos, Codec (+14 more)

### Community 1 - "Type"
Cohesion: 0.17
Nodes (8): Override, Override, Override, Override, Type, BLOCK_HIT, ENTITY_HIT, MISS

### Community 2 - "InkSurfacePatchId"
Cohesion: 0.14
Nodes (4): InkSurfacePatchId, Deprecated, Direction, FriendlyByteBuf

### Community 3 - "FaceBasis"
Cohesion: 0.10
Nodes (19): Axis, FaceBasis, BlockPos, Direction, Vec3, LocalUV, InkHitResolver, BlockGetter (+11 more)

### Community 4 - ".distributePaint"
Cohesion: 0.36
Nodes (7): InkPaintDistributor, BlockPos, Direction, Logger, ServerLevel, Vec3, PatchCandidate

### Community 5 - "ArenaDebugRenderer"
Cohesion: 0.09
Nodes (23): ArenaDebugRenderer, ArenaRenderState, Box, CachedArena, BlockPos, Draw, ExecuteInfo, Level (+15 more)

### Community 6 - "InkArenaManager"
Cohesion: 0.11
Nodes (15): MinecraftServer, SavedData, SavedDataType, ArenaCreateResult, ArenaSavedData, InkArenaManager, BlockPos, Codec (+7 more)

### Community 7 - "InkTrajectoryResult"
Cohesion: 0.13
Nodes (18): TrailSegment, HitType, BLOCK_HIT, ENTITY_HIT, MISS, InkTrajectoryResult, BlockPos, Deprecated (+10 more)

### Community 8 - "Block Paintability"
Cohesion: 0.10
Nodes (21): Block, TagKey, ModBlockTags, InkPaintability, BlockGetter, BlockPos, BlockState, Direction (+13 more)

### Community 9 - "InkRenderer.java"
Cohesion: 0.20
Nodes (11): InkRenderer, Draw, ExecuteInfo, LevelExtractionContext, LevelRenderContext, Matrix4f, Minecraft, RenderPipeline (+3 more)

### Community 10 - "InkWeaponConfig"
Cohesion: 0.06
Nodes (23): InteractionHand, Item, InkShooterItem, InteractionResult, Level, Logger, Override, Player (+15 more)

### Community 11 - "InkAreaMarkerBlock.java"
Cohesion: 0.09
Nodes (23): Block, BlockEntityType, EntityBlock, ArenaPermission, Player, InkableBlock, BlockHitResult, BlockPos (+15 more)

### Community 12 - "InkFaceData"
Cohesion: 0.15
Nodes (6): InkFaceData, IntersectionMode, CELL_CENTER, CELL_RECTANGLE_INTERSECTION, Override, InkTeam

### Community 13 - "ClientInkShot"
Cohesion: 0.06
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

### Community 17 - ".paintTrail"
Cohesion: 0.12
Nodes (18): InkCollisionRaycast, BlockHitResult, Entity, Level, Logger, Vec3, InkTrailPaintConfig, RandomSource (+10 more)

### Community 18 - ".fire"
Cohesion: 0.06
Nodes (29): InkCombatService, Entity, Logger, ServerPlayer, InkPaintAccumulator, BlockPos, InkPaintingService, BlockPos (+21 more)

### Community 19 - "InkFaceUpdatePayload"
Cohesion: 0.36
Nodes (6): InkFaceUpdatePayload, BlockPos, Direction, FriendlyByteBuf, Identifier, StreamCodec

### Community 20 - "ArenaDebugPayload"
Cohesion: 0.27
Nodes (4): ArenaDebugPayload, BlockPos, FriendlyByteBuf, StreamCodec

### Community 21 - "ClientInkCache"
Cohesion: 0.20
Nodes (4): ClientInkCache, BlockPos, Direction, Identifier

### Community 22 - "InkStorage"
Cohesion: 0.20
Nodes (8): InkStorage, BlockPos, Codec, Direction, Logger, ServerLevel, SavedArenaInk, SavedSurface

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

### Community 34 - ".paint"
Cohesion: 0.31
Nodes (5): InkFaceCoordinates, Direction, BlockPos, Direction, PaintResult

### Community 35 - "InkArenaClearPayload"
Cohesion: 0.36
Nodes (5): InkArenaClearPayload, FriendlyByteBuf, Identifier, Override, StreamCodec

### Community 36 - ".extractInkState"
Cohesion: 0.22
Nodes (6): ClientInkColors, ColoredQuad, InkRenderState, Colored, InkCellQuad, Direction

### Community 37 - "CustomPacketPayload"
Cohesion: 0.46
Nodes (5): CustomPacketPayload, InkSyncBeginPayload, FriendlyByteBuf, Identifier, StreamCodec

### Community 38 - "InkSyncEndPayload"
Cohesion: 0.39
Nodes (4): InkSyncEndPayload, FriendlyByteBuf, Override, StreamCodec

### Community 39 - "PlayerMarkerSelectionManager"
Cohesion: 0.26
Nodes (5): BlockPos, Level, ResourceKey, MarkerSelection, PlayerMarkerSelectionManager

### Community 40 - "InkCellGeometry"
Cohesion: 0.38
Nodes (4): InkCellGeometry, BlockPos, Deprecated, Direction

### Community 41 - "PaintFailureReason"
Cohesion: 0.22
Nodes (7): PaintFailureReason, FACE_OCCLUDED, INVALID_TEAM, NO_CHANGE, NO_PERMISSION, NOT_PAINTABLE_BLOCK, OUTSIDE_ARENA

### Community 42 - "InkSurfaceKey"
Cohesion: 0.39
Nodes (3): InkSurfaceKey, BlockPos, Direction

### Community 43 - "ClientInkSurface"
Cohesion: 0.43
Nodes (4): ClientInkSurface, BlockPos, Direction, Identifier

### Community 44 - "InkPlaneCoordinates"
Cohesion: 0.33
Nodes (5): InkPlaneCoordinates, BlockPos, Direction, Vec3, LocalUV

### Community 46 - "ClientInkSurfaceKey"
Cohesion: 0.73
Nodes (3): ClientInkSurfaceKey, BlockPos, Direction

## Knowledge Gaps
- **26 isolated node(s):** `Colored`, `CELL_CENTER`, `CELL_RECTANGLE_INTERSECTION`, `DEFAULT`, `ALLOW` (+21 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **5 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `InkSurfacePatchId` connect `InkSurfacePatchId` to `InkArena`, `FaceBasis`, `InkCellGeometry`, `InkSurfaceKey`, `ClientInkSurface`, `ClientInkSurfaceKey`, `Surface Patch Extraction`, `.fire`, `InkFaceUpdatePayload`, `ClientInkCache`, `InkStorage`?**
  _High betweenness centrality (0.129) - this node is a cross-community bridge._
- **Why does `InkArena` connect `InkArena` to `.paint`, `.distributePaint`, `InkArenaManager`, `Block Paintability`, `.fire`, `InkStorage`?**
  _High betweenness centrality (0.117) - this node is a cross-community bridge._
- **Why does `InkShotVisualPayload` connect `InkShotVisualPayload` to `Salmon.java`, `Type`, `CustomPacketPayload`, `InkWeaponConfig`, `ClientInkShot`?**
  _High betweenness centrality (0.074) - this node is a cross-community bridge._
- **What connects `Colored`, `CELL_CENTER`, `CELL_RECTANGLE_INTERSECTION` to the rest of the system?**
  _26 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `InkArena` be split into smaller, more focused modules?**
  _Cohesion score 0.07091136079900125 - nodes in this community are weakly interconnected._
- **Should `InkSurfacePatchId` be split into smaller, more focused modules?**
  _Cohesion score 0.14285714285714285 - nodes in this community are weakly interconnected._
- **Should `FaceBasis` be split into smaller, more focused modules?**
  _Cohesion score 0.09568627450980392 - nodes in this community are weakly interconnected._
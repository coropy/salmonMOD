# Graph Report - salmonMOD  (2026-07-29)

## Corpus Check
- 97 files · ~35,710 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 935 nodes · 2102 edges · 63 communities (45 shown, 18 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 148 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `c9a933ab`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- InkArena
- Type
- InkSurfacePatchId
- FaceBasis
- ArenaDebugPayload
- ArenaDebugRenderer
- ExampleMixin.java
- .simulate
- Block Paintability
- InkRenderer.java
- InkShooterItem.java
- InkAreaMarkerBlock.java
- InkFaceData
- ClientInkShot
- InkAreaMarkerBlockEntity
- Surface Patch Extraction
- Result
- InkShotEffects.java
- InkSyncManager.java
- InkFaceUpdatePayload
- InkShotVisualPayload
- Salmon.java
- InkArenaClearPayload
- Mixin Example
- Paint Rules
- Ink Source Resolver
- Gradle Script
- Mod Documentation
- Setup README
- CI Workflow
- InkSyncBeginPayload
- CustomPacketPayload
- SalmonCommands
- InkStorage
- .fire
- PaintFailureReason
- .paint
- PlayerMarkerSelectionManager
- InkTrajectoryResult
- .distributePaint
- .paintTrail
- InkPaintAccumulator
- InkPlaneCoordinates
- InkSurfaceKey
- InkCombatService.java
- HitType
- SalmonClient.java
- ExecuteInfo
- LevelRenderContext
- Matrix4f
- Matrix4fc
- RenderPipeline
- StagedVertexBuffer
- Vector3f
- Vector4f
- VertexConsumer
- InteractionResult
- Level
- Player
- FriendlyByteBuf
- StreamCodec

## God Nodes (most connected - your core abstractions)
1. `InkArena` - 59 edges
2. `InkArenaManager` - 40 edges
3. `InkSurfacePatchId` - 39 edges
4. `InkFaceData` - 27 edges
5. `InkTrajectoryResult` - 25 edges
6. `ArenaDebugRenderer` - 25 edges
7. `InkStorage` - 24 edges
8. `InkWeaponConfig` - 21 edges
9. `FaceBasis` - 21 edges
10. `SalmonCommands` - 20 edges

## Surprising Connections (you probably didn't know these)
- `AGENTS.md - Splatoon Mod Development Guidelines` --references--> `Mod Icon`  [EXTRACTED]
  AGENTS.md → src/main/resources/assets/salmon/icon.png
- `AGENTS.md - Splatoon Mod Development Guidelines` --references--> `Ink Area Marker Block Texture`  [EXTRACTED]
  AGENTS.md → src/main/resources/assets/salmon/textures/block/ink_area_marker.png
- `InkTrajectoryResult` --references--> `TrailSegment`  [EXTRACTED]
  src/main/java/yam/salmon/weapon/InkTrajectoryResult.java → src/main/java/yam/salmon/weapon/InkTrailPaintService.java
- `InkTrajectoryResult` --references--> `TrailPaintResult`  [EXTRACTED]
  src/main/java/yam/salmon/weapon/InkTrajectoryResult.java → src/main/java/yam/salmon/weapon/InkTrailPaintService.java
- `PaintabilityResult` --references--> `InkArena`  [EXTRACTED]
  src/main/java/yam/salmon/ink/PaintabilityResult.java → src/main/java/yam/salmon/arena/InkArena.java

## Import Cycles
- None detected.

## Communities (63 total, 18 thin omitted)

### Community 0 - "InkArena"
Cohesion: 0.08
Nodes (20): MinecraftServer, SavedData, SavedDataType, InkArena, BlockPos, Codec, Level, Override (+12 more)

### Community 1 - "Type"
Cohesion: 0.17
Nodes (8): Override, Override, Override, Override, Type, BLOCK_HIT, ENTITY_HIT, MISS

### Community 2 - "InkSurfacePatchId"
Cohesion: 0.08
Nodes (15): ClientInkCache, BlockPos, Direction, Identifier, ClientInkSurface, BlockPos, Direction, Identifier (+7 more)

### Community 3 - "FaceBasis"
Cohesion: 0.10
Nodes (19): Axis, FaceBasis, BlockPos, Direction, Vec3, LocalUV, InkHitResolver, BlockGetter (+11 more)

### Community 4 - "ArenaDebugPayload"
Cohesion: 0.27
Nodes (4): ArenaDebugPayload, BlockPos, FriendlyByteBuf, StreamCodec

### Community 5 - "ArenaDebugRenderer"
Cohesion: 0.09
Nodes (23): ArenaDebugRenderer, ArenaRenderState, Box, CachedArena, BlockPos, Draw, ExecuteInfo, Level (+15 more)

### Community 6 - "ExampleMixin.java"
Cohesion: 0.53
Nodes (4): ExampleMixin, CallbackInfo, Inject, Mixin

### Community 7 - ".simulate"
Cohesion: 0.28
Nodes (8): TrailSegment, InkTrajectorySimulator, BlockPos, Direction, Logger, ServerLevel, ServerPlayer, Vec3

### Community 8 - "Block Paintability"
Cohesion: 0.10
Nodes (21): Block, TagKey, ModBlockTags, InkPaintability, BlockGetter, BlockPos, BlockState, Direction (+13 more)

### Community 9 - "InkRenderer.java"
Cohesion: 0.08
Nodes (21): ClientInkColors, ColoredQuad, InkRenderer, InkRenderState, Draw, ExecuteInfo, LevelExtractionContext, LevelRenderContext (+13 more)

### Community 10 - "InkShooterItem.java"
Cohesion: 0.07
Nodes (22): InteractionHand, InteractionResult, Item, Level, Player, InkShooterItem, Logger, Override (+14 more)

### Community 11 - "InkAreaMarkerBlock.java"
Cohesion: 0.09
Nodes (23): Block, BlockEntityType, EntityBlock, ArenaPermission, Player, InkableBlock, BlockHitResult, BlockPos (+15 more)

### Community 12 - "InkFaceData"
Cohesion: 0.13
Nodes (6): InkFaceData, IntersectionMode, CELL_CENTER, CELL_RECTANGLE_INTERSECTION, Override, InkTeam

### Community 13 - "ClientInkShot"
Cohesion: 0.06
Nodes (18): ExecuteInfo, LevelRenderContext, Matrix4f, Matrix4fc, RenderPipeline, Override, ClientInkShot, Vec3 (+10 more)

### Community 14 - "InkAreaMarkerBlockEntity"
Cohesion: 0.12
Nodes (12): BlockEntity, ClientboundBlockEntityDataPacket, CompoundTag, Nullable, Provider, InkAreaMarkerBlockEntity, BlockPos, BlockState (+4 more)

### Community 15 - "Surface Patch Extraction"
Cohesion: 0.21
Nodes (10): FaceCandidate, FaceCellUV, InkSurfacePatchExtractor, BlockGetter, BlockPos, BlockState, Direction, Logger (+2 more)

### Community 16 - "Result"
Cohesion: 0.29
Nodes (5): InkShotResult, BlockPos, Direction, Vec3, Result

### Community 17 - "InkShotEffects.java"
Cohesion: 0.40
Nodes (5): InkShotEffects, Direction, ServerLevel, ServerPlayer, Vec3

### Community 18 - "InkSyncManager.java"
Cohesion: 0.11
Nodes (20): InkFaceData, InkStorage, InkSurfaceKey, InkPaintingService, BlockPos, Direction, InkArena, Logger (+12 more)

### Community 19 - "InkFaceUpdatePayload"
Cohesion: 0.36
Nodes (6): InkFaceUpdatePayload, BlockPos, Direction, FriendlyByteBuf, Identifier, StreamCodec

### Community 20 - "InkShotVisualPayload"
Cohesion: 0.29
Nodes (7): FriendlyByteBuf, InkShotVisualPayload, InkTrailDropVisual, Override, Vec3, StreamCodec, Type

### Community 21 - "Salmon.java"
Cohesion: 0.53
Nodes (4): ModInitializer, Identifier, Logger, Salmon

### Community 22 - "InkArenaClearPayload"
Cohesion: 0.36
Nodes (5): InkArenaClearPayload, FriendlyByteBuf, Identifier, Override, StreamCodec

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

### Community 32 - "InkSyncBeginPayload"
Cohesion: 0.52
Nodes (4): InkSyncBeginPayload, FriendlyByteBuf, Identifier, StreamCodec

### Community 33 - "CustomPacketPayload"
Cohesion: 0.52
Nodes (4): CustomPacketPayload, InkSyncEndPayload, FriendlyByteBuf, StreamCodec

### Community 34 - "SalmonCommands"
Cohesion: 0.16
Nodes (9): CommandBuildContext, CommandContext, CommandDispatcher, CommandSelection, CommandSourceStack, ServerPlayer, BlockPos, ServerLevel (+1 more)

### Community 35 - "InkStorage"
Cohesion: 0.21
Nodes (8): InkStorage, BlockPos, Codec, Direction, Logger, ServerLevel, SavedArenaInk, SavedSurface

### Community 36 - ".fire"
Cohesion: 0.18
Nodes (9): InkShooterService, Deprecated, InkArena, InkShooterConfig, Logger, Result, ServerLevel, ServerPlayer (+1 more)

### Community 37 - "PaintFailureReason"
Cohesion: 0.16
Nodes (11): BlockPos, Direction, MultiSurfacePaintResult, UpdatedInkSurface, PaintFailureReason, FACE_OCCLUDED, INVALID_TEAM, NO_CHANGE (+3 more)

### Community 38 - ".paint"
Cohesion: 0.31
Nodes (5): InkFaceCoordinates, Direction, BlockPos, Direction, PaintResult

### Community 39 - "PlayerMarkerSelectionManager"
Cohesion: 0.26
Nodes (5): BlockPos, Level, ResourceKey, MarkerSelection, PlayerMarkerSelectionManager

### Community 40 - "InkTrajectoryResult"
Cohesion: 0.26
Nodes (5): InkTrajectoryResult, BlockPos, Direction, Result, Vec3

### Community 41 - ".distributePaint"
Cohesion: 0.36
Nodes (7): InkPaintDistributor, BlockPos, Direction, Logger, ServerLevel, Vec3, PatchCandidate

### Community 42 - ".paintTrail"
Cohesion: 0.33
Nodes (8): RandomSource, InkTrailPaintService, Logger, ServerLevel, ServerPlayer, Vec3, TrailDropVisual, TrailPaintResult

### Community 43 - "InkPaintAccumulator"
Cohesion: 0.29
Nodes (5): InkPaintAccumulator, BlockPos, InkSurfacePatchId, MultiSurfacePaintResult, UpdatedInkSurface

### Community 44 - "InkPlaneCoordinates"
Cohesion: 0.33
Nodes (5): InkPlaneCoordinates, BlockPos, Direction, Vec3, LocalUV

### Community 45 - "InkSurfaceKey"
Cohesion: 0.29
Nodes (3): InkSurfaceKey, BlockPos, Direction

### Community 46 - "InkCombatService.java"
Cohesion: 0.43
Nodes (4): Entity, InkCombatService, Logger, ServerPlayer

### Community 47 - "HitType"
Cohesion: 0.50
Nodes (4): HitType, BLOCK_HIT, ENTITY_HIT, MISS

## Knowledge Gaps
- **27 isolated node(s):** `MISS`, `BLOCK_HIT`, `ENTITY_HIT`, `Colored`, `CELL_CENTER` (+22 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **18 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `InkSurfacePatchId` connect `InkSurfacePatchId` to `FaceBasis`, `InkStorage`, `InkRenderer.java`, `InkSurfaceKey`, `Surface Patch Extraction`, `InkFaceUpdatePayload`?**
  _High betweenness centrality (0.124) - this node is a cross-community bridge._
- **Why does `InkArena` connect `InkArena` to `Block Paintability`, `.distributePaint`, `InkStorage`, `.paint`?**
  _High betweenness centrality (0.105) - this node is a cross-community bridge._
- **Why does `InkShotVisualPayload` connect `InkShotVisualPayload` to `CustomPacketPayload`, `InkShooterItem.java`, `ClientInkShot`, `SalmonClient.java`, `Salmon.java`?**
  _High betweenness centrality (0.066) - this node is a cross-community bridge._
- **What connects `MISS`, `BLOCK_HIT`, `ENTITY_HIT` to the rest of the system?**
  _27 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `InkArena` be split into smaller, more focused modules?**
  _Cohesion score 0.0757314974182444 - nodes in this community are weakly interconnected._
- **Should `InkSurfacePatchId` be split into smaller, more focused modules?**
  _Cohesion score 0.07692307692307693 - nodes in this community are weakly interconnected._
- **Should `FaceBasis` be split into smaller, more focused modules?**
  _Cohesion score 0.09568627450980392 - nodes in this community are weakly interconnected._
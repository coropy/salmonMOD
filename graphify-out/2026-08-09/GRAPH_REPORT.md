# Graph Report - salmonMOD  (2026-07-31)

## Corpus Check
- 107 files · ~39,425 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1078 nodes · 2571 edges · 53 communities (41 shown, 12 thin omitted)
- Extraction: 90% EXTRACTED · 10% INFERRED · 0% AMBIGUOUS · INFERRED: 269 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `16eaa6e4`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- .getInstance
- .paintInto
- InkSurfacePatchId
- FaceBasis
- InkArenaManager
- ArenaDebugRenderer
- ExampleMixin.java
- InkTrajectoryResult
- .checkPaintable
- InkRenderer.java
- InkShooterItem.java
- .useWithoutItem
- InkFaceData
- ClientInkShot
- InkAreaMarkerBlockEntity
- Surface Patch Extraction
- ArenaDebugPayload
- Salmon.java
- .fire
- ActiveInkShot
- InteractionResult
- InkArena
- SalmonClient.java
- Mixin Example
- Paint Rules
- Ink Source Resolver
- Gradle Script
- Mod Documentation
- Setup README
- CI Workflow
- .getArenaId
- InkShotSpawnPayload
- InkStorage
- InkArenaClearPayload
- Player
- InkSyncBeginPayload
- CustomPacketPayload
- .paint
- Identifier
- .distributePaint
- RandomSource
- InkShotImpactPayload
- InkPlaneCoordinates
- InkTrailDropImpactPayload
- PaintFailureReason
- InkTrailDropSpawnPayload
- InkSurfaceKey
- BlockHitResult
- MinecraftServer
- Nullable
- ServerLevel

## God Nodes (most connected - your core abstractions)
1. `InkArena` - 71 edges
2. `ActiveInkShot` - 42 edges
3. `InkSurfacePatchId` - 42 edges
4. `InkArenaManager` - 40 edges
5. `ActiveTrailDrop` - 31 edges
6. `InkFaceData` - 29 edges
7. `InkStorage` - 26 edges
8. `InkProjectileLifecycleManager` - 25 edges
9. `ArenaDebugRenderer` - 25 edges
10. `InkTrajectoryResult` - 22 edges

## Surprising Connections (you probably didn't know these)
- `AGENTS.md - Splatoon Mod Development Guidelines` --references--> `Mod Icon`  [EXTRACTED]
  AGENTS.md → src/main/resources/assets/salmon/icon.png
- `AGENTS.md - Splatoon Mod Development Guidelines` --references--> `Ink Area Marker Block Texture`  [EXTRACTED]
  AGENTS.md → src/main/resources/assets/salmon/textures/block/ink_area_marker.png
- `InkShotImpactPayload` --references--> `ProjectileFinishReason`  [EXTRACTED]
  src/main/java/yam/salmon/network/InkShotImpactPayload.java → src/main/java/yam/salmon/weapon/ProjectileFinishReason.java
- `InkTrailDropImpactPayload` --references--> `ProjectileFinishReason`  [EXTRACTED]
  src/main/java/yam/salmon/network/InkTrailDropImpactPayload.java → src/main/java/yam/salmon/weapon/ProjectileFinishReason.java
- `ArenaCreateResult` --references--> `InkArena`  [EXTRACTED]
  src/main/java/yam/salmon/arena/InkArenaManager.java → src/main/java/yam/salmon/arena/InkArena.java

## Import Cycles
- None detected.

## Communities (53 total, 12 thin omitted)

### Community 0 - ".getInstance"
Cohesion: 0.14
Nodes (11): CommandBuildContext, CommandContext, CommandDispatcher, CommandSelection, CommandSourceStack, ServerPlayer, BlockPos, ServerLevel (+3 more)

### Community 1 - ".paintInto"
Cohesion: 0.21
Nodes (10): InkPaintingService, BlockPos, Direction, Logger, ServerLevel, Vec3, BlockPos, Direction (+2 more)

### Community 2 - "InkSurfacePatchId"
Cohesion: 0.06
Nodes (22): ClientInkCache, BlockPos, Direction, Identifier, ClientInkSurface, BlockPos, Direction, Identifier (+14 more)

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
Cohesion: 0.05
Nodes (38): InkShooterTickHandler, InkCollisionRaycast, BlockHitResult, Entity, Level, Logger, Vec3, InkTrailPaintConfig (+30 more)

### Community 8 - ".checkPaintable"
Cohesion: 0.10
Nodes (21): Block, TagKey, ModBlockTags, InkPaintability, BlockGetter, BlockPos, BlockState, Direction (+13 more)

### Community 9 - "InkRenderer.java"
Cohesion: 0.08
Nodes (21): ClientInkColors, ColoredQuad, InkRenderer, InkRenderState, Draw, ExecuteInfo, LevelExtractionContext, LevelRenderContext (+13 more)

### Community 10 - "InkShooterItem.java"
Cohesion: 0.11
Nodes (16): InkTrajectoryResult, InteractionHand, InteractionResult, Item, Player, InkShooterItem, InkWeaponConfig, Level (+8 more)

### Community 11 - ".useWithoutItem"
Cohesion: 0.07
Nodes (28): Block, BlockEntityType, EntityBlock, ArenaPermission, Player, InkableBlock, BlockHitResult, BlockPos (+20 more)

### Community 12 - "InkFaceData"
Cohesion: 0.14
Nodes (6): InkFaceData, IntersectionMode, CELL_CENTER, CELL_RECTANGLE_INTERSECTION, Override, InkTeam

### Community 13 - "ClientInkShot"
Cohesion: 0.05
Nodes (17): Override, ClientInkShot, Vec3, ClientInkShotManager, Vec3, ClientInkTrailDrop, Vec3, InkShotRenderer (+9 more)

### Community 14 - "InkAreaMarkerBlockEntity"
Cohesion: 0.12
Nodes (12): BlockEntity, ClientboundBlockEntityDataPacket, CompoundTag, Provider, InkAreaMarkerBlockEntity, BlockPos, BlockState, Nullable (+4 more)

### Community 15 - "Surface Patch Extraction"
Cohesion: 0.21
Nodes (10): FaceCandidate, FaceCellUV, InkSurfacePatchExtractor, BlockGetter, BlockPos, BlockState, Direction, Logger (+2 more)

### Community 16 - "ArenaDebugPayload"
Cohesion: 0.07
Nodes (22): ArenaDebugPayload, BlockPos, FriendlyByteBuf, Override, StreamCodec, InkShotVisualPayload, InkTrailDropVisual, FriendlyByteBuf (+14 more)

### Community 17 - "Salmon.java"
Cohesion: 0.31
Nodes (5): Identifier, ModInitializer, Logger, Override, Salmon

### Community 18 - ".fire"
Cohesion: 0.07
Nodes (20): InkCombatService, Entity, Logger, ServerPlayer, InkPaintAccumulator, BlockPos, InkShooterConfig, InkShooterService (+12 more)

### Community 19 - "ActiveInkShot"
Cohesion: 0.06
Nodes (28): BlockHitResult, EntityHitResult, MinecraftServer, Nullable, RandomSource, ServerLevel, ActiveInkShot, InkWeaponConfig (+20 more)

### Community 21 - "InkArena"
Cohesion: 0.17
Nodes (7): InkArena, BlockPos, Codec, Level, Override, ResourceKey, ServerPlayer

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

### Community 32 - ".getArenaId"
Cohesion: 0.20
Nodes (5): InkSyncManager, BlockPos, Direction, ServerLevel, ServerPlayer

### Community 33 - "InkShotSpawnPayload"
Cohesion: 0.36
Nodes (6): InkShotSpawnPayload, FriendlyByteBuf, Override, StreamCodec, Type, Vec3

### Community 34 - "InkStorage"
Cohesion: 0.20
Nodes (8): InkStorage, BlockPos, Codec, Direction, Logger, ServerLevel, SavedArenaInk, SavedSurface

### Community 35 - "InkArenaClearPayload"
Cohesion: 0.36
Nodes (5): InkArenaClearPayload, FriendlyByteBuf, Identifier, Override, StreamCodec

### Community 37 - "InkSyncBeginPayload"
Cohesion: 0.52
Nodes (4): InkSyncBeginPayload, FriendlyByteBuf, Identifier, StreamCodec

### Community 38 - "CustomPacketPayload"
Cohesion: 0.52
Nodes (4): CustomPacketPayload, InkSyncEndPayload, FriendlyByteBuf, StreamCodec

### Community 39 - ".paint"
Cohesion: 0.31
Nodes (5): InkFaceCoordinates, Direction, BlockPos, Direction, PaintResult

### Community 41 - ".distributePaint"
Cohesion: 0.29
Nodes (7): InkPaintDistributor, BlockPos, Direction, Logger, ServerLevel, Vec3, PatchCandidate

### Community 43 - "InkShotImpactPayload"
Cohesion: 0.33
Nodes (7): InkShotImpactPayload, Direction, FriendlyByteBuf, Override, StreamCodec, Type, Vec3

### Community 44 - "InkPlaneCoordinates"
Cohesion: 0.33
Nodes (5): InkPlaneCoordinates, BlockPos, Direction, Vec3, LocalUV

### Community 45 - "InkTrailDropImpactPayload"
Cohesion: 0.33
Nodes (7): InkTrailDropImpactPayload, Direction, FriendlyByteBuf, Override, StreamCodec, Type, Vec3

### Community 46 - "PaintFailureReason"
Cohesion: 0.22
Nodes (7): PaintFailureReason, FACE_OCCLUDED, INVALID_TEAM, NO_CHANGE, NO_PERMISSION, NOT_PAINTABLE_BLOCK, OUTSIDE_ARENA

### Community 47 - "InkTrailDropSpawnPayload"
Cohesion: 0.36
Nodes (6): InkTrailDropSpawnPayload, FriendlyByteBuf, Override, StreamCodec, Type, Vec3

### Community 48 - "InkSurfaceKey"
Cohesion: 0.48
Nodes (3): InkSurfaceKey, BlockPos, Direction

## Knowledge Gaps
- **32 isolated node(s):** `ALIVE`, `BLOCK_HIT`, `ENTITY_HIT`, `SAFETY_TIMEOUT`, `OUT_OF_WORLD` (+27 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **12 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `InkSurfacePatchId` connect `InkSurfacePatchId` to `.getArenaId`, `InkStorage`, `FaceBasis`, `InkRenderer.java`, `Surface Patch Extraction`, `InkSurfaceKey`, `.fire`?**
  _High betweenness centrality (0.126) - this node is a cross-community bridge._
- **Why does `InkArena` connect `InkArena` to `.getArenaId`, `.paintInto`, `InkStorage`, `.getInstance`, `InkArenaManager`, `.paint`, `.checkPaintable`, `.distributePaint`, `.fire`?**
  _High betweenness centrality (0.098) - this node is a cross-community bridge._
- **Why does `InkShotSpawnPayload` connect `InkShotSpawnPayload` to `CustomPacketPayload`, `ClientInkShot`, `Salmon.java`, `ActiveInkShot`, `SalmonClient.java`?**
  _High betweenness centrality (0.048) - this node is a cross-community bridge._
- **What connects `ALIVE`, `BLOCK_HIT`, `ENTITY_HIT` to the rest of the system?**
  _32 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `.getInstance` be split into smaller, more focused modules?**
  _Cohesion score 0.1379800853485064 - nodes in this community are weakly interconnected._
- **Should `InkSurfacePatchId` be split into smaller, more focused modules?**
  _Cohesion score 0.061057692307692306 - nodes in this community are weakly interconnected._
- **Should `FaceBasis` be split into smaller, more focused modules?**
  _Cohesion score 0.09568627450980392 - nodes in this community are weakly interconnected._
# Graph Report - salmonMOD  (2026-07-31)

## Corpus Check
- 99 files · ~36,528 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 924 nodes · 2199 edges · 35 communities (32 shown, 3 thin omitted)
- Extraction: 91% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 187 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `04fc0aa6`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- InkArena
- Type
- InkSurfacePatchId
- .distributePaint
- InkArenaManager
- ArenaDebugRenderer
- ExampleMixin.java
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
- InkShotEffects.java
- .fire
- ArenaDebugPayload
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
8. `FaceBasis` - 21 edges
9. `InkSurfaceKey` - 21 edges
10. `InkWeaponConfig` - 21 edges

## Surprising Connections (you probably didn't know these)
- `AGENTS.md - Splatoon Mod Development Guidelines` --references--> `Mod Icon`  [EXTRACTED]
  AGENTS.md → src/main/resources/assets/salmon/icon.png
- `AGENTS.md - Splatoon Mod Development Guidelines` --references--> `Ink Area Marker Block Texture`  [EXTRACTED]
  AGENTS.md → src/main/resources/assets/salmon/textures/block/ink_area_marker.png
- `ArenaCreateResult` --references--> `InkArena`  [EXTRACTED]
  src/main/java/yam/salmon/arena/InkArenaManager.java → src/main/java/yam/salmon/arena/InkArena.java
- `ArenaSavedData` --references--> `InkArena`  [EXTRACTED]
  src/main/java/yam/salmon/arena/InkArenaManager.java → src/main/java/yam/salmon/arena/InkArena.java
- `InkArenaManager` --references--> `InkArena`  [EXTRACTED]
  src/main/java/yam/salmon/arena/InkArenaManager.java → src/main/java/yam/salmon/arena/InkArena.java

## Import Cycles
- None detected.

## Communities (35 total, 3 thin omitted)

### Community 0 - "InkArena"
Cohesion: 0.07
Nodes (23): CommandBuildContext, CommandContext, CommandDispatcher, CommandSelection, CommandSourceStack, InkArena, BlockPos, Codec (+15 more)

### Community 1 - "Type"
Cohesion: 0.17
Nodes (8): Override, Override, Override, Override, Type, BLOCK_HIT, ENTITY_HIT, MISS

### Community 2 - "InkSurfacePatchId"
Cohesion: 0.05
Nodes (24): ClientInkCache, BlockPos, Direction, Identifier, ClientInkSurface, BlockPos, Direction, Identifier (+16 more)

### Community 3 - ".distributePaint"
Cohesion: 0.08
Nodes (26): Axis, FaceBasis, BlockPos, Direction, Vec3, LocalUV, InkHitResolver, BlockGetter (+18 more)

### Community 4 - "InkArenaManager"
Cohesion: 0.12
Nodes (11): MinecraftServer, SavedData, SavedDataType, ArenaCreateResult, ArenaSavedData, InkArenaManager, BlockPos, Codec (+3 more)

### Community 5 - "ArenaDebugRenderer"
Cohesion: 0.09
Nodes (23): ArenaDebugRenderer, ArenaRenderState, Box, CachedArena, BlockPos, Draw, ExecuteInfo, Level (+15 more)

### Community 6 - "ExampleMixin.java"
Cohesion: 0.53
Nodes (4): ExampleMixin, CallbackInfo, Inject, Mixin

### Community 7 - "InkTrajectoryResult"
Cohesion: 0.07
Nodes (32): InkCollisionRaycast, BlockHitResult, Entity, Level, Logger, Vec3, InkTrailPaintConfig, RandomSource (+24 more)

### Community 8 - "Block Paintability"
Cohesion: 0.10
Nodes (21): Block, TagKey, ModBlockTags, InkPaintability, BlockGetter, BlockPos, BlockState, Direction (+13 more)

### Community 9 - "InkRenderer.java"
Cohesion: 0.09
Nodes (18): ClientInkColors, ColoredQuad, InkRenderer, InkRenderState, Draw, ExecuteInfo, LevelExtractionContext, LevelRenderContext (+10 more)

### Community 10 - "InkWeaponConfig"
Cohesion: 0.06
Nodes (22): InteractionHand, Item, InkShooterItem, InteractionResult, Level, Logger, Override, Player (+14 more)

### Community 11 - "InkAreaMarkerBlock.java"
Cohesion: 0.07
Nodes (28): Block, BlockEntityType, EntityBlock, ArenaPermission, Player, InkableBlock, BlockHitResult, BlockPos (+20 more)

### Community 12 - "InkFaceData"
Cohesion: 0.07
Nodes (23): InkFaceCoordinates, Direction, InkFaceData, IntersectionMode, CELL_CENTER, CELL_RECTANGLE_INTERSECTION, Override, InkStorage (+15 more)

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

### Community 18 - ".fire"
Cohesion: 0.06
Nodes (31): InkCombatService, Entity, Logger, ServerPlayer, InkPaintAccumulator, BlockPos, InkPaintingService, BlockPos (+23 more)

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
- **26 isolated node(s):** `Colored`, `CELL_CENTER`, `CELL_RECTANGLE_INTERSECTION`, `DEFAULT`, `ALLOW` (+21 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **3 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `InkSurfacePatchId` connect `InkSurfacePatchId` to `InkArena`, `.distributePaint`, `InkFaceData`, `Surface Patch Extraction`, `.fire`?**
  _High betweenness centrality (0.132) - this node is a cross-community bridge._
- **Why does `InkArena` connect `InkArena` to `.distributePaint`, `InkArenaManager`, `Block Paintability`, `InkFaceData`, `.fire`?**
  _High betweenness centrality (0.119) - this node is a cross-community bridge._
- **Why does `InkShotVisualPayload` connect `InkShotVisualPayload` to `Salmon.java`, `Type`, `CustomPacketPayload`, `InkWeaponConfig`, `ClientInkShot`?**
  _High betweenness centrality (0.074) - this node is a cross-community bridge._
- **What connects `Colored`, `CELL_CENTER`, `CELL_RECTANGLE_INTERSECTION` to the rest of the system?**
  _26 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `InkArena` be split into smaller, more focused modules?**
  _Cohesion score 0.06837606837606838 - nodes in this community are weakly interconnected._
- **Should `InkSurfacePatchId` be split into smaller, more focused modules?**
  _Cohesion score 0.05368382080710848 - nodes in this community are weakly interconnected._
- **Should `.distributePaint` be split into smaller, more focused modules?**
  _Cohesion score 0.07834101382488479 - nodes in this community are weakly interconnected._
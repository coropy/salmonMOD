# Graph Report - salmonMOD  (2026-07-30)

## Corpus Check
- 98 files · ~36,125 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 915 nodes · 2185 edges · 35 communities (31 shown, 4 thin omitted)
- Extraction: 91% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 187 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `74b5c676`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- InkArena
- Salmon.java
- InkSurfacePatchId
- .distributePaint
- InkShooterItem
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
- InkShotEffects.java
- InkSyncManager.java
- InkVisualColorResolver.java
- Mixin Example
- Paint Rules
- Ink Source Resolver
- Gradle Script
- Mod Documentation
- Setup README
- CI Workflow
- .getInstance
- .fire
- PlayerMarkerSelectionManager
- InkPlaneCoordinates
- InkCombatService.java
- InkShooterItem.java

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
- `PaintabilityResult` --references--> `InkArena`  [EXTRACTED]
  src/main/java/yam/salmon/ink/PaintabilityResult.java → src/main/java/yam/salmon/arena/InkArena.java
- `Entry` --references--> `InkArena`  [EXTRACTED]
  src/main/java/yam/salmon/weapon/InkShotPaintTransaction.java → src/main/java/yam/salmon/arena/InkArena.java
- `InkArenaManager` --references--> `InkStorage`  [EXTRACTED]
  src/main/java/yam/salmon/arena/InkArenaManager.java → src/main/java/yam/salmon/ink/InkStorage.java

## Import Cycles
- None detected.

## Communities (35 total, 4 thin omitted)

### Community 0 - "InkArena"
Cohesion: 0.06
Nodes (30): EntityBlock, MinecraftServer, SavedData, SavedDataType, InkArena, BlockPos, Codec, Level (+22 more)

### Community 1 - "Salmon.java"
Cohesion: 0.06
Nodes (39): ClientModInitializer, CustomPacketPayload, ModInitializer, SalmonClient, Override, InkArenaClearPayload, FriendlyByteBuf, Identifier (+31 more)

### Community 2 - "InkSurfacePatchId"
Cohesion: 0.06
Nodes (19): ClientInkCache, BlockPos, Direction, Identifier, ClientInkSurface, BlockPos, Direction, Identifier (+11 more)

### Community 3 - ".distributePaint"
Cohesion: 0.08
Nodes (26): Axis, FaceBasis, BlockPos, Direction, Vec3, LocalUV, InkHitResolver, BlockGetter (+18 more)

### Community 4 - "InkShooterItem"
Cohesion: 0.27
Nodes (4): Item, InkShooterItem, ServerPlayer, ModItems

### Community 5 - "ArenaDebugRenderer"
Cohesion: 0.09
Nodes (23): ArenaDebugRenderer, ArenaRenderState, Box, CachedArena, BlockPos, Draw, ExecuteInfo, Level (+15 more)

### Community 6 - "ExampleMixin.java"
Cohesion: 0.53
Nodes (4): ExampleMixin, CallbackInfo, Inject, Mixin

### Community 7 - "InkTrajectoryResult"
Cohesion: 0.09
Nodes (26): InkTrailPaintConfig, RandomSource, InkTrailPaintService, Logger, ServerLevel, ServerPlayer, Vec3, TrailDropVisual (+18 more)

### Community 8 - "Block Paintability"
Cohesion: 0.10
Nodes (21): Block, TagKey, ModBlockTags, InkPaintability, BlockGetter, BlockPos, BlockState, Direction (+13 more)

### Community 9 - "InkRenderer.java"
Cohesion: 0.09
Nodes (18): ClientInkColors, ColoredQuad, InkRenderer, InkRenderState, Draw, ExecuteInfo, LevelExtractionContext, LevelRenderContext (+10 more)

### Community 10 - "InkWeaponConfig"
Cohesion: 0.18
Nodes (6): InkShooterTickHandler, InkWeaponConfig, Identifier, InkWeaponRegistry, Identifier, Logger

### Community 11 - ".useWithoutItem"
Cohesion: 0.15
Nodes (13): Block, BlockEntityType, ArenaPermission, Player, InkableBlock, BlockHitResult, BlockPos, BlockState (+5 more)

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

### Community 18 - "InkSyncManager.java"
Cohesion: 0.08
Nodes (18): InkPaintAccumulator, BlockPos, InkPaintingService, BlockPos, Direction, Logger, ServerLevel, Vec3 (+10 more)

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

### Community 34 - ".getInstance"
Cohesion: 0.09
Nodes (14): CommandBuildContext, CommandContext, CommandDispatcher, CommandSelection, CommandSourceStack, ServerPlayer, BlockPos, ServerLevel (+6 more)

### Community 36 - ".fire"
Cohesion: 0.18
Nodes (8): InkShooterConfig, InkShooterService, Deprecated, Logger, ServerLevel, ServerPlayer, Entry, InkShotPaintTransaction

### Community 39 - "PlayerMarkerSelectionManager"
Cohesion: 0.26
Nodes (5): BlockPos, Level, ResourceKey, MarkerSelection, PlayerMarkerSelectionManager

### Community 44 - "InkPlaneCoordinates"
Cohesion: 0.33
Nodes (5): InkPlaneCoordinates, BlockPos, Direction, Vec3, LocalUV

### Community 46 - "InkCombatService.java"
Cohesion: 0.43
Nodes (4): Entity, InkCombatService, Logger, ServerPlayer

### Community 58 - "InkShooterItem.java"
Cohesion: 0.24
Nodes (8): InteractionHand, InteractionResult, Level, Logger, Override, Player, InkShooterVisualConfig, UseOnContext

## Knowledge Gaps
- **26 isolated node(s):** `Colored`, `CELL_CENTER`, `CELL_RECTANGLE_INTERSECTION`, `DEFAULT`, `ALLOW` (+21 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **4 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `InkSurfacePatchId` connect `InkSurfacePatchId` to `Salmon.java`, `.distributePaint`, `InkFaceData`, `Surface Patch Extraction`, `InkSyncManager.java`?**
  _High betweenness centrality (0.133) - this node is a cross-community bridge._
- **Why does `InkArena` connect `InkArena` to `.distributePaint`, `.fire`, `Block Paintability`, `InkFaceData`, `InkSyncManager.java`?**
  _High betweenness centrality (0.121) - this node is a cross-community bridge._
- **Why does `InkShotVisualPayload` connect `Salmon.java` to `InkShooterItem.java`, `InkShooterItem`, `ClientInkShot`?**
  _High betweenness centrality (0.075) - this node is a cross-community bridge._
- **What connects `Colored`, `CELL_CENTER`, `CELL_RECTANGLE_INTERSECTION` to the rest of the system?**
  _26 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `InkArena` be split into smaller, more focused modules?**
  _Cohesion score 0.06403508771929825 - nodes in this community are weakly interconnected._
- **Should `Salmon.java` be split into smaller, more focused modules?**
  _Cohesion score 0.05780885780885781 - nodes in this community are weakly interconnected._
- **Should `InkSurfacePatchId` be split into smaller, more focused modules?**
  _Cohesion score 0.061072261072261075 - nodes in this community are weakly interconnected._
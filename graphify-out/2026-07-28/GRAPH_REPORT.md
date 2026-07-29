# Graph Report - C:\dev\salmonMOD  (2026-07-28)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 814 nodes · 1915 edges · 32 communities (28 shown, 4 thin omitted)
- Extraction: 91% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 163 edges (avg confidence: 0.8)
- Token cost: 1,541 input · 1,272 output

## Graph Freshness
- Built from commit: `71035095`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Arena Entity Data
- Client Mod Init
- Client Ink Cache
- Face Basis Math
- Salmon Commands
- Arena Debug Render
- Arena Manager
- Combat Services
- Block Paintability
- Client Ink Colors
- Shooter Item
- Arena Permissions
- Ink Storage
- Client Ink Shot
- Area Marker Block
- Surface Patch Extraction
- Ink Face Data
- Ink Shot Renderer
- Paint Results
- Face Coordinates
- Paint Distributor
- Plane Coordinates
- Ink Teams
- Mixin Example
- Paint Rules
- Ink Source Resolver
- Gradle Script
- Mod Documentation
- Setup README
- CI Workflow

## God Nodes (most connected - your core abstractions)
1. `InkArena` - 66 edges
2. `InkSurfacePatchId` - 41 edges
3. `InkArenaManager` - 40 edges
4. `InkFaceData` - 29 edges
5. `ArenaDebugRenderer` - 25 edges
6. `InkStorage` - 25 edges
7. `FaceBasis` - 21 edges
8. `SalmonCommands` - 20 edges
9. `InkSurfaceKey` - 20 edges
10. `ClientInkCache` - 19 edges

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

## Communities (32 total, 4 thin omitted)

### Community 0 - "Arena Entity Data"
Cohesion: 0.07
Nodes (29): EntityBlock, InkArena, BlockPos, Codec, Level, Override, ResourceKey, InkAreaMarkerBlock (+21 more)

### Community 1 - "Client Mod Init"
Cohesion: 0.06
Nodes (38): ClientModInitializer, CustomPacketPayload, ModInitializer, SalmonClient, Override, InkArenaClearPayload, FriendlyByteBuf, Identifier (+30 more)

### Community 2 - "Client Ink Cache"
Cohesion: 0.06
Nodes (19): ClientInkCache, BlockPos, Direction, Identifier, ClientInkSurface, BlockPos, Direction, Identifier (+11 more)

### Community 3 - "Face Basis Math"
Cohesion: 0.10
Nodes (19): Axis, FaceBasis, BlockPos, Direction, Vec3, LocalUV, InkHitResolver, BlockGetter (+11 more)

### Community 4 - "Salmon Commands"
Cohesion: 0.09
Nodes (14): CommandBuildContext, CommandContext, CommandDispatcher, CommandSelection, CommandSourceStack, ServerPlayer, BlockPos, ServerLevel (+6 more)

### Community 5 - "Arena Debug Render"
Cohesion: 0.09
Nodes (23): ArenaDebugRenderer, ArenaRenderState, Box, CachedArena, BlockPos, Draw, ExecuteInfo, Level (+15 more)

### Community 6 - "Arena Manager"
Cohesion: 0.11
Nodes (15): MinecraftServer, SavedData, SavedDataType, ArenaCreateResult, ArenaSavedData, InkArenaManager, BlockPos, Codec (+7 more)

### Community 7 - "Combat Services"
Cohesion: 0.10
Nodes (20): Entity, RandomSource, InkCombatService, Logger, ServerPlayer, InkShooterService, Direction, Logger (+12 more)

### Community 8 - "Block Paintability"
Cohesion: 0.10
Nodes (21): Block, TagKey, ModBlockTags, InkPaintability, BlockGetter, BlockPos, BlockState, Direction (+13 more)

### Community 9 - "Client Ink Colors"
Cohesion: 0.09
Nodes (18): ClientInkColors, ColoredQuad, InkRenderer, InkRenderState, Draw, ExecuteInfo, LevelExtractionContext, LevelRenderContext (+10 more)

### Community 10 - "Shooter Item"
Cohesion: 0.11
Nodes (15): InteractionHand, Item, InkShooterItem, InteractionResult, Level, Logger, Override, Player (+7 more)

### Community 11 - "Arena Permissions"
Cohesion: 0.10
Nodes (19): Block, BlockEntityType, ArenaPermission, Player, InkableBlock, BlockHitResult, BlockPos, BlockState (+11 more)

### Community 12 - "Ink Storage"
Cohesion: 0.14
Nodes (11): InkStorage, BlockPos, Codec, Direction, Logger, ServerLevel, SavedArenaInk, SavedSurface (+3 more)

### Community 13 - "Client Ink Shot"
Cohesion: 0.11
Nodes (5): ClientInkShot, Vec3, ClientInkShotManager, Vec3, InkShooterVisualConfig

### Community 14 - "Area Marker Block"
Cohesion: 0.12
Nodes (12): BlockEntity, ClientboundBlockEntityDataPacket, CompoundTag, Nullable, Provider, InkAreaMarkerBlockEntity, BlockPos, BlockState (+4 more)

### Community 15 - "Surface Patch Extraction"
Cohesion: 0.21
Nodes (10): FaceCandidate, FaceCellUV, InkSurfacePatchExtractor, BlockGetter, BlockPos, BlockState, Direction, Logger (+2 more)

### Community 16 - "Ink Face Data"
Cohesion: 0.15
Nodes (5): InkFaceData, IntersectionMode, CELL_CENTER, CELL_RECTANGLE_INTERSECTION, Override

### Community 17 - "Ink Shot Renderer"
Cohesion: 0.22
Nodes (10): InkShotRenderer, ExecuteInfo, LevelRenderContext, Matrix4f, Matrix4fc, RenderPipeline, StagedVertexBuffer, Vector3f (+2 more)

### Community 18 - "Paint Results"
Cohesion: 0.17
Nodes (11): BlockPos, Direction, MultiSurfacePaintResult, UpdatedInkSurface, PaintFailureReason, FACE_OCCLUDED, INVALID_TEAM, NO_CHANGE (+3 more)

### Community 19 - "Face Coordinates"
Cohesion: 0.31
Nodes (5): InkFaceCoordinates, Direction, BlockPos, Direction, PaintResult

### Community 20 - "Paint Distributor"
Cohesion: 0.36
Nodes (7): InkPaintDistributor, BlockPos, Direction, Logger, ServerLevel, Vec3, PatchCandidate

### Community 21 - "Plane Coordinates"
Cohesion: 0.33
Nodes (5): InkPlaneCoordinates, BlockPos, Direction, Vec3, LocalUV

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

## Knowledge Gaps
- **23 isolated node(s):** `Colored`, `CELL_CENTER`, `CELL_RECTANGLE_INTERSECTION`, `DEFAULT`, `ALLOW` (+18 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **4 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `InkSurfacePatchId` connect `Client Ink Cache` to `Arena Entity Data`, `Client Mod Init`, `Face Basis Math`, `Ink Storage`, `Surface Patch Extraction`?**
  _High betweenness centrality (0.151) - this node is a cross-community bridge._
- **Why does `InkArena` connect `Arena Entity Data` to `Arena Manager`, `Block Paintability`, `Arena Permissions`, `Ink Storage`, `Face Coordinates`, `Paint Distributor`?**
  _High betweenness centrality (0.129) - this node is a cross-community bridge._
- **Why does `ArenaDebugRenderer` connect `Arena Debug Render` to `Client Ink Colors`?**
  _High betweenness centrality (0.042) - this node is a cross-community bridge._
- **What connects `Colored`, `CELL_CENTER`, `CELL_RECTANGLE_INTERSECTION` to the rest of the system?**
  _23 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Arena Entity Data` be split into smaller, more focused modules?**
  _Cohesion score 0.06523655598001764 - nodes in this community are weakly interconnected._
- **Should `Client Mod Init` be split into smaller, more focused modules?**
  _Cohesion score 0.05673076923076923 - nodes in this community are weakly interconnected._
- **Should `Client Ink Cache` be split into smaller, more focused modules?**
  _Cohesion score 0.06451612903225806 - nodes in this community are weakly interconnected._
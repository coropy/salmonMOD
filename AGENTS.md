# AGENTS.md - スプラトゥーンMod (salmon) 開発ガイドライン

## 1. プロジェクト概要 & 開発環境

Minecraft Java Edition 上でスプラトゥーンを再現するFabric Modプロジェクトです。

| 項目 | 値 |
|---|---|
| **Minecraft Version** | 26.2 |
| **Fabric Loader** | 0.19.3 |
| **Fabric API** | 0.155.2+26.2 |
| **Java Version** | 25 (Loom 1.17.17-SNAPSHOT) |
| **Mappings** | Mojang Official (intermediary) |
| **Mod ID** | `salmon` |
| **Main Package** | `yam.salmon` |

---

## 2. ディレクトリ構造と主要クラスの役割

AIはコードを検索・修正する際、以下の配置ルールと役割を必ず参考にしてください。

### 2.1. サーバー側 (`src/main/java/yam/salmon/`)

#### `Salmon.java`
ModInitializer。ブロック/BlockEntityの登録、イベントリスナー（切断/破壊/次元移動時のデバッグ再同期等）の定義。`id(String)` で `Identifier` を生成。ネットワークPayload登録。`ServerPlayConnectionEvents.JOIN` で初回参加時のインク完全同期、`ServerPlayerEvents.AFTER_RESPAWN` でリスポーン/次元移動時の再同期。

#### `block/` — ブロック関連
| クラス | 役割 |
|---|---|
| `ModBlocks.java` | ブロック・BlockItem・BlockEntityTypeのレジストリ登録。`register()` 内で `ResourceKey<Block>`, `ResourceKey<Item>` を生成し、`Item.Properties#setId(itemKey)` + `useBlockDescriptionPrefix()` で BlockItem を構築。`INK_AREA_MARKER_BLOCK_ENTITY` は register() 内でブロック登録後に初期化。`INKABLE_BLOCK` の登録も `registerInkableBlock()` で行う。 |
| `InkAreaMarkerBlock.java` | アリーナ範囲選択用マーカーブロック（右クリック処理等）。コンストラクタで `Properties.setId(ResourceKey<Block>)` 必須（26.2）。アリーナ作成成功時に `ArenaDebugSync.broadcastArenaAdded` で同次元のデバッグ有効プレイヤーに即時通知。 |
| `InkAreaMarkerBlockEntity.java` | マーカーのNBTデータ保持・ペア管理、破壊時アリーナ削除。削除の冪等性保証、ペアマーカーの所属情報クリア、ロード時孤立参照自己修復、破壊時に `ArenaDebugSync.broadcastArenaRemoved` でデバッグ表示同期を実装。 |
| `InkableBlock.java` | 塗装可能ブロック。BlockEntity なし。Phase 5: `InkPaintDistributor.distributePaint()` を使用し、3D球ブラシで複数ブロック・複数方向面（90度角含む）への塗装に対応。Shift+右クリックで Team B、通常右クリックで Team A で塗装。失敗時・変更なし時も適切なメッセージを表示。塗装成功時に `InkSyncManager.broadcastMultiFaceUpdate()` で複数面を一括同期。 |
| `ModBlockTags.java` | ブロックタグ定数クラス。`INK_PAINTABLE`（salmon:ink_paintable）、`MINEABLE_PICKAXE`（minecraft:mineable/pickaxe）を定義。 |

#### `arena/` — アリーナ管理ロジック
| クラス | 役割 |
|---|---|
| `InkArena.java` | アリーナのデータモデル（範囲計算・包含判定・重複判定）。`arenaNumber` フィールドを持ち、Codec でシリアライズ（optional、デフォルト0で後方互換）。 |
| `InkArenaManager.java` | ワールドごとのアリーナ永続化管理 (`SavedData`)。作成・削除（boolean戻り値で冪等性保証）・検索・存在確認 (`arenaExists`)・番号割当 (`allocateArenaNumber`)・番号検索 (`getArenaByNumber`)・V1→V2 データ移行 (`migrateFromV1`)。`InkStorage` のインクデータロード・保存も担当（dataVersion=4）。`saveInkDataNow()` で即時保存。アリーナ削除時にインクデータも自動削除。デバッグ表示のプレイヤー単位ON/OFF管理も担当。dataVersion=3 の古いPatch座標系データは安全に破棄。 |
| `ArenaPermission.java` | クリエイティブ+OP権限判定ロジック（`canConfigure`）を一元管理。 |

#### `ink/` — インク塗装システム
| クラス | 役割 |
|---|---|
| `InkPaintability.java` | 塗装可能判定のユーティリティクラス（Phase 7）。`checkPaintable(level, pos, face, arena)` でタグ→アリーナ→面露出の3段階判定。`isPaintableBlock()` は階段・ハーフブロックを `VoxelShape` ベースで許可。水没ブロックも許可（液体そのものは不可）。`isSurfaceExposed()` はMC 26.2の `getOcclusionShape()` 使用。 |
| `PaintabilityResult.java` | 塗装可能判定結果レコード（Phase 1）。 |
| `PaintabilityFailureReason.java` | 塗装不能理由の列挙型（Phase 1）。 |
| `InkTeam.java` | インクチーム定数クラス。`NONE=0`, `TEAM_A=1`, `TEAM_B=2`。`toChar()`, `toName()`, `isValidTeam()`, `normalize()` を提供。 |
| `InkSurfacePatchId.java` | ブロック内部の矩形表面パッチID（Phase 7）。1ブロック=16units固定分解能。`normal`, `plane`, `minU`, `minV`, `maxU`, `maxV`。`fullFace(Direction)` でフル面パッチ生成。`writeToBuffer()`/`readFromBuffer()` でネットワークシリアライズ。 |
| `InkSurfacePatch.java` | 矩形表面パッチ（Phase 7）。`InkSurfacePatchId` + `BlockPos`。`toWorldPoint()`, `fromWorld()`, `projectOntoPatch()`, `getWorldBounds()` を提供。`fullFace(pos, face)` でフル面パッチ生成。 |
| `InkSurfacePatchExtractor.java` | VoxelShapeから外部露出パッチを抽出（Phase 7）。`extract(state, level, pos)` でパッチ一覧取得。内部面除去・同一平面矩形統合・BlockState単位キャッシュ。`isFullCube()` でダブルハーフブロックもフルキューブ扱い。 |
| `InkSurfaceKey.java` | 塗装面識別レコード（Phase 7）。`BlockPos` + `InkSurfacePatchId`。`fullFace(pos, face)` 互換ファクトリ、`getNormal()` ショートカット。 |
| `InkFaceData.java` | 1パッチ面の8×8インクグリッドデータ。`GRID_SIZE=8`, `CELL_COUNT=64`。`getCell()`, `setCell()`, `paintCircle()`, `isEmpty()`, `clear()`, `copyCells()`, `toString()` を提供。 |
| `InkFaceCoordinates.java` | ブロック面へのヒット座標から UV座標・セル座標への変換ユーティリティ。`fromHit(face, localX, localY, localZ)` で6面対応のUV変換。u==1.0, v==1.0 境界の clamp 処理済み。 |
| `InkPlaneCoordinates.java` | ワールド座標から連続的な平面座標（planeU, planeV）への変換（Phase 4）。6面ごとの定義 + `toLocalUV()` で各候補ブロック面のローカルUVへ変換。 |
| `FaceBasis.java` | ブロック面のローカル座標系定義（Phase 7）。6面の法線・U軸・V軸をレコードで保持。`toWorldPoint()`, `fromWorld()`, `projectOntoFace()`, `sphereIntersectsFace()`, `distanceToPlane()`/`signedDistanceToPlane()` に加え、Patch用の `toWorldPointRaw()`, `getBoundsFromUV()`, `getPlaneAxis()`, `sphereIntersectsPatchRect()`, `projectOntoFaceAtCoord()`, `distanceToPatchPlane()` を追加。 |
| `InkPaintDistributor.java` | 塗装分配エンジン（Phase 5+7）。3D球AABB内の全候補BlockPos×6面を走査し、`FaceBasis.sphereIntersectsFace()` で球と面の交差判定 → 露出判定 → 面上投影 → 円塗装 の流れで分配。フルブロック限定。`MAX_PAINT_RADIUS_BLOCKS=8.0`。Phase 7: `InkSurfaceKey` は `fullFace()` 互換生成。 |
| `MultiSurfacePaintResult.java` | 複数面塗装結果レコード（Phase 4）。`success`, `changedSurfaceCount`, `changedCellCount`, `updatedSurfaces` を保持。`UpdatedInkSurface` 内包レコード。 |
| `InkCellGeometry.java` | ブロック面の8×8グリッドセルのワールド座標AABBを計算するユーティリティ（Phase 3）。`getCellBounds(blockPos, face, cellU, cellV)` で6面対応。`SURFACE_OFFSET=0.002`、`INK_THICKNESS=0.008`。 |
| `PaintResult.java` | 塗装操作結果レコード（1面用 / Phase 2互換）。 |
| `PaintFailureReason.java` | 塗装操作失敗理由の列挙型。 |
| `InkStorage.java` | アリーナごとのインクデータ管理ストレージ（Phase 7）。疎な保存方式（`Map<UUID, Map<InkSurfaceKey, InkFaceData>>`）。`paint()`（1面塗装互換）、`getFace()`（PatchId/Direction両対応）、`clearArena()`、`removeArena()`、`getRawArenaMap()`。`importData()` はマージ方式。保存形式 dataVersion=4: `SavedSurface` に新5フィールドで PatchId 永続化。旧 dataVersion=3 データは fullFace にフォールバック。 |
| `InkPaintingService.java` | インク塗装の単一エントリポイント（Phase 6+）。`InkPaintDistributor` に委譲し、永続化保存と `InkSyncManager` による複数面同期を統一的に実行。 |
| `InkSourceResolver.java` | インクのオーナーUUID解決（Phase 6+）。現状はプレイヤー自身のUUIDを返す。将来的なチーム/色ベース所有権のプレースホルダ。 |

#### `item/` — アイテム登録
| クラス | 役割 |
|---|---|
| `ModItems.java` | アイテムのレジストリ登録（Phase 6+）。`ink_shooter`（InkShooterItem）を登録。 |
| `InkShooterItem.java` | インクシューターブキアイテム（Phase 6+）。右クリックで発射開始、レート制限・プレイヤー単位のクールダウン管理、軌道ビジュアルPayloadの近傍クライアント配信。 |
| `InkShooterTickHandler.java` | サーバーTickハンドラ（Phase 6+）。使用キー押下中のプレイヤーに対して継続的に `InkShooterService` を呼び出し、連射を実現。 |

#### `weapon/` — ブキシステム
| クラス | 役割 |
|---|---|
| `InkShooterService.java` | コア発射ロジック（Phase 6+）。設定可能な射程・拡散・重力・速度でブロック/エンティティレイキャストを実行。ブロックヒット時は `InkPaintingService` で塗装、エンティティヒット時は `InkCombatService` でダメージ適用。トレイル塗装の統合・アキュムレータ一括コミットを含む。 |
| `InkShooterConfig.java` | シューターのパラメータ設定（射程、拡散角、重力、速度、ダメージ等）。 |
| `InkShotResult.java` | 1発の結果レコード。ヒット種別（ブロック/エンティティ/なし）、ヒット位置、対象エンティティ、塗装結果を保持。 |
| `InkShotEffects.java` | 発射時の効果（パーティクル、サウンド等）を管理。 |
| `InkShooterVisualConfig.java` | クライアント側ビジュアル用設定（色、サイズ、弧の高さ等）。 |
| `InkVisualColorResolver.java` | プレイヤー/チームに応じたインク色の解決。 |
| `InkWeaponConfig.java` | 武器ごとの全パラメータ設定レコード（Phase 6+）。`InkTrailPaintConfig` を内包。 |
| `InkWeaponRegistry.java` | 武器設定のレジストリ管理。 |
| `InkTrajectoryResult.java` | 軌道シミュレーション結果レコード。substep線分（`trailSegments`）とトレイル塗装結果（`trailPaintResult`）を含む。 |
| `InkTrajectorySimulator.java` | 放物線軌道シミュレーター。substep線分を収集し `TrailSegment` リストを出力。 |
| `InkTrailPaintConfig.java` | トレイル塗装設定レコード。`sampleSpacing`, `downwardRange`, `paintRadius`, `horizontalJitter`, `paintChance` 等（Phase 8）。3種のプリセット（STANDARD/SHORT_RANGE/LONG_RANGE）+ DISABLED。 |
| `InkTrailPaintService.java` | トレイル塗装サービス。軌道substep線分から距離ベースで滴サンプル位置を決定し、下方向レイキャスト→塗装分配→アキュムレータ蓄積（Phase 8）。 |

#### `combat/` — 戦闘システム
| クラス | 役割 |
|---|---|
| `InkCombatService.java` | インク武器のダメージ適用（Phase 6+）。自己攻撃不可・無敵・死亡・攻撃可能判定をチェック後、プレイヤー攻撃ダメージを対象エンティティに適用。 |

#### `command/` — コマンド登録
| クラス | 役割 |
|---|---|
| `SalmonCommands.java` | `/salmon arena list|info|here|remove|debug`、`/salmon ink inspect|clear <arenaNumber>|debug on|off|status` の全コマンド実装。ページング、削除前表示、デバッグブロードキャスト、インク面8×8グリッド表示、インククリア同期、インクデバッグ制御を含む。 |

#### `network/` — ネットワーク同期
| クラス | 役割 |
|---|---|
| `ArenaDebugPayload.java` | `CustomPacketPayload` 実装。FULL_SYNC/ADD/REMOVE/CLEAR の4アクション。完全同期開始(-1)/終了(-2)マーカー付き。 |
| `ArenaDebugSync.java` | サーバー→クライアント同期。`sendFullSync`（個人向け完全同期）、`sendArenaAdded`/`sendArenaRemoved`（個人向け差分）、`broadcastArenaAdded`/`broadcastArenaRemoved`（同次元全デバッグ有効プレイヤー向けブロードキャスト）、`sendClear`（クリア指示）。 |
| `InkFaceUpdatePayload.java` | インク1面更新ペイロード（Phase 3）。arenaUuid, arenaNumber, dimensionId, blockPos, faceName, cells[64], revision。 |
| `InkSyncBeginPayload.java` | インク完全同期開始ペイロード（Phase 3）。dimensionId, sessionId, faceCount, revision。 |
| `InkSyncEndPayload.java` | インク完全同期終了ペイロード（Phase 3）。sessionId, faceCount。 |
| `InkArenaClearPayload.java` | アリーナインク全消去ペイロード（Phase 3）。arenaUuid, arenaNumber, dimensionId, revision。 |
| `InkSyncManager.java` | インクデータ同期マネージャー（Phase 3+4）。アリーナ単位のlongリビジョン管理。`broadcastFaceUpdate()`（1面差分）、`broadcastMultiFaceUpdate()`（複数面一括 / Phase 4）、`broadcastArenaClear()`（クリアブロードキャスト）、`broadcastArenaRemoved()`（アリーナ削除ブロードキャスト）、`sendFullSync()`（個人向け完全同期）。複数面更新時は1回の操作で1回だけリビジョンを増加。 |
| `InkShotVisualPayload.java` | インク弾ビジュアル同期ペイロード（Phase 6+）。発射元・着弾点・色・サイズ・弧の高さ・飛行時間・ヒット種別をクライアントに伝達。 |

#### `selection/` — プレイヤー選択状態管理
| クラス | 役割 |
|---|---|
| `PlayerMarkerSelectionManager.java` | 1地点目選択時の一時状態管理 (ConcurrentHashMap)。 |

#### `mixin/` — Mixin
| クラス | 役割 |
|---|---|
| `ExampleMixin.java` | テンプレート由来のサンプルMixin。プロジェクトでは未使用。 |

---

### 2.2. クライアント側 (`src/client/java/yam/salmon/client/`)

#### `SalmonClient.java`
クライアント側専用処理。`ClientPlayNetworking.registerGlobalReceiver` で `ArenaDebugPayload`, `InkFaceUpdatePayload`, `InkSyncBeginPayload`, `InkSyncEndPayload`, `InkArenaClearPayload`, `InkShotVisualPayload` 受信登録。`ClientTickEvents.END_CLIENT_TICK` でショットTick更新、`LevelExtractionEvents` と `LevelRenderEvents` でアリーナ境界描画・インク描画・ショット描画を登録。

#### `arena/` — アリーナデバッグ描画
| クラス | 役割 |
|---|---|
| `ArenaDebugRenderer.java` | サーバーから同期されたアリーナ情報をクライアント側キャッシュに保持し、`LevelRenderContext` 経由でワイヤーフレームを描画。`PrimitiveTopology.QUADS` の細い直方体（厚さ0.02ブロック）による境界描画。壁越し表示（深度テスト無効）。 |

#### `ink/` — インク描画
| クラス | 役割 |
|---|---|
| `ClientInkCache.java` | クライアント側インクデータキャッシュ（Phase 3）。ConcurrentHashMapでスレッドセーフ。完全同期バッファリング、リビジョン比較、dimension分離、クリア処理。Phase 4では複数面更新Payloadも透過的に処理。 |
| `ClientInkSurfaceKey.java` | キャッシュキー用レコード（Phase 3）。arenaId + blockPos + face。 |
| `ClientInkSurface.java` | クライアント側1面データレコード（Phase 3）。cells[64], revision, teamACells, teamBCells。 |
| `ClientInkColors.java` | インク描画用色定義（Phase 3）。Team A=青(0.05, 0.35, 1.0, 0.92)、Team B=オレンジ(1.0, 0.35, 0.02, 0.92)。 |
| `InkRenderer.java` | インク描画レンダラー（Phase 3）。StagedVertexBuffer + RenderPipelineを使用。深度テスト有効。描画距離128ブロック。チャンクロード・ink_paintableタグ確認。抽出/描画の2フェーズパターン。InkCellGeometryで各セルのAABBを計算。 |

#### `shot/` — インク弾ビジュアル
| クラス | 役割 |
|---|---|
| `ClientInkShot.java` | クライアント側の1発分ビジュアルデータ（Phase 6+）。始点・終点・飛行時間(ticks)・色・サイズ・ヒット種別・弧の高さを保持し、毎tickの補間位置を計算。到達時に自動消滅。 |
| `ClientInkShotManager.java` | 全アクティブショット（最大512）のシングルトン管理（Phase 6+）。Payload受信で追加、毎フレームTick更新・死亡除去、着弾時スプラッシュパーティクル生成、次元移動時全クリア。 |
| `InkShotRenderer.java` | インク弾描画レンダラー（Phase 6+）。`StagedVertexBuffer` + `RenderPipeline`（`salmon:pipeline/ink_shot`）を使用し、カメラ相対座標で全アクティブショットを有色立方体QUADSとして描画。 |

#### `mixin/` — クライアントMixin
| クラス | 役割 |
|---|---|
| `GameRendererMixin.java` | `GameRenderer` にMixin。カスタムレンダーパイプラインの登録ポイントを提供。 |

---

### 2.3. リソース (`src/main/resources/`)

| パス | 内容 |
|---|---|
| `assets/salmon/` | ブロックステート、モデル、テクスチャ、言語ファイル (`ja_jp.json` / `en_us.json`) |
| `data/salmon/` | ルートテーブル (`loot_table/blocks/`)、ブロックタグ (`tags/block/`) 等のデータパック関連 |
| `data/minecraft/tags/block/mineable/pickaxe.json` | 採掘ツールタグ（ink_area_marker, inkable_block） |

---

## 3. 実装済み機能のステータス

### 3.1. Phase 1〜5: 基盤システム（完了）

- **エリアマーカーブロック (`ink_area_marker`)**
  - クリエイティブ/OPプレイヤーが2箇所右クリックで `InkArena` を作成・登録
  - 重複、サイズ上限（最小2 / 最大X=256,Y=128,Z=256 / 最大体積4,194,304）、異ディメンションチェックを自動実行
  - ブロック破壊やログアウト時に選択・登録を安全に解除/削除。同じマーカー再クリックで第1地点選択解除
  - 爆発耐性（3600000.0）、ピストン移動不可、ツルハシ破壊可能
  - ブロックモデルは仮テクスチャとしてバニラ `black_concrete` を参照

- **アリーナ番号システム**
  - アリーナ作成時に連番（#1, #2, ...）を自動割当。削除しても番号は詰め直さず、`nextArenaNumber` で次番号を管理
  - `dataVersion=2` で永続化。V1データは UUID 文字列順ソートで安全にマイグレーション
  - 番号重複検出時は WARN ログ出力、オーバーフロー対策済み

- **アリーナ作成時の表示**: 成功時にインクアリーナ#Nを登録。失敗時は番号を消費しない

- **アリーナ確認コマンド (`/salmon arena`)**
  - `list [page]`（1ページ10件）、`info <arenaNumber>`、`here`、`remove <arenaNumber>`（削除前情報表示、マーカー所属解除、ブロードキャスト、インクデータ自動削除）、`debug [on|off|status]`
  - 権限: クリエイティブ+OPレベル2以上

- **デバッグ表示**
  - `/salmon arena debug on/off` でプレイヤー単位のON/OFF切替
  - ON時完全同期、作成時追加差分、削除時削除差分、次元移動時 (`ServerPlayerEvents.AFTER_RESPAWN`) 再同期、OFF時クリア
  - 表示: 直方体ワイヤーフレーム（緑）、マーカーA（赤ボックス）、マーカーB（青ボックス）、表示距離128ブロック、inclusive座標（max+1）

- **データ永続化**
  - `SavedData` によりワールド単位でアリーナ情報を `salmon_ink_arenas` に保存。`dataVersion=3`
  - V1→V2 移行（arenaNumber追加）対応。V2→V3 でインクデータ（inkData）保存対応
  - `nextArenaNumber` も保存し、再起動後も番号が維持される

- **整合性チェック**: マーカーのBlockEntity型チェック、チャンク未ロード対応、同一マーカー二重登録防止

- **アリーナ削除の冪等性**
  - `removeArena()` / `removeArenaByMarker()` / `removeArenaByNumber()` は削除の成否を `boolean` で返却
  - 存在しないアリーナは `false`。削除通知は `removed == true` のときのみ表示
  - 二重通知防止。削除前にアリーナ情報を取得し、削除後もペアマーカーの所属情報を確実にクリア
  - アリーナ削除時にインクデータも自動削除（`InkStorage.removeArena()` 呼び出し）

- **孤立参照の自己修復**
  - `InkAreaMarkerBlockEntity.loadAdditional()` で `arenaId != null && pairedMarkerId != null` かつ `ArenaSavedData` に該当アリーナが存在しない場合、`arenaId`/`pairedMarkerId` を null クリアし警告ログ

- **塗装可能ブロック (`inkable_block`)**
  - 不透明なフルブロック、BlockEntityなし。石程度の硬さ（硬度1.5F、爆発耐性6.0F）
  - ツルハシで適正採掘。ピストン移動許可
  - ブロックモデルは仮テクスチャとしてバニラ `light_gray_concrete` を参照
  - クリエイティブインベントリに `useBlockDescriptionPrefix()` で表示
  - loot table、ブロックタグ (`mineable/pickaxe`) 完備

- **ブロックタグ (`salmon:ink_paintable`)**
  - `salmon:inkable_block` のみを含む。データパックから拡張可能（`replace: false`）

- **塗装可能判定システム**
  - `InkPaintability.checkPaintable()` でタグチェック → アリーナ内判定 → 面の露出判定 の3段階判定
  - 失敗理由は列挙型で返却

- **インク塗装システム**
  - **8×8 グリッド**: 各面64セル。セル値は byte（NONE=0, TEAM_A=1, TEAM_B=2）
  - **面上UV変換**: `InkFaceCoordinates.fromHit()` で `BlockHitResult` から UV/セル座標計算
  - **円形塗装**: `InkFaceData.paintCircle(centerU, centerV, radius, team)`、デフォルト半径0.25（直径約4セル）。centerU/V の範囲外許容
  - **疎な保存**: 塗られた面のみ `Map<InkSurfaceKey, InkFaceData>` で保持。面が空になると削除
  - **永続化**: dataVersion=3、`InkStorage.STORAGE_CODEC` でシリアライズ

- **複数ブロックにまたがる塗装（3D球ブラシ）**
  - 球AABB内の全BlockPos×6面を走査し、`FaceBasis.sphereIntersectsFace()` で球と面矩形の交差判定 → 露出判定 → `projectOntoFace()` で面上投影 → 円塗装
  - 90度角面にもインクが自然に回り込む
  - 半径上限8.0、候補面数上限4096

- **面ローカル座標系 (`FaceBasis`)**
  - 6面の法線・U軸・V軸を定義。`toWorldPoint()`, `fromWorld()`, `projectOntoFace()`, `sphereIntersectsFace()`, `distanceToPlane()`/`signedDistanceToPlane()` を提供
  - `InkFaceCoordinates`・`InkCellGeometry`・`InkPlaneCoordinates` と整合する一貫した座標変換

- **インクコマンド (`/salmon ink`)**
  - `inspect` : 照準面の8×8グリッド表示
  - `clear <arenaNumber>` : アリーナ全インク削除、即時保存＋同期
  - `debug [on|off|status]` : クライアント側診断表示制御
  - 権限: クリエイティブ+OPレベル2以上

- **インク同期システム**
  - **Payload**: `InkFaceUpdatePayload`, `InkSyncBeginPayload`, `InkSyncEndPayload`, `InkArenaClearPayload`
  - **差分同期**: 塗装時・clear時・アリーナ削除時に同ディメンション全プレイヤーへブロードキャスト
  - **完全同期**: プレイヤー参加時・リスポーン/次元移動時に BEGIN→face×N→END の3段階
  - **リビジョン**: アリーナ単位の単調増加 long 値。複数面更新は同一リビジョンを共有
  - **クライアントキャッシュ**: `ClientInkCache`（ConcurrentHashMap、リビジョン比較、次元分離）
  - **クライアント描画**: `InkRenderer`（StagedVertexBuffer + RenderPipeline、深度テスト有効、描画距離128ブロック）

### 3.2. Phase 6+: ブキ・戦闘システム

- **インクシューター (`ink_shooter`)**
  - 右クリックで発射開始、使用キー押下中は `InkShooterTickHandler` により継続連射
  - プレイヤー単位のクールダウン管理
  - アイテムモデルは `assets/salmon/items/ink_shooter.json` で定義

- **シューター発射ロジック (`InkShooterService`)**
  - 設定可能な射程・拡散角・重力・速度・ダメージでのブロック/エンティティレイキャスト
  - ブロックヒット時: `InkPaintingService` で塗装 + `InkSyncManager` で同期 + `InkShotVisualPayload` でビジュアル配信
  - エンティティヒット時: `InkCombatService` でダメージ適用
  - 空中/壁無視（miss）時もビジュアルのみ配信

- **ダメージ適用 (`InkCombatService`)**
  - 自己攻撃不可・無敵・死亡・攻撃可能判定チェック後、プレイヤー攻撃ダメージを適用

- **インク塗装サービス (`InkPaintingService`)**
  - `InkPaintDistributor` 委譲 + `InkArenaManager` 永続化 + `InkSyncManager` 複数面同期 の統一エントリポイント

- **インク弾ビジュアル**
  - サーバー: `InkShotVisualPayload` で始点・終点・色・サイズ・弧高・飛行時間・ヒット種別を送信
  - クライアント: `ClientInkShotManager` で最大512発管理、`InkShotRenderer` で有色立方体QUADS描画
  - 毎tick補間による弧状軌道アニメーション、着弾時スプラッシュパーティクル、次元移動時全クリア

---

## 4. 既知の課題・今後のタスク (Todo)

1. **グラフィック/テクスチャ**: inkable_block は仮で `light_gray_concrete` のテクスチャを参照中。専用テクスチャの用意とモデルの参照修正が必要。
2. **権限チェックの厳密化**: 現在 `PermissionLevel.GAMEMASTERS` を使用中。必要に応じて権限レベル別の判定に見直し。
3. **メイン機能（イカ状態・移動効果・インク面積）の開発**: ブキ発射・ダメージは完了。次: プレイヤー移動速度変更、イカ状態、インク面積カウント、ゲームルール。
4. **削除時のペアマーカー処理（チャンク未ロード時）**: 現在、ペアマーカーのチャンクが未ロードの場合、そのBlockEntityの所属情報は次回ロード時の自己修復に委ねている。
5. **チャンク索引**: 現在 `findArenaContaining` は全アリーナ線形検索。アリーナ数増加時にチャンク索引へ差し替える必要あり。
6. **デバッグ表示の永続化**: 現在デバッグON/OFFはログアウト時に自動OFF。ワールド間で設定を保持したい場合は SavedData への保存が必要。
7. **面露出判定の精度向上**: 部分ブロック（階段・ハーフブロック・フェンス・透明ブロック等）の厳密な面判定は未実装。Phase 5でもフルキューブブロックのみを前提としている。
8. **セル結合によるメッシュ最適化**: 現在は1セル＝1薄い直方体（6面QUADS×64セル画面）。隣接同色セルを長方形に結合して頂点数・DrawCall数を削減可能。
9. **描画透過/半透明順序**: 現在は不透明（alpha≈0.92）で深度テスト有効。半透明描画へ移行する場合は描画順序問題が発生する可能性。
10. **距離ベース同期フィルタリング**: 現在は同ディメンション全プレイヤーへブロードキャスト。192ブロック距離フィルタリングの導入が望ましい。
11. **通信圧縮**: 64セルをbyte配列で送信。ランレングス圧縮等で削減可能。複数面Payload集約も検討。
12. **Sodium/Iris互換性**: `StagedVertexBuffer` + `RenderPipeline` 方式はSodium/Iris等の最適化Modと競合する可能性。`FeatureRenderer` APIへの移行が望ましい。
13. **円の粗さ**: 8×8解像度で直径約4セルの円では、ブロック境界付近で形状がやや不自然になる。より高解像度グリッド（16×16等）への移行を検討。
14. **凹角・凸角の厳密な区別**: Phase 5は球と面の交差判定で両方を自然に扱うが、露出判定は隣接ブロックの有無のみ。より精確な接続面選択は将来対応。
15. **`InkPlaneCoordinates` の削除検討**: Phase 5で3D球ベース分配に移行したため、`InkPlaneCoordinates` は `InkableBlock` のhit座標→ワールド座標変換以外では使用されなくなった。将来の整理候補。
16. **`ExampleMixin.java` の整理**: テンプレート由来のサンプルMixin。プロジェクトで未使用のため削除検討。

---

## 5. AIへの行動ルール & 制約事項

1. **検索・探索の制限**: 関係のない構成ファイル（gradle設定等）や無用な全検索を行わず、上記のパッケージ構成に従ってピンポイントにファイルを読み込むこと。
2. **置換失敗時の対応**: `replace_in_file` が失敗した場合は、何度も繰り返さず `write_to_file` でファイル全体を書き換えて対処すること。
3. **【最重要】AGENTS.md（セクション2〜6）の自動自己更新**: タスクが完了した際、またはファイル構造・機能に変更が生じた場合は、完了報告をする前に**必ずこの `AGENTS.md` 全体を見直し、セクション2〜6をすべて最新情報に書き換えて上書き保存**すること。
4. **Minecraft 26.2 API 固有の注意事項**:
   - **genSources**: `./gradlew genSources` で 26.2 のソースコードを生成済み。APIの型やメソッドを確認する際は、過去の知識ではなくローカルに生成されたソースコード（参照定義）を優先して確認しながら修正すること。
   - **API解析用のJAR**:
     - **共通ロジック**: `%USERPROFILE%\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-common-deobf\26.2\minecraft-common-deobf-26.2.jar`
     - **クライアント専用**: `%USERPROFILE%\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-clientonly-deobf\26.2\minecraft-clientonly-deobf-26.2.jar`
   - **MC 26.2 レンダリング大変更**: `Tessellator` 削除、`BufferUploader` 削除、描画は `FeatureRenderer`/`SubmitNode` システムに移行。`mc.camera` → `mc.gameRenderer.mainCamera()`。`PrimitiveTopology.LINE_LIST` → `PrimitiveTopology.LINES`。
   - **ネットワーク**: `CustomPacketPayload` + `ServerPlayNetworking` / `ClientPlayNetworking` を使用。`PacketByteBuf` は非推奨。`Identifier` は `Identifier.fromNamespaceAndPath()` または `Identifier.parse()` で作成。FriendlyByteBuf では `readUtf()`/`writeUtf()` で送受信。
   - **コマンド**: `CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> { ... })` の3引数形式を使用。`PermissionLevel.GAMEMASTERS` はレベル2相当。
   - **イベント**: `ServerEntityWorldChangeEvents` は 26.2 の Fabric API に存在しない。次元移動検知には `ServerPlayerEvents.AFTER_RESPAWN` を使用する。プレイヤー参加時は `AFTER_RESPAWN` で完全同期。
   - **BlockState occlusion API (MC 26.2)**: `getOcclusionShape()`, `canOcclude()`, `isSolidRender()`, `isCollisionShapeFullBlock()`, `isFaceSturdy()`, `canBeReplaced()`。
   - **Direction の Codec**: MC 26.2 の `Direction` には Codec が存在しないため、`Codec.STRING.xmap(name -> Direction.byName(name), Direction::getSerializedName)` を使用してシリアライズする。
   - **BlockPos**: MC 26.2 の `BlockPos` は immutable。`BlockPos.CODEC` が利用可能。
   - **StagedVertexBuffer API**: `appendDraw(VertexFormat, PrimitiveTopology, VertexSorting?)` → `getVertexBuilder(draw)` で頂点構築 → `upload()` → `getExecuteInfo(draw)` で情報取得 → `RenderPass.drawIndexed()` で描画 → `endFrame()`。

---

## 6. アセット管理ガイドライン

### 6.1. アセットフォルダ構成

| パス | 内容 |
|---|---|
| `items/` | アイテムのモデル定義 JSON（`models/item/` は使用しない） |
| `models/block/` | ブロックの3D形状モデル JSON |
| `blockstates/` | ブロック状態（Blockstate）定義 JSON |
| `textures/block/` | ブロック用テクスチャ画像（.png） |
| `textures/item/` | アイテム用テクスチャ画像（.png） |

### 6.2. アイテム定義フォーマット (`assets/<mod_id>/items/<item_id>.json`)

1.21.4 以降のコンポーネント指向構文を使用すること。

**標準的な BlockItem（ブロックモデルを参照する場合）:**
```json
{
  "model": {
    "type": "minecraft:model",
    "model": "<mod_id>:block/<block_id>"
  }
}
```

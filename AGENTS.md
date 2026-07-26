# AGENTS.md - スプラトゥーンMod (salmon) 開発ガイドライン

## 1. プロジェクト概要 & 開発環境
Minecraft Java Edition 上でスプラトゥーンを再現するFabric Modプロジェクトです。

- **Minecraft Version**: 26.2
- **Fabric Loader**: 0.19.3 / **Fabric API**: 0.155.2+26.2
- **Java Version**: 25 (Loom 1.17.17-SNAPSHOT)
- **Mappings**: Mojang Official (intermediary)
- **Mod ID**: `salmon`
- **Main Package**: `yam.salmon`

---

## 2. ディレクトリ構造と主要クラスの役割
AIはコードを検索・修正する際、以下の配置ルールと役割を必ず参考にしてください。

- `src/main/java/yam/salmon/`
    - `Salmon.java` : ModInitializer。ブロック/BlockEntityの登録、イベントリスナー（切断/破壊/次元移動時のデバッグ再同期等）の定義。`id(String)` で `Identifier` を生成。ネットワークPayload登録。プレイヤー参加/リスポーン時のインク完全同期。
    - `block/` : ブロック関連の実装
        - `ModBlocks.java` : ブロック・BlockItem・BlockEntityTypeのレジストリ登録。`register()` 内で `ResourceKey<Block>`, `ResourceKey<Item>` を生成し、`Item.Properties#setId(itemKey)` + `useBlockDescriptionPrefix()` で BlockItem を構築。`INK_AREA_MARKER_BLOCK_ENTITY` は register() 内でブロック登録後に初期化。`INKABLE_BLOCK` の登録も `registerInkableBlock()` で行う。
        - `InkAreaMarkerBlock.java` : アリーナ範囲選択用マーカーブロック（右クリック処理等）。コンストラクタで `Properties.setId(ResourceKey<Block>)` 必須（26.2）。アリーナ作成成功時に `ArenaDebugSync.broadcastArenaAdded` で同次元のデバッグ有効プレイヤーに即時通知。
        - `InkAreaMarkerBlockEntity.java` : マーカーのNBTデータ保持・ペア管理、破壊時アリーナ削除。削除の冪等性保証、ペアマーカーの所属情報クリア、ロード時孤立参照自己修復、破壊時に `ArenaDebugSync.broadcastArenaRemoved` でデバッグ表示同期を実装。
        - `InkableBlock.java` : 塗装可能ブロック。BlockEntity なし。管理者が素手で右クリック時に Phase 1 の塗装可能判定を再利用し、Phase 2 の塗装操作を実行する。Shift+右クリックで Team B、通常右クリックで Team A で塗装。失敗時・変更なし時も適切なメッセージを表示。Phase 3: 塗装成功時に `InkSyncManager.broadcastFaceUpdate()` でクライアント同期。
        - `ModBlockTags.java` : ブロックタグ定数クラス。`INK_PAINTABLE`（salmon:ink_paintable）、`MINEABLE_PICKAXE`（minecraft:mineable/pickaxe）を定義。
    - `arena/` : インクアリーナの管理ロジック
        - `InkArena.java` : アリーナのデータモデル（範囲計算・包含判定・重複判定）。`arenaNumber` フィールドを持ち、Codec でシリアライズ（optional、デフォルト0で後方互換）。
        - `InkArenaManager.java` : ワールドごとのアリーナ永続化管理 (`SavedData`)。作成・削除（boolean戻り値で冪等性保証）・検索・存在確認 (`arenaExists`)・番号割当 (`allocateArenaNumber`)・番号検索 (`getArenaByNumber`)・V1→V2 データ移行 (`migrateFromV1`)。`InkStorage` のインクデータロード・保存も担当（dataVersion=3）。`saveInkDataNow()` で即時保存。アリーナ削除時にインクデータも自動削除。デバッグ表示のプレイヤー単位ON/OFF管理も担当。
        - `ArenaPermission.java` : クリエイティブ+OP権限判定ロジック（`canConfigure`）を一元管理。
    - `ink/` : インク塗装システム
        - `InkPaintability.java` : 塗装可能判定のユーティリティクラス（Phase 1）。`checkPaintable()` でタグ→アリーナ→面露出の3段階判定を行う。
        - `PaintabilityResult.java` : 塗装可能判定結果レコード（Phase 1）。
        - `PaintabilityFailureReason.java` : 塗装不能理由の列挙型（Phase 1）。
        - `InkTeam.java` : インクチーム定数クラス。`NONE=0`, `TEAM_A=1`, `TEAM_B=2`。`toChar()`, `toName()`, `isValidTeam()`, `normalize()` を提供。
        - `InkSurfaceKey.java` : 塗装面を一意に識別するレコード。`BlockPos` と `Direction` の組み合わせ。
        - `InkFaceData.java` : ブロック1面の8×8インクグリッドデータ。`GRID_SIZE=8`, `CELL_COUNT=64`。`getCell()`, `setCell()`, `paintCircle()`, `isEmpty()`, `clear()`, `copyCells()`, `toString()` を提供。セル値は byte で保持。内部配列は `index=v*GRID_SIZE+u`。
        - `InkFaceCoordinates.java` : ブロック面へのヒット座標から UV座標・セル座標への変換ユーティリティ。`fromHit(face, localX, localY, localZ)` で6面対応のUV変換。u==1.0, v==1.0 境界の clamp 処理済み。
        - `InkCellGeometry.java` : ブロック面の8×8グリッドセルのワールド座標AABBを計算するユーティリティ（Phase 3）。`getCellBounds(blockPos, face, cellU, cellV)` で6面対応。`InkFaceCoordinates` と同じUV定義を使用し描画との一貫性を保証。`SURFACE_OFFSET=0.002`、`INK_THICKNESS=0.008`。
        - `PaintResult.java` : 塗装操作結果レコード。`success`, `changedCells`, `arenaNumber`, `blockPos`, `face`, `coordinates`, `team`, `failureReason` を保持。
        - `PaintFailureReason.java` : 塗装操作失敗理由の列挙型。`NO_PERMISSION`, `NOT_PAINTABLE_BLOCK`, `OUTSIDE_ARENA`, `FACE_OCCLUDED`, `INVALID_TEAM`, `NO_CHANGE`。
        - `InkStorage.java` : アリーナごとのインクデータ管理ストレージ。疎な保存方式（実際に塗られた面だけ `Map<InkSurfaceKey, InkFaceData>` で保持）。`paint()`（塗装操作＋再検証）、`getFace()`/`getFaceOrEmpty()`、`clearArena()`、`removeArena()`、`exportData()`/`importData()`（永続化用）、`STORAGE_CODEC`（保存用 Codec）、`SavedSurface`/`SavedArenaInk`（保存用中間データ）を提供。
    - `selection/` : プレイヤーの選択状態管理
        - `PlayerMarkerSelectionManager.java` : 1地点目選択時の一時状態管理 (ConcurrentHashMap)
    - `command/` : コマンド登録
        - `SalmonCommands.java` : `/salmon arena list|info|here|remove|debug`、`/salmon ink inspect|clear <arenaNumber>|debug on|off|status` の全コマンド実装。ページング、削除前表示、デバッグブロードキャスト、インク面8×8グリッド表示、インククリア同期、インクデバッグ制御を含む。
    - `network/` : ネットワーク同期
        - `ArenaDebugPayload.java` : `CustomPacketPayload` 実装。FULL_SYNC/ADD/REMOVE/CLEAR の4アクション。完全同期開始(-1)/終了(-2)マーカー付き。
        - `ArenaDebugSync.java` : サーバー→クライアント同期。`sendFullSync`（個人向け完全同期）、`sendArenaAdded`/`sendArenaRemoved`（個人向け差分）、`broadcastArenaAdded`/`broadcastArenaRemoved`（同次元全デバッグ有効プレイヤー向けブロードキャスト）、`sendClear`（クリア指示）。
        - `InkFaceUpdatePayload.java` : インク1面更新ペイロード（Phase 3）。arenaUuid, arenaNumber, dimensionId, blockPos, faceName, cells[64], revision。
        - `InkSyncBeginPayload.java` : インク完全同期開始ペイロード（Phase 3）。dimensionId, sessionId, faceCount, revision。
        - `InkSyncEndPayload.java` : インク完全同期終了ペイロード（Phase 3）。sessionId, faceCount。
        - `InkArenaClearPayload.java` : アリーナインク全消去ペイロード（Phase 3）。arenaUuid, arenaNumber, dimensionId, revision。
        - `InkSyncManager.java` : インクデータ同期マネージャー（Phase 3）。アリーナ単位のlongリビジョン管理。`broadcastFaceUpdate()`（塗装差分ブロードキャスト）、`broadcastArenaClear()`（クリアブロードキャスト）、`broadcastArenaRemoved()`（アリーナ削除ブロードキャスト）、`sendFullSync()`（個人向け完全同期）。Phase 3初期版では同ディメンション全プレイヤーへブロードキャスト。
- `src/client/java/yam/salmon/client/`
    - `SalmonClient.java` : クライアント側専用処理。`ClientPlayNetworking.registerGlobalReceiver` で `ArenaDebugPayload`, `InkFaceUpdatePayload`, `InkSyncBeginPayload`, `InkSyncEndPayload`, `InkArenaClearPayload` 受信登録。`LevelExtractionEvents` と `LevelRenderEvents` でアリーナ境界描画とインク描画を登録。
    - `arena/ArenaDebugRenderer.java` : サーバーから同期されたアリーナ情報をクライアント側キャッシュに保持し、`LevelRenderContext` 経由でワイヤーフレームを描画する。`PrimitiveTopology.QUADS` の細い直方体（厚さ0.02ブロック）による境界描画。壁越し表示（深度テスト無効）。
    - `ink/ClientInkCache.java` : クライアント側インクデータキャッシュ（Phase 3）。ConcurrentHashMapでスレッドセーフ。完全同期バッファリング、リビジョン比較、dimension分離、クリア処理。
    - `ink/ClientInkSurfaceKey.java` : キャッシュキー用レコード（Phase 3）。arenaId + blockPos + face。
    - `ink/ClientInkSurface.java` : クライアント側1面データレコード（Phase 3）。cells[64], revision, teamACells, teamBCells。
    - `ink/ClientInkColors.java` : インク描画用色定義（Phase 3）。Team A=青(0.05, 0.35, 1.0, 0.92)、Team B=オレンジ(1.0, 0.35, 0.02, 0.92)。
    - `ink/InkRenderer.java` : インク描画レンダラー（Phase 3）。StagedVertexBuffer + RenderPipelineを使用。深度テスト有効。描画距離128ブロック。チャンクロード・ink_paintableタグ確認。抽出/描画の2フェーズパターン。InkCellGeometryで各セルのAABBを計算。
- `src/main/resources/`
    - `assets/salmon/` : ブロックステート、モデル、テクスチャ、言語ファイル (`ja_jp.json` / `en_us.json`)
    - `data/salmon/` : ルートテーブル (`loot_table/blocks/`)、ブロックタグ (`tags/block/`) 等のデータパック関連
    - `data/minecraft/tags/block/mineable/pickaxe.json` : 採掘ツールタグ（ink_area_marker, inkable_block）

---

## 3. 実装済み機能のステータス (2026年時点)
- **エリアマーカーブロック (`ink_area_marker`)**: クリエイティブ/OPプレイヤーが2箇所右クリックで `InkArena` を作成・登録。重複、サイズ上限（最小2 / 最大X=256,Y=128,Z=256 / 最大体積4,194,304）、異ディメンションチェックを自動実行。ブロック破壊やログアウト時に選択・登録を安全に解除/削除。同じマーカー再クリックで第1地点選択解除。爆発耐性（3600000.0）、ピストン移動不可、ツルハシ破壊可能。ブロックモデルは仮テクスチャとしてバニラ `black_concrete` を参照。
- **アリーナ番号システム**: アリーナ作成時に連番（#1, #2, ...）を自動割当。削除しても番号は詰め直さず、`nextArenaNumber` で次番号を管理。`dataVersion=2` で永続化。V1データは UUID 文字列順ソートで安全にマイグレーション。番号重複検出時は WARN ログ出力、オーバーフロー対策済み。
- **アリーナ作成時の表示**: 成功時にインクアリーナ#Nを登録。失敗時は番号を消費しない。
- **アリーナ確認コマンド** (`/salmon arena`): `list [page]`（1ページ10件）、`info <arenaNumber>`、`here`、`remove <arenaNumber>`（削除前情報表示、マーカー所属解除、ブロードキャスト、インクデータ自動削除）、`debug [on|off|status]`。権限: クリエイティブ+OPレベル2以上。
- **デバッグ表示**: `/salmon arena debug on/off` でプレイヤー単位のON/OFF切替。ON時完全同期、作成時追加差分、削除時削除差分、次元移動時 (`ServerPlayerEvents.AFTER_RESPAWN`) 再同期、OFF時クリア。表示: 直方体ワイヤーフレーム（緑）、マーカーA（赤ボックス）、マーカーB（青ボックス）、表示距離128ブロック、inclusive座標（max+1）。
- **データ永続化**: `SavedData` によりワールド単位でアリーナ情報を `salmon_ink_arenas` に保存。`dataVersion=3`。V1→V2 移行（arenaNumber追加）対応。V2→V3 でインクデータ（inkData）保存対応。`nextArenaNumber` も保存し、再起動後も番号が維持される。
- **整合性チェック**: マーカーのBlockEntity型チェック、チャンク未ロード対応、同一マーカー二重登録防止。
- **アリーナ削除の冪等性**: `removeArena()` / `removeArenaByMarker()` / `removeArenaByNumber()` は削除の成否を `boolean` で返却。存在しないアリーナは `false`。削除通知は `removed == true` のときのみ表示。二重通知を防止。削除前にアリーナ情報を取得し、削除後もペアマーカーの所属情報を確実にクリア。アリーナ削除時にインクデータも自動削除（`InkStorage.removeArena()` 呼び出し）。
- **孤立参照の自己修復**: `InkAreaMarkerBlockEntity.loadAdditional()` で `arenaId != null && pairedMarkerId != null` かつ `ArenaSavedData` に該当アリーナが存在しない場合、`arenaId`/`pairedMarkerId` を null クリアし警告ログを1回出力。
- **塗装可能ブロック (`inkable_block`)** (Phase 1): 不透明なフルブロック、BlockEntityなし。石程度の硬さ（硬度1.5F、爆発耐性6.0F）。ツルハシで適正採掘。ピストン移動許可。ブロックモデルは仮テクスチャとしてバニラ `light_gray_concrete` を参照。クリエイティブインベントリに `useBlockDescriptionPrefix()` で表示。loot table、ブロックタグ (`mineable/pickaxe`) 完備。
- **ブロックタグ (`salmon:ink_paintable`)**: 塗装可能ブロックを表すタグ。初期状態では `salmon:inkable_block` のみを含む。データパックから拡張可能（`replace: false`）。
- **塗装可能判定システム** (Phase 1): `InkPaintability.checkPaintable()` で以下の3段階判定: 1) タグチェック、2) アリーナ内判定、3) 面の露出判定。失敗理由は列挙型で返却。
- **インク塗装システム** (Phase 2):
    - **8×8 グリッド**: ブロック各面を 8×8=64 セルに分割。各セルは byte 値（NONE=0, TEAM_A=1, TEAM_B=2）でチームを保持。
    - **面上UV変換**: `InkFaceCoordinates.fromHit()` で `BlockHitResult` のヒット座標から UV座標・セル座標（cellU 0..7, cellV 0..7, cellIndex 0..63）を計算。6面すべてで一貫したUV方向を定義。
    - **円形塗装**: `InkFaceData.paintCircle(centerU, centerV, radius, team)` で指定半径内のセルを塗装。デフォルト半径 0.24（直径約4セル）。
    - **疎な保存**: 実際に塗られた面のみ `Map<InkSurfaceKey, InkFaceData>` で保持。`computeIfAbsent` で初回生成。面が空になると Map から削除。
    - **塗装操作**: 管理者が素手で `inkable_block` を右クリック → Team A、Shift+右クリック → Team B。操作後即時 SavedData 保存。
    - **永続化**: dataVersion=3、`InkStorage.STORAGE_CODEC` でシリアライズ。
- **インクコマンド** (`/salmon ink`):
    - `inspect` : プレイヤーが見ている `inkable_block` の面の8×8グリッドをチャットに表示。
    - `clear <arenaNumber>` : 指定アリーナの全インク面データを削除。即時保存＋クライアント同期。
    - `debug [on|off|status]` : クライアント側インク診断表示の制御。
    - 権限: クリエイティブ+OPレベル2以上。
- **インク同期システム** (Phase 3):
    - **Payload**: `InkFaceUpdatePayload`（1面差分）、`InkSyncBeginPayload`/`InkSyncEndPayload`（完全同期セッション）、`InkArenaClearPayload`（クリア/削除）。
    - **差分同期**: 塗装時・clear時・アリーナ削除時に同ディメンション全プレイヤーへブロードキャスト。
    - **完全同期**: プレイヤー参加時・リスポーン/次元移動時。BEGIN→face×N→END の3段階。
    - **リビジョン**: アリーナ単位の単調増加 long 値。古いPayloadの上書き防止。
    - **クライアントキャッシュ**: `ClientInkCache`。ConcurrentHashMapでスレッドセーフ。リビジョン比較、次元分離、完全同期バッファリング。
    - **クライアント描画**: `InkRenderer`。StagedVertexBuffer + RenderPipeline（深度テスト有効）。`InkCellGeometry`で各セルのAABB計算。描画距離128ブロック。チャンクロード・タグチェック。Team A=青、Team B=オレンジの薄い直方体。

---

## 4. 既知の課題・今後のタスク (Todo)
1. **グラフィック/テクスチャ**: マーカーブロックは仮で `black_concrete`、inkable_block は仮で `light_gray_concrete` のテクスチャを参照中。専用テクスチャの用意とモデルの参照修正が必要。
2. **権限チェックの厳密化**: 現在 `PermissionLevel.GAMEMASTERS` を使用中。必要に応じて権限レベル別の判定に見直し。
3. **メイン機能（ブキ・発射物・ダメージ・移動効果）の開発**: Phase 3まで完了（同期・描画）。Phase 4: プレイヤー移動速度変更、イカ状態、ブキ、発射物、ダメージ、インク面積。
4. **削除時のペアマーカー処理（チャンク未ロード時）**: 現在、ペアマーカーのチャンクが未ロードの場合、そのBlockEntityの所属情報は次回ロード時の自己修復に委ねている。
5. **チャンク索引**: 現在 `findArenaContaining` は全アリーナ線形検索。アリーナ数増加時にチャンク索引へ差し替える必要あり。
6. **デバッグ表示の永続化**: 現在デバッグON/OFFはログアウト時に自動OFF。ワールド間で設定を保持したい場合は SavedData への保存が必要。
7. **面露出判定の精度向上**: 部分ブロック（階段・ハーフブロック・フェンス・透明ブロック等）の厳密な面判定は未実装。
8. **複数ブロックにまたがる塗装**: 現在の `paintCircle` は単一面のみ対象。複数ブロックや異なる面への同時塗装は未対応。
9. **セル結合によるメッシュ最適化**: 現在は1セル＝1薄い直方体（6面QUADS×64セル画面）。隣接同色セルを長方形に結合して頂点数・DrawCall数を削減可能。
10. **描画透過/半透明順序**: 現在は不透明（alpha≈0.92）で深度テスト有効。半透明描画へ移行する場合は描画順序問題が発生する可能性。
11. **距離ベース同期フィルタリング**: 現在は同ディメンション全プレイヤーへブロードキャスト。192ブロック距離フィルタリングの導入が望ましい。
12. **通信圧縮**: 64セルをbyte配列で送信しているが、ランレングス圧縮等で削減可能。
13. **Sodium/Iris互換性**: `StagedVertexBuffer` + `RenderPipeline` 方式はSodium/Iris等の最適化Modと競合する可能性。`FeatureRenderer` APIへの移行が望ましい。

---

## 5. AIへの行動ルール & 制約事項
1. **検索・探索の制限**: 関係のない構成ファイル（gradle設定等）や無用な全検索を行わず、上記のパッケージ構成に従ってピンポイントにファイルを読み込むこと。
2. **進捗の簡潔出力**: ファイル修正やコマンド実行の前後には、3行以内で「[進捗]」「[理由]」「[次項]」を出力し、作業内容を報告すること。
3. **置換失敗時の対応**: `replace_in_file` が失敗した場合は、何度も繰り返さず `write_to_file` でファイル全体を書き換えて対処すること。
4. **【最重要】AGENTS.md（セクション2〜5）の自動自己更新**: タスクが完了した際、またはファイル構造・機能に変更が生じた場合は、完了報告をする前に**必ずこの `AGENTS.md` 全体を見直し、以下のセクション2〜5をすべて最新情報に書き換えて上書き保存**すること。
5. **Minecraft 26.2 API 固有の注意事項**:
   - **genSources**: `./gradlew genSources` で 26.2 のソースコードを生成済み。APIの型やメソッドを確認する際は、過去の知識ではなくローカルに生成されたソースコード（参照定義）を優先して確認しながら修正すること。
   - **API解析用のJAR**: クラス検索時は目的に応じて以下の2つのパスを参照すること:
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
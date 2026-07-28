package yam.salmon.ink;

/**
 * 塗装操作の失敗理由を示す列挙型。
 * Phase 1 の {@link PaintabilityFailureReason} に加え、
 * 実際の塗装操作で発生する理由も含む。
 */
public enum PaintFailureReason {
    /** 権限不足 */
    NO_PERMISSION,

    /** 対象ブロックが salmon:ink_paintable タグに含まれていない */
    NOT_PAINTABLE_BLOCK,

    /** 対象座標が登録済みのインクアリーナ内にない */
    OUTSIDE_ARENA,

    /** 指定面が隣接ブロックによって完全に塞がれている */
    FACE_OCCLUDED,

    /** 無効なチーム値 */
    INVALID_TEAM,

    /** すでに同じチームのインクで塗られている（変更なし） */
    NO_CHANGE
}
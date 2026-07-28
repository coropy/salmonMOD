package yam.salmon.ink;

/**
 * 塗装不可能な場合の理由を示す列挙型。
 */
public enum PaintabilityFailureReason {
    /** 対象ブロックが salmon:ink_paintable タグに含まれていない */
    NOT_PAINTABLE_BLOCK,

    /** 対象座標が登録済みのインクアリーナ内にない */
    OUTSIDE_ARENA,

    /** 指定面が隣接ブロックによって完全に塞がれている */
    FACE_OCCLUDED
}
package yam.salmon.weapon;

/**
 * プロジェクタイル（主弾・トレイル滴）の終了理由。
 */
public enum ProjectileFinishReason {
    /** 生存中（まだ飛んでいる） */
    ALIVE,
    /** 固体ブロックに命中 */
    BLOCK_HIT,
    /** Entityに命中 */
    ENTITY_HIT,
    /** 安全タイムアウト（絶対上限tick到達） */
    SAFETY_TIMEOUT,
    /** ワールド外（minBuildHeightより下） */
    OUT_OF_WORLD,
    /** 物理値が不正（NaN/Inf） */
    INVALID_PHYSICS,
}
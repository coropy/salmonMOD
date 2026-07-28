package yam.salmon.ink;

import yam.salmon.arena.InkArena;

import java.util.Optional;

/**
 * 塗装可能判定の結果を表すレコード。
 *
 * @param paintable    塗装可能かどうか
 * @param arena        所属するインクアリーナ（塗装可能な場合のみ値あり）
 * @param failureReason 塗装不可能な場合の理由（塗装可能な場合は空）
 */
public record PaintabilityResult(
        boolean paintable,
        Optional<InkArena> arena,
        Optional<PaintabilityFailureReason> failureReason
) {
    /** 塗装可能な場合の成功結果を生成 */
    public static PaintabilityResult success(InkArena arena) {
        return new PaintabilityResult(true, Optional.of(arena), Optional.empty());
    }

    /** 塗装不可能な場合の失敗結果を生成 */
    public static PaintabilityResult fail(PaintabilityFailureReason reason) {
        return new PaintabilityResult(false, Optional.empty(), Optional.of(reason));
    }
}
package com.example.radio_timer;

import androidx.media3.common.Player;

/**
 * Oynatma durum makinesinin saf karar fonksiyonları (Android Context yok, birim testli).
 * Kurallar: docs/tur2-kararlar.md §3.1 / §3.2.
 */
public final class PlaybackDecisions {

    /** Bu süreden uzun kullanıcı duraklatmasından sonra canlı yayın yeniden hazırlanır. */
    public static final long STALE_PAUSE_MS = 30_000L;

    public enum ToggleAction { PAUSE, RESUME, REPREPARE }

    private PlaybackDecisions() {}

    /** Oynatma geçici olarak bastırılmış mı (ör. kısa süreli audio focus kaybı). */
    public static boolean isSuppressed(int playbackSuppressionReason) {
        return playbackSuppressionReason != Player.PLAYBACK_SUPPRESSION_REASON_NONE;
    }

    /**
     * §3.1 öncelik listesine göre durum. {@code RadioService.STATE_*} değerlerinden birini döndürür, asla null.
     */
    public static String resolveState(boolean hasMedia, boolean hasError, boolean switchingSource,
                                      boolean playWhenReady, int playbackSuppressionReason,
                                      int exoPlaybackState, boolean readySincePrepare) {
        if (!hasMedia) return RadioService.STATE_IDLE;
        if (hasError) return RadioService.STATE_ERROR;
        if (switchingSource) return RadioService.STATE_CONNECTING;
        if (!playWhenReady) return RadioService.STATE_PAUSED;
        if (exoPlaybackState == Player.STATE_ENDED) return RadioService.STATE_ERROR;
        if (isSuppressed(playbackSuppressionReason)) return RadioService.STATE_INTERRUPTED;
        if (exoPlaybackState == Player.STATE_READY) return RadioService.STATE_PLAYING;
        // STATE_BUFFERING veya STATE_IDLE (prepare bekleniyor)
        return readySincePrepare ? RadioService.STATE_BUFFERING : RadioService.STATE_CONNECTING;
    }

    /**
     * Toggle kararı. ERROR/IDLE ⇒ REPREPARE; playWhenReady ⇒ PAUSE;
     * kullanıcı STALE_PAUSE_MS'ten uzun duraklattıysa ⇒ REPREPARE; aksi ⇒ RESUME.
     */
    public static ToggleAction decideToggle(String playerState, boolean playWhenReady,
                                            long pausedAtElapsed, long nowElapsed) {
        if (RadioService.STATE_ERROR.equals(playerState) || RadioService.STATE_IDLE.equals(playerState)) {
            return ToggleAction.REPREPARE;
        }
        if (playWhenReady) return ToggleAction.PAUSE;
        if (pausedAtElapsed > 0 && nowElapsed - pausedAtElapsed > STALE_PAUSE_MS) {
            return ToggleAction.REPREPARE;
        }
        return ToggleAction.RESUME;
    }

    /** Sayaç yalnız STATE_PLAYING'de ilerler. */
    public static boolean counterShouldRun(String playerState) {
        return RadioService.STATE_PLAYING.equals(playerState);
    }
}

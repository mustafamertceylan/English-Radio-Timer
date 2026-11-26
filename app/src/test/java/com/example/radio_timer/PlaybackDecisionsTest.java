package com.example.radio_timer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.media3.common.Player;

import com.example.radio_timer.PlaybackDecisions.ToggleAction;

import org.junit.Test;

public class PlaybackDecisionsTest {

    private static final int NONE = Player.PLAYBACK_SUPPRESSION_REASON_NONE;
    private static final int FOCUS = Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS;

    // ---- isSuppressed

    @Test
    public void isSuppressed() {
        assertFalse(PlaybackDecisions.isSuppressed(NONE));
        assertTrue(PlaybackDecisions.isSuppressed(FOCUS));
    }

    // ---- resolveState: 8 öncelik dalı

    @Test
    public void resolve_1_noMedia_isIdle_evenWithError() {
        assertEquals(RadioService.STATE_IDLE, PlaybackDecisions.resolveState(
                false, true, true, true, FOCUS, Player.STATE_READY, true));
    }

    @Test
    public void resolve_2_error_beatsSwitchingAndPlayWhenReady() {
        assertEquals(RadioService.STATE_ERROR, PlaybackDecisions.resolveState(
                true, true, true, true, NONE, Player.STATE_READY, true));
    }

    @Test
    public void resolve_3_switchingSource_isConnecting_evenIfNotPlayWhenReady() {
        assertEquals(RadioService.STATE_CONNECTING, PlaybackDecisions.resolveState(
                true, false, true, false, NONE, Player.STATE_IDLE, false));
    }

    @Test
    public void resolve_4_notPlayWhenReady_isPaused_evenIfEnded() {
        assertEquals(RadioService.STATE_PAUSED, PlaybackDecisions.resolveState(
                true, false, false, false, NONE, Player.STATE_READY, true));
        assertEquals(RadioService.STATE_PAUSED, PlaybackDecisions.resolveState(
                true, false, false, false, NONE, Player.STATE_ENDED, true));
    }

    @Test
    public void resolve_5_ended_isError_evenIfSuppressed() {
        assertEquals(RadioService.STATE_ERROR, PlaybackDecisions.resolveState(
                true, false, false, true, FOCUS, Player.STATE_ENDED, true));
    }

    @Test
    public void resolve_6_suppressed_isInterrupted() {
        assertEquals(RadioService.STATE_INTERRUPTED, PlaybackDecisions.resolveState(
                true, false, false, true, FOCUS, Player.STATE_READY, true));
    }

    @Test
    public void resolve_6_suppressedWhileBuffering_isInterrupted() {
        assertEquals(RadioService.STATE_INTERRUPTED, PlaybackDecisions.resolveState(
                true, false, false, true, FOCUS, Player.STATE_BUFFERING, true));
    }

    @Test
    public void resolve_7_buffering_dependsOnReadySincePrepare() {
        assertEquals(RadioService.STATE_BUFFERING, PlaybackDecisions.resolveState(
                true, false, false, true, NONE, Player.STATE_BUFFERING, true));
        assertEquals(RadioService.STATE_CONNECTING, PlaybackDecisions.resolveState(
                true, false, false, true, NONE, Player.STATE_BUFFERING, false));
    }

    @Test
    public void resolve_7_exoIdle_dependsOnReadySincePrepare() {
        assertEquals(RadioService.STATE_CONNECTING, PlaybackDecisions.resolveState(
                true, false, false, true, NONE, Player.STATE_IDLE, false));
        assertEquals(RadioService.STATE_BUFFERING, PlaybackDecisions.resolveState(
                true, false, false, true, NONE, Player.STATE_IDLE, true));
    }

    @Test
    public void resolve_8_ready_isPlaying() {
        assertEquals(RadioService.STATE_PLAYING, PlaybackDecisions.resolveState(
                true, false, false, true, NONE, Player.STATE_READY, true));
    }

    // ---- decideToggle

    @Test
    public void toggle_error_reprepares() {
        assertEquals(ToggleAction.REPREPARE,
                PlaybackDecisions.decideToggle(RadioService.STATE_ERROR, false, 0L, 100_000L));
    }

    @Test
    public void toggle_idle_reprepares() {
        assertEquals(ToggleAction.REPREPARE,
                PlaybackDecisions.decideToggle(RadioService.STATE_IDLE, false, 0L, 100_000L));
    }

    @Test
    public void toggle_interrupted_pauses() {
        assertEquals(ToggleAction.PAUSE,
                PlaybackDecisions.decideToggle(RadioService.STATE_INTERRUPTED, true, 0L, 100_000L));
    }

    @Test
    public void toggle_playingOrConnecting_pauses() {
        assertEquals(ToggleAction.PAUSE,
                PlaybackDecisions.decideToggle(RadioService.STATE_PLAYING, true, 0L, 100_000L));
        assertEquals(ToggleAction.PAUSE,
                PlaybackDecisions.decideToggle(RadioService.STATE_CONNECTING, true, 0L, 100_000L));
    }

    @Test
    public void toggle_afterShortFocusLoss_resumes() {
        // Kısa focus kaybı damga basmaz (pausedAt eski bir kullanıcı duraklatmasından kalmış olabilir
        // ama playWhenReady true olduğu için zaten PAUSE). Burada: kullanıcı 5 sn önce duraklattı.
        assertEquals(ToggleAction.RESUME,
                PlaybackDecisions.decideToggle(RadioService.STATE_PAUSED, false, 95_000L, 100_000L));
    }

    @Test
    public void toggle_neverStamped_resumes() {
        assertEquals(ToggleAction.RESUME,
                PlaybackDecisions.decideToggle(RadioService.STATE_PAUSED, false, 0L, 100_000L));
    }

    @Test
    public void toggle_userPause31s_reprepares() {
        assertEquals(ToggleAction.REPREPARE,
                PlaybackDecisions.decideToggle(RadioService.STATE_PAUSED, false, 69_000L, 100_000L));
    }

    @Test
    public void toggle_userPauseExactly30s_resumes() {
        assertEquals(ToggleAction.RESUME, PlaybackDecisions.decideToggle(
                RadioService.STATE_PAUSED, false, 70_000L, 70_000L + PlaybackDecisions.STALE_PAUSE_MS));
    }

    // ---- counterShouldRun

    @Test
    public void counterShouldRun_onlyPlaying() {
        assertTrue(PlaybackDecisions.counterShouldRun(RadioService.STATE_PLAYING));
        assertFalse(PlaybackDecisions.counterShouldRun(RadioService.STATE_INTERRUPTED));
        assertFalse(PlaybackDecisions.counterShouldRun(RadioService.STATE_BUFFERING));
        assertFalse(PlaybackDecisions.counterShouldRun(RadioService.STATE_CONNECTING));
        assertFalse(PlaybackDecisions.counterShouldRun(RadioService.STATE_PAUSED));
        assertFalse(PlaybackDecisions.counterShouldRun(RadioService.STATE_IDLE));
        assertFalse(PlaybackDecisions.counterShouldRun(RadioService.STATE_ERROR));
        assertFalse(PlaybackDecisions.counterShouldRun(null));
    }
}

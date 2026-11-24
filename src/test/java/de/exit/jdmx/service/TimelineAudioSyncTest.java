package de.exit.jdmx.service;

import de.exit.jdmx.core.model.TimelineTrack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * TDD-Tests für die driftfreie Synchronisation zwischen Audio-Hardware-Clock,
 * Timeline-Clock und Waveform-Positionierung.
 */
class TimelineAudioSyncTest {

    @Test
    @DisplayName("Sollte bei ungeraden Tick-Intervallen über 1000 Ticks keine Sub-Millisekunden verlieren (Zero-Drift Nano-Clock)")
    void shouldNotLoseSubMillisecondsOverManyOddTicks() {
        AudioPlaybackService mockAudio = Mockito.mock(AudioPlaybackService.class);
        when(mockAudio.getMasterPlaybackPositionMs()).thenReturn(-1L); // Stille / kein aktiver Track -> Fallback Nano-Clock

        TimelineService service = new TimelineService(new AudioAnalysisService(), mockAudio);
        TimelineTrack track = service.getTracks().get(0);
        track.setDurationMs(60_000L); // 60 Sekunden lang, damit nicht vorzeitig gestoppt wird

        long startNanos = 1_000_000_000L;
        service.resetClockAnchor(0L, startNanos);
        service.startPlaybackForTest(startNanos);

        // Simuliere 1000 Ticks mit jeweils 25.499.000 ns (= 25,499 ms).
        // Bei der alten Modulo-Logik gingen pro Tick 0,499 ms verloren -> ~499 ms Drift!
        // Mit der Anchor-Logik muss exakt 1000 * 25,499 ms = 25.499 ms herauskommen!
        long currentNanos = startNanos;
        for (int i = 0; i < 1000; i++) {
            currentNanos += 25_499_000L;
            service.advanceClockTick(currentNanos);
        }

        assertThat(service.getCurrentPositionMs()).isEqualTo(25_499L);
    }

    @Test
    @DisplayName("Sollte Timeline-Position exakt auf die aktive Audio-Hardware-Clock einrasten (Phase-Lock)")
    void shouldPhaseLockToAudioHardwareMasterClockWhenTrackPlays() {
        AudioPlaybackService mockAudio = Mockito.mock(AudioPlaybackService.class);
        TimelineService service = new TimelineService(new AudioAnalysisService(), mockAudio);
        TimelineTrack track = service.getTracks().get(0);
        track.setDurationMs(120_000L);

        long startNanos = 5_000_000_000L;
        service.resetClockAnchor(10_000L, startNanos);
        service.startPlaybackForTest(startNanos);

        // Szenario 1: Audio-Hardware hat 200ms Startlatenz und steht noch bei 10.000ms,
        // obwohl die CPU-Uhr bereits 200ms weiter wäre!
        when(mockAudio.getMasterPlaybackPositionMs()).thenReturn(10_000L);
        service.advanceClockTick(startNanos + 200_000_000L);
        // Playhead darf nicht weglaufen -> bleibt exakt bei 10.000ms!
        assertThat(service.getCurrentPositionMs()).isEqualTo(10_000L);

        // Szenario 2: Audio-Hardware spielt und meldet 15.432ms
        when(mockAudio.getMasterPlaybackPositionMs()).thenReturn(15_432L);
        service.advanceClockTick(startNanos + 5_000_000_000L);
        assertThat(service.getCurrentPositionMs()).isEqualTo(15_432L);
    }

    @Test
    @DisplayName("Sollte nach Ende eines Tracks nahtlos auf die verlustfreie Nano-Clock umschalten")
    void shouldSeamlesslySwitchBackToNanoClockAfterTrackEnds() {
        AudioPlaybackService mockAudio = Mockito.mock(AudioPlaybackService.class);
        TimelineService service = new TimelineService(new AudioAnalysisService(), mockAudio);
        TimelineTrack track = service.getTracks().get(0);
        track.setDurationMs(120_000L);

        long t0 = 10_000_000_000L;
        service.resetClockAnchor(0L, t0);
        service.startPlaybackForTest(t0);

        // Audio-Hardware meldet 30.000ms bei t0 + 30s
        long t1 = t0 + 30_000_000_000L;
        when(mockAudio.getMasterPlaybackPositionMs()).thenReturn(30_000L);
        service.advanceClockTick(t1);
        assertThat(service.getCurrentPositionMs()).isEqualTo(30_000L);

        // Track endet -> Stille (-1L) -> 500ms später muss Position exakt 30.500ms sein
        when(mockAudio.getMasterPlaybackPositionMs()).thenReturn(-1L);
        service.advanceClockTick(t1 + 500_000_000L);
        assertThat(service.getCurrentPositionMs()).isEqualTo(30_500L);
    }

    @Test
    @DisplayName("AudioPlaybackService sollte -1L liefern wenn keine aktiven Kanäle spielen")
    void shouldReturnNegativeOneWhenNoAudioChannelsPlaying() {
        AudioPlaybackService playback = new AudioPlaybackService();
        assertThat(playback.getMasterPlaybackPositionMs()).isEqualTo(-1L);
    }
}

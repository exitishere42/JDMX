package de.exit.jdmx.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für Song-Laufzeit und Wellenform-Daten in {@link TimelineTrack}.
 */
class TimelineTrackDurationAndWaveformTest {

    @Test
    @DisplayName("Sollte Standard-Dauer 0 und leere Wellenform initialisieren")
    void shouldInitializeWithDefaultDurationAndNoWaveform() {
        TimelineTrack track = new TimelineTrack("track-1", "Spur 1");

        assertThat(track.getDurationMs()).isZero();
        assertThat(track.getWaveformData()).isNull();
        assertThat(track.hasWaveform()).isFalse();
    }

    @Test
    @DisplayName("Sollte Dauer setzen und negative Werte auf 0 abfangen")
    void shouldSetDurationSafely() {
        TimelineTrack track = new TimelineTrack("track-1", "Spur 1");

        track.setDurationMs(185000L);
        assertThat(track.getDurationMs()).isEqualTo(185000L);

        track.setDurationMs(-500L);
        assertThat(track.getDurationMs()).isZero();
    }

    @Test
    @DisplayName("Sollte Wellenform-Peaks setzen und abrufen")
    void shouldSetAndGetWaveformData() {
        TimelineTrack track = new TimelineTrack("track-1", "Spur 1");
        float[] peaks = new float[]{0.1f, 0.5f, 0.9f, 0.2f};

        track.setWaveformData(peaks);

        assertThat(track.hasWaveform()).isTrue();
        assertThat(track.getWaveformData()).containsExactly(0.1f, 0.5f, 0.9f, 0.2f);

        // Defensive Kopie prüfen
        peaks[0] = 0.99f;
        assertThat(track.getWaveformData()[0]).isEqualTo(0.1f);
    }
}

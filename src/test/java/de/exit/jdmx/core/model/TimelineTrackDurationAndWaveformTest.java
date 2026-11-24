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

    @Test
    @DisplayName("Sollte 3-Band-Wellenformdaten (Low, Mid, High) separat speichern und defensive Kopien anlegen")
    void shouldSetAndGet3BandWaveformData() {
        TimelineTrack track = new TimelineTrack("track-1", "Spur 1");

        assertThat(track.has3BandWaveform()).isFalse();
        assertThat(track.getLowBandData()).isNull();
        assertThat(track.getMidBandData()).isNull();
        assertThat(track.getHighBandData()).isNull();

        float[] low = new float[]{0.8f, 0.9f};
        float[] mid = new float[]{0.4f, 0.5f};
        float[] high = new float[]{0.1f, 0.2f};

        track.set3BandWaveformData(low, mid, high);

        assertThat(track.has3BandWaveform()).isTrue();
        assertThat(track.getLowBandData()).containsExactly(0.8f, 0.9f);
        assertThat(track.getMidBandData()).containsExactly(0.4f, 0.5f);
        assertThat(track.getHighBandData()).containsExactly(0.1f, 0.2f);

        // Defensive Kopie prüfen
        low[0] = 0.0f;
        mid[0] = 0.0f;
        high[0] = 0.0f;

        assertThat(track.getLowBandData()[0]).isEqualTo(0.8f);
        assertThat(track.getMidBandData()[0]).isEqualTo(0.4f);
        assertThat(track.getHighBandData()[0]).isEqualTo(0.1f);
    }

    @Test
    @DisplayName("Sollte Standard-Spurhöhe mit 76 Pixeln initialisieren (Pioneer Rekordbox Skalierung)")
    void shouldInitializeWithDefaultHeightOf76Px() {
        TimelineTrack track = new TimelineTrack("track-1", "Spur 1");

        assertThat(TimelineTrack.DEFAULT_TRACK_HEIGHT).isEqualTo(76.0);
        assertThat(track.getHeight()).isEqualTo(76.0);
    }
}

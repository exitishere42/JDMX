package de.exit.jdmx.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit-Tests für das Datenmodell {@link TimelineTrack}.
 */
class TimelineTrackTest {

    @Test
    @DisplayName("Sollte TimelineTrack mit gültigen Parametern initialisieren")
    void shouldInitializeWithValidParameters() {
        TimelineTrack track = new TimelineTrack("track-1", "Track 1");

        assertThat(track.getId()).isEqualTo("track-1");
        assertThat(track.getName()).isEqualTo("Track 1");
        assertThat(track.isMuted()).isFalse();
        assertThat(track.getHeight()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Sollte Null-Werte bei Initialisierung abfangen")
    void shouldRejectNullParameters() {
        assertThatThrownBy(() -> new TimelineTrack(null, "Track 1"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("id");

        assertThatThrownBy(() -> new TimelineTrack("track-1", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("name");
    }

    @Test
    @DisplayName("Sollte Mute-Status umschalten können")
    void shouldToggleMute() {
        TimelineTrack track = new TimelineTrack("track-1", "Track 1");
        assertThat(track.isMuted()).isFalse();

        boolean muted = track.toggleMute();
        assertThat(muted).isTrue();
        assertThat(track.isMuted()).isTrue();

        muted = track.toggleMute();
        assertThat(muted).isFalse();
        assertThat(track.isMuted()).isFalse();
    }

    @Test
    @DisplayName("Sollte Namen und Höhe aktualisieren können")
    void shouldUpdateNameAndHeight() {
        TimelineTrack track = new TimelineTrack("track-1", "Track 1");

        track.setName("Showtanz");
        assertThat(track.getName()).isEqualTo("Showtanz");

        track.setHeight(64.0);
        assertThat(track.getHeight()).isEqualTo(64.0);
    }

    @Test
    @DisplayName("Sollte equals und hashCode basierend auf ID vergleichen")
    void shouldCompareEqualsAndHashCodeById() {
        TimelineTrack track1 = new TimelineTrack("track-1", "Track 1");
        TimelineTrack track2 = new TimelineTrack("track-1", "Anderer Name");
        TimelineTrack track3 = new TimelineTrack("track-2", "Track 1");

        assertThat(track1).isEqualTo(track2);
        assertThat(track1.hashCode()).isEqualTo(track2.hashCode());
        assertThat(track1).isNotEqualTo(track3);
    }
}

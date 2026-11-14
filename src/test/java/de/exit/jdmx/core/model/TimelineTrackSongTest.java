package de.exit.jdmx.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für Song-Zuweisung und Clip-Eigenschaften in {@link TimelineTrack}.
 */
class TimelineTrackSongTest {

    @Test
    @DisplayName("Sollte standardmäßig keinen Song besitzen")
    void shouldNotHaveSongByDefault() {
        TimelineTrack track = new TimelineTrack("track-1", "Track 1");
        assertThat(track.hasSong()).isFalse();
        assertThat(track.getSongName()).isNull();
        assertThat(track.getSongFile()).isNull();
    }

    @Test
    @DisplayName("Sollte Song-Zuweisung speichern und hasSong() aktualisieren")
    void shouldAssignSong() {
        TimelineTrack track = new TimelineTrack("track-1", "Track 1");
        File songFile = new File("showtanz26.mp3");

        track.setSongFile(songFile);
        track.setSongName("showtanz26.mp3");

        assertThat(track.hasSong()).isTrue();
        assertThat(track.getSongName()).isEqualTo("showtanz26.mp3");
        assertThat(track.getSongFile()).isEqualTo(songFile);
    }

    @Test
    @DisplayName("Sollte Song-Zuweisung entfernen können")
    void shouldClearSong() {
        TimelineTrack track = new TimelineTrack("track-1", "Track 1");
        track.setSongName("showtanz26.mp3");
        assertThat(track.hasSong()).isTrue();

        track.setSongName(null);
        track.setSongFile(null);
        assertThat(track.hasSong()).isFalse();
    }
}

package de.exit.jdmx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit-Tests für die Playback- und Song-Import-Logik des {@link TimelineService}.
 */
class TimelinePlaybackTest {

    @Test
    @DisplayName("Sollte standardmäßig nicht im Playback-Modus sein und Position 0ms haben")
    void shouldHaveInitialPlaybackState() {
        TimelineService service = new TimelineService();

        assertThat(service.isPlaying()).isFalse();
        assertThat(service.getCurrentPositionMs()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Sollte Playback starten und stoppen können")
    void shouldStartAndStopPlayback() {
        TimelineService service = new TimelineService();
        AtomicInteger notifications = new AtomicInteger();
        service.addChangeListener(notifications::incrementAndGet);

        service.startPlayback();
        assertThat(service.isPlaying()).isTrue();
        assertThat(notifications.get()).isGreaterThanOrEqualTo(1);

        service.setPositionMs(2500L);
        assertThat(service.getCurrentPositionMs()).isEqualTo(2500L);

        service.stopPlayback();
        assertThat(service.isPlaying()).isFalse();
        assertThat(service.getCurrentPositionMs()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Sollte Playback pausieren können ohne Position zurückzusetzen")
    void shouldPausePlaybackWithoutResettingPosition() {
        TimelineService service = new TimelineService();

        service.startPlayback();
        service.setPositionMs(1800L);

        service.pausePlayback();
        assertThat(service.isPlaying()).isFalse();
        assertThat(service.getCurrentPositionMs()).isEqualTo(1800L);
    }

    @Test
    @DisplayName("Sollte Songs importieren und verwalten können")
    void shouldImportAndManageSongs() {
        TimelineService service = new TimelineService();
        File dummySong = new File("showtanz26.mp3");

        assertThat(service.getImportedSongs()).isEmpty();

        service.importSong(dummySong);
        assertThat(service.getImportedSongs()).hasSize(1);
        assertThat(service.getImportedSongs().get(0).getName()).isEqualTo("showtanz26.mp3");
        assertThat(service.getActiveSongFile()).isEqualTo(dummySong);

        assertThatThrownBy(() -> service.importSong(null))
                .isInstanceOf(NullPointerException.class);
    }
}

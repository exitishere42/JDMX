package de.exit.jdmx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit-Tests für den {@link AudioPlaybackService}.
 */
class AudioPlaybackServiceTest {

    @Test
    @DisplayName("Sollte standardmäßig nicht abspielen und Position 0 haben")
    void shouldHaveInitialStoppedState() {
        AudioPlaybackService service = new AudioPlaybackService();

        assertThat(service.isPlaying()).isFalse();
        assertThat(service.getCurrentPositionMs()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Sollte null oder nicht existierende Dateien robust und ohne Exception handhaben")
    void shouldHandleNullOrNonExistentFilesGracefully() {
        AudioPlaybackService service = new AudioPlaybackService();

        assertThatCode(() -> service.play(null, 0L)).doesNotThrowAnyException();
        assertThat(service.isPlaying()).isFalse();

        File nonExistent = new File("does_not_exist_xyz123.mp3");
        assertThatCode(() -> service.play(nonExistent, 1000L)).doesNotThrowAnyException();
        assertThat(service.isPlaying()).isFalse();
    }

    @Test
    @DisplayName("Sollte Lautstärke auf 0.0 bis 1.0 begrenzen")
    void shouldClampVolume() {
        AudioPlaybackService service = new AudioPlaybackService();

        service.setVolume(0.5);
        assertThat(service.getVolume()).isEqualTo(0.5);

        service.setVolume(1.5);
        assertThat(service.getVolume()).isEqualTo(1.0);

        service.setVolume(-0.2);
        assertThat(service.getVolume()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Sollte Pause und Stop ohne aktive Wiedergabe sicher ausführen")
    void shouldHandlePauseAndStopWhenNotPlaying() {
        AudioPlaybackService service = new AudioPlaybackService();

        assertThatCode(service::pause).doesNotThrowAnyException();
        assertThatCode(service::stop).doesNotThrowAnyException();
        assertThat(service.isPlaying()).isFalse();
    }

    @Test
    @DisplayName("Sollte Position auch im gestoppten Zustand sauber setzen und speichern")
    void shouldStorePositionWhenStopped() {
        AudioPlaybackService service = new AudioPlaybackService();

        service.setPosition(5000L);
        assertThat(service.getCurrentPositionMs()).isEqualTo(5000L);

        service.seek(12500L);
        assertThat(service.getCurrentPositionMs()).isEqualTo(12500L);

        // Negative Positionen müssen auf 0 geklemmt werden
        service.seek(-500L);
        assertThat(service.getCurrentPositionMs()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Sollte Timeline mit mehreren Spuren parallel wiedergeben und stoppen")
    void shouldSupportMultitrackTimelinePlayback() {
        AudioPlaybackService service = new AudioPlaybackService();

        de.exit.jdmx.core.model.TimelineTrack track1 = new de.exit.jdmx.core.model.TimelineTrack("t1", "Track 1");
        track1.setSongName("track1.mp3");
        track1.setSongFile(new File("track1.mp3"));
        track1.setDurationMs(60_000L);
        track1.setStartTimeMs(0L);

        de.exit.jdmx.core.model.TimelineTrack track2 = new de.exit.jdmx.core.model.TimelineTrack("t2", "Track 2");
        track2.setSongName("track2.mp3");
        track2.setSongFile(new File("track2.mp3"));
        track2.setDurationMs(45_000L);
        track2.setStartTimeMs(15_000L);

        // Sollte robust starten selbst wenn Mock-Dateien auf Platte nicht existieren
        assertThatCode(() -> service.playTimeline(List.of(track1, track2), 5000L)).doesNotThrowAnyException();
        assertThat(service.isPlaying()).isTrue();
        assertThat(service.getCurrentPositionMs()).isEqualTo(5000L);

        // Tick bei 20.000 ms -> track2 Startfenster erreicht
        assertThatCode(() -> service.onTimelineTick(20_000L)).doesNotThrowAnyException();
        assertThat(service.getCurrentPositionMs()).isEqualTo(20_000L);

        // Stop -> alles bereinigt
        service.stop();
        assertThat(service.isPlaying()).isFalse();
        assertThat(service.getCurrentPositionMs()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Sollte einzelne Spur bei Verschiebung während Wiedergabe sofort oder debounced resyncen")
    void shouldResyncSingleTrackWhenMovedDuringPlayback() {
        AudioPlaybackService service = new AudioPlaybackService();

        de.exit.jdmx.core.model.TimelineTrack track1 = new de.exit.jdmx.core.model.TimelineTrack("t1", "Track 1");
        track1.setSongName("track1.mp3");
        track1.setSongFile(new File("track1.mp3"));
        track1.setDurationMs(60_000L);
        track1.setStartTimeMs(0L);

        service.playTimeline(List.of(track1), 5000L);
        assertThat(service.isPlaying()).isTrue();

        // Spur wird nach hinten verschoben (z.B. Startzeit 10.000 ms, Timeline ist bei 5.000 ms -> außerhalb)
        track1.setStartTimeMs(10_000L);
        assertThatCode(() -> service.resyncTrackImmediate(track1, 5000L)).doesNotThrowAnyException();

        // Spur wird wieder nach vorne verschoben (Startzeit 2.000 ms, Timeline ist bei 5.000 ms -> innerhalb mit Offset 3.000 ms)
        track1.setStartTimeMs(2_000L);
        assertThatCode(() -> service.resyncTrackDebounced(track1, 5000L)).doesNotThrowAnyException();
        assertThatCode(() -> service.resyncTrackImmediate(track1, 5000L)).doesNotThrowAnyException();

        service.stop();
        assertThat(service.isPlaying()).isFalse();
    }
}

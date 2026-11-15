package de.exit.jdmx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

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
}

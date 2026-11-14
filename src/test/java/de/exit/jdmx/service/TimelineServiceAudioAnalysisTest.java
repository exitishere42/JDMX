package de.exit.jdmx.service;

import de.exit.jdmx.core.model.TimelineTrack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für Audioanalyse-Integration und Timeline-Dauer in {@link TimelineService}.
 */
class TimelineServiceAudioAnalysisTest {

    @Test
    @DisplayName("Sollte beim Zuweisen eines echten Songs Dauer und Wellenform auf Track übertragen")
    void shouldAssignRealAudioDataToTrack(@TempDir Path tempDir) throws IOException {
        TimelineService service = new TimelineService();
        TimelineTrack track = service.getTracks().get(0);

        File songFile = tempDir.resolve("mysong.wav").toFile();
        createSyntheticWavFile(songFile, 44100, 2.0); // 2.0 Sekunden

        boolean assigned = service.assignSongToTrack(track.getId(), songFile);

        assertThat(assigned).isTrue();
        assertThat(track.hasSong()).isTrue();
        assertThat(track.getDurationMs()).isBetween(1900L, 2100L);
        assertThat(track.hasWaveform()).isTrue();
        assertThat(track.getWaveformData()).isNotEmpty();
        assertThat(service.getMaxTimelineDurationMs()).isEqualTo(track.getDurationMs());
    }

    @Test
    @DisplayName("Sollte für Dummy-Dateien ohne Fehler sichere Fallback-Dauer setzen")
    void shouldHandleNonExistentFileGracefully() {
        TimelineService service = new TimelineService();
        TimelineTrack track = service.getTracks().get(0);

        File dummyFile = new File("non_existent_show.mp3");
        boolean assigned = service.assignSongToTrack(track.getId(), dummyFile);

        assertThat(assigned).isTrue();
        assertThat(track.hasSong()).isTrue();
        // Fallback-Dauer mindestens 60 Sekunden für Timeline-Ansicht
        assertThat(track.getDurationMs()).isGreaterThanOrEqualTo(60_000L);
    }

    private void createSyntheticWavFile(File file, int sampleRate, double durationSec) throws IOException {
        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
        int totalSamples = (int) (sampleRate * durationSec);
        byte[] pcmData = new byte[totalSamples * 2];

        for (int i = 0; i < totalSamples; i++) {
            short sample = (short) (Math.sin(2.0 * Math.PI * i * 440.0 / sampleRate) * 25000.0);
            pcmData[i * 2] = (byte) (sample & 0xFF);
            pcmData[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(pcmData);
             AudioInputStream ais = new AudioInputStream(bais, format, totalSamples)) {
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, file);
        }
    }
}

package de.exit.jdmx.service;

import de.exit.jdmx.core.model.AudioAnalysisResult;
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
 * Unit-Tests für den {@link AudioAnalysisService} (Dauer- und Wellenform-Analyse).
 */
class AudioAnalysisServiceTest {

    private final AudioAnalysisService service = new AudioAnalysisService();

    @Test
    @DisplayName("Sollte leeres Ergebnis für null oder nicht existierende Datei liefern")
    void shouldReturnEmptyResultForMissingFile() {
        AudioAnalysisResult resNull = service.analyze(null);
        assertThat(resNull).isNotNull();
        assertThat(resNull.durationMs()).isZero();
        assertThat(resNull.waveformPeaks()).isEmpty();

        AudioAnalysisResult resMissing = service.analyze(new File("nicht-vorhanden.wav"));
        assertThat(resMissing).isNotNull();
        assertThat(resMissing.durationMs()).isZero();
        assertThat(resMissing.waveformPeaks()).isEmpty();
    }

    @Test
    @DisplayName("Sollte Dauer und Wellenform für eine echte WAV-Audiodatei berechnen")
    void shouldAnalyzeRealWavFile(@TempDir Path tempDir) throws IOException {
        File wavFile = tempDir.resolve("test_sine.wav").toFile();
        createSyntheticWavFile(wavFile, 44100, 1.5); // 1.5 Sekunden Audio

        AudioAnalysisResult result = service.analyze(wavFile);

        assertThat(result).isNotNull();
        // 1.5 Sekunden = ca. 1500 ms (Toleranz 100ms)
        assertThat(result.durationMs()).isBetween(1400L, 1600L);
        assertThat(result.sampleRate()).isEqualTo(44100);
        assertThat(result.waveformPeaks()).isNotEmpty();

        // Alle Peak-Werte müssen normalisiert zwischen 0.0 und 1.0 liegen
        for (float peak : result.waveformPeaks()) {
            assertThat(peak).isBetween(0.0f, 1.0f);
        }
    }

    @Test
    @DisplayName("Sollte Resampling von Peaks auf Zielgröße korrekt ausführen")
    void shouldResampleWaveformPeaksCorrectly() {
        float[] original = new float[]{0.1f, 0.8f, 0.4f, 0.9f, 0.2f, 0.5f};
        float[] resampled = service.resamplePeaks(original, 3);

        assertThat(resampled).hasSize(3);
        // Erstes Drittel (0.1, 0.8) -> Max 0.8
        assertThat(resampled[0]).isEqualTo(0.8f);
        // Zweites Drittel (0.4, 0.9) -> Max 0.9
        assertThat(resampled[1]).isEqualTo(0.9f);
        // Drittes Drittel (0.2, 0.5) -> Max 0.5
        assertThat(resampled[2]).isEqualTo(0.5f);
    }

    /**
     * Erstellt eine synthetische WAV-Datei mit Sinuston.
     */
    private void createSyntheticWavFile(File file, int sampleRate, double durationSec) throws IOException {
        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
        int totalSamples = (int) (sampleRate * durationSec);
        byte[] pcmData = new byte[totalSamples * 2];

        double freq = 440.0; // 440 Hz Kammerton A
        for (int i = 0; i < totalSamples; i++) {
            double angle = 2.0 * Math.PI * i * freq / sampleRate;
            short sample = (short) (Math.sin(angle) * 30000.0);
            pcmData[i * 2] = (byte) (sample & 0xFF);
            pcmData[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(pcmData);
             AudioInputStream ais = new AudioInputStream(bais, format, totalSamples)) {
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, file);
        }
    }
}

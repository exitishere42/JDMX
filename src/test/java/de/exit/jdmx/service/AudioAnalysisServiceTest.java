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
        // Resampling sollte normalisierte und dynamisch gespreizte Werte liefern
        assertThat(resampled[0]).isBetween(0.5f, 1.0f);
        assertThat(resampled[1]).isBetween(0.7f, 1.0f);
        assertThat(resampled[2]).isBetween(0.2f, 0.7f);
    }

    @Test
    @DisplayName("Sollte asynchrone Analyse über CompletableFuture zuverlässig ausführen")
    void shouldAnalyzeAsyncSuccessfully(@TempDir Path tempDir) throws Exception {
        File wavFile = tempDir.resolve("async_test.wav").toFile();
        createSyntheticWavFile(wavFile, 44100, 1.0);

        var future = service.analyzeAsync(wavFile);
        assertThat(future).isNotNull();

        AudioAnalysisResult result = future.get();
        assertThat(result).isNotNull();
        assertThat(result.durationMs()).isBetween(900L, 1100L);
        assertThat(result.hasWaveform()).isTrue();
    }

    @Test
    @DisplayName("Sollte leise und laute Abschnitte mit deutlicher Dynamik-Differenz abbilden")
    void shouldReflectDynamicSpikesBetweenQuietAndLoudSections(@TempDir Path tempDir) throws Exception {
        File wavFile = tempDir.resolve("dynamic_test.wav").toFile();
        // 1 Sekunde leise (Amplitude 3000), 1 Sekunde laut (Amplitude 30000)
        AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
        int halfSamples = 44100;
        byte[] pcmData = new byte[halfSamples * 4];

        for (int i = 0; i < halfSamples; i++) {
            short quietSample = (short) (Math.sin(2.0 * Math.PI * i * 440.0 / 44100) * 3000.0);
            pcmData[i * 2] = (byte) (quietSample & 0xFF);
            pcmData[i * 2 + 1] = (byte) ((quietSample >> 8) & 0xFF);
        }
        for (int i = 0; i < halfSamples; i++) {
            int offset = (halfSamples + i) * 2;
            short loudSample = (short) (Math.sin(2.0 * Math.PI * i * 440.0 / 44100) * 30000.0);
            pcmData[offset] = (byte) (loudSample & 0xFF);
            pcmData[offset + 1] = (byte) ((loudSample >> 8) & 0xFF);
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(pcmData);
             AudioInputStream ais = new AudioInputStream(bais, format, halfSamples * 2)) {
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wavFile);
        }

        AudioAnalysisResult result = service.analyze(wavFile);
        assertThat(result.hasWaveform()).isTrue();
        float[] peaks = result.waveformPeaks();

        // Erstes Viertel (leise) muss signifikant kleiner sein als letztes Viertel (laut)
        float quietAvg = (peaks[0] + peaks[peaks.length / 4]) / 2.0f;
        float loudAvg = (peaks[peaks.length * 3 / 4] + peaks[peaks.length - 1]) / 2.0f;

        assertThat(loudAvg).isGreaterThan(quietAvg * 2.0f);
    }

    @Test
    @DisplayName("Sollte 3-Band Frequenzen (Bass, Mitten, Höhen) für Rekordbox-Wellenform extrahieren")
    void shouldExtract3BandFrequencies(@TempDir Path tempDir) throws IOException {
        File lowFile = tempDir.resolve("bass_100hz.wav").toFile();
        File highFile = tempDir.resolve("treble_6000hz.wav").toFile();

        createSyntheticToneFile(lowFile, 44100, 1.0, 100.0);   // 100 Hz Subbass / Kick
        createSyntheticToneFile(highFile, 44100, 1.0, 6000.0); // 6000 Hz Hi-Hat / Cymbals

        AudioAnalysisResult lowResult = service.analyze(lowFile);
        AudioAnalysisResult highResult = service.analyze(highFile);

        assertThat(lowResult.has3BandWaveform()).isTrue();
        assertThat(highResult.has3BandWaveform()).isTrue();

        float avgLowInBass = average(lowResult.lowBand());
        float avgHighInBass = average(lowResult.highBand());
        assertThat(avgLowInBass).isGreaterThan(avgHighInBass * 2.0f);

        float avgLowInTreble = average(highResult.lowBand());
        float avgHighInTreble = average(highResult.highBand());
        assertThat(avgHighInTreble).isGreaterThan(avgLowInTreble * 2.0f);
    }

    private float average(float[] arr) {
        if (arr == null || arr.length == 0) return 0.0f;
        float sum = 0.0f;
        for (float v : arr) sum += v;
        return sum / arr.length;
    }

    private void createSyntheticToneFile(File file, int sampleRate, double durationSec, double freq) throws IOException {
        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
        int totalSamples = (int) (sampleRate * durationSec);
        byte[] pcmData = new byte[totalSamples * 2];

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

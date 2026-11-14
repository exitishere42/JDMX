package de.exit.jdmx.service;

import de.exit.jdmx.core.model.AudioAnalysisResult;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Service für die präzise Extraktion von Audiodaten (Gesamtlaufzeit und echte Wellenform-Peaks).
 * <p>
 * Unterstützt:
 * <ul>
 *     <li><b>WAV-Dateien:</b> Native Dekodierung über {@link AudioSystem} und PCM-Sample-Auswertung</li>
 *     <li><b>MP3-Dateien:</b> Reine plattformunabhängige Java-Dekodierung via JLayer (MPEG Layer III)</li>
 * </ul>
 * Liefert normalisierte Peak-Amplituden (0.0f bis 1.0f) für hochauflösende DAW-Wellenformdarstellung.
 * </p>
 */
public class AudioAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AudioAnalysisService.class);

    /** Standardanzahl von Wellenform-Punkten für die gerenderte Hüllkurve. */
    public static final int DEFAULT_WAVEFORM_POINTS = 1000;

    /**
     * Analysiert eine Audiodatei (WAV oder MP3) und liefert Dauer sowie Wellenform-Peaks.
     *
     * @param audioFile die zu analysierende Datei (darf {@code null} sein)
     * @return {@link AudioAnalysisResult} mit berechneten Werten oder leeres Ergebnis bei Fehlern
     */
    public AudioAnalysisResult analyze(File audioFile) {
        if (audioFile == null || !audioFile.exists() || !audioFile.isFile() || audioFile.length() == 0) {
            return AudioAnalysisResult.empty();
        }

        String name = audioFile.getName().toLowerCase();
        try {
            if (name.endsWith(".wav")) {
                return analyzeWav(audioFile);
            } else if (name.endsWith(".mp3")) {
                return analyzeMp3(audioFile);
            } else {
                // Fallback: Zuerst WAV versuchen, dann MP3
                try {
                    return analyzeWav(audioFile);
                } catch (Exception e) {
                    return analyzeMp3(audioFile);
                }
            }
        } catch (Exception e) {
            log.warn("Fehler bei der Audioanalyse von '{}': {}", audioFile.getName(), e.getMessage());
            return AudioAnalysisResult.empty();
        }
    }

    /**
     * Analysiert eine WAV-Datei über das Java Sound API.
     */
    private AudioAnalysisResult analyzeWav(File wavFile) throws Exception {
        try (AudioInputStream rawAis = AudioSystem.getAudioInputStream(wavFile)) {
            AudioFormat baseFormat = rawAis.getFormat();

            AudioFormat decodedFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false // little endian
            );

            try (AudioInputStream ais = AudioSystem.getAudioInputStream(decodedFormat, rawAis)) {
                int channels = decodedFormat.getChannels();
                float sampleRate = decodedFormat.getSampleRate();
                long frameLength = ais.getFrameLength();

                long durationMs = 0L;
                if (frameLength > 0 && sampleRate > 0) {
                    durationMs = Math.round((frameLength / (double) sampleRate) * 1000.0);
                }

                List<Float> peaks = new ArrayList<>();
                byte[] buffer = new byte[4096 * channels * 2];
                int bytesRead;
                long totalFramesRead = 0;

                while ((bytesRead = ais.read(buffer)) != -1) {
                    int samplesInChunk = bytesRead / 2;
                    float chunkPeak = 0.0f;

                    for (int i = 0; i < samplesInChunk; i++) {
                        int byteIdx = i * 2;
                        short sample = (short) ((buffer[byteIdx] & 0xFF) | (buffer[byteIdx + 1] << 8));
                        float normalized = Math.abs(sample) / 32768.0f;
                        if (normalized > chunkPeak) {
                            chunkPeak = normalized;
                        }
                    }

                    peaks.add(chunkPeak);
                    totalFramesRead += (bytesRead / (channels * 2));
                }

                if (durationMs <= 0 && sampleRate > 0 && totalFramesRead > 0) {
                    durationMs = Math.round((totalFramesRead / (double) sampleRate) * 1000.0);
                }

                float[] waveform = resamplePeaks(toFloatArray(peaks), DEFAULT_WAVEFORM_POINTS);
                return new AudioAnalysisResult(durationMs, waveform, (int) sampleRate);
            }
        }
    }

    /**
     * Analysiert eine MP3-Datei über den reinen Java JLayer-Dekoder.
     * Überspringt ID3v2-Tags und fehlerhafte Sync-Header tolerant, ohne abzubrechen.
     */
    private AudioAnalysisResult analyzeMp3(File mp3File) {
        try (InputStream in = new BufferedInputStream(new FileInputStream(mp3File))) {
            Bitstream bitstream = new Bitstream(in);
            Decoder decoder = new Decoder();

            List<Float> framePeaks = new ArrayList<>();
            int sampleRate = 44100;
            long totalFrames = 0;
            double msPerFrame = 26.122; // Standard für 44.1kHz Layer 3

            while (true) {
                Header header;
                try {
                    header = bitstream.readFrame();
                } catch (Exception e) {
                    try {
                        bitstream.closeFrame();
                    } catch (Exception ignored) {
                    }
                    continue;
                }
                if (header == null) {
                    break;
                }

                totalFrames++;
                sampleRate = header.frequency();
                msPerFrame = header.ms_per_frame();

                try {
                    SampleBuffer output = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                    if (output != null) {
                        short[] buffer = output.getBuffer();
                        int len = output.getBufferLength();
                        float maxPeak = 0.0f;

                        for (int i = 0; i < len; i++) {
                            float absVal = Math.abs(buffer[i]) / 32768.0f;
                            if (absVal > maxPeak) {
                                maxPeak = absVal;
                            }
                        }
                        framePeaks.add(maxPeak);
                    }
                } catch (Exception ignored) {
                    // Einzelner Frame nicht dekodierbar -> überspringen
                } finally {
                    try {
                        bitstream.closeFrame();
                    } catch (Exception ignored) {
                    }
                }
            }

            long durationMs = Math.round(totalFrames * msPerFrame);
            if (durationMs <= 0 && mp3File.length() > 0) {
                // Heuristik: 192 kbit/s = ~24.000 Bytes/s
                durationMs = Math.max(10_000L, (mp3File.length() * 1000L) / 24000L);
            }

            float[] waveform = resamplePeaks(toFloatArray(framePeaks), DEFAULT_WAVEFORM_POINTS);
            return new AudioAnalysisResult(durationMs, waveform, sampleRate);
        } catch (Exception e) {
            log.warn("Fehler beim MP3-Parsing von '{}': {}", mp3File.getName(), e.getMessage());
            long fallbackDuration = mp3File.length() > 0 ? Math.max(10_000L, (mp3File.length() * 1000L) / 24000L) : 60_000L;
            return new AudioAnalysisResult(fallbackDuration, new float[0], 44100);
        }
    }

    /**
     * Reduziert oder interpoliert eine Reihe von Peak-Werten auf eine Zielanzahl von Punkten.
     * Nutzt Peak-Detection (Maximum pro Block), damit keine akustischen Transienten verloren gehen.
     *
     * @param original     originale Peak-Werte
     * @param targetPoints Zielanzahl an Punkten
     * @return resampletes Array mit genau {@code targetPoints} Werten
     */
    public float[] resamplePeaks(float[] original, int targetPoints) {
        if (original == null || original.length == 0 || targetPoints <= 0) {
            return new float[0];
        }
        if (original.length <= targetPoints) {
            float[] copy = new float[original.length];
            System.arraycopy(original, 0, copy, 0, original.length);
            return copy;
        }

        float[] result = new float[targetPoints];
        double step = (double) original.length / targetPoints;

        for (int i = 0; i < targetPoints; i++) {
            int start = (int) (i * step);
            int end = (int) Math.min(original.length, (i + 1) * step);
            float max = 0.0f;
            for (int j = start; j < end; j++) {
                if (original[j] > max) {
                    max = original[j];
                }
            }
            result[i] = max;
        }

        return result;
    }

    private float[] toFloatArray(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }
}

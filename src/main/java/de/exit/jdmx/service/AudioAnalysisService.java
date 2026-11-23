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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service für die präzise Extraktion von Audiodaten (Gesamtlaufzeit und echte Wellenform-Peaks).
 * <p>
 * Unterstützt:
 * <ul>
 *     <li><b>WAV-Dateien:</b> Native Dekodierung über {@link AudioSystem} und PCM-Sample-Auswertung</li>
 *     <li><b>MP3-Dateien:</b> Reine plattformunabhängige Java-Dekodierung via JLayer (MPEG Layer III)</li>
 * </ul>
 * Liefert normalisierte Peak- und RMS-Amplituden (0.0f bis 1.0f) mit Dynamikspreizung für
 * hochauflösende DAW-Wellenformdarstellung.
 * </p>
 */
public class AudioAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AudioAnalysisService.class);

    /** Standardanzahl von Wellenform-Punkten für die gerenderte Hüllkurve. */
    public static final int DEFAULT_WAVEFORM_POINTS = 1000;

    /** Hintergrund-Executor für nicht blockierende Audioanalysen. */
    private static final ExecutorService ANALYSIS_EXECUTOR = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "Audio-Analysis-Worker");
        t.setDaemon(true);
        return t;
    });

    /**
     * Startet eine asynchrone Audioanalyse auf einem Hintergrund-Daemon-Thread,
     * sodass der JavaFX Application Thread niemals blockiert wird.
     *
     * @param audioFile die zu analysierende Datei
     * @return {@link CompletableFuture} mit dem Analyseergebnis
     */
    public CompletableFuture<AudioAnalysisResult> analyzeAsync(File audioFile) {
        return CompletableFuture.supplyAsync(() -> analyze(audioFile), ANALYSIS_EXECUTOR);
    }

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
                List<Float> lowPeaks = new ArrayList<>();
                List<Float> midPeaks = new ArrayList<>();
                List<Float> highPeaks = new ArrayList<>();

                ThreeBandFilter filter = new ThreeBandFilter(sampleRate);
                float[] bands = new float[3];
                byte[] buffer = new byte[4096 * channels * 2];
                int bytesRead;
                long totalFramesRead = 0;

                while ((bytesRead = ais.read(buffer)) != -1) {
                    int samplesInChunk = bytesRead / 2;
                    float chunkPeak = 0.0f;
                    double sumSq = 0.0;
                    float chunkLow = 0.0f;
                    float chunkMid = 0.0f;
                    float chunkHigh = 0.0f;

                    for (int i = 0; i < samplesInChunk; i++) {
                        int byteIdx = i * 2;
                        short sample = (short) ((buffer[byteIdx] & 0xFF) | (buffer[byteIdx + 1] << 8));
                        float normalized = sample / 32768.0f;
                        float absNorm = Math.abs(normalized);
                        sumSq += absNorm * absNorm;
                        if (absNorm > chunkPeak) {
                            chunkPeak = absNorm;
                        }

                        filter.process(normalized, bands);
                        float absL = Math.abs(bands[0]);
                        float absM = Math.abs(bands[1]);
                        float absH = Math.abs(bands[2]);
                        if (absL > chunkLow) chunkLow = absL;
                        if (absM > chunkMid) chunkMid = absM;
                        if (absH > chunkHigh) chunkHigh = absH;
                    }

                    float rms = (float) Math.sqrt(sumSq / Math.max(1, samplesInChunk));
                    // 35% Peak für Transienten-Spikes, 65% RMS für Lautstärke-Dynamik
                    float combined = (0.35f * chunkPeak) + (0.65f * rms);
                    peaks.add(combined);
                    lowPeaks.add(chunkLow);
                    midPeaks.add(chunkMid);
                    highPeaks.add(chunkHigh);
                    totalFramesRead += (bytesRead / (channels * 2));
                }

                if (durationMs <= 0 && sampleRate > 0 && totalFramesRead > 0) {
                    durationMs = Math.round((totalFramesRead / (double) sampleRate) * 1000.0);
                }

                float[] rawPeaks = toFloatArray(peaks);
                float refMax = 0.0f;
                for (float v : rawPeaks) {
                    if (v > refMax) refMax = v;
                }
                float[] waveform = resamplePeaks(rawPeaks, DEFAULT_WAVEFORM_POINTS, refMax);
                float[] lowBand = resamplePeaks(toFloatArray(lowPeaks), DEFAULT_WAVEFORM_POINTS, refMax);
                float[] midBand = resamplePeaks(toFloatArray(midPeaks), DEFAULT_WAVEFORM_POINTS, refMax);
                float[] highBand = resamplePeaks(toFloatArray(highPeaks), DEFAULT_WAVEFORM_POINTS, refMax);
                return new AudioAnalysisResult(durationMs, waveform, (int) sampleRate, lowBand, midBand, highBand);
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
            List<Float> lowPeaks = new ArrayList<>();
            List<Float> midPeaks = new ArrayList<>();
            List<Float> highPeaks = new ArrayList<>();

            ThreeBandFilter filter = null;
            float[] bands = new float[3];
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
                if (filter == null) {
                    filter = new ThreeBandFilter(sampleRate);
                }

                try {
                    SampleBuffer output = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                    if (output != null) {
                        short[] buffer = output.getBuffer();
                        int len = output.getBufferLength();
                        float maxPeak = 0.0f;
                        double sumSq = 0.0;
                        float chunkLow = 0.0f;
                        float chunkMid = 0.0f;
                        float chunkHigh = 0.0f;

                        for (int i = 0; i < len; i++) {
                            float normalized = buffer[i] / 32768.0f;
                            float absVal = Math.abs(normalized);
                            sumSq += absVal * absVal;
                            if (absVal > maxPeak) {
                                maxPeak = absVal;
                            }

                            filter.process(normalized, bands);
                            float absL = Math.abs(bands[0]);
                            float absM = Math.abs(bands[1]);
                            float absH = Math.abs(bands[2]);
                            if (absL > chunkLow) chunkLow = absL;
                            if (absM > chunkMid) chunkMid = absM;
                            if (absH > chunkHigh) chunkHigh = absH;
                        }
                        float rms = (float) Math.sqrt(sumSq / Math.max(1, len));
                        float combined = (0.35f * maxPeak) + (0.65f * rms);
                        framePeaks.add(combined);
                        lowPeaks.add(chunkLow);
                        midPeaks.add(chunkMid);
                        highPeaks.add(chunkHigh);
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

            float[] rawPeaks = toFloatArray(framePeaks);
            float refMax = 0.0f;
            for (float v : rawPeaks) {
                if (v > refMax) refMax = v;
            }
            float[] waveform = resamplePeaks(rawPeaks, DEFAULT_WAVEFORM_POINTS, refMax);
            float[] lowBand = resamplePeaks(toFloatArray(lowPeaks), DEFAULT_WAVEFORM_POINTS, refMax);
            float[] midBand = resamplePeaks(toFloatArray(midPeaks), DEFAULT_WAVEFORM_POINTS, refMax);
            float[] highBand = resamplePeaks(toFloatArray(highPeaks), DEFAULT_WAVEFORM_POINTS, refMax);
            return new AudioAnalysisResult(durationMs, waveform, sampleRate, lowBand, midBand, highBand);
        } catch (Exception e) {
            log.warn("Fehler beim MP3-Parsing von '{}': {}", mp3File.getName(), e.getMessage());
            long fallbackDuration = mp3File.length() > 0 ? Math.max(10_000L, (mp3File.length() * 1000L) / 24000L) : 60_000L;
            return new AudioAnalysisResult(fallbackDuration, new float[0], 44100);
        }
    }

    /**
     * Reduziert oder interpoliert eine Reihe von Peak-Werten auf eine Zielanzahl von Punkten.
     * Nutzt Peak- und RMS-Detection mit anschließender Dynamikspreizung (Power-Curve),
     * damit leise Passagen schmal abfallen und laute Hits/Spikes dynamisch hervortreten.
     *
     * @param original     originale Peak-Werte
     * @param targetPoints Zielanzahl an Punkten
     * @return resampletes Array mit genau {@code targetPoints} Werten
     */
    public float[] resamplePeaks(float[] original, int targetPoints) {
        return resamplePeaks(original, targetPoints, 0.0f);
    }

    /**
     * Reduziert oder interpoliert eine Reihe von Peak-Werten auf eine Zielanzahl von Punkten
     * relativ zu einer Referenz-Spitzenamplitude (verhindert das künstliche Aufblasen leiser Frequenzbänder).
     *
     * @param original     originale Peak-Werte
     * @param targetPoints Zielanzahl an Punkten
     * @param referenceMax optionale Referenz-Maximalamplitude (> 0)
     * @return resampletes Array mit genau {@code targetPoints} Werten
     */
    public float[] resamplePeaks(float[] original, int targetPoints, float referenceMax) {
        if (original == null || original.length == 0 || targetPoints <= 0) {
            return new float[0];
        }

        float[] raw;
        if (original.length <= targetPoints) {
            raw = new float[original.length];
            System.arraycopy(original, 0, raw, 0, original.length);
        } else {
            raw = new float[targetPoints];
            double step = (double) original.length / targetPoints;

            for (int i = 0; i < targetPoints; i++) {
                int start = (int) (i * step);
                int end = (int) Math.min(original.length, (i + 1) * step);
                float max = 0.0f;
                double sum = 0.0;
                int count = 0;
                for (int j = start; j < end; j++) {
                    float val = original[j];
                    sum += val;
                    count++;
                    if (val > max) {
                        max = val;
                    }
                }
                float avg = count > 0 ? (float) (sum / count) : 0.0f;
                // Mix aus Spitzenwert und Block-Durchschnitt
                raw[i] = (0.4f * max) + (0.6f * avg);
            }
        }

        // Dynamik-Spreizung (Dynamic Range Expansion)
        float globalMax = 0.0f;
        for (float v : raw) {
            if (v > globalMax) {
                globalMax = v;
            }
        }

        float normBase = referenceMax > 0.001f ? Math.max(referenceMax, globalMax) : globalMax;

        float[] result = new float[raw.length];
        if (normBase > 0.001f) {
            for (int i = 0; i < raw.length; i++) {
                float norm = Math.min(1.0f, raw[i] / normBase);
                // Exponent 1.35 lässt Täler subtil absinken und Spikes kräftig hervortreten
                result[i] = (float) Math.pow(norm, 1.35);
            }
        } else {
            System.arraycopy(raw, 0, result, 0, raw.length);
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

    /**
     * Schneller 3-Band Frequenzfilter für Rekordbox-Wellenformanalyse.
     * Teilt Audiosignale in Low (< 250 Hz, Bass), Mid (250 Hz - 3000 Hz, Mitten)
     * und High (> 3000 Hz, Höhen) auf.
     */
    public static class ThreeBandFilter {
        private final float alphaLow;
        private final float alphaHigh;
        private float low = 0.0f;
        private float lowHigh = 0.0f;

        /**
         * Erstellt einen neuen {@link ThreeBandFilter} für die angegebene Samplerate.
         *
         * @param sampleRate Abtastrate in Hz (z. B. 44100)
         */
        public ThreeBandFilter(float sampleRate) {
            float sRate = Math.max(8000.0f, sampleRate);
            this.alphaLow = (float) Math.min(1.0, 2.0 * Math.PI * 250.0 / sRate);
            this.alphaHigh = (float) Math.min(1.0, 2.0 * Math.PI * 3000.0 / sRate);
        }

        /**
         * Verarbeitet ein einzelnes normalisiertes Sample (-1.0f bis +1.0f) und schreibt
         * die gefilterten Amplituden in das Ausgabearray (Index 0 = Low, 1 = Mid, 2 = High).
         *
         * @param sample Eingangssample
         * @param out    Ausgabearray der Länge 3
         */
        public void process(float sample, float[] out) {
            low += alphaLow * (sample - low);
            lowHigh += alphaHigh * (sample - lowHigh);
            float high = sample - lowHigh;
            float mid = sample - low - high;

            out[0] = low;
            out[1] = mid;
            out[2] = high;
        }
    }
}

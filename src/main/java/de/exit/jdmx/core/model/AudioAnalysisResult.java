package de.exit.jdmx.core.model;

import java.util.Objects;

/**
 * Unveränderliches Ergebnis der Audioanalyse einer Song-Datei.
 * <p>
 * Enthält:
 * <ul>
 *     <li>Die Gesamtlaufzeit des Songs in Millisekunden</li>
 *     <li>Die normalisierten Wellenform-Peaks (Werte zwischen 0.0f und 1.0f)</li>
 *     <li>Die Abtastrate (Sample-Rate in Hz)</li>
 * </ul>
 * </p>
 *
 * @param durationMs    Gesamtlaufzeit des Songs in Millisekunden (>= 0)
 * @param waveformPeaks Normalisierte Wellenform-Amplituden (darf nicht {@code null} sein)
 * @param sampleRate    Abtastrate in Hz (z. B. 44100)
 */
public record AudioAnalysisResult(
        long durationMs,
        float[] waveformPeaks,
        int sampleRate
) {

    /**
     * Erstellt ein neues {@link AudioAnalysisResult} mit Null-Safety.
     */
    public AudioAnalysisResult {
        waveformPeaks = Objects.requireNonNull(waveformPeaks, "waveformPeaks must not be null");
        durationMs = Math.max(0L, durationMs);
        sampleRate = Math.max(1, sampleRate);
    }

    /**
     * Liefert ein leeres Standard-Ergebnis (z. B. als Fallback für nicht analysierbare Dateien).
     *
     * @return leeres {@link AudioAnalysisResult}
     */
    public static AudioAnalysisResult empty() {
        return new AudioAnalysisResult(0L, new float[0], 44100);
    }

    /**
     * Prüft, ob echte Wellenform-Daten vorhanden sind.
     *
     * @return {@code true}, wenn Peaks vorhanden sind, sonst {@code false}
     */
    public boolean hasWaveform() {
        return waveformPeaks.length > 0;
    }
}

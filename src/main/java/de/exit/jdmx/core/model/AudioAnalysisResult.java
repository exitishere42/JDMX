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
 *     <li>Pioneer Rekordbox 3-Band-Frequenzen: Low (Bass), Mid (Mitten), High (Höhen)</li>
 * </ul>
 * </p>
 *
 * @param durationMs    Gesamtlaufzeit des Songs in Millisekunden (>= 0)
 * @param waveformPeaks Normalisierte Wellenform-Amplituden (darf nicht {@code null} sein)
 * @param sampleRate    Abtastrate in Hz (z. B. 44100)
 * @param lowBand       Bass-Frequenzen (< 250 Hz, darf nicht {@code null} sein)
 * @param midBand       Mitten-Frequenzen (250 Hz - 3000 Hz, darf nicht {@code null} sein)
 * @param highBand      Höhen-Frequenzen (> 3000 Hz, darf nicht {@code null} sein)
 */
public record AudioAnalysisResult(
        long durationMs,
        float[] waveformPeaks,
        int sampleRate,
        float[] lowBand,
        float[] midBand,
        float[] highBand
) {

    /**
     * Erstellt ein neues {@link AudioAnalysisResult} mit Null-Safety.
     */
    public AudioAnalysisResult {
        waveformPeaks = Objects.requireNonNull(waveformPeaks, "waveformPeaks must not be null");
        lowBand = lowBand != null ? lowBand : new float[0];
        midBand = midBand != null ? midBand : new float[0];
        highBand = highBand != null ? highBand : new float[0];
        durationMs = Math.max(0L, durationMs);
        sampleRate = Math.max(1, sampleRate);
    }

    /**
     * Abwärtskompatibler Konstruktor ohne 3-Band-Daten.
     *
     * @param durationMs    Gesamtlaufzeit in Millisekunden
     * @param waveformPeaks Normalisierte Wellenform-Peaks
     * @param sampleRate    Abtastrate in Hz
     */
    public AudioAnalysisResult(long durationMs, float[] waveformPeaks, int sampleRate) {
        this(durationMs, waveformPeaks, sampleRate, new float[0], new float[0], new float[0]);
    }

    /**
     * Liefert ein leeres Standard-Ergebnis (z. B. als Fallback für nicht analysierbare Dateien).
     *
     * @return leeres {@link AudioAnalysisResult}
     */
    public static AudioAnalysisResult empty() {
        return new AudioAnalysisResult(0L, new float[0], 44100, new float[0], new float[0], new float[0]);
    }

    /**
     * Prüft, ob echte Wellenform-Daten vorhanden sind.
     *
     * @return {@code true}, wenn Peaks vorhanden sind, sonst {@code false}
     */
    public boolean hasWaveform() {
        return waveformPeaks.length > 0;
    }

    /**
     * Prüft, ob echte 3-Band Rekordbox-Frequenzdaten vorliegen.
     *
     * @return {@code true}, wenn Low-, Mid- und High-Bänder vorhanden sind
     */
    public boolean has3BandWaveform() {
        return lowBand.length > 0 && midBand.length > 0 && highBand.length > 0;
    }
}

package de.exit.jdmx.core.model;

import de.exit.jdmx.core.i18n.I18n;

import java.util.Objects;

/**
 * Auflösung für das Zeitleisten-Raster und das magnetische Einrasten (Snapping)
 * beim Verschieben und Zuschneiden (Croppen/Resizen) von Clips.
 */
public enum TimelineGridSnap {

    /** Stufenloses Verschieben und Schneiden ohne Einrasten. */
    FREE("timeline.grid.snap.free", 0L),

    /** Einrasten im Viertelsekunden-Raster (250 ms). */
    SEC_1_4("timeline.grid.snap.1_4s", 250L),

    /** Einrasten im Halbsekunden-Raster (500 ms). */
    SEC_1_2("timeline.grid.snap.1_2s", 500L),

    /** Einrasten im 1-Sekunden-Raster. */
    SEC_1("timeline.grid.snap.1s", 1_000L),

    /** Standard: Einrasten im 5-Sekunden-Raster. */
    SEC_5("timeline.grid.snap.5s", 5_000L),

    /** Einrasten im 10-Sekunden-Raster. */
    SEC_10("timeline.grid.snap.10s", 10_000L),

    /** Einrasten im 30-Sekunden-Raster. */
    SEC_30("timeline.grid.snap.30s", 30_000L),

    /** Einrasten im 1-Minuten-Raster (60 Sekunden). */
    MIN_1("timeline.grid.snap.1m", 60_000L);

    private final String i18nKey;
    private final long intervalMs;

    TimelineGridSnap(String i18nKey, long intervalMs) {
        this.i18nKey = Objects.requireNonNull(i18nKey, "i18nKey must not be null");
        this.intervalMs = intervalMs;
    }

    /**
     * Liefert den lokalisierten Anzeigetext der Rastereinstellung (reaktiv gemäß aktiver Sprache).
     *
     * @return lokalisierte Beschriftung
     */
    public String getLabel() {
        return I18n.get(i18nKey);
    }

    /**
     * Liefert den I18n-Ressourcenschlüssel.
     *
     * @return i18nKey
     */
    public String getI18nKey() {
        return i18nKey;
    }

    /**
     * Liefert das Raster-Intervall in Millisekunden (0 für stufenlos).
     *
     * @return Intervall in ms
     */
    public long getIntervalMs() {
        return intervalMs;
    }

    /**
     * Rastet einen beliebigen Zeitstempel auf das nächste Vielfache dieses Rasters ein.
     * Bei {@link #FREE} wird der Wert unverändert zurückgegeben.
     *
     * @param timeMs Zeitstempel in Millisekunden
     * @return gerundeter/eingerasteter Zeitstempel
     */
    public long snap(long timeMs) {
        if (intervalMs <= 0L) {
            return timeMs;
        }
        return Math.round((double) timeMs / intervalMs) * intervalMs;
    }

    @Override
    public String toString() {
        return getLabel();
    }
}

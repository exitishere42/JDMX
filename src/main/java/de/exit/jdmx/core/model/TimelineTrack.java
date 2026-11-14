package de.exit.jdmx.core.model;

import java.util.Objects;

/**
 * Repräsentiert eine einzelne Spur (Track) in der Show-Timeline (FL Studio Playlist-Konzept).
 * <p>
 * Jeder Track kann Audio-Songs, Lichtszenen, Chaser oder Cues beherbergen und verfügt
 * über einen Mute-Status zum Stummschalten der gesamten Spur.
 * </p>
 * <p>
 * <b>Thread-Sicherheit:</b> Das Modell ist zustandsbehaftet. Lese- und Schreibzugriffe
 * werden über die Kernschicht (z. B. {@code TimelineService}) koordiniert.
 * </p>
 */
public class TimelineTrack {

    /** Standardhöhe eines Tracks in Pixeln für die FL Studio Playlist-Ansicht. */
    public static final double DEFAULT_TRACK_HEIGHT = 48.0;

    /** Eindeutige Kennung des Tracks. */
    private final String id;

    /** Anzeigename des Tracks (z. B. "Track 1" oder "Showtanz"). */
    private String name;

    /** Ob der Track stummgeschaltet (gemutet) ist. */
    private boolean muted;

    /** Höhe des Tracks in der Playlist-Ansicht in Pixeln. */
    private double height;

    /** Zugeordneter Dateiname des Songs (z. B. "showtanz26.mp3"), oder {@code null}. */
    private String songName;

    /** Zugeordnete Song-Datei, oder {@code null}. */
    private java.io.File songFile;

    /** Gesamtlaufzeit des Songs in Millisekunden (0 wenn kein Song zugewiesen). */
    private long durationMs = 0L;

    /** Normalisierte Wellenform-Peaks (Werte zwischen 0.0f und 1.0f) oder {@code null}. */
    private float[] waveformData;

    /**
     * Erstellt einen neuen ungemuteten Timeline-Track mit Standardhöhe.
     *
     * @param id   Eindeutige ID des Tracks (darf nicht {@code null} sein)
     * @param name Name des Tracks (darf nicht {@code null} sein)
     * @throws NullPointerException wenn {@code id} oder {@code name} {@code null} ist
     */
    public TimelineTrack(String id, String name) {
        this(id, name, false, DEFAULT_TRACK_HEIGHT);
    }

    /**
     * Erstellt einen neuen Timeline-Track mit vollständiger Parameterkonfiguration.
     *
     * @param id     Eindeutige ID des Tracks (darf nicht {@code null} sein)
     * @param name   Name des Tracks (darf nicht {@code null} sein)
     * @param muted  Ob der Track initial gemutet ist
     * @param height Höhe der Spur in Pixeln
     * @throws NullPointerException wenn {@code id} oder {@code name} {@code null} ist
     */
    public TimelineTrack(String id, String name, boolean muted, double height) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.muted = muted;
        this.height = height > 0.0 ? height : DEFAULT_TRACK_HEIGHT;
    }

    /**
     * Schaltet den Mute-Status des Tracks um (Toggle).
     *
     * @return der neue Mute-Status
     */
    public boolean toggleMute() {
        this.muted = !this.muted;
        return this.muted;
    }

    /**
     * Liefert die eindeutige ID des Tracks.
     *
     * @return die Track-ID
     */
    public String getId() {
        return id;
    }

    /**
     * Liefert den Anzeigenamen des Tracks.
     *
     * @return der Name
     */
    public String getName() {
        return name;
    }

    /**
     * Setzt den Anzeigenamen des Tracks.
     *
     * @param name neuer Name (darf nicht {@code null} sein)
     * @throws NullPointerException wenn {@code name} {@code null} ist
     */
    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    /**
     * Prüft, ob der Track stummgeschaltet ist.
     *
     * @return {@code true}, wenn gemutet, sonst {@code false}
     */
    public boolean isMuted() {
        return muted;
    }

    /**
     * Setzt den Mute-Status des Tracks.
     *
     * @param muted neuer Mute-Status
     */
    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    /**
     * Liefert die Höhe des Tracks in Pixeln.
     *
     * @return Spurhöhe in Pixeln
     */
    public double getHeight() {
        return height;
    }

    /**
     * Setzt die Höhe des Tracks in Pixeln.
     *
     * @param height neue Höhe in Pixeln
     */
    public void setHeight(double height) {
        if (height > 0.0) {
            this.height = height;
        }
    }

    /**
     * Prüft, ob dieser Spur ein Song zugewiesen ist.
     *
     * @return {@code true}, wenn ein Song zugewiesen ist, sonst {@code false}
     */
    public boolean hasSong() {
        return songName != null && !songName.trim().isEmpty();
    }

    /**
     * Liefert den Namen des zugewiesenen Songs.
     *
     * @return Song-Name oder {@code null}
     */
    public String getSongName() {
        return songName;
    }

    /**
     * Setzt den Namen des zugewiesenen Songs.
     *
     * @param songName Song-Name oder {@code null} zum Entfernen
     */
    public void setSongName(String songName) {
        this.songName = songName;
    }

    /**
     * Liefert die zugewiesene Song-Datei.
     *
     * @return Datei oder {@code null}
     */
    public java.io.File getSongFile() {
        return songFile;
    }

    /**
     * Setzt die zugewiesene Song-Datei.
     *
     * @param songFile Song-Datei oder {@code null}
     */
    public void setSongFile(java.io.File songFile) {
        this.songFile = songFile;
    }

    /**
     * Liefert die Gesamtlaufzeit des Songs in Millisekunden.
     *
     * @return Dauer in Millisekunden (>= 0)
     */
    public long getDurationMs() {
        return durationMs;
    }

    /**
     * Setzt die Gesamtlaufzeit des Songs in Millisekunden.
     *
     * @param durationMs Dauer in Millisekunden (Werte < 0 werden als 0 gewertet)
     */
    public void setDurationMs(long durationMs) {
        this.durationMs = Math.max(0L, durationMs);
    }

    /**
     * Liefert eine Kopie der normalisierten Wellenform-Daten oder {@code null}.
     *
     * @return Kopie der Wellenform-Peaks oder {@code null}
     */
    public float[] getWaveformData() {
        return waveformData != null ? waveformData.clone() : null;
    }

    /**
     * Setzt die normalisierten Wellenform-Daten (Werte zwischen 0.0f und 1.0f).
     *
     * @param waveformData Wellenform-Peaks oder {@code null}
     */
    public void setWaveformData(float[] waveformData) {
        this.waveformData = waveformData != null ? waveformData.clone() : null;
    }

    /**
     * Prüft, ob echte Wellenform-Daten für diesen Track vorliegen.
     *
     * @return {@code true}, wenn Wellenform-Peaks vorhanden sind
     */
    public boolean hasWaveform() {
        return waveformData != null && waveformData.length > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimelineTrack that = (TimelineTrack) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TimelineTrack{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", muted=" + muted +
                ", height=" + height +
                '}';
    }
}

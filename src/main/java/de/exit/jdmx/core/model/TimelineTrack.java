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

    /** Standardhöhe eines Tracks in Pixeln für die Show-Timeline (76px für Pioneer Rekordbox Skalierung). */
    public static final double DEFAULT_TRACK_HEIGHT = 76.0;

    /** Eindeutige Kennung des Tracks. */
    private final String id;

    /** Anzeigename des Tracks (z. B. "Track 1" oder "Showtanz"). */
    private String name;

    /** Ob der Track stummgeschaltet (gemutet) ist. */
    private boolean muted;

    /** Ob der Track solo geschaltet ist. */
    private boolean solo = false;

    /** Höhe des Tracks in der Playlist-Ansicht in Pixeln. */
    private double height;

    /** Zugeordneter Dateiname des Songs (z. B. "showtanz26.mp3"), oder {@code null}. */
    private String songName;

    /** Zugeordnete Song-Datei, oder {@code null}. */
    private java.io.File songFile;

    /** Startzeitpunkt des Clips auf der Timeline in Millisekunden (Standard: 0 ms). */
    private long startTimeMs = 0L;

    /** Gesamtlaufzeit des Songs in Millisekunden (0 wenn kein Song zugewiesen). */
    private long durationMs = 0L;

    /** Normalisierte Wellenform-Peaks (Werte zwischen 0.0f und 1.0f) oder {@code null}. */
    private float[] waveformData;

    /** 3-Band Rekordbox Wellenformdaten: Bass (< 250 Hz) */
    private float[] lowBandData;

    /** 3-Band Rekordbox Wellenformdaten: Mitten (250 Hz - 3000 Hz) */
    private float[] midBandData;

    /** 3-Band Rekordbox Wellenformdaten: Höhen (> 3000 Hz) */
    private float[] highBandData;

    /** Zugeordnete Szenen-ID, oder {@code null}. */
    private String sceneId;

    /** Anzeigename der zugeordneten Szene, oder {@code null}. */
    private String sceneName;

    /** Ob diese Spur aktuell aktiv vom Playhead befeuert wird. */
    private boolean playbackActive = false;

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
     * Schaltet den Solo-Status des Tracks um (Toggle).
     *
     * @return der neue Solo-Status
     */
    public boolean toggleSolo() {
        this.solo = !this.solo;
        return this.solo;
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
     * Prüft, ob der Track solo geschaltet ist.
     *
     * @return {@code true}, wenn solo geschaltet, sonst {@code false}
     */
    public boolean isSolo() {
        return solo;
    }

    /**
     * Setzt den Solo-Status des Tracks.
     *
     * @param solo neuer Solo-Status
     */
    public void setSolo(boolean solo) {
        this.solo = solo;
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
     * Liefert den Startzeitpunkt des Clips auf der Timeline in Millisekunden.
     *
     * @return Startzeit in Millisekunden (>= 0)
     */
    public long getStartTimeMs() {
        return startTimeMs;
    }

    /**
     * Setzt den Startzeitpunkt des Clips auf der Timeline in Millisekunden.
     * Negative Werte werden automatisch auf 0 begrenzt.
     *
     * @param startTimeMs Startzeit in Millisekunden
     */
    public void setStartTimeMs(long startTimeMs) {
        this.startTimeMs = Math.max(0L, startTimeMs);
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

    /**
     * Liefert eine Kopie des Low-Bandes (Bass) oder {@code null}.
     *
     * @return Kopie der Bass-Amplituden oder {@code null}
     */
    public float[] getLowBandData() {
        return lowBandData != null ? lowBandData.clone() : null;
    }

    /**
     * Liefert eine Kopie des Mid-Bandes (Mitten) oder {@code null}.
     *
     * @return Kopie der Mitten-Amplituden oder {@code null}
     */
    public float[] getMidBandData() {
        return midBandData != null ? midBandData.clone() : null;
    }

    /**
     * Liefert eine Kopie des High-Bandes (Höhen) oder {@code null}.
     *
     * @return Kopie der Höhen-Amplituden oder {@code null}
     */
    public float[] getHighBandData() {
        return highBandData != null ? highBandData.clone() : null;
    }

    /**
     * Setzt die 3-Band Rekordbox-Wellenformdaten (Low, Mid, High).
     *
     * @param low  Bass-Amplituden oder {@code null}
     * @param mid  Mitten-Amplituden oder {@code null}
     * @param high Höhen-Amplituden oder {@code null}
     */
    public void set3BandWaveformData(float[] low, float[] mid, float[] high) {
        this.lowBandData = low != null ? low.clone() : null;
        this.midBandData = mid != null ? mid.clone() : null;
        this.highBandData = high != null ? high.clone() : null;
    }

    /**
     * Prüft, ob vollständige 3-Band Rekordbox-Daten (Bass, Mitten, Höhen) vorliegen.
     *
     * @return {@code true}, wenn alle 3 Bänder vorhanden sind
     */
    public boolean has3BandWaveform() {
        return lowBandData != null && lowBandData.length > 0
                && midBandData != null && midBandData.length > 0
                && highBandData != null && highBandData.length > 0;
    }

    /**
     * Prüft, ob dieser Spur eine Lichtszene zugewiesen ist.
     *
     * @return {@code true}, wenn eine Szene zugewiesen ist, sonst {@code false}
     */
    public boolean hasScene() {
        return sceneId != null && !sceneId.trim().isEmpty();
    }

    /**
     * Liefert die ID der zugewiesenen Lichtszene.
     *
     * @return Szenen-ID oder {@code null}
     */
    public String getSceneId() {
        return sceneId;
    }

    /**
     * Liefert den Namen der zugewiesenen Lichtszene.
     *
     * @return Szenen-Name oder {@code null}
     */
    public String getSceneName() {
        return sceneName;
    }

    /**
     * Weist diesem Track eine Lichtszene mit Startzeit und Dauer zu.
     *
     * @param sceneId    ID der Szene (darf nicht {@code null} sein)
     * @param sceneName  Name der Szene
     * @param durationMs Dauer des Blocks in ms (Standard 2000 ms)
     */
    public void setScene(String sceneId, String sceneName, long durationMs) {
        this.sceneId = Objects.requireNonNull(sceneId, "sceneId must not be null");
        this.sceneName = sceneName;
        this.durationMs = Math.max(100L, durationMs);
    }

    /**
     * Entfernt die zugewiesene Lichtszene von diesem Track.
     */
    public void clearScene() {
        this.sceneId = null;
        this.sceneName = null;
        this.playbackActive = false;
    }

    /**
     * Prüft, ob diese Spur aktuell aktiv vom Playhead befeuert wird.
     *
     * @return {@code true}, wenn aktiv befeuert
     */
    public boolean isPlaybackActive() {
        return playbackActive;
    }

    /**
     * Setzt den Status, ob die Spur aktuell aktiv vom Playhead befeuert wird.
     *
     * @param playbackActive neuer Aktivitätsstatus
     */
    public void setPlaybackActive(boolean playbackActive) {
        this.playbackActive = playbackActive;
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
                ", solo=" + solo +
                ", height=" + height +
                '}';
    }
}

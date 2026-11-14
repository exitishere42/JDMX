package de.exit.jdmx.service;

import de.exit.jdmx.core.model.TimelineTrack;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service für die Geschäftslogik der Show-Timeline (100% Core/Service-Schicht).
 * <p>
 * Verwaltet:
 * <ul>
 *     <li>Die Liste aller aktiven Spuren (Tracks) in der Show-Timeline</li>
 *     <li>Hinzufügen neuer Tracks im FL Studio Playlist-Stil mit fortlaufender Nummerierung</li>
 *     <li>Umbenennen von Spuren und Mute-Steuerung</li>
 *     <li>Importierte Audio-Songs (MP3/WAV) und aktiver Playback-Track</li>
 *     <li>Start-, Pause- und Stop-Logik für die Timeline mit präziser Zeitverfolgung</li>
 *     <li>Benachrichtigung registrierter Listener bei Änderungen (Reaktivität)</li>
 * </ul>
 * </p>
 * <p>
 * <b>Thread-Sicherheit:</b> Vollständig thread-sicher über atomare Variablen,
 * {@link CopyOnWriteArrayList} und synchronisierte Playback-Verwaltung.
 * </p>
 */
public class TimelineService {

    /** Interne Track-Liste der Timeline. */
    private final List<TimelineTrack> tracks = new CopyOnWriteArrayList<>();

    /** Liste der importierten Songs. */
    private final List<File> importedSongs = new CopyOnWriteArrayList<>();

    /** Aktuell aktive Audio-Datei. */
    private volatile File activeSongFile;

    /** Zähler für fortlaufende Standard-Track-Namen ("Track 1", "Track 2", ...). */
    private final AtomicInteger trackSequenceCounter = new AtomicInteger(1);

    /** Ob die Timeline aktuell abgespielt wird. */
    private final AtomicBoolean playing = new AtomicBoolean(false);

    /** Aktuelle Abspielposition in Millisekunden. */
    private final AtomicLong currentPositionMs = new AtomicLong(0L);

    /** Registrierte Listener für Daten- und Zustandsänderungen. */
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    /** Scheduled Executor für den Playback-Takt. */
    private ScheduledExecutorService playbackExecutor;
    private volatile long lastTickNanos = 0L;

    /**
     * Erstellt einen neuen {@link TimelineService} und initialisiert diesen standardmäßig
     * mit einer ersten Spur ("Track 1").
     */
    public TimelineService() {
        initDefaultTrack();
    }

    /**
     * Erzeugt die initiale Standardspur "Track 1".
     */
    private void initDefaultTrack() {
        String id = "track-" + UUID.randomUUID().toString().substring(0, 8);
        tracks.add(new TimelineTrack(id, "Track 1"));
    }

    /**
     * Liefert eine unveränderliche Liste aller aktuell vorhandenen Tracks.
     *
     * @return unmodifizierbare Liste der {@link TimelineTrack}s
     */
    public List<TimelineTrack> getTracks() {
        return Collections.unmodifiableList(tracks);
    }

    /**
     * Sucht einen Track anhand seiner eindeutigen ID.
     *
     * @param trackId ID des gesuchten Tracks
     * @return Optional mit dem Track, falls vorhanden
     */
    public Optional<TimelineTrack> findTrackById(String trackId) {
        if (trackId == null) {
            return Optional.empty();
        }
        return tracks.stream()
                .filter(t -> t.getId().equals(trackId))
                .findFirst();
    }

    /**
     * Fügt einen neuen Track mit dem nächsten fortlaufenden Standard-Namen
     * ("Track 2", "Track 3", ...) hinzu.
     *
     * @return der neu erstellte {@link TimelineTrack}
     */
    public TimelineTrack addTrack() {
        int nextNum = trackSequenceCounter.incrementAndGet();
        String name = "Track " + nextNum;
        return addTrack(name);
    }

    /**
     * Fügt einen neuen Track mit dem angegebenen Namen hinzu.
     *
     * @param name Name des neuen Tracks (darf nicht {@code null} oder leer sein)
     * @return der neu erstellte {@link TimelineTrack}
     * @throws NullPointerException     wenn {@code name} {@code null} ist
     * @throws IllegalArgumentException wenn {@code name} leer ist
     */
    public TimelineTrack addTrack(String name) {
        Objects.requireNonNull(name, "name must not be null");
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Track name must not be empty");
        }
        String id = "track-" + UUID.randomUUID().toString().substring(0, 8);
        TimelineTrack newTrack = new TimelineTrack(id, trimmed);
        tracks.add(newTrack);
        notifyChange();
        return newTrack;
    }

    /**
     * Benennt einen bestehenden Track um.
     *
     * @param trackId ID des umzubenennenden Tracks
     * @param newName neuer Name (darf nicht {@code null} oder leer sein)
     * @return {@code true}, wenn der Track gefunden und umbenannt wurde
     * @throws NullPointerException     wenn {@code newName} {@code null} ist
     * @throws IllegalArgumentException wenn {@code newName} nach dem Trimmen leer ist
     */
    public boolean renameTrack(String trackId, String newName) {
        Objects.requireNonNull(newName, "newName must not be null");
        String trimmed = newName.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Track name must not be empty");
        }
        Optional<TimelineTrack> trackOpt = findTrackById(trackId);
        if (trackOpt.isPresent()) {
            trackOpt.get().setName(trimmed);
            notifyChange();
            return true;
        }
        return false;
    }

    /**
     * Schaltet den Mute-Status eines Tracks um.
     *
     * @param trackId ID des Tracks
     * @return der neue Mute-Status
     * @throws NoSuchElementException wenn der Track nicht gefunden wird
     */
    public boolean toggleTrackMute(String trackId) {
        TimelineTrack track = findTrackById(trackId)
                .orElseThrow(() -> new NoSuchElementException("Track not found: " + trackId));
        boolean newStatus = track.toggleMute();
        notifyChange();
        return newStatus;
    }

    /**
     * Setzt den Mute-Status eines Tracks explizit.
     *
     * @param trackId ID des Tracks
     * @param muted   neuer Mute-Status
     * @return {@code true}, wenn der Track gefunden und aktualisiert wurde
     */
    public boolean setTrackMuted(String trackId, boolean muted) {
        Optional<TimelineTrack> trackOpt = findTrackById(trackId);
        if (trackOpt.isPresent()) {
            trackOpt.get().setMuted(muted);
            notifyChange();
            return true;
        }
        return false;
    }

    /**
     * Entfernt einen Track anhand seiner ID.
     *
     * @param trackId ID des zu entfernenden Tracks
     * @return {@code true}, wenn der Track entfernt wurde
     */
    public boolean removeTrack(String trackId) {
        if (trackId == null) {
            return false;
        }
        boolean removed = tracks.removeIf(t -> t.getId().equals(trackId));
        if (removed) {
            notifyChange();
        }
        return removed;
    }

    // =========================================================================
    // Song-Import & Audio-Verwaltung
    // =========================================================================

    /**
     * Importiert eine Audio-Datei (MP3/WAV) in die Timeline.
     *
     * @param songFile die zu importierende Datei (darf nicht {@code null} sein)
     * @throws NullPointerException wenn {@code songFile} {@code null} ist
     */
    public void importSong(File songFile) {
        Objects.requireNonNull(songFile, "songFile must not be null");
        if (!importedSongs.contains(songFile)) {
            importedSongs.add(songFile);
        }
        this.activeSongFile = songFile;
        notifyChange();
    }

    /**
     * Liefert die Liste aller importierten Songs.
     *
     * @return unmodifizierbare Liste der importierten Dateien
     */
    public List<File> getImportedSongs() {
        return Collections.unmodifiableList(importedSongs);
    }

    /**
     * Liefert die aktuell aktive Song-Datei für Playback.
     *
     * @return aktive Datei oder {@code null}
     */
    public File getActiveSongFile() {
        return activeSongFile;
    }

    // =========================================================================
    // Playback-Steuerung (Start, Pause, Stop, Position)
    // =========================================================================

    /**
     * Prüft, ob die Timeline aktuell wiedergegeben wird.
     *
     * @return {@code true}, wenn aktiv, sonst {@code false}
     */
    public boolean isPlaying() {
        return playing.get();
    }

    /**
     * Liefert die aktuelle Abspielposition in Millisekunden.
     *
     * @return Position in ms
     */
    public long getCurrentPositionMs() {
        return currentPositionMs.get();
    }

    /**
     * Setzt die aktuelle Abspielposition in Millisekunden.
     *
     * @param positionMs Position in Millisekunden (>= 0)
     */
    public void setPositionMs(long positionMs) {
        currentPositionMs.set(Math.max(0L, positionMs));
        notifyChange();
    }

    /**
     * Startet die Wiedergabe der Timeline ab der aktuellen Position.
     */
    public synchronized void startPlayback() {
        if (playing.compareAndSet(false, true)) {
            lastTickNanos = System.nanoTime();
            startPlaybackTimer();
            notifyChange();
        }
    }

    /**
     * Pausiert die Wiedergabe der Timeline, behält die aktuelle Position bei.
     */
    public synchronized void pausePlayback() {
        if (playing.compareAndSet(true, false)) {
            stopPlaybackTimer();
            notifyChange();
        }
    }

    /**
     * Stoppt die Wiedergabe der Timeline vollständig und setzt die Position auf 0ms zurück.
     */
    public synchronized void stopPlayback() {
        playing.set(false);
        stopPlaybackTimer();
        currentPositionMs.set(0L);
        notifyChange();
    }

    private void startPlaybackTimer() {
        stopPlaybackTimer();
        playbackExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Timeline-Playback-Clock");
            t.setDaemon(true);
            return t;
        });

        playbackExecutor.scheduleAtFixedRate(() -> {
            if (!playing.get()) return;
            long now = System.nanoTime();
            long elapsedNanos = now - lastTickNanos;
            lastTickNanos = now;

            long deltaMs = elapsedNanos / 1_000_000L;
            if (deltaMs > 0) {
                currentPositionMs.addAndGet(deltaMs);
                notifyChange();
            }
        }, 25, 25, TimeUnit.MILLISECONDS);
    }

    private void stopPlaybackTimer() {
        if (playbackExecutor != null && !playbackExecutor.isShutdown()) {
            playbackExecutor.shutdownNow();
            playbackExecutor = null;
        }
    }

    // =========================================================================
    // Change-Listener
    // =========================================================================

    /**
     * Registriert einen Listener, der bei Änderungen an den Tracks oder am Playback aufgerufen wird.
     *
     * @param listener Callback-Aktion
     */
    public void addChangeListener(Runnable listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    /**
     * Entfernt einen registrierten Change-Listener.
     *
     * @param listener Callback-Aktion
     */
    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    /**
     * Benachrichtigt alle registrierten Listener über eine Zustandsänderung.
     */
    public void notifyChange() {
        for (Runnable listener : changeListeners) {
            try {
                listener.run();
            } catch (Exception e) {
                // Einzelne Listener-Fehler fangen
            }
        }
    }
}

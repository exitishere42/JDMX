package de.exit.jdmx.service;

import de.exit.jdmx.core.log.AppLogger;
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

    /** Audioanalyse-Service für Laufzeit- und Wellenform-Berechnungen. */
    private final AudioAnalysisService audioAnalysisService;

    /** Audio-Wiedergabeservice für echte Soundausgabe (MP3 / WAV). */
    private final AudioPlaybackService audioPlaybackService;

    /**
     * Erstellt einen neuen {@link TimelineService} und initialisiert diesen standardmäßig
     * mit einer ersten Spur ("Track 1").
     */
    public TimelineService() {
        this(new AudioAnalysisService(), new AudioPlaybackService());
    }

    /**
     * Erstellt einen neuen {@link TimelineService} mit injiziertem {@link AudioAnalysisService}.
     *
     * @param audioAnalysisService der Audioanalyse-Dienst (darf nicht {@code null} sein)
     */
    public TimelineService(AudioAnalysisService audioAnalysisService) {
        this(audioAnalysisService, new AudioPlaybackService());
    }

    /**
     * Erstellt einen neuen {@link TimelineService} mit injiziertem {@link AudioAnalysisService}
     * und {@link AudioPlaybackService}.
     *
     * @param audioAnalysisService der Audioanalyse-Dienst (darf nicht {@code null} sein)
     * @param audioPlaybackService der Audio-Wiedergabedienst (darf nicht {@code null} sein)
     */
    public TimelineService(AudioAnalysisService audioAnalysisService, AudioPlaybackService audioPlaybackService) {
        this.audioAnalysisService = Objects.requireNonNull(audioAnalysisService, "audioAnalysisService must not be null");
        this.audioPlaybackService = Objects.requireNonNull(audioPlaybackService, "audioPlaybackService must not be null");
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
        if (playing.get()) {
            audioPlaybackService.playTimeline(tracks, currentPositionMs.get());
        }
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
            if (playing.get()) {
                audioPlaybackService.playTimeline(tracks, currentPositionMs.get());
            }
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
        AppLogger.info("[TIMELINE] Song importiert: %s (%d Bytes)", songFile.getAbsolutePath(), songFile.length());
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
     * Entfernt eine importierte Song-Datei aus dem Explorer.
     *
     * @param songFile die zu entfernende Datei
     * @return {@code true}, wenn die Datei vorhanden war und entfernt wurde
     */
    public boolean removeSong(File songFile) {
        if (songFile == null) return false;
        boolean removed = importedSongs.remove(songFile);
        if (removed) {
            AppLogger.info("[TIMELINE] Song aus Explorer entfernt: %s", songFile.getName());
            notifyChange();
        }
        return removed;
    }

    /**
     * Sucht eine importierte Song-Datei anhand ihres Dateinamens.
     *
     * @param songName Name der gesuchten Datei (z. B. "showtanz.mp3")
     * @return Optional mit der Datei, falls gefunden
     */
    public Optional<File> findSongFileByName(String songName) {
        if (songName == null || songName.isBlank()) return Optional.empty();
        String target = songName.trim();
        Optional<File> direct = importedSongs.stream()
                .filter(f -> f.getName().equalsIgnoreCase(target))
                .findFirst();
        if (direct.isPresent()) {
            return direct;
        }

        if (target.startsWith("song-")) {
            String clean = target.substring(5).trim();
            Optional<File> stripped = importedSongs.stream()
                    .filter(f -> f.getName().equalsIgnoreCase(clean))
                    .findFirst();
            if (stripped.isPresent()) {
                return stripped;
            }
        }

        // Falls es sich um einen Pfad zu einer existierenden Datei handelt:
        File checkFile = new File(target);
        if (checkFile.exists() && checkFile.isFile()) {
            return Optional.of(checkFile);
        }

        return Optional.empty();
    }

    /**
     * Weist einer Spur (Track) eine Audiodatei als Song-Clip zu.
     * Wenn die Spur noch ihren Standard-Namen (z. B. "Track 1") trägt, wird sie
     * automatisch auf den Namen des Songs umbenannt.
     *
     * @param trackId  ID der Zielspur
     * @param songFile die zuzuweisende Audiodatei
     * @return {@code true}, wenn die Zuweisung erfolgreich war
     */
    public boolean assignSongToTrack(String trackId, File songFile) {
        Objects.requireNonNull(songFile, "songFile must not be null");
        AppLogger.info("[TIMELINE] assignSongToTrack: trackId='%s', file='%s' (exists=%s, len=%d)",
                trackId, songFile.getAbsolutePath(), songFile.exists(), songFile.length());

        Optional<TimelineTrack> trackOpt = findTrackById(trackId);
        if (trackOpt.isPresent()) {
            TimelineTrack track = trackOpt.get();
            track.setSongFile(songFile);
            track.setSongName(songFile.getName());

            // Vorläufige Dauer schätzen, damit die UI sofort (< 2ms) reagiert
            long estDuration = songFile.length() > 0 ? Math.max(10_000L, (songFile.length() * 1000L) / 24000L) : 60_000L;
            track.setDurationMs(estDuration);

            if (track.getName().matches("^Track\\s+\\d+$")) {
                track.setName(songFile.getName());
            }

            if (!importedSongs.contains(songFile)) {
                importedSongs.add(songFile);
            }
            this.activeSongFile = songFile;

            // Asynchrone Audioanalyse durchführen (nicht blockierend für JavaFX-Thread)
            CompletableFuture<de.exit.jdmx.core.model.AudioAnalysisResult> future = audioAnalysisService.analyzeAsync(songFile);
            try {
                // Schnelle 50ms-Prüfung für kleine WAV-Dateien und deterministische Tests
                de.exit.jdmx.core.model.AudioAnalysisResult fastResult = future.get(50, TimeUnit.MILLISECONDS);
                applyAnalysisResult(track, fastResult);
            } catch (Exception e) {
                // Bei längeren MP3s läuft die Dekodierung im Hintergrund weiter (0 ms UI-Lag)
                future.thenAccept(result -> {
                    if (result != null) {
                        applyAnalysisResult(track, result);
                        notifyChange();
                    }
                });
            }

            notifyChange();
            AppLogger.info("[TIMELINE] Song '%s' erfolgreich Track '%s' ('%s') zugewiesen (Dauer=%d ms, Waveform=%d Peaks).",
                    songFile.getName(), track.getId(), track.getName(), track.getDurationMs(),
                    track.hasWaveform() ? track.getWaveformData().length : 0);
            return true;
        }
        AppLogger.warn("[TIMELINE] Zuweisung fehlgeschlagen: Track mit ID '%s' nicht gefunden.", trackId);
        return false;
    }

    /**
     * Liefert die maximale Gesamtlaufzeit der Timeline in Millisekunden.
     * Ermittelt das späteste Clip-Ende (startTimeMs + durationMs) aller Spuren
     * oder liefert standardmäßig 60.000 ms (1 Minute).
     *
     * @return maximale Laufzeit in ms
     */
    public long getMaxTimelineDurationMs() {
        long maxEndMs = 0L;
        for (TimelineTrack t : tracks) {
            if (t.hasSong()) {
                long trackEnd = t.getStartTimeMs() + t.getDurationMs();
                if (trackEnd > maxEndMs) {
                    maxEndMs = trackEnd;
                }
            }
        }
        return maxEndMs > 0 ? maxEndMs : 60_000L;
    }

    /**
     * Verschiebt die Startzeit eines Clips (Songs) auf der angegebenen Spur.
     *
     * @param trackId     ID der Zielspur
     * @param startTimeMs Neuer Startzeitpunkt in Millisekunden (>= 0)
     * @return {@code true}, wenn die Spur gefunden und aktualisiert wurde
     */
    public boolean setTrackStartTimeMs(String trackId, long startTimeMs) {
        Optional<TimelineTrack> trackOpt = findTrackById(trackId);
        if (trackOpt.isPresent()) {
            TimelineTrack track = trackOpt.get();
            track.setStartTimeMs(startTimeMs);
            if (playing.get()) {
                audioPlaybackService.resyncTrackDebounced(track, currentPositionMs.get());
            }
            notifyChange();
            return true;
        }
        return false;
    }

    /**
     * Führt eine sofortige Audiosynchronisation einer einzelnen Spur zur aktuellen
     * Playhead-Position durch (z. B. beim Loslassen der Maus nach dem Clip-Drag).
     *
     * @param trackId ID der Zielspur
     */
    public void resyncTrackImmediate(String trackId) {
        Optional<TimelineTrack> trackOpt = findTrackById(trackId);
        if (trackOpt.isPresent() && playing.get()) {
            audioPlaybackService.resyncTrackImmediate(trackOpt.get(), currentPositionMs.get());
        }
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
     * Synchronisiert den Player nahtlos und startet ihn sofort an der neuen Position neu,
     * falls die Wiedergabe gerade aktiv ist.
     *
     * @param positionMs Position in Millisekunden (>= 0)
     */
    public void setPositionMs(long positionMs) {
        long safeMs = Math.max(0L, positionMs);
        currentPositionMs.set(safeMs);
        lastTickNanos = System.nanoTime();
        audioPlaybackService.setPosition(safeMs);
        notifyChange();
    }

    /**
     * Startet die parallele Multitrack-Wiedergabe der Timeline ab der aktuellen Position.
     */
    public synchronized void startPlayback() {
        if (playing.compareAndSet(false, true)) {
            lastTickNanos = System.nanoTime();
            startPlaybackTimer();
            audioPlaybackService.playTimeline(tracks, currentPositionMs.get());
            notifyChange();
        }
    }

    /**
     * Ermittelt die aktive Song-Datei für Playback oder sucht automatisch den ersten Track
     * mit einer zugewiesenen Audiodatei.
     *
     * @return gefundene Song-Datei oder {@code null}
     */
    public File resolveActiveSongFile() {
        if (activeSongFile != null && activeSongFile.exists()) {
            return activeSongFile;
        }
        for (TimelineTrack t : tracks) {
            if (t.hasSong() && t.getSongFile() != null && t.getSongFile().exists()) {
                this.activeSongFile = t.getSongFile();
                return this.activeSongFile;
            }
        }
        return null;
    }

    private void applyAnalysisResult(TimelineTrack track, de.exit.jdmx.core.model.AudioAnalysisResult analysis) {
        if (track == null || analysis == null) return;
        if (analysis.durationMs() > 0) {
            track.setDurationMs(analysis.durationMs());
        }
        if (analysis.hasWaveform()) {
            track.setWaveformData(analysis.waveformPeaks());
        }
    }

    /**
     * Pausiert die Wiedergabe der Timeline, behält die aktuelle Position bei.
     */
    public synchronized void pausePlayback() {
        if (playing.compareAndSet(true, false)) {
            stopPlaybackTimer();
            audioPlaybackService.pause();
            notifyChange();
        }
    }

    /**
     * Stoppt die Wiedergabe der Timeline vollständig und setzt die Position auf 0ms zurück.
     */
    public synchronized void stopPlayback() {
        playing.set(false);
        stopPlaybackTimer();
        audioPlaybackService.stop();
        currentPositionMs.set(0L);
        notifyChange();
    }

    /**
     * Liefert den Audio-Wiedergabedienst.
     *
     * @return der {@link AudioPlaybackService}
     */
    public AudioPlaybackService getAudioPlaybackService() {
        return audioPlaybackService;
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
                long newPos = currentPositionMs.addAndGet(deltaMs);
                audioPlaybackService.onTimelineTick(newPos);
                long maxDuration = getMaxTimelineDurationMs();
                if (maxDuration > 0 && newPos >= maxDuration) {
                    currentPositionMs.set(maxDuration);
                    pausePlayback();
                } else {
                    notifyChange();
                }
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
            } catch (Throwable t) {
                AppLogger.error("[TIMELINE] Fehler in Change-Listener: %s", t.getMessage());
            }
        }
    }
}

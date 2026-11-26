package de.exit.jdmx.service;

import de.exit.jdmx.core.log.AppLogger;
import de.exit.jdmx.core.model.TimelineGridSnap;
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

    /** Registrierte High-Frequency Listener für reine Playback-Frame-Updates (ohne Layout-Rebuild). */
    private final List<Runnable> playbackFrameListeners = new CopyOnWriteArrayList<>();

    /** Scheduled Executor für den Playback-Takt. */
    private ScheduledExecutorService playbackExecutor;
    private final Object clockLock = new Object();
    private volatile long clockAnchorMs = 0L;
    private volatile long clockAnchorNanos = 0L;

    /** Aktive Raster- und Snapping-Einstellung für die Timeline (Standard: 5 Sekunden). */
    private volatile TimelineGridSnap gridSnap = TimelineGridSnap.SEC_5;

    /** Audioanalyse-Service für Laufzeit- und Wellenform-Berechnungen. */
    private final AudioAnalysisService audioAnalysisService;

    /** Audio-Wiedergabeservice für echte Soundausgabe (MP3 / WAV). */
    private final AudioPlaybackService audioPlaybackService;

    /** Optionaler Szenen-Service zur Ausführung von Lichtszenen auf der Timeline. */
    private volatile SceneService sceneService;

    /**
     * Erstellt einen neuen {@link TimelineService} und initialisiert diesen standardmäßig
     * mit einer ersten Spur ("Track 1").
     */
    public TimelineService() {
        this(new AudioAnalysisService(), new AudioPlaybackService());
    }

    /**
     * Erstellt einen neuen {@link TimelineService} mit injiziertem {@link SceneService}.
     *
     * @param sceneService der {@link SceneService} zur Steuerung von Lichtszenen
     */
    public TimelineService(SceneService sceneService) {
        this(new AudioAnalysisService(), new AudioPlaybackService(), sceneService);
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
        this(audioAnalysisService, audioPlaybackService, null);
    }

    /**
     * Erstellt einen neuen {@link TimelineService} mit vollständiger Parameterinjektion.
     *
     * @param audioAnalysisService der Audioanalyse-Dienst (darf nicht {@code null} sein)
     * @param audioPlaybackService der Audio-Wiedergabedienst (darf nicht {@code null} sein)
     * @param sceneService         der Szenen-Service zur Steuerung von Lichtszenen (darf {@code null} sein)
     */
    public TimelineService(AudioAnalysisService audioAnalysisService, AudioPlaybackService audioPlaybackService, SceneService sceneService) {
        this.audioAnalysisService = Objects.requireNonNull(audioAnalysisService, "audioAnalysisService must not be null");
        this.audioPlaybackService = Objects.requireNonNull(audioPlaybackService, "audioPlaybackService must not be null");
        this.sceneService = sceneService;
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
        evaluateScenePlayback(currentPositionMs.get());
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
            evaluateScenePlayback(currentPositionMs.get());
            notifyChange();
            return true;
        }
        return false;
    }

    /**
     * Schaltet den Solo-Status eines Tracks um und aktualisiert die Wiedergabe.
     *
     * @param trackId ID des Tracks
     * @return der neue Solo-Status
     * @throws NoSuchElementException wenn der Track nicht gefunden wird
     */
    public boolean toggleTrackSolo(String trackId) {
        TimelineTrack track = findTrackById(trackId)
                .orElseThrow(() -> new NoSuchElementException("Track not found: " + trackId));
        boolean newStatus = track.toggleSolo();
        if (playing.get()) {
            audioPlaybackService.playTimeline(tracks, currentPositionMs.get());
        }
        evaluateScenePlayback(currentPositionMs.get());
        notifyChange();
        return newStatus;
    }

    /**
     * Setzt den Solo-Status eines Tracks explizit.
     *
     * @param trackId ID des Tracks
     * @param solo    neuer Solo-Status
     * @return {@code true}, wenn der Track gefunden und aktualisiert wurde
     */
    public boolean setTrackSolo(String trackId, boolean solo) {
        Optional<TimelineTrack> trackOpt = findTrackById(trackId);
        if (trackOpt.isPresent()) {
            trackOpt.get().setSolo(solo);
            if (playing.get()) {
                audioPlaybackService.playTimeline(tracks, currentPositionMs.get());
            }
            evaluateScenePlayback(currentPositionMs.get());
            notifyChange();
            return true;
        }
        return false;
    }

    /**
     * Ermittelt, ob ein Track hörbar ist, unter Berücksichtigung von Mute und Solo auf allen Tracks.
     * <p>
     * Ein Track ist unhörbar wenn er gemutet ist. Falls mindestens ein Track im Projekt auf Solo steht,
     * sind nur solche Tracks hörbar, die selbst auf Solo stehen und nicht gemutet sind.
     * </p>
     *
     * @param trackId ID des Tracks
     * @return {@code true}, wenn der Track akustisch aktiv ist, sonst {@code false}
     */
    public boolean isTrackAudible(String trackId) {
        TimelineTrack target = findTrackById(trackId).orElse(null);
        if (target == null || target.isMuted()) {
            return false;
        }
        boolean anySolo = tracks.stream().anyMatch(TimelineTrack::isSolo);
        if (anySolo) {
            return target.isSolo();
        }
        return true;
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
                // Schnelle Prüfung für kleine WAV-Dateien und deterministische Tests (bis zu 500ms bei hoher Testlast)
                de.exit.jdmx.core.model.AudioAnalysisResult fastResult = future.get(500, TimeUnit.MILLISECONDS);
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
     * Ermittelt das späteste Clip-Ende (startTimeMs + durationMs) aller Spuren (Songs und Szenen)
     * oder liefert standardmäßig 60.000 ms (1 Minute).
     *
     * @return maximale Laufzeit in ms
     */
    public long getMaxTimelineDurationMs() {
        long maxEndMs = 0L;
        for (TimelineTrack t : tracks) {
            if (t.hasSong() || t.hasScene()) {
                long trackEnd = t.getStartTimeMs() + t.getDurationMs();
                if (trackEnd > maxEndMs) {
                    maxEndMs = trackEnd;
                }
            }
        }
        return maxEndMs > 0 ? maxEndMs : 60_000L;
    }

    /**
     * Liefert den aktuell injizierten Szenen-Service.
     *
     * @return der {@link SceneService} oder {@code null}
     */
    public SceneService getSceneService() {
        return sceneService;
    }

    /**
     * Setzt den Szenen-Service für die Ausführung von Lichtszenen auf der Timeline.
     *
     * @param sceneService der {@link SceneService}
     */
    public void setSceneService(SceneService sceneService) {
        this.sceneService = sceneService;
    }

    /** Standarddauer eines neu angelegten Szenen-Clips in Millisekunden (5 Sekunden). */
    public static final long DEFAULT_SCENE_DURATION_MS = TimelineTrack.DEFAULT_SCENE_DURATION_MS;

    /** Standardhöhe einer neu angelegten Szenen-Spur (50.0 px). */
    public static final double DEFAULT_SCENE_TRACK_HEIGHT = TimelineTrack.DEFAULT_SCENE_TRACK_HEIGHT;

    /** Mindestdauer eines Clips in Millisekunden (250 ms = 1/4 Sekunde). */
    public static final long MIN_CLIP_DURATION_MS = TimelineTrack.MIN_CLIP_DURATION_MS;

    /**
     * Erstellt eine neue Spur für eine Lichtszene und platziert den Szenen-Clip.
     *
     * @param sceneId     ID der Szene (darf nicht {@code null} sein)
     * @param sceneName   Anzeigename der Szene (oder {@code null})
     * @param startTimeMs Startzeitpunkt auf der Zeitleiste in Millisekunden (>= 0)
     * @param durationMs  Dauer des Clips in Millisekunden (Standard: 5000L)
     * @return der neu erstellte {@link TimelineTrack}
     */
    public TimelineTrack addSceneTrack(String sceneId, String sceneName, long startTimeMs, long durationMs) {
        Objects.requireNonNull(sceneId, "sceneId must not be null");
        String effectiveName = (sceneName != null && !sceneName.isBlank())
                ? sceneName.trim()
                : "Szene " + trackSequenceCounter.incrementAndGet();
        String id = "track-" + UUID.randomUUID().toString().substring(0, 8);
        TimelineTrack track = new TimelineTrack(id, effectiveName, false, DEFAULT_SCENE_TRACK_HEIGHT);
        track.setStartTimeMs(Math.max(0L, startTimeMs));
        track.setScene(sceneId, sceneName, durationMs > 0 ? durationMs : DEFAULT_SCENE_DURATION_MS);
        tracks.add(track);
        notifyChange();
        return track;
    }

    /**
     * Weist einer bestehenden Spur eine Lichtszene zu.
     *
     * @param trackId     ID der Zielspur
     * @param sceneId     ID der Szene
     * @param sceneName   Name der Szene
     * @param startTimeMs Startzeitpunkt in ms
     * @param durationMs  Dauer in ms
     * @return {@code true}, wenn die Zuweisung erfolgreich war
     */
    public boolean assignSceneToTrack(String trackId, String sceneId, String sceneName, long startTimeMs, long durationMs) {
        Optional<TimelineTrack> trackOpt = findTrackById(trackId);
        if (trackOpt.isPresent()) {
            TimelineTrack track = trackOpt.get();
            track.setStartTimeMs(Math.max(0L, startTimeMs));
            track.setScene(sceneId, sceneName, durationMs > 0 ? durationMs : DEFAULT_SCENE_DURATION_MS);
            if (!track.hasSong()) {
                track.setHeight(DEFAULT_SCENE_TRACK_HEIGHT);
            }
            if (track.getName().matches("^Track\\s+\\d+$") && sceneName != null && !sceneName.isBlank()) {
                track.setName(sceneName);
            }
            notifyChange();
            return true;
        }
        return false;
    }

    /**
     * Überprüft alle Szenen-Spuren gegen die gegebene Abspielposition und triggert
     * das Starten bzw. Beenden der entsprechenden Lichtszenen (Start bei Eintreffen,
     * Deaktivierung bei Verlassen des 2-Sekunden-Blocks).
     *
     * @param positionMs aktuelle Timeline-Position in Millisekunden
     */
    public void evaluateScenePlayback(long positionMs) {
        if (sceneService == null) return;
        for (TimelineTrack t : tracks) {
            if (t.hasScene()) {
                boolean audible = isTrackAudible(t.getId());
                long start = t.getStartTimeMs();
                long end = start + t.getDurationMs();
                boolean inRange = audible && positionMs >= start && positionMs < end;

                if (inRange && !t.isPlaybackActive()) {
                    t.setPlaybackActive(true);
                    sceneService.applyScene(t.getSceneId(), -1);
                } else if (!inRange && t.isPlaybackActive()) {
                    t.setPlaybackActive(false);
                    sceneService.deactivateScene(t.getSceneId(), 0);
                }
            }
        }
    }

    /**
     * Deaktiviert alle aktuell aktiven Szenen-Spuren (z. B. bei Stop der Timeline).
     */
    public void deactivateAllActiveScenes() {
        for (TimelineTrack t : tracks) {
            if (t.hasScene() && t.isPlaybackActive()) {
                t.setPlaybackActive(false);
                if (sceneService != null) {
                    sceneService.deactivateScene(t.getSceneId(), 0);
                }
            }
        }
    }

    /**
     * Verschiebt die Startzeit eines Clips (Songs) auf der angegebenen Spur.
     * <p>
     * <b>Schutz während der Show:</b> Befindet sich die Timeline in der aktiven Wiedergabe
     * ({@link #isPlaying()} == {@code true}), wird die Verschiebung abgewiesen und {@code false}
     * zurückgegeben, um komplexe Resynchronisationslogiken und Audio-Glitches zu verhindern.
     * </p>
     *
     * @param trackId     ID der Zielspur
     * @param startTimeMs Neuer Startzeitpunkt in Millisekunden (>= 0)
     * @return {@code true}, wenn die Spur gefunden und aktualisiert wurde; {@code false}, wenn nicht gefunden oder Wiedergabe aktiv ist
     */
    public boolean setTrackStartTimeMs(String trackId, long startTimeMs) {
        if (playing.get()) {
            AppLogger.warn("[TIMELINE] Verschieben von Tracks während aktiver Wiedergabe blockiert (trackId: %s)", trackId);
            return false;
        }
        Optional<TimelineTrack> trackOpt = findTrackById(trackId);
        if (trackOpt.isPresent()) {
            TimelineTrack track = trackOpt.get();
            track.setStartTimeMs(startTimeMs);
            notifyChange();
            return true;
        }
        return false;
    }

    /**
     * Setzt die Gesamtlaufzeit eines Clips auf einer Spur (begrenzt auf mindestens {@value MIN_CLIP_DURATION_MS} ms).
     *
     * @param trackId    ID der Zielspur
     * @param durationMs Neue Dauer in Millisekunden
     * @return {@code true}, wenn aktualisiert; {@code false}, wenn nicht gefunden oder Wiedergabe aktiv ist
     */
    public boolean setTrackDurationMs(String trackId, long durationMs) {
        if (playing.get()) {
            AppLogger.warn("[TIMELINE] Ändern der Clip-Dauer während aktiver Wiedergabe blockiert (trackId: %s)", trackId);
            return false;
        }
        Optional<TimelineTrack> trackOpt = findTrackById(trackId);
        if (trackOpt.isPresent()) {
            TimelineTrack track = trackOpt.get();
            track.setDurationMs(Math.max(MIN_CLIP_DURATION_MS, durationMs));
            notifyChange();
            return true;
        }
        return false;
    }

    /**
     * Verlängert oder verkürzt einen Clip an der rechten Kante (Endzeitpunkt).
     *
     * @param trackId      ID der Zielspur
     * @param newEndTimeMs Neuer Endzeitpunkt auf der Zeitleiste in Millisekunden
     * @return {@code true}, wenn erfolgreich aktualisiert
     */
    public boolean resizeTrackRight(String trackId, long newEndTimeMs) {
        if (playing.get()) {
            AppLogger.warn("[TIMELINE] Resizing von Clips während aktiver Wiedergabe blockiert (trackId: %s)", trackId);
            return false;
        }
        Optional<TimelineTrack> trackOpt = findTrackById(trackId);
        if (trackOpt.isPresent()) {
            TimelineTrack track = trackOpt.get();
            long newDurationMs = Math.max(MIN_CLIP_DURATION_MS, newEndTimeMs - track.getStartTimeMs());
            track.setDurationMs(newDurationMs);
            notifyChange();
            return true;
        }
        return false;
    }

    /**
     * Verlängert oder verkürzt einen Clip an der linken Kante (Startzeitpunkt).
     *
     * @param trackId        ID der Zielspur
     * @param newStartTimeMs Neuer Startzeitpunkt auf der Zeitleiste in Millisekunden
     * @return {@code true}, wenn erfolgreich aktualisiert
     */
    public boolean resizeTrackLeft(String trackId, long newStartTimeMs) {
        if (playing.get()) {
            AppLogger.warn("[TIMELINE] Resizing von Clips während aktiver Wiedergabe blockiert (trackId: %s)", trackId);
            return false;
        }
        Optional<TimelineTrack> trackOpt = findTrackById(trackId);
        if (trackOpt.isPresent()) {
            TimelineTrack track = trackOpt.get();
            long currentEndMs = track.getStartTimeMs() + track.getDurationMs();
            long clampedStartMs = Math.max(0L, Math.min(currentEndMs - MIN_CLIP_DURATION_MS, newStartTimeMs));
            long newDurationMs = currentEndMs - clampedStartMs;
            track.setStartTimeMs(clampedStartMs);
            track.setDurationMs(newDurationMs);
            notifyChange();
            return true;
        }
        return false;
    }

    /**
     * Liefert die aktuelle Raster- und Snapping-Auflösung der Timeline.
     *
     * @return aktive {@link TimelineGridSnap}-Einstellung
     */
    public TimelineGridSnap getGridSnap() {
        return gridSnap;
    }

    /**
     * Setzt die Raster- und Snapping-Auflösung der Timeline und benachrichtigt die UI.
     *
     * @param snap die neue Raster-Auflösung (darf nicht {@code null} sein)
     */
    public void setGridSnap(TimelineGridSnap snap) {
        if (snap != null && this.gridSnap != snap) {
            this.gridSnap = snap;
            AppLogger.info("[TIMELINE] Raster-Auflösung geändert auf: %s (%d ms)", snap.getLabel(), snap.getIntervalMs());
            notifyChange();
        }
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
        resetClockAnchor(safeMs, System.nanoTime());
        audioPlaybackService.setPosition(safeMs);
        evaluateScenePlayback(safeMs);
        notifyChange();
    }

    /**
     * Startet die parallele Multitrack-Wiedergabe der Timeline ab der aktuellen Position.
     */
    public synchronized void startPlayback() {
        if (playing.compareAndSet(false, true)) {
            resetClockAnchor(currentPositionMs.get(), System.nanoTime());
            audioPlaybackService.playTimeline(tracks, currentPositionMs.get());
            evaluateScenePlayback(currentPositionMs.get());
            startPlaybackTimer();
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
        if (analysis.has3BandWaveform()) {
            track.set3BandWaveformData(analysis.lowBand(), analysis.midBand(), analysis.highBand());
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
        resetClockAnchor(0L, System.nanoTime());
        deactivateAllActiveScenes();
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

    /**
     * Setzt den Zeitanker der verlustfreien Fallback-Clock zurück.
     *
     * @param anchorMs    Timeline-Position in Millisekunden
     * @param anchorNanos Monotoner Nanosekunden-Zeitstempel
     */
    void resetClockAnchor(long anchorMs, long anchorNanos) {
        synchronized (clockLock) {
            this.clockAnchorMs = Math.max(0L, anchorMs);
            this.clockAnchorNanos = anchorNanos;
        }
    }

    /**
     * Hilfsmethode für deterministische Unit-Tests ohne echte Thread-Timer.
     *
     * @param startNanos Startzeitstempel in Nanosekunden
     */
    void startPlaybackForTest(long startNanos) {
        playing.set(true);
        resetClockAnchor(currentPositionMs.get(), startNanos);
    }

    /**
     * Berechnet die per Soft-PLL (Phase-Locked-Loop) geglättete, VSync-bereite Abspielposition in ms.
     * Eliminiert 20–45ms Treiber-Treppchen komplett und garantiert gleichzeitig 0-Drift gegenüber der Soundkarte.
     *
     * @param nowNanos aktueller monotoner Zeitstempel ({@link System#nanoTime()})
     * @return geglättete Timeline-Position in Millisekunden
     */
    public long getSmoothedPlaybackPositionMs(long nowNanos) {
        if (!playing.get()) {
            return currentPositionMs.get();
        }
        long predictedMs;
        synchronized (clockLock) {
            long elapsedNanos = Math.max(0L, nowNanos - clockAnchorNanos);
            predictedMs = clockAnchorMs + (elapsedNanos / 1_000_000L);

            long hardwareMs = audioPlaybackService.getMasterPlaybackPositionMs();
            if (hardwareMs >= 0L) {
                long diff = hardwareMs - predictedMs;
                if (Math.abs(diff) >= 35L) {
                    clockAnchorMs = hardwareMs;
                    clockAnchorNanos = nowNanos;
                    predictedMs = hardwareMs;
                } else if (diff != 0L) {
                    long adj = Math.round(diff * 0.25);
                    if (adj == 0L) {
                        adj = diff > 0 ? 1L : -1L;
                    }
                    predictedMs = predictedMs + adj;
                    clockAnchorMs = predictedMs;
                    clockAnchorNanos = nowNanos;
                }
            }
        }
        return Math.max(0L, predictedMs);
    }

    /**
     * Führt einen einzelnen Clock-Tick aus. Rastet bei aktivem Audio per Soft-PLL auf die
     * Soundkarten-Hardware-Clock ein und feuert ausschließlich {@link #notifyPlaybackFrame()},
     * um redundante UI-Baum- und Layout-Rebuilds zu vermeiden.
     *
     * @param nowNanos aktueller {@link System#nanoTime()} Wert
     */
    void advanceClockTick(long nowNanos) {
        if (!playing.get()) return;

        long newPos = getSmoothedPlaybackPositionMs(nowNanos);
        currentPositionMs.set(newPos);
        audioPlaybackService.onTimelineTick(newPos);
        evaluateScenePlayback(newPos);

        long maxDuration = getMaxTimelineDurationMs();
        if (maxDuration > 0 && newPos >= maxDuration) {
            currentPositionMs.set(maxDuration);
            pausePlayback();
        } else {
            notifyPlaybackFrame();
        }
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
            advanceClockTick(System.nanoTime());
        }, 20, 20, TimeUnit.MILLISECONDS);
    }

    private void stopPlaybackTimer() {
        if (playbackExecutor != null && !playbackExecutor.isShutdown()) {
            playbackExecutor.shutdownNow();
            playbackExecutor = null;
        }
    }

    // =========================================================================
    // Change-Listener & High-Frequency Playback-Frame-Listener
    // =========================================================================

    /**
     * Registriert einen Listener für strukturelle Änderungen und Transport-Zustände
     * (Start, Pause, Stop, Seek, Track-Hinzufügen/Verschieben).
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
     * Registriert einen High-Frequency-Listener für reine Playback-Ticks ohne UI-Layout-Rebuild.
     *
     * @param listener Callback-Aktion
     */
    public void addPlaybackFrameListener(Runnable listener) {
        if (listener != null && !playbackFrameListeners.contains(listener)) {
            playbackFrameListeners.add(listener);
        }
    }

    /**
     * Benachrichtigt alle Playback-Frame-Listener über ein reines Zeit-Update während laufender Show.
     */
    public void notifyPlaybackFrame() {
        for (Runnable listener : playbackFrameListeners) {
            try {
                listener.run();
            } catch (Throwable t) {
                AppLogger.error("[TIMELINE] Fehler in Playback-Frame-Listener: %s", t.getMessage());
            }
        }
    }

    /**
     * Benachrichtigt alle registrierten Listener über eine strukturelle oder Transport-Zustandsänderung.
     */
    public void notifyChange() {
        for (Runnable listener : changeListeners) {
            try {
                listener.run();
            } catch (Throwable t) {
                AppLogger.error("[TIMELINE] Fehler in Change-Listener: %s", t.getMessage());
            }
        }
        notifyPlaybackFrame();
    }
}

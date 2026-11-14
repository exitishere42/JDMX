package de.exit.jdmx.service;

import de.exit.jdmx.core.model.TimelineTrack;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service für die Geschäftslogik der Show-Timeline (100% Core/Service-Schicht).
 * <p>
 * Verwaltet:
 * <ul>
 *     <li>Die Liste aller aktiven Spuren (Tracks) in der Show-Timeline</li>
 *     <li>Hinzufügen neuer Tracks im FL Studio Playlist-Stil mit fortlaufender Nummerierung</li>
 *     <li>Umbenennen von Spuren und Mute-Steuerung</li>
 *     <li>Benachrichtigung registrierter Listener bei Änderungen (Reaktivität)</li>
 * </ul>
 * </p>
 * <p>
 * <b>Thread-Sicherheit:</b> Vollständig thread-sicher über interne Synchronisation
 * und {@link CopyOnWriteArrayList}.
 * </p>
 */
public class TimelineService {

    /** Interne Track-Liste der Timeline. */
    private final List<TimelineTrack> tracks = new CopyOnWriteArrayList<>();

    /** Zähler für fortlaufende Standard-Track-Namen ("Track 1", "Track 2", ...). */
    private final AtomicInteger trackSequenceCounter = new AtomicInteger(1);

    /** Registrierte Listener für Daten- und Zustandsänderungen. */
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

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

    /**
     * Registriert einen Listener, der bei Änderungen an den Tracks aufgerufen wird.
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

package de.exit.jdmx.service;

import de.exit.jdmx.core.project.model.ProjectData;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Zentraler Dienst zur Verwaltung von Undo- und Redo-Schritten (Rückgängig & Wiederholen) in JDMX.
 * <p>
 * Basiert auf dem Memento-Entwurfsmuster unter Verwendung unveränderlicher {@link ProjectData}-Snapshots.
 * Verwaltet separate Stacks für Undo und Redo mit einer konfigurierbaren maximalen Historie.
 * </p>
 * <p>
 * <b>Thread-Sicherheit:</b>
 * Zustandsänderungen sind synchronisiert. Registrierte Listener werden nach jeder Stack-Mutation
 * unverzüglich benachrichtigt, um UI-Steuerelemente (Buttons, Menüeinträge, Tooltips) zu aktualisieren.
 * </p>
 */
public class UndoRedoService {

    /** Standardmäßige maximale Anzahl rückgängig machbarer Schritte. */
    public static final int DEFAULT_MAX_HISTORY = 50;

    /** Maximale Anzahl an Schritten in der Historie. */
    private final int maxHistory;

    /** Stack vergangener Projektzustände für Undo (Rückgängig). */
    private final Deque<ProjectData> undoStack = new ArrayDeque<>();

    /** Stack wiederherstellbarer Projektzustände für Redo (Wiederholen). */
    private final Deque<ProjectData> redoStack = new ArrayDeque<>();

    /** Listener für Zustandsänderungen der Stacks. */
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    /**
     * Erstellt einen neuen {@link UndoRedoService} mit der Standard-Historie (50 Schritte).
     */
    public UndoRedoService() {
        this(DEFAULT_MAX_HISTORY);
    }

    /**
     * Erstellt einen neuen {@link UndoRedoService} mit einer benutzerdefinierten Historien-Größe.
     *
     * @param maxHistory Maximale Anzahl gespeicherter Schritte (muss mindestens 1 sein)
     */
    public UndoRedoService(int maxHistory) {
        if (maxHistory < 1) {
            throw new IllegalArgumentException("maxHistory must be at least 1, but was: " + maxHistory);
        }
        this.maxHistory = maxHistory;
    }

    /**
     * Zeichnet einen Snapshot des Projektzustands vor einer Benutzeraktion auf dem Undo-Stack auf.
     * Ein neuer Bearbeitungsschritt leert den Redo-Stack vollständig.
     *
     * @param stateBeforeAction Der Projektzustand vor Durchführung der Aktion
     */
    public synchronized void pushUndoState(ProjectData stateBeforeAction) {
        if (stateBeforeAction == null) {
            return;
        }

        // Duplikate an der Spitze des Undo-Stacks vermeiden
        if (!undoStack.isEmpty() && isEquivalent(undoStack.peek(), stateBeforeAction)) {
            return;
        }

        undoStack.push(stateBeforeAction);

        // Historie auf maximale Größe beschränken (älteste Einträge entfernen)
        while (undoStack.size() > maxHistory) {
            undoStack.removeLast();
        }

        // Neuer Bearbeitungsschritt macht bisherige Redo-Kette ungültig
        redoStack.clear();

        fireChange();
    }

    /**
     * Prüft, ob ein Rückgängig-Schritt verfügbar ist.
     *
     * @return {@code true}, wenn der Undo-Stack nicht leer ist
     */
    public synchronized boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /**
     * Prüft, ob ein Wiederholen-Schritt verfügbar ist.
     *
     * @return {@code true}, wenn der Redo-Stack nicht leer ist
     */
    public synchronized boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * Führt eine Rückgängig-Operation durch.
     * Legt den aktuellen Zustand auf den Redo-Stack und liefert den vorherigen Zustand vom Undo-Stack.
     *
     * @param currentState Der gegenwärtige Projektzustand vor dem Rückgängigmachen
     * @return Der wiederherzustellende vorherige Projektzustand oder {@code null}, wenn kein Undo möglich ist
     */
    public synchronized ProjectData undo(ProjectData currentState) {
        if (!canUndo()) {
            return null;
        }

        if (currentState != null) {
            redoStack.push(currentState);
        }

        ProjectData previousState = undoStack.pop();
        fireChange();
        return previousState;
    }

    /**
     * Führt eine Wiederholen-Operation durch.
     * Legt den aktuellen Zustand auf den Undo-Stack und liefert den nächsten Zustand vom Redo-Stack.
     *
     * @param currentState Der gegenwärtige Projektzustand vor dem Wiederholen
     * @return Der wiederherzustellende nächste Projektzustand oder {@code null}, wenn kein Redo möglich ist
     */
    public synchronized ProjectData redo(ProjectData currentState) {
        if (!canRedo()) {
            return null;
        }

        if (currentState != null) {
            undoStack.push(currentState);
        }

        ProjectData nextState = redoStack.pop();
        fireChange();
        return nextState;
    }

    /**
     * Leert sowohl den Undo- als auch den Redo-Stack (z. B. beim Neuanlegen eines Projekts).
     */
    public synchronized void clear() {
        undoStack.clear();
        redoStack.clear();
        fireChange();
    }

    /**
     * Liefert die Anzahl der verfügbaren Undo-Schritte.
     *
     * @return Anzahl der Elemente auf dem Undo-Stack
     */
    public synchronized int getUndoCount() {
        return undoStack.size();
    }

    /**
     * Liefert die Anzahl der verfügbaren Redo-Schritte.
     *
     * @return Anzahl der Elemente auf dem Redo-Stack
     */
    public synchronized int getRedoCount() {
        return redoStack.size();
    }

    /**
     * Registriert einen Listener, der bei Änderungen der Stack-Zustände benachrichtigt wird.
     *
     * @param listener Callback
     */
    public void addChangeListener(Runnable listener) {
        if (listener != null) {
            changeListeners.add(listener);
        }
    }

    /**
     * Entfernt einen zuvor registrierten Listener.
     *
     * @param listener Callback
     */
    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    private void fireChange() {
        for (Runnable listener : changeListeners) {
            try {
                listener.run();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Prüft zwei Projektzustände auf strukturelle Gleichheit bzgl. Universen, Fixtures und Gruppen.
     */
    private boolean isEquivalent(ProjectData a, ProjectData b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return Objects.equals(a.universes(), b.universes())
                && Objects.equals(a.fixtures(), b.fixtures())
                && Objects.equals(a.groups(), b.groups());
    }
}

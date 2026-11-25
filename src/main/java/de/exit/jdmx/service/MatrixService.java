package de.exit.jdmx.service;

import de.exit.jdmx.core.log.AppLogger;
import de.exit.jdmx.core.model.MatrixButtonDefinition;
import de.exit.jdmx.core.model.MatrixGridDefinition;
import de.exit.jdmx.core.model.MatrixTargetType;
import de.exit.jdmx.core.model.MatrixTriggerMode;
import de.exit.jdmx.core.model.SceneDefinition;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service zur Steuerung der Button Matrix (Tab 3).
 * <p>
 * Verwaltet das logische Tastenraster, den aktuellen Aktivierungs- und Flash-Zustand jedes
 * Buttons und führt bei Benutzerinteraktionen thread-sicher DMX-, Szenen- und Chaser-Aktionen aus.
 * </p>
 */
public class MatrixService {

    private final DmxService dmxService;
    private final SceneService sceneService;
    private final ChaserService chaserService;

    private MatrixGridDefinition grid;
    private final Set<Integer> activeButtons = ConcurrentHashMap.newKeySet();
    private final Set<Integer> flashPressedButtons = ConcurrentHashMap.newKeySet();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    /**
     * Erstellt einen neuen {@link MatrixService}.
     *
     * @param dmxService    der DMX-Ausgabeservice (darf nicht {@code null} sein)
     * @param sceneService  der Szenen-Service (darf nicht {@code null} sein)
     * @param chaserService der Chaser-Service (darf nicht {@code null} sein)
     */
    public MatrixService(DmxService dmxService, SceneService sceneService, ChaserService chaserService) {
        this.dmxService = Objects.requireNonNull(dmxService, "dmxService darf nicht null sein");
        this.sceneService = Objects.requireNonNull(sceneService, "sceneService darf nicht null sein");
        this.chaserService = Objects.requireNonNull(chaserService, "chaserService darf nicht null sein");
        this.grid = new MatrixGridDefinition();
    }

    /**
     * Gibt das aktuelle Tastenraster zurück.
     *
     * @return das {@link MatrixGridDefinition}
     */
    public MatrixGridDefinition getGrid() {
        return grid;
    }

    /**
     * Setzt ein neues Tastenraster (z. B. beim Projekt-Laden).
     *
     * @param newGrid das neue Grid (darf nicht {@code null} sein)
     */
    public void setGrid(MatrixGridDefinition newGrid) {
        this.grid = Objects.requireNonNull(newGrid, "newGrid darf nicht null sein");
        stopAll();
        notifyMatrixChanged();
    }

    /**
     * Prüft, ob der Button am angegebenen Index aktuell aktiv ist.
     *
     * @param index der Button-Index
     * @return {@code true}, wenn aktiv, sonst {@code false}
     */
    public boolean isButtonActive(int index) {
        return activeButtons.contains(index);
    }

    /**
     * Prüft, ob der Button am angegebenen Index aktuell im Flash-Modus gedrückt gehalten wird.
     *
     * @param index der Button-Index
     * @return {@code true}, wenn Flash aktiv gehalten wird, sonst {@code false}
     */
    public boolean isButtonFlashPressed(int index) {
        return flashPressedButtons.contains(index);
    }

    /**
     * Gibt alle aktuell aktiven Button-Indizes zurück.
     *
     * @return unmodifizierbares Set der aktiven Indizes
     */
    public Set<Integer> getActiveButtonIndices() {
        return Collections.unmodifiableSet(activeButtons);
    }

    /**
     * Löst das Drücken eines Buttons aus (Mausklick oder MIDI Note On).
     *
     * @param index der Index des Buttons
     */
    public synchronized void pressButton(int index) {
        MatrixButtonDefinition btn = grid.getButton(index);
        if (btn == null || !btn.isAssigned()) {
            return;
        }

        if (btn.triggerMode() == MatrixTriggerMode.TOGGLE) {
            if (activeButtons.contains(index)) {
                deactivateButton(index, btn);
            } else {
                activateButton(index, btn);
            }
        } else if (btn.triggerMode() == MatrixTriggerMode.FLASH) {
            flashPressedButtons.add(index);
            activateButton(index, btn);
        }
        notifyMatrixChanged();
    }

    /**
     * Löst das Loslassen eines Buttons aus (Maus losgelassen oder MIDI Note Off).
     *
     * @param index der Index des Buttons
     */
    public synchronized void releaseButton(int index) {
        MatrixButtonDefinition btn = grid.getButton(index);
        if (btn == null || !btn.isAssigned()) {
            return;
        }

        if (btn.triggerMode() == MatrixTriggerMode.FLASH) {
            flashPressedButtons.remove(index);
            deactivateButton(index, btn);
            notifyMatrixChanged();
        }
    }

    private void activateButton(int index, MatrixButtonDefinition btn) {
        activeButtons.add(index);
        AppLogger.info("[MATRIX] Button " + index + " ('" + btn.name() + "') aktiviert (" + btn.triggerMode() + ")");

        if (btn.targetType() == MatrixTargetType.SCENE && btn.targetId() != null) {
            sceneService.applyScene(btn.targetId(), 0);
        } else if (btn.targetType() == MatrixTargetType.CHASER && btn.targetId() != null) {
            chaserService.play(btn.targetId());
        }
    }

    private void deactivateButton(int index, MatrixButtonDefinition btn) {
        activeButtons.remove(index);
        AppLogger.info("[MATRIX] Button " + index + " ('" + btn.name() + "') deaktiviert");

        if (btn.targetType() == MatrixTargetType.SCENE && btn.targetId() != null) {
            SceneDefinition scene = sceneService.getScene(btn.targetId());
            if (scene != null) {
                // Setze die Kanäle der Szene auf 0 zurück
                for (Map.Entry<Integer, Map<Integer, Integer>> uniEntry : scene.getChannelValues().entrySet()) {
                    int universe = uniEntry.getKey();
                    for (Integer channel : uniEntry.getValue().keySet()) {
                        dmxService.setChannel(universe, channel, 0);
                    }
                }
            }
        } else if (btn.targetType() == MatrixTargetType.CHASER && btn.targetId() != null) {
            chaserService.stop(btn.targetId());
        }
    }

    /**
     * Not-Aus / Global Stop: Deaktiviert sofort alle aktiven Buttons und stoppt alle laufenden Chaser.
     */
    public synchronized void stopAll() {
        for (Integer index : activeButtons) {
            MatrixButtonDefinition btn = grid.getButton(index);
            if (btn != null && btn.targetType() == MatrixTargetType.CHASER && btn.targetId() != null) {
                chaserService.stop(btn.targetId());
            } else if (btn != null && btn.targetType() == MatrixTargetType.SCENE && btn.targetId() != null) {
                SceneDefinition scene = sceneService.getScene(btn.targetId());
                if (scene != null) {
                    for (Map.Entry<Integer, Map<Integer, Integer>> uniEntry : scene.getChannelValues().entrySet()) {
                        int universe = uniEntry.getKey();
                        for (Integer channel : uniEntry.getValue().keySet()) {
                            dmxService.setChannel(universe, channel, 0);
                        }
                    }
                }
            }
        }
        activeButtons.clear();
        flashPressedButtons.clear();
        AppLogger.info("[MATRIX] stopAll ausgeführt: Alle Matrix-Buttons freigegeben.");
        notifyMatrixChanged();
    }

    /**
     * Weist dem Button am angegebenen Index ein Ziel (Szene oder Chaser) zu.
     *
     * @param index    der Button-Index
     * @param type     der Zieltyp ({@link MatrixTargetType})
     * @param targetId die ID des Ziels
     * @param name     der Anzeigename
     */
    public void assignTarget(int index, MatrixTargetType type, String targetId, String name) {
        MatrixButtonDefinition btn = grid.getButton(index);
        if (btn != null) {
            grid.setButton(btn.withTarget(type, targetId, name));
            notifyMatrixChanged();
        }
    }

    /**
     * Ändert den Trigger-Modus des Buttons am angegebenen Index.
     *
     * @param index der Button-Index
     * @param mode  der neue {@link MatrixTriggerMode}
     */
    public void setTriggerMode(int index, MatrixTriggerMode mode) {
        MatrixButtonDefinition btn = grid.getButton(index);
        if (btn != null) {
            grid.setButton(btn.withTriggerMode(mode));
            notifyMatrixChanged();
        }
    }

    /**
     * Ändert die Farbe des Buttons am angegebenen Index.
     *
     * @param index der Button-Index
     * @param color der neue Hex-Farbcode
     */
    public void setButtonColor(int index, String color) {
        MatrixButtonDefinition btn = grid.getButton(index);
        if (btn != null) {
            grid.setButton(btn.withColor(color));
            notifyMatrixChanged();
        }
    }

    /**
     * Ändert die Beschriftung des Buttons am angegebenen Index.
     *
     * @param index der Button-Index
     * @param name  der neue Name
     */
    public void setButtonName(int index, String name) {
        MatrixButtonDefinition btn = grid.getButton(index);
        if (btn != null) {
            grid.setButton(btn.withName(name));
            notifyMatrixChanged();
        }
    }

    /**
     * Setzt den Button am angegebenen Index zurück.
     *
     * @param index der Button-Index
     */
    public void clearButton(int index) {
        if (activeButtons.contains(index)) {
            MatrixButtonDefinition btn = grid.getButton(index);
            if (btn != null) {
                deactivateButton(index, btn);
            }
        }
        grid.clearButton(index);
        notifyMatrixChanged();
    }

    /**
     * Setzt alle Buttons im Raster zurück.
     */
    public void clearAllButtons() {
        stopAll();
        grid.clearAll();
        notifyMatrixChanged();
    }

    /**
     * Registriert einen Listener für Zustandsänderungen der Button Matrix.
     *
     * @param listener der auszuführende Listener
     */
    public void addMatrixChangeListener(Runnable listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Entfernt einen registrierten Listener.
     *
     * @param listener der zu entfernende Listener
     */
    public void removeMatrixChangeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyMatrixChanged() {
        for (Runnable l : listeners) {
            try {
                l.run();
            } catch (Exception e) {
                AppLogger.error("[MATRIX] Fehler im MatrixChangeListener", e);
            }
        }
    }
}

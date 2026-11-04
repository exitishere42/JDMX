package de.exit.jdmx.ui.stage;

import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;

/**
 * Der primäre Arbeitsbereich von Tab 1 (Stage & Fixtures).
 * <p>
 * Beherbergt oben die 2D-Bühne (Stage & U-Traverse) und unten das 24/7 geöffnete
 * Steuerungs-Dock mit den 3 Modulen (Intensität, Farbe, Position).
 * <p>
 * Garantiert über harte Clamping-Logik, dass die obere Bühne <b>immer mehr als 60%</b>
 * des vertikalen Platzes behält und nicht erdrückt werden kann.
 * </p>
 */
public class StageWorkspaceView extends SplitPane {

    /** Mindestanteil der vertikalen Höhe, die der oberen Bühne zusteht (60%). */
    public static final double MIN_STAGE_RATIO = 0.60;

    /** Standard-Startposition des vertikalen Splitters (68% Bühne, 32% Dock). */
    public static final double DEFAULT_STAGE_RATIO = 0.68;

    private final StageCanvasView stageCanvasView;
    private final DeviceControlDock controlDock;

    public StageWorkspaceView() {
        setOrientation(Orientation.VERTICAL);
        getStyleClass().add("stage-workspace-view");

        this.stageCanvasView = new StageCanvasView();
        this.controlDock = new DeviceControlDock();

        getItems().addAll(stageCanvasView, controlDock);

        // Standard-Teilung: 68% für die Bühne (mehr als 60%)
        setDividerPositions(DEFAULT_STAGE_RATIO);

        // Clamping-Schutz: Verhindert, dass das Dock weiter als 40% nach oben gezogen werden kann
        if (!getDividers().isEmpty()) {
            getDividers().getFirst().positionProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() < MIN_STAGE_RATIO) {
                    setDividerPositions(MIN_STAGE_RATIO);
                }
            });
        }
    }

    /**
     * Setzt die Divider-Position mit garantierter Einhaltung des 60%-Mindestplatzes für die Stage.
     *
     * @param requestedPosition Gewünschte Teilung (z. B. 0.68)
     */
    public void requestDividerPosition(double requestedPosition) {
        double clamped = Math.max(MIN_STAGE_RATIO, requestedPosition);
        setDividerPositions(clamped);
    }

    /**
     * Liefert die aktuelle Position des vertikalen Splitters.
     */
    public double getDividerPosition() {
        if (!getDividers().isEmpty()) {
            return getDividers().getFirst().getPosition();
        }
        return DEFAULT_STAGE_RATIO;
    }

    public StageCanvasView getStageCanvasView() {
        return stageCanvasView;
    }

    public DeviceControlDock getControlDock() {
        return controlDock;
    }
}

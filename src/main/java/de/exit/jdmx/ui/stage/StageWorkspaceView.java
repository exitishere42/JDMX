package de.exit.jdmx.ui.stage;

import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;

/**
 * Der primäre Arbeitsbereich von Tab 1 (Stage & Fixtures).
 * <p>
 * Beherbergt oben die saubere Arbeitsfläche der Bühne und unten das 24/7 geöffnete,
 * kompakte Steuerungs-Dock mit den 3 Modulen (Intensität, Farbe, Position).
 * <p>
 * Begrenzungs-Garantie:
 * <ul>
 *     <li>Nach unten kann das Dock <b>niemals kleiner</b> als seine Standard-Startposition werden.</li>
 *     <li>Nach oben kann das Dock gezogen werden, wobei die Bühne <b>immer mindestens 60%</b> behält.</li>
 * </ul>
 * </p>
 */
public class StageWorkspaceView extends SplitPane {

    /** Mindestanteil der vertikalen Höhe, die der oberen Bühne zusteht (60%). */
    public static final double MIN_STAGE_RATIO = 0.60;

    /** Standard-Startposition des vertikalen Splitters (ca. 82% Bühne, 18% Dock). */
    public static final double DEFAULT_STAGE_RATIO = 0.82;

    private final StageCanvasView stageCanvasView;
    private final DeviceControlDock controlDock;

    public StageWorkspaceView() {
        setOrientation(Orientation.VERTICAL);
        getStyleClass().add("stage-workspace-view");

        this.stageCanvasView = new StageCanvasView();
        this.controlDock = new DeviceControlDock();

        getItems().addAll(stageCanvasView, controlDock);

        // Das Dashboard darf beim Resizing des Fensters nicht unkontrolliert schrumpfen
        SplitPane.setResizableWithParent(controlDock, false);

        // Standard-Teilung: 82% für die Bühne
        setDividerPositions(DEFAULT_STAGE_RATIO);

        // Clamping-Schutz:
        // Nach oben begrenzt auf MIN_STAGE_RATIO (Bühne behält mind. 60%)
        // Nach unten fest arretiert auf DEFAULT_STAGE_RATIO (Dock kann nicht kleiner als Startzustand werden)
        if (!getDividers().isEmpty()) {
            getDividers().getFirst().positionProperty().addListener((obs, oldVal, newVal) -> {
                double val = newVal.doubleValue();
                if (val < MIN_STAGE_RATIO) {
                    setDividerPositions(MIN_STAGE_RATIO);
                } else if (val > DEFAULT_STAGE_RATIO) {
                    setDividerPositions(DEFAULT_STAGE_RATIO);
                }
            });
        }
    }

    /**
     * Setzt die Divider-Position mit garantierter Einhaltung des Bereichs zwischen
     * MIN_STAGE_RATIO (60%) und DEFAULT_STAGE_RATIO (Startposition).
     *
     * @param requestedPosition Gewünschte Teilung
     */
    public void requestDividerPosition(double requestedPosition) {
        double clamped = Math.max(MIN_STAGE_RATIO, Math.min(DEFAULT_STAGE_RATIO, requestedPosition));
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

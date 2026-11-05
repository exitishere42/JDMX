package de.exit.jdmx.ui.stage;

import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Der primäre Arbeitsbereich von Tab 1 (Stage & Fixtures).
 * <p>
 * Aufteilung:
 * <ul>
 *     <li><b>Links (Hauptbereich):</b> Vertikales SplitPane mit oberer Bühne und unterem Steuerungs-Dock (220px)</li>
 *     <li><b>Rechts (Volle Höhe):</b> {@link ProjectExplorerView} durchgehend von ganz oben bis ganz unten</li>
 * </ul>
 * Features:
 * <ul>
 *     <li>Flexibel dicker oder dünner ziehbar</li>
 *     <li>One-Click-Toggle: Klick auf die vertikale Haupt-Dragbar klappt den Project Explorer zu 100% auf 0px zu</li>
 *     <li>Andere Dragbars (z. B. im Steuerungsdock) werden durch strikte Parent-Prüfung nicht beeinflusst</li>
 * </ul>
 * </p>
 */
public class StageWorkspaceView extends SplitPane {

    /** Standard-Teilung des horizontalen Haupt-Splitters (78% Bühne/Dock, 22% Explorer). */
    public static final double DEFAULT_HORIZONTAL_RATIO = 0.78;

    /** Mindestanteil der vertikalen Höhe, die der oberen Bühne zusteht (60%). */
    public static final double MIN_STAGE_RATIO = 0.60;

    /** Standard-Startposition des vertikalen Splitters im linken Bereich (ca. 78% Bühne, 22% Dock). */
    public static final double DEFAULT_STAGE_RATIO = 0.78;

    private final StageCanvasView stageCanvasView;
    private final DeviceControlDock controlDock;
    private final SplitPane stageAndDockPane;
    private final ProjectExplorerView projectExplorerView;

    private double lastExpandedRatio = DEFAULT_HORIZONTAL_RATIO;
    private boolean isCollapsed = false;

    private double dragStartX = 0;
    private double dragStartY = 0;

    public StageWorkspaceView() {
        setOrientation(Orientation.HORIZONTAL);
        getStyleClass().add("stage-workspace-view");

        // 1. Linker Hauptbereich (Bühne oben + 220px Steuerungsdock unten)
        this.stageCanvasView = new StageCanvasView();
        this.controlDock = new DeviceControlDock();

        this.stageAndDockPane = new SplitPane();
        stageAndDockPane.setOrientation(Orientation.VERTICAL);
        stageAndDockPane.getStyleClass().add("stage-and-dock-split");
        VBox.setVgrow(stageAndDockPane, Priority.ALWAYS);

        stageAndDockPane.getItems().addAll(stageCanvasView, controlDock);
        SplitPane.setResizableWithParent(controlDock, false);
        stageAndDockPane.setDividerPositions(DEFAULT_STAGE_RATIO);

        // Clamping-Schutz im linken Bereich:
        if (!stageAndDockPane.getDividers().isEmpty()) {
            stageAndDockPane.getDividers().getFirst().positionProperty().addListener((obs, oldVal, newVal) -> {
                double val = newVal.doubleValue();
                if (val < MIN_STAGE_RATIO) {
                    stageAndDockPane.setDividerPositions(MIN_STAGE_RATIO);
                } else if (val > DEFAULT_STAGE_RATIO) {
                    stageAndDockPane.setDividerPositions(DEFAULT_STAGE_RATIO);
                }
            });
        }

        // 2. Rechter Bereich: Project Explorer über die volle Höhe
        this.projectExplorerView = new ProjectExplorerView();

        // 3. Horizontales Haupt-SplitPane zusammenbauen
        getItems().addAll(stageAndDockPane, projectExplorerView);
        SplitPane.setResizableWithParent(projectExplorerView, false);
        setDividerPositions(DEFAULT_HORIZONTAL_RATIO);

        // 4. One-Click Toggle ausschließlich auf die vertikale Haupt-Dragbar verdrahten
        setupDividerClickHandler();
    }

    private void setupDividerClickHandler() {
        Platform.runLater(() -> {
            for (Node node : lookupAll(".split-pane-divider")) {
                // Strikte Isolierung: Nur den Divider registrieren, der direkt zu diesem SplitPane gehört!
                if (node.getParent() != this) {
                    continue;
                }

                node.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
                    if (e.getButton() == MouseButton.PRIMARY) {
                        dragStartX = e.getScreenX();
                        dragStartY = e.getScreenY();
                    }
                });

                node.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
                    if (e.getButton() == MouseButton.PRIMARY) {
                        double deltaX = Math.abs(e.getScreenX() - dragStartX);
                        double deltaY = Math.abs(e.getScreenY() - dragStartY);
                        // Wenn es ein Klick war (weniger als 4px Bewegung) -> Zuklappen / Aufklappen
                        if (deltaX < 4 && deltaY < 4) {
                            toggleProjectExplorer();
                            e.consume();
                        }
                    }
                });
            }
        });
    }

    /**
     * Schaltet die Anzeige des Project Explorers vollständig um (Zuklappen auf 0px / Aufklappen).
     */
    public void toggleProjectExplorer() {
        if (!getDividers().isEmpty()) {
            if (!isCollapsed) {
                // Aktuelle Position merken und auf 1.0 (ganz zu) setzen
                double currentPos = getDividers().getFirst().getPosition();
                if (currentPos < 0.98) {
                    lastExpandedRatio = currentPos;
                }
                projectExplorerView.setMinWidth(0);
                projectExplorerView.setMaxWidth(0);
                projectExplorerView.setVisible(false);
                projectExplorerView.setManaged(false);
                setDividerPositions(1.0);
                isCollapsed = true;
            } else {
                // Vollständig wieder auf vorherige Breite aufklappen
                projectExplorerView.setVisible(true);
                projectExplorerView.setManaged(true);
                projectExplorerView.setMinWidth(140.0);
                projectExplorerView.setMaxWidth(Double.MAX_VALUE);
                setDividerPositions(lastExpandedRatio > 0 && lastExpandedRatio < 0.98 ? lastExpandedRatio : DEFAULT_HORIZONTAL_RATIO);
                isCollapsed = false;
            }
        }
    }

    public boolean isProjectExplorerCollapsed() {
        return isCollapsed;
    }

    public StageCanvasView getStageCanvasView() {
        return stageCanvasView;
    }

    public DeviceControlDock getControlDock() {
        return controlDock;
    }

    public ProjectExplorerView getProjectExplorerView() {
        return projectExplorerView;
    }

    public SplitPane getStageAndDockPane() {
        return stageAndDockPane;
    }
}

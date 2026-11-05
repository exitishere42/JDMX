package de.exit.jdmx.ui.stage;

import de.exit.jdmx.service.ProjectExplorerService;
import de.exit.jdmx.ui.icon.LucideIcon;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.Objects;

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
 *     <li>Flexibel dicker oder dünner ziehbar über die vertikale Haupt-Dragbar</li>
 *     <li>Einklappen über das Symbol {@code arrow-right-to-line} neben dem Text {@code Project Explorer}</li>
 *     <li>Ausklappen über das Symbol {@code arrow-left-to-line} an der rechten Kante der Bühne</li>
 *     <li>Strikte Entkopplung: Klick auf die Dragbars dient ausschließlich dem Ziehen</li>
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
    private final Button btnExpandProjectExplorer;
    private final ProjectExplorerService explorerService;

    private double lastExpandedRatio = DEFAULT_HORIZONTAL_RATIO;
    private boolean isCollapsed = false;

    public StageWorkspaceView() {
        this(new ProjectExplorerService());
    }

    public StageWorkspaceView(ProjectExplorerService explorerService) {
        this.explorerService = Objects.requireNonNull(explorerService, "explorerService must not be null");
        setOrientation(Orientation.HORIZONTAL);
        getStyleClass().add("stage-workspace-view");

        // 1. Linker Hauptbereich (Bühne oben + 220px Steuerungsdock unten)
        this.stageCanvasView = new StageCanvasView(explorerService);
        this.controlDock = new DeviceControlDock();

        // Ausklapp-Button an der rechten Kante der Bühne vorbereiten (arrow-left-to-line)
        this.btnExpandProjectExplorer = new Button();
        btnExpandProjectExplorer.getStyleClass().add("project-explorer-expand-btn");
        btnExpandProjectExplorer.setGraphic(new LucideIcon("arrow-left-to-line", 14, Color.web("#9CA3AF")));
        btnExpandProjectExplorer.setTooltip(new Tooltip("Ausklappen"));
        btnExpandProjectExplorer.setVisible(false);
        btnExpandProjectExplorer.setManaged(false);
        btnExpandProjectExplorer.setOnAction(e -> toggleProjectExplorer());

        StackPane.setAlignment(btnExpandProjectExplorer, Pos.TOP_RIGHT);
        StackPane.setMargin(btnExpandProjectExplorer, new Insets(6, 6, 0, 0));
        stageCanvasView.getChildren().add(btnExpandProjectExplorer);

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
        this.projectExplorerView = new ProjectExplorerView(explorerService);
        projectExplorerView.setOnToggleCollapse(this::toggleProjectExplorer);

        // 3. Horizontales Haupt-SplitPane zusammenbauen
        getItems().addAll(stageAndDockPane, projectExplorerView);
        SplitPane.setResizableWithParent(projectExplorerView, false);
        setDividerPositions(DEFAULT_HORIZONTAL_RATIO);
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
                btnExpandProjectExplorer.setVisible(true);
                btnExpandProjectExplorer.setManaged(true);
            } else {
                // Vollständig wieder auf vorherige Breite aufklappen
                btnExpandProjectExplorer.setVisible(false);
                btnExpandProjectExplorer.setManaged(false);
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

    public Button getBtnExpandProjectExplorer() {
        return btnExpandProjectExplorer;
    }

    public SplitPane getStageAndDockPane() {
        return stageAndDockPane;
    }

    public ProjectExplorerService getExplorerService() {
        return explorerService;
    }
}

package de.exit.jdmx.ui.matrix;

import de.exit.jdmx.service.ChaserService;
import de.exit.jdmx.service.DmxService;
import de.exit.jdmx.service.MatrixService;
import de.exit.jdmx.service.ProjectExplorerService;
import de.exit.jdmx.service.SceneService;
import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.stage.ProjectExplorerView;
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
 * Der primäre Arbeitsbereich von Tab 3 (Button Matrix).
 * <p>
 * Folgt exakt dem ergonomischen Zwei-Spalten-Layout der Stage View ({@code StageWorkspaceView}):
 * <ul>
 *     <li><b>Links (Hauptbereich):</b> Vertikales SplitPane mit oberem Button-Grid ({@link MatrixGridView})
 *         und unterem 220px Steuerungs-Dock ({@link MatrixControlDock})</li>
 *     <li><b>Rechts (Volle Höhe):</b> Durchgehender {@link ProjectExplorerView} mit Einklapp- und Ausklapp-Funktion</li>
 * </ul>
 * </p>
 */
public class MatrixWorkspaceView extends SplitPane {

    /** Standard-Teilung des horizontalen Haupt-Splitters (78% Grid/Dock, 22% Explorer). */
    public static final double DEFAULT_HORIZONTAL_RATIO = 0.78;

    /** Mindestanteil der vertikalen Höhe für das obere Button-Grid (60%). */
    public static final double MIN_GRID_RATIO = 0.60;

    /** Standard-Startposition des vertikalen Splitters im linken Bereich (ca. 78% Grid, 22% Dock). */
    public static final double DEFAULT_GRID_RATIO = 0.78;

    private final MatrixService matrixService;
    private final ProjectExplorerService explorerService;

    private final MatrixGridView matrixGridView;
    private final MatrixControlDock controlDock;
    private final SplitPane gridAndDockPane;
    private final ProjectExplorerView projectExplorerView;
    private final Button btnExpandProjectExplorer;

    private double lastExpandedRatio = DEFAULT_HORIZONTAL_RATIO;
    private boolean isCollapsed = false;

    /**
     * Erstellt einen neuen {@link MatrixWorkspaceView}.
     *
     * @param matrixService   der MatrixService (darf nicht {@code null} sein)
     * @param explorerService der ExplorerService (darf nicht {@code null} sein)
     */
    public MatrixWorkspaceView(MatrixService matrixService, ProjectExplorerService explorerService) {
        this(matrixService, explorerService, null, null);
    }

    /**
     * Vollständiger Konstruktor für den {@link MatrixWorkspaceView}.
     *
     * @param matrixService   der {@link MatrixService} (darf nicht {@code null} sein)
     * @param explorerService der {@link ProjectExplorerService} (darf nicht {@code null} sein)
     * @param sceneService    der optionale {@link SceneService}
     * @param chaserService   der optionale {@link ChaserService}
     */
    public MatrixWorkspaceView(MatrixService matrixService, ProjectExplorerService explorerService,
                               SceneService sceneService, ChaserService chaserService) {
        this.matrixService = Objects.requireNonNull(matrixService, "matrixService darf nicht null sein");
        this.explorerService = Objects.requireNonNull(explorerService, "explorerService darf nicht null sein");

        setOrientation(Orientation.HORIZONTAL);
        getStyleClass().add("matrix-workspace-view");

        // 1. Linker Hauptbereich (Button-Grid oben + 220px Steuerungsdock unten)
        this.matrixGridView = new MatrixGridView(matrixService, explorerService);
        this.controlDock = new MatrixControlDock(matrixService);

        // Ausklapp-Button an der rechten Kante des Grids vorbereiten
        this.btnExpandProjectExplorer = new Button();
        btnExpandProjectExplorer.getStyleClass().add("project-explorer-expand-btn");
        btnExpandProjectExplorer.setGraphic(new LucideIcon("arrow-left-to-line", 14, Color.web("#9CA3AF")));
        btnExpandProjectExplorer.setTooltip(new Tooltip("Ausklappen"));
        btnExpandProjectExplorer.setVisible(false);
        btnExpandProjectExplorer.setManaged(false);
        btnExpandProjectExplorer.setOnAction(e -> toggleProjectExplorer());

        StackPane gridWrapper = new StackPane(matrixGridView, btnExpandProjectExplorer);
        StackPane.setAlignment(btnExpandProjectExplorer, Pos.TOP_RIGHT);
        StackPane.setMargin(btnExpandProjectExplorer, new Insets(8, 8, 0, 0));

        this.gridAndDockPane = new SplitPane();
        gridAndDockPane.setOrientation(Orientation.VERTICAL);
        gridAndDockPane.getStyleClass().add("matrix-and-dock-split");
        VBox.setVgrow(gridAndDockPane, Priority.ALWAYS);

        gridAndDockPane.getItems().addAll(gridWrapper, controlDock);
        SplitPane.setResizableWithParent(controlDock, false);
        gridAndDockPane.setDividerPositions(DEFAULT_GRID_RATIO);

        // Clamping-Schutz im vertikalen SplitPane
        if (!gridAndDockPane.getDividers().isEmpty()) {
            gridAndDockPane.getDividers().getFirst().positionProperty().addListener((obs, oldVal, newVal) -> {
                double val = newVal.doubleValue();
                if (val < MIN_GRID_RATIO) {
                    gridAndDockPane.setDividerPositions(MIN_GRID_RATIO);
                } else if (val > DEFAULT_GRID_RATIO) {
                    gridAndDockPane.setDividerPositions(DEFAULT_GRID_RATIO);
                }
            });
        }

        // 2. Rechter Bereich: Project Explorer über die volle Höhe
        this.projectExplorerView = new ProjectExplorerView(explorerService, sceneService, chaserService);
        projectExplorerView.setOnToggleCollapse(this::toggleProjectExplorer);

        // 3. Horizontales Haupt-SplitPane zusammenbauen
        getItems().addAll(gridAndDockPane, projectExplorerView);
        SplitPane.setResizableWithParent(projectExplorerView, false);
        setDividerPositions(DEFAULT_HORIZONTAL_RATIO);

        // 4. Kopplung: Auswahl eines Buttons im Grid aktualisiert das Dock
        matrixGridView.setOnButtonSelected(controlDock::selectButton);

        // Standardmäßig den ersten Button auswählen
        matrixGridView.selectButton(0);
    }

    /**
     * Schaltet das Ein- und Ausklappen des Project Explorers um.
     */
    public void toggleProjectExplorer() {
        if (!isCollapsed) {
            if (!getDividers().isEmpty()) {
                lastExpandedRatio = getDividers().getFirst().getPosition();
            }
            projectExplorerView.setVisible(false);
            projectExplorerView.setManaged(false);
            getItems().remove(projectExplorerView);
            btnExpandProjectExplorer.setVisible(true);
            btnExpandProjectExplorer.setManaged(true);
            isCollapsed = true;
        } else {
            btnExpandProjectExplorer.setVisible(false);
            btnExpandProjectExplorer.setManaged(false);
            projectExplorerView.setVisible(true);
            projectExplorerView.setManaged(true);
            if (!getItems().contains(projectExplorerView)) {
                getItems().add(projectExplorerView);
                SplitPane.setResizableWithParent(projectExplorerView, false);
            }
            setDividerPositions(lastExpandedRatio);
            isCollapsed = false;
        }
    }

    /**
     * Gibt an, ob der rechte Project Explorer eingeklappt ist.
     *
     * @return {@code true}, wenn eingeklappt
     */
    public boolean isExplorerCollapsed() {
        return isCollapsed;
    }

    /**
     * Gibt die obere Grid-Ansicht zurück.
     *
     * @return das {@link MatrixGridView}
     */
    public MatrixGridView getMatrixGridView() {
        return matrixGridView;
    }

    /**
     * Gibt das untere Steuerungsdock zurück.
     *
     * @return das {@link MatrixControlDock}
     */
    public MatrixControlDock getControlDock() {
        return controlDock;
    }

    /**
     * Gibt den rechten Project Explorer zurück.
     *
     * @return der {@link ProjectExplorerView}
     */
    public ProjectExplorerView getProjectExplorerView() {
        return projectExplorerView;
    }

    /**
     * Gibt den zugrunde liegenden MatrixService zurück.
     *
     * @return der {@link MatrixService}
     */
    public MatrixService getMatrixService() {
        return matrixService;
    }
}

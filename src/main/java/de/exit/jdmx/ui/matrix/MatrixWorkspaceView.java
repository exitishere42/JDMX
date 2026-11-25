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
 *     <li><b>Links (Volle Höhe):</b> Vollflächiges Button-Grid ({@link MatrixGridView})</li>
 *     <li><b>Rechts (Volle Höhe):</b> Durchgehender {@link ProjectExplorerView} mit Einklapp- und Ausklapp-Funktion</li>
 * </ul>
 * </p>
 */
public class MatrixWorkspaceView extends SplitPane {

    /** Standard-Teilung des horizontalen Haupt-Splitters (78% Grid, 22% Explorer). */
    public static final double DEFAULT_HORIZONTAL_RATIO = 0.78;

    private final MatrixService matrixService;
    private final ProjectExplorerService explorerService;

    private final MatrixGridView matrixGridView;
    private final StackPane gridWrapper;
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

        // 1. Linker Hauptbereich: Vollflächiges Button-Grid über die gesamte Höhe
        this.matrixGridView = new MatrixGridView(matrixService, explorerService);

        // Ausklapp-Button an der rechten Kante des Grids vorbereiten
        this.btnExpandProjectExplorer = new Button();
        btnExpandProjectExplorer.getStyleClass().add("project-explorer-expand-btn");
        btnExpandProjectExplorer.setGraphic(new LucideIcon("arrow-left-to-line", 14, Color.web("#9CA3AF")));
        btnExpandProjectExplorer.setTooltip(new Tooltip("Ausklappen"));
        btnExpandProjectExplorer.setVisible(false);
        btnExpandProjectExplorer.setManaged(false);
        btnExpandProjectExplorer.setOnAction(e -> toggleProjectExplorer());

        this.gridWrapper = new StackPane(matrixGridView, btnExpandProjectExplorer);
        StackPane.setAlignment(btnExpandProjectExplorer, Pos.TOP_RIGHT);
        StackPane.setMargin(btnExpandProjectExplorer, new Insets(8, 8, 0, 0));

        // 2. Rechter Bereich: Project Explorer über die volle Höhe
        this.projectExplorerView = new ProjectExplorerView(explorerService, sceneService, chaserService);
        projectExplorerView.setOnToggleCollapse(this::toggleProjectExplorer);

        // 3. Horizontales Haupt-SplitPane zusammenbauen
        getItems().addAll(gridWrapper, projectExplorerView);
        SplitPane.setResizableWithParent(projectExplorerView, false);
        setDividerPositions(DEFAULT_HORIZONTAL_RATIO);
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

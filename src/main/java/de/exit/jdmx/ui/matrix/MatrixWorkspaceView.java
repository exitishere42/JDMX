package de.exit.jdmx.ui.matrix;

import de.exit.jdmx.service.ChaserService;
import de.exit.jdmx.service.MatrixService;
import de.exit.jdmx.service.ProjectExplorerService;
import de.exit.jdmx.service.SceneService;
import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.stage.ProjectExplorerView;
import de.exit.jdmx.ui.theme.StudioTheme;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.util.Objects;

/**
 * Der primäre Arbeitsbereich von Tab 3 (Button Matrix).
 * <p>
 * Folgt exakt dem ergonomischen Zwei-Spalten-Layout der Stage View ({@code StageWorkspaceView}):
 * <ul>
 *     <li><b>Links:</b> Vollflächiger, sauberer Hauptarbeitsbereich ({@link StackPane})</li>
 *     <li><b>Rechts (Volle Höhe):</b> Durchgehender {@link ProjectExplorerView} mit Einklapp- und Ausklapp-Funktion</li>
 * </ul>
 * </p>
 */
public class MatrixWorkspaceView extends SplitPane {

    /** Standard-Teilung des horizontalen Haupt-Splitters (78% Hauptbereich, 22% Explorer). */
    public static final double DEFAULT_HORIZONTAL_RATIO = 0.78;

    private final MatrixService matrixService;
    private final ProjectExplorerService explorerService;

    private final StackPane mainArea;
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

        // 1. Linker Hauptbereich: Sauberer, vollflächiger Arbeitsbereich (wie bei Stage)
        this.mainArea = new StackPane();
        mainArea.setStyle("-fx-background-color: " + StudioTheme.HEX_BG_BASE + ";");

        // Ausklapp-Button an der rechten Kante des Hauptbereichs vorbereiten
        this.btnExpandProjectExplorer = new Button();
        btnExpandProjectExplorer.getStyleClass().add("project-explorer-expand-btn");
        btnExpandProjectExplorer.setGraphic(new LucideIcon("arrow-left-to-line", 14, Color.web("#9CA3AF")));
        btnExpandProjectExplorer.setTooltip(new Tooltip("Ausklappen"));
        btnExpandProjectExplorer.setVisible(false);
        btnExpandProjectExplorer.setManaged(false);
        btnExpandProjectExplorer.setOnAction(e -> toggleProjectExplorer());

        StackPane.setAlignment(btnExpandProjectExplorer, Pos.TOP_RIGHT);
        StackPane.setMargin(btnExpandProjectExplorer, new Insets(6, 6, 0, 0));
        mainArea.getChildren().add(btnExpandProjectExplorer);

        // 2. Rechter Bereich: Project Explorer über die volle Höhe
        this.projectExplorerView = new ProjectExplorerView(explorerService, sceneService, chaserService);
        projectExplorerView.setOnToggleCollapse(this::toggleProjectExplorer);

        // 3. Horizontales Haupt-SplitPane zusammenbauen
        getItems().addAll(mainArea, projectExplorerView);
        SplitPane.setResizableWithParent(projectExplorerView, false);
        setDividerPositions(DEFAULT_HORIZONTAL_RATIO);

        // Breite überwachen, um beim ersten Rendern / Tab-Wechsel den Divider zuverlässig zu setzen
        widthProperty().addListener((obs, oldW, newW) -> {
            if (newW.doubleValue() > 0 && !isCollapsed) {
                javafx.application.Platform.runLater(() -> {
                    if (!isCollapsed && !getDividers().isEmpty()) {
                        setDividerPositions(lastExpandedRatio);
                    }
                });
            }
        });
    }

    /**
     * Schaltet das Ein- und Ausklappen des Project Explorers um (wie in StageWorkspaceView).
     */
    public void toggleProjectExplorer() {
        if (!getDividers().isEmpty()) {
            if (!isCollapsed) {
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
                btnExpandProjectExplorer.setVisible(false);
                btnExpandProjectExplorer.setManaged(false);
                projectExplorerView.setVisible(true);
                projectExplorerView.setManaged(true);
                projectExplorerView.setMinWidth(ProjectExplorerView.DEFAULT_MIN_WIDTH);
                projectExplorerView.setMaxWidth(Double.MAX_VALUE);
                setDividerPositions(lastExpandedRatio > 0 && lastExpandedRatio < 0.98 ? lastExpandedRatio : DEFAULT_HORIZONTAL_RATIO);
                isCollapsed = false;
            }
        } else {
            isCollapsed = !isCollapsed;
            projectExplorerView.setVisible(!isCollapsed);
            projectExplorerView.setManaged(!isCollapsed);
            btnExpandProjectExplorer.setVisible(isCollapsed);
            btnExpandProjectExplorer.setManaged(isCollapsed);
        }
    }

    /**
     * Liefert die aktuelle Position des horizontalen Haupt-Splitters (zwischen Arbeitsbereich und Explorer).
     *
     * @return die Divider-Position (0.0 bis 1.0)
     */
    public double getHorizontalDividerPosition() {
        if (lastExpandedRatio > 0.0) {
            return lastExpandedRatio;
        }
        if (!getDividers().isEmpty()) {
            return getDividers().getFirst().getPosition();
        }
        return lastExpandedRatio;
    }

    /**
     * Setzt die Position des horizontalen Haupt-Splitters synchron (z. B. aus anderen Workspace-Tabs).
     *
     * @param ratio die gewünschte Divider-Position
     */
    public void setHorizontalDividerPosition(double ratio) {
        if (ratio > 0.0 && ratio < 0.98) {
            this.lastExpandedRatio = ratio;
            if (!isCollapsed) {
                setDividerPositions(ratio);
            }
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
     * Gibt den linken Hauptarbeitsbereich zurück.
     *
     * @return die {@link StackPane}
     */
    public StackPane getMainArea() {
        return mainArea;
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

    /**
     * Gibt den ProjectExplorerService zurück.
     *
     * @return der {@link ProjectExplorerService}
     */
    public ProjectExplorerService getExplorerService() {
        return explorerService;
    }
}

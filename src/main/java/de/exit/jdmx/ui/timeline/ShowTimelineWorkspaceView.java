package de.exit.jdmx.ui.timeline;

import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.theme.StudioTheme;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.StackPane;

/**
 * Haupt-Workspace von Tab 4 (Show-Timeline).
 * <p>
 * Teilt den Bereich horizontal auf:
 * <ul>
 *     <li><b>Links (Hauptbereich):</b> Platzhalter für die zukünftige Timeline-Canvas</li>
 *     <li><b>Rechts:</b> {@link ShowTimelineExplorerView} mit kompaktem Tab-Header und TreeView</li>
 * </ul>
 * </p>
 */
public class ShowTimelineWorkspaceView extends SplitPane {

    /** Standard-Trennposition: 76% für die Timeline, 24% für den Explorer. */
    public static final double DEFAULT_SPLIT_RATIO = 0.76;

    private final ShowTimelineExplorerView explorerView;

    /**
     * Erstellt den Show-Timeline Workspace mit Platzhalter und Explorer-Seitenleiste.
     */
    public ShowTimelineWorkspaceView() {
        setOrientation(Orientation.HORIZONTAL);
        getStyleClass().add("timeline-workspace-view");

        // --- Linker Hauptbereich (Timeline-Platzhalter) ---
        StackPane timelinePlaceholder = new StackPane();
        timelinePlaceholder.getStyleClass().add("timeline-canvas-placeholder");

        LucideIcon icon = new LucideIcon("audio-lines", 32, StudioTheme.COLOR_ICON_TIMELINE);
        Label lbl = new Label("Show-Timeline");
        lbl.getStyleClass().add("timeline-placeholder-label");
        lbl.setGraphic(icon);
        lbl.setGraphicTextGap(10);
        timelinePlaceholder.getChildren().add(lbl);
        StackPane.setAlignment(lbl, Pos.CENTER);

        // --- Rechter Bereich (Explorer) ---
        this.explorerView = new ShowTimelineExplorerView();

        getItems().addAll(timelinePlaceholder, explorerView);
        setDividerPositions(DEFAULT_SPLIT_RATIO);

        // Explorer darf nicht kleiner als seine Mindestbreite werden
        SplitPane.setResizableWithParent(explorerView, false);
    }

    /**
     * Gibt die Show-Timeline Explorer-Seitenleiste zurück.
     *
     * @return der {@link ShowTimelineExplorerView}
     */
    public ShowTimelineExplorerView getExplorerView() {
        return explorerView;
    }
}

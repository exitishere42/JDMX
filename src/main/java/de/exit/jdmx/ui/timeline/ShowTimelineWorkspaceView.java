package de.exit.jdmx.ui.timeline;

import de.exit.jdmx.service.TimelineService;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;

import java.util.Objects;

/**
 * Haupt-Workspace von Tab 4 (Show-Timeline).
 * <p>
 * Teilt den Bereich horizontal auf:
 * <ul>
 *     <li><b>Links (Hauptbereich):</b> {@link TimelineMainView} mit Playlist-Grid und Trackboxen</li>
 *     <li><b>Rechts:</b> {@link ShowTimelineExplorerView} mit kompaktem Tab-Header und TreeView</li>
 * </ul>
 * </p>
 */
public class ShowTimelineWorkspaceView extends SplitPane {

    /** Standard-Trennposition: 76% für die Timeline, 24% für den Explorer. */
    public static final double DEFAULT_SPLIT_RATIO = 0.76;

    private final TimelineService timelineService;
    private final TimelineMainView timelineMainView;
    private final ShowTimelineExplorerView explorerView;

    /**
     * Erstellt den Show-Timeline Workspace mit Standard-TimelineService.
     */
    public ShowTimelineWorkspaceView() {
        this(new TimelineService());
    }

    /**
     * Erstellt den Show-Timeline Workspace mit dem gegebenen TimelineService.
     *
     * @param timelineService der {@link TimelineService} (darf nicht {@code null} sein)
     */
    public ShowTimelineWorkspaceView(TimelineService timelineService) {
        this.timelineService = Objects.requireNonNull(timelineService, "timelineService must not be null");
        setOrientation(Orientation.HORIZONTAL);
        getStyleClass().add("timeline-workspace-view");

        // --- Linker Hauptbereich (Timeline Playlist) ---
        this.timelineMainView = new TimelineMainView(this.timelineService);

        // --- Rechter Bereich (Explorer) ---
        this.explorerView = new ShowTimelineExplorerView(this.timelineService);

        getItems().addAll(timelineMainView, explorerView);
        setDividerPositions(DEFAULT_SPLIT_RATIO);

        // Explorer darf nicht kleiner als seine Mindestbreite werden
        SplitPane.setResizableWithParent(explorerView, false);
    }

    /**
     * Gibt die Hauptansicht der Show-Timeline zurück.
     *
     * @return die {@link TimelineMainView}
     */
    public TimelineMainView getTimelineMainView() {
        return timelineMainView;
    }

    /**
     * Gibt den Timeline-Service zurück.
     *
     * @return der {@link TimelineService}
     */
    public TimelineService getTimelineService() {
        return timelineService;
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

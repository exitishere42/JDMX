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

    /** Standard-Trennposition: 78% für die Timeline, 22% für den Explorer (identisch mit dem Stage-Tab). */
    public static final double DEFAULT_SPLIT_RATIO = 0.78;

    private final TimelineService timelineService;
    private final TimelineMainView timelineMainView;
    private final ShowTimelineExplorerView explorerView;

    private double lastExpandedRatio = DEFAULT_SPLIT_RATIO;

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

        // Divider-Listener zur dauerhaften Speicherung der relativen Position bei Benutzerinteraktion
        if (!getDividers().isEmpty()) {
            getDividers().getFirst().positionProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    double pos = newVal.doubleValue();
                    if (pos > 0.0 && pos < 0.98) {
                        this.lastExpandedRatio = pos;
                    }
                }
            });
        }

        // Explorer darf nicht kleiner als seine Mindestbreite werden
        SplitPane.setResizableWithParent(explorerView, false);

        // Breite überwachen, um beim ersten Rendern / Tab-Wechsel den Divider zuverlässig zu setzen
        widthProperty().addListener((obs, oldW, newW) -> {
            if (newW != null && newW.doubleValue() > 0) {
                javafx.application.Platform.runLater(() -> {
                    if (!getDividers().isEmpty()) {
                        setDividerPositions(lastExpandedRatio);
                    }
                });
            }
        });
    }

    /**
     * Liefert die aktuelle Position des horizontalen Haupt-Splitters (zwischen Timeline und Explorer).
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
     * Setzt die Position des horizontalen Haupt-Splitters (zur Synchronisation mit dem Stage-Tab).
     *
     * @param ratio die gewünschte Divider-Position
     */
    public void setHorizontalDividerPosition(double ratio) {
        if (ratio > 0.0 && ratio < 0.98) {
            this.lastExpandedRatio = ratio;
            setDividerPositions(ratio);
        }
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

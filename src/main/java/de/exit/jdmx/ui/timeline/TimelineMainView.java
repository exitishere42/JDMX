package de.exit.jdmx.ui.timeline;

import de.exit.jdmx.core.model.TimelineTrack;
import de.exit.jdmx.service.TimelineService;
import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.theme.StudioTheme;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Haupt-Ansicht der Show-Timeline (FL Studio Playlist-Konzept).
 * <p>
 * Beherbergt:
 * <ul>
 *     <li>Kompakte obere Toolbar mit Transport-Steuerung und Zeitanzeige</li>
 *     <li>Linke Track-Header-Leiste mit {@link TimelineTrackHeaderBox}en und dem [+]-Outline-Button</li>
 *     <li>Rechtes synchronisiertes {@link TimelineGridView}-Raster</li>
 * </ul>
 * Reagiert atomar auf Änderungen im {@link TimelineService}.
 * </p>
 */
public class TimelineMainView extends VBox {

    private final TimelineService service;
    private final List<TimelineTrackHeaderBox> trackBoxes = new ArrayList<>();

    private final HBox topToolbar;
    private final Button btnPlay;
    private final Button btnStop;
    private final Label lblTimeDisplay;

    private final VBox trackBoxesContainer;
    private final Button btnAddTrack;
    private final TimelineGridView timelineGridView;

    private final ScrollPane tracksScrollPane;
    private final ScrollPane gridScrollPane;

    /**
     * Erstellt die Hauptansicht der Show-Timeline.
     *
     * @param service der {@link TimelineService} (darf nicht {@code null} sein)
     */
    public TimelineMainView(TimelineService service) {
        super(0);
        this.service = Objects.requireNonNull(service, "service must not be null");
        getStyleClass().add("timeline-main-view");
        VBox.setVgrow(this, Priority.ALWAYS);

        // 1. Obere Toolbar
        this.btnPlay = new Button();
        btnPlay.getStyleClass().add("timeline-transport-btn");
        btnPlay.setGraphic(new LucideIcon("play", 13, StudioTheme.COLOR_ICON_GREEN));
        btnPlay.setTooltip(new Tooltip("Play"));

        this.btnStop = new Button();
        btnStop.getStyleClass().add("timeline-transport-btn");
        btnStop.setGraphic(new LucideIcon("square", 12, StudioTheme.COLOR_ICON_RED));
        btnStop.setTooltip(new Tooltip("Stop"));

        this.lblTimeDisplay = new Label("00:00.000");
        lblTimeDisplay.getStyleClass().add("timeline-time-display");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        this.topToolbar = new HBox(6, btnPlay, btnStop, lblTimeDisplay, spacer);
        topToolbar.setAlignment(Pos.CENTER_LEFT);
        topToolbar.getStyleClass().add("timeline-top-toolbar");

        // 2. Linke Track-Header-Leiste
        this.trackBoxesContainer = new VBox(0);
        trackBoxesContainer.getStyleClass().add("timeline-tracks-container");

        // Kleiner Button mit Outline unter den Trackboxen zum Hinzufügen einer neuen Spur
        this.btnAddTrack = new Button();
        btnAddTrack.getStyleClass().add("timeline-track-add-btn");
        btnAddTrack.setGraphic(new LucideIcon("plus", 11, StudioTheme.COLOR_ICON_TIMELINE));
        btnAddTrack.setTooltip(new Tooltip("Track hinzufügen"));
        btnAddTrack.setOnAction(e -> service.addTrack());

        VBox leftHeadersPane = new VBox(0, trackBoxesContainer, btnAddTrack);
        leftHeadersPane.getStyleClass().add("timeline-left-headers-pane");

        this.tracksScrollPane = new ScrollPane(leftHeadersPane);
        tracksScrollPane.getStyleClass().addAll("timeline-tracks-scroll", "edge-to-edge");
        tracksScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        tracksScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        tracksScrollPane.setFitToWidth(true);

        // 3. Rechtes Timeline-Grid
        this.timelineGridView = new TimelineGridView(service);
        HBox.setHgrow(timelineGridView, Priority.ALWAYS);
        VBox.setVgrow(timelineGridView, Priority.ALWAYS);

        this.gridScrollPane = new ScrollPane(timelineGridView);
        gridScrollPane.getStyleClass().addAll("timeline-grid-scroll", "edge-to-edge");
        gridScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        gridScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        gridScrollPane.setFitToWidth(true);
        gridScrollPane.setFitToHeight(true);
        HBox.setHgrow(gridScrollPane, Priority.ALWAYS);
        VBox.setVgrow(gridScrollPane, Priority.ALWAYS);

        // Vertikales Scrollen beider Bereiche synchronisieren
        tracksScrollPane.vvalueProperty().bindBidirectional(gridScrollPane.vvalueProperty());

        // 4. Zentraler Split/Content-Bereich
        HBox contentBox = new HBox(0, tracksScrollPane, gridScrollPane);
        VBox.setVgrow(contentBox, Priority.ALWAYS);
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        getChildren().addAll(topToolbar, contentBox);

        // Initialer Aufbau der Trackboxen
        rebuildTrackBoxes();

        // Auf Änderungen im Service lauschen
        service.addChangeListener(() -> {
            if (getScene() != null && !Platform.isFxApplicationThread()) {
                Platform.runLater(this::rebuildTrackBoxes);
            } else {
                rebuildTrackBoxes();
            }
        });
    }

    /**
     * Baut die Trackbox-Liste anhand der Tracks im {@link TimelineService} neu auf.
     */
    public void rebuildTrackBoxes() {
        trackBoxes.clear();
        trackBoxesContainer.getChildren().clear();

        for (TimelineTrack track : service.getTracks()) {
            TimelineTrackHeaderBox box = new TimelineTrackHeaderBox(track, service);
            trackBoxes.add(box);
            trackBoxesContainer.getChildren().add(box);
        }
    }

    /**
     * Liefert die Liste aller aktuellen Track-Header-Boxen.
     *
     * @return unmodifizierbare Liste der {@link TimelineTrackHeaderBox}en
     */
    public List<TimelineTrackHeaderBox> getTrackBoxes() {
        return Collections.unmodifiableList(trackBoxes);
    }

    /**
     * Liefert den [+]-Button zum Hinzufügen von Tracks.
     *
     * @return der Add-Button
     */
    public Button getBtnAddTrack() {
        return btnAddTrack;
    }

    /**
     * Liefert die Grid-Ansicht.
     *
     * @return das {@link TimelineGridView}
     */
    public TimelineGridView getTimelineGridView() {
        return timelineGridView;
    }

    /**
     * Liefert den Play-Button.
     *
     * @return der Play-Button
     */
    public Button getBtnPlay() {
        return btnPlay;
    }

    /**
     * Liefert den Stop-Button.
     *
     * @return der Stop-Button
     */
    public Button getBtnStop() {
        return btnStop;
    }

    /**
     * Liefert die Zeitanzeige.
     *
     * @return das Label der Zeitanzeige
     */
    public Label getLblTimeDisplay() {
        return lblTimeDisplay;
    }

    /**
     * Liefert den TimelineService.
     *
     * @return der {@link TimelineService}
     */
    public TimelineService getService() {
        return service;
    }
}

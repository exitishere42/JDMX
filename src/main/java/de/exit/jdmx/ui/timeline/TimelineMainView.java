package de.exit.jdmx.ui.timeline;

import de.exit.jdmx.core.model.TimelineTrack;
import de.exit.jdmx.service.TimelineService;
import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.theme.StudioTheme;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
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

    /** Feste Breite der linken Track-Header-Leiste in Pixeln (verhindert Zusammenstauchen). */
    public static final double HEADER_PANE_WIDTH = 160.0;

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

    private Boolean lastPlayVisualState = null;
    private String lastTrackSignature = "";

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
        btnPlay.setOnAction(e -> {
            if (service.isPlaying()) {
                service.pausePlayback();
            } else {
                service.startPlayback();
            }
        });

        this.btnStop = new Button();
        btnStop.getStyleClass().add("timeline-transport-btn");
        btnStop.setGraphic(new LucideIcon("square", 12, StudioTheme.COLOR_ICON_RED));
        btnStop.setTooltip(new Tooltip("Stop"));
        btnStop.setOnAction(e -> service.stopPlayback());
        btnStop.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                service.stopPlayback();
                if (e.getClickCount() >= 2) {
                    resetViewToBeginning();
                }
            }
        });

        this.lblTimeDisplay = new Label("00:00.000");
        lblTimeDisplay.getStyleClass().add("timeline-time-display");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 2. Rechtes Timeline-Grid (vor Toolbar-Initialisierung für Zoom-Actions)
        this.timelineGridView = new TimelineGridView(service);
        HBox.setHgrow(timelineGridView, Priority.ALWAYS);
        VBox.setVgrow(timelineGridView, Priority.ALWAYS);

        // Zoom-Steuerung für die horizontale Zeitleiste
        Button btnZoomOut = new Button();
        btnZoomOut.getStyleClass().add("timeline-transport-btn");
        btnZoomOut.setGraphic(new LucideIcon("zoom-out", 12, StudioTheme.COLOR_ICON_TIMELINE));
        btnZoomOut.setTooltip(new Tooltip("Verkleinern (Zoom Out)"));
        btnZoomOut.setOnAction(e -> timelineGridView.zoomOut());

        Button btnZoomReset = new Button("100%");
        btnZoomReset.getStyleClass().addAll("timeline-transport-btn", "timeline-zoom-reset-btn");
        btnZoomReset.setTooltip(new Tooltip("Standard-Zoom (100%)"));
        btnZoomReset.setOnAction(e -> timelineGridView.resetZoom());

        Button btnZoomIn = new Button();
        btnZoomIn.getStyleClass().add("timeline-transport-btn");
        btnZoomIn.setGraphic(new LucideIcon("zoom-in", 12, StudioTheme.COLOR_ICON_TIMELINE));
        btnZoomIn.setTooltip(new Tooltip("Vergrößern (Zoom In)"));
        btnZoomIn.setOnAction(e -> timelineGridView.zoomIn());

        HBox zoomBox = new HBox(4, btnZoomOut, btnZoomReset, btnZoomIn);
        zoomBox.setAlignment(Pos.CENTER_RIGHT);

        this.topToolbar = new HBox(6, btnPlay, btnStop, lblTimeDisplay, spacer, zoomBox);
        topToolbar.setAlignment(Pos.CENTER_LEFT);
        topToolbar.getStyleClass().add("timeline-top-toolbar");

        // 3. Linke Track-Header-Leiste
        this.trackBoxesContainer = new VBox(0);
        trackBoxesContainer.getStyleClass().add("timeline-tracks-container");

        // Kleiner Button mit Outline unter den Trackboxen zum Hinzufügen einer neuen Spur
        this.btnAddTrack = new Button();
        btnAddTrack.getStyleClass().add("timeline-track-add-btn");
        btnAddTrack.setGraphic(new LucideIcon("plus", 11, StudioTheme.COLOR_ICON_TIMELINE));
        btnAddTrack.setTooltip(new Tooltip("Track hinzufügen"));
        btnAddTrack.setMaxWidth(Double.MAX_VALUE);
        btnAddTrack.setOnAction(e -> service.addTrack());

        VBox leftHeadersPane = new VBox(0, trackBoxesContainer, btnAddTrack);
        leftHeadersPane.getStyleClass().add("timeline-left-headers-pane");
        leftHeadersPane.setMinWidth(HEADER_PANE_WIDTH);
        leftHeadersPane.setPrefWidth(HEADER_PANE_WIDTH);
        leftHeadersPane.setMaxWidth(HEADER_PANE_WIDTH);

        this.tracksScrollPane = new ScrollPane(leftHeadersPane);
        tracksScrollPane.getStyleClass().addAll("timeline-tracks-scroll", "edge-to-edge");
        tracksScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        tracksScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        tracksScrollPane.setFitToWidth(true);
        tracksScrollPane.setMinWidth(HEADER_PANE_WIDTH);
        tracksScrollPane.setPrefWidth(HEADER_PANE_WIDTH);
        tracksScrollPane.setMaxWidth(HEADER_PANE_WIDTH);

        // 4. Rechtes Timeline-Grid ScrollPane
        this.gridScrollPane = new ScrollPane(timelineGridView);
        gridScrollPane.getStyleClass().addAll("timeline-grid-scroll", "edge-to-edge");
        gridScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        gridScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        gridScrollPane.setFitToWidth(false);
        gridScrollPane.setFitToHeight(false);
        HBox.setHgrow(gridScrollPane, Priority.ALWAYS);
        VBox.setVgrow(gridScrollPane, Priority.ALWAYS);

        // Hardware-Texture-Sicherheit: Viewport-Anbindung an das Grid
        timelineGridView.attachScrollPane(gridScrollPane);

        // Vertikales Scrollen beider Bereiche synchronisieren
        tracksScrollPane.vvalueProperty().bindBidirectional(gridScrollPane.vvalueProperty());

        // 4. Zentraler Split/Content-Bereich
        HBox contentBox = new HBox(0, tracksScrollPane, gridScrollPane);
        VBox.setVgrow(contentBox, Priority.ALWAYS);
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        getChildren().addAll(topToolbar, contentBox);

        // Initialer Aufbau der Trackboxen und Playback-Zustand
        updateFromService();

        // 5. Leertaste (Spacebar) zum Starten / Pausieren der Wiedergabe
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.SPACE) {
                Node focusOwner = getScene() != null ? getScene().getFocusOwner() : null;
                if (!(focusOwner instanceof TextInputControl)) {
                    if (service.isPlaying()) {
                        service.pausePlayback();
                    } else {
                        service.startPlayback();
                    }
                    event.consume();
                }
            }
        });

        // Auf Änderungen im Service lauschen
        service.addChangeListener(() -> {
            if (Platform.isFxApplicationThread() || getScene() == null) {
                updateFromService();
            } else {
                Platform.runLater(this::updateFromService);
            }
        });
    }

    /**
     * Setzt die Scroll-Ansicht der Timeline vollständig an den Anfang (0, 0) zurück.
     */
    public void resetViewToBeginning() {
        if (gridScrollPane != null) {
            gridScrollPane.setHvalue(0.0);
            gridScrollPane.setVvalue(0.0);
        }
        if (tracksScrollPane != null) {
            tracksScrollPane.setVvalue(0.0);
        }
        if (timelineGridView != null) {
            timelineGridView.redraw();
        }
    }

    /**
     * Formatiert eine Zeitspanne in Millisekunden in die digitale Zeitanzeige {@code mm:ss.SSS}.
     *
     * @param totalMs Zeit in Millisekunden
     * @return formatierter String im Format {@code mm:ss.SSS}
     */
    public static String formatTime(long totalMs) {
        long safeMs = Math.max(0L, totalMs);
        long minutes = (safeMs / 1000L) / 60L;
        long seconds = (safeMs / 1000L) % 60L;
        long millis = safeMs % 1000L;
        return String.format("%02d:%02d.%03d", minutes, seconds, millis);
    }

    /**
     * Aktualisiert die Trackboxen, die Zeitanzeige und den Play-Button-Status.
     * Nutzt Caching, um unnötige Layout-Invalidierungen und Cursor-Flackern während des Playbacks zu verhindern.
     */
    public void updateFromService() {
        String currentSig = computeTrackSignature();
        if (!currentSig.equals(lastTrackSignature)) {
            lastTrackSignature = currentSig;
            rebuildTrackBoxes();
        }

        long curPos = service.getCurrentPositionMs();
        long maxDuration = service.getMaxTimelineDurationMs();
        boolean hasSong = service.getTracks().stream().anyMatch(TimelineTrack::hasSong);
        String formatted = (hasSong || maxDuration > 60_000L)
                ? formatTime(curPos) + " / " + formatTime(maxDuration)
                : formatTime(curPos);

        if (!formatted.equals(lblTimeDisplay.getText())) {
            lblTimeDisplay.setText(formatted);
        }

        updatePlayButtonVisual();
    }

    /**
     * Berechnet eine Signatur der aktuellen Spurenkonfiguration zur Vermeidung redundanter Rebuilds.
     *
     * @return Track-Signatur-String
     */
    private String computeTrackSignature() {
        StringBuilder sb = new StringBuilder();
        for (TimelineTrack t : service.getTracks()) {
            sb.append(t.getId()).append(':')
              .append(t.getName()).append(':')
              .append(t.getSongName()).append(':')
              .append(t.getStartTimeMs()).append(':')
              .append(t.isMuted()).append(';');
        }
        return sb.toString();
    }

    /**
     * Aktualisiert das Icon und Styling des Play-Buttons nur bei echtem Statuswechsel.
     * Verhindert das 40-FPS-Austauschen von SVG-Nodes und damit das Zittern des Mauszeigers.
     */
    public void updatePlayButtonVisual() {
        boolean playing = service.isPlaying();
        if (lastPlayVisualState != null && lastPlayVisualState == playing) {
            return;
        }
        lastPlayVisualState = playing;

        if (playing) {
            btnPlay.setGraphic(new LucideIcon("pause", 13, StudioTheme.COLOR_ICON_TIMELINE));
            if (!btnPlay.getStyleClass().contains("active")) {
                btnPlay.getStyleClass().add("active");
            }
        } else {
            btnPlay.setGraphic(new LucideIcon("play", 13, StudioTheme.COLOR_ICON_GREEN));
            btnPlay.getStyleClass().remove("active");
        }
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
     * Liefert die obere Toolbar.
     *
     * @return die {@link HBox} der oberen Toolbar
     */
    public HBox getTopToolbar() {
        return topToolbar;
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
     * Liefert die ScrollPane der linken Track-Header-Leiste.
     *
     * @return die {@link ScrollPane} für Tracks
     */
    public ScrollPane getTracksScrollPane() {
        return tracksScrollPane;
    }

    /**
     * Liefert die ScrollPane des rechten Timeline-Grid.
     *
     * @return die {@link ScrollPane} für das Grid
     */
    public ScrollPane getGridScrollPane() {
        return gridScrollPane;
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

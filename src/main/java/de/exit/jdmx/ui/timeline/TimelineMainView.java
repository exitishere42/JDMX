package de.exit.jdmx.ui.timeline;

import de.exit.jdmx.core.model.TimelineTrack;
import de.exit.jdmx.service.TimelineService;
import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.theme.StudioTheme;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
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
    private final ToggleButton btnFollowPlayhead;
    private final javafx.animation.AnimationTimer vsyncPlaybackTimer;

    private final VBox trackBoxesContainer;
    private final Button btnAddTrack;
    private final TimelineGridView timelineGridView;

    private final ScrollPane tracksScrollPane;
    private final BorderPane gridViewportContainer;
    private final ScrollBar hScrollBar;
    private final ScrollBar vScrollBar;
    private boolean updatingScrollBars = false;

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
        lblTimeDisplay.setMinSize(155.0, 24.0);
        lblTimeDisplay.setPrefSize(155.0, 24.0);
        lblTimeDisplay.setMaxSize(155.0, 24.0);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 2. Rechtes Timeline-Grid (vor Toolbar-Initialisierung für Zoom-Actions)
        this.timelineGridView = new TimelineGridView(service);
        HBox.setHgrow(timelineGridView, Priority.ALWAYS);
        VBox.setVgrow(timelineGridView, Priority.ALWAYS);

        // Follow-Playhead Toggle (automatisches Verfolgen des Timeline-Strichs, standardmäßig aktiv)
        this.btnFollowPlayhead = new ToggleButton();
        btnFollowPlayhead.getStyleClass().addAll("timeline-transport-btn", "timeline-follow-btn");
        btnFollowPlayhead.setSelected(true);
        btnFollowPlayhead.setTooltip(new Tooltip("Timeline verfolgen"));
        updateFollowButtonIcon();
        btnFollowPlayhead.selectedProperty().addListener((obs, oldVal, newVal) -> {
            updateFollowButtonIcon();
            if (Boolean.TRUE.equals(newVal) && service.isPlaying()) {
                scrollToFollowPlayhead(service.getCurrentPositionMs());
            }
        });

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

        this.topToolbar = new HBox(6, btnPlay, btnStop, lblTimeDisplay, spacer, btnFollowPlayhead, zoomBox);
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

        // 4. Rechtes entkoppeltes GPU-Viewport-Container (ohne ScrollPane-Layout-Thrashing)
        this.hScrollBar = new ScrollBar();
        hScrollBar.setOrientation(Orientation.HORIZONTAL);
        hScrollBar.getStyleClass().add("timeline-hbar");

        this.vScrollBar = new ScrollBar();
        vScrollBar.setOrientation(Orientation.VERTICAL);
        vScrollBar.getStyleClass().add("timeline-vbar");

        this.gridViewportContainer = new BorderPane();
        gridViewportContainer.getStyleClass().add("timeline-grid-scroll");
        gridViewportContainer.setCenter(timelineGridView);
        gridViewportContainer.setBottom(hScrollBar);
        gridViewportContainer.setRight(vScrollBar);
        HBox.setHgrow(gridViewportContainer, Priority.ALWAYS);
        VBox.setVgrow(gridViewportContainer, Priority.ALWAYS);

        // ScrollBar-Synchronisation ohne Ping-Pong und ohne Layout-Invalidierung
        hScrollBar.valueProperty().addListener((obs, oldV, newV) -> {
            if (!updatingScrollBars) {
                timelineGridView.setScrollX(newV.doubleValue());
            }
        });
        vScrollBar.valueProperty().addListener((obs, oldV, newV) -> {
            if (!updatingScrollBars) {
                double sy = newV.doubleValue();
                timelineGridView.setScrollY(sy);
                double maxY = timelineGridView.getMaxScrollY();
                tracksScrollPane.setVvalue(maxY > 0.0 ? (sy / maxY) : 0.0);
            }
        });
        tracksScrollPane.vvalueProperty().addListener((obs, oldV, newV) -> {
            if (!updatingScrollBars) {
                double maxY = timelineGridView.getMaxScrollY();
                double sy = maxY * Math.max(0.0, Math.min(1.0, newV.doubleValue()));
                timelineGridView.setScrollY(sy);
                syncScrollBarRanges();
            }
        });

        timelineGridView.scrollXProperty().addListener((obs, oldV, newV) -> syncScrollBarRanges());
        timelineGridView.scrollYProperty().addListener((obs, oldV, newV) -> syncScrollBarRanges());
        timelineGridView.contentWidthProperty().addListener((obs, oldV, newV) -> syncScrollBarRanges());
        timelineGridView.contentHeightProperty().addListener((obs, oldV, newV) -> syncScrollBarRanges());
        timelineGridView.widthProperty().addListener((obs, oldV, newV) -> syncScrollBarRanges());
        timelineGridView.heightProperty().addListener((obs, oldV, newV) -> syncScrollBarRanges());
        syncScrollBarRanges();

        // 4. Zentraler Split/Content-Bereich
        HBox contentBox = new HBox(0, tracksScrollPane, gridViewportContainer);
        VBox.setVgrow(contentBox, Priority.ALWAYS);
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        getChildren().addAll(topToolbar, contentBox);

        timelineGridView.setVsyncControlled(true);
        this.vsyncPlaybackTimer = new javafx.animation.AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!service.isPlaying()) {
                    stop();
                    return;
                }
                long smoothedPos = service.getSmoothedPlaybackPositionMs(now);
                updatePlaybackFrame(smoothedPos);
            }
        };

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
        if (timelineGridView != null) {
            timelineGridView.setScrollX(0.0);
            timelineGridView.setScrollY(0.0);
            timelineGridView.redraw();
        }
        if (tracksScrollPane != null) {
            tracksScrollPane.setVvalue(0.0);
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

        updatePlayButtonVisual();

        if (service.isPlaying()) {
            if (getScene() != null) {
                vsyncPlaybackTimer.start();
            } else {
                updatePlaybackFrame(service.getCurrentPositionMs());
            }
        } else {
            vsyncPlaybackTimer.stop();
            timelineGridView.redrawAtPosition(-1L);
            updateTimeLabel(service.getCurrentPositionMs());
        }
    }

    /**
     * Aktualisiert Zeitanzeige, Subpixel-Follow-Scrolling und GPU-Layer-Rendering in einem einzigen
     * VSync-Durchlauf ohne doppelte Redraws oder Layout-Invalidierung.
     *
     * @param curPosMs aktuelle (ggf. Soft-PLL geglättete) Position in Millisekunden
     */
    public void updatePlaybackFrame(long curPosMs) {
        updateTimeLabel(curPosMs);

        double targetScrollX = -1.0;
        if (service.isPlaying() && isFollowPlayheadEnabled()) {
            targetScrollX = computeFollowScrollX(curPosMs);
        }
        timelineGridView.updatePlaybackView(curPosMs, targetScrollX);
    }

    private void updateTimeLabel(long curPos) {
        long maxDuration = service.getMaxTimelineDurationMs();
        boolean hasSong = service.getTracks().stream().anyMatch(TimelineTrack::hasSong);
        String formatted = (hasSong || maxDuration > 60_000L)
                ? formatTime(curPos) + " / " + formatTime(maxDuration)
                : formatTime(curPos);

        if (!formatted.equals(lblTimeDisplay.getText())) {
            lblTimeDisplay.setText(formatted);
        }
    }

    private void updateFollowButtonIcon() {
        javafx.scene.paint.Color iconColor = btnFollowPlayhead.isSelected()
                ? javafx.scene.paint.Color.web("#FB923C")
                : javafx.scene.paint.Color.web("#9CA3AF");
        btnFollowPlayhead.setGraphic(new LucideIcon("crosshair", 12, iconColor));
    }

    private void scrollToFollowPlayhead(long curPosMs) {
        double targetScrollX = computeFollowScrollX(curPosMs);
        if (targetScrollX >= 0.0) {
            timelineGridView.setScrollX(targetScrollX);
        }
    }

    private double computeFollowScrollX(long curPosMs) {
        if (timelineGridView == null) {
            return -1.0;
        }
        double viewW = timelineGridView.getViewportWidth();
        double maxScrollX = timelineGridView.getMaxScrollX();
        if (maxScrollX <= 0.0 || viewW <= 0.0) {
            return -1.0;
        }
        double playheadX = timelineGridView.millisToX(curPosMs);
        double currentScrollX = timelineGridView.getScrollX();
        double playheadScreenX = playheadX - currentScrollX;
        double rightThreshold = viewW;

        // Solange der Playhead zwischen linkem Rand und Viewport-Ende läuft: Viewport komplett ruhig halten
        if (playheadScreenX >= 0.0 && playheadScreenX < rightThreshold) {
            return -1.0;
        }

        // Am Ende angelangt (oder bei Seek/Start außerhalb): einmalig umblättern (Playhead bei 10% links)
        double targetScrollX = Math.max(0.0, Math.min(maxScrollX, playheadX - (viewW * 0.10)));
        if (Math.abs(currentScrollX - targetScrollX) > 0.5) {
            return targetScrollX;
        }
        return -1.0;
    }

    private void syncScrollBarRanges() {
        if (updatingScrollBars || timelineGridView == null) {
            return;
        }
        updatingScrollBars = true;
        try {
            double maxX = timelineGridView.getMaxScrollX();
            double viewW = timelineGridView.getViewportWidth();
            double safeMaxX = Math.max(0.0, maxX);
            double visW = maxX > 0.0 ? Math.max(20.0, viewW) : 0.0;
            if (Math.abs(hScrollBar.getMax() - safeMaxX) > 0.5) {
                hScrollBar.setMin(0.0);
                hScrollBar.setMax(safeMaxX);
                hScrollBar.setVisibleAmount(visW);
                boolean showH = maxX > 0.0;
                hScrollBar.setVisible(showH);
                hScrollBar.setManaged(showH);
            }
            double targetHVal = Math.max(0.0, Math.min(safeMaxX, timelineGridView.getScrollX()));
            if (Math.abs(hScrollBar.getValue() - targetHVal) >= 0.01) {
                hScrollBar.setValue(targetHVal);
            }

            double maxY = timelineGridView.getMaxScrollY();
            double viewH = timelineGridView.getHeight() > 0.0 ? timelineGridView.getHeight() : 600.0;
            double safeMaxY = Math.max(0.0, maxY);
            double visH = maxY > 0.0 ? Math.max(20.0, viewH) : 0.0;
            if (Math.abs(vScrollBar.getMax() - safeMaxY) > 0.5) {
                vScrollBar.setMin(0.0);
                vScrollBar.setMax(safeMaxY);
                vScrollBar.setVisibleAmount(visH);
                boolean showV = maxY > 0.0;
                vScrollBar.setVisible(showV);
                vScrollBar.setManaged(showV);
            }
            double targetVVal = Math.max(0.0, Math.min(safeMaxY, timelineGridView.getScrollY()));
            if (Math.abs(vScrollBar.getValue() - targetVVal) >= 0.01) {
                vScrollBar.setValue(targetVVal);
            }
        } finally {
            updatingScrollBars = false;
        }
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
     * Liefert den Follow-Playhead Toggle-Button.
     *
     * @return der {@link ToggleButton} zum Verfolgen des Timeline-Strichs
     */
    public ToggleButton getBtnFollowPlayhead() {
        return btnFollowPlayhead;
    }

    /**
     * Prüft, ob das automatische Verfolgen des Playheads aktiviert ist.
     *
     * @return {@code true}, wenn die Ansicht beim Abspielen mitscrollt
     */
    public boolean isFollowPlayheadEnabled() {
        return btnFollowPlayhead != null && btnFollowPlayhead.isSelected();
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
     * Liefert die horizontale ScrollBar des Timeline-Viewports.
     *
     * @return die horizontale {@link ScrollBar}
     */
    public ScrollBar getHorizontalScrollBar() {
        return hScrollBar;
    }

    /**
     * Liefert die vertikale ScrollBar des Timeline-Viewports.
     *
     * @return die vertikale {@link ScrollBar}
     */
    public ScrollBar getVerticalScrollBar() {
        return vScrollBar;
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

package de.exit.jdmx.ui.timeline;

import de.exit.jdmx.core.log.AppLogger;
import de.exit.jdmx.core.model.TimelineTrack;
import de.exit.jdmx.service.TimelineService;
import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Grid-Canvas für die Show-Timeline Playlist (FL Studio Raster-Stil).
 * <p>
 * Zeichnet:
 * <ul>
 *     <li>Horizontale Spur-Trennlinien exakt passend zur Höhe der linken Track-Boxen</li>
 *     <li>Vertikale Taktraster-Linien (Bars & Beats)</li>
 *     <li>Song-Clips und Audio-Blöcke auf Spuren mit zugewiesenem Song und echter Wellenform</li>
 *     <li>Die dynamische <b>Timeline-Linie (Playhead)</b> mit Abspielanzeige</li>
 * </ul>
 * Ermöglicht Scrubbing per Mausklick sowie Drag & Drop von Songs aus dem Explorer direkt auf Spuren.
 * </p>
 * <p>
 * <b>Hardware-Texture-Sicherheit:</b>
 * Die interne {@link Canvas} wird strikt auf den sichtbaren Viewport-Bereich (z. B. 1920x800)
 * begrenzt und virtuell mit dem Scrollbalken mitgeführt. Dadurch wird das Überschreiten der
 * maximalen OpenGL/GPU-Texturgröße (16384px auf HiDPI-Displays) zuverlässig verhindert.
 * </p>
 */
public class TimelineGridView extends Pane {

    /** Raster-Breite pro Takt (Bar) in Pixeln. */
    public static final double BAR_WIDTH = 96.0;

    /** Raster-Unterteilung pro Viertel-Beat in Pixeln. */
    public static final double BEAT_WIDTH = BAR_WIDTH / 4.0;

    /** Standard-Zeitmaßstab: Anzahl Pixel pro Sekunde bei 100% Zoom (60px = 1 Sekunde). */
    public static final double DEFAULT_PIXELS_PER_SECOND = 60.0;

    /** Abwärtskompatible Konstante für statische Zugriffe (entspricht 100% Standard-Zoom). */
    public static final double PIXELS_PER_SECOND = DEFAULT_PIXELS_PER_SECOND;

    /** Minimaler Zoom: 24 Pixel pro Sekunde (Überblicks-Zoom für lange Shows). */
    public static final double MIN_PIXELS_PER_SECOND = 24.0;

    /** Maximaler Zoom: 160 Pixel pro Sekunde (Detail-Zoom für präzises Beat-Matching). */
    public static final double MAX_PIXELS_PER_SECOND = 160.0;

    /** Zoom-Faktor pro Stufe (+/- 20%). */
    public static final double ZOOM_FACTOR = 1.2;

    /** Original Pioneer Rekordbox Farbton für Tiefen/Bass (< 250 Hz, Royal Blue). */
    public static final Color COLOR_REKORDBOX_LOW = Color.web("#2253D9");

    /** Original Pioneer Rekordbox Farbton für Mitten (250 Hz - 3 kHz, Amber Gold / Warmes Orange). */
    public static final Color COLOR_REKORDBOX_MID = Color.web("#D97706");

    /** Original Pioneer Rekordbox Farbton für Höhen/Transienten (> 3 kHz, Strahlendes Pure White). */
    public static final Color COLOR_REKORDBOX_HIGH = Color.web("#FFFFFF");

    /** Maximale sichere Canvas-Breite für GPU-Texturen (verhindert HiDPI 16384px Überlauf). */
    public static final double MAX_CANVAS_SAFE_DIM = 4096.0;

    /** Breite einer gecachten Offscreen-Waveform-Kachel in Pixeln (optimal für GPU-Blitting). */
    private static final int WAVEFORM_TILE_WIDTH = 1024;

    private final TimelineService service;
    private final Canvas canvas;
    private final Map<String, Image> waveformTileCache = new HashMap<>();
    private ScrollPane attachedScrollPane;

    /** Dynamischer Zeitmaßstab: Aktuelle Pixel pro Sekunde (horizontaler Zoom). */
    private double pixelsPerSecond = DEFAULT_PIXELS_PER_SECOND;

    /** Optionale geglättete Playhead-Position (ms) für VSync-Rendering ohne 20ms-Stufen. */
    private long activeRenderPositionMs = -1L;

    /** Ob das Frame-Rendering während Playback vom VSync-AnimationTimer gesteuert wird. */
    private boolean vsyncControlled = false;

    private boolean isDraggingPlayhead = false;
    private boolean wasPlayingBeforePlayheadDrag = false;
    private TimelineTrack draggingClipTrack = null;
    private double dragStartMouseX = 0;
    private long dragStartClipTimeMs = 0;

    /**
     * Erstellt die Grid-Ansicht für die gegebene TimelineService-Instanz.
     *
     * @param service der {@link TimelineService} (darf nicht {@code null} sein)
     */
    public TimelineGridView(TimelineService service) {
        this.service = Objects.requireNonNull(service, "service must not be null");
        getStyleClass().add("timeline-grid-view");

        this.canvas = new Canvas(1200, 600);
        this.canvas.setManaged(false);
        this.canvas.setMouseTransparent(true);
        getChildren().add(canvas);

        updateGridDimensions();

        service.addChangeListener(() -> {
            Runnable action = () -> {
                clearWaveformCache();
                boolean sizeChanged = updateGridDimensions();
                if (sizeChanged) {
                    requestLayout();
                }
                redraw();
            };
            if (Platform.isFxApplicationThread() || getScene() == null) {
                action.run();
            } else {
                Platform.runLater(action);
            }
        });

        service.addPlaybackFrameListener(() -> {
            if (vsyncControlled && service.isPlaying() && getScene() != null) {
                return; // VSync AnimationTimer in TimelineMainView steuert die Frames exklusiv (60/120 FPS)
            }
            // Reine Playback-Ticks ohne Layout-Invalidierung und ohne Cache-Löschung!
            if (Platform.isFxApplicationThread() || getScene() == null) {
                redraw();
            } else {
                Platform.runLater(this::redraw);
            }
        });

        // Interaktionssteuerung: Playhead-Dreieck (Scrubbing) ODER Lieder verschieben (Clip-Drag)
        setOnMousePressed(e -> {
            if (!e.isPrimaryButtonDown()) return;

            double playheadContentX = millisToX(service.getCurrentPositionMs());
            // Prüfen ob Klick auf das obere Playhead-Dreieck erfolgte (y <= 24px und im Radius von 16px um den Playhead)
            if (e.getY() <= 24.0 && Math.abs(e.getX() - playheadContentX) <= 16.0) {
                isDraggingPlayhead = true;
                wasPlayingBeforePlayheadDrag = service.isPlaying();
                if (wasPlayingBeforePlayheadDrag) {
                    service.pausePlayback();
                }
                handleSeek(e.getX());
                return;
            }

            // Prüfen ob Klick auf einen Song-Clip erfolgte
            TimelineTrack hitTrack = findClipTrackAt(e.getX(), e.getY());
            if (hitTrack != null) {
                if (service.isPlaying()) {
                    // Während aktiver Show-Wiedergabe ist das Verschieben von Tracks gesperrt!
                    draggingClipTrack = null;
                    return;
                }
                draggingClipTrack = hitTrack;
                dragStartMouseX = e.getX();
                dragStartClipTimeMs = hitTrack.getStartTimeMs();
                setCursor(Cursor.MOVE);
                return;
            }

            // Klick auf freie Fläche: Keine unabsichtliche Verschiebung
            isDraggingPlayhead = false;
            draggingClipTrack = null;
        });

        setOnMouseDragged(e -> {
            if (!e.isPrimaryButtonDown()) return;

            if (isDraggingPlayhead) {
                handleSeek(e.getX());
            } else if (draggingClipTrack != null) {
                if (service.isPlaying()) {
                    draggingClipTrack = null;
                    return;
                }
                double deltaPx = e.getX() - dragStartMouseX;
                long deltaMs = Math.round((deltaPx / pixelsPerSecond) * 1000.0);
                long newStartMs = Math.max(0L, dragStartClipTimeMs + deltaMs);
                service.setTrackStartTimeMs(draggingClipTrack.getId(), newStartMs);
                updateGridDimensions();
                redraw();
            }
        });

        setOnMouseReleased(e -> {
            if (isDraggingPlayhead) {
                if (wasPlayingBeforePlayheadDrag) {
                    service.startPlayback();
                    wasPlayingBeforePlayheadDrag = false;
                }
            }
            if (draggingClipTrack != null) {
                service.resyncTrackImmediate(draggingClipTrack.getId());
            }
            isDraggingPlayhead = false;
            draggingClipTrack = null;
            updateCursorForPosition(e.getX(), e.getY());
        });

        setOnMouseMoved(e -> updateCursorForPosition(e.getX(), e.getY()));

        // Ctrl + Mausrad Zoom (Standard-DAW-Verhalten: Maus-zentriert)
        addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, e -> handleCtrlScroll(e, false));

        // Kontextmenü & Drag & Drop für Audio-Songs aus dem Explorer auf Spuren
        setupContextMenu();
        setupDragAndDrop();
    }

    /**
     * Verknüpft das umschließende {@link ScrollPane}, um die Canvas auf den sichtbaren Viewport
     * zu begrenzen und GPU-Textur-Überläufe auf HiDPI-Displays zu verhindern.
     *
     * @param scrollPane das umschließende ScrollPane
     */
    public void attachScrollPane(ScrollPane scrollPane) {
        this.attachedScrollPane = scrollPane;
        if (scrollPane != null) {
            scrollPane.viewportBoundsProperty().addListener((obs, oldVal, newVal) -> redraw());
            scrollPane.hvalueProperty().addListener((obs, oldVal, newVal) -> redraw());
            scrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> redraw());

            // Auch auf ScrollPane-Ebene Ctrl + Mausrad Zoom abfangen (inkl. Linux/Trackpad Support)
            scrollPane.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, e -> handleCtrlScroll(e, true));
        }
        redraw();
    }

    private double resolveViewportWidth() {
        if (attachedScrollPane == null) {
            return Math.max(100.0, getWidth() > 0.0 ? getWidth() : 800.0);
        }
        double vpW = attachedScrollPane.getViewportBounds() != null ? attachedScrollPane.getViewportBounds().getWidth() : 0.0;
        if (vpW > 0.0) {
            return vpW;
        }
        return attachedScrollPane.getWidth() > 0.0 ? attachedScrollPane.getWidth() : 800.0;
    }

    private void handleCtrlScroll(javafx.scene.input.ScrollEvent e, boolean viewportRelative) {
        double deltaY = Math.abs(e.getDeltaY()) > 0.001 ? e.getDeltaY() : e.getTextDeltaY();
        if (e.isControlDown()) {
            if (Math.abs(deltaY) > 0.001) {
                double factor = deltaY > 0 ? ZOOM_FACTOR : (1.0 / ZOOM_FACTOR);
                double screenX = e.getX();
                if (!viewportRelative && attachedScrollPane != null) {
                    double viewW = resolveViewportWidth();
                    double totalW = Math.max(1200.0, getPrefWidth());
                    double maxScrollX = Math.max(0.0, totalW - viewW);
                    double scrollX = maxScrollX * Math.max(0.0, Math.min(1.0, attachedScrollPane.getHvalue()));
                    screenX = e.getX() - scrollX;
                }
                zoomAtScreenX(factor, screenX);
                e.consume();
            }
            return;
        }

        // Glattes, pixel-genaues horizontales Scrollen (verhindert grobe JavaFX ScrollPane-Sprünge bei langen Shows)
        if (attachedScrollPane != null) {
            double totalH = Math.max(600.0, getPrefHeight());
            double vpH = attachedScrollPane.getViewportBounds() != null ? attachedScrollPane.getViewportBounds().getHeight() : attachedScrollPane.getHeight();
            boolean verticalFits = vpH > 0.0 && totalH <= vpH + 2.0;
            if (e.isShiftDown() || Math.abs(e.getDeltaX()) > 0.001 || (verticalFits && Math.abs(deltaY) > 0.001)) {
                double rawDelta = Math.abs(e.getDeltaX()) > 0.001 ? e.getDeltaX() : deltaY;
                if (Math.abs(rawDelta) > 0.001) {
                    double viewW = resolveViewportWidth();
                    double totalW = Math.max(1200.0, getPrefWidth());
                    double maxScrollX = Math.max(0.0, totalW - viewW);
                    if (maxScrollX > 0.0) {
                        double currentScrollX = attachedScrollPane.getHvalue() * maxScrollX;
                        double nextScrollX = Math.max(0.0, Math.min(maxScrollX, currentScrollX - (rawDelta * 1.2)));
                        attachedScrollPane.setHvalue(nextScrollX / maxScrollX);
                    }
                    e.consume();
                }
            }
        }
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        redraw();
    }

    /**
     * Berechnet die Viewport-Koordinaten und aktualisiert die Canvas-Position und -Zeichnung.
     */
    public void redraw() {
        redrawAtPosition(-1L);
    }

    /**
     * Zeichnet das Grid inklusive des Playheads an der übergebenen VSync-interpolierten Position (in ms).
     *
     * @param smoothedPositionMs geglättete Playhead-Position oder {@code -1L} für {@link TimelineService#getCurrentPositionMs()}
     */
    public void redrawAtPosition(long smoothedPositionMs) {
        this.activeRenderPositionMs = smoothedPositionMs;
        double totalW = Math.max(1200.0, getPrefWidth());
        double totalH = Math.max(600.0, getPrefHeight());

        double viewW;
        double viewH;
        double scrollX = 0;
        double scrollY = 0;

        if (attachedScrollPane != null && attachedScrollPane.getViewportBounds() != null) {
            double vpW = attachedScrollPane.getViewportBounds().getWidth();
            double vpH = attachedScrollPane.getViewportBounds().getHeight();
            viewW = Math.min(MAX_CANVAS_SAFE_DIM, Math.max(100.0, vpW > 0 ? vpW : resolveViewportWidth()));
            viewH = Math.min(MAX_CANVAS_SAFE_DIM, Math.max(100.0, vpH > 0 ? vpH : (attachedScrollPane.getHeight() > 0 ? attachedScrollPane.getHeight() : 600.0)));
            double maxScrollX = Math.max(0.0, totalW - viewW);
            double maxScrollY = Math.max(0.0, totalH - viewH);
            scrollX = maxScrollX * Math.max(0.0, Math.min(1.0, attachedScrollPane.getHvalue()));
            scrollY = maxScrollY * Math.max(0.0, Math.min(1.0, attachedScrollPane.getVvalue()));
        } else {
            viewW = Math.min(MAX_CANVAS_SAFE_DIM, Math.max(100.0, getWidth() > 0 ? getWidth() : totalW));
            viewH = Math.min(MAX_CANVAS_SAFE_DIM, Math.max(100.0, getHeight() > 0 ? getHeight() : totalH));
        }

        // Canvas exakt synchron (subpixel-genau) via GPU-Translate positionieren (triggert 0 Layout-Passes!)
        if (Math.abs(canvas.getTranslateX() - scrollX) >= 0.001) {
            canvas.setTranslateX(scrollX);
        }
        if (Math.abs(canvas.getTranslateY() - scrollY) >= 0.001) {
            canvas.setTranslateY(scrollY);
        }

        if (Math.abs(canvas.getWidth() - viewW) >= 1.0 || Math.abs(canvas.getHeight() - viewH) >= 1.0) {
            canvas.setWidth(viewW);
            canvas.setHeight(viewH);
        }

        drawGrid(scrollX, scrollY, viewW, viewH, totalW, totalH);
    }

    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem itemAssign = new MenuItem("Song zuweisen...");
        MenuItem itemAddTrack = new MenuItem("Track hinzufügen");

        contextMenu.getItems().addAll(itemAssign, itemAddTrack);

        setOnContextMenuRequested(e -> {
            int trackIdx = getTrackIndexAtY(e.getY());
            List<TimelineTrack> tracks = service.getTracks();
            if (trackIdx >= 0 && trackIdx < tracks.size()) {
                TimelineTrack targetTrack = tracks.get(trackIdx);
                itemAssign.setText("Song zuweisen zu '" + targetTrack.getName() + "'...");
                itemAssign.setOnAction(ev -> handleAssignSongViaChooser(targetTrack));
            } else {
                itemAssign.setText("Song zuweisen...");
                itemAssign.setOnAction(ev -> {
                    TimelineTrack newTrack = service.addTrack();
                    handleAssignSongViaChooser(newTrack);
                });
            }
            itemAddTrack.setOnAction(ev -> service.addTrack());
            contextMenu.show(this, e.getScreenX(), e.getScreenY());
        });
    }

    private void handleAssignSongViaChooser(TimelineTrack targetTrack) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Song für " + targetTrack.getName() + " auswählen");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audiodateien (*.mp3, *.wav)", "*.mp3", "*.wav"));
        File chosen = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (chosen != null) {
            service.assignSongToTrack(targetTrack.getId(), chosen);
        }
    }

    private void handleSeek(double x) {
        long targetMs = xToMillis(x);
        service.setPositionMs(targetMs);
    }

    private void setupDragAndDrop() {
        setOnDragOver(e -> {
            Dragboard db = e.getDragboard();
            if (TimelineDragHelper.canAcceptDrag(db)) {
                e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            e.consume();
        });

        setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            Optional<File> audioOpt = TimelineDragHelper.extractAudioFile(db, service);

            if (audioOpt.isPresent()) {
                File targetFile = audioOpt.get();
                int trackIdx = getTrackIndexAtY(e.getY());
                List<TimelineTrack> tracks = service.getTracks();
                if (trackIdx >= 0 && trackIdx < tracks.size()) {
                    TimelineTrack targetTrack = tracks.get(trackIdx);
                    AppLogger.info("[TIMELINE-GRID] Drop auf Track %d ('%s'): %s", trackIdx, targetTrack.getName(), targetFile.getName());
                    service.assignSongToTrack(targetTrack.getId(), targetFile);
                    e.setDropCompleted(true);
                } else {
                    TimelineTrack newTrack = service.addTrack();
                    AppLogger.info("[TIMELINE-GRID] Drop auf neue Spur ('%s'): %s", newTrack.getName(), targetFile.getName());
                    service.assignSongToTrack(newTrack.getId(), targetFile);
                    e.setDropCompleted(true);
                }
            } else {
                AppLogger.warn("[TIMELINE-GRID] Drop fehlgeschlagen: Keine gültige Audiodatei im Dragboard.");
            }
            e.consume();
        });
    }

    private int getTrackIndexAtY(double y) {
        List<TimelineTrack> tracks = service.getTracks();
        double currentY = 0;
        for (int i = 0; i < tracks.size(); i++) {
            double h = tracks.get(i).getHeight();
            if (y >= currentY && y < currentY + h) {
                return i;
            }
            currentY += h;
        }
        return -1; // Freie Fläche unterhalb der Spuren -> Neue Spur anlegen!
    }

    /**
     * Ermittelt die passende Mauszeigerform an der angegebenen Position.
     * Zeigt Resize-Cursor über dem Playhead-Dreieck, Hand-Cursor über Clips (nur im gestoppten Zustand)
     * und Standardzeiger auf Freiflächen oder über gesperrten Clips während aktiver Show-Wiedergabe.
     */
    private void updateCursorForPosition(double x, double y) {
        double playheadContentX = millisToX(service.getCurrentPositionMs());
        if (y <= 24.0 && Math.abs(x - playheadContentX) <= 16.0) {
            setCursor(Cursor.H_RESIZE);
        } else if (findClipTrackAt(x, y) != null) {
            setCursor(service.isPlaying() ? Cursor.DEFAULT : Cursor.HAND);
        } else {
            setCursor(Cursor.DEFAULT);
        }
    }

    /**
     * Ermittelt, ob sich an den angegebenen Koordinaten ein Song-Clip befindet.
     *
     * @param x X-Koordinate in Content-Pixeln
     * @param y Y-Koordinate in Content-Pixeln
     * @return gefundener {@link TimelineTrack} oder {@code null}
     */
    public TimelineTrack findClipTrackAt(double x, double y) {
        List<TimelineTrack> tracks = service.getTracks();
        double currentY = 0;
        for (TimelineTrack track : tracks) {
            double h = track.getHeight();
            if (y >= currentY && y < currentY + h) {
                if (track.hasSong()) {
                    double clipStartX = millisToX(track.getStartTimeMs());
                    long dur = track.getDurationMs() > 0 ? track.getDurationMs() : 60_000L;
                    double clipW = Math.max(80.0, millisToX(dur));
                    if (x >= clipStartX && x <= clipStartX + clipW) {
                        return track;
                    }
                }
                return null;
            }
            currentY += h;
        }
        return null;
    }

    /**
     * Rechnet einen Zeitstempel in Millisekunden in eine horizontale X-Pixelposition um.
     *
     * @param ms Zeitstempel in Millisekunden
     * @return X-Koordinate in Pixeln
     */
    public double millisToX(long ms) {
        return (Math.max(0L, ms) / 1000.0) * pixelsPerSecond;
    }

    /**
     * Rechnet eine horizontale X-Pixelposition in Millisekunden um.
     *
     * @param x X-Koordinate in Pixeln
     * @return Zeitstempel in Millisekunden
     */
    public long xToMillis(double x) {
        if (x <= 0.0) return 0L;
        return Math.round((x / pixelsPerSecond) * 1000.0);
    }

    /**
     * Liefert den aktuellen horizontalen Zoom-Wert in Pixeln pro Sekunde.
     *
     * @return Zeitmaßstab in Pixeln pro Sekunde
     */
    public double getPixelsPerSecond() {
        return pixelsPerSecond;
    }

    /**
     * Setzt den horizontalen Zoom-Wert in Pixeln pro Sekunde, strikt begrenzt zwischen
     * {@link #MIN_PIXELS_PER_SECOND} und {@link #MAX_PIXELS_PER_SECOND}.
     *
     * @param pps der neue Zeitmaßstab in Pixeln pro Sekunde
     */
    public void setPixelsPerSecond(double pps) {
        double clamped = Math.max(MIN_PIXELS_PER_SECOND, Math.min(MAX_PIXELS_PER_SECOND, pps));
        if (Math.abs(this.pixelsPerSecond - clamped) > 0.001) {
            this.pixelsPerSecond = clamped;
            clearWaveformCache();
            updateGridDimensions();
            requestLayout();
            redraw();
        }
    }

    /**
     * Vergrößert die Zeitleiste horizontal um 20% (Zoom In), zentriert auf den Playhead oder die Viewport-Mitte.
     */
    public void zoomIn() {
        if (attachedScrollPane != null && attachedScrollPane.getViewportBounds() != null) {
            zoomAtPlayheadOrCenter(ZOOM_FACTOR);
        } else {
            setPixelsPerSecond(pixelsPerSecond * ZOOM_FACTOR);
        }
    }

    /**
     * Verkleinert die Zeitleiste horizontal um 20% (Zoom Out), zentriert auf den Playhead oder die Viewport-Mitte.
     */
    public void zoomOut() {
        if (attachedScrollPane != null && attachedScrollPane.getViewportBounds() != null) {
            zoomAtPlayheadOrCenter(1.0 / ZOOM_FACTOR);
        } else {
            setPixelsPerSecond(pixelsPerSecond / ZOOM_FACTOR);
        }
    }

    /**
     * Zoomt mit dem angegebenen Faktor und hält dabei den Zeitpunkt unter der gegebenen
     * Bildschirm-X-Koordinate im Viewport exakt fixiert (kein Wegspringen der Waveform).
     *
     * @param factor            Zoom-Faktor (> 1 für Zoom In, < 1 für Zoom Out)
     * @param screenXInViewport X-Position im sichtbaren Viewport (z. B. Mausposition)
     */
    public void zoomAtScreenX(double factor, double screenXInViewport) {
        if (factor <= 0.0) {
            return;
        }
        double viewW = resolveViewportWidth();
        double oldScrollX = 0.0;
        if (attachedScrollPane != null) {
            double totalW = Math.max(1200.0, getPrefWidth());
            double maxScrollX = Math.max(0.0, totalW - viewW);
            oldScrollX = maxScrollX * Math.max(0.0, Math.min(1.0, attachedScrollPane.getHvalue()));
        }

        double clampedScreenX = Math.max(0.0, Math.min(viewW, screenXInViewport));
        long anchorMs = xToMillis(oldScrollX + clampedScreenX);

        double oldPps = this.pixelsPerSecond;
        setPixelsPerSecond(oldPps * factor);

        if (attachedScrollPane != null && Math.abs(this.pixelsPerSecond - oldPps) > 0.001) {
            double newTotalW = Math.max(1200.0, getPrefWidth());
            double newMaxScrollX = Math.max(0.0, newTotalW - viewW);
            if (newMaxScrollX > 0.0) {
                double newAnchorContentX = millisToX(anchorMs);
                double newScrollX = Math.max(0.0, Math.min(newMaxScrollX, newAnchorContentX - clampedScreenX));
                attachedScrollPane.setHvalue(newScrollX / newMaxScrollX);
            } else {
                attachedScrollPane.setHvalue(0.0);
            }
            redraw();
        }
    }

    private void zoomAtPlayheadOrCenter(double factor) {
        double viewW = resolveViewportWidth();
        double totalW = Math.max(1200.0, getPrefWidth());
        double maxScrollX = Math.max(0.0, totalW - viewW);
        double scrollX = maxScrollX * Math.max(0.0, Math.min(1.0, attachedScrollPane.getHvalue()));
        double playheadScreenX = millisToX(service.getCurrentPositionMs()) - scrollX;
        double anchorScreenX = (playheadScreenX >= 0 && playheadScreenX <= viewW) ? playheadScreenX : (viewW * 0.5);
        zoomAtScreenX(factor, anchorScreenX);
    }

    /**
     * Setzt den Zoom auf den Standardwert (100% = 60.0 px/s) zurück.
     */
    public void resetZoom() {
        setPixelsPerSecond(DEFAULT_PIXELS_PER_SECOND);
    }

    /**
     * Aktualisiert die Gesamtbreite des Grid-Panes passend zur Maximallaufzeit der Timeline.
     *
     * @return {@code true}, wenn sich die Abmessungen geändert haben
     */
    public boolean updateGridDimensions() {
        long maxDurationMs = service.getMaxTimelineDurationMs();
        double targetWidth = Math.max(1200.0, millisToX(maxDurationMs) + (BAR_WIDTH * 8));
        double totalTracksH = service.getTracks().stream().mapToDouble(TimelineTrack::getHeight).sum();
        double targetHeight = Math.max(600.0, totalTracksH + 300.0);
        boolean changed = Math.abs(getPrefWidth() - targetWidth) > 0.5 || Math.abs(getPrefHeight() - targetHeight) > 0.5;
        if (changed) {
            setMinWidth(targetWidth);
            setPrefWidth(targetWidth);
            setMinHeight(targetHeight);
            setPrefHeight(targetHeight);
        }
        return changed;
    }

    /**
     * Leert den Offscreen-Waveform-Kachel-Cache (z. B. nach Zoom- oder Song-Änderung).
     */
    public void clearWaveformCache() {
        waveformTileCache.clear();
    }

    /**
     * Liefert die Anzahl der aktuell im Speicher gehaltenen Waveform-Kacheln.
     *
     * @return Anzahl gecachter Kacheln
     */
    public int getCachedWaveformTileCount() {
        return waveformTileCache.size();
    }

    /**
     * Abwärtskompatible Methode: Zeichnet das Playlist-Raster.
     */
    public void drawGrid() {
        redraw();
    }

    /**
     * Zeichnet das Playlist-Raster, die Song-Clips und die Timeline-Linie im sichtbaren Viewport.
     *
     * @param scrollX  horizontale Scroll-Position
     * @param scrollY  vertikale Scroll-Position
     * @param viewW    sichtbare Viewport-Breite
     * @param viewH    sichtbare Viewport-Höhe
     * @param totalW   Gesamtbreite des Rasters
     * @param totalH   Gesamthöhe des Rasters
     */
    private void drawGrid(double scrollX, double scrollY, double viewW, double viewH, double totalW, double totalH) {
        if (viewW <= 0 || viewH <= 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, viewW, viewH);

        List<TimelineTrack> tracks = service.getTracks();

        // 1. Hintergrund für die gesamte sichtbare Fläche
        gc.setFill(Color.web("#101010"));
        gc.fillRect(0, 0, viewW, viewH);

        // 2. Horizontale Spuren und Hintergrund-Streifen
        double currentY = 0;
        for (int i = 0; i < tracks.size(); i++) {
            TimelineTrack track = tracks.get(i);
            double trackH = track.getHeight();
            double screenY = currentY - scrollY;

            if (screenY + trackH > 0 && screenY < viewH) {
                if (i % 2 == 0) {
                    gc.setFill(Color.web("#141414"));
                } else {
                    gc.setFill(Color.web("#171717"));
                }
                gc.fillRect(0, screenY, viewW, trackH);
            }
            currentY += trackH;
        }

        // 3. Vertikale Takt- und Beat-Rasterlinien (nur für sichtbaren X-Bereich)
        double startX = Math.floor(scrollX / BEAT_WIDTH) * BEAT_WIDTH;
        double endX = scrollX + viewW;
        for (double x = startX; x <= endX; x += BEAT_WIDTH) {
            double screenX = x - scrollX;
            boolean isBarLine = Math.abs(x % BAR_WIDTH) < 0.1;
            if (isBarLine) {
                gc.setStroke(Color.web("#2A2A2A"));
                gc.setLineWidth(1.0);
                gc.strokeLine(screenX + 0.5, 0, screenX + 0.5, viewH);
            } else {
                gc.setStroke(Color.web("#1B1B1B"));
                gc.setLineWidth(0.5);
                gc.strokeLine(screenX + 0.5, 0, screenX + 0.5, viewH);
            }
        }

        // 4. Horizontale Trennlinien zwischen den Spuren
        currentY = 0;
        for (int i = 0; i < tracks.size(); i++) {
            TimelineTrack track = tracks.get(i);
            double trackH = track.getHeight();
            double screenY = currentY - scrollY;

            if (screenY + trackH > 0 && screenY < viewH) {
                gc.setStroke(Color.web("#252525"));
                gc.setLineWidth(1.0);
                gc.strokeLine(0, screenY + trackH - 0.5, viewW, screenY + trackH - 0.5);
            }
            currentY += trackH;
        }

        // 5. Song-Clips AUF das Raster zeichnen (Vordergrund)
        currentY = 0;
        for (int i = 0; i < tracks.size(); i++) {
            TimelineTrack track = tracks.get(i);
            double trackH = track.getHeight();
            double screenY = currentY - scrollY;

            if (track.hasSong() && screenY + trackH > 0 && screenY < viewH) {
                drawSongClip(gc, track, screenY, trackH, scrollX, viewW);
            }
            currentY += trackH;
        }

        // 6. Timeline-Linie (Playhead) ganz oben zeichnen
        drawPlayhead(gc, scrollX, viewW, viewH);
    }

    /**
     * Zeichnet einen Song-Clip Block auf einer Spur im modernen Pioneer Rekordbox 3-Band DJ-Stil
     * mithilfe gecachter 1024px-GPU-Kacheln für ruckelfreies 60/120 FPS Scrollen.
     */
    private void drawSongClip(GraphicsContext gc, TimelineTrack track, double screenY, double h, double scrollX, double viewW) {
        long durationMs = track.getDurationMs() > 0 ? track.getDurationMs() : 60_000L;
        double clipStartX = millisToX(track.getStartTimeMs()) - scrollX;
        double clipW = Math.max(80.0, millisToX(durationMs));
        double clipH = h - 6;
        double clipY = screenY + 3;

        // Nur zeichnen, wenn zumindest teilweise im sichtbaren Viewport
        if (clipStartX + clipW < 0 || clipStartX > viewW) {
            return;
        }

        // 1. Gecachte Rekordbox 3-Band Solid-Waveform Kacheln zeichnen (< 0.05 ms GPU-Blit!)
        boolean has3Band = track.has3BandWaveform();
        float[] waveform = track.getWaveformData();

        if (has3Band || (waveform != null && waveform.length > 0)) {
            int totalClipPx = Math.max(1, (int) Math.ceil(clipW));
            int clipHeightPx = Math.max(4, (int) Math.round(clipH));
            double visibleLeft = Math.max(0.0, -clipStartX);
            double visibleRight = Math.min(clipW, viewW - clipStartX);
            int firstTile = Math.max(0, (int) Math.floor(visibleLeft / WAVEFORM_TILE_WIDTH));
            int lastTile = Math.max(firstTile, (int) Math.floor(Math.max(0.0, visibleRight - 0.01) / WAVEFORM_TILE_WIDTH));

            for (int tileIdx = firstTile; tileIdx <= lastTile; tileIdx++) {
                int tileOffset = tileIdx * WAVEFORM_TILE_WIDTH;
                if (tileOffset >= totalClipPx) break;
                int tileW = Math.min(WAVEFORM_TILE_WIDTH, totalClipPx - tileOffset);
                Image tileImg = getOrRenderWaveformTile(track, totalClipPx, clipHeightPx, tileIdx, tileW);
                gc.drawImage(tileImg, clipStartX + tileOffset, clipY, tileW, clipH);
            }
        } else {
            // Fallback Hintergrund ohne Audiodaten
            gc.setFill(Color.web("#121215"));
            gc.fillRoundRect(clipStartX, clipY, clipW, clipH, 4, 4);
            double waveCenterY = clipY + (clipH / 2.0);
            gc.setStroke(Color.web("#1E1E24"));
            gc.setLineWidth(1.0);
            gc.strokeLine(Math.max(clipStartX, 0.0), waveCenterY, Math.min(clipStartX + clipW, viewW), waveCenterY);
        }

        // 2. Crisp Clip-Rahmen darüber
        gc.setStroke(Color.web("#27272A"));
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(clipStartX + 0.5, clipY + 0.5, clipW - 1.0, clipH - 1.0, 4, 4);
    }

    private Image getOrRenderWaveformTile(TimelineTrack track, int totalClipPx, int clipHeightPx, int tileIdx, int tileW) {
        String key = track.getId() + "|" + Math.round(pixelsPerSecond * 100.0)
                + "|" + totalClipPx + "|" + clipHeightPx + "|" + tileIdx;
        Image cached = waveformTileCache.get(key);
        if (cached != null && Math.abs(cached.getWidth() - tileW) < 1.0) {
            return cached;
        }
        Image rendered = renderWaveformTileImage(track, totalClipPx, clipHeightPx, tileIdx * WAVEFORM_TILE_WIDTH, tileW);
        waveformTileCache.put(key, rendered);
        return rendered;
    }

    private Image renderWaveformTileImage(TimelineTrack track, int totalClipPx, int clipHeightPx, int tileOffsetPx, int tileW) {
        WritableImage img = new WritableImage(tileW, clipHeightPx);
        int[] pixels = new int[tileW * clipHeightPx];

        boolean has3Band = track.has3BandWaveform();
        float[] lowBand = has3Band ? track.getLowBandData() : track.getWaveformData();
        float[] midBand = has3Band ? track.getMidBandData() : null;
        float[] highBand = has3Band ? track.getHighBandData() : null;

        double waveCenterY = clipHeightPx / 2.0;
        double maxAmpH = Math.max(2.0, (clipHeightPx / 2.0) - 3.0);

        // Exakte ARGB Farbkomponenten
        final int bgR = 0x12, bgG = 0x12, bgB = 0x15;       // Studio Deck #121215
        final int lineR = 0x1E, lineG = 0x1E, lineB = 0x24;   // Nulllinie #1E1E24
        final int lowR = 0x22, lowG = 0x53, lowB = 0xD9;      // Pioneer Royal Blue #2253D9
        final int midR = 0xD9, midG = 0x77, midB = 0x06;      // Amber Gold #D97706
        final int highR = 0xFF, highG = 0xFF, highB = 0xFF;   // Pure White #FFFFFF

        for (int px = 0; px < tileW; px++) {
            double progress = (tileOffsetPx + px) / (double) Math.max(1, totalClipPx - 1);
            float lowVal = sampleInterpolated(lowBand, progress);
            float midVal = has3Band ? sampleInterpolated(midBand, progress) : (lowVal * 0.65f);
            float highVal = has3Band ? sampleInterpolated(highBand, progress) : (lowVal * 0.25f);

            double lowAmp = lowBand != null && lowBand.length > 0 ? Math.min(maxAmpH, Math.max(0.5, lowVal * maxAmpH)) : 0.0;
            double midAmp = Math.min(maxAmpH, Math.max(0.5, midVal * maxAmpH * 0.75));
            double highAmp = Math.min(maxAmpH, Math.max(0.4, highVal * maxAmpH * 0.45));

            for (int py = 0; py < clipHeightPx; py++) {
                double dy = Math.abs((py + 0.5) - waveCenterY);

                int r = (dy < 0.75) ? lineR : bgR;
                int g = (dy < 0.75) ? lineG : bgG;
                int b = (dy < 0.75) ? lineB : bgB;

                // 1. Low Layer (Royal Blue) mit Anti-Aliased Coverage
                double covLow = clampCoverage(lowAmp + 0.5 - dy);
                if (covLow > 0.0) {
                    r = lerpInt(r, lowR, covLow);
                    g = lerpInt(g, lowG, covLow);
                    b = lerpInt(b, lowB, covLow);
                }

                // 2. Mid Layer (Amber Gold) darüberlegen
                double covMid = clampCoverage(midAmp + 0.5 - dy);
                if (covMid > 0.0) {
                    r = lerpInt(r, midR, covMid);
                    g = lerpInt(g, midG, covMid);
                    b = lerpInt(b, midB, covMid);
                }

                // 3. High Layer (Pure White) als innerster Kern oben drauf
                double covHigh = clampCoverage(highAmp + 0.5 - dy);
                if (covHigh > 0.0) {
                    r = lerpInt(r, highR, covHigh);
                    g = lerpInt(g, highG, covHigh);
                    b = lerpInt(b, highB, covHigh);
                }

                pixels[py * tileW + px] = (0xFF << 24) | (r << 16) | (g << 8) | b;
            }
        }

        img.getPixelWriter().setPixels(0, 0, tileW, clipHeightPx, PixelFormat.getIntArgbPreInstance(), pixels, 0, tileW);
        return img;
    }

    private static float sampleInterpolated(float[] data, double progress) {
        if (data == null || data.length == 0) {
            return 0.0f;
        }
        double exactIdx = Math.max(0.0, Math.min(data.length - 1, progress * (data.length - 1)));
        int i0 = (int) Math.floor(exactIdx);
        int i1 = Math.min(data.length - 1, i0 + 1);
        double frac = exactIdx - i0;
        return (float) (data[i0] * (1.0 - frac) + data[i1] * frac);
    }

    private static double clampCoverage(double val) {
        if (val <= 0.0) return 0.0;
        return Math.min(1.0, val);
    }

    private static int lerpInt(int from, int to, double t) {
        return (int) Math.round(from + (to - from) * t);
    }

    /**
     * Zeichnet die vertikale Timeline-Linie (Playhead) mit Kopfanzeiger.
     */
    private void drawPlayhead(GraphicsContext gc, double scrollX, double viewW, double viewH) {
        long posMs = activeRenderPositionMs >= 0L ? activeRenderPositionMs : service.getCurrentPositionMs();
        double playheadContentX = millisToX(posMs);
        double playheadScreenX = playheadContentX - scrollX;

        if (playheadScreenX < -10 || playheadScreenX > viewW + 10) {
            return;
        }

        // Vertikale Linie (warm orange / amber für optimale Sichtbarkeit)
        gc.setStroke(Color.web("#FB923C"));
        gc.setLineWidth(2.0);
        gc.strokeLine(playheadScreenX, 0, playheadScreenX, viewH);

        // Oberer Pfeil-Indikator (Playhead-Kopf / Greifdreieck)
        gc.setFill(Color.web("#FB923C"));
        double[] xPoints = {playheadScreenX - 8, playheadScreenX + 8, playheadScreenX};
        double[] yPoints = {0, 0, 12};
        gc.fillPolygon(xPoints, yPoints, 3);
        gc.setStroke(Color.web("#C2410C"));
        gc.setLineWidth(1.0);
        gc.strokePolygon(xPoints, yPoints, 3);
    }

    /**
     * Aktiviert/deaktiviert die exklusive VSync-Steuerung über {@link TimelineMainView}.
     *
     * @param vsyncControlled {@code true}, wenn ein externer AnimationTimer die Playback-Frames zeichnet
     */
    public void setVsyncControlled(boolean vsyncControlled) {
        this.vsyncControlled = vsyncControlled;
    }

    /**
     * Liefert die interne Canvas.
     *
     * @return die {@link Canvas}-Instanz
     */
    public Canvas getCanvas() {
        return canvas;
    }
}

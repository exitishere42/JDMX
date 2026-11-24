package de.exit.jdmx.ui.timeline;

import de.exit.jdmx.core.log.AppLogger;
import de.exit.jdmx.core.model.TimelineTrack;
import de.exit.jdmx.service.TimelineService;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
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
    private final Group waveformLayer = new Group();
    private final Group playheadGroup = new Group();
    private final Line playheadLine = new Line();
    private final Polygon playheadHead = new Polygon(-8.0, 0.0, 8.0, 0.0, 0.0, 12.0);
    private final Rectangle viewportClip = new Rectangle(1200, 600);

    private final Map<String, Image> waveformTileCache = new HashMap<>();
    private final Map<String, ImageView> activeTileViews = new HashMap<>();
    private final Map<String, Rectangle> activeEmptyClipNodes = new HashMap<>();

    private final DoubleProperty scrollXProperty = new SimpleDoubleProperty(0.0);
    private final DoubleProperty scrollYProperty = new SimpleDoubleProperty(0.0);
    private final DoubleProperty contentWidthProperty = new SimpleDoubleProperty(1200.0);
    private final DoubleProperty contentHeightProperty = new SimpleDoubleProperty(600.0);

    private ScrollPane attachedScrollPane;

    private double explicitViewportW = -1.0;
    private double explicitViewportH = -1.0;

    private double lastBgViewW = -1.0;
    private double lastBgViewH = -1.0;
    private double lastBgScrollY = Double.NaN;
    private double lastBgZoom = Double.NaN;
    private boolean bgDirty = true;
    private boolean waveformLayerDirty = true;
    private long gridRedrawCount = 0L;

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
        this.canvas.setCache(true);
        this.canvas.setCacheHint(javafx.scene.CacheHint.SPEED);

        this.waveformLayer.setManaged(false);
        this.waveformLayer.setMouseTransparent(true);

        this.playheadLine.setStartX(0.0);
        this.playheadLine.setEndX(0.0);
        this.playheadLine.setStartY(0.0);
        this.playheadLine.setEndY(600.0);
        this.playheadLine.setStroke(Color.web("#FB923C"));
        this.playheadLine.setStrokeWidth(2.0);
        this.playheadHead.setFill(Color.web("#FB923C"));
        this.playheadHead.setStroke(Color.web("#C2410C"));
        this.playheadHead.setStrokeWidth(1.0);
        this.playheadGroup.getChildren().addAll(playheadLine, playheadHead);
        this.playheadGroup.setManaged(false);
        this.playheadGroup.setMouseTransparent(true);
        this.playheadGroup.setCache(true);
        this.playheadGroup.setCacheHint(javafx.scene.CacheHint.SPEED);

        getChildren().addAll(canvas, waveformLayer, playheadGroup);
        setClip(viewportClip);
        setMinSize(0.0, 0.0);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        widthProperty().addListener((obs, oldV, newV) -> {
            if (attachedScrollPane == null) {
                redraw();
            }
        });
        heightProperty().addListener((obs, oldV, newV) -> {
            if (attachedScrollPane == null) {
                redraw();
            }
        });

        updateGridDimensions();

        service.addChangeListener(() -> {
            Runnable action = () -> {
                clearWaveformCache();
                bgDirty = true;
                waveformLayerDirty = true;
                boolean sizeChanged = updateGridDimensions();
                if (sizeChanged && attachedScrollPane != null) {
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
                updatePlaybackView(service.getCurrentPositionMs(), -1.0);
            } else {
                Platform.runLater(() -> updatePlaybackView(service.getCurrentPositionMs(), -1.0));
            }
        });

        // Interaktionssteuerung: Playhead-Dreieck (Scrubbing) ODER Lieder verschieben (Clip-Drag)
        setOnMousePressed(e -> {
            if (!e.isPrimaryButtonDown()) return;

            double worldX = toWorldX(e.getX());
            double worldY = toWorldY(e.getY());
            double playheadContentX = millisToX(service.getCurrentPositionMs());
            // Prüfen ob Klick auf das obere Playhead-Dreieck erfolgte (y <= 24px im Viewport und im Radius von 16px um den Playhead)
            if (e.getY() <= 24.0 && Math.abs(worldX - playheadContentX) <= 16.0) {
                isDraggingPlayhead = true;
                wasPlayingBeforePlayheadDrag = service.isPlaying();
                if (wasPlayingBeforePlayheadDrag) {
                    service.pausePlayback();
                }
                handleSeek(worldX);
                return;
            }

            // Prüfen ob Klick auf einen Song-Clip erfolgte
            TimelineTrack hitTrack = findClipTrackAt(worldX, worldY);
            if (hitTrack != null) {
                if (service.isPlaying()) {
                    // Während aktiver Show-Wiedergabe ist das Verschieben von Tracks gesperrt!
                    draggingClipTrack = null;
                    return;
                }
                draggingClipTrack = hitTrack;
                dragStartMouseX = worldX;
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

            double worldX = toWorldX(e.getX());
            if (isDraggingPlayhead) {
                handleSeek(worldX);
            } else if (draggingClipTrack != null) {
                if (service.isPlaying()) {
                    draggingClipTrack = null;
                    return;
                }
                double deltaPx = worldX - dragStartMouseX;
                long deltaMs = Math.round((deltaPx / pixelsPerSecond) * 1000.0);
                long newStartMs = Math.max(0L, dragStartClipTimeMs + deltaMs);
                service.setTrackStartTimeMs(draggingClipTrack.getId(), newStartMs);
                waveformLayerDirty = true;
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
            updateCursorForPosition(toWorldX(e.getX()), toWorldY(e.getY()));
        });

        setOnMouseMoved(e -> updateCursorForPosition(toWorldX(e.getX()), toWorldY(e.getY())));

        // Ctrl + Mausrad Zoom (Standard-DAW-Verhalten: Maus-zentriert)
        addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, e -> handleCtrlScroll(e, false));

        // Kontextmenü & Drag & Drop für Audio-Songs aus dem Explorer auf Spuren
        setupContextMenu();
        setupDragAndDrop();
    }

    /**
     * Verknüpft optional ein umschließendes {@link ScrollPane} (Legacy-/Test-Kompatibilität).
     * Im Standardbetrieb arbeitet {@link TimelineGridView} als entkoppelter GPU-Viewport ohne ScrollPane.
     *
     * @param scrollPane das umschließende ScrollPane
     */
    public void attachScrollPane(ScrollPane scrollPane) {
        this.attachedScrollPane = scrollPane;
        if (scrollPane != null) {
            updateGridDimensions();
            scrollPane.viewportBoundsProperty().addListener((obs, oldVal, newVal) -> redraw());
            scrollPane.hvalueProperty().addListener((obs, oldVal, newVal) -> {
                double maxScrollX = getMaxScrollX();
                double sx = maxScrollX * Math.max(0.0, Math.min(1.0, newVal.doubleValue()));
                scrollXProperty.set(sx);
                applyGpuFrame(activeRenderPositionMs);
            });
            scrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
                double maxScrollY = getMaxScrollY();
                double sy = maxScrollY * Math.max(0.0, Math.min(1.0, newVal.doubleValue()));
                scrollYProperty.set(sy);
                bgDirty = true;
                redraw();
            });

            // Auch auf ScrollPane-Ebene Ctrl + Mausrad Zoom abfangen (inkl. Linux/Trackpad Support)
            scrollPane.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, e -> handleCtrlScroll(e, true));
        }
        redraw();
    }

    private double resolveViewportWidth() {
        if (explicitViewportW > 0.0) {
            return explicitViewportW;
        }
        if (attachedScrollPane == null) {
            return Math.max(100.0, getWidth() > 0.0 ? getWidth() : 800.0);
        }
        double vpW = attachedScrollPane.getViewportBounds() != null ? attachedScrollPane.getViewportBounds().getWidth() : 0.0;
        if (vpW > 0.0) {
            return vpW;
        }
        return attachedScrollPane.getWidth() > 0.0 ? attachedScrollPane.getWidth() : 800.0;
    }

    private double resolveViewportHeight() {
        if (explicitViewportH > 0.0) {
            return explicitViewportH;
        }
        if (attachedScrollPane == null) {
            return Math.max(100.0, getHeight() > 0.0 ? getHeight() : 600.0);
        }
        double vpH = attachedScrollPane.getViewportBounds() != null ? attachedScrollPane.getViewportBounds().getHeight() : 0.0;
        if (vpH > 0.0) {
            return vpH;
        }
        return attachedScrollPane.getHeight() > 0.0 ? attachedScrollPane.getHeight() : 600.0;
    }

    private double toWorldX(double eventX) {
        return attachedScrollPane != null ? eventX : (eventX + scrollXProperty.get());
    }

    private double toWorldY(double eventY) {
        return attachedScrollPane != null ? eventY : (eventY + scrollYProperty.get());
    }

    private void handleCtrlScroll(javafx.scene.input.ScrollEvent e, boolean viewportRelative) {
        double deltaY = Math.abs(e.getDeltaY()) > 0.001 ? e.getDeltaY() : e.getTextDeltaY();
        if (e.isControlDown()) {
            if (Math.abs(deltaY) > 0.001) {
                double factor = deltaY > 0 ? ZOOM_FACTOR : (1.0 / ZOOM_FACTOR);
                double screenX = e.getX();
                if (!viewportRelative && attachedScrollPane != null) {
                    screenX = e.getX() - getScrollX();
                }
                zoomAtScreenX(factor, screenX);
                e.consume();
            }
            return;
        }

        // Glattes, pixel-genaues horizontales & vertikales Scrollen ohne grobe ScrollPane-Sprünge
        double viewH = resolveViewportHeight();
        double totalH = getContentHeight();
        boolean verticalFits = viewH > 0.0 && totalH <= viewH + 2.0;

        if (e.isShiftDown() || Math.abs(e.getDeltaX()) > 0.001 || (verticalFits && Math.abs(deltaY) > 0.001)) {
            double rawDelta = Math.abs(e.getDeltaX()) > 0.001 ? e.getDeltaX() : deltaY;
            if (Math.abs(rawDelta) > 0.001) {
                double maxScrollX = getMaxScrollX();
                if (maxScrollX > 0.0) {
                    double nextScrollX = Math.max(0.0, Math.min(maxScrollX, getScrollX() - (rawDelta * 1.2)));
                    setScrollX(nextScrollX);
                }
                e.consume();
            }
        } else if (attachedScrollPane == null && Math.abs(deltaY) > 0.001) {
            double maxScrollY = getMaxScrollY();
            if (maxScrollY > 0.0) {
                double nextScrollY = Math.max(0.0, Math.min(maxScrollY, getScrollY() - (deltaY * 1.2)));
                setScrollY(nextScrollY);
            }
            e.consume();
        }
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        double viewW = resolveViewportWidth();
        double viewH = resolveViewportHeight();
        if (attachedScrollPane == null) {
            viewportClip.setX(0.0);
            viewportClip.setY(0.0);
            viewportClip.setWidth(viewW);
            viewportClip.setHeight(viewH);
        } else {
            viewportClip.setX(getScrollX());
            viewportClip.setY(getScrollY());
            viewportClip.setWidth(viewW);
            viewportClip.setHeight(viewH);
        }
    }

    /**
     * Setzt die Viewport-Größe explizit (z. B. für Headless-Unit-Tests ohne Stage).
     *
     * @param width  Viewport-Breite in Pixeln
     * @param height Viewport-Höhe in Pixeln
     */
    public void setViewportSize(double width, double height) {
        this.explicitViewportW = Math.max(100.0, width);
        this.explicitViewportH = Math.max(100.0, height);
        this.bgDirty = true;
        this.waveformLayerDirty = true;
        redraw();
    }

    /**
     * Liefert die aktuelle horizontale Scroll-Position in Pixeln.
     *
     * @return horizontale Scroll-Position (px)
     */
    public double getScrollX() {
        if (attachedScrollPane != null) {
            return getMaxScrollX() * Math.max(0.0, Math.min(1.0, attachedScrollPane.getHvalue()));
        }
        return scrollXProperty.get();
    }

    /**
     * Setzt die horizontale Scroll-Position in Pixeln (reine GPU-Translation ohne Grid-Neuzeichnung).
     *
     * @param targetScrollX neue horizontale Scroll-Position (px)
     */
    public void setScrollX(double targetScrollX) {
        double maxScrollX = getMaxScrollX();
        double clamped = maxScrollX > 0.0 ? Math.max(0.0, Math.min(maxScrollX, targetScrollX)) : 0.0;
        if (attachedScrollPane != null) {
            attachedScrollPane.setHvalue(maxScrollX > 0.0 ? (clamped / maxScrollX) : 0.0);
            return;
        }
        if (Math.abs(scrollXProperty.get() - clamped) > 0.001) {
            scrollXProperty.set(clamped);
            applyGpuFrame(activeRenderPositionMs);
        }
    }

    /**
     * Liefert die aktuelle vertikale Scroll-Position in Pixeln.
     *
     * @return vertikale Scroll-Position (px)
     */
    public double getScrollY() {
        if (attachedScrollPane != null) {
            return getMaxScrollY() * Math.max(0.0, Math.min(1.0, attachedScrollPane.getVvalue()));
        }
        return scrollYProperty.get();
    }

    /**
     * Setzt die vertikale Scroll-Position in Pixeln.
     *
     * @param targetScrollY neue vertikale Scroll-Position (px)
     */
    public void setScrollY(double targetScrollY) {
        double maxScrollY = getMaxScrollY();
        double clamped = maxScrollY > 0.0 ? Math.max(0.0, Math.min(maxScrollY, targetScrollY)) : 0.0;
        if (attachedScrollPane != null) {
            attachedScrollPane.setVvalue(maxScrollY > 0.0 ? (clamped / maxScrollY) : 0.0);
            return;
        }
        if (Math.abs(scrollYProperty.get() - clamped) > 0.001) {
            scrollYProperty.set(clamped);
            bgDirty = true;
            redraw();
        }
    }

    public ReadOnlyDoubleProperty scrollXProperty() {
        return scrollXProperty;
    }

    public ReadOnlyDoubleProperty scrollYProperty() {
        return scrollYProperty;
    }

    public ReadOnlyDoubleProperty contentWidthProperty() {
        return contentWidthProperty;
    }

    public ReadOnlyDoubleProperty contentHeightProperty() {
        return contentHeightProperty;
    }

    /**
     * Liefert die aktuelle effektive Viewport-Breite in Pixeln.
     *
     * @return Viewport-Breite (px)
     */
    public double getViewportWidth() {
        return resolveViewportWidth();
    }

    public double getContentWidth() {
        return contentWidthProperty.get();
    }

    public double getContentHeight() {
        return contentHeightProperty.get();
    }

    public double getMaxScrollX() {
        return Math.max(0.0, getContentWidth() - resolveViewportWidth());
    }

    public double getMaxScrollY() {
        return Math.max(0.0, getContentHeight() - resolveViewportHeight());
    }

    /**
     * Aktualisiert Playhead und optionales horizontales Follow-Scrolling in einem einzigen
     * GPU-Frame-Durchlauf ohne Grid-Canvas-Neuzeichnung oder Layout-Pass.
     *
     * @param smoothedPositionMs aktuelle Playhead-Position in Millisekunden (oder {@code -1L})
     * @param targetScrollX      neue horizontale Scroll-Position in Pixeln, oder {@code < 0}, um {@code scrollX} beizubehalten
     */
    public void updatePlaybackView(long smoothedPositionMs, double targetScrollX) {
        this.activeRenderPositionMs = smoothedPositionMs;
        if (targetScrollX >= 0.0) {
            double maxScrollX = getMaxScrollX();
            double clamped = maxScrollX > 0.0 ? Math.max(0.0, Math.min(maxScrollX, targetScrollX)) : 0.0;
            if (attachedScrollPane != null) {
                double nextH = maxScrollX > 0.0 ? (clamped / maxScrollX) : 0.0;
                if (Math.abs(attachedScrollPane.getHvalue() - nextH) > 1e-7) {
                    attachedScrollPane.setHvalue(nextH);
                    return;
                }
            } else if (Math.abs(scrollXProperty.get() - clamped) > 0.001) {
                scrollXProperty.set(clamped);
            }
        }
        applyGpuFrame(smoothedPositionMs);
    }

    /**
     * Berechnet die Viewport-Koordinaten und aktualisiert alle GPU-Layer.
     */
    public void redraw() {
        redrawAtPosition(activeRenderPositionMs);
    }

    /**
     * Zeichnet das Grid inklusive des Playheads an der übergebenen VSync-interpolierten Position (in ms).
     *
     * @param smoothedPositionMs geglättete Playhead-Position oder {@code -1L} für {@link TimelineService#getCurrentPositionMs()}
     */
    public void redrawAtPosition(long smoothedPositionMs) {
        this.activeRenderPositionMs = smoothedPositionMs;
        applyGpuFrame(smoothedPositionMs);
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
            this.bgDirty = true;
            this.waveformLayerDirty = true;
            updateGridDimensions();
            if (attachedScrollPane != null) {
                requestLayout();
            }
            redraw();
        }
    }

    /**
     * Vergrößert die Zeitleiste horizontal um 20% (Zoom In), zentriert auf den Playhead oder die Viewport-Mitte.
     */
    public void zoomIn() {
        zoomAtPlayheadOrCenter(ZOOM_FACTOR);
    }

    /**
     * Verkleinert die Zeitleiste horizontal um 20% (Zoom Out), zentriert auf den Playhead oder die Viewport-Mitte.
     */
    public void zoomOut() {
        zoomAtPlayheadOrCenter(1.0 / ZOOM_FACTOR);
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
        double oldScrollX = getScrollX();

        double clampedScreenX = Math.max(0.0, Math.min(viewW, screenXInViewport));
        long anchorMs = xToMillis(oldScrollX + clampedScreenX);

        double oldPps = this.pixelsPerSecond;
        setPixelsPerSecond(oldPps * factor);

        if (Math.abs(this.pixelsPerSecond - oldPps) > 0.001) {
            double newMaxScrollX = getMaxScrollX();
            if (newMaxScrollX > 0.0) {
                double newAnchorContentX = millisToX(anchorMs);
                double newScrollX = Math.max(0.0, Math.min(newMaxScrollX, newAnchorContentX - clampedScreenX));
                setScrollX(newScrollX);
            } else {
                setScrollX(0.0);
            }
        }
    }

    private void zoomAtPlayheadOrCenter(double factor) {
        double viewW = resolveViewportWidth();
        double scrollX = getScrollX();
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
     * Aktualisiert die virtuellen Gesamtabmessungen der Timeline passend zur Maximallaufzeit.
     *
     * @return {@code true}, wenn sich die Abmessungen geändert haben
     */
    public boolean updateGridDimensions() {
        long maxDurationMs = service.getMaxTimelineDurationMs();
        double targetWidth = Math.max(1200.0, millisToX(maxDurationMs) + (BAR_WIDTH * 8));
        double totalTracksH = service.getTracks().stream().mapToDouble(TimelineTrack::getHeight).sum();
        double targetHeight = Math.max(600.0, totalTracksH + 300.0);
        boolean changed = Math.abs(contentWidthProperty.get() - targetWidth) > 0.5
                || Math.abs(contentHeightProperty.get() - targetHeight) > 0.5;
        if (changed) {
            contentWidthProperty.set(targetWidth);
            contentHeightProperty.set(targetHeight);
        }
        if (attachedScrollPane != null) {
            boolean prefChanged = Math.abs(getPrefWidth() - targetWidth) > 0.5 || Math.abs(getPrefHeight() - targetHeight) > 0.5;
            if (prefChanged) {
                setMinWidth(targetWidth);
                setPrefWidth(targetWidth);
                setMinHeight(targetHeight);
                setPrefHeight(targetHeight);
            }
        }
        return changed;
    }

    /**
     * Leert den Offscreen-Waveform-Kachel-Cache (z. B. nach Zoom- oder Song-Änderung).
     */
    public void clearWaveformCache() {
        waveformTileCache.clear();
        activeTileViews.clear();
        activeEmptyClipNodes.clear();
        waveformLayer.getChildren().clear();
        waveformLayerDirty = true;
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
     * Abwärtskompatible Methode: Aktualisiert alle GPU-Layer.
     */
    public void drawGrid() {
        redraw();
    }

    /**
     * Wendet den GPU-Scenegraph-Update-Zyklus für den aktuellen Viewport-Zustand an:
     * <ul>
     *     <li><b>Layer 1 (Grid-Canvas):</b> Wird nur bei Resize, Zoom, vertikalem Scroll oder Track-Änderung neu gezeichnet;
     *     horizontales Scrollen verschiebt lediglich {@code canvas.setTranslateX(-(scrollX % BAR_WIDTH))}.</li>
     *     <li><b>Layer 2 (Waveform-Layer):</b> Gecachte 1024px-{@link ImageView}-Kacheln im GPU-Scenegraph,
     *     verschoben via {@code waveformLayer.setTranslateX(-scrollX)}.</li>
     *     <li><b>Layer 3 (Playhead):</b> Direkte {@code playheadGroup.setTranslateX(...)}-Verschiebung ohne Redraw.</li>
     * </ul>
     */
    private void applyGpuFrame(long smoothedPositionMs) {
        double viewW = Math.min(MAX_CANVAS_SAFE_DIM, resolveViewportWidth());
        double viewH = Math.min(MAX_CANVAS_SAFE_DIM, resolveViewportHeight());
        if (viewW <= 0 || viewH <= 0) {
            return;
        }

        double scrollX = getScrollX();
        double scrollY = getScrollY();
        double baseX = attachedScrollPane != null ? scrollX : 0.0;
        double baseY = attachedScrollPane != null ? scrollY : 0.0;

        viewportClip.setX(baseX);
        viewportClip.setY(baseY);
        viewportClip.setWidth(viewW);
        viewportClip.setHeight(viewH);

        // Layer 1: Periodisches Beat-/Track-Hintergrund-Muster (nur neu zeichnen wenn nötig!)
        double patternW = Math.min(MAX_CANVAS_SAFE_DIM, viewW + BAR_WIDTH);
        boolean needBgRedraw = bgDirty
                || Math.abs(lastBgViewW - viewW) >= 1.0
                || Math.abs(lastBgViewH - viewH) >= 1.0
                || Math.abs(lastBgScrollY - scrollY) >= 0.5
                || Math.abs(lastBgZoom - pixelsPerSecond) > 0.001;

        if (needBgRedraw) {
            if (Math.abs(canvas.getWidth() - patternW) >= 1.0 || Math.abs(canvas.getHeight() - viewH) >= 1.0) {
                canvas.setWidth(patternW);
                canvas.setHeight(viewH);
            }
            drawGridBackgroundPattern(scrollY, patternW, viewH);
            lastBgViewW = viewW;
            lastBgViewH = viewH;
            lastBgScrollY = scrollY;
            lastBgZoom = pixelsPerSecond;
            bgDirty = false;
            gridRedrawCount++;
        }

        double patternOffset = scrollX % BAR_WIDTH;
        canvas.setTranslateX(baseX - patternOffset);
        canvas.setTranslateY(baseY);

        // Layer 2: GPU-Waveform-Kacheln (reine Translation + Kachel-Visibility-Sync)
        syncWaveformLayer(scrollX, scrollY, viewW, viewH);
        waveformLayer.setTranslateX(baseX - scrollX);
        waveformLayer.setTranslateY(baseY - scrollY);

        // Layer 3: GPU-Playhead (reine Translation)
        updatePlayheadNode(smoothedPositionMs, scrollX, viewW, viewH, baseX, baseY);
    }

    /**
     * Zeichnet das periodische Hintergrund- und Beat-Muster auf das Hintergrund-Canvas.
     */
    private void drawGridBackgroundPattern(double scrollY, double patternW, double viewH) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, patternW, viewH);

        List<TimelineTrack> tracks = service.getTracks();

        // 1. Hintergrund
        gc.setFill(Color.web("#101010"));
        gc.fillRect(0, 0, patternW, viewH);

        // 2. Horizontale Spur-Streifen
        double currentY = 0;
        for (int i = 0; i < tracks.size(); i++) {
            TimelineTrack track = tracks.get(i);
            double trackH = track.getHeight();
            double screenY = currentY - scrollY;

            if (screenY + trackH > 0 && screenY < viewH) {
                gc.setFill(i % 2 == 0 ? Color.web("#141414") : Color.web("#171717"));
                gc.fillRect(0, screenY, patternW, trackH);
            }
            currentY += trackH;
        }

        // 3. Periodisches Takt- und Beat-Raster (ab x=0, da TranslateX das Modulo abbildet)
        for (double x = 0; x <= patternW; x += BEAT_WIDTH) {
            boolean isBarLine = Math.abs(x % BAR_WIDTH) < 0.1;
            if (isBarLine) {
                gc.setStroke(Color.web("#2A2A2A"));
                gc.setLineWidth(1.0);
            } else {
                gc.setStroke(Color.web("#1B1B1B"));
                gc.setLineWidth(0.5);
            }
            gc.strokeLine(x + 0.5, 0, x + 0.5, viewH);
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
                gc.strokeLine(0, screenY + trackH - 0.5, patternW, screenY + trackH - 0.5);
            }
            currentY += trackH;
        }
    }

    /**
     * Synchronisiert die sichtbaren Waveform-{@link ImageView}-Kacheln im GPU-{@link Group}-Layer.
     * Bestehende Kacheln bleiben erhalten; nur neu in den Viewport eintretende oder verlassende Kacheln werden angepasst.
     */
    private void syncWaveformLayer(double scrollX, double scrollY, double viewW, double viewH) {
        List<TimelineTrack> tracks = service.getTracks();
        java.util.Set<String> requiredTileKeys = new java.util.HashSet<>();
        java.util.Set<String> requiredEmptyKeys = new java.util.HashSet<>();

        double currentY = 0;
        for (int i = 0; i < tracks.size(); i++) {
            TimelineTrack track = tracks.get(i);
            double trackH = track.getHeight();
            double screenY = currentY - scrollY;

            if (track.hasSong() && screenY + trackH > -100 && screenY < viewH + 100) {
                long durationMs = track.getDurationMs() > 0 ? track.getDurationMs() : 60_000L;
                double clipWorldX = millisToX(track.getStartTimeMs());
                double clipW = Math.max(80.0, millisToX(durationMs));
                double clipH = trackH - 6;
                double clipWorldY = currentY + 3;

                if (clipWorldX + clipW >= scrollX - WAVEFORM_TILE_WIDTH && clipWorldX <= scrollX + viewW + WAVEFORM_TILE_WIDTH) {
                    boolean has3Band = track.has3BandWaveform();
                    float[] waveform = track.getWaveformData();

                    if (has3Band || (waveform != null && waveform.length > 0)) {
                        int totalClipPx = Math.max(1, (int) Math.ceil(clipW));
                        int clipHeightPx = Math.max(4, (int) Math.round(clipH));
                        double visibleLeft = Math.max(0.0, (scrollX - WAVEFORM_TILE_WIDTH) - clipWorldX);
                        double visibleRight = Math.min(clipW, (scrollX + viewW + WAVEFORM_TILE_WIDTH) - clipWorldX);
                        int firstTile = Math.max(0, (int) Math.floor(visibleLeft / WAVEFORM_TILE_WIDTH));
                        int lastTile = Math.max(firstTile, (int) Math.floor(Math.max(0.0, visibleRight - 0.01) / WAVEFORM_TILE_WIDTH));

                        for (int tileIdx = firstTile; tileIdx <= lastTile; tileIdx++) {
                            int tileOffset = tileIdx * WAVEFORM_TILE_WIDTH;
                            if (tileOffset >= totalClipPx) break;
                            int tileW = Math.min(WAVEFORM_TILE_WIDTH, totalClipPx - tileOffset);
                            String key = track.getId() + "|" + Math.round(pixelsPerSecond * 100.0)
                                    + "|" + totalClipPx + "|" + clipHeightPx + "|" + tileIdx;
                            requiredTileKeys.add(key);

                            ImageView iv = activeTileViews.get(key);
                            if (iv == null) {
                                Image tileImg = getOrRenderWaveformTile(track, totalClipPx, clipHeightPx, tileIdx, tileW);
                                iv = new ImageView(tileImg);
                                iv.setSmooth(false);
                                iv.setMouseTransparent(true);
                                iv.setManaged(false);
                                activeTileViews.put(key, iv);
                                waveformLayer.getChildren().add(iv);
                            }
                            iv.setLayoutX(clipWorldX + tileOffset);
                            iv.setLayoutY(clipWorldY);
                        }
                    } else {
                        String emptyKey = track.getId() + "|empty";
                        requiredEmptyKeys.add(emptyKey);
                        Rectangle rect = activeEmptyClipNodes.get(emptyKey);
                        if (rect == null) {
                            rect = new Rectangle(clipW, clipH);
                            rect.setFill(Color.web("#121215"));
                            rect.setStroke(Color.web("#27272A"));
                            rect.setStrokeWidth(1.0);
                            rect.setArcWidth(4.0);
                            rect.setArcHeight(4.0);
                            rect.setMouseTransparent(true);
                            rect.setManaged(false);
                            activeEmptyClipNodes.put(emptyKey, rect);
                            waveformLayer.getChildren().add(rect);
                        }
                        rect.setWidth(clipW);
                        rect.setHeight(clipH);
                        rect.setLayoutX(clipWorldX);
                        rect.setLayoutY(clipWorldY);
                    }
                }
            }
            currentY += trackH;
        }

        // Veraltete / nicht mehr sichtbare Kacheln entfernen
        if (waveformLayerDirty || activeTileViews.size() > requiredTileKeys.size()) {
            activeTileViews.entrySet().removeIf(entry -> {
                if (!requiredTileKeys.contains(entry.getKey())) {
                    waveformLayer.getChildren().remove(entry.getValue());
                    return true;
                }
                return false;
            });
        }
        if (waveformLayerDirty || activeEmptyClipNodes.size() > requiredEmptyKeys.size()) {
            activeEmptyClipNodes.entrySet().removeIf(entry -> {
                if (!requiredEmptyKeys.contains(entry.getKey())) {
                    waveformLayer.getChildren().remove(entry.getValue());
                    return true;
                }
                return false;
            });
        }
        waveformLayerDirty = false;
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
        final int borderR = 0x27, borderG = 0x27, borderB = 0x2A; // Clip-Rahmen #27272A
        final int lowR = 0x22, lowG = 0x53, lowB = 0xD9;      // Pioneer Royal Blue #2253D9
        final int midR = 0xD9, midG = 0x77, midB = 0x06;      // Amber Gold #D97706
        final int highR = 0xFF, highG = 0xFF, highB = 0xFF;   // Pure White #FFFFFF

        for (int px = 0; px < tileW; px++) {
            boolean isLeftOrRightBorder = (tileOffsetPx + px == 0) || (tileOffsetPx + px == totalClipPx - 1);
            double progress = (tileOffsetPx + px) / (double) Math.max(1, totalClipPx - 1);
            float lowVal = sampleInterpolated(lowBand, progress);
            float midVal = has3Band ? sampleInterpolated(midBand, progress) : (lowVal * 0.65f);
            float highVal = has3Band ? sampleInterpolated(highBand, progress) : (lowVal * 0.25f);

            double lowAmp = lowBand != null && lowBand.length > 0 ? Math.min(maxAmpH, Math.max(0.5, lowVal * maxAmpH)) : 0.0;
            double midAmp = Math.min(maxAmpH, Math.max(0.5, midVal * maxAmpH * 0.75));
            double highAmp = Math.min(maxAmpH, Math.max(0.4, highVal * maxAmpH * 0.45));

            for (int py = 0; py < clipHeightPx; py++) {
                if (isLeftOrRightBorder || py == 0 || py == clipHeightPx - 1) {
                    pixels[py * tileW + px] = (0xFF << 24) | (borderR << 16) | (borderG << 8) | borderB;
                    continue;
                }

                double dy = Math.abs((py + 0.5) - waveCenterY);

                int r = (dy < 0.75) ? lineR : bgR;
                int g = (dy < 0.75) ? lineG : bgG;
                int b = (dy < 0.75) ? lineB : bgB;

                double covLow = clampCoverage(lowAmp + 0.5 - dy);
                if (covLow > 0.0) {
                    r = lerpInt(r, lowR, covLow);
                    g = lerpInt(g, lowG, covLow);
                    b = lerpInt(b, lowB, covLow);
                }

                double covMid = clampCoverage(midAmp + 0.5 - dy);
                if (covMid > 0.0) {
                    r = lerpInt(r, midR, covMid);
                    g = lerpInt(g, midG, covMid);
                    b = lerpInt(b, midB, covMid);
                }

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
     * Aktualisiert die GPU-Position des Playhead-Layers ohne Canvas-Neuzeichnung.
     */
    private void updatePlayheadNode(long smoothedPositionMs, double scrollX, double viewW, double viewH, double baseX, double baseY) {
        long posMs = smoothedPositionMs >= 0L ? smoothedPositionMs : service.getCurrentPositionMs();
        double playheadScreenX = millisToX(posMs) - scrollX;
        playheadLine.setEndY(viewH);
        playheadGroup.setVisible(playheadScreenX >= -12.0 && playheadScreenX <= viewW + 12.0);
        playheadGroup.setTranslateX(baseX + playheadScreenX);
        playheadGroup.setTranslateY(baseY);
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
     * Liefert die Anzahl der tatsächlichen Hintergrund-Grid-Canvas-Neuzeichnungen (zu Test-/Diagnosezwecken).
     *
     * @return Anzahl durchgeführter Hintergrund-Grid-Redraws
     */
    public long getGridRedrawCount() {
        return gridRedrawCount;
    }

    /**
     * Liefert den GPU-Waveform-Layer ({@link Group} mit gecachten {@link ImageView}-Kacheln).
     *
     * @return der Waveform-Layer
     */
    public Group getWaveformLayer() {
        return waveformLayer;
    }

    /**
     * Liefert die dedizierte GPU-Playhead-Node ({@link Group}).
     *
     * @return die Playhead-Node
     */
    public Node getPlayheadNode() {
        return playheadGroup;
    }

    /**
     * Liefert die interne Hintergrund-Grid-Canvas.
     *
     * @return die {@link Canvas}-Instanz
     */
    public Canvas getCanvas() {
        return canvas;
    }
}

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
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;
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

    private final TimelineService service;
    private final Canvas canvas;
    private ScrollPane attachedScrollPane;

    /** Dynamischer Zeitmaßstab: Aktuelle Pixel pro Sekunde (horizontaler Zoom). */
    private double pixelsPerSecond = DEFAULT_PIXELS_PER_SECOND;

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
        this.canvas.setMouseTransparent(true);
        getChildren().add(canvas);

        updateGridDimensions();

        service.addChangeListener(() -> {
            Runnable action = () -> {
                updateGridDimensions();
                requestLayout();
                redraw();
            };
            if (Platform.isFxApplicationThread() || getScene() == null) {
                action.run();
            } else {
                Platform.runLater(action);
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

        // Ctrl + Mausrad Zoom (Standard-DAW-Verhalten wie in FL Studio, Ableton, Cubase)
        addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, e -> {
            if (e.isControlDown()) {
                if (e.getDeltaY() > 0) {
                    zoomIn();
                } else if (e.getDeltaY() < 0) {
                    zoomOut();
                }
                e.consume();
            }
        });

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

            // Auch auf ScrollPane-Ebene Ctrl + Mausrad Zoom abfangen
            scrollPane.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, e -> {
                if (e.isControlDown()) {
                    if (e.getDeltaY() > 0) {
                        zoomIn();
                    } else if (e.getDeltaY() < 0) {
                        zoomOut();
                    }
                    e.consume();
                }
            });
        }
        redraw();
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
        double totalW = Math.max(1200.0, getPrefWidth());
        double totalH = Math.max(600.0, getPrefHeight());

        double viewW;
        double viewH;
        double scrollX = 0;
        double scrollY = 0;

        if (attachedScrollPane != null && attachedScrollPane.getViewportBounds() != null) {
            viewW = Math.min(MAX_CANVAS_SAFE_DIM, Math.max(100.0, attachedScrollPane.getViewportBounds().getWidth()));
            viewH = Math.min(MAX_CANVAS_SAFE_DIM, Math.max(100.0, attachedScrollPane.getViewportBounds().getHeight()));
            double maxScrollX = Math.max(0.0, totalW - viewW);
            double maxScrollY = Math.max(0.0, totalH - viewH);
            scrollX = maxScrollX * Math.max(0.0, Math.min(1.0, attachedScrollPane.getHvalue()));
            scrollY = maxScrollY * Math.max(0.0, Math.min(1.0, attachedScrollPane.getVvalue()));
        } else {
            viewW = Math.min(MAX_CANVAS_SAFE_DIM, Math.max(100.0, getWidth() > 0 ? getWidth() : totalW));
            viewH = Math.min(MAX_CANVAS_SAFE_DIM, Math.max(100.0, getHeight() > 0 ? getHeight() : totalH));
        }

        // Canvas an der sichtbaren Position im Pane verankern (nur bei echter Pixelverschiebung >= 1.0)
        double roundedScrollX = Math.round(scrollX);
        double roundedScrollY = Math.round(scrollY);
        if (Math.abs(canvas.getLayoutX() - roundedScrollX) >= 1.0) {
            canvas.setLayoutX(roundedScrollX);
        }
        if (Math.abs(canvas.getLayoutY() - roundedScrollY) >= 1.0) {
            canvas.setLayoutY(roundedScrollY);
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
            updateGridDimensions();
            requestLayout();
            redraw();
        }
    }

    /**
     * Vergrößert die Zeitleiste horizontal um 20% (Zoom In).
     */
    public void zoomIn() {
        setPixelsPerSecond(pixelsPerSecond * ZOOM_FACTOR);
    }

    /**
     * Verkleinert die Zeitleiste horizontal um 20% (Zoom Out).
     */
    public void zoomOut() {
        setPixelsPerSecond(pixelsPerSecond / ZOOM_FACTOR);
    }

    /**
     * Setzt den Zoom auf den Standardwert (100% = 60.0 px/s) zurück.
     */
    public void resetZoom() {
        setPixelsPerSecond(DEFAULT_PIXELS_PER_SECOND);
    }

    /**
     * Aktualisiert die Gesamtbreite des Grid-Panes passend zur Maximallaufzeit der Timeline.
     */
    public void updateGridDimensions() {
        long maxDurationMs = service.getMaxTimelineDurationMs();
        double targetWidth = Math.max(1200.0, millisToX(maxDurationMs) + (BAR_WIDTH * 8));
        double totalTracksH = service.getTracks().stream().mapToDouble(TimelineTrack::getHeight).sum();
        double targetHeight = Math.max(600.0, totalTracksH + 300.0);
        setMinWidth(targetWidth);
        setPrefWidth(targetWidth);
        setMinHeight(targetHeight);
        setPrefHeight(targetHeight);
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
     * (Blau für Bass/Subbass < 250 Hz, Orange für Mitten 250-3000 Hz, Weiß für Höhen > 3000 Hz).
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

        // 1. Clip-Karten-Hintergrund: Tiefdunkles Studio-Deck (Pioneer CDJ-3000 Anthrazit)
        gc.setFill(Color.web("#121215"));
        gc.fillRoundRect(clipStartX, clipY, clipW, clipH, 4, 4);

        gc.setStroke(Color.web("#27272A"));
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(clipStartX + 0.5, clipY + 0.5, clipW - 1.0, clipH - 1.0, 4, 4);

        // 2. Subtile Nulllinie in der vertikalen Mitte
        double waveCenterY = clipY + (clipH / 2.0);
        double maxAmpH = (clipH / 2.0) - 3.0;

        double drawStartX = Math.max(clipStartX, 0.0);
        double drawEndX = Math.min(clipStartX + clipW, viewW);

        gc.setStroke(Color.web("#1E1E24"));
        gc.setLineWidth(1.0);
        gc.strokeLine(drawStartX, waveCenterY, drawEndX, waveCenterY);

        // 3. Rekordbox 3-Band Solid-Wellenform zeichnen (Drei eigenständige Layer deckend aufeinandergelegt)
        boolean has3Band = track.has3BandWaveform();
        float[] waveform = track.getWaveformData();

        if (has3Band) {
            // Layer 1: Bass / Low-Band (Royal Blue) als Fundament
            drawSingleBandSolidWaveform(gc, track.getLowBandData(), clipStartX, clipW, drawStartX, drawEndX,
                    waveCenterY, maxAmpH, 1.0, COLOR_REKORDBOX_LOW);

            // Layer 2: Mitten / Mid-Band (Warmes Amber Gold / Orange) direkt über das Blau gelegt
            drawSingleBandSolidWaveform(gc, track.getMidBandData(), clipStartX, clipW, drawStartX, drawEndX,
                    waveCenterY, maxAmpH, 0.75, COLOR_REKORDBOX_MID);

            // Layer 3: Höhen / Transienten / High-Band (Pure White) ganz oben drauf gelegt
            drawSingleBandSolidWaveform(gc, track.getHighBandData(), clipStartX, clipW, drawStartX, drawEndX,
                    waveCenterY, maxAmpH, 0.45, COLOR_REKORDBOX_HIGH);
        } else if (waveform != null && waveform.length > 0) {
            // Fallback für Monochrom-Wellenformen: Solides Polygon in Studio-Blau
            drawSingleBandSolidWaveform(gc, waveform, clipStartX, clipW, drawStartX, drawEndX,
                    waveCenterY, maxAmpH, 1.0, COLOR_REKORDBOX_LOW);
        }
    }

    /**
     * Zeichnet einen einzelnen Frequenzband-Wellenformkörper als massives, geschlossenes 2D-Polygon
     * (Solid Mesh) ohne Zwischenräume oder Einzelstriche, identisch zum Pioneer Rekordbox DJ-System.
     *
     * @param gc          der JavaFX {@link GraphicsContext}
     * @param bandData    die normalisierten Amplituden-Werte (0.0 bis 1.0)
     * @param clipStartX  Startposition des Clips in Bildschirmkoordinaten
     * @param clipW       Gesamtbreite des Clips in Pixeln
     * @param drawStartX  linke Begrenzung des sichtbaren Bereichs
     * @param drawEndX    rechte Begrenzung des sichtbaren Bereichs
     * @param waveCenterY vertikale Mittellinie des Clips
     * @param maxAmpH     maximale halbe Amplitude in Pixeln
     * @param scaleFactor Amplituden-Skalierungsfaktor dieses Frequenzbandes
     * @param fillColor   Füllfarbe des Polygons
     */
    private void drawSingleBandSolidWaveform(
            GraphicsContext gc,
            float[] bandData,
            double clipStartX,
            double clipW,
            double drawStartX,
            double drawEndX,
            double waveCenterY,
            double maxAmpH,
            double scaleFactor,
            Color fillColor
    ) {
        if (bandData == null || bandData.length == 0 || drawEndX <= drawStartX || clipW <= 0) {
            return;
        }

        int len = bandData.length;
        double stepPx = 1.0; // 1-Pixel-Schritte für maximale Schärfe und Glätte

        gc.setFill(fillColor);
        gc.beginPath();

        // 1. Obere Hüllkurve: Von links nach rechts (drawStartX bis drawEndX)
        boolean first = true;
        for (double x = drawStartX; x <= drawEndX; x += stepPx) {
            double progress = (x - clipStartX) / clipW;
            int idx = Math.min((int) (progress * len), len - 1);
            float val = bandData[idx];
            double amp = Math.min(maxAmpH, Math.max(0.5, val * maxAmpH * scaleFactor));
            double yTop = waveCenterY - amp;

            if (first) {
                gc.moveTo(x, yTop);
                first = false;
            } else {
                gc.lineTo(x, yTop);
            }
        }

        // 2. Untere Hüllkurve: Von rechts nach links (drawEndX zurück zu drawStartX)
        for (double x = drawEndX; x >= drawStartX; x -= stepPx) {
            double progress = (x - clipStartX) / clipW;
            int idx = Math.min((int) (progress * len), len - 1);
            float val = bandData[idx];
            double amp = Math.min(maxAmpH, Math.max(0.5, val * maxAmpH * scaleFactor));
            double yBottom = waveCenterY + amp;

            gc.lineTo(x, yBottom);
        }

        // 3. Pfad schließen und vollflächig füllen
        gc.closePath();
        gc.fill();
    }

    /**
     * Zeichnet die vertikale Timeline-Linie (Playhead) mit Kopfanzeiger.
     */
    private void drawPlayhead(GraphicsContext gc, double scrollX, double viewW, double viewH) {
        double playheadContentX = millisToX(service.getCurrentPositionMs());
        double playheadScreenX = Math.round(playheadContentX - scrollX);

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
     * Liefert die interne Canvas.
     *
     * @return die {@link Canvas}-Instanz
     */
    public Canvas getCanvas() {
        return canvas;
    }
}

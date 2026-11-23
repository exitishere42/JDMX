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

    /** Zeitmaßstab: Anzahl Pixel pro Sekunde (48px = 1 Sekunde). */
    public static final double PIXELS_PER_SECOND = 48.0;

    /** Maximale sichere Canvas-Breite für GPU-Texturen (verhindert HiDPI 16384px Überlauf). */
    public static final double MAX_CANVAS_SAFE_DIM = 4096.0;

    private final TimelineService service;
    private final Canvas canvas;
    private ScrollPane attachedScrollPane;

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
                long deltaMs = Math.round((deltaPx / PIXELS_PER_SECOND) * 1000.0);
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
        return (Math.max(0L, ms) / 1000.0) * PIXELS_PER_SECOND;
    }

    /**
     * Rechnet eine horizontale X-Pixelposition in Millisekunden um.
     *
     * @param x X-Koordinate in Pixeln
     * @return Zeitstempel in Millisekunden
     */
    public long xToMillis(double x) {
        if (x <= 0.0) return 0L;
        return Math.round((x / PIXELS_PER_SECOND) * 1000.0);
    }

    /**
     * Aktualisiert die Gesamtbreite des Grid-Panes passend zur Maximallaufzeit der Timeline.
     */
    public void updateGridDimensions() {
        long maxDurationMs = service.getMaxTimelineDurationMs();
        double targetWidth = Math.max(1200.0, millisToX(maxDurationMs) + (BAR_WIDTH * 8));
        double targetHeight = Math.max(600.0, (service.getTracks().size() * 48.0) + 300.0);
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

        // 3. Rekordbox 3-Band Wellenform zeichnen
        boolean has3Band = track.has3BandWaveform();
        float[] waveform = track.getWaveformData();

        if (has3Band || (waveform != null && waveform.length > 0)) {
            float[] lowBand = has3Band ? track.getLowBandData() : null;
            float[] midBand = has3Band ? track.getMidBandData() : null;
            float[] highBand = has3Band ? track.getHighBandData() : null;

            int len = has3Band ? lowBand.length : waveform.length;
            double stepPx = 1.5; // Feine, dichte Schrittweite

            if (drawEndX > drawStartX && len > 0) {
                // A. Bass-Band (Blau / Electric Blue #2563EB)
                gc.setStroke(Color.web("#2563EB"));
                gc.setLineWidth(1.2);
                gc.beginPath();
                for (double wx = drawStartX; wx <= drawEndX; wx += stepPx) {
                    double progress = (wx - clipStartX) / clipW;
                    int idx = Math.min((int) (progress * len), len - 1);
                    float lowVal = has3Band ? lowBand[idx] : (waveform[idx] * 0.50f);
                    double lowH = Math.min(maxAmpH, Math.max(0.5, lowVal * maxAmpH));
                    gc.moveTo(wx, waveCenterY - lowH);
                    gc.lineTo(wx, waveCenterY + lowH);
                }
                gc.stroke();

                // B. Mitten-Band (Warmes Bernstein / Orange #F59E0B)
                gc.setStroke(Color.web("#F59E0B"));
                gc.setLineWidth(1.2);
                gc.beginPath();
                for (double wx = drawStartX; wx <= drawEndX; wx += stepPx) {
                    double progress = (wx - clipStartX) / clipW;
                    int idx = Math.min((int) (progress * len), len - 1);
                    float lowVal = has3Band ? lowBand[idx] : (waveform[idx] * 0.50f);
                    float midVal = has3Band ? midBand[idx] : (waveform[idx] * 0.35f);
                    double lowH = Math.min(maxAmpH, Math.max(0.5, lowVal * maxAmpH));
                    double midTotalH = Math.min(maxAmpH, lowH + (midVal * maxAmpH * 0.7));
                    if (midTotalH > lowH) {
                        gc.moveTo(wx, waveCenterY - midTotalH);
                        gc.lineTo(wx, waveCenterY - lowH);
                        gc.moveTo(wx, waveCenterY + lowH);
                        gc.lineTo(wx, waveCenterY + midTotalH);
                    }
                }
                gc.stroke();

                // C. Höhen-Band (Strahlendes Weiß / Ice Cyan #FFFFFF)
                gc.setStroke(Color.web("#FFFFFF"));
                gc.setLineWidth(1.2);
                gc.beginPath();
                for (double wx = drawStartX; wx <= drawEndX; wx += stepPx) {
                    double progress = (wx - clipStartX) / clipW;
                    int idx = Math.min((int) (progress * len), len - 1);
                    float lowVal = has3Band ? lowBand[idx] : (waveform[idx] * 0.50f);
                    float midVal = has3Band ? midBand[idx] : (waveform[idx] * 0.35f);
                    float highVal = has3Band ? highBand[idx] : (waveform[idx] * 0.15f);
                    double lowH = Math.min(maxAmpH, Math.max(0.5, lowVal * maxAmpH));
                    double midTotalH = Math.min(maxAmpH, lowH + (midVal * maxAmpH * 0.7));
                    double highTotalH = Math.min(maxAmpH, midTotalH + (highVal * maxAmpH * 0.6));
                    if (highTotalH > midTotalH) {
                        gc.moveTo(wx, waveCenterY - highTotalH);
                        gc.lineTo(wx, waveCenterY - midTotalH);
                        gc.moveTo(wx, waveCenterY + midTotalH);
                        gc.lineTo(wx, waveCenterY + highTotalH);
                    }
                }
                gc.stroke();
            }
        }

        // 4. Song-Titel Header mit Studio-Pill für perfekte Lesbarkeit
        String durationStr = formatDuration(durationMs);
        String label = track.getSongName() + " (" + durationStr + ")";
        if (track.getStartTimeMs() > 0) {
            label += " @" + formatDuration(track.getStartTimeMs());
        }

        double textX = Math.max(clipStartX + 10, 10);
        if (textX < viewW - 20) {
            // Hintergrund-Pill für den Text
            double textW = Math.min(viewW - textX - 10, (label.length() * 6.5) + 12);
            gc.setFill(Color.web("#09090B", 0.75));
            gc.fillRoundRect(textX - 4, clipY + 3, textW, 16, 4, 4);

            gc.setFill(Color.web("#F4F4F5"));
            gc.fillText(label, textX, clipY + 15);
        }
    }

    private String formatDuration(long ms) {
        long totalSec = ms / 1000;
        long mins = totalSec / 60;
        long secs = totalSec % 60;
        return String.format("%02d:%02d", mins, secs);
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

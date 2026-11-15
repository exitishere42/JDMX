package de.exit.jdmx.ui.timeline;

import de.exit.jdmx.core.log.AppLogger;
import de.exit.jdmx.core.model.TimelineTrack;
import de.exit.jdmx.service.TimelineService;
import de.exit.jdmx.ui.stage.ProjectExplorerView;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

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
 *     <li>Song-Clips und Audio-Blöcke auf Spuren mit zugewiesenem Song</li>
 *     <li>Die dynamische <b>Timeline-Linie (Playhead)</b>, die exakt anzeigt, wo sich die Wiedergabe befindet</li>
 * </ul>
 * Ermöglicht Scrubbing per Mausklick sowie Drag & Drop von Songs aus dem Explorer direkt auf Spuren.
 * </p>
 */
public class TimelineGridView extends Pane {

    /** Raster-Breite pro Takt (Bar) in Pixeln. */
    public static final double BAR_WIDTH = 96.0;

    /** Raster-Unterteilung pro Viertel-Beat in Pixeln. */
    public static final double BEAT_WIDTH = BAR_WIDTH / 4.0;

    /** Zeitmaßstab: Anzahl Pixel pro Sekunde (48px = 1 Sekunde). */
    public static final double PIXELS_PER_SECOND = 48.0;

    private final TimelineService service;
    private final Canvas canvas;

    /**
     * Erstellt die Grid-Ansicht für die gegebene TimelineService-Instanz.
     *
     * @param service der {@link TimelineService} (darf nicht {@code null} sein)
     */
    public TimelineGridView(TimelineService service) {
        this.service = Objects.requireNonNull(service, "service must not be null");
        getStyleClass().add("timeline-grid-view");

        this.canvas = new Canvas();
        getChildren().add(canvas);

        updateGridDimensions();

        service.addChangeListener(() -> {
            Runnable action = () -> {
                updateGridDimensions();
                requestLayout();
                drawGrid();
            };
            if (javafx.application.Platform.isFxApplicationThread()) {
                action.run();
            } else {
                javafx.application.Platform.runLater(action);
            }
        });

        // Klick und Drag zum Scrubben (Versetzen der Timeline-Linie)
        setOnMousePressed(e -> {
            if (e.isPrimaryButtonDown()) {
                handleSeek(e.getX());
            }
        });
        setOnMouseDragged(e -> {
            if (e.isPrimaryButtonDown()) {
                handleSeek(e.getX());
            }
        });

        // Kontextmenü & Drag & Drop für Audio-Songs aus dem Explorer auf Spuren
        setupContextMenu();
        setupDragAndDrop();
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        double w = Math.max(1.0, getWidth());
        double h = Math.max(1.0, getHeight());
        if (canvas.getWidth() != w || canvas.getHeight() != h) {
            canvas.setWidth(w);
            canvas.setHeight(h);
        }
        drawGrid();
    }

    private void setupContextMenu() {
        javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();

        javafx.scene.control.MenuItem itemAssign = new javafx.scene.control.MenuItem("Song zuweisen...");
        javafx.scene.control.MenuItem itemAddTrack = new javafx.scene.control.MenuItem("Track hinzufügen");

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
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Song für " + targetTrack.getName() + " auswählen");
        chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Audiodateien (*.mp3, *.wav)", "*.mp3", "*.wav"));
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
     * Zeichnet das Playlist-Raster, die Song-Clips und die Timeline-Linie auf die Canvas.
     */
    public void drawGrid() {
        double w = Math.max(getWidth(), canvas.getWidth());
        double h = Math.max(getHeight(), canvas.getHeight());
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        List<TimelineTrack> tracks = service.getTracks();
        double totalTracksHeight = tracks.size() * 48.0;
        double gridHeight = Math.max(h, totalTracksHeight);

        // 1. Hintergrund für gesamte Fläche
        gc.setFill(Color.web("#101010"));
        gc.fillRect(0, 0, w, h);

        // 2. Horizontale Spuren und Hintergrund-Streifen
        double currentY = 0;
        for (int i = 0; i < tracks.size(); i++) {
            TimelineTrack track = tracks.get(i);
            double trackH = track.getHeight();

            if (i % 2 == 0) {
                gc.setFill(Color.web("#141414"));
            } else {
                gc.setFill(Color.web("#171717"));
            }
            gc.fillRect(0, currentY, w, trackH);
            currentY += trackH;
        }

        // 3. Vertikale Takt- und Beat-Rasterlinien zeichnen (Bars & Beats)
        for (double x = 0; x < w; x += BEAT_WIDTH) {
            boolean isBarLine = Math.abs(x % BAR_WIDTH) < 0.1;
            if (isBarLine) {
                gc.setStroke(Color.web("#2A2A2A"));
                gc.setLineWidth(1.0);
                gc.strokeLine(x + 0.5, 0, x + 0.5, gridHeight);
            } else {
                gc.setStroke(Color.web("#1B1B1B"));
                gc.setLineWidth(0.5);
                gc.strokeLine(x + 0.5, 0, x + 0.5, gridHeight);
            }
        }

        // 4. Horizontale Trennlinien zwischen den Spuren
        currentY = 0;
        for (int i = 0; i < tracks.size(); i++) {
            TimelineTrack track = tracks.get(i);
            double trackH = track.getHeight();
            gc.setStroke(Color.web("#252525"));
            gc.setLineWidth(1.0);
            gc.strokeLine(0, currentY + trackH - 0.5, w, currentY + trackH - 0.5);
            currentY += trackH;
        }

        // 5. Song-Clips AUF das Raster zeichnen (Vordergrund)
        currentY = 0;
        for (int i = 0; i < tracks.size(); i++) {
            TimelineTrack track = tracks.get(i);
            double trackH = track.getHeight();
            if (track.hasSong()) {
                drawSongClip(gc, track, currentY, trackH, w);
            }
            currentY += trackH;
        }

        // 6. Timeline-Linie (Playhead) ganz oben zeichnen
        drawPlayhead(gc, gridHeight);
    }

    /**
     * Zeichnet einen Song-Clip Block auf einer Spur mit realer Länge und echter Wellenform.
     */
    private void drawSongClip(GraphicsContext gc, TimelineTrack track, double y, double h, double maxW) {
        double clipX = 0;
        long durationMs = track.getDurationMs() > 0 ? track.getDurationMs() : 60_000L;
        double clipW = Math.max(80.0, millisToX(durationMs));
        double clipH = h - 6;
        double clipY = y + 3;

        // Clip-Karten-Hintergrund (smaragdgrün/teal Studio-Theme)
        gc.setFill(Color.web("#064E3B"));
        gc.fillRoundRect(clipX + 2, clipY, clipW, clipH, 6, 6);

        gc.setStroke(Color.web("#10B981"));
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(clipX + 2, clipY, clipW, clipH, 6, 6);

        // Echte Wellenform zeichnen
        float[] waveform = track.getWaveformData();
        if (waveform != null && waveform.length > 0) {
            gc.setStroke(Color.web("#34D399"));
            gc.setLineWidth(1.5);
            double waveCenterY = clipY + (clipH / 2.0);
            double maxAmpH = (clipH / 2.0) - 5.0;

            double stepPx = 2.0;
            for (double wx = clipX + 4; wx < clipX + clipW - 4; wx += stepPx) {
                double progress = (wx - clipX) / clipW;
                int sampleIdx = (int) (progress * (waveform.length - 1));
                float amp = waveform[Math.min(sampleIdx, waveform.length - 1)];
                double ampHeight = Math.max(1.0, amp * maxAmpH);
                gc.strokeLine(wx, waveCenterY - ampHeight, wx, waveCenterY + ampHeight);
            }
        } else {
            // Subtile Mittellinie als visueller Anker, falls noch keine Wellenform berechnet
            gc.setStroke(Color.web("#059669"));
            gc.setLineWidth(1.0);
            double waveCenterY = clipY + (clipH / 2.0);
            gc.strokeLine(clipX + 8, waveCenterY, clipX + clipW - 8, waveCenterY);
        }

        // Song-Titel Header mit Formatierung der Laufzeit
        gc.setFill(Color.web("#F3F4F6"));
        String durationStr = formatDuration(durationMs);
        gc.fillText(track.getSongName() + " (" + durationStr + ")", clipX + 12, clipY + 14);
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
    private void drawPlayhead(GraphicsContext gc, double height) {
        double playheadX = Math.round(millisToX(service.getCurrentPositionMs()));

        // Vertikale Linie (warm orange / amber für optimale Sichtbarkeit)
        gc.setStroke(Color.web("#FB923C"));
        gc.setLineWidth(2.0);
        gc.strokeLine(playheadX, 0, playheadX, height);

        // Oberer Pfeil-Indikator (Playhead-Kopf)
        gc.setFill(Color.web("#FB923C"));
        double[] xPoints = {playheadX - 6, playheadX + 6, playheadX};
        double[] yPoints = {0, 0, 9};
        gc.fillPolygon(xPoints, yPoints, 3);
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

package de.exit.jdmx.ui.timeline;

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

        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        widthProperty().addListener((obs, oldW, newW) -> drawGrid());
        heightProperty().addListener((obs, oldH, newH) -> drawGrid());

        service.addChangeListener(this::drawGrid);

        // Klick und Drag zum Scrubben (Versetzen der Timeline-Linie)
        setOnMousePressed(e -> handleSeek(e.getX()));
        setOnMouseDragged(e -> handleSeek(e.getX()));

        // Drag & Drop für Audio-Songs aus dem Explorer auf Spuren
        setupDragAndDrop();
    }

    private void handleSeek(double x) {
        long targetMs = xToMillis(x);
        service.setPositionMs(targetMs);
    }

    private void setupDragAndDrop() {
        setOnDragOver(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasContent(ProjectExplorerView.EXPLORER_ITEM_FORMAT) || db.hasString()) {
                e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            e.consume();
        });

        setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            String draggedText = (String) (db.hasContent(ProjectExplorerView.EXPLORER_ITEM_FORMAT)
                    ? db.getContent(ProjectExplorerView.EXPLORER_ITEM_FORMAT)
                    : db.getString());

            if (draggedText != null) {
                // Ermitteln, auf welche Spur die Maus zeigt
                int trackIdx = getTrackIndexAtY(e.getY());
                List<TimelineTrack> tracks = service.getTracks();
                if (trackIdx >= 0 && trackIdx < tracks.size()) {
                    TimelineTrack targetTrack = tracks.get(trackIdx);

                    // Song anhand ID oder Name auffinden
                    String songName = db.hasString() ? db.getString() : draggedText;
                    Optional<File> songOpt = service.findSongFileByName(songName);

                    File songFile = songOpt.orElseGet(() -> new File(songName));
                    service.assignSongToTrack(targetTrack.getId(), songFile);
                    e.setDropCompleted(true);
                }
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
        return tracks.isEmpty() ? -1 : tracks.size() - 1;
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
     * Zeichnet das Playlist-Raster, die Song-Clips und die Timeline-Linie auf die Canvas.
     */
    public void drawGrid() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        List<TimelineTrack> tracks = service.getTracks();
        double currentY = 0;

        // 1. Horizontale Spuren und Hintergründe zeichnen
        for (int i = 0; i < tracks.size(); i++) {
            TimelineTrack track = tracks.get(i);
            double trackH = track.getHeight();

            // Alternierende Zeilenfarbe
            if (i % 2 == 0) {
                gc.setFill(Color.web("#141414"));
            } else {
                gc.setFill(Color.web("#171717"));
            }
            gc.fillRect(0, currentY, w, trackH);

            // Song-Clip zeichnen, falls dieser Spur ein Song zugewiesen ist
            if (track.hasSong()) {
                drawSongClip(gc, track, currentY, trackH, w);
            }

            // Horizontale Trennlinie unten
            gc.setStroke(Color.web("#222222"));
            gc.setLineWidth(1.0);
            gc.strokeLine(0, currentY + trackH - 0.5, w, currentY + trackH - 0.5);

            currentY += trackH;
        }

        // Restfläche unterhalb der Spuren
        if (currentY < h) {
            gc.setFill(Color.web("#101010"));
            gc.fillRect(0, currentY, w, h - currentY);
        }

        // 2. Vertikale Takt- und Beat-Rasterlinien zeichnen
        double gridHeight = Math.max(h, currentY);
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

        // 3. Timeline-Linie (Playhead) zeichnen
        drawPlayhead(gc, gridHeight);
    }

    /**
     * Zeichnet einen Song-Clip Block auf einer Spur.
     */
    private void drawSongClip(GraphicsContext gc, TimelineTrack track, double y, double h, double maxW) {
        double clipX = 0;
        double clipW = Math.min(maxW - 20, 600.0); // Vorläufige Clip-Länge
        double clipH = h - 6;
        double clipY = y + 3;

        // Clip-Karten-Hintergrund (smaragdgrün/teal Studio-Theme)
        gc.setFill(Color.web("#064E3B"));
        gc.fillRoundRect(clipX + 2, clipY, clipW, clipH, 6, 6);

        gc.setStroke(Color.web("#10B981"));
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(clipX + 2, clipY, clipW, clipH, 6, 6);

        // Simulierte Wellenform im Clip
        gc.setStroke(Color.web("#34D399"));
        gc.setLineWidth(1.5);
        double waveCenterY = clipY + (clipH / 2.0);
        for (double wx = clipX + 10; wx < clipX + clipW - 10; wx += 6) {
            double amplitude = (Math.sin(wx * 0.15) * 0.5 + 0.5) * (clipH * 0.35);
            gc.strokeLine(wx, waveCenterY - amplitude, wx, waveCenterY + amplitude);
        }

        // Song-Titel Header
        gc.setFill(Color.web("#F3F4F6"));
        gc.fillText(track.getSongName(), clipX + 12, clipY + 14);
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

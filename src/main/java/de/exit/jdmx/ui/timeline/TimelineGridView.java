package de.exit.jdmx.ui.timeline;

import de.exit.jdmx.core.model.TimelineTrack;
import de.exit.jdmx.service.TimelineService;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Objects;

/**
 * Grid-Canvas für die Show-Timeline Playlist (FL Studio Raster-Stil).
 * <p>
 * Zeichnet:
 * <ul>
 *     <li>Horizontale Spur-Trennlinien exakt passend zur Höhe der linken Track-Boxen</li>
 *     <li>Vertikale Taktraster-Linien (Bars & Beats)</li>
 *     <li>Alternierende Zeilen-Hintergründe für optimale Lesbarkeit</li>
 * </ul>
 * Reagiert reaktiv auf Änderungen der Fenstergröße und der Track-Anzahl.
 * </p>
 */
public class TimelineGridView extends Pane {

    /** Raster-Breite pro Takt (Bar) in Pixeln. */
    public static final double BAR_WIDTH = 96.0;

    /** Raster-Unterteilung pro Viertel-Beat in Pixeln. */
    public static final double BEAT_WIDTH = BAR_WIDTH / 4.0;

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
    }

    /**
     * Zeichnet das Playlist-Raster auf die Canvas.
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
        int barIndex = 1;
        for (double x = 0; x < w; x += BEAT_WIDTH) {
            boolean isBarLine = Math.abs(x % BAR_WIDTH) < 0.1;

            if (isBarLine) {
                gc.setStroke(Color.web("#2A2A2A"));
                gc.setLineWidth(1.0);
                gc.strokeLine(x + 0.5, 0, x + 0.5, gridHeight);
                barIndex++;
            } else {
                gc.setStroke(Color.web("#1B1B1B"));
                gc.setLineWidth(0.5);
                gc.strokeLine(x + 0.5, 0, x + 0.5, gridHeight);
            }
        }
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

package de.exit.jdmx.ui.icon;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * Standardkonforme Vektor-Icons aus dem Lucide Icon Set (lucide.dev) für die JDMX Desktop-Suite.
 * <p>
 * Zeichnet Icons hochauflösend und verlustfrei über einen JavaFX {@link Canvas}.
 * Erfüllt ausnahmslos das strikte Emoji-Verbot in der gesamten Benutzeroberfläche.
 * Unterstützt farbige Presets und dynamische Skalierung.
 * </p>
 */
public class LucideIcon extends Canvas {

    /** Name des aktuellen Lucide-Icons. */
    private String name;

    /** Zeichenfarbe des Icons. */
    private Color color;

    /** Quadratische Grundgröße in Pixeln. */
    private final double size;

    /**
     * Erstellt ein neues Lucide-Vektor-Icon.
     *
     * @param name  Name des Icons (z. B. "power", "square", "sliders", "music")
     * @param size  Größe in Pixeln (Breite und Höhe)
     * @param color Linien- und Füllfarbe
     */
    public LucideIcon(String name, double size, Color color) {
        super(size, size);
        this.name = name != null ? name : "circle";
        this.size = size;
        this.color = color != null ? color : Color.WHITE;
        draw();
    }

    /**
     * Ändert Icon-Namen und Farbe und zeichnet das Icon neu.
     *
     * @param name  Neuer Icon-Name
     * @param color Neue Farbe
     */
    public void setIcon(String name, Color color) {
        this.name = name;
        this.color = color;
        draw();
    }

    /**
     * Ändert die Farbe des Icons und zeichnet es neu.
     *
     * @param color Neue Farbe
     */
    public void setColor(Color color) {
        this.color = color;
        draw();
    }

    /**
     * Zeichnet die Vektor-Pfade des Icons auf den internen Canvas.
     */
    public void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());

        double scale = size / 24.0;
        gc.setStroke(color);
        gc.setFill(color);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);
        gc.setLineWidth(2.0 * scale);

        switch (name.toLowerCase()) {
            case "power" -> {
                // Power / Standby symbol
                gc.strokeArc(4 * scale, 4 * scale, 16 * scale, 16 * scale, 130, 280, ArcType.OPEN);
                gc.strokeLine(12 * scale, 2 * scale, 12 * scale, 12 * scale);
            }
            case "square", "stop" -> {
                // Square / Stop
                gc.fillRect(5 * scale, 5 * scale, 14 * scale, 14 * scale);
                gc.strokeRect(5 * scale, 5 * scale, 14 * scale, 14 * scale);
            }
            case "play" -> {
                // Play Triangle
                double[] x = {7 * scale, 19 * scale, 7 * scale};
                double[] y = {4 * scale, 12 * scale, 20 * scale};
                gc.fillPolygon(x, y, 3);
                gc.strokePolygon(x, y, 3);
            }
            case "sliders" -> {
                // Fader / Sliders
                gc.strokeLine(6 * scale, 3 * scale, 6 * scale, 21 * scale);
                gc.strokeLine(12 * scale, 3 * scale, 12 * scale, 21 * scale);
                gc.strokeLine(18 * scale, 3 * scale, 18 * scale, 21 * scale);
                gc.strokeLine(3 * scale, 8 * scale, 9 * scale, 8 * scale);
                gc.strokeLine(9 * scale, 16 * scale, 15 * scale, 16 * scale);
                gc.strokeLine(15 * scale, 10 * scale, 21 * scale, 10 * scale);
            }
            case "box", "stage" -> {
                // 3D Box / Stage
                double[] px = {12 * scale, 21 * scale, 12 * scale, 3 * scale};
                double[] py = {3 * scale, 7.5 * scale, 12 * scale, 7.5 * scale};
                gc.strokePolygon(px, py, 4);
                gc.strokeLine(3 * scale, 7.5 * scale, 3 * scale, 16.5 * scale);
                gc.strokeLine(21 * scale, 7.5 * scale, 21 * scale, 16.5 * scale);
                gc.strokeLine(12 * scale, 12 * scale, 12 * scale, 21 * scale);
                gc.strokeLine(3 * scale, 16.5 * scale, 12 * scale, 21 * scale);
                gc.strokeLine(21 * scale, 16.5 * scale, 12 * scale, 21 * scale);
            }
            case "grid", "layout-grid" -> {
                // 4-Quadrant Grid
                gc.strokeRoundRect(3 * scale, 3 * scale, 8 * scale, 8 * scale, 2 * scale, 2 * scale);
                gc.strokeRoundRect(13 * scale, 3 * scale, 8 * scale, 8 * scale, 2 * scale, 2 * scale);
                gc.strokeRoundRect(3 * scale, 13 * scale, 8 * scale, 8 * scale, 2 * scale, 2 * scale);
                gc.strokeRoundRect(13 * scale, 13 * scale, 8 * scale, 8 * scale, 2 * scale, 2 * scale);
            }
            case "music", "audio-lines" -> {
                // Audio Wave Lines / Music
                gc.strokeLine(4 * scale, 10 * scale, 4 * scale, 14 * scale);
                gc.strokeLine(8 * scale, 6 * scale, 8 * scale, 18 * scale);
                gc.strokeLine(12 * scale, 3 * scale, 12 * scale, 21 * scale);
                gc.strokeLine(16 * scale, 7 * scale, 16 * scale, 17 * scale);
                gc.strokeLine(20 * scale, 10 * scale, 20 * scale, 14 * scale);
            }
            case "cable", "network" -> {
                // I/O Cable / Network Connector
                gc.strokeLine(4 * scale, 12 * scale, 8 * scale, 12 * scale);
                gc.strokeLine(16 * scale, 12 * scale, 20 * scale, 12 * scale);
                gc.strokeRoundRect(8 * scale, 7 * scale, 8 * scale, 10 * scale, 2 * scale, 2 * scale);
                gc.strokeLine(10 * scale, 10 * scale, 10 * scale, 14 * scale);
                gc.strokeLine(14 * scale, 10 * scale, 14 * scale, 14 * scale);
            }
            case "activity", "traffic" -> {
                // Heartbeat / Bus Activity Pulse
                double[] ptsX = {2, 6, 9, 14, 17, 22};
                double[] ptsY = {12, 12, 3, 21, 12, 12};
                for (int i = 0; i < ptsX.length - 1; i++) {
                    gc.strokeLine(ptsX[i] * scale, ptsY[i] * scale, ptsX[i + 1] * scale, ptsY[i + 1] * scale);
                }
            }
            case "x" -> {
                // Close / Delete X
                gc.strokeLine(6 * scale, 6 * scale, 18 * scale, 18 * scale);
                gc.strokeLine(18 * scale, 6 * scale, 6 * scale, 18 * scale);
            }
            case "plus" -> {
                // Plus Symbol
                gc.strokeLine(12 * scale, 5 * scale, 12 * scale, 19 * scale);
                gc.strokeLine(5 * scale, 12 * scale, 19 * scale, 12 * scale);
            }
            case "circle" -> {
                // Dot
                gc.fillOval(7 * scale, 7 * scale, 10 * scale, 10 * scale);
            }
            default -> {
                // Fallback Circle
                gc.strokeOval(4 * scale, 4 * scale, 16 * scale, 16 * scale);
            }
        }
    }
}

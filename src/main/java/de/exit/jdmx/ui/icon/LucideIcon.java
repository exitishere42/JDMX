package de.exit.jdmx.ui.icon;

import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * Standardkonforme Vektor-Icons aus dem Lucide Icon Set (lucide.dev) basierend auf JavaFX {@link SVGPath}.
 * <p>
 * Verwendet die exakten mathematischen SVG-Pfade von Lucide.
 * Strikte Einhaltung der Zero-Emoji-Regel. Unterstützt stufenlose Farb- und Größenanpassung.
 * </p>
 */
public class LucideIcon extends StackPane {

    /** Der primäre SVG-Pfad des Icons. */
    private final SVGPath svgPath;

    /** Name des aktuellen Lucide-Icons. */
    private String name;

    /** Gewünschte Pixelgröße (Breite und Höhe). */
    private final double size;

    /**
     * Erstellt ein neues Lucide-Vektor-Icon auf Basis von SVGPath.
     *
     * @param name  Name des Icons (z. B. "power", "square", "sliders", "box")
     * @param size  Größe in Pixeln
     * @param color Vektor-Farbe
     */
    public LucideIcon(String name, double size, Color color) {
        this.name = name != null ? name.toLowerCase() : "circle";
        this.size = size;

        setAlignment(Pos.CENTER);
        setMinSize(size, size);
        setPrefSize(size, size);
        setMaxSize(size, size);

        this.svgPath = new SVGPath();
        svgPath.setStrokeLineCap(StrokeLineCap.ROUND);
        svgPath.setStrokeLineJoin(StrokeLineJoin.ROUND);

        updateSvg(color);
        getChildren().add(svgPath);
    }

    /**
     * Ändert die Farbe des Icons.
     *
     * @param color Neue Farbe
     */
    public void setColor(Color color) {
        if (isFilledIcon(name)) {
            svgPath.setFill(color);
            svgPath.setStroke(color);
        } else {
            svgPath.setFill(Color.TRANSPARENT);
            svgPath.setStroke(color);
        }
    }

    /**
     * Ändert Icon-Name und Farbe.
     *
     * @param name  Neuer Icon-Name
     * @param color Neue Farbe
     */
    public void setIcon(String name, Color color) {
        this.name = name != null ? name.toLowerCase() : "circle";
        updateSvg(color);
    }

    /**
     * Aktualisiert den SVG-Pfad und die Skalierung entsprechend des Icon-Namens.
     */
    private void updateSvg(Color color) {
        String pathData = getSvgPathData(name);
        svgPath.setContent(pathData);

        double scale = size / 24.0;
        svgPath.setScaleX(scale);
        svgPath.setScaleY(scale);
        svgPath.setStrokeWidth(2.0);

        if (isFilledIcon(name)) {
            svgPath.setFill(color);
            svgPath.setStroke(color);
        } else {
            svgPath.setFill(Color.TRANSPARENT);
            svgPath.setStroke(color);
        }
    }

    /**
     * Prüft, ob ein Icon standardmäßig solide gefüllt sein soll.
     */
    private boolean isFilledIcon(String iconName) {
        return "square".equals(iconName) || "stop".equals(iconName) || "play".equals(iconName);
    }

    /**
     * Liefert die exakten Lucide SVG Path Data Strings (24x24 ViewBox).
     *
     * @param iconName Name des Lucide-Icons
     * @return SVG-Pfad-Daten
     */
    public static String getSvgPathData(String iconName) {
        return switch (iconName) {
            case "power" ->
                // Lucide power: circle arc + vertical line
                "M12 2v10 M18.36 6.64a9 9 0 1 1-12.73 0";
            case "square", "stop" ->
                // Lucide square
                "M3 5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z";
            case "play" ->
                // Lucide play
                "M6 4l15 8-15 8z";
            case "box", "stage" ->
                // Lucide box / isometric stage
                "M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z " +
                "M3.27 6.96 12 12.01l8.73-5.05 M12 22.08V12";
            case "sliders" ->
                // Lucide sliders
                "M4 21v-7 M4 10V3 M12 21v-9 M12 8V3 M20 21v-5 M20 12V3 M1 14h6 M9 8h6 M17 16h6";
            case "grid", "layout-grid" ->
                // Lucide layout-grid
                "M3 3h7v7H3z M14 3h7v7h-7z M14 14h7v7h-7z M3 14h7v7H3z";
            case "audio-lines", "music" ->
                // Lucide audio-lines
                "M2 10v3 M6 6v11 M10 3v18 M14 8v7 M18 5v13 M22 10v3";
            case "arrow-right-left", "arrows-horizontal", "io" ->
                // Lucide arrow-right-left
                "M16 3l4 4-4 4 M20 7H4 M8 21l-4-4 4-4 M4 17h16";
            case "cable", "network" ->
                // Lucide cable
                "M4 12v6 M16 12v6 M8 12v2 M12 12v2 M3 18h18 M18 6h-4a2 2 0 0 0-2 2v4 M6 6h4a2 2 0 0 1 2 2v4";
            case "activity", "traffic" ->
                // Lucide activity pulse
                "M22 12h-4l-3 9L9 3l-3 9H2";
            case "x" ->
                // Lucide x
                "M18 6 6 18 M6 6l12 12";
            case "plus" ->
                // Lucide plus
                "M5 12h14 M12 5v14";
            case "lightbulb", "lamp" ->
                // Lucide lightbulb
                "M15 14c.2-1 .7-1.7 1.5-2.5 1-.9 1.5-2.2 1.5-3.5A6 6 0 0 0 6 8c0 1 .2 2.2 1.5 3.5.7.7 1.3 1.5 1.5 2.5 M9 18h6 M10 22h4";
            case "star" ->
                // Lucide star
                "M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z";
            default ->
                // Lucide circle
                "M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2z";
        };
    }
}

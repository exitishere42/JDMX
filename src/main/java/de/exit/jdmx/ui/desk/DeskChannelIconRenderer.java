package de.exit.jdmx.ui.desk;

import de.exit.jdmx.core.model.ChannelType;
import de.exit.jdmx.ui.icon.LucideIcon;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Performanter Vektor-Renderer für offizielle Lucide.dev DMX-Kanal-Funktions-Icons im LightDesk Canvas.
 * <p>
 * Nutzt die exakten mathematischen SVG-Pfade von lucide.dev aus {@link LucideIcon}:
 * <ul>
 *     <li><b>DIMMER:</b> Offizielles Lucide {@code lightbulb} in Warm-Amber ({@code #F59E0B}).</li>
 *     <li><b>FARBKANÄLE (RGBW, Amber, UV):</b> Scharfkantig gefülltes Lucide {@code circle} mit Glow-Kontur.</li>
 *     <li><b>COLOR_WHEEL:</b> Offizielles Lucide {@code palette}.</li>
 *     <li><b>PAN / PAN_FINE:</b> Offizielles Lucide {@code arrow-left-right} (Motion Cyan).</li>
 *     <li><b>TILT / TILT_FINE:</b> Offizielles Lucide {@code arrow-up-down} (Motion Cyan).</li>
 *     <li><b>GOBO:</b> Offizielles Lucide {@code disc} (Silberweiß).</li>
 *     <li><b>PRISM:</b> Offizielles Lucide {@code gem} (Prism-Teal).</li>
 *     <li><b>SHUTTER:</b> Offizielles Lucide {@code zap} (Warngelb).</li>
 *     <li><b>SPEED:</b> Offizielles Lucide {@code gauge} (Mintgrün).</li>
 *     <li><b>EFFECT:</b> Offizielles Lucide {@code sparkles} (Magenta).</li>
 *     <li><b>MAINTENANCE:</b> Offizielles Lucide {@code wrench} (Grau).</li>
 *     <li><b>GENERIC:</b> Offizielles Lucide {@code sliders} (Studio-Primärgrau).</li>
 * </ul>
 * <b>Zero-Lag & Zero-Emoji:</b> 100 % reine Vektorgrafiken via {@link GraphicsContext#appendSVGPath(String)}.
 * </p>
 */
public final class DeskChannelIconRenderer {

    private DeskChannelIconRenderer() {
        // Utility-Klasse
    }

    /**
     * Zeichnet das zur Funktion passende offizielle Lucide.dev Vektor-Icon zentriert an die angegebene Koordinate.
     *
     * @param gc            Der 2D-Grafikkontext des Canvas (darf nicht null sein)
     * @param centerX       Horizontale Mitte des Icons
     * @param centerY       Vertikale Mitte des Icons
     * @param size          Kantenlänge der Icon-Bounding-Box (typischerweise 18 bis 24 px)
     * @param type          Der DMX-Kanaltyp
     * @param fallbackColor Fallback-Farbe
     */
    public static void renderIcon(GraphicsContext gc, double centerX, double centerY, double size, ChannelType type, Color fallbackColor) {
        if (gc == null || type == null) {
            return;
        }

        String svgPath = getLucideSvg(type);
        Color color = resolveIconColor(type, fallbackColor);
        boolean isFill = isFilled(type);

        gc.save();
        try {
            double scale = size / 24.0;
            gc.translate(centerX - (size / 2.0), centerY - (size / 2.0));
            gc.scale(scale, scale);

            gc.beginPath();
            gc.appendSVGPath(svgPath);

            gc.setLineCap(StrokeLineCap.ROUND);
            gc.setLineJoin(StrokeLineJoin.ROUND);

            if (isFill) {
                gc.setFill(color);
                gc.fill();
                // Subtile 1px Begrenzungskontur
                gc.setStroke(type == ChannelType.WHITE ? Color.web("#9CA3AF") : Color.web("#FFFFFF", 0.4));
                gc.setLineWidth(1.2 / scale);
                gc.stroke();
            } else {
                gc.setStroke(color);
                gc.setLineWidth(1.9 / scale);
                gc.stroke();
            }

            // Fein-Kennzeichnung für 16-Bit PAN/TILT
            if (type == ChannelType.PAN_FINE || type == ChannelType.TILT_FINE) {
                gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 8.5 / scale));
                gc.setFill(Color.WHITE);
                gc.setTextAlign(TextAlignment.CENTER);
                gc.fillText("F", 12, 15);
            }
        } finally {
            gc.restore();
        }
    }

    /**
     * Liefert den passenden Lucide SVG-Pfad für den jeweiligen DMX-Kanaltyp.
     *
     * @param type Kanaltyp
     * @return Exakter SVG-Pfad aus Lucide.dev
     */
    public static String getLucideSvg(ChannelType type) {
        if (type == null) {
            return LucideIcon.getSvgPathData("circle");
        }
        return switch (type) {
            case DIMMER -> LucideIcon.getSvgPathData("lightbulb");
            case RED, GREEN, BLUE, WHITE, AMBER, UV -> LucideIcon.getSvgPathData("circle");
            case COLOR_WHEEL -> LucideIcon.getSvgPathData("palette");
            case PAN, PAN_FINE -> LucideIcon.getSvgPathData("arrow-left-right");
            case TILT, TILT_FINE -> LucideIcon.getSvgPathData("arrow-up-down");
            case GOBO -> LucideIcon.getSvgPathData("disc");
            case PRISM -> LucideIcon.getSvgPathData("gem");
            case SHUTTER -> LucideIcon.getSvgPathData("zap");
            case SPEED -> LucideIcon.getSvgPathData("gauge");
            case EFFECT -> LucideIcon.getSvgPathData("sparkles");
            case MAINTENANCE -> LucideIcon.getSvgPathData("wrench");
            case GENERIC -> LucideIcon.getSvgPathData("sliders");
        };
    }

    /**
     * Bestimmt, ob das Icon flächig gefüllt werden soll (z. B. Farbkreise).
     *
     * @param type Kanaltyp
     * @return true wenn gefüllt
     */
    public static boolean isFilled(ChannelType type) {
        if (type == null) return false;
        return switch (type) {
            case RED, GREEN, BLUE, WHITE, AMBER, UV -> true;
            default -> false;
        };
    }

    /**
     * Ermittelt die primäre Signalfarbe für den jeweiligen Kanaltyp.
     *
     * @param type          Kanaltyp
     * @param fallbackColor Fallback-Farbe
     * @return Berechnete JavaFX-Farbe
     */
    public static Color resolveIconColor(ChannelType type, Color fallbackColor) {
        if (type == null) {
            return fallbackColor != null ? fallbackColor : Color.WHITE;
        }
        return switch (type) {
            case DIMMER -> Color.web("#F59E0B"); // Pult-Orange / Warm-Amber
            case RED -> Color.web("#EF4444");    // Signalrot
            case GREEN -> Color.web("#10B981");  // Smaragdgrün
            case BLUE -> Color.web("#3B82F6");   // Sattes Blau
            case WHITE -> Color.web("#F3F4F6");  // Kaltweiß
            case AMBER -> Color.web("#F59E0B");  // Bernstein
            case UV -> Color.web("#A855F7");     // Schwarzlicht-Violett
            case COLOR_WHEEL -> Color.web("#06B6D4"); // Cyan / Türkis
            case PAN, PAN_FINE, TILT, TILT_FINE -> Color.web("#38BDF8"); // Motion Cyan
            case GOBO -> Color.web("#E5E7EB");   // Helles Silber
            case PRISM -> Color.web("#2DD4BF");  // Prism-Teal
            case SHUTTER -> Color.web("#FACC15");// Warngelb
            case SPEED -> Color.web("#34D399");  // Mintgrün
            case EFFECT -> Color.web("#EC4899"); // Magenta FX
            case MAINTENANCE -> Color.web("#9CA3AF"); // Grau
            case GENERIC -> fallbackColor != null ? fallbackColor : Color.web("#D1D5DB");
        };
    }
}

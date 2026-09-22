package de.exit.jdmx.ui.icons;

import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

/**
 * Lucide SVG Icons factory for JavaFX.
 * Scalable vector icons using artnet2dmx Material Dark theme colors.
 */
public final class LucideIcon {

    /** Primary Teal 200 accent color. */
    public static final Color COLOR_PRIMARY = Color.web("#03DAC6");

    /** Hover Cyan color for interactive feedback. */
    public static final Color COLOR_HOVER = Color.web("#00E5FF");

    /** Pressed Neon Mint color for physical click feedback. */
    public static final Color COLOR_PRESSED = Color.web("#00FF9A");

    /** Material Red error/stop color. */
    public static final Color COLOR_ERROR = Color.web("#CF6679");

    /** Warning Amber color. */
    public static final Color COLOR_WARNING = Color.web("#FF9F0A");

    /** Neutral Drag Handle & muted icon color. */
    public static final Color COLOR_HANDLE = Color.web("#8E8E8E");

    /** High emphasis text color (87%). */
    public static final Color COLOR_TEXT = Color.web("#E1E1E1");

    /** Medium emphasis text color (60%). */
    public static final Color COLOR_MUTED = Color.web("#9E9E9E");

    /** Disabled text color (38%). */
    public static final Color COLOR_DISABLED = Color.web("#616161");

    private LucideIcon() {}

    // SVG Path Strings for standard Lucide Icons
    public static final String STAR = "M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z";
    public static final String POWER = "M18.36 6.64a9 9 0 1 1-12.73 0M12 2v10";
    public static final String SQUARE_X = "M3 3h18v18H3zM9 9l6 6M15 9l-6 6";
    public static final String WRENCH = "M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z";
    public static final String BOX = "M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z M3.3 7l8.7 5 8.7-5 M12 22V12";
    public static final String SLIDERS = "M4 21v-7 M4 10V3 M12 21v-9 M12 8V3 M20 21v-5 M20 12V3 M1 14h6 M9 8h6 M17 16h6";
    public static final String GRID = "M3 3h7v7H3z M14 3h7v7h-7z M14 14h7v7h-7z M3 14h7v7H3z";
    public static final String AUDIO_LINES = "M2 10v3 M6 6v11 M10 3v18 M14 8v7 M18 5v13 M22 10v3";
    public static final String CABLE = "M4 9a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v4a6 6 0 0 1-6 6h-1 M8 7V3 M16 7V3 M20 9a2 2 0 0 0-2-2h-2a2 2 0 0 0-2 2v4a6 6 0 0 0 6 6h1 M12 17v4";
    public static final String EDIT = "M12 20h9 M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z";
    public static final String TOGGLE = "M16 5H8a7 7 0 0 0 0 14h8a7 7 0 0 0 0-14z M8 15a3 3 0 1 1 0-6 3 3 0 0 1 0 6z";
    public static final String PALETTE = "M12 2a10 10 0 0 0-9.2 14.1A6 6 0 0 1 8 16h1a4 4 0 0 0 4-4v-.5a2.5 2.5 0 0 1 5 0 4.5 4.5 0 0 1-4.5 4.5H13a2 2 0 0 0-2 2c0 .6.2 1.1.6 1.5A10 10 0 0 0 12 2z";
    public static final String ERASER = "m7 21-4.3-4.3c-1-1-1-2.5 0-3.4l9.6-9.6c1-1 2.5-1 3.4 0l5.6 5.6c1 1 1 2.5 0 3.4L13 21 M22 21H7 M5 11l9 9";
    public static final String CHEVRON_LEFT = "M15 18l-6-6 6-6";
    public static final String CHEVRON_RIGHT = "M9 18l6-6-6-6";
    public static final String PLAY = "M5 3l14 9-14 9V3z";
    public static final String SQUARE = "M3 3h18v18H3z";
    public static final String REPEAT = "M17 2l4 4-4 4 M3 11v-1a4 4 0 0 1 4-4h14 M7 22l-4-4 4-4 M21 13v1a4 4 0 0 1-4 4H3";
    public static final String VOLUME_2 = "M11 5L6 9H2v6h4l5 4V5z M15.54 8.46a5 5 0 0 1 0 7.07 M19.07 4.93a10 10 0 0 1 0 14.14";
    public static final String SETTINGS = "M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6z";
    public static final String MONITOR = "M2 3h20a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z M8 21h8 M12 17v4";
    public static final String USB = "M12 2v6 M12 16v6 M12 22a2 2 0 0 1-2-2h4a2 2 0 0 1-2 2z M12 2l-3 4h6z M6.5 13H5a2 2 0 0 1-2-2V7 M17.5 13H19a2 2 0 0 0 2-2V7 M3 7h4v4H3z M17 7h4v4h-4z";
    public static final String MUSIC = "M9 18V5l12-2v13 M6 15a3 3 0 1 0 0 6 3 3 0 0 0 0-6z M18 13a3 3 0 1 0 0 6 3 3 0 0 0 0-6z";
    public static final String ACTIVITY = "M22 12h-4l-3 9L9 3l-3 9H2";
    public static final String ZAP = "M13 2L3 14h9l-1 8 10-12h-9l1-8z";

    public static SVGPath create(String pathData, Color color, double scale) {
        SVGPath svg = new SVGPath();
        svg.setContent(pathData);
        svg.setFill(color);
        svg.setStroke(color);
        svg.setStrokeWidth(1.2);
        svg.setScaleX(scale);
        svg.setScaleY(scale);
        return svg;
    }

    public static SVGPath createOutline(String pathData, Color strokeColor, double scale) {
        SVGPath svg = new SVGPath();
        svg.setContent(pathData);
        svg.setFill(Color.TRANSPARENT);
        svg.setStroke(strokeColor);
        svg.setStrokeWidth(1.5);
        svg.setScaleX(scale);
        svg.setScaleY(scale);
        return svg;
    }

    public static SVGPath createStar(Color color, double scale) {
        SVGPath svg = new SVGPath();
        svg.setContent(STAR);
        svg.setFill(color);
        svg.setStroke(color);
        svg.setStrokeWidth(0.5);
        svg.setScaleX(scale);
        svg.setScaleY(scale);
        return svg;
    }
}

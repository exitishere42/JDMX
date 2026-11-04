package de.exit.jdmx.ui.theme;

import javafx.scene.paint.Color;

/**
 * Farb- und Design-Tokens für das moderne Studio Dark Theme der JDMX Lichtsteuerungs-Suite.
 * <p>
 * Bietet konsistente Kontraste, blendfreie Dunkelstufen für Live-Bühnenumgebungen
 * und signalstarke Farben für Not-Aus, Blackout und Statusanzeigen.
 * </p>
 */
public final class StudioTheme {

    private StudioTheme() {
        // Utility-Klasse ohne Instanziierung
    }

    // --- Dunkelstufen & Oberflächen ---
    public static final String HEX_BG_BASE = "#121212";
    public static final String HEX_SURFACE_1 = "#1A1A1A";
    public static final String HEX_SURFACE_2 = "#242424";
    public static final String HEX_SURFACE_3 = "#2D2D2D";
    public static final String HEX_BORDER = "#383838";

    // --- Textfarben ---
    public static final String HEX_TEXT_HIGH = "#FFFFFF";
    public static final String HEX_TEXT_MED = "#B3B3B3";
    public static final String HEX_TEXT_LOW = "#737373";

    // --- Akzent- und Signalfarben ---
    public static final String HEX_CYAN_ACCENT = "#00E5FF";
    public static final String HEX_RED_STOP = "#EF4444";
    public static final String HEX_GREEN_ACTIVE = "#10B981";
    public static final String HEX_AMBER_WARN = "#F59E0B";

    // --- JavaFX Color-Instanzen für Vektor-Icons & Canvas ---
    public static final Color COLOR_TEXT_HIGH = Color.web(HEX_TEXT_HIGH);
    public static final Color COLOR_TEXT_MED = Color.web(HEX_TEXT_MED);
    public static final Color COLOR_CYAN = Color.web(HEX_CYAN_ACCENT);
    public static final Color COLOR_RED = Color.web(HEX_RED_STOP);
    public static final Color COLOR_GREEN = Color.web(HEX_GREEN_ACTIVE);
    public static final Color COLOR_AMBER = Color.web(HEX_AMBER_WARN);
}

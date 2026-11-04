package de.exit.jdmx.ui.theme;

import javafx.scene.paint.Color;

/**
 * Farb- und Design-Tokens für das Studio Dark Theme von JDMX.
 * <p>
 * Primärfarbe ist ein dezentes, unaufdringliches Hellgrau.
 * Die visuelle Orientierung erfolgt über bunte, standardkonforme Lucide-Vektor-Icons.
 * </p>
 */
public final class StudioTheme {

    private StudioTheme() {
        // Utility-Klasse
    }

    // --- Dunkelstufen & Oberflächen ---
    public static final String HEX_BG_BASE = "#121212";
    public static final String HEX_SURFACE_1 = "#1A1A1A";
    public static final String HEX_SURFACE_2 = "#242424";
    public static final String HEX_SURFACE_3 = "#2D2D2D";
    public static final String HEX_BORDER = "#383838";

    // --- Text- & Dezent-Farben (Primärfarbe ist Hellgrau) ---
    public static final String HEX_PRIMARY_GRAY = "#D1D5DB";
    public static final String HEX_TEXT_HIGH = "#F3F4F6";
    public static final String HEX_TEXT_MED = "#9CA3AF";
    public static final String HEX_TEXT_LOW = "#6B7280";

    // --- Brand Logo Farbe (Warmes Amber-Gold) ---
    public static final String HEX_BRAND_LOGO = "#FEDC94";
    public static final Color COLOR_BRAND_LOGO = Color.web(HEX_BRAND_LOGO);

    // --- Bunte Signalfarben für Icons ---
    public static final String HEX_ICON_STAGE = "#38BDF8";     // Cyan / Helles Blau
    public static final String HEX_ICON_FADER = "#F59E0B";     // Warm Amber / Orange
    public static final String HEX_ICON_MATRIX = "#A855F7";    // Violett / Purple
    public static final String HEX_ICON_TIMELINE = "#10B981";  // Smaragd-Grün
    public static final String HEX_ICON_IO = "#2DD4BF";        // Türkis / Teal
    public static final String HEX_ICON_RED = "#EF4444";       // Signalrot für Blackout & STOP
    public static final String HEX_ICON_GREEN = "#10B981";     // Grün für DMX Traffic

    // --- Action Dropdown Menü Farben ---
    public static final String HEX_MENU_NEW = "#38BDF8";       // Cyan
    public static final String HEX_MENU_OPEN = "#F59E0B";      // Warm Amber
    public static final String HEX_MENU_SAVE = "#10B981";      // Emerald Green
    public static final String HEX_MENU_SAVE_AS = "#2DD4BF";   // Teal
    public static final String HEX_MENU_UNDO = "#A855F7";      // Purple
    public static final String HEX_MENU_REDO = "#38BDF8";      // Cyan
    public static final String HEX_MENU_SETTINGS = "#FB923C";  // Orange
    public static final String HEX_MENU_FULLSCREEN = "#D1D5DB";// Light Gray

    // --- JavaFX Color Instanzen ---
    public static final Color COLOR_PRIMARY_GRAY = Color.web(HEX_PRIMARY_GRAY);
    public static final Color COLOR_TEXT_HIGH = Color.web(HEX_TEXT_HIGH);
    public static final Color COLOR_TEXT_MED = Color.web(HEX_TEXT_MED);
    public static final Color COLOR_TEXT_LOW = Color.web(HEX_TEXT_LOW);

    public static final Color COLOR_ICON_STAGE = Color.web(HEX_ICON_STAGE);
    public static final Color COLOR_ICON_FADER = Color.web(HEX_ICON_FADER);
    public static final Color COLOR_ICON_MATRIX = Color.web(HEX_ICON_MATRIX);
    public static final Color COLOR_ICON_TIMELINE = Color.web(HEX_ICON_TIMELINE);
    public static final Color COLOR_ICON_IO = Color.web(HEX_ICON_IO);
    public static final Color COLOR_ICON_RED = Color.web(HEX_ICON_RED);
    public static final Color COLOR_ICON_GREEN = Color.web(HEX_ICON_GREEN);

    public static final Color COLOR_MENU_NEW = Color.web(HEX_MENU_NEW);
    public static final Color COLOR_MENU_OPEN = Color.web(HEX_MENU_OPEN);
    public static final Color COLOR_MENU_SAVE = Color.web(HEX_MENU_SAVE);
    public static final Color COLOR_MENU_SAVE_AS = Color.web(HEX_MENU_SAVE_AS);
    public static final Color COLOR_MENU_UNDO = Color.web(HEX_MENU_UNDO);
    public static final Color COLOR_MENU_REDO = Color.web(HEX_MENU_REDO);
    public static final Color COLOR_MENU_SETTINGS = Color.web(HEX_MENU_SETTINGS);
    public static final Color COLOR_MENU_FULLSCREEN = Color.web(HEX_MENU_FULLSCREEN);
}

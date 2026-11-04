package de.exit.jdmx.service;

/**
 * Aktionen, die über Tastaturkürzel (Shortcuts) ausgelöst werden können.
 */
public enum ShortcutAction {
    NEW_PROJECT("Neues Projekt", "file-plus"),
    OPEN_PROJECT("Projekt öffnen", "folder-open"),
    SAVE_PROJECT("Projekt speichern", "save"),
    SAVE_AS_PROJECT("Speichern unter...", "file-pen"),
    UNDO("Rückgängig", "undo-2"),
    REDO("Wiederholen", "redo-2"),
    FULLSCREEN("Vollbild umschalten", "maximize"),
    TAB_STAGE("Tab: Stage & Fixtures", "stage"),
    TAB_DESK("Tab: Simple Desk", "sliders"),
    TAB_MATRIX("Tab: Button Matrix", "layout-grid"),
    TAB_TIMELINE("Tab: Show-Timeline", "audio-lines"),
    TAB_IO("Tab: Input/Output", "arrow-right-left");

    private final String label;
    private final String iconName;

    ShortcutAction(String label, String iconName) {
        this.label = label;
        this.iconName = iconName;
    }

    public String getLabel() {
        return label;
    }

    public String getIconName() {
        return iconName;
    }
}

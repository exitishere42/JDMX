package de.exit.jdmx.service;

import javafx.scene.input.KeyCombination;

/**
 * Repräsentiert eine konfigurierbare Shortcut-Belegung.
 *
 * @param action             Die zugehörige Aktion
 * @param keyCombination     Die aktuell zugewiesene Tastenkombination
 * @param defaultCombination Die werksseitige Standard-Tastenkombination
 */
public record ShortcutItem(
        ShortcutAction action,
        KeyCombination keyCombination,
        KeyCombination defaultCombination
) {
    /**
     * Prüft, ob die Belegung vom Standard abweicht.
     */
    public boolean isCustomized() {
        return !keyCombination.equals(defaultCombination);
    }
}

package de.exit.jdmx.service;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zentraler Service für das Verwalten, Anpassen und Auflösen von Tastaturkürzeln.
 * <p>
 * Reines MVC-Modell: Keine UI-Komponenten, 100% thread-sicher und testbar.
 * </p>
 */
public class ShortcutService {

    /** Map der aktuellen Belegungen pro Aktion. */
    private final Map<ShortcutAction, ShortcutItem> shortcuts = new LinkedHashMap<>();

    /**
     * Erstellt den Shortcut-Service und lädt die werksseitigen Standard-Belegungen.
     */
    public ShortcutService() {
        initDefaults();
    }

    /**
     * Initialisiert alle Standard-Tastenkombinationen.
     */
    private void initDefaults() {
        registerDefault(ShortcutAction.NEW_PROJECT, new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
        registerDefault(ShortcutAction.OPEN_PROJECT, new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        registerDefault(ShortcutAction.SAVE_PROJECT, new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        registerDefault(ShortcutAction.SAVE_AS_PROJECT, new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        registerDefault(ShortcutAction.UNDO, new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN));
        registerDefault(ShortcutAction.REDO, new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN));
        registerDefault(ShortcutAction.FULLSCREEN, new KeyCodeCombination(KeyCode.F11));

        registerDefault(ShortcutAction.TAB_STAGE, new KeyCodeCombination(KeyCode.DIGIT1));
        registerDefault(ShortcutAction.TAB_DESK, new KeyCodeCombination(KeyCode.DIGIT2));
        registerDefault(ShortcutAction.TAB_MATRIX, new KeyCodeCombination(KeyCode.DIGIT3));
        registerDefault(ShortcutAction.TAB_TIMELINE, new KeyCodeCombination(KeyCode.DIGIT4));
        registerDefault(ShortcutAction.TAB_IO, new KeyCodeCombination(KeyCode.DIGIT5));
    }

    private void registerDefault(ShortcutAction action, KeyCombination defaultComb) {
        shortcuts.put(action, new ShortcutItem(action, defaultComb, defaultComb));
    }

    /**
     * Liefert alle konfigurierten Shortcuts.
     */
    public List<ShortcutItem> getAllShortcuts() {
        return new ArrayList<>(shortcuts.values());
    }

    /**
     * Liefert den Shortcut für eine bestimmte Aktion.
     */
    public ShortcutItem getShortcut(ShortcutAction action) {
        return shortcuts.get(action);
    }

    /**
     * Passt die Tastenkombination einer Aktion an.
     *
     * @param action      Die anzupassende Aktion
     * @param combination Die neue Tastenkombination
     */
    public void setShortcut(ShortcutAction action, KeyCombination combination) {
        ShortcutItem existing = shortcuts.get(action);
        if (existing != null) {
            shortcuts.put(action, new ShortcutItem(action, combination, existing.defaultCombination()));
        }
    }

    /**
     * Setzt einen Shortcut auf seinen Standardwert zurück.
     *
     * @param action Die zurückzusetzende Aktion
     */
    public void resetShortcut(ShortcutAction action) {
        ShortcutItem existing = shortcuts.get(action);
        if (existing != null) {
            shortcuts.put(action, new ShortcutItem(action, existing.defaultCombination(), existing.defaultCombination()));
        }
    }

    /**
     * Exportiert alle konfigurierten Shortcuts in eine Map (Aktionsname -> KeyCombination String).
     */
    public Map<String, String> exportShortcuts() {
        Map<String, String> map = new LinkedHashMap<>();
        for (Map.Entry<ShortcutAction, ShortcutItem> entry : shortcuts.entrySet()) {
            KeyCombination comb = entry.getValue().keyCombination();
            if (comb != null) {
                map.put(entry.getKey().name(), comb.getName());
            }
        }
        return map;
    }

    /**
     * Importiert Shortcuts aus einer Map. Ungültige Belegungen werden übersprungen.
     *
     * @param map Map von Aktionsname auf Tastenkombinations-String
     */
    public void importShortcuts(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : map.entrySet()) {
            try {
                ShortcutAction action = ShortcutAction.valueOf(entry.getKey());
                KeyCombination comb = KeyCombination.valueOf(entry.getValue());
                setShortcut(action, comb);
            } catch (Exception ignored) {
                // Bei ungültigen Einträgen überspringen
            }
        }
    }

    /**
     * Setzt alle Shortcuts auf ihre Standardwerte zurück.
     */
    public void resetAll() {
        initDefaults();
    }

    /**
     * Prüft, ob eine Tastenkombination bereits von einer anderen Aktion verwendet wird.
     *
     * @param targetAction Die zu belegende Aktion
     * @param combination  Die gewünschte Tastenkombination
     * @return Die kollidierende Aktion oder {@code null}, falls frei
     */
    public ShortcutAction findCollision(ShortcutAction targetAction, KeyCombination combination) {
        if (combination == null) {
            return null;
        }
        for (ShortcutItem item : shortcuts.values()) {
            if (item.action() != targetAction && combination.equals(item.keyCombination())) {
                return item.action();
            }
        }
        return null;
    }

    /**
     * Formatiert eine KeyCombination in einen ansprechenden Anzeige-Text (z. B. "Strg + S").
     *
     * @param action Die Aktion
     * @return Lesbarer Text
     */
    public String getDisplayText(ShortcutAction action) {
        ShortcutItem item = shortcuts.get(action);
        if (item == null || item.keyCombination() == null) {
            return "-";
        }
        return formatKeyCombination(item.keyCombination());
    }

    /**
     * Erzeugt eine lesbare Darstellung einer Tastenkombination.
     */
    public static String formatKeyCombination(KeyCombination comb) {
        if (comb instanceof KeyCodeCombination kcc) {
            StringBuilder sb = new StringBuilder();
            if (kcc.getControl() == KeyCombination.ModifierValue.DOWN) {
                sb.append("Strg + ");
            }
            if (kcc.getAlt() == KeyCombination.ModifierValue.DOWN) {
                sb.append("Alt + ");
            }
            if (kcc.getShift() == KeyCombination.ModifierValue.DOWN) {
                sb.append("Umschalt + ");
            }
            if (kcc.getMeta() == KeyCombination.ModifierValue.DOWN) {
                sb.append("Cmd + ");
            }

            KeyCode code = kcc.getCode();
            String name = switch (code) {
                case DIGIT1 -> "1";
                case DIGIT2 -> "2";
                case DIGIT3 -> "3";
                case DIGIT4 -> "4";
                case DIGIT5 -> "5";
                case DIGIT6 -> "6";
                case DIGIT7 -> "7";
                case DIGIT8 -> "8";
                case DIGIT9 -> "9";
                case DIGIT0 -> "0";
                default -> code.getName();
            };
            sb.append(name);
            return sb.toString();
        }
        return comb.getDisplayText();
    }

    /**
     * Prüft, ob ein Tastatur-Event der konfigurierten Tastenkombination einer Aktion entspricht.
     * Unterstützt für Tab-Wechsel (1-5) automatisch auch das Numpad (NumPad 1-5).
     *
     * @param action Die Aktion
     * @param event  Das KeyEvent
     * @return {@code true}, wenn das Event übereinstimmt
     */
    public boolean matches(ShortcutAction action, KeyEvent event) {
        if (action == null || event == null) {
            return false;
        }
        ShortcutItem item = shortcuts.get(action);
        if (item == null || item.keyCombination() == null) {
            return false;
        }

        // Direkter KeyCombination Match
        if (item.keyCombination().match(event)) {
            return true;
        }

        // Strg+Umschalt+Z als alternativer Redo-Shortcut
        if (action == ShortcutAction.REDO && event.isControlDown() && event.isShiftDown() && event.getCode() == KeyCode.Z) {
            return true;
        }

        // Optionale Numpad-Unterstützung für Tab 1 bis 5 (wenn keine Modifier aktiv sind)
        if (!event.isControlDown() && !event.isAltDown() && !event.isShiftDown() && !event.isMetaDown()) {
            KeyCode code = event.getCode();
            return switch (action) {
                case TAB_STAGE -> code == KeyCode.NUMPAD1;
                case TAB_DESK -> code == KeyCode.NUMPAD2;
                case TAB_MATRIX -> code == KeyCode.NUMPAD3;
                case TAB_TIMELINE -> code == KeyCode.NUMPAD4;
                case TAB_IO -> code == KeyCode.NUMPAD5;
                default -> false;
            };
        }

        return false;
    }
}

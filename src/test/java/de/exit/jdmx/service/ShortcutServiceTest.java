package de.exit.jdmx.service;

import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für den {@link ShortcutService}.
 */
class ShortcutServiceTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Bereits initialisiert
        }
    }

    @Test
    @DisplayName("Sollte alle Standard-Shortcuts initialisieren")
    void shouldInitializeDefaultShortcuts() {
        ShortcutService service = new ShortcutService();
        List<ShortcutItem> shortcuts = service.getAllShortcuts();

        assertThat(shortcuts).isNotEmpty();
        assertThat(shortcuts).anyMatch(s -> s.action() == ShortcutAction.NEW_PROJECT);
        assertThat(shortcuts).anyMatch(s -> s.action() == ShortcutAction.SAVE_PROJECT);
        assertThat(shortcuts).anyMatch(s -> s.action() == ShortcutAction.FULLSCREEN);
        assertThat(shortcuts).anyMatch(s -> s.action() == ShortcutAction.TAB_STAGE);

        // Keine versehentlichen Live-Gefahren: Space und Escape dürfen NICHT als Blackout/Stop registriert sein
        assertThat(shortcuts).noneMatch(s -> s.keyCombination().match(new javafx.scene.input.KeyEvent(
                javafx.scene.input.KeyEvent.KEY_PRESSED, "", "", KeyCode.SPACE, false, false, false, false)));
        assertThat(shortcuts).noneMatch(s -> s.keyCombination().match(new javafx.scene.input.KeyEvent(
                javafx.scene.input.KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false)));
    }

    @Test
    @DisplayName("Sollte Shortcuts anpassen und zurücksetzen können")
    void shouldCustomizeAndResetShortcut() {
        ShortcutService service = new ShortcutService();

        KeyCombination customComb = new KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN);
        service.setShortcut(ShortcutAction.SAVE_PROJECT, customComb);

        ShortcutItem updated = service.getShortcut(ShortcutAction.SAVE_PROJECT);
        assertThat(updated.keyCombination()).isEqualTo(customComb);
        assertThat(service.getDisplayText(ShortcutAction.SAVE_PROJECT)).contains("Strg + P");

        // Zurücksetzen
        service.resetShortcut(ShortcutAction.SAVE_PROJECT);
        ShortcutItem reset = service.getShortcut(ShortcutAction.SAVE_PROJECT);
        assertThat(service.getDisplayText(ShortcutAction.SAVE_PROJECT)).contains("Strg + S");
    }

    @Test
    @DisplayName("Sollte Kollisionen bei doppelter Tastenbelegung erkennen")
    void shouldDetectShortcutCollision() {
        ShortcutService service = new ShortcutService();

        // Strg + S ist standardmäßig SAVE_PROJECT
        KeyCombination saveComb = service.getShortcut(ShortcutAction.SAVE_PROJECT).keyCombination();

        // Versuch, NEW_PROJECT ebenfalls auf Strg + S zu legen
        ShortcutAction collision = service.findCollision(ShortcutAction.NEW_PROJECT, saveComb);
        assertThat(collision).isEqualTo(ShortcutAction.SAVE_PROJECT);
    }

    @Test
    @DisplayName("Sollte KeyEvent mit ShortcutAction abgleichen (matches)")
    void shouldMatchKeyEvents() {
        ShortcutService service = new ShortcutService();

        // F11 sollte FULLSCREEN matchen
        javafx.scene.input.KeyEvent f11Event = new javafx.scene.input.KeyEvent(
                javafx.scene.input.KeyEvent.KEY_PRESSED, "", "", KeyCode.F11, false, false, false, false);
        assertThat(service.matches(ShortcutAction.FULLSCREEN, f11Event)).isTrue();

        // F10 sollte NICHT FULLSCREEN matchen
        javafx.scene.input.KeyEvent f10Event = new javafx.scene.input.KeyEvent(
                javafx.scene.input.KeyEvent.KEY_PRESSED, "", "", KeyCode.F10, false, false, false, false);
        assertThat(service.matches(ShortcutAction.FULLSCREEN, f10Event)).isFalse();

        // Digit 1 und Numpad 1 sollten TAB_STAGE matchen
        javafx.scene.input.KeyEvent digit1Event = new javafx.scene.input.KeyEvent(
                javafx.scene.input.KeyEvent.KEY_PRESSED, "", "", KeyCode.DIGIT1, false, false, false, false);
        assertThat(service.matches(ShortcutAction.TAB_STAGE, digit1Event)).isTrue();

        javafx.scene.input.KeyEvent numpad1Event = new javafx.scene.input.KeyEvent(
                javafx.scene.input.KeyEvent.KEY_PRESSED, "", "", KeyCode.NUMPAD1, false, false, false, false);
        assertThat(service.matches(ShortcutAction.TAB_STAGE, numpad1Event)).isTrue();
    }
}

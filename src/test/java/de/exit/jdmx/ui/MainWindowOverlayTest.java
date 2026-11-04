package de.exit.jdmx.ui;

import javafx.application.Platform;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für die In-App Modal Overlay Infrastruktur von {@link MainWindow}.
 */
class MainWindowOverlayTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Bereits initialisiert
        }
    }

    @Test
    @DisplayName("Sollte Modal-Overlays einblenden und schließen können")
    void shouldShowAndHideModalOverlay() {
        MainWindow mainWindow = new MainWindow();
        assertThat(mainWindow.isOverlayShowing()).isFalse();

        Label modalCard = new Label("Settings Card");
        mainWindow.showOverlay(modalCard);

        assertThat(mainWindow.isOverlayShowing()).isTrue();

        mainWindow.hideOverlay();
        assertThat(mainWindow.isOverlayShowing()).isFalse();
    }

    @Test
    @DisplayName("Sollte zentralen ShortcutService bereitstellen und Tab per Shortcut umschalten")
    void shouldHandleShortcutsInMainWindow() {
        MainWindow mainWindow = new MainWindow();
        assertThat(mainWindow.getShortcutService()).isNotNull();
        assertThat(mainWindow.getHeaderBar().getActiveTabIndex()).isEqualTo(0);

        // Simulieren von Taste "2" -> Tab 1 (Simple Desk)
        javafx.scene.input.KeyEvent key2 = new javafx.scene.input.KeyEvent(
                javafx.scene.input.KeyEvent.KEY_PRESSED, "", "", javafx.scene.input.KeyCode.DIGIT2, false, false, false, false);
        mainWindow.fireEvent(key2);

        assertThat(mainWindow.getHeaderBar().getActiveTabIndex()).isEqualTo(1);
    }

    @Test
    @DisplayName("Sollte Fullscreen-Hinweis ohne Fehler einblenden können")
    void shouldShowFullscreenHint() {
        MainWindow mainWindow = new MainWindow();
        mainWindow.showFullscreenHint();
        // Kein Fehler geworfen
        assertThat(mainWindow).isNotNull();
    }
}

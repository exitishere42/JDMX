package de.exit.jdmx.ui;

import de.exit.jdmx.service.ShortcutService;
import de.exit.jdmx.ui.dialog.SettingsDialog;
import javafx.application.Platform;
import javafx.scene.Node;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für den {@link SettingsDialog}.
 */
class SettingsDialogTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit aktiv
        }
    }

    @Test
    @DisplayName("Sollte Settings-Dialog Karte mit Header, Tabs und Footer erzeugen")
    void shouldCreateSettingsDialogCard() {
        MainWindow mainWindow = new MainWindow();
        ShortcutService shortcutService = new ShortcutService();

        SettingsDialog dialog = new SettingsDialog(mainWindow, shortcutService);
        Node card = dialog.getView();

        assertThat(card).isNotNull();
        assertThat(card.getStyleClass()).contains("settings-dialog-card");
    }

    @Test
    @DisplayName("Sollte Settings-Dialog über MainWindow einblenden können")
    void shouldShowSettingsOverlay() {
        MainWindow mainWindow = new MainWindow();
        assertThat(mainWindow.isOverlayShowing()).isFalse();

        SettingsDialog.show(mainWindow);
        assertThat(mainWindow.isOverlayShowing()).isTrue();

        mainWindow.hideOverlay();
        assertThat(mainWindow.isOverlayShowing()).isFalse();
    }
}

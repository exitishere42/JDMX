package de.exit.jdmx.ui.dialog;

import de.exit.jdmx.service.UpdateService;
import de.exit.jdmx.ui.MainWindow;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für den Update-Dialog {@link AboutUpdateDialog}.
 * Prüft den Aufbau des UI-Kartenlayouts, die Anzeige der installierten Version
 * und die Reaktionsfähigkeit der Steuerelemente.
 */
class AboutUpdateDialogTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // JavaFX Toolkit aktiv
        }
    }

    @Test
    @DisplayName("Sollte Dialog-Karte mit aktuellem Versionsstand initialisieren")
    void shouldInitializeUpdateDialogCard() {
        AboutUpdateDialog dialog = new AboutUpdateDialog(null);
        VBox view = (VBox) dialog.getView();

        assertThat(view).isNotNull();
        assertThat(view.getStyleClass()).contains("settings-dialog-card");
        assertThat(dialog.getLblCurrentVersion().getText()).contains(UpdateService.CURRENT_VERSION);
        assertThat(dialog.getBtnClose()).isNotNull();
    }

    @Test
    @DisplayName("Sollte Overlay bei Klick auf Schließen-Button ausblenden")
    void shouldHideOverlayOnClose() {
        MainWindow mainWindow = new MainWindow();
        AboutUpdateDialog dialog = new AboutUpdateDialog(mainWindow);

        mainWindow.showOverlay(dialog.getView());
        assertThat(mainWindow.isOverlayShowing()).isTrue();

        dialog.getBtnClose().fire();
        assertThat(mainWindow.isOverlayShowing()).isFalse();
    }
}

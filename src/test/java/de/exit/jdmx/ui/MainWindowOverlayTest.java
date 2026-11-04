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
}

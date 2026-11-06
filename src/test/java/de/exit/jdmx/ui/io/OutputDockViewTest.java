package de.exit.jdmx.ui.io;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Unit-Tests für {@link OutputDockView}.
 * Prüft das verschiebbare Steuerungs-Dock für Ausgänge (Art-Net Out & USB-DMX) im control-module-card Stil.
 */
class OutputDockViewTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit aktiv
        }
    }

    @Test
    @DisplayName("Sollte control-module-card StyleClass und Titel Ausgang besitzen")
    void shouldHaveModuleCardStyleAndOutputTitle() {
        OutputDockView dock = new OutputDockView();

        assertThat(dock.getStyleClass()).contains("control-module-card");
        assertThat(dock.getTitle()).contains("Ausgang");
    }

    @Test
    @DisplayName("Sollte Steuerelemente für Art-Net Ziel-IP und Universum bereitstellen")
    void shouldProvideArtNetControls() {
        OutputDockView dock = new OutputDockView();

        assertThat(dock.getTargetIpField()).isNotNull();
        assertThat(dock.getTargetIpField().getText()).isEqualTo("127.0.0.1");
        assertThat(dock.getUniverseSpinner()).isNotNull();
        assertThat(dock.getUniverseSpinner().getValue()).isEqualTo(0);
    }
}

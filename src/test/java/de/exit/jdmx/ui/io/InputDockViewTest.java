package de.exit.jdmx.ui.io;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Unit-Tests für {@link InputDockView}.
 * Prüft das verschiebbare Steuerungs-Dock für Eingänge (MIDI & Art-Net In) im control-module-card Stil.
 */
class InputDockViewTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit aktiv
        }
    }

    @Test
    @DisplayName("Sollte control-module-card StyleClass und Titel Eingang besitzen")
    void shouldHaveModuleCardStyleAndInputTitle() {
        InputDockView dock = new InputDockView();

        assertThat(dock.getStyleClass()).contains("control-module-card");
        assertThat(dock.getTitle()).contains("Eingang");
    }

    @Test
    @DisplayName("Sollte Steuerelemente für MIDI-Gerät und Art-Net In bereitstellen")
    void shouldProvideMidiAndArtNetInputControls() {
        InputDockView dock = new InputDockView();

        assertThat(dock.getMidiDeviceCombo()).isNotNull();
        assertThat(dock.getMidiDeviceCombo().getItems()).isNotEmpty();
        assertThat(dock.getArtNetInCombo()).isNotNull();
    }
}

package de.exit.jdmx.ui.io;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Unit-Tests für {@link SignalRoutingPane}.
 * Prüft den vereinfachten Signalfluss nach Vorbild von sound2artnet:
 * [INPUT] ---> [JDMX PROZESSOR] ---> [OUTPUT] mit Ziel-IP und Universum.
 */
class SignalRoutingPaneTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit aktiv
        }
    }

    @Test
    @DisplayName("Sollte drei Boxen (Input, JDMX, Output) und zwei Verbindungs-Pfeile enthalten")
    void shouldContainThreeBoxesAndTwoConnectingArrows() {
        SignalRoutingPane pane = new SignalRoutingPane();

        assertThat(pane.getInputBox()).isNotNull();
        assertThat(pane.getJdmxBox()).isNotNull();
        assertThat(pane.getOutputBox()).isNotNull();

        assertThat(pane.getArrowInputToJdmx()).isNotNull();
        assertThat(pane.getArrowJdmxToOutput()).isNotNull();
    }

    @Test
    @DisplayName("Sollte vereinfachte Art-Net Steuerelemente wie in sound2artnet bereitstellen")
    void shouldProvideSimpleArtNetControlsLikeSound2ArtNet() {
        SignalRoutingPane pane = new SignalRoutingPane();

        assertThat(pane.getArtNetCheckbox()).isNotNull();
        assertThat(pane.getArtNetCheckbox().isSelected()).isTrue();

        assertThat(pane.getTargetIpField()).isNotNull();
        assertThat(pane.getTargetIpField().getText()).isEqualTo("127.0.0.1");

        assertThat(pane.getUniverseSpinner()).isNotNull();
        assertThat(pane.getUniverseSpinner().getValue()).isEqualTo(0);

        assertThat(pane.getMidiDeviceCombo()).isNotNull();
        assertThat(pane.getMidiDeviceCombo().getItems()).contains("Kein Gerät verbunden");
    }
}

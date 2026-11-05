package de.exit.jdmx.ui.io;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Unit-Tests für {@link SignalRoutingPane}.
 * Prüft den horizontalen Signalfluss [INPUT] ---> [JDMX] ---> [OUTPUT] mit Art-Net Steuerelementen.
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
    @DisplayName("Sollte Art-Net Optionen im Output-Bereich bereitstellen")
    void shouldProvideArtNetControlsInOutputBox() {
        SignalRoutingPane pane = new SignalRoutingPane();

        assertThat(pane.getArtNetCheckbox()).isNotNull();
        assertThat(pane.getArtNetCheckbox().isSelected()).isTrue();

        assertThat(pane.getNetworkInterfaceCombo()).isNotNull();
        assertThat(pane.getNetworkInterfaceCombo().getItems()).isNotEmpty();

        assertThat(pane.getBroadcastRadio()).isNotNull();
        assertThat(pane.getUnicastRadio()).isNotNull();
        assertThat(pane.getBroadcastRadio().isSelected()).isTrue();

        assertThat(pane.getSubnetSpinner()).isNotNull();
        assertThat(pane.getUniverseSpinner()).isNotNull();
    }
}

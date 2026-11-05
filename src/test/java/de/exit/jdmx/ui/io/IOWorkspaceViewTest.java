package de.exit.jdmx.ui.io;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Unit-Tests für {@link IOWorkspaceView} (Tab 5).
 * Prüft die Zusammenführung von SignalRoutingPane und DmxMatrixView.
 */
class IOWorkspaceViewTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit aktiv
        }
    }

    @Test
    @DisplayName("Sollte SignalRoutingPane oben und DmxMatrixView unten bereitstellen")
    void shouldAssembleRoutingPaneAndMatrixView() {
        IOWorkspaceView view = new IOWorkspaceView();

        assertThat(view.getRoutingPane()).isNotNull();
        assertThat(view.getMatrixView()).isNotNull();
        assertThat(view.getChildren()).contains(view.getRoutingPane(), view.getMatrixView());
    }
}

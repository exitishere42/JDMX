package de.exit.jdmx.ui.io;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Unit-Tests für {@link IOWorkspaceView} (Tab 5).
 * Prüft die Zusammenführung von UniversePatchTable (QLC+ Stil) und DmxMatrixView.
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
    @DisplayName("Sollte UniversePatchTable oben und DmxMatrixView unten bereitstellen")
    void shouldAssemblePatchTableAndMatrixView() {
        IOWorkspaceView view = new IOWorkspaceView();

        assertThat(view.getPatchTable()).isNotNull();
        assertThat(view.getMatrixView()).isNotNull();
        assertThat(view.getChildren()).contains(view.getPatchTable(), view.getMatrixView());
    }
}

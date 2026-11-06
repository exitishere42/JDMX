package de.exit.jdmx.ui.io;

import javafx.application.Platform;
import javafx.geometry.Orientation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Unit-Tests für {@link IOWorkspaceView} (Tab 5: I/O Patch).
 * Prüft die SplitPane-Architektur mit 2 frei verschiebbaren Steuerungsdocks oben
 * und der DMX-Matrix unten (wie in Stage & Fixtures).
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
    @DisplayName("Sollte vertikales SplitPane mit oberen Docks und unterer DMX-Matrix sein")
    void shouldBeVerticalSplitPaneWithDocksAndMatrix() {
        IOWorkspaceView view = new IOWorkspaceView();

        assertThat(view.getOrientation()).isEqualTo(Orientation.VERTICAL);
        assertThat(view.getDocksSplitPane()).isNotNull();
        assertThat(view.getMatrixView()).isNotNull();
        assertThat(view.getItems()).contains(view.getDocksSplitPane(), view.getMatrixView());
    }

    @Test
    @DisplayName("Sollte im oberen Bereich ein horizontales SplitPane mit genau 2 Docks enthalten")
    void shouldHaveHorizontalSplitPaneWithTwoDocks() {
        IOWorkspaceView view = new IOWorkspaceView();

        assertThat(view.getDocksSplitPane().getOrientation()).isEqualTo(Orientation.HORIZONTAL);
        assertThat(view.getInputDock()).isNotNull();
        assertThat(view.getOutputDock()).isNotNull();
        assertThat(view.getDocksSplitPane().getItems()).contains(view.getInputDock(), view.getOutputDock());
    }
}

package de.exit.jdmx.ui.stage;

import javafx.application.Platform;
import javafx.geometry.Orientation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für den Haupt-Workspace von Tab 1 (StageWorkspaceView).
 * Stellt sicher, dass die leere Stage oben immer mehr als 60% des Platzes behält.
 */
class StageWorkspaceViewTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit aktiv
        }
    }

    @Test
    @DisplayName("Sollte vertikalen Splitter mit oberer Stage und unterem Dock aufbauen")
    void shouldBuildVerticalSplitPane() {
        StageWorkspaceView workspace = new StageWorkspaceView();

        assertThat(workspace.getOrientation()).isEqualTo(Orientation.VERTICAL);
        assertThat(workspace.getItems()).hasSize(2);

        assertThat(workspace.getStageCanvasView()).isNotNull();
        assertThat(workspace.getControlDock()).isNotNull();
    }

    @Test
    @DisplayName("Sollte der Stage standardmäßig und garantiert mehr als 60% vertikalen Platz gewähren")
    void shouldGuaranteeStageKeepsMoreThanSixtyPercentHeight() {
        StageWorkspaceView workspace = new StageWorkspaceView();

        // Standard-Teilung: ca. 80-85% für die Stage, 15-20% für die kompakte Leiste
        double defaultDividerPos = workspace.getDividerPosition();
        assertThat(defaultDividerPos).isGreaterThan(0.60);
    }

    @Test
    @DisplayName("Sollte die Divider-Position auf mindestens 60% begrenzen (Clamping-Schutz)")
    void shouldClampDividerPositionToMinimumSixtyPercent() {
        StageWorkspaceView workspace = new StageWorkspaceView();

        // Versuch, den Divider auf 0.40 (40% für die Stage) zu ziehen
        workspace.requestDividerPosition(0.40);

        // Muss automatisch auf mindestens 0.60 geklammert werden
        assertThat(workspace.getDividerPosition()).isGreaterThanOrEqualTo(0.60);
    }
}

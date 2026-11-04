package de.exit.jdmx.ui.stage;

import javafx.application.Platform;
import javafx.geometry.Orientation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für den Haupt-Workspace von Tab 1 (StageWorkspaceView).
 * Stellt sicher, dass das Dock nur nach oben (größer) gezogen werden kann
 * und nach unten exakt an der Standardposition stoppt.
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

        double defaultDividerPos = workspace.getDividerPosition();
        assertThat(defaultDividerPos).isGreaterThan(0.60);
    }

    @Test
    @DisplayName("Sollte die Divider-Position nach oben auf mindestens 60% begrenzen")
    void shouldClampDividerPositionToMinimumSixtyPercent() {
        StageWorkspaceView workspace = new StageWorkspaceView();

        // Versuch, den Divider auf 0.40 (40% für die Stage) nach oben zu ziehen
        workspace.requestDividerPosition(0.40);

        assertThat(workspace.getDividerPosition()).isGreaterThanOrEqualTo(StageWorkspaceView.MIN_STAGE_RATIO);
    }

    @Test
    @DisplayName("Sollte das Dock nach unten auf die Standardposition begrenzen (darf nicht kleiner als Standard werden)")
    void shouldClampDividerPositionToMaximumDefaultRatio() {
        StageWorkspaceView workspace = new StageWorkspaceView();

        // Versuch, den Divider weiter nach unten auf 0.95 (Dock noch kleiner machen) zu schieben
        workspace.requestDividerPosition(0.95);

        // Muss automatisch auf DEFAULT_STAGE_RATIO geklammert werden
        assertThat(workspace.getDividerPosition()).isLessThanOrEqualTo(StageWorkspaceView.DEFAULT_STAGE_RATIO);
    }
}

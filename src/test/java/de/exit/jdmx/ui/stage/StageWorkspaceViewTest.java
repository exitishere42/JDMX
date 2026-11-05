package de.exit.jdmx.ui.stage;

import javafx.application.Platform;
import javafx.geometry.Orientation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für den Haupt-Workspace von Tab 1 (StageWorkspaceView).
 * Prüft die horizontale Teilung mit dem durchgehenden Project Explorer rechts
 * und das vertikale Dock links.
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
    @DisplayName("Sollte horizontales Haupt-SplitPane mit linkem Bereich und rechtem Project Explorer aufbauen")
    void shouldBuildHorizontalMainSplitPane() {
        StageWorkspaceView workspace = new StageWorkspaceView();

        assertThat(workspace.getOrientation()).isEqualTo(Orientation.HORIZONTAL);
        assertThat(workspace.getItems()).hasSize(2);

        assertThat(workspace.getStageCanvasView()).isNotNull();
        assertThat(workspace.getControlDock()).isNotNull();
        assertThat(workspace.getProjectExplorerView()).isNotNull();
    }

    @Test
    @DisplayName("Sollte Project Explorer per Toggle zuklappen und wieder aufklappen können")
    void shouldToggleProjectExplorerCollapseAndExpand() {
        StageWorkspaceView workspace = new StageWorkspaceView();

        assertThat(workspace.isProjectExplorerCollapsed()).isFalse();

        // 1. Zuklappen
        workspace.toggleProjectExplorer();
        assertThat(workspace.isProjectExplorerCollapsed()).isTrue();

        // 2. Wieder aufklappen
        workspace.toggleProjectExplorer();
        assertThat(workspace.isProjectExplorerCollapsed()).isFalse();
    }

    @Test
    @DisplayName("Sollte das Steuerungs-Dock nach unten auf die Mindesthöhe begrenzen")
    void shouldEnforceDockHeightLimit() {
        StageWorkspaceView workspace = new StageWorkspaceView();

        assertThat(workspace.getControlDock().getMinHeight()).isEqualTo(DeviceControlDock.DOCK_HEIGHT);
    }
}

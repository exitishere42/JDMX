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
 * und das fehlerfreie Zuklappen zu 100%.
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
    @DisplayName("Sollte Project Explorer vollständig auf 0px zuklappen und wieder aufklappen können")
    void shouldToggleProjectExplorerCollapseAndExpand() {
        StageWorkspaceView workspace = new StageWorkspaceView();

        assertThat(workspace.isProjectExplorerCollapsed()).isFalse();
        assertThat(workspace.getProjectExplorerView().isVisible()).isTrue();
        assertThat(workspace.getBtnExpandProjectExplorer().isVisible()).isFalse();

        // 1. Zuklappen über Button im Project Explorer
        workspace.getProjectExplorerView().getBtnCollapse().fire();
        assertThat(workspace.isProjectExplorerCollapsed()).isTrue();
        assertThat(workspace.getProjectExplorerView().isVisible()).isFalse();
        assertThat(workspace.getProjectExplorerView().getMinWidth()).isEqualTo(0.0);
        assertThat(workspace.getProjectExplorerView().getMaxWidth()).isEqualTo(0.0);
        assertThat(workspace.getBtnExpandProjectExplorer().isVisible()).isTrue();

        // 2. Wieder aufklappen über den Ausklapp-Button an der rechten Kante
        workspace.getBtnExpandProjectExplorer().fire();
        assertThat(workspace.isProjectExplorerCollapsed()).isFalse();
        assertThat(workspace.getProjectExplorerView().isVisible()).isTrue();
        assertThat(workspace.getProjectExplorerView().getMinWidth()).isGreaterThanOrEqualTo(140.0);
        assertThat(workspace.getProjectExplorerView().getMaxWidth()).isGreaterThanOrEqualTo(500.0);
        assertThat(workspace.getBtnExpandProjectExplorer().isVisible()).isFalse();
    }

    @Test
    @DisplayName("Sollte das Steuerungs-Dock nach unten auf die Mindesthöhe begrenzen")
    void shouldEnforceDockHeightLimit() {
        StageWorkspaceView workspace = new StageWorkspaceView();

        assertThat(workspace.getControlDock().getMinHeight()).isEqualTo(DeviceControlDock.DOCK_HEIGHT);
    }

    @Test
    @DisplayName("Sollte Slider- und Farbänderungen im Steuerungs-Dock auf das selektierte Fixture übertragen")
    void shouldSynchronizeControlDockWithSelectedFixture() {
        StageWorkspaceView workspace = new StageWorkspaceView();

        de.exit.jdmx.core.model.ExplorerItem spot = new de.exit.jdmx.core.model.ExplorerItem(
                "mh-spot", "Moving Head Spot", de.exit.jdmx.core.model.ExplorerItemType.FIXTURE, false, "box", javafx.scene.paint.Color.web("#60A5FA"));
        StageFixtureNode node = workspace.getStageCanvasView().placeFixture(spot, 100.0, 100.0);

        // Selektieren
        node.setSelected(true);

        // Dimmer im Dock ändern -> Node übernimmt Helligkeit
        workspace.getControlDock().getIntensityModule().getDimmerSlider().setValue(50.0);
        assertThat(node.getDimmer()).isEqualTo(0.5);

        // Farbe im Dock ändern -> Node übernimmt Farbe
        workspace.getControlDock().getColorModule().setColor(javafx.scene.paint.Color.RED);
        assertThat(node.getBaseColor()).isEqualTo(javafx.scene.paint.Color.RED);

        // Strobo im Dock ändern -> Node übernimmt Strobo
        workspace.getControlDock().getIntensityModule().getStrobeSlider().setValue(30.0);
        assertThat(node.isStrobing()).isTrue();
    }
}

package de.exit.jdmx.ui.matrix;

import de.exit.jdmx.service.ChaserService;
import de.exit.jdmx.service.DmxService;
import de.exit.jdmx.service.MatrixService;
import de.exit.jdmx.service.ProjectExplorerService;
import de.exit.jdmx.service.SceneService;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für den {@link MatrixWorkspaceView} (Tab 3: Button Matrix).
 * Verifiziert das StageView-äquivalente SplitPane-Layout mit dem Project Explorer auf der rechten Seite
 * und dem Button-Grid sowie dem Steuerungsdock auf der linken Seite.
 */
class MatrixWorkspaceViewTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // JavaFX Toolkit bereits initialisiert
        }
    }

    @Test
    @DisplayName("MatrixWorkspaceView baut ein horizontales SplitPane mit Grid links und Explorer rechts auf")
    void testLayoutStructure() {
        DmxService dmxService = Mockito.mock(DmxService.class);
        SceneService sceneService = Mockito.mock(SceneService.class);
        ChaserService chaserService = Mockito.mock(ChaserService.class);
        ProjectExplorerService explorerService = new ProjectExplorerService();
        MatrixService matrixService = new MatrixService(dmxService, sceneService, chaserService);

        MatrixWorkspaceView workspace = new MatrixWorkspaceView(matrixService, explorerService);

        assertThat(workspace.getOrientation()).isEqualTo(Orientation.HORIZONTAL);
        assertThat(workspace.getItems()).hasSize(2);
        assertThat(workspace.getMatrixGridView()).isNotNull();
        assertThat(workspace.getProjectExplorerView()).isNotNull();

        assertThat(workspace.getItems().get(1)).isEqualTo(workspace.getProjectExplorerView());
    }

    @Test
    @DisplayName("MatrixWorkspaceView unterstützt das Ein- und Ausklappen des Project Explorers")
    void testCollapseExpandExplorer() {
        DmxService dmxService = Mockito.mock(DmxService.class);
        SceneService sceneService = Mockito.mock(SceneService.class);
        ChaserService chaserService = Mockito.mock(ChaserService.class);
        ProjectExplorerService explorerService = new ProjectExplorerService();
        MatrixService matrixService = new MatrixService(dmxService, sceneService, chaserService);

        MatrixWorkspaceView workspace = new MatrixWorkspaceView(matrixService, explorerService);

        assertThat(workspace.isExplorerCollapsed()).isFalse();
        workspace.toggleProjectExplorer();
        assertThat(workspace.isExplorerCollapsed()).isTrue();
        workspace.toggleProjectExplorer();
        assertThat(workspace.isExplorerCollapsed()).isFalse();
    }
}

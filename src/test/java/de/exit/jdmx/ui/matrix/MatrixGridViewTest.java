package de.exit.jdmx.ui.matrix;

import de.exit.jdmx.service.ChaserService;
import de.exit.jdmx.service.DmxService;
import de.exit.jdmx.service.MatrixService;
import de.exit.jdmx.service.SceneService;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für das {@link MatrixGridView}.
 */
class MatrixGridViewTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // JavaFX Toolkit bereits initialisiert
        }
    }

    @Test
    @DisplayName("MatrixGridView initialisiert Kacheln für alle Buttons im Grid")
    void testGridInitialization() {
        DmxService dmxService = Mockito.mock(DmxService.class);
        SceneService sceneService = Mockito.mock(SceneService.class);
        ChaserService chaserService = Mockito.mock(ChaserService.class);
        MatrixService matrixService = new MatrixService(dmxService, sceneService, chaserService);

        MatrixGridView gridView = new MatrixGridView(matrixService);

        assertThat(gridView.getButtonNodes()).hasSize(32);
        assertThat(gridView.getButtonNode(0)).isNotNull();
        assertThat(gridView.getButtonNode(31)).isNotNull();
    }

    @Test
    @DisplayName("MatrixGridView unterstützt Auswahl eines Buttons")
    void testButtonSelection() {
        DmxService dmxService = Mockito.mock(DmxService.class);
        SceneService sceneService = Mockito.mock(SceneService.class);
        ChaserService chaserService = Mockito.mock(ChaserService.class);
        MatrixService matrixService = new MatrixService(dmxService, sceneService, chaserService);

        MatrixGridView gridView = new MatrixGridView(matrixService);

        int[] selected = {-1};
        gridView.setOnButtonSelected(idx -> selected[0] = idx);

        gridView.selectButton(5);
        assertThat(selected[0]).isEqualTo(5);
        assertThat(gridView.getSelectedButtonIndex()).isEqualTo(5);
    }
}

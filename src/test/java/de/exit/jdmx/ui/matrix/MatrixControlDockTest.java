package de.exit.jdmx.ui.matrix;

import de.exit.jdmx.core.model.MatrixButtonDefinition;
import de.exit.jdmx.core.model.MatrixTargetType;
import de.exit.jdmx.core.model.MatrixTriggerMode;
import de.exit.jdmx.service.ChaserService;
import de.exit.jdmx.service.DmxService;
import de.exit.jdmx.service.MatrixService;
import de.exit.jdmx.service.SceneService;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für das {@link MatrixControlDock}.
 */
class MatrixControlDockTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // JavaFX Toolkit bereits aktiv
        }
    }

    private MatrixService matrixService;
    private MatrixControlDock controlDock;

    @BeforeEach
    void setUp() {
        DmxService dmxService = Mockito.mock(DmxService.class);
        SceneService sceneService = Mockito.mock(SceneService.class);
        ChaserService chaserService = Mockito.mock(ChaserService.class);
        matrixService = new MatrixService(dmxService, sceneService, chaserService);
        controlDock = new MatrixControlDock(matrixService);
    }

    @Test
    @DisplayName("MatrixControlDock besitzt feste Mindesthöhe von 220px wie das Steuerungsdock in Tab 1")
    void testDimensions() {
        assertThat(controlDock.getMinHeight()).isEqualTo(MatrixControlDock.DOCK_HEIGHT);
        assertThat(controlDock.getPrefHeight()).isEqualTo(MatrixControlDock.DOCK_HEIGHT);
    }

    @Test
    @DisplayName("Auswahl eines Buttons synchronisiert die Dock-Module")
    void testSelectButton() {
        matrixService.assignTarget(3, MatrixTargetType.SCENE, "scene-42", "Warmweiß");
        matrixService.setTriggerMode(3, MatrixTriggerMode.FLASH);

        controlDock.selectButton(3);

        assertThat(controlDock.getSelectedButtonIndex()).isEqualTo(3);
        assertThat(controlDock.getNameTextField().getText()).isEqualTo("Warmweiß");
        assertThat(controlDock.getSelectedTriggerMode()).isEqualTo(MatrixTriggerMode.FLASH);
    }

    @Test
    @DisplayName("Änderung des Trigger-Modus im Dock aktualisiert das Modell")
    void testChangeTriggerMode() {
        controlDock.selectButton(0);
        controlDock.setTriggerMode(MatrixTriggerMode.FLASH);

        MatrixButtonDefinition btn = matrixService.getGrid().getButton(0);
        assertThat(btn.triggerMode()).isEqualTo(MatrixTriggerMode.FLASH);
    }

    @Test
    @DisplayName("Klick auf Leeren setzt den Button zurück")
    void testClearButton() {
        matrixService.assignTarget(1, MatrixTargetType.CHASER, "chaser-1", "Effekt");
        controlDock.selectButton(1);

        controlDock.clearCurrentButton();

        MatrixButtonDefinition btn = matrixService.getGrid().getButton(1);
        assertThat(btn.isAssigned()).isFalse();
    }
}

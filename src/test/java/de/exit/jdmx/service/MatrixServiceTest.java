package de.exit.jdmx.service;

import de.exit.jdmx.core.model.ChaserDefinition;
import de.exit.jdmx.core.model.MatrixButtonDefinition;
import de.exit.jdmx.core.model.MatrixGridDefinition;
import de.exit.jdmx.core.model.MatrixTargetType;
import de.exit.jdmx.core.model.MatrixTriggerMode;
import de.exit.jdmx.core.model.SceneDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-Tests für den {@link MatrixService}.
 */
class MatrixServiceTest {

    private DmxService dmxService;
    private SceneService sceneService;
    private ChaserService chaserService;
    private MatrixService matrixService;

    @BeforeEach
    void setUp() {
        dmxService = Mockito.mock(DmxService.class);
        sceneService = Mockito.mock(SceneService.class);
        chaserService = Mockito.mock(ChaserService.class);

        matrixService = new MatrixService(dmxService, sceneService, chaserService);
    }

    @Test
    @DisplayName("MatrixService initialisiert Standard-Grid mit 32 Buttons")
    void testInitialization() {
        assertThat(matrixService.getGrid()).isNotNull();
        assertThat(matrixService.getGrid().getTotalButtons()).isEqualTo(32);
        assertThat(matrixService.getActiveButtonIndices()).isEmpty();
    }

    @Test
    @DisplayName("MatrixService validiert Null-Parameter im Konstruktor")
    void testConstructorValidation() {
        assertThatThrownBy(() -> new MatrixService(null, sceneService, chaserService))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new MatrixService(dmxService, null, chaserService))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new MatrixService(dmxService, sceneService, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Unzugewiesener Button löst bei Klick nichts aus")
    void testPressUnassignedButton() {
        matrixService.pressButton(0);
        assertThat(matrixService.isButtonActive(0)).isFalse();
        verify(sceneService, never()).applyScene(any(String.class), anyInt());
        verify(chaserService, never()).play(any(String.class));
    }

    @Test
    @DisplayName("TOGGLE-Modus mit Szene: Erster Klick schaltet an, zweiter Klick schaltet aus")
    void testToggleScene() {
        Map<Integer, Map<Integer, Integer>> values = Map.of(1, Map.of(1, 255, 2, 128));
        SceneDefinition scene = new SceneDefinition("scene-1", "Bühne Blau", 0, values);
        when(sceneService.getScene("scene-1")).thenReturn(scene);

        matrixService.assignTarget(0, MatrixTargetType.SCENE, "scene-1", "Bühne Blau");
        matrixService.setTriggerMode(0, MatrixTriggerMode.TOGGLE);

        // 1. Klick -> Aktivieren
        matrixService.pressButton(0);
        assertThat(matrixService.isButtonActive(0)).isTrue();
        verify(sceneService, times(1)).applyScene("scene-1", 0);

        // 2. Klick -> Deaktivieren
        matrixService.pressButton(0);
        assertThat(matrixService.isButtonActive(0)).isFalse();
        verify(dmxService, times(1)).setChannel(1, 1, 0);
        verify(dmxService, times(1)).setChannel(1, 2, 0);
    }

    @Test
    @DisplayName("TOGGLE-Modus mit Chaser: Erster Klick startet Chaser, zweiter Klick stoppt Chaser")
    void testToggleChaser() {
        matrixService.assignTarget(1, MatrixTargetType.CHASER, "chaser-1", "Lauflicht");
        matrixService.setTriggerMode(1, MatrixTriggerMode.TOGGLE);

        // 1. Klick -> Starten
        matrixService.pressButton(1);
        assertThat(matrixService.isButtonActive(1)).isTrue();
        verify(chaserService, times(1)).play("chaser-1");

        // 2. Klick -> Stoppen
        matrixService.pressButton(1);
        assertThat(matrixService.isButtonActive(1)).isFalse();
        verify(chaserService, times(1)).stop("chaser-1");
    }

    @Test
    @DisplayName("FLASH-Modus mit Szene: Solange gedrückt aktiv, beim Loslassen deaktiviert")
    void testFlashScene() {
        Map<Integer, Map<Integer, Integer>> values = Map.of(1, Map.of(5, 255));
        SceneDefinition scene = new SceneDefinition("scene-flash", "Strobe", 0, values);
        when(sceneService.getScene("scene-flash")).thenReturn(scene);

        matrixService.assignTarget(2, MatrixTargetType.SCENE, "scene-flash", "Strobe");
        matrixService.setTriggerMode(2, MatrixTriggerMode.FLASH);

        // Drücken -> Aktiv
        matrixService.pressButton(2);
        assertThat(matrixService.isButtonActive(2)).isTrue();
        assertThat(matrixService.isButtonFlashPressed(2)).isTrue();
        verify(sceneService, times(1)).applyScene("scene-flash", 0);

        // Loslassen -> Aus
        matrixService.releaseButton(2);
        assertThat(matrixService.isButtonActive(2)).isFalse();
        assertThat(matrixService.isButtonFlashPressed(2)).isFalse();
        verify(dmxService, times(1)).setChannel(1, 5, 0);
    }

    @Test
    @DisplayName("stopAll deaktiviert sofort alle aktiven Buttons und stoppt Chaser")
    void testStopAll() {
        matrixService.assignTarget(0, MatrixTargetType.CHASER, "chaser-1", "C1");
        matrixService.assignTarget(1, MatrixTargetType.CHASER, "chaser-2", "C2");

        matrixService.pressButton(0);
        matrixService.pressButton(1);

        assertThat(matrixService.getActiveButtonIndices()).containsExactlyInAnyOrder(0, 1);

        matrixService.stopAll();

        assertThat(matrixService.getActiveButtonIndices()).isEmpty();
        verify(chaserService, times(1)).stop("chaser-1");
        verify(chaserService, times(1)).stop("chaser-2");
    }

    @Test
    @DisplayName("MatrixService benachrichtigt Listener bei Änderungen")
    void testListeners() {
        boolean[] called = {false};
        matrixService.addMatrixChangeListener(() -> called[0] = true);

        matrixService.setButtonColor(0, "#FF0000");
        assertThat(called[0]).isTrue();

        called[0] = false;
        matrixService.setButtonName(0, "Neuer Name");
        assertThat(called[0]).isTrue();
    }
}

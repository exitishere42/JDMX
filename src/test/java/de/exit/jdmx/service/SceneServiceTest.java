package de.exit.jdmx.service;

import de.exit.jdmx.core.model.ChannelDefinition;
import de.exit.jdmx.core.model.ChannelType;
import de.exit.jdmx.core.model.DmxMode;
import de.exit.jdmx.core.model.ExplorerItemType;
import de.exit.jdmx.core.model.FixtureDefinition;
import de.exit.jdmx.core.model.FixtureInstance;
import de.exit.jdmx.core.model.SceneDefinition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für {@link SceneService} und {@link SceneDefinition}.
 * Testet Snapshot-Erfassung von DMX-Kanalwerten und das Anwenden von Szenen.
 */
class SceneServiceTest {

    private DmxService dmxService;
    private SceneService sceneService;
    private FixtureInstance fixture1;

    @BeforeEach
    void setUp() {
        dmxService = new DmxService();
        sceneService = new SceneService(dmxService);

        DmxMode mode = new DmxMode("3ch", List.of(
                new ChannelDefinition(1, "Dimmer", ChannelType.DIMMER, 0, false),
                new ChannelDefinition(2, "Red", ChannelType.RED, 0, false),
                new ChannelDefinition(3, "Blue", ChannelType.BLUE, 0, false)
        ));
        FixtureDefinition def = new FixtureDefinition("par", "Brand", "LED Par", ExplorerItemType.FIXTURE, List.of(mode));
        fixture1 = new FixtureInstance("fix-1", "Par 1", def, mode, 1, 1, 100.0, 100.0);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (dmxService != null) {
            dmxService.close();
        }
    }

    @Test
    @DisplayName("Sollte aktuellen DMX-Zustand der gepatchten Fixtures als Szene aufnehmen")
    void shouldCaptureCurrentDmxStateAsScene() {
        // DMX-Werte setzen: Uni 0 (Universe 1), Ch 1=255, Ch 2=200, Ch 3=50
        dmxService.setChannel(0, 1, 255);
        dmxService.setChannel(0, 2, 200);
        dmxService.setChannel(0, 3, 50);

        SceneDefinition scene = sceneService.captureScene("Rot-Look", 500, List.of(fixture1));

        assertThat(scene).isNotNull();
        assertThat(scene.getId()).isNotBlank();
        assertThat(scene.getName()).isEqualTo("Rot-Look");
        assertThat(scene.getDefaultFadeTimeMs()).isEqualTo(500);

        Map<Integer, Integer> uni0Values = scene.getChannelValuesForUniverse(0);
        assertThat(uni0Values).isNotNull();
        assertThat(uni0Values.get(1)).isEqualTo(255);
        assertThat(uni0Values.get(2)).isEqualTo(200);
        assertThat(uni0Values.get(3)).isEqualTo(50);
    }

    @Test
    @DisplayName("Sollte Szene sofort anwenden (Snap / 0ms Fade)")
    void shouldApplySceneInstantly() {
        Map<Integer, Map<Integer, Integer>> values = Map.of(
                0, Map.of(1, 128, 2, 255, 3, 0)
        );
        SceneDefinition scene = new SceneDefinition("scene-test", "Test Szene", 0, values);
        sceneService.registerScene(scene);

        // Vorher auf 0
        dmxService.setChannel(0, 1, 0);
        dmxService.setChannel(0, 2, 0);

        // Szene anwenden
        sceneService.applyScene("scene-test", 0);

        assertThat(dmxService.getChannel(0, 1)).isEqualTo(128);
        assertThat(dmxService.getChannel(0, 2)).isEqualTo(255);
        assertThat(dmxService.getChannel(0, 3)).isEqualTo(0);
    }

    @Test
    @DisplayName("Sollte Szenen registrieren, abrufen und löschen können")
    void shouldManageScenes() {
        SceneDefinition scene = new SceneDefinition("s1", "Szene 1", 0, Map.of());
        sceneService.registerScene(scene);

        assertThat(sceneService.getScene("s1")).isEqualTo(scene);
        assertThat(sceneService.getAllScenes()).contains(scene);

        sceneService.deleteScene("s1");
        assertThat(sceneService.getScene("s1")).isNull();
        assertThat(sceneService.getAllScenes()).doesNotContain(scene);
    }

    @Test
    @DisplayName("Sollte Szene mit vorgegebener ID erfassen und umbenennen können")
    void shouldCaptureWithExplicitIdAndRename() {
        dmxService.setChannel(0, 1, 200);
        SceneDefinition scene = sceneService.captureScene("custom-scene-id", "Mein Preset", 100, List.of(fixture1));

        assertThat(scene.getId()).isEqualTo("custom-scene-id");
        assertThat(scene.getName()).isEqualTo("Mein Preset");
        assertThat(sceneService.getScene("custom-scene-id")).isNotNull();

        sceneService.renameScene("custom-scene-id", "Neuer Preset Name");
        assertThat(sceneService.getScene("custom-scene-id").getName()).isEqualTo("Neuer Preset Name");
    }
}

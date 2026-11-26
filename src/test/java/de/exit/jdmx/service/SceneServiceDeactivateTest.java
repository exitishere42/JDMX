package de.exit.jdmx.service;

import de.exit.jdmx.core.model.SceneDefinition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit-Tests für das Deaktivieren von Lichtszenen im {@link SceneService}.
 */
class SceneServiceDeactivateTest {

    private DmxService dmxService;
    private SceneService sceneService;

    @BeforeEach
    void setUp() {
        dmxService = new DmxService();
        sceneService = new SceneService(dmxService);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (sceneService != null) {
            sceneService.close();
        }
        if (dmxService != null) {
            dmxService.close();
        }
    }

    @Test
    @DisplayName("Sollte alle Kanäle einer Szene bei Deaktivierung auf 0 zurücksetzen")
    void shouldResetSceneChannelsToZeroOnDeactivation() {
        // Szene mit Universe 0 (Ch 1=255, Ch 2=128) und Universe 1 (Ch 5=200)
        Map<Integer, Map<Integer, Integer>> channels = Map.of(
                0, Map.of(1, 255, 2, 128),
                1, Map.of(5, 200)
        );
        SceneDefinition scene = new SceneDefinition("scene-test", "Test Look", 0, channels);
        sceneService.registerScene(scene);

        // Weiterer Kanal, der nicht zur Szene gehört
        dmxService.setChannel(0, 10, 180);

        // Szene anwenden
        sceneService.applyScene("scene-test", 0);
        assertThat(dmxService.getChannel(0, 1)).isEqualTo(255);
        assertThat(dmxService.getChannel(0, 2)).isEqualTo(128);
        assertThat(dmxService.getChannel(1, 5)).isEqualTo(200);
        assertThat(dmxService.getChannel(0, 10)).isEqualTo(180);

        // Szene deaktivieren (Snap, 0 ms Fade)
        sceneService.deactivateScene("scene-test", 0);

        // Kanäle der Szene müssen 0 sein
        assertThat(dmxService.getChannel(0, 1)).isEqualTo(0);
        assertThat(dmxService.getChannel(0, 2)).isEqualTo(0);
        assertThat(dmxService.getChannel(1, 5)).isEqualTo(0);

        // Nicht zur Szene gehörender Kanal bleibt unberührt
        assertThat(dmxService.getChannel(0, 10)).isEqualTo(180);
    }

    @Test
    @DisplayName("Sollte robust bei unbekannter oder null Szenen-ID reagieren")
    void shouldHandleNullOrUnknownSceneIdSafely() {
        assertThatCode(() -> sceneService.deactivateScene((String) null, 0))
                .doesNotThrowAnyException();
        assertThatCode(() -> sceneService.deactivateScene("nicht-existent", 0))
                .doesNotThrowAnyException();
    }
}

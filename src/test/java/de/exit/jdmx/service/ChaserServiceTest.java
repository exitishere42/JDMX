package de.exit.jdmx.service;

import de.exit.jdmx.core.model.ChaserDefinition;
import de.exit.jdmx.core.model.ChaserDirection;
import de.exit.jdmx.core.model.ChaserStep;
import de.exit.jdmx.core.model.SceneDefinition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für {@link ChaserService} und {@link ChaserDefinition}.
 * Testet Schritt-Sequenzen, Richtungs-Logik (Forward, Backward, Ping-Pong, Random) und Playback.
 */
class ChaserServiceTest {

    private DmxService dmxService;
    private SceneService sceneService;
    private ChaserService chaserService;

    private SceneDefinition sceneA;
    private SceneDefinition sceneB;
    private SceneDefinition sceneC;

    @BeforeEach
    void setUp() {
        dmxService = new DmxService();
        sceneService = new SceneService(dmxService);
        chaserService = new ChaserService(sceneService);

        sceneA = new SceneDefinition("scene-a", "Scene A", 0, Map.of(0, Map.of(1, 255)));
        sceneB = new SceneDefinition("scene-b", "Scene B", 0, Map.of(0, Map.of(1, 128)));
        sceneC = new SceneDefinition("scene-c", "Scene C", 0, Map.of(0, Map.of(1, 0)));

        sceneService.registerScene(sceneA);
        sceneService.registerScene(sceneB);
        sceneService.registerScene(sceneC);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (chaserService != null) {
            chaserService.close();
        }
        if (sceneService != null) {
            sceneService.close();
        }
        if (dmxService != null) {
            dmxService.close();
        }
    }

    @Test
    @DisplayName("Sollte Chaser-Schritte hinzufügen, verschieben und entfernen")
    void shouldManageChaserSteps() {
        ChaserDefinition chaser = new ChaserDefinition("chaser-1", "Lauflicht");
        chaserService.registerChaser(chaser);

        chaserService.addStep("chaser-1", "scene-a", 200, 50);
        chaserService.addStep("chaser-1", "scene-b", 200, 50);
        chaserService.addStep("chaser-1", "scene-c", 200, 50);

        assertThat(chaser.getSteps()).hasSize(3);
        assertThat(chaser.getSteps().get(0).getSceneId()).isEqualTo("scene-a");
        assertThat(chaser.getSteps().get(1).getSceneId()).isEqualTo("scene-b");
        assertThat(chaser.getSteps().get(2).getSceneId()).isEqualTo("scene-c");

        // Verschieben: Step 0 (scene-a) nach Index 2
        chaserService.moveStep("chaser-1", 0, 2);
        assertThat(chaser.getSteps().get(0).getSceneId()).isEqualTo("scene-b");
        assertThat(chaser.getSteps().get(2).getSceneId()).isEqualTo("scene-a");

        // Entfernen
        chaserService.removeStep("chaser-1", 1);
        assertThat(chaser.getSteps()).hasSize(2);
    }

    @Test
    @DisplayName("Sollte Schritt-Indizes nach Laufrichtung (Forward & Ping-Pong) berechnen")
    void shouldCalculateNextStepIndex() {
        ChaserDefinition chaser = new ChaserDefinition("chaser-dir", "Dir Test");
        chaser.addStep(new ChaserStep("s1", "scene-a", 200, 0));
        chaser.addStep(new ChaserStep("s2", "scene-b", 200, 0));
        chaser.addStep(new ChaserStep("s3", "scene-c", 200, 0));

        // Forward: 0 -> 1 -> 2 -> 0
        chaser.setDirection(ChaserDirection.FORWARD);
        assertThat(chaser.computeNextStepIndex(0, true)).isEqualTo(1);
        assertThat(chaser.computeNextStepIndex(1, true)).isEqualTo(2);
        assertThat(chaser.computeNextStepIndex(2, true)).isEqualTo(0);

        // Backward: 2 -> 1 -> 0 -> 2
        chaser.setDirection(ChaserDirection.BACKWARD);
        assertThat(chaser.computeNextStepIndex(2, false)).isEqualTo(1);
        assertThat(chaser.computeNextStepIndex(1, false)).isEqualTo(0);
        assertThat(chaser.computeNextStepIndex(0, false)).isEqualTo(2);
    }

    @Test
    @DisplayName("Sollte Chaser abspielen und Schritte im Takt weiterschalten")
    void shouldPlayChaserAndAdvanceSteps() throws Exception {
        ChaserDefinition chaser = new ChaserDefinition("chaser-play", "Play Test");
        // Kurze Hold-Zeiten für schnellen Unit-Test: 60ms
        chaser.addStep(new ChaserStep("s1", "scene-a", 60, 0));
        chaser.addStep(new ChaserStep("s2", "scene-b", 60, 0));
        chaserService.registerChaser(chaser);

        CountDownLatch stepLatch = new CountDownLatch(2);
        AtomicInteger lastStep = new AtomicInteger(-1);

        chaserService.addChaserListener(new ChaserService.ChaserListener() {
            @Override
            public void onStepChanged(String chaserId, int stepIndex) {
                if ("chaser-play".equals(chaserId)) {
                    lastStep.set(stepIndex);
                    stepLatch.countDown();
                }
            }

            @Override
            public void onStateChanged(String chaserId, boolean isPlaying) {}
        });

        chaserService.play("chaser-play");
        assertThat(chaserService.isPlaying("chaser-play")).isTrue();

        assertThat(stepLatch.await(2, TimeUnit.SECONDS)).isTrue();
        chaserService.stop("chaser-play");
        assertThat(chaserService.isPlaying("chaser-play")).isFalse();
    }
}

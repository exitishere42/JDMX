package de.exit.jdmx.ui;

import de.exit.jdmx.core.model.ExplorerItem;
import de.exit.jdmx.core.model.ExplorerItemType;
import de.exit.jdmx.core.model.FixtureDefinition;
import de.exit.jdmx.core.model.FixtureInstance;
import de.exit.jdmx.core.project.model.FixtureDataDto;
import de.exit.jdmx.core.project.model.ProjectData;
import de.exit.jdmx.core.project.model.UniverseDataDto;
import de.exit.jdmx.service.DmxService;
import de.exit.jdmx.service.ShortcutService;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests für die Snapshot- und Wiederherstellungslogik von Projekten in {@link MainWindow}.
 */
class MainWindowProjectPersistenceTest {

    @BeforeAll
    static void initJavaFx() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            // Bereits initialisiert
            latch.countDown();
        }
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("Soll buildCurrentProjectData und applyProjectData konsistent ausführen")
    void shouldBuildAndApplyProjectDataConsistently() throws InterruptedException {
        CountDownLatch testLatch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                MainWindow window = new MainWindow(new ShortcutService(), new DmxService());

                // Test-Projektdaten anwenden (Universum 1)
                UniverseDataDto u0 = UniverseDataDto.of(0, "Kein Input", "Art-Net Output", "192.168.1.42", 2, 0, 0);
                FixtureDataDto f0 = new FixtureDataDto(
                        "fix-test-1", "Generic Dimmer Test", "generic-dimmer", "Generic", "Generic Dimmer", "1-Channel",
                        1, 5, 200.0, 150.0
                );

                ProjectData testProject = new ProjectData(
                        ProjectData.CURRENT_VERSION,
                        "SnapshotTest",
                        "2026-11-06T20:00:00Z",
                        List.of(u0),
                        List.of(f0)
                );

                window.applyProjectData(testProject);

                // Prüfen ob DmxService die Konfiguration übernommen hat
                assertThat(window.getDmxService().getUniverseConfig(0).artNetIp()).isEqualTo("192.168.1.42");
                assertThat(window.getDmxService().getUniverseConfig(0).artNetUniverse()).isEqualTo(2);

                // Snapshot erstellen
                ProjectData snapshot = window.buildCurrentProjectData();
                assertThat(snapshot).isNotNull();
                assertThat(snapshot.universes()).isNotEmpty();
                assertThat(snapshot.universes().get(0).artNetIp()).isEqualTo("192.168.1.42");
                assertThat(snapshot.fixtures()).hasSize(1);
                assertThat(snapshot.fixtures().get(0).name()).isEqualTo("Generic Dimmer Test");
                assertThat(snapshot.fixtures().get(0).stageX()).isEqualTo(200.0);
                assertThat(snapshot.fixtures().get(0).stageY()).isEqualTo(150.0);
            } finally {
                testLatch.countDown();
            }
        });

        assertThat(testLatch.await(5, TimeUnit.SECONDS)).isTrue();
    }
}

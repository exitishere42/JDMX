package de.exit.jdmx.ui;

import de.exit.jdmx.core.model.ExplorerItem;
import de.exit.jdmx.core.model.ExplorerItemType;
import de.exit.jdmx.core.model.FixtureDefinition;
import de.exit.jdmx.core.model.FixtureInstance;
import de.exit.jdmx.core.model.SceneDefinition;
import de.exit.jdmx.core.model.ChaserDefinition;
import de.exit.jdmx.core.project.model.ChaserDataDto;
import de.exit.jdmx.core.project.model.FixtureDataDto;
import de.exit.jdmx.core.project.model.ProjectData;
import de.exit.jdmx.core.project.model.SceneDataDto;
import de.exit.jdmx.core.project.model.SettingsDataDto;
import de.exit.jdmx.core.project.model.UniverseDataDto;
import de.exit.jdmx.service.DmxService;
import de.exit.jdmx.service.ProjectStorageService;
import de.exit.jdmx.service.ShortcutService;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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

    @Test
    @DisplayName("Soll MainWindow ohne default.jdmx initialisieren und mit currentProjectFile == null starten")
    void shouldStartWithNullProjectFileAndNoDefaultFile() throws InterruptedException {
        CountDownLatch testLatch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                MainWindow window = new MainWindow(new ShortcutService(), new DmxService());
                assertThat(window.getCurrentProjectFile()).isNull();

                // AutoSave bei unbenanntem Projekt darf keine Datei anlegen
                window.autoSave();
                assertThat(window.getCurrentProjectFile()).isNull();
            } finally {
                testLatch.countDown();
            }
        });

        assertThat(testLatch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("Soll bei newProject alle Services vollständig leeren und currentProjectFile auf null setzen")
    void shouldCleanAllServicesOnNewProject() throws InterruptedException {
        CountDownLatch testLatch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                MainWindow window = new MainWindow(new ShortcutService(), new DmxService());

                // Szenen und Chaser hinzufügen
                window.getStageWorkspaceView().getSceneService().captureScene("sc-1", "Test-Szene", 200, List.of());
                window.getStageWorkspaceView().getChaserService().createChaser("ch-1", "Test-Chaser", 120.0);
                assertThat(window.getStageWorkspaceView().getSceneService().getAllScenes()).isNotEmpty();
                assertThat(window.getStageWorkspaceView().getChaserService().getAllChasers()).isNotEmpty();

                // newProject aufrufen
                window.newProject();

                assertThat(window.getCurrentProjectFile()).isNull();
                assertThat(window.getStageWorkspaceView().getSceneService().getAllScenes()).isEmpty();
                assertThat(window.getStageWorkspaceView().getChaserService().getAllChasers()).isEmpty();
                assertThat(window.getStageWorkspaceView().getStageCanvasView().getPlacedFixtures()).isEmpty();
            } finally {
                testLatch.countDown();
            }
        });

        assertThat(testLatch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("Soll beim Laden eines Projekts vorherige Szenen und Chaser restlos entfernen")
    void shouldCleanPreviousStateWhenOpeningNewProject(@TempDir Path tempDir) throws Exception {
        CountDownLatch testLatch = new CountDownLatch(1);
        File projectFile = tempDir.resolve("sample.jdmx").toFile();

        SceneDataDto sNew = new SceneDataDto("sc-new", "Frische Szene", 100, Map.of(0, Map.of(1, 255)));
        ProjectData projectData = new ProjectData(
                ProjectData.CURRENT_VERSION,
                "SampleProject",
                "2026-11-10T12:00:00Z",
                List.of(),
                List.of(),
                List.of(),
                List.of(sNew),
                List.of()
        );
        new ProjectStorageService().saveProject(projectData, projectFile);

        Platform.runLater(() -> {
            try {
                MainWindow window = new MainWindow(new ShortcutService(), new DmxService());

                // Altzustand mit alten Szenen belegen
                window.getStageWorkspaceView().getSceneService().captureScene("sc-old", "Alte Szene", 500, List.of());
                assertThat(window.getStageWorkspaceView().getSceneService().getScene("sc-old")).isNotNull();

                // Projekt laden
                window.loadProject(projectFile);

                assertThat(window.getCurrentProjectFile()).isEqualTo(projectFile);
                assertThat(window.getStageWorkspaceView().getSceneService().getScene("sc-old")).isNull();
                assertThat(window.getStageWorkspaceView().getSceneService().getScene("sc-new")).isNotNull();
            } finally {
                testLatch.countDown();
            }
        });

        assertThat(testLatch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("Soll Profileinstellungen (Sprache, Fullscreen, Shortcuts) im Projekt speichern und laden")
    void shouldPersistAndRestoreSettingsInMainWindow(@TempDir Path tempDir) throws Exception {
        CountDownLatch testLatch = new CountDownLatch(1);
        File projectFile = tempDir.resolve("profile_test.jdmx").toFile();

        Platform.runLater(() -> {
            try {
                MainWindow window = new MainWindow(new ShortcutService(), new DmxService());

                // Einstellungen anpassen
                window.getSettingsService().setLanguage("en");
                window.getSettingsService().setFullscreen(true);
                window.getSettingsService().setConfirmExit(false);
                window.getSettingsService().setAutoSaveInterval("Alle 5 Minuten");
                window.getSettingsService().setDefaultFolder("D:\\Shows");
                window.getShortcutService().setShortcut(
                        de.exit.jdmx.service.ShortcutAction.SAVE_PROJECT,
                        new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.F9)
                );

                // Projekt speichern
                window.saveCurrentProject(projectFile);
                assertThat(projectFile).exists().isNotEmpty();

                // Auf neues leeres Projekt zurücksetzen
                window.newProject();
                assertThat(window.getSettingsService().isFullscreen()).isFalse();

                // Projekt wieder laden
                window.loadProject(projectFile);

                assertThat(window.getSettingsService().getLanguage()).isEqualTo("en");
                assertThat(window.getSettingsService().isFullscreen()).isTrue();
                assertThat(window.getSettingsService().isConfirmExit()).isFalse();
                assertThat(window.getSettingsService().getAutoSaveInterval()).isEqualTo("Alle 5 Minuten");
                assertThat(window.getSettingsService().getDefaultFolder()).isEqualTo("D:\\Shows");
                assertThat(window.getShortcutService().getDisplayText(de.exit.jdmx.service.ShortcutAction.SAVE_PROJECT)).contains("F9");
            } finally {
                testLatch.countDown();
            }
        });

        assertThat(testLatch.await(5, TimeUnit.SECONDS)).isTrue();
    }
}

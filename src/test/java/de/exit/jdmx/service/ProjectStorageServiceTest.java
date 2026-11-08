package de.exit.jdmx.service;

import de.exit.jdmx.core.project.model.ChaserDataDto;
import de.exit.jdmx.core.project.model.ChaserStepDto;
import de.exit.jdmx.core.project.model.FixtureDataDto;
import de.exit.jdmx.core.project.model.GroupDataDto;
import de.exit.jdmx.core.project.model.ProjectData;
import de.exit.jdmx.core.project.model.SceneDataDto;
import de.exit.jdmx.core.project.model.UniverseDataDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit-Tests für {@link ProjectStorageService} nach TDD-Vorgabe.
 */
class ProjectStorageServiceTest {

    @Test
    @DisplayName("Soll Projektdatei korrekt speichern und wieder deserialisieren")
    void shouldSaveAndLoadProjectFile(@TempDir Path tempDir) throws IOException {
        ProjectStorageService service = new ProjectStorageService();
        File targetFile = tempDir.resolve("test_project.jdmx").toFile();

        UniverseDataDto uni1 = UniverseDataDto.of(0, "Art-Net", "Art-Net Output", "192.168.1.100", 0, 0, 0);
        FixtureDataDto fix1 = new FixtureDataDto(
                "fix-1", "Moving Head Links", "uking-mh", "U`King", "Spot 25W", "11Ch",
                0, 1, 150.0, 220.5
        );

        ProjectData project = new ProjectData(
                ProjectData.CURRENT_VERSION,
                "Bühnen-Setup OpenAir",
                "2026-11-06T20:00:00Z",
                List.of(uni1),
                List.of(fix1)
        );

        service.saveProject(project, targetFile);
        assertThat(targetFile).exists().isNotEmpty();

        ProjectData loaded = service.loadProject(targetFile);
        assertThat(loaded).isNotNull();
        assertThat(loaded.version()).isEqualTo(ProjectData.CURRENT_VERSION);
        assertThat(loaded.projectName()).isEqualTo("Bühnen-Setup OpenAir");
        assertThat(loaded.universes()).hasSize(1);
        assertThat(loaded.universes().get(0).artNetIp()).isEqualTo("192.168.1.100");
        assertThat(loaded.fixtures()).hasSize(1);
        assertThat(loaded.fixtures().get(0).name()).isEqualTo("Moving Head Links");
        assertThat(loaded.fixtures().get(0).stageX()).isEqualTo(150.0);
        assertThat(loaded.fixtures().get(0).stageY()).isEqualTo(220.5);
    }

    @Test
    @DisplayName("Soll leeres Projekt mit createEmpty erstellen und speichern können")
    void shouldHandleEmptyProject(@TempDir Path tempDir) throws IOException {
        ProjectStorageService service = new ProjectStorageService();
        File targetFile = tempDir.resolve("empty.jdmx").toFile();

        ProjectData empty = ProjectData.createEmpty("Neues Projekt");
        service.saveProject(empty, targetFile);

        ProjectData loaded = service.loadProject(targetFile);
        assertThat(loaded.projectName()).isEqualTo("Neues Projekt");
        assertThat(loaded.universes()).isEmpty();
        assertThat(loaded.fixtures()).isEmpty();
    }

    @Test
    @DisplayName("Soll bei fehlerhafter Datei entsprechende Exception werfen")
    void shouldThrowOnCorruptedFile(@TempDir Path tempDir) {
        ProjectStorageService service = new ProjectStorageService();
        File brokenFile = tempDir.resolve("broken.jdmx").toFile();

        assertThatThrownBy(() -> service.loadProject(brokenFile))
                .isInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("Soll Projekt mit Szenen und Chasern korrekt serialisieren und deserialisieren")
    void shouldSaveAndLoadProjectWithScenesAndChasers(@TempDir Path tempDir) throws IOException {
        ProjectStorageService service = new ProjectStorageService();
        File targetFile = tempDir.resolve("scenes_chasers_project.jdmx").toFile();

        SceneDataDto scene1 = new SceneDataDto("scene-1", "Intro Warm", 1500, java.util.Map.of(0, java.util.Map.of(1, 255, 2, 128)));
        ChaserStepDto step1 = new ChaserStepDto("step-1", "scene-1", 500, 200);
        ChaserDataDto chaser1 = new ChaserDataDto("chaser-1", "Color Wave", 130.0, "FORWARD", List.of(step1));

        ProjectData project = new ProjectData(
                ProjectData.CURRENT_VERSION,
                "Show 2026",
                "2026-11-08T12:00:00Z",
                List.of(),
                List.of(),
                List.of(),
                List.of(scene1),
                List.of(chaser1)
        );

        service.saveProject(project, targetFile);
        assertThat(targetFile).exists().isNotEmpty();

        ProjectData loaded = service.loadProject(targetFile);
        assertThat(loaded.scenes()).hasSize(1);
        assertThat(loaded.scenes().get(0).name()).isEqualTo("Intro Warm");
        assertThat(loaded.scenes().get(0).channelValues().get(0).get(1)).isEqualTo(255);

        assertThat(loaded.chasers()).hasSize(1);
        assertThat(loaded.chasers().get(0).name()).isEqualTo("Color Wave");
        assertThat(loaded.chasers().get(0).bpm()).isEqualTo(130.0);
        assertThat(loaded.chasers().get(0).steps()).hasSize(1);
        assertThat(loaded.chasers().get(0).steps().get(0).sceneId()).isEqualTo("scene-1");
    }

    @Test
    @DisplayName("Soll valides Verzeichnis für Projektdateien ermitteln")
    void shouldResolveValidProjectsDirectory() {
        ProjectStorageService service = new ProjectStorageService();
        File dir = service.getProjectsDirectory();
        assertThat(dir).isNotNull();
        assertThat(dir.exists() || dir.mkdirs()).isTrue();
    }
}

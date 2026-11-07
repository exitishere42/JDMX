package de.exit.jdmx.core.model;

import de.exit.jdmx.core.project.model.GroupDataDto;
import de.exit.jdmx.core.project.model.ProjectData;
import de.exit.jdmx.service.ProjectStorageService;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests für die Gerätegruppen-Logik im Datenmodell und der Projekt-Persistenz.
 */
class ExplorerGroupTest {

    @BeforeAll
    static void initJavaFx() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            latch.countDown();
        }
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("Sollte Member-Fixture-IDs in ExplorerItem verwalten können")
    void shouldManageMemberFixtureIdsInExplorerItem() {
        ExplorerItem group = new ExplorerItem("grp-1", "Moving Heads", ExplorerItemType.GROUP, false, "layers", Color.GREEN);

        assertThat(group.getMemberFixtureIds()).isEmpty();

        group.addMemberFixtureId("f1");
        group.addMemberFixtureId("f2");

        assertThat(group.getMemberFixtureIds()).containsExactly("f1", "f2");
        assertThat(group.hasMemberFixture("f1")).isTrue();
        assertThat(group.hasMemberFixture("f3")).isFalse();

        group.removeMemberFixtureId("f1");
        assertThat(group.getMemberFixtureIds()).containsExactly("f2");
        assertThat(group.hasMemberFixture("f1")).isFalse();
    }

    @Test
    @DisplayName("Sollte Gerätegruppen in ProjectData JSON speichern und laden können")
    void shouldPersistAndLoadGroupsInProjectData(@TempDir File tempDir) throws Exception {
        ProjectStorageService storage = new ProjectStorageService(tempDir);
        File projectFile = new File(tempDir, "group_test.jdmx");

        GroupDataDto groupDto = new GroupDataDto("grp-1", "Frontlichter", List.of("f1", "f2", "f3"));
        ProjectData data = new ProjectData(
                ProjectData.CURRENT_VERSION,
                "Group Project",
                java.time.Instant.now().toString(),
                List.of(),
                List.of(),
                List.of(groupDto)
        );

        storage.saveProject(data, projectFile);

        ProjectData loaded = storage.loadProject(projectFile);
        assertThat(loaded.groups()).isNotNull().hasSize(1);
        GroupDataDto loadedGroup = loaded.groups().get(0);
        assertThat(loadedGroup.id()).isEqualTo("grp-1");
        assertThat(loadedGroup.name()).isEqualTo("Frontlichter");
        assertThat(loadedGroup.fixtureIds()).containsExactly("f1", "f2", "f3");
    }
}

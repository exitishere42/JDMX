package de.exit.jdmx.ui.stage;

import de.exit.jdmx.core.model.ChannelDefinition;
import de.exit.jdmx.core.model.ChannelType;
import de.exit.jdmx.core.model.DmxMode;
import de.exit.jdmx.core.model.ExplorerItem;
import de.exit.jdmx.core.model.ExplorerItemType;
import de.exit.jdmx.core.model.FixtureDefinition;
import de.exit.jdmx.core.model.FixtureInstance;
import de.exit.jdmx.service.ProjectExplorerService;
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
 * Tests für die bidirektionale Selektions-Synchronisation zwischen der 2D-Bühne
 * und dem Project Explorer.
 */
class BidirectionalSelectionTest {

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

    private StageFixtureNode createFixture(String id, String name, double x, double y) {
        ChannelDefinition ch = new ChannelDefinition(1, "Dimmer", ChannelType.DIMMER, 0, false);
        DmxMode mode = new DmxMode("1CH", List.of(ch));
        FixtureDefinition def = new FixtureDefinition("generic-dimmer", "Generic", "Dimmer", ExplorerItemType.FIXTURE, List.of(mode));
        FixtureInstance instance = new FixtureInstance(id, name, def, mode, 1, 1, x, y);
        ExplorerItem item = new ExplorerItem(id, name, ExplorerItemType.FIXTURE, false, "lightbulb", Color.WHITE, instance);
        return new StageFixtureNode(item, x, y);
    }

    @Test
    @DisplayName("Soll Fixture auf Canvas selektieren, wenn es im Project Explorer ausgewählt wird")
    void shouldSelectFixtureOnCanvasWhenSelectedInExplorer() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();

        Platform.runLater(() -> {
            try {
                ProjectExplorerService service = new ProjectExplorerService();
                StageWorkspaceView workspace = new StageWorkspaceView(service);
                ProjectExplorerView explorer = workspace.getProjectExplorerView();
                StageCanvasView canvas = workspace.getStageCanvasView();

                StageFixtureNode node1 = createFixture("f1", "Spot 1", 50, 50);
                StageFixtureNode node2 = createFixture("f2", "Spot 2", 150, 50);

                service.addItemToFolder(ProjectExplorerService.FOLDER_DEVICES_ID, node1.getItem());
                service.addItemToFolder(ProjectExplorerService.FOLDER_DEVICES_ID, node2.getItem());

                canvas.addFixtureNode(node1);
                canvas.addFixtureNode(node2);

                explorer.rebuildTree();

                // Im Explorer Node 1 auswählen
                explorer.selectItem("f1");

                assertThat(node1.isSelected()).isTrue();
                assertThat(node2.isSelected()).isFalse();

                // Im Explorer Node 2 auswählen
                explorer.selectItem("f2");

                assertThat(node1.isSelected()).isFalse();
                assertThat(node2.isSelected()).isTrue();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        if (error.get() != null) throw error.get();
    }

    @Test
    @DisplayName("Soll TreeItem im Explorer selektieren, wenn Fixture auf der Bühne ausgewählt wird")
    void shouldSelectTreeItemInExplorerWhenFixtureSelectedOnCanvas() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();

        Platform.runLater(() -> {
            try {
                ProjectExplorerService service = new ProjectExplorerService();
                StageWorkspaceView workspace = new StageWorkspaceView(service);
                ProjectExplorerView explorer = workspace.getProjectExplorerView();
                StageCanvasView canvas = workspace.getStageCanvasView();

                StageFixtureNode node1 = createFixture("f1", "Spot 1", 50, 50);
                service.addItemToFolder(ProjectExplorerService.FOLDER_DEVICES_ID, node1.getItem());
                canvas.addFixtureNode(node1);

                explorer.rebuildTree();

                // Auf der Bühne selektieren
                node1.setSelected(true);

                // Im Explorer TreeView muss Node 1 selektiert sein
                assertThat(explorer.getTreeView().getSelectionModel().getSelectedItem()).isNotNull();
                assertThat(explorer.getTreeView().getSelectionModel().getSelectedItem().getValue().getId()).isEqualTo("f1");
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        if (error.get() != null) throw error.get();
    }

    @Test
    @DisplayName("Soll alle Gruppen-Fixtures auf der Bühne selektieren, wenn die Gruppe im Explorer ausgewählt wird")
    void shouldSelectAllGroupFixturesWhenGroupSelectedInExplorer() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();

        Platform.runLater(() -> {
            try {
                ProjectExplorerService service = new ProjectExplorerService();
                StageWorkspaceView workspace = new StageWorkspaceView(service);
                ProjectExplorerView explorer = workspace.getProjectExplorerView();
                StageCanvasView canvas = workspace.getStageCanvasView();

                StageFixtureNode node1 = createFixture("f1", "Spot 1", 50, 50);
                StageFixtureNode node2 = createFixture("f2", "Spot 2", 150, 50);
                StageFixtureNode node3 = createFixture("f3", "Spot 3", 250, 50);

                service.addItemToFolder(ProjectExplorerService.FOLDER_DEVICES_ID, node1.getItem());
                service.addItemToFolder(ProjectExplorerService.FOLDER_DEVICES_ID, node2.getItem());
                service.addItemToFolder(ProjectExplorerService.FOLDER_DEVICES_ID, node3.getItem());

                canvas.addFixtureNode(node1);
                canvas.addFixtureNode(node2);
                canvas.addFixtureNode(node3);

                // Gruppe anlegen mit f1 und f2
                ExplorerItem group = new ExplorerItem("grp-1", "Frontlicht", ExplorerItemType.GROUP, false, "layers", Color.GREEN);
                group.setMemberFixtureIds(List.of("f1", "f2"));
                service.addItemToFolder(ProjectExplorerService.FOLDER_GROUPS_ID, group);

                explorer.rebuildTree();

                // Gruppe im Explorer auswählen
                explorer.selectItem("grp-1");

                // node1 und node2 müssen selektiert sein, node3 nicht
                assertThat(node1.isSelected()).isTrue();
                assertThat(node2.isSelected()).isTrue();
                assertThat(node3.isSelected()).isFalse();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        if (error.get() != null) throw error.get();
    }
}

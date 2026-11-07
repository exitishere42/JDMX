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
import javafx.scene.control.TreeItem;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für die synchrone Deselektion (Bühne <-> Explorer)
 * sowie die hierarchische Darstellung von Fixtures innerhalb von Gerätegruppen.
 */
class DeselectionAndGroupHierarchyTest {

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
        ExplorerItem item = new ExplorerItem(id, name, ExplorerItemType.FIXTURE, false, "lamp", Color.WHITE, instance);
        return new StageFixtureNode(item, x, y);
    }

    @Test
    @DisplayName("Soll Explorer-Selektion aufheben, wenn auf der Bühne deselektiert wird")
    void shouldClearExplorerSelectionWhenStageDeselected() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                ProjectExplorerService service = new ProjectExplorerService();
                StageWorkspaceView workspace = new StageWorkspaceView(service);
                ProjectExplorerView explorer = workspace.getProjectExplorerView();
                StageCanvasView canvas = workspace.getStageCanvasView();

                StageFixtureNode node = createFixture("f1", "Moving Head", 50, 50);
                service.addItemToFolder(ProjectExplorerService.FOLDER_DEVICES_ID, node.getItem());
                canvas.addFixtureNode(node);
                explorer.rebuildTree();

                // 1. Auswählen im Explorer
                explorer.selectItem("f1");
                assertThat(node.isSelected()).isTrue();
                assertThat(explorer.getTreeView().getSelectionModel().getSelectedItem()).isNotNull();

                // 2. Auf der Bühne deselektieren (z. B. Klick auf leere Fläche)
                canvas.deselectAll();

                // Explorer muss deselektiert sein
                assertThat(explorer.getTreeView().getSelectionModel().getSelectedItem()).isNull();
                assertThat(node.isSelected()).isFalse();
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
    @DisplayName("Soll Bühnen-Fixtures deselektieren, wenn im Explorer die Selektion geleert wird")
    void shouldDeselectStageFixturesWhenExplorerSelectionCleared() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                ProjectExplorerService service = new ProjectExplorerService();
                StageWorkspaceView workspace = new StageWorkspaceView(service);
                ProjectExplorerView explorer = workspace.getProjectExplorerView();
                StageCanvasView canvas = workspace.getStageCanvasView();

                StageFixtureNode node = createFixture("f1", "Moving Head", 50, 50);
                service.addItemToFolder(ProjectExplorerService.FOLDER_DEVICES_ID, node.getItem());
                canvas.addFixtureNode(node);
                explorer.rebuildTree();

                // 1. Auf der Bühne auswählen
                node.setSelected(true);
                assertThat(explorer.getTreeView().getSelectionModel().getSelectedItem()).isNotNull();

                // 2. Im Explorer Selektion aufheben (z. B. Klick auf leere Baumfläche)
                explorer.getTreeView().getSelectionModel().clearSelection();

                // Bühne muss deselektiert sein
                assertThat(node.isSelected()).isFalse();
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
    @DisplayName("Soll Fixtures als Unterelemente innerhalb einer Gerätegruppe im Explorer anzeigen")
    void shouldDisplayMemberFixturesInsideGroupTreeItem() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                ProjectExplorerService service = new ProjectExplorerService();
                StageWorkspaceView workspace = new StageWorkspaceView(service);
                ProjectExplorerView explorer = workspace.getProjectExplorerView();
                StageCanvasView canvas = workspace.getStageCanvasView();

                StageFixtureNode node1 = createFixture("f1", "MH 1", 50, 50);
                StageFixtureNode node2 = createFixture("f2", "MH 2", 150, 50);
                service.addItemToFolder(ProjectExplorerService.FOLDER_DEVICES_ID, node1.getItem());
                service.addItemToFolder(ProjectExplorerService.FOLDER_DEVICES_ID, node2.getItem());
                canvas.addFixtureNode(node1);
                canvas.addFixtureNode(node2);

                // Gruppe erstellen mit f1 und f2
                ExplorerItem group = new ExplorerItem("grp-1", "Moving Heads", ExplorerItemType.GROUP, false, "layers", Color.GREEN);
                group.setMemberFixtureIds(List.of("f1", "f2"));
                service.addItemToFolder(ProjectExplorerService.FOLDER_GROUPS_ID, group);

                explorer.rebuildTree();

                // Baum analysieren: FOLDER_GROUPS_ID suchen
                TreeItem<ExplorerItem> groupsFolder = null;
                for (TreeItem<ExplorerItem> rootChild : explorer.getRootItem().getChildren()) {
                    if (rootChild.getValue() != null && ProjectExplorerService.FOLDER_GROUPS_ID.equals(rootChild.getValue().getId())) {
                        groupsFolder = rootChild;
                        break;
                    }
                }
                assertThat(groupsFolder).isNotNull();
                assertThat(groupsFolder.getChildren()).hasSize(1);

                TreeItem<ExplorerItem> groupNode = groupsFolder.getChildren().getFirst();
                assertThat(groupNode.getValue().getName()).isEqualTo("Moving Heads");

                // Gruppe muss als Kinder die beiden Fixtures enthalten
                assertThat(groupNode.getChildren()).hasSize(2);
                assertThat(groupNode.getChildren().get(0).getValue().getId()).isEqualTo("f1");
                assertThat(groupNode.getChildren().get(1).getValue().getId()).isEqualTo("f2");

                // Klick auf Unterelement in Gruppe selektiert das entsprechende Fixture auf der Bühne
                explorer.getTreeView().getSelectionModel().select(groupNode.getChildren().get(0));
                assertThat(node1.isSelected()).isTrue();
                assertThat(node2.isSelected()).isFalse();
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

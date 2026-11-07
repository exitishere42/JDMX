package de.exit.jdmx.ui;

import de.exit.jdmx.core.model.ChannelDefinition;
import de.exit.jdmx.core.model.ChannelType;
import de.exit.jdmx.core.model.DmxMode;
import de.exit.jdmx.core.model.ExplorerItem;
import de.exit.jdmx.core.model.ExplorerItemType;
import de.exit.jdmx.core.model.FixtureDefinition;
import de.exit.jdmx.core.model.FixtureInstance;
import de.exit.jdmx.service.DmxService;
import de.exit.jdmx.service.ProjectExplorerService;
import de.exit.jdmx.service.ShortcutService;
import de.exit.jdmx.ui.stage.StageFixtureNode;
import javafx.application.Platform;
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
 * Integrationstests für Undo und Redo im {@link MainWindow}.
 */
class MainWindowUndoRedoTest {

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
    @DisplayName("Soll Undo und Redo bei Scheinwerfer-Aktionen ausführen")
    void shouldUndoAndRedoFixtureActions() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                MainWindow window = new MainWindow(new ShortcutService(), new DmxService());
                int initialCount = window.getStageWorkspaceView().getStageCanvasView().getPlacedFixtures().size();

                // 1. Snapshot vor dem Hinzufügen aufnehmen
                window.recordUndoSnapshot();

                StageFixtureNode node = createFixture("f-undo-1", "Test Spot", 100, 100);
                window.getStageWorkspaceView().getExplorerService().addItemToFolder(ProjectExplorerService.FOLDER_DEVICES_ID, node.getItem());
                window.getStageWorkspaceView().getStageCanvasView().addFixtureNode(node);

                assertThat(window.getUndoRedoService().canUndo()).isTrue();
                assertThat(window.getStageWorkspaceView().getStageCanvasView().getPlacedFixtures()).hasSize(initialCount + 1);

                // 2. Undo ausführen -> Scheinwerfer muss wieder weg sein
                window.undo();
                assertThat(window.getStageWorkspaceView().getStageCanvasView().getPlacedFixtures()).hasSize(initialCount);
                assertThat(window.getUndoRedoService().canRedo()).isTrue();

                // 3. Redo ausführen -> Scheinwerfer muss wieder da sein
                window.redo();
                assertThat(window.getStageWorkspaceView().getStageCanvasView().getPlacedFixtures()).hasSize(initialCount + 1);
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

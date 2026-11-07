package de.exit.jdmx.ui.stage;

import de.exit.jdmx.core.model.ExplorerItem;
import de.exit.jdmx.core.model.ExplorerItemType;
import de.exit.jdmx.service.ProjectExplorerService;
import de.exit.jdmx.ui.menu.ExplorerItemContextMenu;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests für das Kontextmenü-Verhalten der TreeCells im Project Explorer.
 */
class ProjectExplorerContextMenuTest {

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
    @DisplayName("Sollte für normale Items Kontextmenü öffnen und Aktionen verdrahten")
    void shouldOpenContextMenuForNormalItems() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                ProjectExplorerService service = new ProjectExplorerService();
                ProjectExplorerView explorerView = new ProjectExplorerView(service);

                ExplorerItem item = new ExplorerItem("spot-1", "Spot 1", ExplorerItemType.FIXTURE, false, "lamp", Color.BLUE);
                service.addItemToFolder("folder-devices", item);

                AtomicBoolean renameCalled = new AtomicBoolean(false);
                AtomicBoolean deleteCalled = new AtomicBoolean(false);

                explorerView.setOnItemRenameRequested(it -> renameCalled.set(true));
                explorerView.setOnItemDeleteRequested(it -> deleteCalled.set(true));

                ProjectExplorerView.ProjectExplorerTreeCell cell = new ProjectExplorerView.ProjectExplorerTreeCell(service, explorerView);
                cell.updateItem(item, false);

                // Kontextmenü anfordern
                Event.fireEvent(cell, new ContextMenuEvent(
                        ContextMenuEvent.CONTEXT_MENU_REQUESTED,
                        10.0, 10.0, 100.0, 100.0, false, null));

                ExplorerItemContextMenu contextMenu = cell.getCurrentContextMenu();
                assertThat(contextMenu).isNotNull();

                // Test: Klick auf Umbenennen
                contextMenu.getItemButton("rename").fire();
                assertThat(renameCalled.get()).isTrue();

                // Test: Klick auf Entfernen
                contextMenu.getItemButton("delete").fire();
                assertThat(deleteCalled.get()).isTrue();
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("Sollte für geschützte Standard-Ordner kein Kontextmenü öffnen")
    void shouldNotOpenContextMenuForStandardFolders() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                ProjectExplorerService service = new ProjectExplorerService();
                ProjectExplorerView explorerView = new ProjectExplorerView(service);

                ExplorerItem folder = service.getStandardFolders().get(0);

                ProjectExplorerView.ProjectExplorerTreeCell cell = new ProjectExplorerView.ProjectExplorerTreeCell(service, explorerView);
                cell.updateItem(folder, false);

                Event.fireEvent(cell, new ContextMenuEvent(
                        ContextMenuEvent.CONTEXT_MENU_REQUESTED,
                        10.0, 10.0, 100.0, 100.0, false, null));

                assertThat(cell.getCurrentContextMenu()).isNull();
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("Sollte Item bei Löschen ohne Custom-Callback direkt aus Service und Canvas entfernen")
    void shouldRemoveItemFromServiceAndCanvasByDefault() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                ProjectExplorerService service = new ProjectExplorerService();
                StageCanvasView canvas = new StageCanvasView(service);
                ProjectExplorerView explorerView = new ProjectExplorerView(service);
                explorerView.setStageCanvasView(canvas);

                ExplorerItem item = new ExplorerItem("spot-1", "Spot 1", ExplorerItemType.FIXTURE, false, "lamp", Color.BLUE);
                service.addItemToFolder("folder-devices", item);
                canvas.placeFixture(item, 50, 50);

                assertThat(service.getItemsInFolder("folder-devices")).hasSize(1);
                assertThat(canvas.getPlacedFixtures()).hasSize(1);

                ProjectExplorerView.ProjectExplorerTreeCell cell = new ProjectExplorerView.ProjectExplorerTreeCell(service, explorerView);
                cell.updateItem(item, false);

                Event.fireEvent(cell, new ContextMenuEvent(
                        ContextMenuEvent.CONTEXT_MENU_REQUESTED,
                        10.0, 10.0, 100.0, 100.0, false, null));

                ExplorerItemContextMenu contextMenu = cell.getCurrentContextMenu();
                assertThat(contextMenu).isNotNull();

                contextMenu.getItemButton("delete").fire();

                // Item muss aus dem Service und der Canvas verschwunden sein!
                assertThat(service.getItemsInFolder("folder-devices")).isEmpty();
                assertThat(canvas.getPlacedFixtures()).isEmpty();
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }
}

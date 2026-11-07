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
import javafx.event.Event;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für Tastatur-Aktionen (Löschen mit Entf / Backspace) auf der 2D-Bühne
 * und im Project Explorer.
 */
class StageKeyboardInteractionTest {

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
    @DisplayName("Soll selektierte Fixtures auf der Bühne bei Druck auf die Taste ENTF (DELETE) löschen")
    void shouldDeleteSelectedFixturesOnDeleteKey() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();

        Platform.runLater(() -> {
            try {
                ProjectExplorerService service = new ProjectExplorerService();
                StageCanvasView canvas = new StageCanvasView(service);

                StageFixtureNode node1 = createFixture("f1", "Spot 1", 50, 50);
                StageFixtureNode node2 = createFixture("f2", "Spot 2", 150, 50);

                service.addItemToFolder("folder-devices", node1.getItem());
                service.addItemToFolder("folder-devices", node2.getItem());

                canvas.addFixtureNode(node1);
                canvas.addFixtureNode(node2);

                AtomicBoolean deletedFired = new AtomicBoolean(false);
                canvas.setOnFixturesDeleted(() -> deletedFired.set(true));

                // Node 1 selektieren
                node1.setSelected(true);
                node2.setSelected(false);

                // Delete-Taste auf Canvas auslösen
                Event.fireEvent(canvas, new KeyEvent(
                        KeyEvent.KEY_PRESSED, "", "", KeyCode.DELETE,
                        false, false, false, false));

                // Node 1 muss von Bühne und aus Service entfernt sein
                assertThat(canvas.getPlacedFixtures()).containsExactly(node2);
                assertThat(service.getItemsInFolder("folder-devices")).containsExactly(node2.getItem());
                assertThat(deletedFired.get()).isTrue();
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
    @DisplayName("Soll mehrere selektierte Fixtures bei Druck auf BACK_SPACE löschen")
    void shouldDeleteMultipleSelectedFixturesOnBackspaceKey() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();

        Platform.runLater(() -> {
            try {
                ProjectExplorerService service = new ProjectExplorerService();
                StageCanvasView canvas = new StageCanvasView(service);

                StageFixtureNode node1 = createFixture("f1", "Spot 1", 50, 50);
                StageFixtureNode node2 = createFixture("f2", "Spot 2", 150, 50);
                StageFixtureNode node3 = createFixture("f3", "Spot 3", 250, 50);

                service.addItemToFolder("folder-devices", node1.getItem());
                service.addItemToFolder("folder-devices", node2.getItem());
                service.addItemToFolder("folder-devices", node3.getItem());

                canvas.addFixtureNode(node1);
                canvas.addFixtureNode(node2);
                canvas.addFixtureNode(node3);

                // Node 1 und Node 2 selektieren via selectFixtures
                canvas.selectFixtures(List.of(node1, node2));

                Event.fireEvent(canvas, new KeyEvent(
                        KeyEvent.KEY_PRESSED, "", "", KeyCode.BACK_SPACE,
                        false, false, false, false));

                // Nur noch Node 3 übrig
                assertThat(canvas.getPlacedFixtures()).containsExactly(node3);
                assertThat(service.getItemsInFolder("folder-devices")).containsExactly(node3.getItem());
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
    @DisplayName("Soll fokussiertes Item im Project Explorer TreeView bei ENTF löschen")
    void shouldDeleteFixtureInExplorerOnDeleteKey() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();

        Platform.runLater(() -> {
            try {
                ProjectExplorerService service = new ProjectExplorerService();
                StageCanvasView canvas = new StageCanvasView(service);
                ProjectExplorerView explorer = new ProjectExplorerView(service);
                explorer.setStageCanvasView(canvas);

                StageFixtureNode node1 = createFixture("f1", "Spot 1", 50, 50);
                service.addItemToFolder("folder-devices", node1.getItem());
                canvas.addFixtureNode(node1);

                // TreeView aktualisieren und Item auswählen
                explorer.rebuildTree();
                explorer.selectItem("f1");

                // ENTF auf TreeView auslösen
                Event.fireEvent(explorer.getTreeView(), new KeyEvent(
                        KeyEvent.KEY_PRESSED, "", "", KeyCode.DELETE,
                        false, false, false, false));

                assertThat(service.getItemsInFolder("folder-devices")).isEmpty();
                assertThat(canvas.getPlacedFixtures()).isEmpty();
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

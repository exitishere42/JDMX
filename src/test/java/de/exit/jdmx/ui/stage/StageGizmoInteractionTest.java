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
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
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
 * TDD-Tests für das 2D-Transformations-Gizmo mit X- und Y-Achsen-Pfeilen zur Fixture-Bewegung.
 */
class StageGizmoInteractionTest {

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

    private StageFixtureNode createTestFixture(String id, String name, double x, double y) {
        ChannelDefinition ch = new ChannelDefinition(1, "Dimmer", ChannelType.DIMMER, 0, false);
        DmxMode mode = new DmxMode("1CH", List.of(ch));
        FixtureDefinition def = new FixtureDefinition("test-dimmer", "Generic", "Dimmer", ExplorerItemType.FIXTURE, List.of(mode));
        FixtureInstance instance = new FixtureInstance(id, name, def, mode, 1, 1, x, y);
        ExplorerItem item = new ExplorerItem(id, name, ExplorerItemType.FIXTURE, false, "lightbulb", Color.WHITE, instance);
        return new StageFixtureNode(item, x, y);
    }

    @Test
    @DisplayName("Sollte X- und Y-Gizmo-Pfeile nur bei Selektion einblenden und sonst verbergen")
    void shouldToggleGizmoVisibilityOnSelection() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                StageFixtureNode node = createTestFixture("f1", "Spot 1", 100.0, 100.0);

                // Initial nicht selektiert: Gizmo unsichtbar
                assertThat(node.isSelected()).isFalse();
                assertThat(node.getGizmoXArrow()).isNotNull();
                assertThat(node.getGizmoYArrow()).isNotNull();
                assertThat(node.getGizmoXArrow().isVisible()).isFalse();
                assertThat(node.getGizmoYArrow().isVisible()).isFalse();

                // Selektion: Gizmo blendet sich ein
                node.setSelected(true);
                assertThat(node.getGizmoXArrow().isVisible()).isTrue();
                assertThat(node.getGizmoYArrow().isVisible()).isTrue();

                // Deselektion: Gizmo verschwindet
                node.setSelected(false);
                assertThat(node.getGizmoXArrow().isVisible()).isFalse();
                assertThat(node.getGizmoYArrow().isVisible()).isFalse();
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("Sollte Fixture bei Ziehen des X-Pfeils strikt nur auf der X-Achse verschieben")
    void shouldDragStrictlyAlongXAxis() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                StageFixtureNode node = createTestFixture("f1", "Spot 1", 100.0, 100.0);
                node.setSelected(true);

                AtomicBoolean dragFinishedCalled = new AtomicBoolean(false);
                node.setOnDragFinished(() -> dragFinishedCalled.set(true));

                javafx.scene.Node xArrow = node.getGizmoXArrow();

                // 1. Mouse Pressed auf X-Pfeil (Start an Bildschirmszene X=100, Y=100)
                Event.fireEvent(xArrow, new MouseEvent(
                        MouseEvent.MOUSE_PRESSED,
                        100.0, 100.0, 100.0, 100.0, MouseButton.PRIMARY, 1,
                        false, false, false, false, false, false, false, false, false, false, null));

                // 2. Mouse Dragged um +40px X und absichtlich +25px Y
                Event.fireEvent(xArrow, new MouseEvent(
                        MouseEvent.MOUSE_DRAGGED,
                        140.0, 125.0, 140.0, 125.0, MouseButton.PRIMARY, 1,
                        false, false, false, false, false, false, false, false, false, false, null));

                // X muss gewandert sein (+40 = 140), Y MUSS exakt 100.0 geblieben sein!
                assertThat(node.getLayoutX()).isEqualTo(140.0);
                assertThat(node.getLayoutY()).isEqualTo(100.0);

                // 3. Mouse Released
                Event.fireEvent(xArrow, new MouseEvent(
                        MouseEvent.MOUSE_RELEASED,
                        140.0, 125.0, 140.0, 125.0, MouseButton.PRIMARY, 1,
                        false, false, false, false, false, false, false, false, false, false, null));

                assertThat(dragFinishedCalled.get()).isTrue();
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("Sollte Fixture bei Ziehen des Y-Pfeils strikt nur auf der Y-Achse verschieben")
    void shouldDragStrictlyAlongYAxis() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                StageFixtureNode node = createTestFixture("f1", "Spot 1", 100.0, 100.0);
                node.setSelected(true);

                AtomicBoolean dragFinishedCalled = new AtomicBoolean(false);
                node.setOnDragFinished(() -> dragFinishedCalled.set(true));

                javafx.scene.Node yArrow = node.getGizmoYArrow();

                // 1. Mouse Pressed auf Y-Pfeil (Start an X=100, Y=100)
                Event.fireEvent(yArrow, new MouseEvent(
                        MouseEvent.MOUSE_PRESSED,
                        100.0, 100.0, 100.0, 100.0, MouseButton.PRIMARY, 1,
                        false, false, false, false, false, false, false, false, false, false, null));

                // 2. Mouse Dragged um +30px X und -35px Y
                Event.fireEvent(yArrow, new MouseEvent(
                        MouseEvent.MOUSE_DRAGGED,
                        130.0, 65.0, 130.0, 65.0, MouseButton.PRIMARY, 1,
                        false, false, false, false, false, false, false, false, false, false, null));

                // Y muss gewandert sein (-35 = 65), X MUSS exakt 100.0 geblieben sein!
                assertThat(node.getLayoutX()).isEqualTo(100.0);
                assertThat(node.getLayoutY()).isEqualTo(65.0);

                // 3. Mouse Released
                Event.fireEvent(yArrow, new MouseEvent(
                        MouseEvent.MOUSE_RELEASED,
                        130.0, 65.0, 130.0, 65.0, MouseButton.PRIMARY, 1,
                        false, false, false, false, false, false, false, false, false, false, null));

                assertThat(dragFinishedCalled.get()).isTrue();
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("Sollte bei Mehrfachauswahl alle selektierten Fixtures synchron auf der Achse verschieben")
    void shouldSynchronizeMultiSelectionDragAlongAxis() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                ProjectExplorerService explorerService = new ProjectExplorerService();
                StageCanvasView canvas = new StageCanvasView(explorerService);

                StageFixtureNode nodeA = createTestFixture("fA", "Spot A", 50.0, 50.0);
                StageFixtureNode nodeB = createTestFixture("fB", "Spot B", 150.0, 150.0);

                canvas.addFixtureNode(nodeA);
                canvas.addFixtureNode(nodeB);

                nodeA.setSelected(true);
                nodeB.setSelected(true);

                // Ziehen des X-Pfeils von nodeA um +30px
                javafx.scene.Node xArrowA = nodeA.getGizmoXArrow();
                Event.fireEvent(xArrowA, new MouseEvent(
                        MouseEvent.MOUSE_PRESSED,
                        50.0, 50.0, 50.0, 50.0, MouseButton.PRIMARY, 1,
                        false, false, false, false, false, false, false, false, false, false, null));

                Event.fireEvent(xArrowA, new MouseEvent(
                        MouseEvent.MOUSE_DRAGGED,
                        80.0, 99.0, 80.0, 99.0, MouseButton.PRIMARY, 1,
                        false, false, false, false, false, false, false, false, false, false, null));

                Event.fireEvent(xArrowA, new MouseEvent(
                        MouseEvent.MOUSE_RELEASED,
                        80.0, 99.0, 80.0, 99.0, MouseButton.PRIMARY, 1,
                        false, false, false, false, false, false, false, false, false, false, null));

                // Beide müssen sich um +30 in X verschoben haben, Y bleibt unverändert!
                assertThat(nodeA.getLayoutX()).isEqualTo(80.0);
                assertThat(nodeA.getLayoutY()).isEqualTo(50.0);
                assertThat(nodeB.getLayoutX()).isEqualTo(180.0);
                assertThat(nodeB.getLayoutY()).isEqualTo(150.0);
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }
}

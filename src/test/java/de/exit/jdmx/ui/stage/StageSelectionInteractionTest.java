package de.exit.jdmx.ui.stage;

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

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests für die Marquee Selection und die Fixture-Hitbox in {@link StageCanvasView} und {@link StageFixtureNode}.
 */
class StageSelectionInteractionTest {

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
    @DisplayName("Soll SelectionRect mit 4px Eckenrundung und JDMX-Styling initialisieren")
    void shouldInitializeSelectionRectWithCorrectRadius() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                StageCanvasView canvas = new StageCanvasView(new ProjectExplorerService());
                assertThat(canvas.getSelectionRect()).isNotNull();
                assertThat(canvas.getSelectionRect().getArcWidth()).isEqualTo(8.0); // 4px Radius
                assertThat(canvas.getSelectionRect().getArcHeight()).isEqualTo(8.0); // 4px Radius
                assertThat(canvas.getSelectionRect().isMouseTransparent()).isTrue();
                assertThat(canvas.getSelectionRect().isVisible()).isFalse();
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("Soll FixtureNode mit exakter Square-Hitbox konfigurieren (kein Überhang)")
    void shouldConfigurePreciseHitboxWithoutOverhang() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                de.exit.jdmx.core.model.ChannelDefinition ch = new de.exit.jdmx.core.model.ChannelDefinition(1, "Dimmer", de.exit.jdmx.core.model.ChannelType.DIMMER, 0, false);
                de.exit.jdmx.core.model.DmxMode mode = new de.exit.jdmx.core.model.DmxMode("1CH", java.util.List.of(ch));
                FixtureDefinition def = new FixtureDefinition("test-dimmer", "Generic", "Dimmer", ExplorerItemType.FIXTURE, java.util.List.of(mode));
                FixtureInstance instance = new FixtureInstance("inst-1", "Spot 1", def, mode, 1, 1, 50, 50);
                ExplorerItem item = new ExplorerItem("item-1", "Spot 1", ExplorerItemType.FIXTURE, false, "lightbulb", Color.WHITE, instance);

                StageFixtureNode node = new StageFixtureNode(item, 50, 50);

                // Die Eltern-Pane ist exakt 36x36px groß und der Badge ist unmanaged (kein Überhang)
                assertThat(node.getPrefWidth()).isEqualTo(36.0);
                assertThat(node.getPrefHeight()).isEqualTo(36.0);
                assertThat(node.getNameBadge().isManaged()).isFalse();

                // Das 36x36px Gehäuse fängt die Mausklicks
                assertThat(node.getSquare().getPrefWidth()).isEqualTo(36.0);
                assertThat(node.getSquare().getPrefHeight()).isEqualTo(36.0);
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }
}

package de.exit.jdmx.ui.stage;

import de.exit.jdmx.core.model.ChannelDefinition;
import de.exit.jdmx.core.model.ChannelType;
import de.exit.jdmx.core.model.DmxMode;
import de.exit.jdmx.core.model.ExplorerItem;
import de.exit.jdmx.core.model.ExplorerItemType;
import de.exit.jdmx.core.model.FixtureDefinition;
import de.exit.jdmx.core.model.FixtureInstance;
import de.exit.jdmx.service.DmxService;
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
 * Tests für die Pan/Tilt-Synchronisation zwischen selektierten Fixtures
 * und dem Positions-Steuerungs-Dock.
 */
class PositionModuleSyncTest {

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

    private StageFixtureNode createMovingHead(String id, String name, double x, double y) {
        ChannelDefinition chPan = new ChannelDefinition(1, "Pan", ChannelType.PAN, 0, false);
        ChannelDefinition chTilt = new ChannelDefinition(2, "Tilt", ChannelType.TILT, 0, false);
        ChannelDefinition chDim = new ChannelDefinition(3, "Dimmer", ChannelType.DIMMER, 0, false);
        DmxMode mode = new DmxMode("3CH", List.of(chPan, chTilt, chDim));
        FixtureDefinition def = new FixtureDefinition("generic-mh", "Generic", "Moving Head", ExplorerItemType.FIXTURE, List.of(mode));
        FixtureInstance instance = new FixtureInstance(id, name, def, mode, 1, 1, x, y);
        ExplorerItem item = new ExplorerItem(id, name, ExplorerItemType.FIXTURE, false, "box", Color.CYAN, instance);
        return new StageFixtureNode(item, x, y);
    }

    @Test
    @DisplayName("Sollte Pan und Tilt in StageFixtureNode speichern und per Getter bereitstellen")
    void shouldStorePanAndTiltInStageFixtureNode() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();

        Platform.runLater(() -> {
            try {
                StageFixtureNode node = createMovingHead("mh1", "MH 1", 100, 100);

                // Standardmäßig auf 50% (0.5)
                assertThat(node.getPan()).isEqualTo(0.5);
                assertThat(node.getTilt()).isEqualTo(0.5);

                node.setPosition(0.25, 0.75);
                assertThat(node.getPan()).isEqualTo(0.25);
                assertThat(node.getTilt()).isEqualTo(0.75);
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
    @DisplayName("Sollte Dock-Slider für Pan und Tilt beim Auswählen eines Fixtures synchronisieren")
    void shouldSyncDockSlidersToSelectedFixturePanTilt() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();

        Platform.runLater(() -> {
            try {
                ProjectExplorerService service = new ProjectExplorerService();
                DmxService dmxService = new DmxService();
                StageWorkspaceView workspace = new StageWorkspaceView(service, dmxService);

                StageFixtureNode mh1 = createMovingHead("mh1", "MH 1", 50, 50);
                mh1.setPosition(0.20, 0.80);

                StageFixtureNode mh2 = createMovingHead("mh2", "MH 2", 150, 50);
                mh2.setPosition(0.70, 0.30);

                workspace.getStageCanvasView().addFixtureNode(mh1);
                workspace.getStageCanvasView().addFixtureNode(mh2);

                PositionModuleView posModule = workspace.getControlDock().getPositionModule();

                // 1. MH 1 selektieren -> Dock übernimmt Pan=20, Tilt=80
                mh1.setSelected(true);
                mh2.setSelected(false);

                assertThat(posModule.getPanSlider().getValue()).isEqualTo(20.0);
                assertThat(posModule.getTiltSlider().getValue()).isEqualTo(80.0);

                // 2. MH 2 selektieren -> Dock übernimmt Pan=70, Tilt=30
                mh1.setSelected(false);
                mh2.setSelected(true);

                assertThat(posModule.getPanSlider().getValue()).isEqualTo(70.0);
                assertThat(posModule.getTiltSlider().getValue()).isEqualTo(30.0);
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
    @DisplayName("Sollte Home-Button Pan und Tilt auf 50% zentrieren und Fixture-Position aktualisieren")
    void shouldResetToHomePositionViaButton() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();

        Platform.runLater(() -> {
            try {
                ProjectExplorerService service = new ProjectExplorerService();
                DmxService dmxService = new DmxService();
                StageWorkspaceView workspace = new StageWorkspaceView(service, dmxService);

                StageFixtureNode mh1 = createMovingHead("mh1", "MH 1", 50, 50);
                mh1.setPosition(0.10, 0.90);
                workspace.getStageCanvasView().addFixtureNode(mh1);

                mh1.setSelected(true);

                PositionModuleView posModule = workspace.getControlDock().getPositionModule();
                assertThat(posModule.getPanSlider().getValue()).isEqualTo(10.0);

                // Home-Button betätigen
                posModule.getBtnHome().fire();

                assertThat(posModule.getPanSlider().getValue()).isEqualTo(50.0);
                assertThat(posModule.getTiltSlider().getValue()).isEqualTo(50.0);
                assertThat(mh1.getPan()).isEqualTo(0.5);
                assertThat(mh1.getTilt()).isEqualTo(0.5);
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

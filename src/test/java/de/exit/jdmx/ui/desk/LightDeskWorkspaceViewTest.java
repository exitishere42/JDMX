package de.exit.jdmx.ui.desk;

import de.exit.jdmx.core.model.*;
import de.exit.jdmx.service.DmxService;
import de.exit.jdmx.service.ProjectExplorerService;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.control.ComboBox;
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
 * TDD Unit-Tests für den {@link LightDeskWorkspaceView} (Tab 2: LightDesk).
 * Prüft den visuellen Aufbau mit 50/50 Split, das obere Fader-Dock und Navigation.
 */
class LightDeskWorkspaceViewTest {

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
    @DisplayName("Sollte LightDesk mit Universums-Dropdown, 50/50 SplitPane und oberem Fader-Dock aufbauen")
    void shouldBuildLightDeskLayout() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                DmxService dmxService = new DmxService();
                ProjectExplorerService explorerService = new ProjectExplorerService();

                LightDeskWorkspaceView desk = new LightDeskWorkspaceView(dmxService, explorerService);

                assertThat(desk).isNotNull();
                assertThat(desk.getStyleClass()).contains("lightdesk-workspace");

                // 1. Header & Universum Dropdown
                ComboBox<String> comboUni = desk.getUniverseComboBox();
                assertThat(comboUni).isNotNull();
                assertThat(comboUni.getItems()).containsExactly("Universum 1", "Universum 2", "Universum 3", "Universum 4");
                assertThat(comboUni.getValue()).isEqualTo("Universum 1");

                // 2. SplitPane (großes Faderdock oben, schlanke Fixture-Leiste unten)
                assertThat(desk.getSplitPane()).isNotNull();
                assertThat(desk.getSplitPane().getOrientation()).isEqualTo(Orientation.VERTICAL);
                assertThat(desk.getSplitPane().getDividerPositions()).contains(0.78);

                // 3. Oberes Fader-Dock
                assertThat(desk.getTopFaderDock()).isNotNull();
                assertThat(desk.getCanvasDeskView()).isNotNull();
                assertThat(desk.getCanvasDeskView().getTotalChannels()).isEqualTo(512);
                assertThat(desk.getCanvasDeskView().getChannelValue(1)).isEqualTo(0);

                // 4. Unterer Bereich mit DeskFixtureBarView
                assertThat(desk.getBottomWorkspaceArea()).isNotNull();
                assertThat(desk.getDeskFixtureBarView()).isNotNull();
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
    @DisplayName("Sollte Kanalwert über CanvasDeskView ändern und über Reset-Button auf 0 zurücksetzen")
    void shouldChangeAndResetChannelValue() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                DmxService dmxService = new DmxService();
                ProjectExplorerService explorerService = new ProjectExplorerService();
                LightDeskWorkspaceView desk = new LightDeskWorkspaceView(dmxService, explorerService);

                // Wert für Kanal 5 setzen
                desk.getCanvasDeskView().setChannelValue(5, 180);
                assertThat(desk.getCanvasDeskView().getChannelValue(5)).isEqualTo(180);
                // DMX-Service muss den Wert im Backend atomar empfangen haben
                assertThat(dmxService.getChannel(0, 5)).isEqualTo(180);

                // Alle zurücksetzen
                desk.resetAllChannels();
                assertThat(desk.getCanvasDeskView().getChannelValue(5)).isEqualTo(0);
                assertThat(dmxService.getChannel(0, 5)).isEqualTo(0);
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
    @DisplayName("Sollte gepatchte Geräte den Kanälen zuordnen")
    void shouldAssignPatchedFixturesToChannels() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                DmxService dmxService = new DmxService();
                ProjectExplorerService explorerService = new ProjectExplorerService();

                // Gerät mit Dimmer, Pan, Tilt patchen
                ChannelDefinition ch1 = new ChannelDefinition(1, "Dimmer", ChannelType.DIMMER, 0, false);
                ChannelDefinition ch2 = new ChannelDefinition(2, "Pan", ChannelType.PAN, 0, false);
                ChannelDefinition ch3 = new ChannelDefinition(3, "Tilt", ChannelType.TILT, 0, false);
                DmxMode mode = new DmxMode("3CH", List.of(ch1, ch2, ch3));
                FixtureDefinition def = new FixtureDefinition("test-spot", "Test", "Spot", ExplorerItemType.FIXTURE, List.of(mode));
                FixtureInstance instance = new FixtureInstance("f1", "Moving Head 1", def, mode, 1, 1, 100, 100);
                ExplorerItem item = new ExplorerItem("item-1", "Moving Head 1", ExplorerItemType.FIXTURE, false, "lamp", Color.CYAN, instance);

                explorerService.addItemToFolder(ProjectExplorerService.FOLDER_DEVICES_ID, item);

                LightDeskWorkspaceView desk = new LightDeskWorkspaceView(dmxService, explorerService);

                // Kanal-Daten in CanvasDeskView prüfen
                DeskChannelData data1 = desk.getCanvasDeskView().getChannelData(1);
                DeskChannelData data2 = desk.getCanvasDeskView().getChannelData(2);
                DeskChannelData data3 = desk.getCanvasDeskView().getChannelData(3);
                DeskChannelData data4 = desk.getCanvasDeskView().getChannelData(4); // Unbelegt

                assertThat(data1.getFunctionName()).isEqualTo("Dimmer");
                assertThat(data2.getFunctionName()).isEqualTo("Pan");
                assertThat(data3.getFunctionName()).isEqualTo("Tilt");
                assertThat(data4.getFunctionName()).isEqualTo("-");

                // Test: Fixture in DeskFixtureBarView vorhanden
                assertThat(desk.getDeskFixtureBarView().getFixtureRowCount()).isEqualTo(1);

                // Test: Locate-Funktion schaltet Dimmer auf 255 und Pan/Tilt mittig (128)
                desk.locateFixture(instance);
                assertThat(dmxService.getChannel(0, 1)).isEqualTo(255);
                assertThat(dmxService.getChannel(0, 2)).isEqualTo(128);
                assertThat(dmxService.getChannel(0, 3)).isEqualTo(128);

                // Test: Zero-Funktion schaltet Dimmer auf 0
                desk.zeroFixture(instance);
                assertThat(dmxService.getChannel(0, 1)).isEqualTo(0);
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
    @DisplayName("Sollte Scroll-Navigation über DeskScrollBar und scrollToChannel unterstützen")
    void shouldSupportScrollNavigation() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                DmxService dmxService = new DmxService();
                ProjectExplorerService explorerService = new ProjectExplorerService();
                LightDeskWorkspaceView desk = new LightDeskWorkspaceView(dmxService, explorerService);

                // ScrollBar ist vorhanden und initial auf 0
                DeskScrollBar scrollBar = desk.getCanvasDeskView().getScrollBar();
                assertThat(scrollBar).isNotNull();
                assertThat(desk.getCanvasDeskView().getScrollOffset()).isEqualTo(0.0);

                // Zu Kanal 65 scrollen
                desk.scrollToChannel(65);
                assertThat(desk.getCanvasDeskView().getScrollOffset()).isEqualTo(64.0);

                // Direkt über ScrollBar scrollen
                scrollBar.setValue(128.0);
                assertThat(desk.getCanvasDeskView().getScrollOffset()).isEqualTo(128.0);
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
    @DisplayName("Sollte nahtlos in MainWindow als Tab 2 integriert sein")
    void shouldIntegrateInMainWindowTab2() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                de.exit.jdmx.ui.MainWindow window = new de.exit.jdmx.ui.MainWindow();
                assertThat(window.getLightDeskWorkspaceView()).isNotNull();

                // Tab 2 auswählen
                window.switchToTab(1);
                assertThat(window.getHeaderBar().getActiveTabIndex()).isEqualTo(1);
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

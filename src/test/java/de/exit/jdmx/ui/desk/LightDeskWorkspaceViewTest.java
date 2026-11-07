package de.exit.jdmx.ui.desk;

import de.exit.jdmx.core.model.*;
import de.exit.jdmx.service.DmxService;
import de.exit.jdmx.service.ProjectExplorerService;
import javafx.application.Platform;
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
 * Prüft den visuellen Aufbau, Layout-Hierarchien, performante Canvas-Balkenbank und Navigation.
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
    @DisplayName("Sollte LightDesk mit Universums-Dropdown, CanvasDeskView und Fixture-Liste aufbauen")
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

                // 2. Performante Canvas-Faderbank
                assertThat(desk.getCanvasDeskView()).isNotNull();
                assertThat(desk.getCanvasDeskView().getTotalChannels()).isEqualTo(512);
                assertThat(desk.getCanvasDeskView().getChannelValue(1)).isEqualTo(0);

                // 3. Fixture-Liste
                assertThat(desk.getFixtureListContainer()).isNotNull();
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

                // Alle zurücksetzen
                desk.getBtnResetAll().fire();
                assertThat(desk.getCanvasDeskView().getChannelValue(5)).isEqualTo(0);
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
    @DisplayName("Sollte gepatchte Geräte in der unteren Liste anzeigen und Kanalbeschriftungen zuordnen")
    void shouldDisplayPatchedFixturesAndChannelLabels() throws Throwable {
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

                // Fixture-Karten prüfen
                assertThat(desk.getFixtureCards()).hasSize(1);
                assertThat(desk.getFixtureCards().get(0).getTitleLabel().getText()).contains("Moving Head 1");
                assertThat(desk.getFixtureCards().get(0).getChannelsLabel().getText()).contains("1 - 3");

                // Kanal-Daten in CanvasDeskView prüfen
                DeskChannelData data1 = desk.getCanvasDeskView().getChannelData(1);
                DeskChannelData data2 = desk.getCanvasDeskView().getChannelData(2);
                DeskChannelData data3 = desk.getCanvasDeskView().getChannelData(3);
                DeskChannelData data4 = desk.getCanvasDeskView().getChannelData(4); // Unbelegt

                assertThat(data1.getFunctionName()).isEqualTo("Dimmer");
                assertThat(data2.getFunctionName()).isEqualTo("Pan");
                assertThat(data3.getFunctionName()).isEqualTo("Tilt");
                assertThat(data4.getFunctionName()).isEqualTo("-");
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
    @DisplayName("Sollte Bank-Navigation unterstützen (z. B. Bank 1: 1-32, Bank 2: 33-64)")
    void shouldSupportBankNavigation() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                DmxService dmxService = new DmxService();
                ProjectExplorerService explorerService = new ProjectExplorerService();
                LightDeskWorkspaceView desk = new LightDeskWorkspaceView(dmxService, explorerService);

                assertThat(desk.getCurrentBank()).isEqualTo(1);
                desk.selectBank(2);
                assertThat(desk.getCurrentBank()).isEqualTo(2);

                // Scrollposition muss Bank 2 (Kanal 33 -> Offset 32) widerspiegeln
                assertThat(desk.getCanvasDeskView().getScrollOffset()).isEqualTo(32.0);
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

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
 * Prüft den visuellen Aufbau, Layout-Hierarchien, Fader-Erzeugung und Navigation.
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
    @DisplayName("Sollte LightDesk mit Universums-Dropdown, Faderbank und Fixture-Liste aufbauen")
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

                // 2. Faderbank
                assertThat(desk.getFaderContainer()).isNotNull();
                assertThat(desk.getChannelStrips()).hasSize(512);

                // Erster Kanalstreifen prüfen
                DeskChannelStrip firstStrip = desk.getChannelStrips().get(0);
                assertThat(firstStrip.getChannelNumber()).isEqualTo(1);
                assertThat(firstStrip.getSlider().getMin()).isEqualTo(0.0);
                assertThat(firstStrip.getSlider().getMax()).isEqualTo(255.0);
                assertThat(firstStrip.getValueLabel().getText()).isEqualTo("0");

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
    @DisplayName("Sollte Kanalwert über Slider ändern und über Reset-Button auf 0 zurücksetzen")
    void shouldChangeAndResetChannelValue() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                DmxService dmxService = new DmxService();
                ProjectExplorerService explorerService = new ProjectExplorerService();
                LightDeskWorkspaceView desk = new LightDeskWorkspaceView(dmxService, explorerService);

                DeskChannelStrip strip = desk.getChannelStrips().get(4); // Kanal 5

                // Slider bewegen
                strip.getSlider().setValue(180.0);
                assertThat(strip.getValueLabel().getText()).isEqualTo("180");

                // Reset-Button drücken
                strip.getBtnReset().fire();
                assertThat(strip.getSlider().getValue()).isEqualTo(0.0);
                assertThat(strip.getValueLabel().getText()).isEqualTo("0");
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

                // Kanalstreifen-Labels prüfen
                DeskChannelStrip strip1 = desk.getChannelStrips().get(0);
                DeskChannelStrip strip2 = desk.getChannelStrips().get(1);
                DeskChannelStrip strip3 = desk.getChannelStrips().get(2);
                DeskChannelStrip strip4 = desk.getChannelStrips().get(3); // Unbelegt

                assertThat(strip1.getChannelFunctionLabel().getText()).isEqualTo("Dimmer");
                assertThat(strip2.getChannelFunctionLabel().getText()).isEqualTo("Pan");
                assertThat(strip3.getChannelFunctionLabel().getText()).isEqualTo("Tilt");
                assertThat(strip4.getChannelFunctionLabel().getText()).isEqualTo("-");
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

                // Scrollposition muss sich verändert haben
                assertThat(desk.getFaderScrollPane().getHvalue()).isGreaterThan(0.0);
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

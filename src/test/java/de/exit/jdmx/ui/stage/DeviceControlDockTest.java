package de.exit.jdmx.ui.stage;

import javafx.application.Platform;
import javafx.geometry.Orientation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für das untere 24/7 Steuerungs-Dock (DeviceControlDock).
 * Prüft die feste Entkopplung des Intensitätsmoduls (300px) und die
 * dynamische 50/50 Teilung für Farb- und Positionsmodul.
 */
class DeviceControlDockTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit aktiv
        }
    }

    @Test
    @DisplayName("Sollte 3 Module initialisieren mit entkoppeltem SplitPane für Farbe und Position")
    void shouldInitializeWithThreeControlModules() {
        DeviceControlDock dock = new DeviceControlDock();

        assertThat(dock.getOrientation()).isEqualTo(Orientation.HORIZONTAL);
        assertThat(dock.getItems()).hasSize(3);

        assertThat(dock.getIntensityModule()).isNotNull();
        assertThat(dock.getColorModule()).isNotNull();
        assertThat(dock.getPositionModule()).isNotNull();
        assertThat(dock.getDynamicSplitPane()).isNotNull();
    }

    @Test
    @DisplayName("Sollte Intensitätsmodul auf feste 300px fixieren (horizontal starr entkoppelt)")
    void shouldFixIntensityModuleWidth() {
        DeviceControlDock dock = new DeviceControlDock();

        assertThat(dock.getIntensityModule().getMinWidth()).isEqualTo(IntensityModuleView.FIXED_WIDTH);
        assertThat(dock.getIntensityModule().getMaxWidth()).isEqualTo(IntensityModuleView.FIXED_WIDTH);
        assertThat(dock.getIntensityModule().getPrefWidth()).isEqualTo(IntensityModuleView.FIXED_WIDTH);
        assertThat(IntensityModuleView.FIXED_WIDTH).isEqualTo(300.0);
    }

    @Test
    @DisplayName("Sollte für Farb- und Positionsmodul Mindestbreiten im dynamischen SplitPane festlegen")
    void shouldSetMinimumWidthsOnModules() {
        DeviceControlDock dock = new DeviceControlDock();

        assertThat(dock.getColorModule().getMinWidth()).isGreaterThanOrEqualTo(160.0);
        assertThat(dock.getPositionModule().getMinWidth()).isGreaterThanOrEqualTo(140.0);
    }

    @Test
    @DisplayName("Sollte eine flache, feste Mindesthöhe für das 24/7 Dock definieren")
    void shouldHaveCompactHeight() {
        DeviceControlDock dock = new DeviceControlDock();

        assertThat(dock.getMinHeight()).isEqualTo(DeviceControlDock.DOCK_HEIGHT);
        assertThat(dock.getPrefHeight()).isEqualTo(DeviceControlDock.DOCK_HEIGHT);
    }
}

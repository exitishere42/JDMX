package de.exit.jdmx.ui.stage;

import javafx.application.Platform;
import javafx.geometry.Orientation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für das untere 24/7 Steuerungs-Dock (DeviceControlDock).
 * Prüft, dass das Intensitätsmodul horizontal auf eine feste Breite fixiert ist
 * und nicht verschoben werden kann.
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
    @DisplayName("Sollte 3 Module nebeneinander in einem horizontalen SplitPane initialisieren")
    void shouldInitializeWithThreeControlModules() {
        DeviceControlDock dock = new DeviceControlDock();

        assertThat(dock.getOrientation()).isEqualTo(Orientation.HORIZONTAL);
        assertThat(dock.getItems()).hasSize(3);

        assertThat(dock.getIntensityModule()).isNotNull();
        assertThat(dock.getColorModule()).isNotNull();
        assertThat(dock.getPositionModule()).isNotNull();
    }

    @Test
    @DisplayName("Sollte Intensitätsmodul auf feste 245px fixieren (horizontal starr gesperrt)")
    void shouldFixIntensityModuleWidth() {
        DeviceControlDock dock = new DeviceControlDock();

        assertThat(dock.getIntensityModule().getMinWidth()).isEqualTo(IntensityModuleView.FIXED_WIDTH);
        assertThat(dock.getIntensityModule().getMaxWidth()).isEqualTo(IntensityModuleView.FIXED_WIDTH);
        assertThat(dock.getIntensityModule().getPrefWidth()).isEqualTo(IntensityModuleView.FIXED_WIDTH);
    }

    @Test
    @DisplayName("Sollte für Farb- und Positionsmodul Mindestbreiten festlegen")
    void shouldSetMinimumWidthsOnModules() {
        DeviceControlDock dock = new DeviceControlDock();

        assertThat(dock.getColorModule().getMinWidth()).isGreaterThanOrEqualTo(160.0);
        assertThat(dock.getPositionModule().getMinWidth()).isGreaterThanOrEqualTo(140.0);
    }

    @Test
    @DisplayName("Sollte eine flache, kompakte Höhe für das 24/7 Dock definieren")
    void shouldHaveCompactHeight() {
        DeviceControlDock dock = new DeviceControlDock();

        assertThat(dock.getMinHeight()).isGreaterThanOrEqualTo(100.0);
        assertThat(dock.getPrefHeight()).isLessThanOrEqualTo(145.0);
    }
}

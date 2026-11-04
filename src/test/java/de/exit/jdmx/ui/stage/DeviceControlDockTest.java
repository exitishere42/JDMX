package de.exit.jdmx.ui.stage;

import javafx.application.Platform;
import javafx.geometry.Orientation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für das untere 24/7 Steuerungs-Dock (DeviceControlDock)
 * mit den 3 horizontal verschiebbaren Modulen (Intensität, Farbe, Position).
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
    @DisplayName("Sollte für jedes Modul eine Mindestbreite festlegen (Harter Stopp gegen Kollabieren)")
    void shouldSetMinimumWidthsOnAllModules() {
        DeviceControlDock dock = new DeviceControlDock();

        assertThat(dock.getIntensityModule().getMinWidth()).isGreaterThanOrEqualTo(140.0);
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

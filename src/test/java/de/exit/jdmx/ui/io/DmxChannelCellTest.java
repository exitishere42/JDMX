package de.exit.jdmx.ui.io;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Unit-Tests für {@link DmxChannelCell}.
 * Prüft die Darstellung eines DMX-Kanal-Vierecks in der 512-Matrix:
 * Kanalnummer oben, Auslastungswert (0–255) unten und dynamisches grünes Leuchten.
 */
class DmxChannelCellTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // JavaFX Toolkit aktiv
        }
    }

    @Test
    @DisplayName("Sollte Zelle mit korrekter Kanalnummer und Ruhewert 0 initialisieren")
    void shouldInitializeWithChannelNumberAndZeroValue() {
        DmxChannelCell cell = new DmxChannelCell(1);

        assertThat(cell.getChannelNumber()).isEqualTo(1);
        assertThat(cell.getValue()).isEqualTo(0);
        assertThat(cell.getChannelLabel().getText()).isEqualTo("1");
        assertThat(cell.getValueLabel().getText()).isEqualTo("0");
        assertThat(cell.isActive()).isFalse();
    }

    @Test
    @DisplayName("Sollte DMX-Wert zwischen 0 und 255 begrenzen (Clamping)")
    void shouldClampValueBetween0And255() {
        DmxChannelCell cell = new DmxChannelCell(5);

        cell.setValue(300);
        assertThat(cell.getValue()).isEqualTo(255);
        assertThat(cell.getValueLabel().getText()).isEqualTo("255");
        assertThat(cell.isActive()).isTrue();

        cell.setValue(-10);
        assertThat(cell.getValue()).isEqualTo(0);
        assertThat(cell.getValueLabel().getText()).isEqualTo("0");
        assertThat(cell.isActive()).isFalse();
    }

    @Test
    @DisplayName("Sollte bei Werten > 0 den Aktivzustand für grünes Leuchten aktivieren")
    void shouldActivateGreenGlowWhenValueGreaterThanZero() {
        DmxChannelCell cell = new DmxChannelCell(42);

        cell.setValue(128);
        assertThat(cell.isActive()).isTrue();
        assertThat(cell.getStyleClass()).contains("dmx-channel-cell-active");

        cell.setValue(0);
        assertThat(cell.isActive()).isFalse();
        assertThat(cell.getStyleClass()).doesNotContain("dmx-channel-cell-active");
    }
}

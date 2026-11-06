package de.exit.jdmx.ui.io;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Unit-Tests für {@link DmxMatrixView}.
 * Prüft die 512-Kanal Matrix im 16-Spalten Mischpult-Raster (32 Zeilen à 16 Kanäle),
 * Universums-Umschaltung (1–4) und die Filterung (Alle 512 vs. Nur Aktive).
 */
class DmxMatrixViewTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit aktiv
        }
    }

    @Test
    @DisplayName("Sollte 512 Kanal-Zellen erzeugen und Universum 1 standardmäßig selektieren")
    void shouldCreate512ChannelCellsAndDefaultToUniverse1() {
        DmxMatrixView matrixView = new DmxMatrixView();

        assertThat(matrixView.getChannelCells()).hasSize(512);
        assertThat(matrixView.getCell(1)).isNotNull();
        assertThat(matrixView.getCell(512)).isNotNull();
        assertThat(matrixView.getSelectedUniverse()).isEqualTo(1);
    }

    @Test
    @DisplayName("Sollte im 16-Spalten Raster mit 32 Zeilen aufgebaut sein")
    void shouldBeOrganizedIn16ColumnsAnd32Rows() {
        DmxMatrixView matrixView = new DmxMatrixView();

        assertThat(matrixView.getColumnsPerRow()).isEqualTo(16);
        assertThat(matrixView.getRowCount()).isEqualTo(32);
    }

    @Test
    @DisplayName("Sollte Universums-Tabs von 1 bis 4 umschalten können")
    void shouldSwitchUniversesFrom1To4() {
        DmxMatrixView matrixView = new DmxMatrixView();

        matrixView.selectUniverse(2);
        assertThat(matrixView.getSelectedUniverse()).isEqualTo(2);

        matrixView.selectUniverse(4);
        assertThat(matrixView.getSelectedUniverse()).isEqualTo(4);
    }

    @Test
    @DisplayName("Sollte Filter zwischen 'Alle' und 'Nur Aktive' umschalten")
    void shouldToggleFilterBetweenAllAndOnlyActive() {
        DmxMatrixView matrixView = new DmxMatrixView();

        assertThat(matrixView.isOnlyActiveFilter()).isFalse();

        matrixView.setOnlyActiveFilter(true);
        assertThat(matrixView.isOnlyActiveFilter()).isTrue();

        matrixView.setOnlyActiveFilter(false);
        assertThat(matrixView.isOnlyActiveFilter()).isFalse();
    }

    @Test
    @DisplayName("Sollte Kanalwerte einzeln oder per Byte-Array aktualisieren können")
    void shouldUpdateChannelValues() {
        DmxMatrixView matrixView = new DmxMatrixView();

        matrixView.setChannelValue(1, 255);
        assertThat(matrixView.getCell(1).getValue()).isEqualTo(255);

        byte[] frame = new byte[512];
        frame[0] = (byte) 200;
        frame[1] = (byte) 100;
        matrixView.setChannels(frame);

        assertThat(matrixView.getCell(1).getValue()).isEqualTo(200);
        assertThat(matrixView.getCell(2).getValue()).isEqualTo(100);
    }
}

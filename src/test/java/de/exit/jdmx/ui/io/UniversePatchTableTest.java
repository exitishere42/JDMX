package de.exit.jdmx.ui.io;

import de.exit.jdmx.service.HardwareDiscoveryService;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Unit-Tests für {@link UniversePatchTable}.
 * Prüft die QLC+-ähnliche Universum-Zuordnungstabelle mit 4 Universen,
 * Custom-Dropdowns für Input/Output, Status-Dots, Config-Buttons und eingerückten Trennstrichen.
 */
class UniversePatchTableTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit aktiv
        }
    }

    @Test
    @DisplayName("Sollte genau 4 Universum-Zeilen erzeugen")
    void shouldCreateFourUniverseRows() {
        UniversePatchTable table = new UniversePatchTable();

        assertThat(table.getUniverseRows()).hasSize(4);
    }

    @Test
    @DisplayName("Sollte für jede Zeile Input- und Output-Custom-Dropdowns bereitstellen")
    void shouldProvideInputAndOutputDropdownsWithRequiredOptions() {
        HardwareDiscoveryService mockHardware = new HardwareDiscoveryService(
                () -> List.of("JMS USB to DMX PRO (COM3)"),
                () -> List.of("MIDI: AKAI APC Mini")
        );
        UniversePatchTable table = new UniversePatchTable(mockHardware);

        for (int i = 0; i < 4; i++) {
            UniversePatchTable.UniverseRow row = table.getUniverseRows().get(i);
            assertThat(row.getUniverseNumber()).isEqualTo(i + 1);

            // Input
            assertThat(row.getInputDropdown().getItems()).contains(
                    "Kein Input",
                    "Art-Net (127.0.0.1, Uni " + i + ")",
                    "MIDI: AKAI APC Mini",
                    "Loopback"
            );

            // Output
            assertThat(row.getOutputDropdown().getItems()).contains(
                    "Art-Net (127.0.0.1, Uni " + i + ")",
                    "JMS USB to DMX PRO (COM3)",
                    "Kein Output"
            );
        }
    }

    @Test
    @DisplayName("Universum 1 sollte Art-Net Output als Default haben, Universen 2-4 Kein Output")
    void shouldSetCorrectDefaults() {
        UniversePatchTable table = new UniversePatchTable();

        UniversePatchTable.UniverseRow row1 = table.getUniverseRows().get(0);
        assertThat(row1.getInputDropdown().getSelectedItem()).isEqualTo("Kein Input");
        assertThat(row1.getOutputDropdown().getSelectedItem()).contains("Art-Net");
        assertThat(row1.isActive()).isTrue();

        for (int i = 1; i < 4; i++) {
            UniversePatchTable.UniverseRow row = table.getUniverseRows().get(i);
            assertThat(row.getInputDropdown().getSelectedItem()).isEqualTo("Kein Input");
            assertThat(row.getOutputDropdown().getSelectedItem()).isEqualTo("Kein Output");
            assertThat(row.isActive()).isFalse();
        }
    }

    @Test
    @DisplayName("Sollte eingerückte Trennstriche unter den Zeilen besitzen")
    void shouldHaveDividersBetweenRows() {
        UniversePatchTable table = new UniversePatchTable();
        assertThat(table.getDividers().size()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Sollte für jede Zeile einen Config-Button (⚙) bereitstellen")
    void shouldProvideConfigButtonPerRow() {
        UniversePatchTable table = new UniversePatchTable();

        for (UniversePatchTable.UniverseRow row : table.getUniverseRows()) {
            assertThat(row.getConfigBtn()).isNotNull();
        }
    }
}

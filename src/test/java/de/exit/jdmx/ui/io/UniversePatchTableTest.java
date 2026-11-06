package de.exit.jdmx.ui.io;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Unit-Tests für {@link UniversePatchTable}.
 * Prüft die QLC+-ähnliche I/O Patch-Tabelle mit Dropdown-Auswahl
 * für Input und Output pro Universum (1–4).
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
    @DisplayName("Sollte für jede Zeile Input- und Output-Dropdowns bereitstellen")
    void shouldProvideInputAndOutputDropdownsPerRow() {
        UniversePatchTable table = new UniversePatchTable();

        for (int i = 0; i < 4; i++) {
            UniversePatchTable.UniverseRow row = table.getUniverseRows().get(i);
            assertThat(row.getInputCombo()).isNotNull();
            assertThat(row.getOutputCombo()).isNotNull();
            assertThat(row.getInputCombo().getItems()).isNotEmpty();
            assertThat(row.getOutputCombo().getItems()).isNotEmpty();
        }
    }

    @Test
    @DisplayName("Sollte 'Kein Input' und 'Kein Output' als Default auswählen (außer Universum 1)")
    void shouldDefaultToNoInputAndNoOutput() {
        UniversePatchTable table = new UniversePatchTable();

        // Universum 2-4: Default "Kein Input" / "Kein Output"
        for (int i = 1; i < 4; i++) {
            UniversePatchTable.UniverseRow row = table.getUniverseRows().get(i);
            assertThat(row.getInputCombo().getValue()).isEqualTo("Kein Input");
            assertThat(row.getOutputCombo().getValue()).isEqualTo("Kein Output");
        }
    }

    @Test
    @DisplayName("Universum 1 sollte standardmäßig Art-Net Output vorausgewählt haben")
    void shouldDefaultUniverse1ToArtNetOutput() {
        UniversePatchTable table = new UniversePatchTable();
        UniversePatchTable.UniverseRow row1 = table.getUniverseRows().get(0);

        assertThat(row1.getOutputCombo().getValue()).contains("Art-Net");
    }

    @Test
    @DisplayName("Sollte die Universumsnummer korrekt anzeigen (1–4)")
    void shouldShowCorrectUniverseNumbers() {
        UniversePatchTable table = new UniversePatchTable();

        for (int i = 0; i < 4; i++) {
            UniversePatchTable.UniverseRow row = table.getUniverseRows().get(i);
            assertThat(row.getUniverseNumber()).isEqualTo(i + 1);
        }
    }
}

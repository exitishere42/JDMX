package de.exit.jdmx.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit-Tests für die Matrix-Kernmodelle (MatrixTriggerMode, MatrixTargetType,
 * MatrixButtonDefinition, MatrixGridDefinition).
 */
class MatrixModelTest {

    @Test
    @DisplayName("MatrixTriggerMode enthält TOGGLE und FLASH")
    void testTriggerModes() {
        assertThat(MatrixTriggerMode.values())
                .containsExactly(MatrixTriggerMode.TOGGLE, MatrixTriggerMode.FLASH);
    }

    @Test
    @DisplayName("MatrixTargetType enthält NONE, SCENE und CHASER")
    void testTargetTypes() {
        assertThat(MatrixTargetType.values())
                .containsExactly(MatrixTargetType.NONE, MatrixTargetType.SCENE, MatrixTargetType.CHASER);
    }

    @Test
    @DisplayName("MatrixButtonDefinition speichert Attribute korrekt und bietet sinnvolle Defaults")
    void testButtonDefinitionAttributes() {
        MatrixButtonDefinition button = new MatrixButtonDefinition(
                0, 0, 0, "Test Button", "#03DAC6", MatrixTargetType.SCENE, "scene-123", MatrixTriggerMode.TOGGLE
        );

        assertThat(button.index()).isEqualTo(0);
        assertThat(button.row()).isEqualTo(0);
        assertThat(button.col()).isEqualTo(0);
        assertThat(button.name()).isEqualTo("Test Button");
        assertThat(button.color()).isEqualTo("#03DAC6");
        assertThat(button.targetType()).isEqualTo(MatrixTargetType.SCENE);
        assertThat(button.targetId()).isEqualTo("scene-123");
        assertThat(button.triggerMode()).isEqualTo(MatrixTriggerMode.TOGGLE);
        assertThat(button.isAssigned()).isTrue();
    }

    @Test
    @DisplayName("MatrixButtonDefinition erkennt unzugewiesene Buttons korrekt")
    void testUnassignedButton() {
        MatrixButtonDefinition empty = MatrixButtonDefinition.createEmpty(5, 1, 2);

        assertThat(empty.index()).isEqualTo(5);
        assertThat(empty.row()).isEqualTo(1);
        assertThat(empty.col()).isEqualTo(2);
        assertThat(empty.targetType()).isEqualTo(MatrixTargetType.NONE);
        assertThat(empty.targetId()).isNull();
        assertThat(empty.isAssigned()).isFalse();
        assertThat(empty.triggerMode()).isEqualTo(MatrixTriggerMode.TOGGLE);
    }

    @Test
    @DisplayName("MatrixButtonDefinition validiert Grenzwerte und Null-Werte")
    void testButtonDefinitionValidation() {
        assertThatThrownBy(() -> new MatrixButtonDefinition(-1, 0, 0, "Name", "#FFFFFF", MatrixTargetType.NONE, null, MatrixTriggerMode.TOGGLE))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new MatrixButtonDefinition(0, -1, 0, "Name", "#FFFFFF", MatrixTargetType.NONE, null, MatrixTriggerMode.TOGGLE))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new MatrixButtonDefinition(0, 0, -1, "Name", "#FFFFFF", MatrixTargetType.NONE, null, MatrixTriggerMode.TOGGLE))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new MatrixButtonDefinition(0, 0, 0, null, "#FFFFFF", MatrixTargetType.NONE, null, MatrixTriggerMode.TOGGLE))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new MatrixButtonDefinition(0, 0, 0, "Name", null, MatrixTargetType.NONE, null, MatrixTriggerMode.TOGGLE))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new MatrixButtonDefinition(0, 0, 0, "Name", "#FFFFFF", null, null, MatrixTriggerMode.TOGGLE))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new MatrixButtonDefinition(0, 0, 0, "Name", "#FFFFFF", MatrixTargetType.NONE, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("MatrixGridDefinition initialisiert Standard-Raster 4 Zeilen x 8 Spalten")
    void testGridDefinitionDefaults() {
        MatrixGridDefinition grid = new MatrixGridDefinition();

        assertThat(grid.getRows()).isEqualTo(4);
        assertThat(grid.getCols()).isEqualTo(8);
        assertThat(grid.getTotalButtons()).isEqualTo(32);
        assertThat(grid.getButtons()).hasSize(32);

        MatrixButtonDefinition first = grid.getButton(0);
        assertThat(first).isNotNull();
        assertThat(first.index()).isEqualTo(0);
        assertThat(first.row()).isEqualTo(0);
        assertThat(first.col()).isEqualTo(0);
        assertThat(first.isAssigned()).isFalse();

        MatrixButtonDefinition last = grid.getButton(3, 7);
        assertThat(last).isNotNull();
        assertThat(last.index()).isEqualTo(31);
        assertThat(last.row()).isEqualTo(3);
        assertThat(last.col()).isEqualTo(7);
    }

    @Test
    @DisplayName("MatrixGridDefinition erlaubt benutzerdefinierte Abmessungen")
    void testCustomGridDimensions() {
        MatrixGridDefinition grid = new MatrixGridDefinition(3, 6);

        assertThat(grid.getRows()).isEqualTo(3);
        assertThat(grid.getCols()).isEqualTo(6);
        assertThat(grid.getTotalButtons()).isEqualTo(18);
        assertThat(grid.getButtons()).hasSize(18);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -5})
    @DisplayName("MatrixGridDefinition wirft Fehler bei ungültigen Zeilen oder Spalten")
    void testInvalidGridDimensions(int invalid) {
        assertThatThrownBy(() -> new MatrixGridDefinition(invalid, 8))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new MatrixGridDefinition(4, invalid))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("MatrixGridDefinition aktualisiert Buttons und erlaubt Zurücksetzen")
    void testGridButtonUpdates() {
        MatrixGridDefinition grid = new MatrixGridDefinition(2, 2);

        MatrixButtonDefinition newBtn = new MatrixButtonDefinition(
                1, 0, 1, "Chaser 1", "#FF9800", MatrixTargetType.CHASER, "chaser-1", MatrixTriggerMode.FLASH
        );

        grid.setButton(newBtn);

        assertThat(grid.getButton(1)).isEqualTo(newBtn);
        assertThat(grid.getButton(0, 1)).isEqualTo(newBtn);

        grid.clearButton(1);
        assertThat(grid.getButton(1).isAssigned()).isFalse();
        assertThat(grid.getButton(1).name()).isEmpty();
    }
}

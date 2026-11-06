package de.exit.jdmx.ui.stage;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für das vergrößerte Farb-Steuerungsmodul (ColorModuleView).
 * Großer HSV-Farbkreis (ohne HSV-Schriftzug) sowie je 4 anpassbare Farb-Buttons links und rechts.
 */
class ColorModuleViewTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit aktiv
        }
    }

    @Test
    @DisplayName("Sollte großen Farbkreis ohne HSV-Schriftzug sowie 4 Buttons links und 4 rechts enthalten")
    void shouldContainLargeColorWheelAndCustomizableButtons() {
        ColorModuleView module = new ColorModuleView();

        assertThat(module.getTitle()).isEqualTo("Farbe");
        assertThat(module.getColorWheel()).isNotNull();
        // Farbkreis groß: mindestens 94px Breite/Höhe
        assertThat(module.getColorWheel().getWidth()).isGreaterThanOrEqualTo(94.0);
        assertThat(module.getColorWheel().getHeight()).isGreaterThanOrEqualTo(94.0);

        // 4 Farb-Buttons links und 4 Farb-Buttons rechts
        assertThat(module.getLeftPresetButtons()).hasSize(4);
        assertThat(module.getRightPresetButtons()).hasSize(4);
    }

    @Test
    @DisplayName("Sollte ausgewählte Farbe programmatisch anpassen können")
    void shouldUpdateSelectedColor() {
        ColorModuleView module = new ColorModuleView();

        module.selectColor(Color.RED);
        assertThat(module.getSelectedColor()).isEqualTo(Color.RED);
    }

    @Test
    @DisplayName("Sollte Klick auf Preset-Button die Farbe auswählen")
    void shouldSelectColorWhenPresetButtonClicked() {
        ColorModuleView module = new ColorModuleView();

        // Klick auf den ersten linken Preset-Button (z. B. Weiß)
        module.getLeftPresetButtons().get(0).fire();
        assertThat(module.getSelectedColor()).isNotNull();
    }

    @Test
    @DisplayName("Sollte Preset-Button-Farben customizen können")
    void shouldCustomizePresetButtonColor() {
        ColorModuleView module = new ColorModuleView();

        Color customColor = Color.web("#FF00AA");
        module.setLeftPresetColor(0, customColor);

        assertThat(module.getLeftPresetColor(0)).isEqualTo(customColor);
    }

    @Test
    @DisplayName("Sollte vordefinierte Presets Weiß, Schwarz, Rot, Grün, Blau und 3 leere Slots enthalten")
    void shouldContainWhiteBlackRedGreenBlueAndEmptySlots() {
        ColorModuleView module = new ColorModuleView();

        // Links: Weiß, Schwarz, Rot, Grün
        assertThat(module.getLeftPresetColor(0)).isEqualTo(Color.web("#FFFFFF"));
        assertThat(module.getLeftPresetColor(1)).isEqualTo(Color.web("#000000"));
        assertThat(module.getLeftPresetColor(2)).isEqualTo(Color.web("#EF4444"));
        assertThat(module.getLeftPresetColor(3)).isEqualTo(Color.web("#10B981"));

        // Rechts: Blau und 3 leere Slots (null)
        assertThat(module.getRightPresetColor(0)).isEqualTo(Color.web("#3B82F6"));
        assertThat(module.getRightPresetColor(1)).isNull();
        assertThat(module.getRightPresetColor(2)).isNull();
        assertThat(module.getRightPresetColor(3)).isNull();

        // Klick auf Schwarz wählt Schwarz
        module.getLeftPresetButtons().get(1).fire();
        assertThat(module.getSelectedColor()).isEqualTo(Color.web("#000000"));

        // Klick auf leeren Slot ändert die Farbe nicht
        Color before = module.getSelectedColor();
        module.getRightPresetButtons().get(1).fire();
        assertThat(module.getSelectedColor()).isEqualTo(before);
    }
}

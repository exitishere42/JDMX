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
}

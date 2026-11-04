package de.exit.jdmx.ui.stage;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für das Farb-Steuerungsmodul (ColorModuleView).
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
    @DisplayName("Sollte Farbkreis und Schnell-Farbpaletten bereitstellen")
    void shouldContainColorControls() {
        ColorModuleView module = new ColorModuleView();

        assertThat(module.getTitle()).isEqualTo("FARBE");
        assertThat(module.getColorWheel()).isNotNull();
        assertThat(module.getColorPresets()).isNotEmpty();
    }

    @Test
    @DisplayName("Sollte ausgewählte Farbe über Preset-Klick anpassen")
    void shouldSelectColorFromPreset() {
        ColorModuleView module = new ColorModuleView();

        // Klick auf den roten Preset-Button
        module.selectColor(Color.RED);
        assertThat(module.getSelectedColor()).isEqualTo(Color.RED);
    }
}

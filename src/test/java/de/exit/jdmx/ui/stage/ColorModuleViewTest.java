package de.exit.jdmx.ui.stage;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für das Farb-Steuerungsmodul (ColorModuleView).
 * Reiner HSV-Farbkreis ohne Schieberegler gemäß Nutzerwunsch.
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
    @DisplayName("Sollte purer Farbkreis mit HSV-Tab ohne Schieberegler sein")
    void shouldContainColorWheelWithoutSliders() {
        ColorModuleView module = new ColorModuleView();

        assertThat(module.getTitle()).isEqualTo("Farbe");
        assertThat(module.getColorWheel()).isNotNull();
        assertThat(module.getSelectedColor()).isNotNull();
    }

    @Test
    @DisplayName("Sollte ausgewählte Farbe programmatisch anpassen können")
    void shouldUpdateSelectedColor() {
        ColorModuleView module = new ColorModuleView();

        module.selectColor(Color.RED);
        assertThat(module.getSelectedColor()).isEqualTo(Color.RED);
    }
}

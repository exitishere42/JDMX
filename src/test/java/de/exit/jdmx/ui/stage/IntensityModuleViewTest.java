package de.exit.jdmx.ui.stage;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für das Intensitäts-Steuerungsmodul (IntensityModuleView).
 */
class IntensityModuleViewTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit aktiv
        }
    }

    @Test
    @DisplayName("Sollte 5er-Tastenleiste, 3 Fader (Dimmer, Strobo, Shutter) und 2x2 Block enthalten")
    void shouldContainAllIntensityControls() {
        IntensityModuleView module = new IntensityModuleView();

        assertThat(module.getTitle()).isEqualTo("Intensität");
        assertThat(module.getDimmerSlider()).isNotNull();
        assertThat(module.getStrobeSlider()).isNotNull();
        assertThat(module.getShutterSlider()).isNotNull();

        assertThat(module.getQuickButtons()).hasSize(5); // VL, 70, 50, 30, 0
        assertThat(module.getQuickButtons().get(0).getText()).isEqualTo("VL");

        assertThat(module.getBtnAuf()).isNotNull();
        assertThat(module.getBtnZu()).isNotNull();
        assertThat(module.getBtnLumos()).isNotNull();
        assertThat(module.getBtnNox()).isNotNull();
    }

    @Test
    @DisplayName("Sollte Dimmer-Wert per Schnellwahltaste anpassen können")
    void shouldUpdateDimmerViaQuickButton() {
        IntensityModuleView module = new IntensityModuleView();

        // 70 Taste anklicken
        module.getQuickButtons().get(1).fire(); // 70
        assertThat(module.getDimmerSlider().getValue()).isEqualTo(70.0);

        // 0 Taste anklicken
        module.getQuickButtons().get(4).fire(); // 0
        assertThat(module.getDimmerSlider().getValue()).isEqualTo(0.0);

        // VL Taste anklicken (100%)
        module.getQuickButtons().get(0).fire(); // VL
        assertThat(module.getDimmerSlider().getValue()).isEqualTo(100.0);
    }
}

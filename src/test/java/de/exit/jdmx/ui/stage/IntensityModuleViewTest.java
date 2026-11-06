package de.exit.jdmx.ui.stage;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für das Intensitäts-Steuerungsmodul (IntensityModuleView).
 * Entspricht dem DMXControl-Vorbild: 5er-Tastenleiste (VL, 70, 50, 30, 0),
 * 2 Fader (Dimmer, Strobo - ohne Shutter) und 2x2 Block (Auf, Zu, Lumos, Nox).
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
    @DisplayName("Sollte 5er-Tastenleiste, 2 Fader (Dimmer, Strobo - OHNE Shutter) und 2x2 Block enthalten")
    void shouldContainAllIntensityControlsWithoutShutter() {
        IntensityModuleView module = new IntensityModuleView();

        assertThat(module.getTitle()).isEqualTo("Intensität");
        assertThat(module.getDimmerSlider()).isNotNull();
        assertThat(module.getStrobeSlider()).isNotNull();

        assertThat(module.getQuickButtons()).hasSize(5); // VL, 70, 50, 30, 0
        assertThat(module.getQuickButtons().get(0).getText()).isEqualTo("VL");
        assertThat(module.getQuickButtons().get(1).getText()).isEqualTo("70");
        assertThat(module.getQuickButtons().get(2).getText()).isEqualTo("50");
        assertThat(module.getQuickButtons().get(3).getText()).isEqualTo("30");
        assertThat(module.getQuickButtons().get(4).getText()).isEqualTo("0");

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

    @Test
    @DisplayName("Sollte vergrößerte ergonomische Tastenhöhe (50px) für Zu und Nox Tasten besitzen")
    void shouldHaveErgonomicHeightForZuAndNoxButtons() {
        IntensityModuleView module = new IntensityModuleView();

        assertThat(module.getBtnZu().getPrefHeight()).isEqualTo(50.0);
        assertThat(module.getBtnZu().getMinHeight()).isGreaterThanOrEqualTo(44.0);

        assertThat(module.getBtnNox().getPrefHeight()).isEqualTo(50.0);
        assertThat(module.getBtnNox().getMinHeight()).isGreaterThanOrEqualTo(44.0);
    }

    @Test
    @DisplayName("Sollte Dimmer-Command auch feuern wenn Slider bereits denselben Wert hat (Lumos und Nox)")
    void shouldTriggerDimmerCommandOnLumosAndNox() {
        IntensityModuleView module = new IntensityModuleView();
        double[] lastReported = {-1.0};
        module.setOnDimmerCommand(val -> lastReported[0] = val);

        // Lumos feuern
        module.getBtnLumos().fire();
        assertThat(module.getDimmerSlider().getValue()).isEqualTo(100.0);
        assertThat(lastReported[0]).isEqualTo(100.0);

        // Nochmal Lumos feuern (obwohl Slider bereits auf 100 ist)
        lastReported[0] = -1.0;
        module.getBtnLumos().fire();
        assertThat(lastReported[0]).isEqualTo(100.0);

        // Nox feuern
        module.getBtnNox().fire();
        assertThat(module.getDimmerSlider().getValue()).isEqualTo(0.0);
        assertThat(lastReported[0]).isEqualTo(0.0);
    }
}

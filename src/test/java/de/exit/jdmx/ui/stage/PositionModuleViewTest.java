package de.exit.jdmx.ui.stage;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für das Positionierungs-Modul (PositionModuleView) mit Pan/Tilt XY-Pad.
 */
class PositionModuleViewTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit aktiv
        }
    }

    @Test
    @DisplayName("Sollte XY-Fadenkreuz, Pan/Tilt-Slider, Invert-Checkboxen und Home-Button enthalten")
    void shouldContainAllPositionControls() {
        PositionModuleView module = new PositionModuleView();

        assertThat(module.getTitle()).isEqualTo("POSITION");
        assertThat(module.getXyPad()).isNotNull();
        assertThat(module.getPanSlider()).isNotNull();
        assertThat(module.getTiltSlider()).isNotNull();

        assertThat(module.getCbInvertPan()).isNotNull();
        assertThat(module.getCbInvertTilt()).isNotNull();
        assertThat(module.getBtnHome()).isNotNull();
    }

    @Test
    @DisplayName("Sollte Home-Button Pan und Tilt auf 50% (Mitte) zurücksetzen")
    void shouldResetToHomePosition() {
        PositionModuleView module = new PositionModuleView();

        // Verstellen
        module.getPanSlider().setValue(10.0);
        module.getTiltSlider().setValue(90.0);

        // Home drücken
        module.getBtnHome().fire();

        assertThat(module.getPanSlider().getValue()).isEqualTo(50.0);
        assertThat(module.getTiltSlider().getValue()).isEqualTo(50.0);
    }
}

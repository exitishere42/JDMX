package de.exit.jdmx.ui.stage;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für das vergrößerte Positionierungs-Modul (PositionModuleView).
 * Großes Pan/Tilt XY-Pad (nahezu bis zum Border) mit Home-Button und Pan/Tilt-Slidern.
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
    @DisplayName("Sollte vergrößertes XY-Pad (mind. 90x90px), Pan/Tilt-Slider und Home-Button enthalten")
    void shouldContainLargePositionControls() {
        PositionModuleView module = new PositionModuleView();

        assertThat(module.getTitle()).isEqualTo("Position");
        assertThat(module.getXyPad()).isNotNull();
        // Großes Pad (mindestens 90x90px)
        assertThat(module.getXyPad().getWidth()).isGreaterThanOrEqualTo(90.0);
        assertThat(module.getXyPad().getHeight()).isGreaterThanOrEqualTo(90.0);

        assertThat(module.getPanSlider()).isNotNull();
        assertThat(module.getTiltSlider()).isNotNull();
        assertThat(module.getBtnHome()).isNotNull();
    }

    @Test
    @DisplayName("Sollte Home-Button Pan und Tilt auf 50% (Mitte) zurücksetzen")
    void shouldResetToHomePosition() {
        PositionModuleView module = new PositionModuleView();

        // Verstellen
        module.getPanSlider().setValue(15.0);
        module.getTiltSlider().setValue(85.0);

        // Home drücken
        module.getBtnHome().fire();

        assertThat(module.getPanSlider().getValue()).isEqualTo(50.0);
        assertThat(module.getTiltSlider().getValue()).isEqualTo(50.0);
    }
}

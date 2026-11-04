package de.exit.jdmx.ui;

import de.exit.jdmx.ui.icon.LucideIcon;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für {@link LucideIcon}.
 */
class LucideIconTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // JavaFX Toolkit bereits aktiv
        }
    }

    @Test
    @DisplayName("Sollte arrow-right-left Icon für Input/Output korrekt erzeugen")
    void shouldCreateArrowRightLeftIcon() {
        LucideIcon icon = new LucideIcon("arrow-right-left", 16, Color.web("#4ade80"));
        assertThat(icon).isNotNull();
        assertThat(LucideIcon.getSvgPathData("arrow-right-left")).contains("M16 3");
    }

    @Test
    @DisplayName("Sollte lightbulb Icon für App-Logo korrekt erzeugen")
    void shouldCreateLightbulbIcon() {
        LucideIcon icon = new LucideIcon("lightbulb", 18, Color.web("#FFB74D"));
        assertThat(icon).isNotNull();
        assertThat(LucideIcon.getSvgPathData("lightbulb")).contains("M15 14");
    }
}

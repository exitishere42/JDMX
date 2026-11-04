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
        LucideIcon icon = new LucideIcon("lightbulb", 18, Color.web("#FEDC94"));
        assertThat(icon).isNotNull();
        assertThat(LucideIcon.getSvgPathData("lightbulb")).contains("M15 14");
    }

    @Test
    @DisplayName("Sollte alle Action-Menü Lucide-Icons bereitstellen")
    void shouldProvideAllActionMenuIcons() {
        assertThat(LucideIcon.getSvgPathData("file-plus")).contains("M15 2H6");
        assertThat(LucideIcon.getSvgPathData("folder-open")).contains("m6 14");
        assertThat(LucideIcon.getSvgPathData("save")).contains("M15.2 3");
        assertThat(LucideIcon.getSvgPathData("file-pen")).contains("M12 22h6");
        assertThat(LucideIcon.getSvgPathData("undo-2")).contains("M9 14");
        assertThat(LucideIcon.getSvgPathData("redo-2")).contains("m15 14");
        assertThat(LucideIcon.getSvgPathData("settings")).contains("M9.671");
        assertThat(LucideIcon.getSvgPathData("maximize")).contains("M8 3H5");
    }
}

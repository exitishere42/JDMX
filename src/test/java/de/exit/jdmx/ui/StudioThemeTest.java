package de.exit.jdmx.ui;

import de.exit.jdmx.ui.theme.StudioTheme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für das UI-Theme und Farb-Tokens von JDMX.
 */
class StudioThemeTest {

    @Test
    @DisplayName("Sollte valide Farb-Codes für Studio Dark Theme bereitstellen")
    void shouldProvideValidColorTokens() {
        assertThat(StudioTheme.HEX_BG_BASE).isEqualTo("#121212");
        assertThat(StudioTheme.HEX_CYAN_ACCENT).isEqualTo("#00E5FF");
        assertThat(StudioTheme.HEX_RED_STOP).isEqualTo("#EF4444");

        assertThat(StudioTheme.COLOR_RED).isNotNull();
        assertThat(StudioTheme.COLOR_CYAN).isNotNull();
    }
}

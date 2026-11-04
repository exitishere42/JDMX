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
    @DisplayName("Sollte neutrale Hellgrau-Primärfarbe und bunte Icon-Farben bereitstellen")
    void shouldProvideValidColorTokens() {
        assertThat(StudioTheme.HEX_BG_BASE).isEqualTo("#121212");
        assertThat(StudioTheme.HEX_PRIMARY_GRAY).isEqualTo("#D1D5DB");
        assertThat(StudioTheme.COLOR_PRIMARY_GRAY).isNotNull();

        assertThat(StudioTheme.COLOR_ICON_STAGE).isNotNull();
        assertThat(StudioTheme.COLOR_ICON_FADER).isNotNull();
        assertThat(StudioTheme.COLOR_ICON_MATRIX).isNotNull();
        assertThat(StudioTheme.COLOR_ICON_TIMELINE).isNotNull();
        assertThat(StudioTheme.COLOR_ICON_IO).isNotNull();
        assertThat(StudioTheme.COLOR_ICON_RED).isNotNull();
        assertThat(StudioTheme.COLOR_ICON_GREEN).isNotNull();
    }
}

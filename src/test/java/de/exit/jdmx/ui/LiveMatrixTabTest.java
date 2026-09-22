package de.exit.jdmx.ui;

import de.exit.jdmx.ui.tabs.LiveMatrixTab;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test verifying LiveMatrixTab data structures and defaults.
 */
class LiveMatrixTabTest {

    @Test
    @DisplayName("PadData should initialize with correct default values")
    void testPadDataDefaults() {
        LiveMatrixTab.PadData pad = new LiveMatrixTab.PadData();
        assertThat(pad.isEmpty).isTrue();
        assertThat(pad.isFlash).isFalse();
        assertThat(pad.active).isFalse();
        assertThat(pad.color).isEqualTo(Color.web("#ffff00"));
    }

    @Test
    @DisplayName("PadData active state can be toggled")
    void testPadDataActiveToggle() {
        LiveMatrixTab.PadData pad = new LiveMatrixTab.PadData();
        pad.isEmpty = false;
        pad.active = true;
        assertThat(pad.active).isTrue();

        pad.active = false;
        assertThat(pad.active).isFalse();
    }
}

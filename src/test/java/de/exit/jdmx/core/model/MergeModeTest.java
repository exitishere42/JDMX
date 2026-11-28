package de.exit.jdmx.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MergeModeTest {

    @Test
    @DisplayName("MergeMode enthält HTP, LTP und OVERRIDE mit korrekten Bezeichnungen")
    void testMergeModeValues() {
        assertThat(MergeMode.values()).containsExactly(
                MergeMode.HTP,
                MergeMode.LTP,
                MergeMode.OVERRIDE
        );

        assertThat(MergeMode.HTP.getDisplayName()).isEqualTo("HTP");
        assertThat(MergeMode.LTP.getDisplayName()).isEqualTo("LTP");
        assertThat(MergeMode.OVERRIDE.getDisplayName()).isEqualTo("Override");
    }

    @Test
    @DisplayName("fromString parst Werte case-insensitive oder fällt auf HTP zurück")
    void testFromString() {
        assertThat(MergeMode.fromString("htp")).isEqualTo(MergeMode.HTP);
        assertThat(MergeMode.fromString("LTP")).isEqualTo(MergeMode.LTP);
        assertThat(MergeMode.fromString("Override")).isEqualTo(MergeMode.OVERRIDE);
        assertThat(MergeMode.fromString("unbekannt")).isEqualTo(MergeMode.HTP);
        assertThat(MergeMode.fromString(null)).isEqualTo(MergeMode.HTP);
    }
}

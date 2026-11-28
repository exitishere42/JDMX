package de.exit.jdmx.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UniverseConfigMergeModeTest {

    @Test
    @DisplayName("defaultFor initialisiert mergeMode mit HTP")
    void testDefaultForHasHtp() {
        UniverseConfig cfg = UniverseConfig.defaultFor(0);
        assertThat(cfg.mergeMode()).isEqualTo(MergeMode.HTP);
    }

    @Test
    @DisplayName("withMergeMode ändert den MergeMode korrekt")
    void testWithMergeMode() {
        UniverseConfig cfg = UniverseConfig.defaultFor(0);
        UniverseConfig ltp = cfg.withMergeMode(MergeMode.LTP);

        assertThat(ltp.mergeMode()).isEqualTo(MergeMode.LTP);
        assertThat(ltp.universeIndex()).isEqualTo(0);
        assertThat(ltp.artNetIp()).isEqualTo(cfg.artNetIp());

        UniverseConfig override = ltp.withMergeMode(MergeMode.OVERRIDE);
        assertThat(override.mergeMode()).isEqualTo(MergeMode.OVERRIDE);
    }
}

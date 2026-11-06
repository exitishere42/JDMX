package de.exit.jdmx.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Unit-Tests für {@link UniverseConfig}.
 */
class UniverseConfigTest {

    @Test
    @DisplayName("Sollte sinnvolle Standardwerte für ein Universum erzeugen")
    void shouldCreateDefaultConfig() {
        UniverseConfig config = UniverseConfig.defaultFor(0);

        assertThat(config.universeIndex()).isEqualTo(0);
        assertThat(config.universeNumber()).isEqualTo(1);
        assertThat(config.artNetIp()).isEqualTo("127.0.0.1");
        assertThat(config.artNetPort()).isEqualTo(6454);
        assertThat(config.artNetSubnet()).isEqualTo(0);
        assertThat(config.artNetUniverse()).isEqualTo(0);
        assertThat(config.refreshRateHz()).isEqualTo(40);
        assertThat(config.isBroadcast()).isFalse();
    }

    @Test
    @DisplayName("Sollte Werte über With-Methoden unveränderlich aktualisieren")
    void shouldUpdateValuesImmutably() {
        UniverseConfig original = UniverseConfig.defaultFor(1);
        UniverseConfig updated = original
                .withArtNetIp("192.168.1.100")
                .withArtNetPort(6455)
                .withArtNetSubnet(2)
                .withArtNetUniverse(3)
                .withBroadcast(true)
                .withRefreshRateHz(35);

        assertThat(updated.artNetIp()).isEqualTo("192.168.1.100");
        assertThat(updated.artNetPort()).isEqualTo(6455);
        assertThat(updated.artNetSubnet()).isEqualTo(2);
        assertThat(updated.artNetUniverse()).isEqualTo(3);
        assertThat(updated.isBroadcast()).isTrue();
        assertThat(updated.refreshRateHz()).isEqualTo(35);

        // Original bleibt unverändert
        assertThat(original.artNetIp()).isEqualTo("127.0.0.1");
    }
}

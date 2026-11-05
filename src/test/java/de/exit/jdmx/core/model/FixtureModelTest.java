package de.exit.jdmx.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit-Tests für das Core Fixture Datenmodell.
 */
class FixtureModelTest {

    @Test
    @DisplayName("Sollte ChannelDefinition mit Typ und 16-Bit Fine Flag erstellen")
    void shouldCreateChannelDefinition() {
        ChannelDefinition chDimmer = new ChannelDefinition(1, "Dimmer", ChannelType.DIMMER, 0, false);
        ChannelDefinition chPan = new ChannelDefinition(2, "Pan", ChannelType.PAN, 128, false);
        ChannelDefinition chPanFine = new ChannelDefinition(3, "Pan Fine", ChannelType.PAN_FINE, 0, true);

        assertThat(chDimmer.index()).isEqualTo(1);
        assertThat(chDimmer.name()).isEqualTo("Dimmer");
        assertThat(chDimmer.type()).isEqualTo(ChannelType.DIMMER);
        assertThat(chDimmer.defaultValue()).isEqualTo(0);
        assertThat(chDimmer.isFine()).isFalse();

        assertThat(chPan.isFine()).isFalse();
        assertThat(chPanFine.isFine()).isTrue();
    }

    @Test
    @DisplayName("Sollte DmxMode mit Kanälen verwalten und Kanalanzahl validieren")
    void shouldCreateDmxMode() {
        ChannelDefinition ch1 = new ChannelDefinition(1, "Dimmer", ChannelType.DIMMER, 0, false);
        ChannelDefinition ch2 = new ChannelDefinition(2, "Strobe", ChannelType.SHUTTER, 0, false);
        ChannelDefinition ch3 = new ChannelDefinition(3, "Red", ChannelType.RED, 255, false);

        DmxMode mode = new DmxMode("3CH", List.of(ch1, ch2, ch3));

        assertThat(mode.getName()).isEqualTo("3CH");
        assertThat(mode.getChannelCount()).isEqualTo(3);
        assertThat(mode.getChannels()).containsExactly(ch1, ch2, ch3);
    }

    @Test
    @DisplayName("Sollte FixtureDefinition mit Modi und Metadaten erstellen")
    void shouldCreateFixtureDefinition() {
        ChannelDefinition chDimmer = new ChannelDefinition(1, "Dimmer", ChannelType.DIMMER, 0, false);
        DmxMode mode1Ch = new DmxMode("1CH", List.of(chDimmer));

        FixtureDefinition def = new FixtureDefinition(
                "generic-dimmer",
                "Generic",
                "Dimmer",
                ExplorerItemType.FIXTURE,
                List.of(mode1Ch)
        );

        assertThat(def.getId()).isEqualTo("generic-dimmer");
        assertThat(def.getManufacturer()).isEqualTo("Generic");
        assertThat(def.getModel()).isEqualTo("Dimmer");
        assertThat(def.getModes()).hasSize(1);
        assertThat(def.getDefaultMode()).isEqualTo(mode1Ch);
    }

    @Test
    @DisplayName("Sollte FixtureInstance mit Startadresse und Universum validieren")
    void shouldCreateFixtureInstanceAndValidateAddresses() {
        ChannelDefinition chDimmer = new ChannelDefinition(1, "Dimmer", ChannelType.DIMMER, 0, false);
        DmxMode mode = new DmxMode("1CH", List.of(chDimmer));
        FixtureDefinition def = new FixtureDefinition("generic-dimmer", "Generic", "Dimmer", ExplorerItemType.FIXTURE, List.of(mode));

        FixtureInstance instance = new FixtureInstance("spot-1", "Front 1", def, mode, 1, 10, 150.0, 200.0);

        assertThat(instance.getId()).isEqualTo("spot-1");
        assertThat(instance.getName()).isEqualTo("Front 1");
        assertThat(instance.getUniverse()).isEqualTo(1);
        assertThat(instance.getStartAddress()).isEqualTo(10);
        assertThat(instance.getEndAddress()).isEqualTo(10); // 1-Kanal -> Endadresse 10
        assertThat(instance.getStageX()).isEqualTo(150.0);
        assertThat(instance.getStageY()).isEqualTo(200.0);

        // Kollisions-Check: Bereich 10..10 kollidiert mit Adresse 10
        assertThat(instance.occupiesAddress(1, 10)).isTrue();
        assertThat(instance.occupiesAddress(1, 11)).isFalse();
        assertThat(instance.occupiesAddress(2, 10)).isFalse(); // Anderes Universum
    }

    @Test
    @DisplayName("Sollte ungültige DMX-Adressen (0 oder > 512) abfangen")
    void shouldRejectInvalidDmxAddresses() {
        ChannelDefinition chDimmer = new ChannelDefinition(1, "Dimmer", ChannelType.DIMMER, 0, false);
        DmxMode mode = new DmxMode("1CH", List.of(chDimmer));
        FixtureDefinition def = new FixtureDefinition("generic-dimmer", "Generic", "Dimmer", ExplorerItemType.FIXTURE, List.of(mode));

        assertThatThrownBy(() -> new FixtureInstance("id", "Name", def, mode, 1, 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new FixtureInstance("id", "Name", def, mode, 1, 513, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

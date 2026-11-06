package de.exit.jdmx.core.fixture.parser;

import de.exit.jdmx.core.model.ChannelType;
import de.exit.jdmx.core.model.ExplorerItemType;
import de.exit.jdmx.core.model.FixtureDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit-Tests für den nativen QLC+ QXF XML Parser ({@link QxfParser}).
 */
class QxfParserTest {

    private final QxfParser parser = new QxfParser();

    @Test
    @DisplayName("Sollte .qxf Endung unterstützen")
    void shouldSupportQxfExtension() {
        assertThat(parser.supports("stairville-mhx50.qxf")).isTrue();
        assertThat(parser.supports("stairville-mhx50.QXF")).isTrue();
        assertThat(parser.supports("device.gdtf")).isFalse();
        assertThat(parser.supports("device.json")).isFalse();
    }

    @Test
    @DisplayName("Sollte valide QLC+ QXF XML-Datei fehlerfrei parsen")
    void shouldParseValidQxfXml() throws Exception {
        String sampleQxf = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE FixtureDefinition>
                <FixtureDefinition xmlns="http://www.qlcplus.org/FixtureDefinition">
                 <Creator>
                  <Name>Q Light Controller Plus</Name>
                  <Version>4.12.7</Version>
                 </Creator>
                 <Manufacturer>Showtec</Manufacturer>
                 <Model>Phantom 50 LED Spot</Model>
                 <Type>Moving Head</Type>
                 <Channel Name="Pan">
                  <Group Byte="0">Pan</Group>
                  <Capability Min="0" Max="255">Pan (0 - 540)</Capability>
                 </Channel>
                 <Channel Name="Pan fine">
                  <Group Byte="1">Pan</Group>
                  <Capability Min="0" Max="255">Pan fine (16-bit)</Capability>
                 </Channel>
                 <Channel Name="Tilt">
                  <Group Byte="0">Tilt</Group>
                  <Capability Min="0" Max="255">Tilt (0 - 270)</Capability>
                 </Channel>
                 <Channel Name="Dimmer">
                  <Group Byte="0">Intensity</Group>
                  <Capability Min="0" Max="255">0% to 100%</Capability>
                 </Channel>
                 <Channel Name="Red">
                  <Group Byte="0">Intensity</Group>
                  <Colour>Red</Colour>
                  <Capability Min="0" Max="255">Red</Capability>
                 </Channel>
                 <Mode Name="11 Channel">
                  <Channel Number="0">Pan</Channel>
                  <Channel Number="1">Tilt</Channel>
                  <Channel Number="2">Dimmer</Channel>
                 </Mode>
                 <Mode Name="14 Channel">
                  <Channel Number="0">Pan</Channel>
                  <Channel Number="1">Pan fine</Channel>
                  <Channel Number="2">Tilt</Channel>
                  <Channel Number="3">Dimmer</Channel>
                  <Channel Number="4">Red</Channel>
                 </Mode>
                </FixtureDefinition>
                """;

        ByteArrayInputStream in = new ByteArrayInputStream(sampleQxf.getBytes(StandardCharsets.UTF_8));
        FixtureDefinition def = parser.parse(in, "phantom50.qxf");

        assertThat(def).isNotNull();
        assertThat(def.getManufacturer()).isEqualTo("Showtec");
        assertThat(def.getModel()).isEqualTo("Phantom 50 LED Spot");
        assertThat(def.getType()).isEqualTo(ExplorerItemType.FIXTURE);

        // Prüfe Modi
        assertThat(def.getModes()).hasSize(2);

        // 1. Modus: 11 Channel
        assertThat(def.getModes().get(0).getName()).isEqualTo("11 Channel");
        assertThat(def.getModes().get(0).getChannelCount()).isEqualTo(3);
        assertThat(def.getModes().get(0).getChannels().get(0).type()).isEqualTo(ChannelType.PAN);
        assertThat(def.getModes().get(0).getChannels().get(1).type()).isEqualTo(ChannelType.TILT);
        assertThat(def.getModes().get(0).getChannels().get(2).type()).isEqualTo(ChannelType.DIMMER);

        // 2. Modus: 14 Channel (mit 16-Bit Pan Fine)
        assertThat(def.getModes().get(1).getName()).isEqualTo("14 Channel");
        assertThat(def.getModes().get(1).getChannelCount()).isEqualTo(5);
        assertThat(def.getModes().get(1).getChannels().get(1).type()).isEqualTo(ChannelType.PAN_FINE);
        assertThat(def.getModes().get(1).getChannels().get(1).isFine()).isTrue();
        assertThat(def.getModes().get(1).getChannels().get(4).type()).isEqualTo(ChannelType.RED);
    }

    @Test
    @DisplayName("Sollte fehlerhaftes XML abfangen und Exception werfen")
    void shouldThrowOnCorruptXml() {
        ByteArrayInputStream in = new ByteArrayInputStream("<Invalid>Broken".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> parser.parse(in, "corrupt.qxf"))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Sollte QLC+ Presets wie Dimming, PositionPanFine, ShutterStrobe und White sauber erkennen")
    void shouldParsePresetAttributesAndUKingFixture() throws Exception {
        String ukingQxf = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE FixtureDefinition>
                <FixtureDefinition xmlns="http://www.qlcplus.org/FixtureDefinition">
                 <Creator>
                  <Name>Q Light Controller Plus</Name>
                  <Version>5.2.0</Version>
                  <Author>Timo</Author>
                 </Creator>
                 <Manufacturer>UKing</Manufacturer>
                 <Model>ZQ-02042</Model>
                 <Type>Moving Head</Type>
                 <Channel Name="Pan" Preset="PositionPan"/>
                 <Channel Name="Pan fine" Preset="PositionPanFine"/>
                 <Channel Name="Tilt" Preset="PositionTilt"/>
                 <Channel Name="Tilt fine" Preset="PositionTiltFine"/>
                 <Channel Name="P/T speed" Preset="SpeedPanTiltFastSlow"/>
                 <Channel Name="Dimming" Preset="IntensityMasterDimmer"/>
                 <Channel Name="Strobe" Preset="ShutterStrobeSlowFast"/>
                 <Channel Name="Red" Preset="IntensityRed"/>
                 <Channel Name="Green" Preset="IntensityGreen"/>
                 <Channel Name="Blue" Preset="IntensityBlue"/>
                 <Channel Name="White" Preset="IntensityWhite"/>
                 <Channel Name="Mode">
                  <Group Byte="0">Effect</Group>
                  <Capability Min="0" Max="30">No function</Capability>
                  </Channel>
                 <Channel Name="Reset">
                  <Group Byte="0">Maintenance</Group>
                  <Capability Min="0" Max="149">No function</Capability>
                 </Channel>
                 <Mode Name="13 Channel">
                  <Channel Number="0">Pan</Channel>
                  <Channel Number="1">Pan fine</Channel>
                  <Channel Number="2">Tilt</Channel>
                  <Channel Number="3">Tilt fine</Channel>
                  <Channel Number="4">P/T speed</Channel>
                  <Channel Number="5">Dimming</Channel>
                  <Channel Number="6">Strobe</Channel>
                  <Channel Number="7">Red</Channel>
                  <Channel Number="8">Green</Channel>
                  <Channel Number="9">Blue</Channel>
                  <Channel Number="10">White</Channel>
                  <Channel Number="11">Mode</Channel>
                  <Channel Number="12">Reset</Channel>
                 </Mode>
                </FixtureDefinition>
                """;

        ByteArrayInputStream in = new ByteArrayInputStream(ukingQxf.getBytes(StandardCharsets.UTF_8));
        FixtureDefinition def = parser.parse(in, "uking-zq-02042.qxf");

        assertThat(def).isNotNull();
        assertThat(def.getManufacturer()).isEqualTo("UKing");
        assertThat(def.getModel()).isEqualTo("ZQ-02042");

        var channels = def.getModes().getFirst().getChannels();
        assertThat(channels).hasSize(13);

        assertThat(channels.get(0).type()).isEqualTo(ChannelType.PAN);
        assertThat(channels.get(1).type()).isEqualTo(ChannelType.PAN_FINE);
        assertThat(channels.get(1).isFine()).isTrue();
        assertThat(channels.get(2).type()).isEqualTo(ChannelType.TILT);
        assertThat(channels.get(3).type()).isEqualTo(ChannelType.TILT_FINE);
        assertThat(channels.get(3).isFine()).isTrue();
        assertThat(channels.get(4).type()).isEqualTo(ChannelType.SPEED);
        assertThat(channels.get(5).type()).isEqualTo(ChannelType.DIMMER); // "Dimming" mit Preset="IntensityMasterDimmer"
        assertThat(channels.get(6).type()).isEqualTo(ChannelType.SHUTTER); // "Strobe"
        assertThat(channels.get(7).type()).isEqualTo(ChannelType.RED);
        assertThat(channels.get(8).type()).isEqualTo(ChannelType.GREEN);
        assertThat(channels.get(9).type()).isEqualTo(ChannelType.BLUE);
        assertThat(channels.get(10).type()).isEqualTo(ChannelType.WHITE); // "White"
        assertThat(channels.get(11).type()).isEqualTo(ChannelType.EFFECT);
        assertThat(channels.get(12).type()).isEqualTo(ChannelType.MAINTENANCE); // "Reset" darf NICHT Red sein!
    }
}

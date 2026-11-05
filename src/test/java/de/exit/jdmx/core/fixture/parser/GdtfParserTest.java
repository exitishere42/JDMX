package de.exit.jdmx.core.fixture.parser;

import de.exit.jdmx.core.model.ChannelType;
import de.exit.jdmx.core.model.FixtureDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit-Tests für den nativen GDTF ZIP-Container Parser ({@link GdtfParser}).
 */
class GdtfParserTest {

    private final GdtfParser parser = new GdtfParser();

    @Test
    @DisplayName("Sollte .gdtf Endung unterstützen")
    void shouldSupportGdtfExtension() {
        assertThat(parser.supports("fixture.gdtf")).isTrue();
        assertThat(parser.supports("fixture.GDTF")).isTrue();
        assertThat(parser.supports("fixture.qxf")).isFalse();
    }

    @Test
    @DisplayName("Sollte valides GDTF ZIP-Archiv mit description.xml fehlerfrei parsen")
    void shouldParseValidGdtfZipArchive() throws Exception {
        String descriptionXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <GDTF DataVersion="1.2">
                 <FixtureType Name="Opus SP5" Manufacturer="Cameo">
                  <DMXModes>
                   <DMXMode Name="Standard">
                    <DMXChannels>
                     <DMXChannel DMXBreak="1" Offset="1">
                      <LogicalChannel Attribute="Dimmer"/>
                     </DMXChannel>
                     <DMXChannel DMXBreak="1" Offset="2">
                      <LogicalChannel Attribute="Pan"/>
                     </DMXChannel>
                     <DMXChannel DMXBreak="1" Offset="3">
                      <LogicalChannel Attribute="Tilt"/>
                     </DMXChannel>
                    </DMXChannels>
                   </DMXMode>
                  </DMXModes>
                 </FixtureType>
                </GDTF>
                """;

        // GDTF ZIP-Archiv im Speicher zusammenbauen
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry("description.xml");
            zos.putNextEntry(entry);
            zos.write(descriptionXml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        ByteArrayInputStream in = new ByteArrayInputStream(baos.toByteArray());
        FixtureDefinition def = parser.parse(in, "cameo-opus.gdtf");

        assertThat(def).isNotNull();
        assertThat(def.getManufacturer()).isEqualTo("Cameo");
        assertThat(def.getModel()).isEqualTo("Opus SP5");
        assertThat(def.getModes()).hasSize(1);

        assertThat(def.getModes().get(0).getName()).isEqualTo("Standard");
        assertThat(def.getModes().get(0).getChannelCount()).isEqualTo(3);
        assertThat(def.getModes().get(0).getChannels().get(0).type()).isEqualTo(ChannelType.DIMMER);
        assertThat(def.getModes().get(0).getChannels().get(1).type()).isEqualTo(ChannelType.PAN);
        assertThat(def.getModes().get(0).getChannels().get(2).type()).isEqualTo(ChannelType.TILT);
    }

    @Test
    @DisplayName("Sollte Exception werfen, wenn description.xml im Archiv fehlt")
    void shouldThrowWhenDescriptionXmlMissing() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry("other.txt");
            zos.putNextEntry(entry);
            zos.write("hello".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        ByteArrayInputStream in = new ByteArrayInputStream(baos.toByteArray());
        assertThatThrownBy(() -> parser.parse(in, "corrupt.gdtf"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("description.xml");
    }
}

package de.exit.jdmx.service;

import de.exit.jdmx.core.model.ChannelDefinition;
import de.exit.jdmx.core.model.ChannelType;
import de.exit.jdmx.core.model.DmxMode;
import de.exit.jdmx.core.model.ExplorerItemType;
import de.exit.jdmx.core.model.FixtureDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für den {@link FixtureLibraryService}.
 */
class FixtureLibraryServiceTest {

    @Test
    @DisplayName("Sollte Standard-Geräte vorinstalliert initialisieren")
    void shouldInitializeWithBuiltinFixtures() {
        FixtureLibraryService service = new FixtureLibraryService();

        assertThat(service.getAllDefinitions()).isNotEmpty();
        assertThat(service.getManufacturers()).contains("Generic");

        // Suche nach Dimmer oder Generic
        List<FixtureDefinition> results = service.search("Generic");
        assertThat(results).isNotEmpty();
    }

    @Test
    @DisplayName("Sollte QXF-Datei über den Service importieren und registrieren")
    void shouldImportQxfFile() throws Exception {
        FixtureLibraryService service = new FixtureLibraryService();

        String qxf = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE FixtureDefinition>
                <FixtureDefinition xmlns="http://www.qlcplus.org/FixtureDefinition">
                 <Manufacturer>Stairville</Manufacturer>
                 <Model>MH-X50</Model>
                 <Type>Moving Head</Type>
                 <Channel Name="Pan"><Group Byte="0">Pan</Group></Channel>
                 <Channel Name="Tilt"><Group Byte="0">Tilt</Group></Channel>
                 <Mode Name="2CH">
                  <Channel Number="0">Pan</Channel>
                  <Channel Number="1">Tilt</Channel>
                 </Mode>
                </FixtureDefinition>
                """;

        ByteArrayInputStream in = new ByteArrayInputStream(qxf.getBytes(StandardCharsets.UTF_8));
        FixtureDefinition imported = service.importFixture(in, "mhx50.qxf");

        assertThat(imported).isNotNull();
        assertThat(imported.getManufacturer()).isEqualTo("Stairville");
        assertThat(imported.getModel()).isEqualTo("MH-X50");

        assertThat(service.getAllDefinitions()).contains(imported);
        assertThat(service.getManufacturers()).contains("Stairville");
        assertThat(service.getByManufacturer("Stairville")).contains(imported);
    }

    @Test
    @DisplayName("Sollte Suche nach Begriffen wie 'RGB' oder 'Moving' filtern")
    void shouldSearchFixtures() {
        FixtureLibraryService service = new FixtureLibraryService();

        ChannelDefinition ch = new ChannelDefinition(1, "Dimmer", ChannelType.DIMMER, 0, false);
        DmxMode mode = new DmxMode("1CH", List.of(ch));
        service.addDefinition(new FixtureDefinition("uking-b230", "U'King", "B230 Mini Spot", ExplorerItemType.FIXTURE, List.of(mode)));

        List<FixtureDefinition> found = service.search("B230");
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getManufacturer()).isEqualTo("U'King");
    }

    @Test
    @DisplayName("Sollte Benutzer-Verzeichnisse für QXF-Profile scannen falls vorhanden")
    void shouldScanUserDirectoriesIfPresent() {
        FixtureLibraryService service = new FixtureLibraryService();
        java.io.File qlcDir = new java.io.File(System.getProperty("user.home"), "Documents/QLC+ Saves/qxf");
        if (qlcDir.exists() && qlcDir.isDirectory()) {
            assertThat(service.getAllDefinitions().size()).isGreaterThan(10);
            assertThat(service.getManufacturers()).contains("UKing");
        }
    }
}

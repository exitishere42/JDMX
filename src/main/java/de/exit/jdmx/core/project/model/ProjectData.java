package de.exit.jdmx.core.project.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

/**
 * Persistentes Datenmodell für ein JDMX-Projekt (*.jdmx).
 * Speichert Metadaten, Universums-Routings sowie gepatchte Fixtures mitsamt Koordinaten.
 *
 * @param version     Formatversion der Projektdatei (z. B. "1.0")
 * @param projectName Name des Projekts
 * @param savedAt     Zeitstempel der letzten Speicherung im ISO-8601-Format
 * @param universes   Liste der konfigurierten DMX-Universen
 * @param fixtures    Liste der gepatchten Fixtures mit Positionen
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProjectData(
        String version,
        String projectName,
        String savedAt,
        List<UniverseDataDto> universes,
        List<FixtureDataDto> fixtures
) {
    /**
     * Aktuelle Formatversion für neue Projektdateien.
     */
    public static final String CURRENT_VERSION = "1.0";

    /**
     * Erstellt ein leeres Projekt mit Vorgabewerten.
     *
     * @param name Name des neuen Projekts
     * @return Neue leere Instanz von {@link ProjectData}
     */
    public static ProjectData createEmpty(String name) {
        return new ProjectData(
                CURRENT_VERSION,
                name != null && !name.isBlank() ? name : "Default Project",
                Instant.now().toString(),
                List.of(),
                List.of()
        );
    }
}

package de.exit.jdmx.core.project.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

/**
 * Persistentes Datenmodell für ein JDMX-Projekt (*.jdmx).
 * Speichert Metadaten, Universums-Routings, gepatchte Fixtures mitsamt Koordinaten, Gerätegruppen, Lichtszenen sowie Chaser.
 *
 * @param version     Formatversion der Projektdatei (z. B. "1.0")
 * @param projectName Name des Projekts
 * @param savedAt     Zeitstempel der letzten Speicherung im ISO-8601-Format
 * @param universes   Liste der konfigurierten DMX-Universen
 * @param fixtures    Liste der gepatchten Fixtures mit Positionen
 * @param groups      Liste der konfigurierten Gerätegruppen
 * @param scenes      Liste der gespeicherten Lichtszenen (Cues)
 * @param chasers     Liste der konfigurierten Chaser (Sequenzen)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProjectData(
        String version,
        String projectName,
        String savedAt,
        List<UniverseDataDto> universes,
        List<FixtureDataDto> fixtures,
        List<GroupDataDto> groups,
        List<SceneDataDto> scenes,
        List<ChaserDataDto> chasers
) {
    /**
     * Aktuelle Formatversion für neue Projektdateien.
     */
    public static final String CURRENT_VERSION = "1.0";

    /**
     * Überladener Konstruktor für Abwärtskompatibilität (ohne explizite Szenen und Chaser).
     */
    public ProjectData(String version, String projectName, String savedAt, List<UniverseDataDto> universes, List<FixtureDataDto> fixtures, List<GroupDataDto> groups) {
        this(version, projectName, savedAt, universes, fixtures, groups, List.of(), List.of());
    }

    /**
     * Überladener Konstruktor für Abwärtskompatibilität (ohne explizite Gruppen, Szenen und Chaser).
     */
    public ProjectData(String version, String projectName, String savedAt, List<UniverseDataDto> universes, List<FixtureDataDto> fixtures) {
        this(version, projectName, savedAt, universes, fixtures, List.of(), List.of(), List.of());
    }

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
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}

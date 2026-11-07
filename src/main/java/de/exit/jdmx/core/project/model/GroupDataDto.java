package de.exit.jdmx.core.project.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * DTO zur persistenten Speicherung von Gerätegruppen in Projektdateien (*.jdmx).
 *
 * @param id         Eindeutige Gruppen-ID
 * @param name       Anzeigename der Gruppe
 * @param fixtureIds Liste der IDs zugehöriger Scheinwerfer
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GroupDataDto(
        String id,
        String name,
        List<String> fixtureIds
) {
}

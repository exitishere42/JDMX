package de.exit.jdmx.core.project.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO zur persistenten Speicherung gepatchter Scheinwerfer und ihrer Bühnenkoordinaten.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FixtureDataDto(
        String id,
        String name,
        String definitionId,
        String manufacturer,
        String model,
        String modeName,
        int universe,
        int startAddress,
        double stageX,
        double stageY
) {
}

package de.exit.jdmx.core.project.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO zur persistenten Speicherung eines einzelnen Chaser-Schritts.
 *
 * @param stepId     Eindeutige Schritt-ID
 * @param sceneId    ID der aufzurufenden Szene
 * @param holdTimeMs Haltezeit in Millisekunden
 * @param fadeTimeMs Überblendzeit in Millisekunden
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChaserStepDto(
        String stepId,
        String sceneId,
        long holdTimeMs,
        long fadeTimeMs
) {
}

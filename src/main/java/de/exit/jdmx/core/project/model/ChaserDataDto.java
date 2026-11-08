package de.exit.jdmx.core.project.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * DTO zur persistenten Speicherung von Chasern (Lauflicht-Sequenzen) in Projektdateien (*.jdmx).
 *
 * @param id        Eindeutige Chaser-ID
 * @param name      Anzeigename des Chasers
 * @param bpm       Geschwindigkeit in Beats per Minute
 * @param direction Laufrichtung (FORWARD, BACKWARD, PING_PONG, RANDOM)
 * @param steps     Liste der Chaser-Schritte
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChaserDataDto(
        String id,
        String name,
        double bpm,
        String direction,
        List<ChaserStepDto> steps
) {
}

package de.exit.jdmx.core.project.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * DTO zur persistenten Speicherung von Lichtszenen (Cues) in Projektdateien (*.jdmx).
 *
 * @param id            Eindeutige Szenen-ID
 * @param name          Anzeigename der Szene
 * @param fadeTimeMs    Überblendzeit in Millisekunden
 * @param channelValues Abgespeicherte DMX-Werte pro Universum und Kanal
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SceneDataDto(
        String id,
        String name,
        long fadeTimeMs,
        Map<Integer, Map<Integer, Integer>> channelValues
) {
}

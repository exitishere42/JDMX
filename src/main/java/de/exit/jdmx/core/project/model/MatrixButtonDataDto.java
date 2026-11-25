package de.exit.jdmx.core.project.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO zur Persistierung eines Buttons der Button Matrix in der JDMX-Projektdatei (*.jdmx).
 *
 * @param index       der lineare Button-Index im Grid
 * @param row         die Zeile
 * @param col         die Spalte
 * @param name        die Beschriftung des Buttons
 * @param color       der Farbcode
 * @param targetType  der Zieltyp (z. B. "SCENE", "CHASER", "NONE")
 * @param targetId    die ID der zugewiesenen Szene oder des Chasers
 * @param triggerMode der Trigger-Modus (z. B. "TOGGLE", "FLASH")
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MatrixButtonDataDto(
        int index,
        int row,
        int col,
        String name,
        String color,
        String targetType,
        String targetId,
        String triggerMode
) {
}

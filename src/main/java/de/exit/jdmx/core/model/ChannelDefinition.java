package de.exit.jdmx.core.model;

import java.util.Objects;

/**
 * Definition eines einzelnen DMX-Kanals innerhalb eines Scheinwerfer-Modus.
 *
 * @param index        1-basierter Kanalindex innerhalb des Geräts (1, 2, ...)
 * @param name         Bezeichnung des Kanals (z. B. "Dimmer", "Pan", "Red")
 * @param type         Der funktionale {@link ChannelType}
 * @param defaultValue Standardwert beim Einschalten (0–255)
 * @param isFine       True, falls es sich um das 16-Bit Fine-Byte handelt
 */
public record ChannelDefinition(
        int index,
        String name,
        ChannelType type,
        int defaultValue,
        boolean isFine
) {
    public ChannelDefinition {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");
        if (defaultValue < 0 || defaultValue > 255) {
            throw new IllegalArgumentException("defaultValue must be between 0 and 255");
        }
    }
}

package de.exit.jdmx.core.model;

/**
 * Merging-Modus für eingehende DMX- und Controller-Signale.
 * <p>
 * Definiert, wie Eingangsdaten mit dem aktuellen Universums-Puffer zusammengeführt werden:
 * <ul>
 *   <li>{@link #HTP}: Highest Takes Precedence – Der höhere DMX-Wert gewinnt (Standard für Dimmer).</li>
 *   <li>{@link #LTP}: Latest Takes Precedence – Die zuletzt empfangene Änderung überschreibt den Wert.</li>
 *   <li>{@link #OVERRIDE}: Das Eingangssignal überschreibt den Puffer vollständig.</li>
 * </ul>
 * </p>
 */
public enum MergeMode {
    HTP("HTP"),
    LTP("LTP"),
    OVERRIDE("Override");

    private final String displayName;

    MergeMode(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Liefert die saubere Fachbezeichnung für die Benutzeroberfläche.
     *
     * @return Fachbezeichnung ohne Klammern oder Erklärungen
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Parst einen String tolerant in einen {@link MergeMode}.
     * Fällt bei ungültigen oder null-Werten sicher auf {@link #HTP} zurück.
     *
     * @param value Textwert
     * @return Entsprechender MergeMode
     */
    public static MergeMode fromString(String value) {
        if (value == null || value.isBlank()) {
            return HTP;
        }
        for (MergeMode mode : values()) {
            if (mode.name().equalsIgnoreCase(value.trim()) || mode.displayName.equalsIgnoreCase(value.trim())) {
                return mode;
            }
        }
        return HTP;
    }
}

package de.exit.jdmx.core.model;

/**
 * Typen von Elementen im Project Explorer.
 */
public enum ExplorerItemType {
    /** Ein aufklappbarer Standard- oder Benutzer-Ordner. */
    FOLDER,

    /** Ein DMX-Scheinwerfer (Fixture). */
    FIXTURE,

    /** Eine logische Gerätegruppe. */
    GROUP,

    /** Ein gespeichertes Beleuchtungs-Preset (z. B. Farb-/Positionswert). */
    PRESET,

    /** Eine Szenenliste (Cuelist) für Show-Abläufe. */
    CUELIST,

    /** Ein dynamischer Chaser- oder Wellen-Effekt. */
    EFFECT
}

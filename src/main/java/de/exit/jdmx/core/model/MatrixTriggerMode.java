package de.exit.jdmx.core.model;

/**
 * Definiert den Auslöse- bzw. Trigger-Modus eines Buttons in der Button Matrix.
 */
public enum MatrixTriggerMode {

    /**
     * Umschalter: Ein Klick schaltet den Button an (Szene/Chaser aktiv),
     * ein erneuter Klick schaltet ihn wieder aus.
     */
    TOGGLE,

    /**
     * Taster: Der Button ist nur aktiv, solange die Maustaste (oder das MIDI-Pad)
     * gedrückt gehalten wird. Beim Loslassen stoppt die Wiedergabe sofort.
     */
    FLASH
}

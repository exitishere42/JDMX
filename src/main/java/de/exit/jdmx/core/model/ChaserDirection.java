package de.exit.jdmx.core.model;

/**
 * Laufrichtung eines DMX-Chasers (Lauflicht / Cue-Sequenz).
 */
public enum ChaserDirection {
    /** 1 -> 2 -> 3 -> 1 ... */
    FORWARD,
    /** 3 -> 2 -> 1 -> 3 ... */
    BACKWARD,
    /** 1 -> 2 -> 3 -> 2 -> 1 ... */
    PING_PONG,
    /** Zufällige Reihenfolge */
    RANDOM
}

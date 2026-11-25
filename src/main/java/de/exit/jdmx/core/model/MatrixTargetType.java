package de.exit.jdmx.core.model;

/**
 * Definiert den Ziel-Typ eines Buttons in der Button Matrix.
 */
public enum MatrixTargetType {

    /**
     * Kein Ziel zugewiesen (leerer Button).
     */
    NONE,

    /**
     * Der Button steuert eine statische Lichtszene ({@link SceneDefinition}).
     */
    SCENE,

    /**
     * Der Button steuert einen dynamischen Chaser / ein Lauflicht ({@link ChaserDefinition}).
     */
    CHASER
}

package de.exit.jdmx.core.model;

import java.util.Objects;

/**
 * Repräsentiert die unveränderliche Definition eines einzelnen Buttons in der Button Matrix.
 *
 * @param index       der fortlaufende Index des Buttons im Raster (0-basiert)
 * @param row         die Zeilenposition im Raster (0-basiert)
 * @param col         die Spaltenposition im Raster (0-basiert)
 * @param name        die Beschriftung des Buttons (darf nicht {@code null} sein)
 * @param color       der Hex-Farbcode des Buttons (z. B. {@code "#03DAC6"}, darf nicht {@code null} sein)
 * @param targetType  der zugewiesene Ziel-Typ (darf nicht {@code null} sein)
 * @param targetId    die ID der zugewiesenen Szene oder des Chasers (kann {@code null} sein)
 * @param triggerMode der Auslösemodus ({@link MatrixTriggerMode#TOGGLE} oder {@link MatrixTriggerMode#FLASH})
 */
public record MatrixButtonDefinition(
        int index,
        int row,
        int col,
        String name,
        String color,
        MatrixTargetType targetType,
        String targetId,
        MatrixTriggerMode triggerMode
) {

    /** Standard-Farbe für leere oder unzugewiesene Buttons. */
    public static final String DEFAULT_COLOR = "#2A2A2A";

    /**
     * Kompakter Konstruktor mit Validierungs- und Nicht-Null-Prüfungen.
     */
    public MatrixButtonDefinition {
        if (index < 0) {
            throw new IllegalArgumentException("Index darf nicht negativ sein: " + index);
        }
        if (row < 0) {
            throw new IllegalArgumentException("Row darf nicht negativ sein: " + row);
        }
        if (col < 0) {
            throw new IllegalArgumentException("Col darf nicht negativ sein: " + col);
        }
        Objects.requireNonNull(name, "name darf nicht null sein");
        Objects.requireNonNull(color, "color darf nicht null sein");
        Objects.requireNonNull(targetType, "targetType darf nicht null sein");
        Objects.requireNonNull(triggerMode, "triggerMode darf nicht null sein");
    }

    /**
     * Erstellt einen neuen, unzugewiesenen Button für die gegebene Rasterposition.
     *
     * @param index der fortlaufende Index
     * @param row   die Zeile
     * @param col   die Spalte
     * @return eine leere {@link MatrixButtonDefinition}
     */
    public static MatrixButtonDefinition createEmpty(int index, int row, int col) {
        return new MatrixButtonDefinition(
                index,
                row,
                col,
                "",
                DEFAULT_COLOR,
                MatrixTargetType.NONE,
                null,
                MatrixTriggerMode.TOGGLE
        );
    }

    /**
     * Gibt an, ob diesem Button ein Ziel (Szene oder Chaser) zugewiesen ist.
     *
     * @return {@code true}, wenn ein Ziel zugewiesen ist, sonst {@code false}
     */
    public boolean isAssigned() {
        return targetType != MatrixTargetType.NONE && targetId != null && !targetId.isBlank();
    }

    /**
     * Erstellt eine Kopie dieses Buttons mit neuer Beschriftung.
     *
     * @param newName der neue Name
     * @return aktualisierte Button-Definition
     */
    public MatrixButtonDefinition withName(String newName) {
        return new MatrixButtonDefinition(index, row, col, newName, color, targetType, targetId, triggerMode);
    }

    /**
     * Erstellt eine Kopie dieses Buttons mit neuer Farbe.
     *
     * @param newColor die neue Hex-Farbe
     * @return aktualisierte Button-Definition
     */
    public MatrixButtonDefinition withColor(String newColor) {
        return new MatrixButtonDefinition(index, row, col, name, newColor, targetType, targetId, triggerMode);
    }

    /**
     * Erstellt eine Kopie dieses Buttons mit neuem Ziel.
     *
     * @param newType der neue Ziel-Typ
     * @param newTargetId die neue Ziel-ID
     * @param newName die optionale neue Beschriftung (wenn leer, wird der bisherige Name beibehalten)
     * @return aktualisierte Button-Definition
     */
    public MatrixButtonDefinition withTarget(MatrixTargetType newType, String newTargetId, String newName) {
        String finalName = (newName != null && !newName.isBlank()) ? newName : this.name;
        return new MatrixButtonDefinition(index, row, col, finalName, color, newType, newTargetId, triggerMode);
    }

    /**
     * Erstellt eine Kopie dieses Buttons mit neuem Trigger-Modus.
     *
     * @param newMode der neue {@link MatrixTriggerMode}
     * @return aktualisierte Button-Definition
     */
    public MatrixButtonDefinition withTriggerMode(MatrixTriggerMode newMode) {
        return new MatrixButtonDefinition(index, row, col, name, color, targetType, targetId, newMode);
    }

    /**
     * Erstellt eine Kopie dieses Buttons im unzugewiesenen Zustand.
     *
     * @return geleerte Button-Definition
     */
    public MatrixButtonDefinition withCleared() {
        return createEmpty(index, row, col);
    }
}

package de.exit.jdmx.core.model;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verwaltet das logische Raster der Button Matrix mit konfigurierbaren Zeilen und Spalten.
 * <p>
 * Standardmäßig wird ein 4×8-Raster (32 Buttons) erzeugt, welches sich dynamisch anpassen lässt.
 * </p>
 */
public class MatrixGridDefinition {

    /** Standard-Zeilenanzahl der Button Matrix. */
    public static final int DEFAULT_ROWS = 4;

    /** Standard-Spaltenanzahl der Button Matrix. */
    public static final int DEFAULT_COLS = 8;

    private final int rows;
    private final int cols;
    private final Map<Integer, MatrixButtonDefinition> buttons = new ConcurrentHashMap<>();

    /**
     * Erstellt ein neues Grid mit Standard-Abmessungen (4 Zeilen × 8 Spalten = 32 Buttons).
     */
    public MatrixGridDefinition() {
        this(DEFAULT_ROWS, DEFAULT_COLS);
    }

    /**
     * Erstellt ein neues Grid mit benutzerdefinierten Abmessungen.
     *
     * @param rows Anzahl der Zeilen (muss &gt; 0 sein)
     * @param cols Anzahl der Spalten (muss &gt; 0 sein)
     * @throws IllegalArgumentException wenn rows oder cols kleiner oder gleich 0 sind
     */
    public MatrixGridDefinition(int rows, int cols) {
        if (rows <= 0) {
            throw new IllegalArgumentException("Zeilenanzahl muss positiv sein: " + rows);
        }
        if (cols <= 0) {
            throw new IllegalArgumentException("Spaltenanzahl muss positiv sein: " + cols);
        }
        this.rows = rows;
        this.cols = cols;

        initializeButtons();
    }

    private void initializeButtons() {
        buttons.clear();
        int total = rows * cols;
        for (int i = 0; i < total; i++) {
            int r = i / cols;
            int c = i % cols;
            buttons.put(i, MatrixButtonDefinition.createEmpty(i, r, c));
        }
    }

    /**
     * Gibt die Anzahl der Zeilen zurück.
     *
     * @return Zeilenanzahl
     */
    public int getRows() {
        return rows;
    }

    /**
     * Gibt die Anzahl der Spalten zurück.
     *
     * @return Spaltenanzahl
     */
    public int getCols() {
        return cols;
    }

    /**
     * Gibt die Gesamtanzahl aller Buttons im Raster zurück.
     *
     * @return Gesamtanzahl (rows * cols)
     */
    public int getTotalButtons() {
        return rows * cols;
    }

    /**
     * Gibt eine unmodifizierbare Ansicht aller Buttons im Raster zurück.
     *
     * @return Map aller Buttons nach Index
     */
    public Map<Integer, MatrixButtonDefinition> getButtons() {
        return Collections.unmodifiableMap(buttons);
    }

    /**
     * Gibt den Button am angegebenen linearen Index zurück.
     *
     * @param index der Button-Index (0 bis total - 1)
     * @return die {@link MatrixButtonDefinition} oder {@code null}, wenn Index außerhalb des Rasters liegt
     */
    public MatrixButtonDefinition getButton(int index) {
        return buttons.get(index);
    }

    /**
     * Gibt den Button an der angegebenen Zeilen- und Spaltenposition zurück.
     *
     * @param row die Zeile (0 bis rows - 1)
     * @param col die Spalte (0 bis cols - 1)
     * @return die {@link MatrixButtonDefinition} oder {@code null}, wenn Position außerhalb des Rasters liegt
     */
    public MatrixButtonDefinition getButton(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            return null;
        }
        int index = row * cols + col;
        return buttons.get(index);
    }

    /**
     * Weist dem Raster eine aktualisierte Button-Definition zu.
     *
     * @param button die neue Button-Definition
     * @throws IllegalArgumentException wenn der Index außerhalb des Rasters liegt
     */
    public void setButton(MatrixButtonDefinition button) {
        if (button == null) {
            return;
        }
        int index = button.index();
        if (index < 0 || index >= getTotalButtons()) {
            throw new IllegalArgumentException("Button-Index " + index + " liegt außerhalb des Rasters (0-" + (getTotalButtons() - 1) + ")");
        }
        buttons.put(index, button);
    }

    /**
     * Setzt den Button am angegebenen Index auf den unzugewiesenen Ausgangszustand zurück.
     *
     * @param index der Index des zu leerenden Buttons
     */
    public void clearButton(int index) {
        MatrixButtonDefinition existing = buttons.get(index);
        if (existing != null) {
            buttons.put(index, existing.withCleared());
        }
    }

    /**
     * Leert alle Buttons im gesamten Raster.
     */
    public void clearAll() {
        initializeButtons();
    }
}

package de.exit.jdmx.ui.io;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Performante visuelle Kachel (Viereck) für einen einzelnen DMX-Kanal (1–512) in der DMX-Matrix.
 * <p>
 * Performance-Garantie:
 * Inaktive Kanäle (Wert 0) bleiben im Standard-Grau und lösen KEINE Repaints, CSS-Neuberechnungen
 * oder SceneGraph-Mutationen aus. Nur Kanäle, die tatsächlich aktiv (> 0) sind oder ihren Wert
 * verändern, werden dynamisch gerendert.
 * </p>
 */
public class DmxChannelCell extends VBox {

    /** Feste Kachelbreite in Pixeln. */
    public static final double CELL_WIDTH = 50.0;
    /** Feste Kachelhöhe in Pixeln. */
    public static final double CELL_HEIGHT = 44.0;

    private final int channelNumber;
    private final Label channelLabel;
    private final Label valueLabel;

    private int value = 0;

    /**
     * Erstellt eine neue DMX-Kanal-Kachel.
     *
     * @param channelNumber DMX-Kanalnummer (1–512)
     */
    public DmxChannelCell(int channelNumber) {
        this.channelNumber = channelNumber;

        getStyleClass().add("dmx-channel-cell");
        setAlignment(Pos.CENTER);
        setSpacing(2.0);
        setMinSize(CELL_WIDTH, CELL_HEIGHT);
        setPrefSize(CELL_WIDTH, CELL_HEIGHT);
        setMaxSize(CELL_WIDTH, CELL_HEIGHT);

        this.channelLabel = new Label(String.valueOf(channelNumber));
        channelLabel.getStyleClass().add("dmx-channel-number");

        this.valueLabel = new Label("0");
        valueLabel.getStyleClass().add("dmx-channel-value");

        getChildren().addAll(channelLabel, valueLabel);
        // Ruhezustand: Standard-Grau via CSS-Klasse ohne Inline-Styles
    }

    /**
     * Setzt den DMX-Wert des Kanals (0–255).
     * <p>
     * Führt eine strikte Dirty-Prüfung durch: Wenn der Wert identisch ist,
     * wird die Methode sofort verlassen (kein UI-Overhead).
     * </p>
     *
     * @param newValue Neuer Auslastungswert (0..255)
     */
    public void setValue(int newValue) {
        int clamped = Math.max(0, Math.min(255, newValue));
        if (this.value == clamped) {
            return; // Nichts zu tun - verhindert jeglichen Lag bei 40 FPS
        }

        int oldValue = this.value;
        this.value = clamped;

        if (clamped > 0) {
            // Kanal ist aktiv: Wert anzeigen und dynamisch grün aufleuchten lassen
            valueLabel.setText(String.valueOf(clamped));
            renderActiveGlow(clamped);
        } else if (oldValue > 0) {
            // Kanal war vorher aktiv und ist jetzt 0: Einmalig zurück auf Standard-Grau
            valueLabel.setText("0");
            resetToDefaultGray();
        }
    }

    /**
     * Rendert das dynamische grüne Aufleuchten proportional zur DMX-Auslastung.
     */
    private void renderActiveGlow(int val) {
        if (!getStyleClass().contains("dmx-channel-cell-active")) {
            getStyleClass().add("dmx-channel-cell-active");
        }
        double ratio = val / 255.0;
        double alpha = 0.20 + (0.75 * ratio);
        double borderAlpha = Math.min(1.0, 0.45 + (0.55 * ratio));

        setStyle(String.format(
                "-fx-background-color: rgba(16, 185, 129, %.2f); -fx-border-color: rgba(52, 211, 153, %.2f);",
                alpha, borderAlpha
        ));
        valueLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold;");
        channelLabel.setStyle("-fx-text-fill: " + (ratio > 0.5 ? "#064E3B;" : "#A7F3D0;"));
    }

    /**
     * Setzt die Kachel wieder auf das unveränderliche Standard-Grau zurück.
     */
    private void resetToDefaultGray() {
        getStyleClass().remove("dmx-channel-cell-active");
        setStyle(null); // Entfernt Inline-Styles, Standard-CSS-Grau greift sofort
        valueLabel.setStyle(null);
        channelLabel.setStyle(null);
    }

    public int getChannelNumber() {
        return channelNumber;
    }

    public int getValue() {
        return value;
    }

    public boolean isActive() {
        return value > 0;
    }

    public Label getChannelLabel() {
        return channelLabel;
    }

    public Label getValueLabel() {
        return valueLabel;
    }
}

package de.exit.jdmx.ui.io;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Visuelle Kachel (Viereck) für einen einzelnen DMX-Kanal (1–512) in der DMX-Matrix.
 * <p>
 * Zeigt oben die Kanalnummer und unten die Auslastung (Wert von 0 bis 255).
 * Leuchtet stufenlos grün auf, abhängig von der aktuellen Füllung des DMX-Kanals.
 * </p>
 */
public class DmxChannelCell extends VBox {

    /** Feste Kachelgröße in Pixeln. */
    public static final double CELL_SIZE = 36.0;

    private final int channelNumber;
    private final Label channelLabel;
    private final Label valueLabel;
    private final Tooltip tooltip;

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
        setSpacing(1.0);
        setMinSize(CELL_SIZE, CELL_SIZE);
        setPrefSize(CELL_SIZE, CELL_SIZE);
        setMaxSize(CELL_SIZE, CELL_SIZE);

        this.channelLabel = new Label(String.valueOf(channelNumber));
        channelLabel.getStyleClass().add("dmx-channel-number");

        this.valueLabel = new Label("0");
        valueLabel.getStyleClass().add("dmx-channel-value");

        this.tooltip = new Tooltip("Kanal " + channelNumber + ": 0");
        tooltip.setShowDelay(Duration.millis(150));
        Tooltip.install(this, tooltip);

        getChildren().addAll(channelLabel, valueLabel);
        updateVisuals();
    }

    /**
     * Setzt den DMX-Wert des Kanals (0–255).
     *
     * @param value Auslastungswert (wird auf 0..255 begrenzt)
     */
    public void setValue(int value) {
        int clamped = Math.max(0, Math.min(255, value));
        if (this.value != clamped) {
            this.value = clamped;
            valueLabel.setText(String.valueOf(clamped));
            tooltip.setText("Kanal " + channelNumber + ": " + clamped);
            updateVisuals();
        }
    }

    /**
     * Aktualisiert das dynamische grüne Aufleuchten anhand des Kanalwerts.
     */
    private void updateVisuals() {
        if (value > 0) {
            if (!getStyleClass().contains("dmx-channel-cell-active")) {
                getStyleClass().add("dmx-channel-cell-active");
            }
            // Dynamische Grün-Deckkraft proportional zum DMX-Wert (16..255)
            double ratio = value / 255.0;
            double alpha = 0.20 + (0.75 * ratio);
            String greenRgba = String.format("rgba(16, 185, 129, %.2f)", alpha);
            String borderRgba = String.format("rgba(52, 211, 153, %.2f)", Math.min(1.0, 0.40 + (0.60 * ratio)));

            setStyle(String.format(
                    "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: 1px; -fx-border-radius: 3px; -fx-background-radius: 3px;",
                    greenRgba, borderRgba
            ));
            valueLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold;");
            channelLabel.setStyle("-fx-text-fill: " + (ratio > 0.5 ? "#064E3B;" : "#A7F3D0;"));
        } else {
            getStyleClass().remove("dmx-channel-cell-active");
            setStyle("-fx-background-color: #181818; -fx-border-color: #282828; -fx-border-width: 1px; -fx-border-radius: 3px; -fx-background-radius: 3px;");
            valueLabel.setStyle("-fx-text-fill: #52525B; -fx-font-weight: normal;");
            channelLabel.setStyle("-fx-text-fill: #52525B;");
        }
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

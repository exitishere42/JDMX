package de.exit.jdmx.ui.io;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Steuerungs-Dock für Eingänge (MIDI & Art-Net In) im Tab "I/O Patch".
 * <p>
 * Baut exakt auf dem Design-Standard der Steuerungs-Docks aus Stage & Fixtures auf:
 * Scharfkantige Karte ({@code control-module-card}) mit konsistentem Header und flexibler
 * Verschiebbarkeit im SplitPane.
 * </p>
 */
public class InputDockView extends VBox {

    private final String title = "Eingang (Input)";
    private final ComboBox<String> midiDeviceCombo;
    private final ComboBox<String> artNetInCombo;
    private final Label lblSignal;
    private final Label badge;

    public InputDockView() {
        super(4);
        getStyleClass().add("control-module-card");
        setPadding(new Insets(6.0, 10.0, 8.0, 10.0));
        setMinHeight(110.0);

        // 1. Schlichter Header
        HBox header = new HBox(6.0);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("module-card-header");

        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("module-card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        this.badge = new Label("Inaktiv");
        badge.setStyle("-fx-background-color: #242424; -fx-text-fill: #71717A; -fx-font-size: 10px; -fx-padding: 2 6; -fx-border-color: #333333; -fx-border-radius: 3px; -fx-background-radius: 3px;");

        header.getChildren().addAll(lblTitle, spacer, badge);

        // 2. Formular-Körper
        VBox body = new VBox(8.0);
        body.setAlignment(Pos.CENTER_LEFT);
        body.setPadding(new Insets(6.0, 2.0, 4.0, 2.0));
        VBox.setVgrow(body, Priority.ALWAYS);

        // Zeile 1: MIDI-Geräte
        HBox rowMidi = new HBox(8.0);
        rowMidi.setAlignment(Pos.CENTER_LEFT);
        Label lblMidi = new Label("MIDI-Gerät:");
        lblMidi.setStyle("-fx-font-size: 11px; -fx-text-fill: #A1A1AA; -fx-min-width: 85px;");

        this.midiDeviceCombo = new ComboBox<>();
        midiDeviceCombo.getStyleClass().add("routing-combo");
        midiDeviceCombo.getItems().addAll("Kein MIDI-Gerät verbunden", "Korg nanoKONTROL2", "Akai APC Mini");
        midiDeviceCombo.setValue("Kein MIDI-Gerät verbunden");
        midiDeviceCombo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(midiDeviceCombo, Priority.ALWAYS);

        rowMidi.getChildren().addAll(lblMidi, midiDeviceCombo);

        // Zeile 2: Art-Net In
        HBox rowArtNet = new HBox(8.0);
        rowArtNet.setAlignment(Pos.CENTER_LEFT);
        Label lblArt = new Label("Art-Net In:");
        lblArt.setStyle("-fx-font-size: 11px; -fx-text-fill: #A1A1AA; -fx-min-width: 85px;");

        this.artNetInCombo = new ComboBox<>();
        artNetInCombo.getStyleClass().add("routing-combo");
        artNetInCombo.getItems().addAll("Inaktiv", "Port 6454 (Alle Interfaces)", "127.0.0.1 (Loopback)");
        artNetInCombo.setValue("Inaktiv");
        artNetInCombo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(artNetInCombo, Priority.ALWAYS);

        rowArtNet.getChildren().addAll(lblArt, artNetInCombo);

        // Zeile 3: Signal-Pegel / Info
        HBox rowSignal = new HBox(8.0);
        rowSignal.setAlignment(Pos.CENTER_LEFT);
        Label lblSigTitle = new Label("Signal-Pegel:");
        lblSigTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #A1A1AA; -fx-min-width: 85px;");

        this.lblSignal = new Label("Wartet auf Eingangsdaten...");
        lblSignal.setStyle("-fx-font-size: 11px; -fx-text-fill: #71717A;");

        rowSignal.getChildren().addAll(lblSigTitle, lblSignal);

        body.getChildren().addAll(rowMidi, rowArtNet, rowSignal);

        getChildren().addAll(header, body);
    }

    public String getTitle() {
        return title;
    }

    public ComboBox<String> getMidiDeviceCombo() {
        return midiDeviceCombo;
    }

    public ComboBox<String> getArtNetInCombo() {
        return artNetInCombo;
    }

    public Label getLblSignal() {
        return lblSignal;
    }

    public Label getBadge() {
        return badge;
    }
}

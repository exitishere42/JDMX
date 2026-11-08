package de.exit.jdmx.ui.io;

import de.exit.jdmx.core.i18n.I18n;
import javafx.application.Platform;
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

    private final Label lblTitle;
    private final ComboBox<String> midiDeviceCombo;
    private final ComboBox<String> artNetInCombo;
    private final Label lblMidi;
    private final Label lblArt;
    private final Label lblSigTitle;
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

        this.lblTitle = new Label(I18n.get("io.dock.input"));
        lblTitle.getStyleClass().add("module-card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        this.badge = new Label(I18n.get("io.status.inactive"));
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
        this.lblMidi = new Label(I18n.get("io.midi_device"));
        lblMidi.setStyle("-fx-font-size: 11px; -fx-text-fill: #A1A1AA; -fx-min-width: 85px;");

        this.midiDeviceCombo = new ComboBox<>();
        midiDeviceCombo.getStyleClass().add("routing-combo");
        midiDeviceCombo.getItems().addAll(I18n.get("io.no_midi"), "Korg nanoKONTROL2", "Akai APC Mini");
        midiDeviceCombo.setValue(I18n.get("io.no_midi"));
        midiDeviceCombo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(midiDeviceCombo, Priority.ALWAYS);

        rowMidi.getChildren().addAll(lblMidi, midiDeviceCombo);

        // Zeile 2: Art-Net In
        HBox rowArtNet = new HBox(8.0);
        rowArtNet.setAlignment(Pos.CENTER_LEFT);
        this.lblArt = new Label(I18n.get("io.artnet_in"));
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
        this.lblSigTitle = new Label(I18n.get("io.signal_level"));
        lblSigTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #A1A1AA; -fx-min-width: 85px;");

        this.lblSignal = new Label(I18n.get("io.waiting_data"));
        lblSignal.setStyle("-fx-font-size: 11px; -fx-text-fill: #71717A;");

        rowSignal.getChildren().addAll(lblSigTitle, lblSignal);

        body.getChildren().addAll(rowMidi, rowArtNet, rowSignal);

        getChildren().addAll(header, body);

        // Dynamischer Sprachwechsel
        I18n.addLanguageChangeListener(lang -> {
            if (Platform.isFxApplicationThread()) {
                updateLocalization();
            } else {
                Platform.runLater(this::updateLocalization);
            }
        });
    }

    public String getTitle() {
        return I18n.get("io.dock.input");
    }

    /**
     * Aktualisiert alle Beschriftungen bei einem Sprachwechsel.
     */
    public void updateLocalization() {
        lblTitle.setText(I18n.get("io.dock.input"));
        lblMidi.setText(I18n.get("io.midi_device"));
        lblArt.setText(I18n.get("io.artnet_in"));
        lblSigTitle.setText(I18n.get("io.signal_level"));
        badge.setText(I18n.get("io.status.inactive"));
        lblSignal.setText(I18n.get("io.waiting_data"));
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

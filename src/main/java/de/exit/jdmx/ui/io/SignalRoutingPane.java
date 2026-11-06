package de.exit.jdmx.ui.io;

import de.exit.jdmx.ui.icon.LucideIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Horizontale Signalfluss-Leiste in der oberen Bildschirmhälfte von Tab 5:
 * <pre>
 * [ INPUT ] ════► [ JDMX PROZESSOR ] ════► [ OUTPUT ]
 * </pre>
 * <p>
 * Links: Konfiguration der Eingänge (MIDI-Geräte).<br>
 * Mitte: Status-Zentrale der 40 FPS Engine.<br>
 * Rechts: DMX-Ausgänge (einfaches, klares Art-Net Setup wie in sound2artnet mit Ziel-IP und Universum).
 * </p>
 */
public class SignalRoutingPane extends HBox {

    /** Feste Höhe der kompakten Signal-Routing-Leiste. */
    public static final double PANE_HEIGHT = 100.0;

    private final VBox inputBox;
    private final VBox jdmxBox;
    private final VBox outputBox;

    private final Node arrowInputToJdmx;
    private final Node arrowJdmxToOutput;

    // Input Bedienelemente
    private final ComboBox<String> midiDeviceCombo;

    // Art-Net Bedienelemente (Output)
    private final CheckBox artNetCheckbox;
    private final TextField targetIpField;
    private final Spinner<Integer> universeSpinner;

    public SignalRoutingPane() {
        getStyleClass().add("signal-routing-pane");
        setAlignment(Pos.CENTER);
        setSpacing(14.0);
        setPadding(new Insets(8.0, 10.0, 8.0, 10.0));
        setMinHeight(PANE_HEIGHT);
        setPrefHeight(PANE_HEIGHT);
        setMaxHeight(PANE_HEIGHT);

        // 1. Controls initialisieren
        this.midiDeviceCombo = new ComboBox<>();
        midiDeviceCombo.getStyleClass().add("routing-combo");
        midiDeviceCombo.getItems().add("Kein Gerät verbunden");
        midiDeviceCombo.setValue("Kein Gerät verbunden");
        midiDeviceCombo.setMaxWidth(Double.MAX_VALUE);

        this.artNetCheckbox = new CheckBox("Art-Net");
        artNetCheckbox.getStyleClass().add("routing-checkbox");
        artNetCheckbox.setSelected(true);

        this.targetIpField = new TextField("127.0.0.1");
        targetIpField.getStyleClass().add("routing-text-field");
        targetIpField.setPromptText("127.0.0.1");
        targetIpField.setPrefWidth(120.0);
        targetIpField.setMaxWidth(160.0);

        this.universeSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 15, 0));
        universeSpinner.getStyleClass().add("routing-spinner");
        universeSpinner.setPrefWidth(60.0);

        // 2. Box Links: INPUT
        this.inputBox = buildInputBox();
        HBox.setHgrow(inputBox, Priority.ALWAYS);

        // Pfeil 1: INPUT -> JDMX
        this.arrowInputToJdmx = buildFlowArrow("MIDI", Color.web("#71717A"));

        // 3. Box Mitte: JDMX CORE
        this.jdmxBox = buildJdmxBox();
        HBox.setHgrow(jdmxBox, Priority.ALWAYS);

        // Pfeil 2: JDMX -> OUTPUT
        this.arrowJdmxToOutput = buildFlowArrow("DMX", Color.web("#10B981"));

        // 4. Box Rechts: OUTPUT
        this.outputBox = buildOutputBox();
        HBox.setHgrow(outputBox, Priority.ALWAYS);

        getChildren().addAll(inputBox, arrowInputToJdmx, jdmxBox, arrowJdmxToOutput, outputBox);
    }

    private VBox buildInputBox() {
        VBox box = new VBox(6.0);
        box.getStyleClass().add("routing-box");
        box.setPadding(new Insets(8.0, 12.0, 8.0, 12.0));

        // Header
        HBox header = new HBox(6.0);
        header.setAlignment(Pos.CENTER_LEFT);
        LucideIcon icon = new LucideIcon("radio", 14.0, Color.web("#A1A1AA"));
        Label lblTitle = new Label("INPUT");
        lblTitle.getStyleClass().add("routing-box-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label badge = new Label("Inaktiv");
        badge.getStyleClass().add("routing-badge-muted");
        header.getChildren().addAll(icon, lblTitle, spacer, badge);

        // Zeile 1: Device Dropdown
        HBox row1 = new HBox(6.0);
        row1.setAlignment(Pos.CENTER_LEFT);
        Label lblMidi = new Label("MIDI:");
        lblMidi.getStyleClass().add("routing-field-label");
        HBox.setHgrow(midiDeviceCombo, Priority.ALWAYS);
        row1.getChildren().addAll(lblMidi, midiDeviceCombo);

        // Zeile 2: Footer-Info
        HBox row2 = new HBox(8.0);
        row2.setAlignment(Pos.CENTER_LEFT);
        Label lblSignal = new Label("Letztes Signal: --");
        lblSignal.getStyleClass().add("routing-subtext");
        row2.getChildren().add(lblSignal);

        box.getChildren().addAll(header, row1, row2);
        return box;
    }

    private VBox buildJdmxBox() {
        VBox box = new VBox(6.0);
        box.getStyleClass().addAll("routing-box", "routing-box-core");
        box.setPadding(new Insets(8.0, 12.0, 8.0, 12.0));

        // Header
        HBox header = new HBox(6.0);
        header.setAlignment(Pos.CENTER_LEFT);
        LucideIcon icon = new LucideIcon("cpu", 14.0, Color.web("#38BDF8"));
        Label lblTitle = new Label("JDMX PROZESSOR");
        lblTitle.getStyleClass().add("routing-box-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label badge = new Label("Aktiv");
        badge.getStyleClass().add("routing-badge-active");
        header.getChildren().addAll(icon, lblTitle, spacer, badge);

        // Zeile 1: Engine Status
        HBox row1 = new HBox(8.0);
        row1.setAlignment(Pos.CENTER_LEFT);
        Label lblFps = new Label("Engine: 40 FPS");
        lblFps.getStyleClass().add("routing-stat-highlight");
        Region sp1 = new Region();
        HBox.setHgrow(sp1, Priority.ALWAYS);
        Label lblMaster = new Label("Master: 100%");
        lblMaster.getStyleClass().add("routing-stat-normal");
        row1.getChildren().addAll(lblFps, sp1, lblMaster);

        // Zeile 2: Blackout
        HBox row2 = new HBox(8.0);
        row2.setAlignment(Pos.CENTER_LEFT);
        Label lblBlackout = new Label("Blackout: AUS");
        lblBlackout.getStyleClass().add("routing-subtext-green");
        row2.getChildren().add(lblBlackout);

        box.getChildren().addAll(header, row1, row2);
        return box;
    }

    private VBox buildOutputBox() {
        VBox box = new VBox(6.0);
        box.getStyleClass().addAll("routing-box", "routing-box-output");
        box.setPadding(new Insets(8.0, 12.0, 8.0, 12.0));

        // Header
        HBox header = new HBox(6.0);
        header.setAlignment(Pos.CENTER_LEFT);
        LucideIcon icon = new LucideIcon("arrow-right-left", 14.0, Color.web("#10B981"));
        Label lblTitle = new Label("OUTPUT");
        lblTitle.getStyleClass().add("routing-box-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label badge = new Label("Art-Net Aktiv");
        badge.getStyleClass().add("routing-badge-active");
        header.getChildren().addAll(icon, lblTitle, spacer, badge);

        // Zeile 1: Art-Net Checkbox + Ziel-IP + Universum (wie sound2artnet)
        HBox row1 = new HBox(6.0);
        row1.setAlignment(Pos.CENTER_LEFT);

        Label lblIp = new Label("Ziel-IP:");
        lblIp.getStyleClass().add("routing-field-label");

        Label lblUni = new Label("Universum:");
        lblUni.getStyleClass().add("routing-field-label");

        Region sp1 = new Region();
        HBox.setHgrow(sp1, Priority.ALWAYS);

        row1.getChildren().addAll(artNetCheckbox, sp1, lblIp, targetIpField, lblUni, universeSpinner);

        // Zeile 2: Hardware USB-DMX Platzhalter
        HBox row2 = new HBox(8.0);
        row2.setAlignment(Pos.CENTER_LEFT);
        Label lblHw = new Label("JMS USB-DMX: Inaktiv (Phase 2)");
        lblHw.getStyleClass().add("routing-subtext");
        row2.getChildren().add(lblHw);

        box.getChildren().addAll(header, row1, row2);
        return box;
    }

    private Node buildFlowArrow(String caption, Color color) {
        VBox arrowBox = new VBox(2.0);
        arrowBox.setAlignment(Pos.CENTER);
        arrowBox.getStyleClass().add("routing-arrow-box");

        Label lbl = new Label(caption);
        lbl.getStyleClass().add("routing-arrow-caption");
        lbl.setTextFill(color);

        LucideIcon arrowIcon = new LucideIcon("arrow-right", 22.0, color);
        arrowBox.getChildren().addAll(lbl, arrowIcon);
        return arrowBox;
    }

    public VBox getInputBox() {
        return inputBox;
    }

    public VBox getJdmxBox() {
        return jdmxBox;
    }

    public VBox getOutputBox() {
        return outputBox;
    }

    public Node getArrowInputToJdmx() {
        return arrowInputToJdmx;
    }

    public Node getArrowJdmxToOutput() {
        return arrowJdmxToOutput;
    }

    public CheckBox getArtNetCheckbox() {
        return artNetCheckbox;
    }

    public TextField getTargetIpField() {
        return targetIpField;
    }

    public Spinner<Integer> getUniverseSpinner() {
        return universeSpinner;
    }

    public ComboBox<String> getMidiDeviceCombo() {
        return midiDeviceCombo;
    }
}

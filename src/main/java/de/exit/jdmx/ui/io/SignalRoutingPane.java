package de.exit.jdmx.ui.io;

import de.exit.jdmx.ui.icon.LucideIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Horizontale Signalfluss-Leiste in der oberen Bildschirmhälfte von Tab 5:
 * <pre>
 * [ INPUT ] ════► [ JDMX PROZESSOR ] ════► [ OUTPUT ]
 * </pre>
 * <p>
 * Links: Konfiguration der Eingänge (MIDI, Controller, Loopback).<br>
 * Mitte: Reine Status-Zentrale der 40 Hz Engine (unberührbar).<br>
 * Rechts: DMX-Ausgänge (voll ausgestattetes Art-Net Setup & vorbereitetes Hardware-DMX).
 * </p>
 */
public class SignalRoutingPane extends HBox {

    /** Feste Höhe der kompakten Signal-Routing-Leiste. */
    public static final double PANE_HEIGHT = 118.0;

    private final VBox inputBox;
    private final VBox jdmxBox;
    private final VBox outputBox;

    private final Node arrowInputToJdmx;
    private final Node arrowJdmxToOutput;

    // Art-Net Bedienelemente (Output)
    private final CheckBox artNetCheckbox;
    private final ComboBox<String> networkInterfaceCombo;
    private final RadioButton broadcastRadio;
    private final RadioButton unicastRadio;
    private final TextField unicastIpField;
    private final Spinner<Integer> subnetSpinner;
    private final Spinner<Integer> universeSpinner;

    public SignalRoutingPane() {
        getStyleClass().add("signal-routing-pane");
        setAlignment(Pos.CENTER);
        setSpacing(12.0);
        setPadding(new Insets(8.0, 10.0, 8.0, 10.0));
        setMinHeight(PANE_HEIGHT);
        setPrefHeight(PANE_HEIGHT);
        setMaxHeight(PANE_HEIGHT);

        // 1. Box Links: INPUT
        this.inputBox = buildInputBox();
        HBox.setHgrow(inputBox, Priority.ALWAYS);

        // Pfeil 1: INPUT -> JDMX
        this.arrowInputToJdmx = buildFlowArrow("MIDI", Color.web("#71717A"));

        // 2. Box Mitte: JDMX CORE
        this.jdmxBox = buildJdmxBox();
        HBox.setHgrow(jdmxBox, Priority.ALWAYS);

        // Pfeil 2: JDMX -> OUTPUT
        this.arrowJdmxToOutput = buildFlowArrow("DMX", Color.web("#10B981"));

        // 3. Box Rechts: OUTPUT
        // Controls initialisieren
        this.artNetCheckbox = new CheckBox("Art-Net");
        artNetCheckbox.getStyleClass().add("routing-checkbox");
        artNetCheckbox.setSelected(true);

        this.networkInterfaceCombo = new ComboBox<>();
        networkInterfaceCombo.getStyleClass().add("routing-combo");
        populateNetworkInterfaces();

        this.broadcastRadio = new RadioButton("Broadcast");
        broadcastRadio.getStyleClass().add("routing-radio");
        broadcastRadio.setSelected(true);

        this.unicastRadio = new RadioButton("Unicast");
        unicastRadio.getStyleClass().add("routing-radio");

        ToggleGroup destGroup = new ToggleGroup();
        broadcastRadio.setToggleGroup(destGroup);
        unicastRadio.setToggleGroup(destGroup);

        this.unicastIpField = new TextField("255.255.255.255");
        unicastIpField.getStyleClass().add("routing-text-field");
        unicastIpField.setPrefWidth(110.0);
        unicastIpField.setDisable(true);

        unicastRadio.selectedProperty().addListener((obs, oldV, isUnicast) -> {
            unicastIpField.setDisable(!isUnicast);
        });

        this.subnetSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 15, 0));
        subnetSpinner.getStyleClass().add("routing-spinner");
        subnetSpinner.setPrefWidth(55.0);

        this.universeSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 15, 0));
        universeSpinner.getStyleClass().add("routing-spinner");
        universeSpinner.setPrefWidth(55.0);

        this.outputBox = buildOutputBox();
        HBox.setHgrow(outputBox, Priority.ALWAYS);

        getChildren().addAll(inputBox, arrowInputToJdmx, jdmxBox, arrowJdmxToOutput, outputBox);
    }

    private VBox buildInputBox() {
        VBox box = new VBox(4.0);
        box.getStyleClass().add("routing-box");
        box.setPadding(new Insets(8.0, 10.0, 8.0, 10.0));

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

        // Device Dropdown
        ComboBox<String> midiCombo = new ComboBox<>();
        midiCombo.getStyleClass().add("routing-combo");
        midiCombo.getItems().add("Kein Gerät verbunden");
        midiCombo.setValue("Kein Gerät verbunden");
        midiCombo.setDisable(true);
        midiCombo.setMaxWidth(Double.MAX_VALUE);

        // Footer-Info
        HBox footer = new HBox(8.0);
        footer.setAlignment(Pos.CENTER_LEFT);
        Label lblSignal = new Label("Letztes Signal: --");
        lblSignal.getStyleClass().add("routing-subtext");
        footer.getChildren().add(lblSignal);

        box.getChildren().addAll(header, midiCombo, footer);
        return box;
    }

    private VBox buildJdmxBox() {
        VBox box = new VBox(4.0);
        box.getStyleClass().addAll("routing-box", "routing-box-core");
        box.setPadding(new Insets(8.0, 10.0, 8.0, 10.0));

        // Header
        HBox header = new HBox(6.0);
        header.setAlignment(Pos.CENTER_LEFT);
        LucideIcon icon = new LucideIcon("cpu", 14.0, Color.web("#38BDF8"));
        Label lblTitle = new Label("JDMX PROZESSOR");
        lblTitle.getStyleClass().add("routing-box-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label badge = new Label("40 Hz Aktiv");
        badge.getStyleClass().add("routing-badge-active");
        header.getChildren().addAll(icon, lblTitle, spacer, badge);

        // Live-Metrics Grid
        HBox row1 = new HBox(8.0);
        row1.setAlignment(Pos.CENTER_LEFT);
        Label lblFps = new Label("Engine: 40.0 FPS (25ms)");
        lblFps.getStyleClass().add("routing-stat-highlight");
        Region sp1 = new Region();
        HBox.setHgrow(sp1, Priority.ALWAYS);
        Label lblMerging = new Label("HTP / LTP");
        lblMerging.getStyleClass().add("routing-stat-normal");
        row1.getChildren().addAll(lblFps, sp1, lblMerging);

        HBox row2 = new HBox(8.0);
        row2.setAlignment(Pos.CENTER_LEFT);
        Label lblMaster = new Label("Grand Master: 100%");
        lblMaster.getStyleClass().add("routing-subtext");
        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);
        Label lblBlackout = new Label("Blackout: AUS");
        lblBlackout.getStyleClass().add("routing-subtext-green");
        row2.getChildren().addAll(lblMaster, sp2, lblBlackout);

        box.getChildren().addAll(header, row1, row2);
        return box;
    }

    private VBox buildOutputBox() {
        VBox box = new VBox(4.0);
        box.getStyleClass().addAll("routing-box", "routing-box-output");
        box.setPadding(new Insets(8.0, 10.0, 8.0, 10.0));

        // Header
        HBox header = new HBox(6.0);
        header.setAlignment(Pos.CENTER_LEFT);
        LucideIcon icon = new LucideIcon("arrow-right-left", 14.0, Color.web("#10B981"));
        Label lblTitle = new Label("OUTPUT");
        lblTitle.getStyleClass().add("routing-box-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label badge = new Label("Art-Net Sendet");
        badge.getStyleClass().add("routing-badge-active");
        header.getChildren().addAll(icon, lblTitle, spacer, badge);

        // Zeile 1: Art-Net Checkbox + NIC ComboBox + Sub/Uni
        HBox row1 = new HBox(6.0);
        row1.setAlignment(Pos.CENTER_LEFT);
        networkInterfaceCombo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(networkInterfaceCombo, Priority.ALWAYS);

        Label lblSub = new Label("S:");
        lblSub.getStyleClass().add("routing-field-label");
        Label lblUni = new Label("U:");
        lblUni.getStyleClass().add("routing-field-label");

        row1.getChildren().addAll(artNetCheckbox, networkInterfaceCombo, lblSub, subnetSpinner, lblUni, universeSpinner);

        // Zeile 2: Broadcast / Unicast Toggle + Hardware JMS Platzhalter
        HBox row2 = new HBox(8.0);
        row2.setAlignment(Pos.CENTER_LEFT);
        Label lblHw = new Label("JMS USB-DMX: Inaktiv");
        lblHw.getStyleClass().add("routing-subtext");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        row2.getChildren().addAll(broadcastRadio, unicastRadio, sp, lblHw);

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

        LucideIcon arrowIcon = new LucideIcon("arrow-right", 24.0, color);
        arrowBox.getChildren().addAll(lbl, arrowIcon);
        return arrowBox;
    }

    private void populateNetworkInterfaces() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    NetworkInterface nif = interfaces.nextElement();
                    if (nif.isUp() && !nif.isLoopback()) {
                        String name = nif.getDisplayName();
                        if (name.length() > 22) {
                            name = name.substring(0, 22) + "...";
                        }
                        networkInterfaceCombo.getItems().add(name);
                    }
                }
            }
        } catch (Exception ignored) {
            // Fallback bei Sicherheitsbeschränkungen
        }

        if (networkInterfaceCombo.getItems().isEmpty()) {
            networkInterfaceCombo.getItems().addAll("Ethernet", "WLAN", "Loopback");
        }
        networkInterfaceCombo.getSelectionModel().selectFirst();
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

    public ComboBox<String> getNetworkInterfaceCombo() {
        return networkInterfaceCombo;
    }

    public RadioButton getBroadcastRadio() {
        return broadcastRadio;
    }

    public RadioButton getUnicastRadio() {
        return unicastRadio;
    }

    public TextField getUnicastIpField() {
        return unicastIpField;
    }

    public Spinner<Integer> getSubnetSpinner() {
        return subnetSpinner;
    }

    public Spinner<Integer> getUniverseSpinner() {
        return universeSpinner;
    }
}

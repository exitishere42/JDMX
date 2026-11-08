package de.exit.jdmx.ui.io;

import de.exit.jdmx.core.i18n.I18n;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Steuerungs-Dock für Ausgänge (Art-Net Out & USB-DMX) im Tab "I/O Patch".
 * <p>
 * Baut exakt auf dem Design-Standard der Steuerungs-Docks aus Stage & Fixtures auf:
 * Scharfkantige Karte ({@code control-module-card}) mit konsistentem Header und flexibler
 * Verschiebbarkeit im SplitPane.
 * </p>
 */
public class OutputDockView extends VBox {

    private final Label lblTitle;
    private final TextField targetIpField;
    private final Spinner<Integer> universeSpinner;
    private final Label lblIp;
    private final Label lblUni;
    private final Label lblHwTitle;
    private final Label lblHwStatus;
    private final Label lblRateTitle;
    private final Label lblRate;
    private final Label badge;

    public OutputDockView() {
        super(4);
        getStyleClass().add("control-module-card");
        setPadding(new Insets(6.0, 10.0, 8.0, 10.0));
        setMinHeight(110.0);

        // 1. Schlichter Header
        HBox header = new HBox(6.0);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("module-card-header");

        this.lblTitle = new Label(I18n.get("io.dock.output"));
        lblTitle.getStyleClass().add("module-card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        this.badge = new Label(I18n.get("io.artnet_active"));
        badge.setStyle("-fx-background-color: rgba(16, 185, 129, 0.15); -fx-text-fill: #34D399; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-border-color: #065F46; -fx-border-radius: 3px; -fx-background-radius: 3px;");

        header.getChildren().addAll(lblTitle, spacer, badge);

        // 2. Formular-Körper
        VBox body = new VBox(8.0);
        body.setAlignment(Pos.CENTER_LEFT);
        body.setPadding(new Insets(6.0, 2.0, 4.0, 2.0));
        VBox.setVgrow(body, Priority.ALWAYS);

        // Zeile 1: Ziel-IP & Universum
        HBox rowArtNet = new HBox(8.0);
        rowArtNet.setAlignment(Pos.CENTER_LEFT);

        this.lblIp = new Label(I18n.get("io.target_ip"));
        lblIp.setStyle("-fx-font-size: 11px; -fx-text-fill: #A1A1AA; -fx-min-width: 60px;");

        this.targetIpField = new TextField("127.0.0.1");
        targetIpField.getStyleClass().add("routing-text-field");
        targetIpField.setPromptText("127.0.0.1");
        targetIpField.setPrefWidth(130.0);
        HBox.setHgrow(targetIpField, Priority.ALWAYS);

        this.lblUni = new Label(I18n.get("io.col.universe") + ":");
        lblUni.setStyle("-fx-font-size: 11px; -fx-text-fill: #A1A1AA; -fx-min-width: 65px;");

        this.universeSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 15, 0));
        universeSpinner.getStyleClass().add("routing-spinner");
        universeSpinner.setPrefWidth(60.0);

        rowArtNet.getChildren().addAll(lblIp, targetIpField, lblUni, universeSpinner);

        // Zeile 2: Hardware USB-DMX
        HBox rowHw = new HBox(8.0);
        rowHw.setAlignment(Pos.CENTER_LEFT);
        this.lblHwTitle = new Label(I18n.get("io.usb_dmx"));
        lblHwTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #A1A1AA; -fx-min-width: 60px;");

        this.lblHwStatus = new Label(I18n.get("io.hw_status"));
        lblHwStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #71717A;");

        rowHw.getChildren().addAll(lblHwTitle, lblHwStatus);

        // Zeile 3: Rate
        HBox rowRate = new HBox(8.0);
        rowRate.setAlignment(Pos.CENTER_LEFT);
        this.lblRateTitle = new Label(I18n.get("io.rate_title"));
        lblRateTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #A1A1AA; -fx-min-width: 60px;");

        this.lblRate = new Label("40 DMX/s (25 ms)");
        lblRate.setStyle("-fx-font-size: 11px; -fx-text-fill: #34D399; -fx-font-weight: bold;");

        rowRate.getChildren().addAll(lblRateTitle, lblRate);

        body.getChildren().addAll(rowArtNet, rowHw, rowRate);

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
        return I18n.get("io.dock.output");
    }

    /**
     * Aktualisiert alle Beschriftungen bei einem Sprachwechsel.
     */
    public void updateLocalization() {
        lblTitle.setText(I18n.get("io.dock.output"));
        badge.setText(I18n.get("io.artnet_active"));
        lblIp.setText(I18n.get("io.target_ip"));
        lblUni.setText(I18n.get("io.col.universe") + ":");
        lblHwTitle.setText(I18n.get("io.usb_dmx"));
        lblHwStatus.setText(I18n.get("io.hw_status"));
        lblRateTitle.setText(I18n.get("io.rate_title"));
    }

    public TextField getTargetIpField() {
        return targetIpField;
    }

    public Spinner<Integer> getUniverseSpinner() {
        return universeSpinner;
    }

    public Label getLblHwStatus() {
        return lblHwStatus;
    }

    public Label getLblRate() {
        return lblRate;
    }

    public Label getBadge() {
        return badge;
    }
}

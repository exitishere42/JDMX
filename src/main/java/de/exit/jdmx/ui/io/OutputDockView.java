package de.exit.jdmx.ui.io;

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

    private final String title = "Ausgang (Output)";
    private final TextField targetIpField;
    private final Spinner<Integer> universeSpinner;
    private final Label lblHwStatus;
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

        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("module-card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        this.badge = new Label("Art-Net Aktiv");
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

        Label lblIp = new Label("Ziel-IP:");
        lblIp.setStyle("-fx-font-size: 11px; -fx-text-fill: #A1A1AA; -fx-min-width: 60px;");

        this.targetIpField = new TextField("127.0.0.1");
        targetIpField.getStyleClass().add("routing-text-field");
        targetIpField.setPromptText("127.0.0.1");
        targetIpField.setPrefWidth(130.0);
        HBox.setHgrow(targetIpField, Priority.ALWAYS);

        Label lblUni = new Label("Universum:");
        lblUni.setStyle("-fx-font-size: 11px; -fx-text-fill: #A1A1AA; -fx-min-width: 65px;");

        this.universeSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 15, 0));
        universeSpinner.getStyleClass().add("routing-spinner");
        universeSpinner.setPrefWidth(60.0);

        rowArtNet.getChildren().addAll(lblIp, targetIpField, lblUni, universeSpinner);

        // Zeile 2: Hardware USB-DMX
        HBox rowHw = new HBox(8.0);
        rowHw.setAlignment(Pos.CENTER_LEFT);
        Label lblHwTitle = new Label("USB-DMX:");
        lblHwTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #A1A1AA; -fx-min-width: 60px;");

        this.lblHwStatus = new Label("JMS USB-DMX (Inaktiv / Phase 2)");
        lblHwStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #71717A;");

        rowHw.getChildren().addAll(lblHwTitle, lblHwStatus);

        // Zeile 3: Rate
        HBox rowRate = new HBox(8.0);
        rowRate.setAlignment(Pos.CENTER_LEFT);
        Label lblRateTitle = new Label("Ausgaberate:");
        lblRateTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #A1A1AA; -fx-min-width: 60px;");

        this.lblRate = new Label("40 DMX/s (25 ms)");
        lblRate.setStyle("-fx-font-size: 11px; -fx-text-fill: #34D399; -fx-font-weight: bold;");

        rowRate.getChildren().addAll(lblRateTitle, lblRate);

        body.getChildren().addAll(rowArtNet, rowHw, rowRate);

        getChildren().addAll(header, body);
    }

    public String getTitle() {
        return title;
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

package de.exit.jdmx.ui.io;

import de.exit.jdmx.ui.icon.LucideIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Universum I/O Patch-Tabelle nach Vorbild von QLC+.
 * <p>
 * Zeigt eine übersichtliche Tabelle aller 4 DMX-Universen an.
 * Jedes Universum verfügt über Dropdown-Menüs zur Auswahl von Input (Art-Net, MIDI, Loopback, Kein)
 * und Output (Art-Net, USB-DMX, Kein) sowie eine Live-Statusanzeige und einen Konfigurations-Button.
 * </p>
 */
public class UniversePatchTable extends VBox {

    public static final int UNIVERSE_COUNT = 4;

    private final List<UniverseRow> universeRows = new ArrayList<>(UNIVERSE_COUNT);

    public UniversePatchTable() {
        getStyleClass().add("universe-patch-table");
        setSpacing(4.0);
        setPadding(new Insets(10.0, 12.0, 10.0, 12.0));

        // Titel-Leiste
        HBox titleBar = new HBox(8.0);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        LucideIcon titleIcon = new LucideIcon("arrow-right-left", 15.0, Color.web("#A1A1AA"));
        Label lblTitle = new Label("UNIVERSUM-ZUORDNUNG (INPUT / OUTPUT PATCH)");
        lblTitle.getStyleClass().add("patch-table-title");
        titleBar.getChildren().addAll(titleIcon, lblTitle);
        titleBar.setPadding(new Insets(0, 0, 4.0, 0));

        // Tabellenkopf
        HBox headerRow = buildHeaderRow();

        // Zeilencontainer
        VBox rowsContainer = new VBox(2.0);
        rowsContainer.getStyleClass().add("patch-rows-container");

        for (int u = 1; u <= UNIVERSE_COUNT; u++) {
            UniverseRow row = new UniverseRow(u);
            universeRows.add(row);
            rowsContainer.getChildren().add(row);
        }

        getChildren().addAll(titleBar, headerRow, rowsContainer);
    }

    private HBox buildHeaderRow() {
        HBox header = new HBox(12.0);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("patch-header-row");
        header.setPadding(new Insets(4.0, 8.0, 4.0, 8.0));

        Label lblUni = new Label("UNIVERSUM");
        lblUni.getStyleClass().add("patch-header-label");
        lblUni.setPrefWidth(120.0);

        Label lblInput = new Label("INPUT");
        lblInput.getStyleClass().add("patch-header-label");
        lblInput.setPrefWidth(220.0);
        HBox.setHgrow(lblInput, Priority.ALWAYS);

        Label lblOutput = new Label("OUTPUT");
        lblOutput.getStyleClass().add("patch-header-label");
        lblOutput.setPrefWidth(220.0);
        HBox.setHgrow(lblOutput, Priority.ALWAYS);

        Label lblStatus = new Label("STATUS");
        lblStatus.getStyleClass().add("patch-header-label");
        lblStatus.setPrefWidth(90.0);

        Label lblAction = new Label("");
        lblAction.setPrefWidth(36.0);

        header.getChildren().addAll(lblUni, lblInput, lblOutput, lblStatus, lblAction);
        return header;
    }

    public List<UniverseRow> getUniverseRows() {
        return Collections.unmodifiableList(universeRows);
    }

    public UniverseRow getRow(int universeNumber) {
        if (universeNumber >= 1 && universeNumber <= UNIVERSE_COUNT) {
            return universeRows.get(universeNumber - 1);
        }
        return null;
    }

    /**
     * Einzelne Tabellenzeile für ein DMX-Universum.
     */
    public static class UniverseRow extends HBox {

        private final int universeNumber;
        private final ComboBox<String> inputCombo;
        private final ComboBox<String> outputCombo;
        private final Circle statusDot;
        private final Label statusText;
        private final Button configBtn;

        public UniverseRow(int universeNumber) {
            this.universeNumber = universeNumber;

            getStyleClass().add("patch-data-row");
            setAlignment(Pos.CENTER_LEFT);
            setSpacing(12.0);
            setPadding(new Insets(5.0, 8.0, 5.0, 8.0));

            // Spalte 1: Universum Name
            Label lblName = new Label("Universum " + universeNumber);
            lblName.getStyleClass().add("patch-universe-name");
            lblName.setPrefWidth(120.0);

            // Spalte 2: Input Dropdown
            this.inputCombo = new ComboBox<>();
            inputCombo.getStyleClass().add("patch-combo");
            inputCombo.setPrefWidth(220.0);
            inputCombo.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(inputCombo, Priority.ALWAYS);

            inputCombo.getItems().addAll(
                    "Kein Input",
                    "Art-Net (127.0.0.1, Uni " + (universeNumber - 1) + ")",
                    "MIDI: Kein Gerät verbunden",
                    "Loopback"
            );
            inputCombo.setValue("Kein Input");

            // Spalte 3: Output Dropdown
            this.outputCombo = new ComboBox<>();
            outputCombo.getStyleClass().add("patch-combo");
            outputCombo.setPrefWidth(220.0);
            outputCombo.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(outputCombo, Priority.ALWAYS);

            outputCombo.getItems().addAll(
                    "Kein Output",
                    "Art-Net (127.0.0.1, Uni " + (universeNumber - 1) + ")",
                    "JMS USB-DMX (Inaktiv)"
            );

            // Universum 1 standardmäßig mit Art-Net Output vorwählen
            if (universeNumber == 1) {
                outputCombo.setValue("Art-Net (127.0.0.1, Uni 0)");
            } else {
                outputCombo.setValue("Kein Output");
            }

            // Spalte 4: Status-Dot + Status-Text
            this.statusDot = new Circle(4.0);
            this.statusText = new Label();
            statusText.getStyleClass().add("patch-status-text");

            HBox statusBox = new HBox(6.0, statusDot, statusText);
            statusBox.setAlignment(Pos.CENTER_LEFT);
            statusBox.setPrefWidth(90.0);

            // Spalte 5: Config Button
            this.configBtn = new Button();
            configBtn.getStyleClass().add("patch-config-btn");
            LucideIcon cfgIcon = new LucideIcon("settings", 13.0, Color.web("#A1A1AA"));
            configBtn.setGraphic(cfgIcon);
            configBtn.setTooltip(new Tooltip("Einstellungen für Universum " + universeNumber));
            configBtn.setPrefWidth(32.0);
            configBtn.setPrefHeight(26.0);

            // Event-Listener zur Status-Aktualisierung
            inputCombo.valueProperty().addListener((obs, oldV, newV) -> updateStatus());
            outputCombo.valueProperty().addListener((obs, oldV, newV) -> updateStatus());

            getChildren().addAll(lblName, inputCombo, outputCombo, statusBox, configBtn);
            updateStatus();
        }

        private void updateStatus() {
            boolean hasInput = inputCombo.getValue() != null && !inputCombo.getValue().equals("Kein Input");
            boolean hasOutput = outputCombo.getValue() != null && !outputCombo.getValue().equals("Kein Output");
            boolean active = hasInput || hasOutput;

            if (active) {
                statusDot.setFill(Color.web("#10B981"));
                statusText.setText("Aktiv");
                statusText.setStyle("-fx-text-fill: #34D399; -fx-font-weight: bold;");
            } else {
                statusDot.setFill(Color.web("#52525B"));
                statusText.setText("Inaktiv");
                statusText.setStyle("-fx-text-fill: #71717A; -fx-font-weight: normal;");
            }
        }

        public int getUniverseNumber() {
            return universeNumber;
        }

        public ComboBox<String> getInputCombo() {
            return inputCombo;
        }

        public ComboBox<String> getOutputCombo() {
            return outputCombo;
        }

        public Circle getStatusDot() {
            return statusDot;
        }

        public Label getStatusText() {
            return statusText;
        }

        public Button getConfigBtn() {
            return configBtn;
        }

        public boolean isActive() {
            boolean hasInput = inputCombo.getValue() != null && !inputCombo.getValue().equals("Kein Input");
            boolean hasOutput = outputCombo.getValue() != null && !outputCombo.getValue().equals("Kein Output");
            return hasInput || hasOutput;
        }
    }
}

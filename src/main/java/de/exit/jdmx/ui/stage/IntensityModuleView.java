package de.exit.jdmx.ui.stage;

import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Kompaktes Steuerungsmodul für Intensität (exakter DMXControl-Stil).
 * <p>
 * Scharfkantig (0px Radius auf der Modulkarte) mit festen, harmonischen Proportionen
 * ohne horizontale Verzerrung:
 * <ul>
 *     <li>Links: 5er-Tastenleiste (VL, 70, 50, 30, 0) bündig von oben nach unten gestapelt</li>
 *     <li>Mitte: 2 vertikale Fader (Dimmer und Strobo – ohne Shutter) mit Beschriftung darunter</li>
 *     <li>Rechts: 2x2 Tastenblock (Auf, Zu, Lumos, Nox) mit Beschriftung "Intensität" darunter</li>
 * </ul>
 * </p>
 */
public class IntensityModuleView extends VBox {

    private final String title = "Intensität";

    private final Slider dimmerSlider;
    private final Slider strobeSlider;

    private final List<Button> quickButtons = new ArrayList<>();
    private final Button btnAuf;
    private final Button btnZu;
    private final Button btnLumos;
    private final Button btnNox;

    public IntensityModuleView() {
        super(2);
        getStyleClass().add("control-module-card");
        setMinWidth(150.0);
        setPrefWidth(185.0);

        // 1. Schlichter Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("module-card-header");
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("module-card-title");
        header.getChildren().add(lblTitle);

        // 2. Fader (Dimmer & Strobo)
        this.dimmerSlider = new Slider(0, 100, 100);
        dimmerSlider.setOrientation(Orientation.VERTICAL);
        dimmerSlider.getStyleClass().add("fader-slider");
        dimmerSlider.setPrefHeight(86);
        dimmerSlider.setMaxHeight(95);
        VBox.setVgrow(dimmerSlider, Priority.ALWAYS);
        VBox dimmerCol = createFaderCol("Dimmer", dimmerSlider);

        this.strobeSlider = new Slider(0, 25, 0);
        strobeSlider.setOrientation(Orientation.VERTICAL);
        strobeSlider.getStyleClass().add("fader-slider");
        strobeSlider.setPrefHeight(86);
        strobeSlider.setMaxHeight(95);
        VBox.setVgrow(strobeSlider, Priority.ALWAYS);
        VBox strobeCol = createFaderCol("Strobo", strobeSlider);

        // 3. Links: 5er Tastenleiste (VL, 70, 50, 30, 0) bündig gestapelt
        VBox quickBox = new VBox(1);
        quickBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(quickBox, Priority.ALWAYS);

        double[] percentages = {100.0, 70.0, 50.0, 30.0, 0.0};
        String[] labels = {"VL", "70", "50", "30", "0"};

        for (int i = 0; i < percentages.length; i++) {
            final double pct = percentages[i];
            Button btn = new Button(labels[i]);
            btn.getStyleClass().add("module-quick-btn");
            btn.setPrefSize(32, 16);
            btn.setMinSize(28, 14);
            btn.setMaxSize(36, 18);
            VBox.setVgrow(btn, Priority.ALWAYS);
            btn.setOnAction(e -> dimmerSlider.setValue(pct));
            quickButtons.add(btn);
            quickBox.getChildren().add(btn);
        }

        Label lblQuickPlaceholder = new Label("");
        lblQuickPlaceholder.getStyleClass().add("fader-title-label");
        VBox quickCol = new VBox(2, quickBox, lblQuickPlaceholder);
        quickCol.setAlignment(Pos.CENTER);
        VBox.setVgrow(quickCol, Priority.ALWAYS);

        // 4. Rechts: 2x2 Tastenblock (Auf, Zu, Lumos, Nox) mit festen, kompakten Maßen
        this.btnAuf = new Button("Auf");
        btnAuf.getStyleClass().add("module-small-btn");
        btnAuf.setPrefSize(42, 46);
        btnAuf.setMinSize(36, 38);
        btnAuf.setMaxSize(48, 52);
        VBox.setVgrow(btnAuf, Priority.ALWAYS);
        btnAuf.setOnAction(e -> dimmerSlider.setValue(100.0));

        this.btnZu = new Button("Zu");
        btnZu.getStyleClass().add("module-small-btn");
        btnZu.setPrefSize(42, 26);
        btnZu.setMinSize(36, 20);
        btnZu.setMaxSize(48, 30);
        btnZu.setOnAction(e -> dimmerSlider.setValue(0.0));

        VBox colAufZu = new VBox(2, btnAuf, btnZu);
        colAufZu.setAlignment(Pos.CENTER);
        VBox.setVgrow(colAufZu, Priority.ALWAYS);

        this.btnLumos = new Button("Lumos");
        btnLumos.getStyleClass().add("module-small-btn");
        btnLumos.setPrefSize(48, 46);
        btnLumos.setMinSize(42, 38);
        btnLumos.setMaxSize(54, 52);
        VBox.setVgrow(btnLumos, Priority.ALWAYS);
        btnLumos.setOnAction(e -> dimmerSlider.setValue(100.0));

        this.btnNox = new Button("Nox");
        btnNox.getStyleClass().add("module-small-btn");
        btnNox.setPrefSize(48, 26);
        btnNox.setMinSize(42, 20);
        btnNox.setMaxSize(54, 30);
        btnNox.setOnAction(e -> dimmerSlider.setValue(0.0));

        VBox colLumosNox = new VBox(2, btnLumos, btnNox);
        colLumosNox.setAlignment(Pos.CENTER);
        VBox.setVgrow(colLumosNox, Priority.ALWAYS);

        HBox btnGridRow = new HBox(2, colAufZu, colLumosNox);
        btnGridRow.setAlignment(Pos.CENTER);
        VBox.setVgrow(btnGridRow, Priority.ALWAYS);

        Label lblIntensitaet = new Label("Intensität");
        lblIntensitaet.getStyleClass().add("fader-title-label");

        VBox actionCol = new VBox(2, btnGridRow, lblIntensitaet);
        actionCol.setAlignment(Pos.CENTER);
        VBox.setVgrow(actionCol, Priority.ALWAYS);

        // Kompakter, zentrierter Gesamtinhalt
        HBox body = new HBox(6, quickCol, dimmerCol, strobeCol, actionCol);
        body.setAlignment(Pos.CENTER);
        VBox.setVgrow(body, Priority.ALWAYS);

        getChildren().addAll(header, body);
    }

    private VBox createFaderCol(String labelText, Slider slider) {
        VBox col = new VBox(2);
        col.setAlignment(Pos.CENTER);
        VBox.setVgrow(slider, Priority.ALWAYS);
        VBox.setVgrow(col, Priority.ALWAYS);

        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("fader-title-label");

        col.getChildren().addAll(slider, lbl);
        return col;
    }

    public String getTitle() {
        return title;
    }

    public Slider getDimmerSlider() {
        return dimmerSlider;
    }

    public Slider getStrobeSlider() {
        return strobeSlider;
    }

    public List<Button> getQuickButtons() {
        return quickButtons;
    }

    public Button getBtnAuf() {
        return btnAuf;
    }

    public Button getBtnZu() {
        return btnZu;
    }

    public Button getBtnLumos() {
        return btnLumos;
    }

    public Button getBtnNox() {
        return btnNox;
    }
}

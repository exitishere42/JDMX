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
 * Scharfkantig (0px Radius auf Modul und Buttons mit Umrandung), mit 300px Breite
 * optimal bemessen für großzügigen Randabstand zur Farbkante und vollflächig in der vertikalen Höhe:
 * <ul>
 *     <li>Links: 5er-Tastenleiste (VL, 70, 50, 30, 0) mit 44px Breite eckig über die volle Faderhöhe</li>
 *     <li>Mitte: 2 vertikale Fader (Dimmer und Strobo) in 50px Spalten mit voll ausgeschriebenen Beschriftungen</li>
 *     <li>Rechts: 2x2 Tastenblock (Auf, Zu, Lumos, Nox) mit reichlich Luft zur rechten Border</li>
 * </ul>
 * </p>
 */
public class IntensityModuleView extends VBox {

    /** Feste, unveränderliche Breite des Intensitätsmoduls (300px für perfekten Randabstand). */
    public static final double FIXED_WIDTH = 300.0;

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
        setMinWidth(FIXED_WIDTH);
        setPrefWidth(FIXED_WIDTH);
        setMaxWidth(FIXED_WIDTH);

        // 1. Schlichter Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("module-card-header");
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("module-card-title");
        header.getChildren().add(lblTitle);

        // 2. Fader (Dimmer & Strobo) - 50px Spalten mit reichlich Platz
        this.dimmerSlider = new Slider(0, 100, 100);
        dimmerSlider.setOrientation(Orientation.VERTICAL);
        dimmerSlider.getStyleClass().add("fader-slider");
        dimmerSlider.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(dimmerSlider, Priority.ALWAYS);
        VBox dimmerCol = createFaderCol("Dimmer", dimmerSlider);

        this.strobeSlider = new Slider(0, 25, 0);
        strobeSlider.setOrientation(Orientation.VERTICAL);
        strobeSlider.getStyleClass().add("fader-slider");
        strobeSlider.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(strobeSlider, Priority.ALWAYS);
        VBox strobeCol = createFaderCol("Strobo", strobeSlider);

        // 3. Links: 5er Tastenleiste (VL, 70, 50, 30, 0) - 44px breit, eckig mit Umrandung
        VBox quickBox = new VBox(1);
        quickBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(quickBox, Priority.ALWAYS);

        double[] percentages = {100.0, 70.0, 50.0, 30.0, 0.0};
        String[] labels = {"VL", "70", "50", "30", "0"};

        for (int i = 0; i < percentages.length; i++) {
            final double pct = percentages[i];
            Button btn = new Button(labels[i]);
            btn.getStyleClass().add("module-quick-btn");
            btn.setPrefWidth(44);
            btn.setMinWidth(40);
            btn.setMaxWidth(48);
            btn.setMaxHeight(Double.MAX_VALUE);
            VBox.setVgrow(btn, Priority.ALWAYS);
            btn.setOnAction(e -> dimmerSlider.setValue(pct));
            quickButtons.add(btn);
            quickBox.getChildren().add(btn);
        }

        Label lblQuickPlaceholder = new Label("");
        lblQuickPlaceholder.getStyleClass().add("fader-title-label");
        lblQuickPlaceholder.setMinWidth(Region.USE_PREF_SIZE);
        VBox quickCol = new VBox(2, quickBox, lblQuickPlaceholder);
        quickCol.setAlignment(Pos.CENTER);
        VBox.setVgrow(quickCol, Priority.ALWAYS);

        // 4. Rechts: 2x2 Tastenblock (Auf, Zu, Lumos, Nox)
        // Spalte 1: Auf & Zu (52px breit)
        this.btnAuf = new Button("Auf");
        btnAuf.getStyleClass().add("module-small-btn");
        btnAuf.setPrefWidth(52);
        btnAuf.setMinWidth(46);
        btnAuf.setMaxWidth(56);
        btnAuf.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(btnAuf, Priority.ALWAYS);
        btnAuf.setOnAction(e -> dimmerSlider.setValue(100.0));

        this.btnZu = new Button("Zu");
        btnZu.getStyleClass().add("module-small-btn");
        btnZu.setPrefWidth(52);
        btnZu.setMinWidth(46);
        btnZu.setMaxWidth(56);
        btnZu.setPrefHeight(50);
        btnZu.setMinHeight(44);
        btnZu.setOnAction(e -> dimmerSlider.setValue(0.0));

        VBox colAufZu = new VBox(2, btnAuf, btnZu);
        colAufZu.setAlignment(Pos.CENTER);
        VBox.setVgrow(colAufZu, Priority.ALWAYS);

        // Spalte 2: Lumos & Nox (62px breit für saubere Textdarstellung und Randabstand)
        this.btnLumos = new Button("Lumos");
        btnLumos.getStyleClass().add("module-small-btn");
        btnLumos.setPrefWidth(62);
        btnLumos.setMinWidth(56);
        btnLumos.setMaxWidth(68);
        btnLumos.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(btnLumos, Priority.ALWAYS);
        btnLumos.setOnAction(e -> dimmerSlider.setValue(100.0));

        this.btnNox = new Button("Nox");
        btnNox.getStyleClass().add("module-small-btn");
        btnNox.setPrefWidth(62);
        btnNox.setMinWidth(56);
        btnNox.setMaxWidth(68);
        btnNox.setPrefHeight(50);
        btnNox.setMinHeight(44);
        btnNox.setOnAction(e -> dimmerSlider.setValue(0.0));

        VBox colLumosNox = new VBox(2, btnLumos, btnNox);
        colLumosNox.setAlignment(Pos.CENTER);
        VBox.setVgrow(colLumosNox, Priority.ALWAYS);

        HBox btnGridRow = new HBox(3, colAufZu, colLumosNox);
        btnGridRow.setAlignment(Pos.CENTER);
        VBox.setVgrow(btnGridRow, Priority.ALWAYS);

        Label lblIntensitaet = new Label("Intensität");
        lblIntensitaet.getStyleClass().add("fader-title-label");
        lblIntensitaet.setMinWidth(Region.USE_PREF_SIZE);

        VBox actionCol = new VBox(2, btnGridRow, lblIntensitaet);
        actionCol.setAlignment(Pos.CENTER);
        VBox.setVgrow(actionCol, Priority.ALWAYS);

        // Bündiger, luftiger Aufbau mit 8px Spacing über 300px
        HBox body = new HBox(8, quickCol, dimmerCol, strobeCol, actionCol);
        body.setAlignment(Pos.CENTER);
        VBox.setVgrow(body, Priority.ALWAYS);

        getChildren().addAll(header, body);
    }

    private VBox createFaderCol(String labelText, Slider slider) {
        VBox col = new VBox(2);
        col.setAlignment(Pos.CENTER);
        col.setPrefWidth(50);
        col.setMinWidth(48);
        col.setMaxWidth(54);
        VBox.setVgrow(slider, Priority.ALWAYS);
        VBox.setVgrow(col, Priority.ALWAYS);

        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("fader-title-label");
        lbl.setMinWidth(Region.USE_PREF_SIZE);

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

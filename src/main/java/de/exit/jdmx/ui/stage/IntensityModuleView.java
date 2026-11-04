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
 * Scharfkantig (0px Radius auf Modul und Buttons mit Umrandung), mit großzügig bemessener
 * fester Breite (270px) horizontal entkoppelt gegen Verzerrungen und vollflächig in der vertikalen Höhe:
 * <ul>
 *     <li>Links: 5er-Tastenleiste (VL, 70, 50, 30, 0) mit 46px Breite eckig über die volle Faderhöhe</li>
 *     <li>Mitte: 2 vertikale Fader (Dimmer und Strobo) in 54px Spalten mit voll ausgeschriebenen Beschriftungen</li>
 *     <li>Rechts: 2x2 Tastenblock (Auf, Zu, Lumos, Nox) mit viel Platz (Lumos mit 66px) und Label "Intensität"</li>
 * </ul>
 * </p>
 */
public class IntensityModuleView extends VBox {

    /** Feste, unveränderliche Breite des Intensitätsmoduls (270px für perfekten Freiraum). */
    public static final double FIXED_WIDTH = 270.0;

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

        // 2. Fader (Dimmer & Strobo) - 54px Spalten mit reichlich Platz
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

        // 3. Links: 5er Tastenleiste (VL, 70, 50, 30, 0) - 46px breit, eckig mit Umrandung
        VBox quickBox = new VBox(1);
        quickBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(quickBox, Priority.ALWAYS);

        double[] percentages = {100.0, 70.0, 50.0, 30.0, 0.0};
        String[] labels = {"VL", "70", "50", "30", "0"};

        for (int i = 0; i < percentages.length; i++) {
            final double pct = percentages[i];
            Button btn = new Button(labels[i]);
            btn.getStyleClass().add("module-quick-btn");
            btn.setPrefWidth(46);
            btn.setMinWidth(42);
            btn.setMaxWidth(50);
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

        // 4. Rechts: 2x2 Tastenblock (Auf, Zu, Lumos, Nox) - großzügige Breiten
        // Spalte 1: Auf & Zu (56px breit)
        this.btnAuf = new Button("Auf");
        btnAuf.getStyleClass().add("module-small-btn");
        btnAuf.setPrefWidth(56);
        btnAuf.setMinWidth(50);
        btnAuf.setMaxWidth(62);
        btnAuf.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(btnAuf, Priority.ALWAYS);
        btnAuf.setOnAction(e -> dimmerSlider.setValue(100.0));

        this.btnZu = new Button("Zu");
        btnZu.getStyleClass().add("module-small-btn");
        btnZu.setPrefWidth(56);
        btnZu.setMinWidth(50);
        btnZu.setMaxWidth(62);
        btnZu.setPrefHeight(32);
        btnZu.setOnAction(e -> dimmerSlider.setValue(0.0));

        VBox colAufZu = new VBox(2, btnAuf, btnZu);
        colAufZu.setAlignment(Pos.CENTER);
        VBox.setVgrow(colAufZu, Priority.ALWAYS);

        // Spalte 2: Lumos & Nox (66px breit für perfektes Text-Padding)
        this.btnLumos = new Button("Lumos");
        btnLumos.getStyleClass().add("module-small-btn");
        btnLumos.setPrefWidth(66);
        btnLumos.setMinWidth(60);
        btnLumos.setMaxWidth(72);
        btnLumos.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(btnLumos, Priority.ALWAYS);
        btnLumos.setOnAction(e -> dimmerSlider.setValue(100.0));

        this.btnNox = new Button("Nox");
        btnNox.getStyleClass().add("module-small-btn");
        btnNox.setPrefWidth(66);
        btnNox.setMinWidth(60);
        btnNox.setMaxWidth(72);
        btnNox.setPrefHeight(32);
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

        // Bündiger, luftiger Aufbau mit 8px Spacing über die 270px
        HBox body = new HBox(8, quickCol, dimmerCol, strobeCol, actionCol);
        body.setAlignment(Pos.CENTER);
        VBox.setVgrow(body, Priority.ALWAYS);

        getChildren().addAll(header, body);
    }

    private VBox createFaderCol(String labelText, Slider slider) {
        VBox col = new VBox(2);
        col.setAlignment(Pos.CENTER);
        col.setPrefWidth(54);
        col.setMinWidth(50);
        col.setMaxWidth(60);
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

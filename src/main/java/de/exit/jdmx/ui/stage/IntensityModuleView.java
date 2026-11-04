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
 * Kompaktes Steuerungsmodul für Intensität (DMXControl-Stil).
 * <p>
 * Scharfkantig (0px Radius auf der Modulkarte) mit 5er-Tastenleiste (VL, 70, 50, 30, 0),
 * 3 schlanken Fadern (Dimmer, Strobo, Shutter) und 2x2 Tastenblock (Auf, Zu, Lumos, Nox).
 * </p>
 */
public class IntensityModuleView extends VBox {

    private final String title = "Intensität";

    private final Slider dimmerSlider;
    private final Slider strobeSlider;
    private final Slider shutterSlider;

    private final List<Button> quickButtons = new ArrayList<>();
    private final Button btnAuf;
    private final Button btnZu;
    private final Button btnLumos;
    private final Button btnNox;

    public IntensityModuleView() {
        super(3);
        getStyleClass().add("control-module-card");
        setMinWidth(240.0);

        // 1. Schlichter Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("module-card-header");
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("module-card-title");
        header.getChildren().add(lblTitle);

        // 2. Fader initialisieren (Dimmer, Strobo, Shutter)
        this.dimmerSlider = new Slider(0, 100, 100);
        dimmerSlider.setOrientation(Orientation.VERTICAL);
        dimmerSlider.getStyleClass().add("fader-slider");
        VBox dimmerCol = createFaderCol("Dimmer", dimmerSlider);

        this.strobeSlider = new Slider(0, 25, 0);
        strobeSlider.setOrientation(Orientation.VERTICAL);
        strobeSlider.getStyleClass().add("fader-slider");
        VBox strobeCol = createFaderCol("Strobo", strobeSlider);

        this.shutterSlider = new Slider(0, 100, 100);
        shutterSlider.setOrientation(Orientation.VERTICAL);
        shutterSlider.getStyleClass().add("fader-slider");
        VBox shutterCol = createFaderCol("Shutter", shutterSlider);

        // 3. Inhalt zusammenbauen:
        // A. 5er Tastenleiste (VL, 70, 50, 30, 0)
        VBox quickBox = new VBox(2);
        quickBox.setAlignment(Pos.CENTER);
        double[] percentages = {100.0, 70.0, 50.0, 30.0, 0.0};
        String[] labels = {"VL", "70", "50", "30", "0"};

        for (int i = 0; i < percentages.length; i++) {
            final double pct = percentages[i];
            Button btn = new Button(labels[i]);
            btn.getStyleClass().add("module-quick-btn");
            btn.setPrefSize(32, 16);
            btn.setMinSize(32, 16);
            btn.setMaxSize(32, 16);
            btn.setOnAction(e -> dimmerSlider.setValue(pct));
            quickButtons.add(btn);
            quickBox.getChildren().add(btn);
        }

        // C. 2x2 Tastenblock (Auf, Zu, Lumos, Nox)
        GridPane btnGrid = new GridPane();
        btnGrid.setHgap(3);
        btnGrid.setVgap(3);
        btnGrid.setAlignment(Pos.CENTER);

        this.btnAuf = new Button("Auf");
        btnAuf.getStyleClass().add("module-small-btn");
        btnAuf.setPrefSize(38, 38);
        btnAuf.setOnAction(e -> dimmerSlider.setValue(100.0));

        this.btnLumos = new Button("Lumos");
        btnLumos.getStyleClass().add("module-small-btn");
        btnLumos.setPrefSize(44, 38);
        btnLumos.setOnAction(e -> dimmerSlider.setValue(100.0));

        this.btnZu = new Button("Zu");
        btnZu.getStyleClass().add("module-small-btn");
        btnZu.setPrefSize(38, 38);
        btnZu.setOnAction(e -> dimmerSlider.setValue(0.0));

        this.btnNox = new Button("Nox");
        btnNox.getStyleClass().add("module-small-btn");
        btnNox.setPrefSize(44, 38);
        btnNox.setOnAction(e -> dimmerSlider.setValue(0.0));

        btnGrid.add(btnAuf, 0, 0);
        btnGrid.add(btnLumos, 1, 0);
        btnGrid.add(btnZu, 0, 1);
        btnGrid.add(btnNox, 1, 1);

        Label lblIntensitaet = new Label("Intensität");
        lblIntensitaet.getStyleClass().add("fader-title-label");
        VBox gridCol = new VBox(2, btnGrid, lblIntensitaet);
        gridCol.setAlignment(Pos.CENTER);

        HBox body = new HBox(8, quickBox, dimmerCol, strobeCol, shutterCol, gridCol);
        body.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(body, Priority.ALWAYS);

        getChildren().addAll(header, body);
    }

    private VBox createFaderCol(String name, Slider slider) {
        VBox col = new VBox(2);
        col.setAlignment(Pos.CENTER);
        VBox.setVgrow(slider, Priority.ALWAYS);

        Label lbl = new Label(name);
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

    public Slider getShutterSlider() {
        return shutterSlider;
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

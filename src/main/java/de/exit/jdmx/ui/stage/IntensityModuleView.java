package de.exit.jdmx.ui.stage;

import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.theme.StudioTheme;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Steuerungsmodul für Intensität (Dimmer, Shutter/Strobo und Schnelltasten).
 * <p>
 * Bleibt als 24/7 Modul im unteren Dock permanent geöffnet und bietet
 * schnellen Direktzugriff auf Helligkeit und Strobo-Effekte für selektierte Geräte.
 * </p>
 */
public class IntensityModuleView extends VBox {

    private final String title = "INTENSITÄT";

    private final Slider dimmerSlider;
    private final Slider strobeSlider;
    private final List<Button> quickButtons = new ArrayList<>();
    private final Button btnLumos;
    private final Button btnNox;

    private final Label lblDimmerValue;
    private final Label lblStrobeValue;

    public IntensityModuleView() {
        super(8);
        getStyleClass().add("control-module-card");
        setMinWidth(200.0);

        // 1. Header (Icon + Titel)
        HBox header = createHeader();

        // 2. Body mit Quick-Buttons, Dimmer-Fader und Strobo-Fader
        this.dimmerSlider = new Slider(0, 100, 100);
        dimmerSlider.setOrientation(Orientation.VERTICAL);
        dimmerSlider.getStyleClass().add("fader-slider");
        VBox.setVgrow(dimmerSlider, Priority.ALWAYS);

        this.lblDimmerValue = new Label("100%");
        lblDimmerValue.getStyleClass().add("fader-value-label");
        dimmerSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            lblDimmerValue.setText(String.format("%.0f%%", newVal.doubleValue()));
        });

        this.strobeSlider = new Slider(0, 25, 0);
        strobeSlider.setOrientation(Orientation.VERTICAL);
        strobeSlider.getStyleClass().add("fader-slider");
        VBox.setVgrow(strobeSlider, Priority.ALWAYS);

        this.lblStrobeValue = new Label("0 Hz");
        lblStrobeValue.getStyleClass().add("fader-value-label");
        strobeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            lblStrobeValue.setText(String.format("%.0f Hz", newVal.doubleValue()));
        });

        // Quick-Prozent Buttons (100, 70, 50, 30, 0)
        VBox quickBox = new VBox(4);
        quickBox.setAlignment(Pos.CENTER);
        double[] percentages = {100.0, 70.0, 50.0, 30.0, 0.0};
        String[] labels = {"100%", "70%", "50%", "30%", "0%"};

        for (int i = 0; i < percentages.length; i++) {
            final double pct = percentages[i];
            Button btn = new Button(labels[i]);
            btn.getStyleClass().add("module-quick-btn");
            btn.setPrefWidth(52);
            btn.setOnAction(e -> dimmerSlider.setValue(pct));
            quickButtons.add(btn);
            quickBox.getChildren().add(btn);
        }

        // Fader-Spalte Dimmer
        VBox dimmerCol = new VBox(4, new Label("Dimmer"), dimmerSlider, lblDimmerValue);
        dimmerCol.setAlignment(Pos.CENTER);
        dimmerCol.getChildren().get(0).getStyleClass().add("fader-title-label");

        // Fader-Spalte Strobo
        VBox strobeCol = new VBox(4, new Label("Strobo"), strobeSlider, lblStrobeValue);
        strobeCol.setAlignment(Pos.CENTER);
        strobeCol.getChildren().get(0).getStyleClass().add("fader-title-label");

        HBox fadersRow = new HBox(12, quickBox, dimmerCol, strobeCol);
        fadersRow.setAlignment(Pos.CENTER);
        VBox.setVgrow(fadersRow, Priority.ALWAYS);

        // 3. Footer mit Lumos (100%) & Nox (0%) Tastern
        HBox actionRow = new HBox(8);
        actionRow.setAlignment(Pos.CENTER);

        this.btnLumos = new Button("Lumos");
        btnLumos.getStyleClass().add("module-action-btn-green");
        btnLumos.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnLumos, Priority.ALWAYS);
        btnLumos.setOnAction(e -> dimmerSlider.setValue(100.0));

        this.btnNox = new Button("Nox");
        btnNox.getStyleClass().add("module-action-btn-red");
        btnNox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnNox, Priority.ALWAYS);
        btnNox.setOnAction(e -> dimmerSlider.setValue(0.0));

        actionRow.getChildren().addAll(btnLumos, btnNox);

        getChildren().addAll(header, fadersRow, actionRow);
    }

    private HBox createHeader() {
        HBox header = new HBox(6);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("module-card-header");

        LucideIcon icon = new LucideIcon("sun", 14, Color.web("#F59E0B"));
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("module-card-title");

        header.getChildren().addAll(icon, lblTitle);
        return header;
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

    public Button getBtnLumos() {
        return btnLumos;
    }

    public Button getBtnNox() {
        return btnNox;
    }
}

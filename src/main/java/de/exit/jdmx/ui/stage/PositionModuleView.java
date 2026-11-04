package de.exit.jdmx.ui.stage;

import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

/**
 * Kompaktes Steuerungsmodul für Position (DMXControl-Stil).
 * <p>
 * Scharfkantig (0px Radius auf der Modulkarte) mit großem 2D XY-Pad (dynamisch bis ca. 0.5cm Rand),
 * Pan-Slider exakt zentriert und bündig darunter, Tilt-Slider bündig rechts daneben
 * sowie Home-Button im Header.
 * </p>
 */
public class PositionModuleView extends VBox {

    private final String title = "Position";

    private final Canvas xyPadCanvas;
    private final Slider panSlider;
    private final Slider tiltSlider;
    private final Button btnHome;

    public PositionModuleView() {
        super(2);
        getStyleClass().add("control-module-card");
        setMinWidth(150.0);
        setPrefWidth(185.0);

        // 1. Sliders & 2D XY-Pad (Standard: 96x96 px)
        this.xyPadCanvas = new Canvas(96, 96);
        xyPadCanvas.getStyleClass().add("xy-pad-canvas");

        this.panSlider = new Slider(0, 100, 50);
        panSlider.setPrefWidth(96);
        panSlider.setMaxWidth(96);
        panSlider.getStyleClass().add("fader-slider-horizontal");

        this.tiltSlider = new Slider(0, 100, 50);
        tiltSlider.setOrientation(Orientation.VERTICAL);
        tiltSlider.setPrefHeight(96);
        tiltSlider.setMaxHeight(96);
        tiltSlider.getStyleClass().add("fader-slider");

        xyPadCanvas.setOnMousePressed(this::handlePadDrag);
        xyPadCanvas.setOnMouseDragged(this::handlePadDrag);

        panSlider.valueProperty().addListener((obs, oldVal, newVal) -> drawXyPad());
        tiltSlider.valueProperty().addListener((obs, oldVal, newVal) -> drawXyPad());

        drawXyPad();

        // 2. Header (Titel + Home-Button rechts)
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("module-card-header");

        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("module-card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        this.btnHome = new Button("Home");
        btnHome.getStyleClass().add("module-small-btn");
        btnHome.setOnAction(e -> {
            panSlider.setValue(50.0);
            tiltSlider.setValue(50.0);
        });

        header.getChildren().addAll(lblTitle, spacer, btnHome);

        // 3. Geometrisch perfekt zentrierter Aufbau:
        // Pad und Pan-Slider in einer vertikalen Spalte (Pan exakt unter dem Pad!)
        VBox padAndPanCol = new VBox(2, xyPadCanvas, panSlider);
        padAndPanCol.setAlignment(Pos.CENTER);

        // Tilt-Slider bündig rechts neben dem Pad
        HBox padGroup = new HBox(4, padAndPanCol, tiltSlider);
        padGroup.setAlignment(Pos.CENTER);
        VBox.setVgrow(padGroup, Priority.ALWAYS);

        // Dynamisches Mitwachsen mit der Modulhöhe
        padGroup.heightProperty().addListener((obs, oldV, newV) -> {
            double h = newV.doubleValue();
            if (h > 50) {
                double size = Math.max(92.0, Math.min(125.0, h - 22.0));
                xyPadCanvas.setWidth(size);
                xyPadCanvas.setHeight(size);
                panSlider.setPrefWidth(size);
                panSlider.setMaxWidth(size);
                tiltSlider.setPrefHeight(size);
                tiltSlider.setMaxHeight(size);
                drawXyPad();
            }
        });

        HBox body = new HBox(padGroup);
        body.setAlignment(Pos.CENTER);
        VBox.setVgrow(body, Priority.ALWAYS);

        getChildren().addAll(header, body);
    }

    private void handlePadDrag(MouseEvent event) {
        double w = xyPadCanvas.getWidth();
        double h = xyPadCanvas.getHeight();

        double clampedX = Math.max(0, Math.min(w, event.getX()));
        double clampedY = Math.max(0, Math.min(h, event.getY()));

        double panPct = (clampedX / w) * 100.0;
        // Oben = 100%, Unten = 0%
        double tiltPct = (1.0 - (clampedY / h)) * 100.0;

        panSlider.setValue(panPct);
        tiltSlider.setValue(tiltPct);
    }

    private void drawXyPad() {
        GraphicsContext gc = xyPadCanvas.getGraphicsContext2D();
        double w = xyPadCanvas.getWidth();
        double h = xyPadCanvas.getHeight();

        gc.clearRect(0, 0, w, h);

        // Hintergrund (scharfkantig)
        gc.setFill(Color.web("#141414"));
        gc.fillRect(0, 0, w, h);

        // Rahmen
        gc.setStroke(Color.web("#2E2E2E"));
        gc.setLineWidth(1);
        gc.strokeRect(0, 0, w, h);

        // Fadenkreuz (Mittellinien)
        gc.setStroke(Color.web("#282828"));
        gc.strokeLine(w / 2.0, 0, w / 2.0, h);
        gc.strokeLine(0, h / 2.0, w, h / 2.0);

        // Marker Position
        double panVal = panSlider.getValue();
        double tiltVal = tiltSlider.getValue();

        double markerX = (panVal / 100.0) * w;
        double markerY = (1.0 - (tiltVal / 100.0)) * h;

        // Fadenkreuz-Leitlinien zum Marker
        gc.setStroke(Color.web("#3E3E3E"));
        gc.setLineDashes(2, 2);
        gc.strokeLine(markerX, 0, markerX, h);
        gc.strokeLine(0, markerY, w, markerY);
        gc.setLineDashes(null);

        // Cursor Dot (Punkt)
        gc.setFill(Color.web("#A855F7"));
        gc.fillOval(markerX - 4, markerY - 4, 8, 8);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1.2);
        gc.strokeOval(markerX - 4, markerY - 4, 8, 8);
    }

    public String getTitle() {
        return title;
    }

    public Canvas getXyPad() {
        return xyPadCanvas;
    }

    public Slider getPanSlider() {
        return panSlider;
    }

    public Slider getTiltSlider() {
        return tiltSlider;
    }

    public Button getBtnHome() {
        return btnHome;
    }
}

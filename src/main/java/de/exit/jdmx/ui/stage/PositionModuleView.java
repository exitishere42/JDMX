package de.exit.jdmx.ui.stage;

import de.exit.jdmx.ui.icon.LucideIcon;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

/**
 * Steuerungsmodul für Position (Pan/Tilt mit 2D XY-Pad, Schiebereglern und Invert-Optionen).
 */
public class PositionModuleView extends VBox {

    private final String title = "POSITION";

    private final Canvas xyPadCanvas;
    private final Slider panSlider;
    private final Slider tiltSlider;
    private final CheckBox cbInvertPan;
    private final CheckBox cbInvertTilt;
    private final Button btnHome;

    public PositionModuleView() {
        super(8);
        getStyleClass().add("control-module-card");
        setMinWidth(240.0);

        // 1. Header (Icon + Titel)
        HBox header = createHeader();

        // 2. Sliders & 2D XY-Pad
        this.panSlider = new Slider(0, 100, 50);
        panSlider.getStyleClass().add("fader-slider-horizontal");

        this.tiltSlider = new Slider(0, 100, 50);
        tiltSlider.setOrientation(Orientation.VERTICAL);
        tiltSlider.getStyleClass().add("fader-slider");

        this.xyPadCanvas = new Canvas(120, 100);
        xyPadCanvas.getStyleClass().add("xy-pad-canvas");

        xyPadCanvas.setOnMousePressed(this::handlePadDrag);
        xyPadCanvas.setOnMouseDragged(this::handlePadDrag);

        panSlider.valueProperty().addListener((obs, oldVal, newVal) -> drawXyPad());
        tiltSlider.valueProperty().addListener((obs, oldVal, newVal) -> drawXyPad());

        drawXyPad();

        // XY-Pad in der Mitte, Tilt rechts daneben
        HBox padWithTiltRow = new HBox(6, xyPadCanvas, tiltSlider);
        padWithTiltRow.setAlignment(Pos.CENTER);
        VBox.setVgrow(padWithTiltRow, Priority.ALWAYS);

        // Pan-Slider unter dem Pad
        VBox padBox = new VBox(4, padWithTiltRow, panSlider);
        padBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(padBox, Priority.ALWAYS);

        // 3. Footer: Invert-Checkboxen & Home-Button
        this.cbInvertPan = new CheckBox("Pan");
        cbInvertPan.getStyleClass().add("settings-checkbox");

        this.cbInvertTilt = new CheckBox("Tilt");
        cbInvertTilt.getStyleClass().add("settings-checkbox");

        this.btnHome = new Button("Home");
        btnHome.getStyleClass().add("module-action-btn-secondary");
        btnHome.setOnAction(e -> {
            panSlider.setValue(50.0);
            tiltSlider.setValue(50.0);
        });

        HBox optionsRow = new HBox(8, cbInvertPan, cbInvertTilt, btnHome);
        optionsRow.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(header, padBox, optionsRow);
    }

    private HBox createHeader() {
        HBox header = new HBox(6);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("module-card-header");

        LucideIcon icon = new LucideIcon("crosshair", 14, Color.web("#A855F7"));
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("module-card-title");

        header.getChildren().addAll(icon, lblTitle);
        return header;
    }

    private void handlePadDrag(MouseEvent event) {
        double w = xyPadCanvas.getWidth();
        double h = xyPadCanvas.getHeight();

        double clampedX = Math.max(0, Math.min(w, event.getX()));
        double clampedY = Math.max(0, Math.min(h, event.getY()));

        double panPct = (clampedX / w) * 100.0;
        // Invert Y so top is 100% and bottom is 0%
        double tiltPct = (1.0 - (clampedY / h)) * 100.0;

        panSlider.setValue(panPct);
        tiltSlider.setValue(tiltPct);
    }

    private void drawXyPad() {
        GraphicsContext gc = xyPadCanvas.getGraphicsContext2D();
        double w = xyPadCanvas.getWidth();
        double h = xyPadCanvas.getHeight();

        gc.clearRect(0, 0, w, h);

        // Hintergrund
        gc.setFill(Color.web("#141414"));
        gc.fillRoundRect(0, 0, w, h, 6, 6);

        // Rahmen
        gc.setStroke(Color.web("#2E2E2E"));
        gc.setLineWidth(1);
        gc.strokeRoundRect(0, 0, w, h, 6, 6);

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

        // Cursor Dot
        gc.setFill(Color.web("#A855F7"));
        gc.fillOval(markerX - 5, markerY - 5, 10, 10);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1.5);
        gc.strokeOval(markerX - 5, markerY - 5, 10, 10);
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

    public CheckBox getCbInvertPan() {
        return cbInvertPan;
    }

    public CheckBox getCbInvertTilt() {
        return cbInvertTilt;
    }

    public Button getBtnHome() {
        return btnHome;
    }
}

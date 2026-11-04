package de.exit.jdmx.ui.stage;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

/**
 * Kompaktes Steuerungsmodul für Farbe (DMXControl-Stil).
 * <p>
 * Scharfkantig (0px Radius auf der Modulkarte) mit HSV-Reiter und
 * reinem, kompaktem HSV-Farbkreis ohne Schieberegler gemäß Nutzerwunsch.
 * </p>
 */
public class ColorModuleView extends VBox {

    private final String title = "Farbe";

    private final ObjectProperty<Color> selectedColor = new SimpleObjectProperty<>(Color.WHITE);
    private final Canvas colorWheelCanvas;

    private double currentHue = 0.0;
    private double currentSat = 0.0;

    public ColorModuleView() {
        super(3);
        getStyleClass().add("control-module-card");
        setMinWidth(140.0);

        // 1. Schlichter Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("module-card-header");
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("module-card-title");
        header.getChildren().add(lblTitle);

        // 2. HSV-Tab-Reiter
        HBox tabRow = new HBox();
        tabRow.setAlignment(Pos.CENTER_LEFT);
        Label lblTab = new Label("HSV");
        lblTab.getStyleClass().add("module-inner-tab-label");
        tabRow.getChildren().add(lblTab);

        // 3. Purer HSV-Farbkreis
        this.colorWheelCanvas = new Canvas(84, 84);
        StackPane wheelContainer = new StackPane(colorWheelCanvas);
        wheelContainer.setAlignment(Pos.CENTER);
        VBox.setVgrow(wheelContainer, Priority.ALWAYS);

        colorWheelCanvas.setOnMousePressed(this::handleColorWheelClick);
        colorWheelCanvas.setOnMouseDragged(this::handleColorWheelClick);

        drawColorWheel();

        getChildren().addAll(header, tabRow, wheelContainer);
    }

    private void handleColorWheelClick(MouseEvent event) {
        double w = colorWheelCanvas.getWidth();
        double h = colorWheelCanvas.getHeight();
        double cx = w / 2.0;
        double cy = h / 2.0;
        double radius = Math.min(cx, cy) - 3.0;

        double dx = event.getX() - cx;
        double dy = event.getY() - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);

        double sat = Math.min(1.0, dist / radius);
        double angle = Math.toDegrees(Math.atan2(dy, dx));
        if (angle < 0) {
            angle += 360.0;
        }

        this.currentHue = angle;
        this.currentSat = sat;

        Color newColor = Color.hsb(currentHue, currentSat, 1.0);
        this.selectedColor.set(newColor);
        drawColorWheel();
    }

    public void selectColor(Color color) {
        if (color == null) {
            return;
        }
        this.selectedColor.set(color);
        this.currentHue = color.getHue();
        this.currentSat = color.getSaturation();
        drawColorWheel();
    }

    private void drawColorWheel() {
        GraphicsContext gc = colorWheelCanvas.getGraphicsContext2D();
        double w = colorWheelCanvas.getWidth();
        double h = colorWheelCanvas.getHeight();
        gc.clearRect(0, 0, w, h);

        double cx = w / 2.0;
        double cy = h / 2.0;
        double radius = Math.min(cx, cy) - 3.0;

        // HSV Farbkreis zeichnen
        for (int r = (int) radius; r > 0; r -= 2) {
            double sat = (double) r / radius;
            for (int a = 0; a < 360; a += 6) {
                double rad = Math.toRadians(a);
                double x1 = cx + (r - 2) * Math.cos(rad);
                double y1 = cy + (r - 2) * Math.sin(rad);
                double x2 = cx + r * Math.cos(rad);
                double y2 = cy + r * Math.sin(rad);

                gc.setStroke(Color.hsb(a, sat, 1.0));
                gc.setLineWidth(2.5);
                gc.strokeLine(x1, y1, x2, y2);
            }
        }

        // Weißer Mittelpunkt
        gc.setFill(Color.WHITE);
        gc.fillOval(cx - 2, cy - 2, 4, 4);

        // Randkreis
        gc.setStroke(Color.web("#383838"));
        gc.setLineWidth(1);
        gc.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);

        // Marker-Punkt
        double rad = Math.toRadians(currentHue);
        double markerX = cx + (currentSat * radius) * Math.cos(rad);
        double markerY = cy + (currentSat * radius) * Math.sin(rad);

        gc.setFill(selectedColor.get());
        gc.fillOval(markerX - 4, markerY - 4, 8, 8);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1.5);
        gc.strokeOval(markerX - 4, markerY - 4, 8, 8);
    }

    public String getTitle() {
        return title;
    }

    public Canvas getColorWheel() {
        return colorWheelCanvas;
    }

    public Color getSelectedColor() {
        return selectedColor.get();
    }

    public ObjectProperty<Color> selectedColorProperty() {
        return selectedColor;
    }
}

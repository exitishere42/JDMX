package de.exit.jdmx.ui.stage;

import de.exit.jdmx.ui.icon.LucideIcon;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Steuerungsmodul für Farbe (HSV-Farbkreis und Schnell-Farbpaletten).
 */
public class ColorModuleView extends VBox {

    private final String title = "FARBE";

    private final ObjectProperty<Color> selectedColor = new SimpleObjectProperty<>(Color.WHITE);
    private final Canvas colorWheelCanvas;
    private final List<Button> colorPresets = new ArrayList<>();

    // Aktueller Picker-Punkt (Winkel 0-360, Distanz 0-1)
    private double currentHue = 0.0;
    private double currentSat = 0.0;

    public ColorModuleView() {
        super(8);
        getStyleClass().add("control-module-card");
        setMinWidth(220.0);

        // 1. Header (Icon + Titel)
        HBox header = createHeader();

        // 2. HSV-Farbkreis (Canvas)
        this.colorWheelCanvas = new Canvas(130, 130);
        StackPane wheelContainer = new StackPane(colorWheelCanvas);
        wheelContainer.setAlignment(Pos.CENTER);
        VBox.setVgrow(wheelContainer, Priority.ALWAYS);

        colorWheelCanvas.setOnMousePressed(this::handleColorWheelClick);
        colorWheelCanvas.setOnMouseDragged(this::handleColorWheelClick);

        drawColorWheel();

        // 3. Schnell-Farbpaletten (Kaltweiß, Warmweiß, Rot, Grün, Blau, Amber, Cyan, Magenta)
        FlowPane presetGrid = createPresetGrid();

        getChildren().addAll(header, wheelContainer, presetGrid);
    }

    private HBox createHeader() {
        HBox header = new HBox(6);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("module-card-header");

        LucideIcon icon = new LucideIcon("palette", 14, Color.web("#2DD4BF"));
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("module-card-title");

        header.getChildren().addAll(icon, lblTitle);
        return header;
    }

    private FlowPane createPresetGrid() {
        FlowPane grid = new FlowPane(4, 4);
        grid.setAlignment(Pos.CENTER);

        ColorPreset[] presets = {
                new ColorPreset("Kaltweiß", Color.WHITE),
                new ColorPreset("Warmweiß", Color.web("#FFF3D6")),
                new ColorPreset("Rot", Color.web("#EF4444")),
                new ColorPreset("Grün", Color.web("#10B981")),
                new ColorPreset("Blau", Color.web("#3B82F6")),
                new ColorPreset("Amber", Color.web("#F59E0B")),
                new ColorPreset("Cyan", Color.web("#06B6D4")),
                new ColorPreset("Magenta", Color.web("#D946EF"))
        };

        for (ColorPreset cp : presets) {
            Button btn = new Button();
            btn.getStyleClass().add("color-chip-btn");
            btn.setPrefSize(22, 22);
            btn.setMinSize(22, 22);
            btn.setMaxSize(22, 22);

            String hex = String.format("#%02X%02X%02X",
                    (int) (cp.color.getRed() * 255),
                    (int) (cp.color.getGreen() * 255),
                    (int) (cp.color.getBlue() * 255));
            btn.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: 4px; -fx-cursor: hand;", hex));
            btn.setTooltip(new Tooltip(cp.name));
            btn.setOnAction(e -> selectColor(cp.color));

            colorPresets.add(btn);
            grid.getChildren().add(btn);
        }

        return grid;
    }

    private void handleColorWheelClick(MouseEvent event) {
        double w = colorWheelCanvas.getWidth();
        double h = colorWheelCanvas.getHeight();
        double cx = w / 2.0;
        double cy = h / 2.0;
        double radius = Math.min(cx, cy) - 4.0;

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
        double radius = Math.min(cx, cy) - 5.0;

        // HSV-Farbkreis zeichnen
        for (int r = (int) radius; r > 0; r -= 2) {
            double sat = (double) r / radius;
            for (int a = 0; a < 360; a += 4) {
                double rad = Math.toRadians(a);
                double x1 = cx + (r - 2) * Math.cos(rad);
                double y1 = cy + (r - 2) * Math.sin(rad);
                double x2 = cx + r * Math.cos(rad);
                double y2 = cy + r * Math.sin(rad);

                gc.setStroke(Color.hsb(a, sat, 1.0));
                gc.setLineWidth(3);
                gc.strokeLine(x1, y1, x2, y2);
            }
        }

        // Weißer Mittelpunkt
        gc.setFill(Color.WHITE);
        gc.fillOval(cx - 3, cy - 3, 6, 6);

        // Randkreis
        gc.setStroke(Color.web("#383838"));
        gc.setLineWidth(1.5);
        gc.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);

        // Aktueller Picker-Punkt (Marker)
        double rad = Math.toRadians(currentHue);
        double markerX = cx + (currentSat * radius) * Math.cos(rad);
        double markerY = cy + (currentSat * radius) * Math.sin(rad);

        gc.setFill(selectedColor.get());
        gc.fillOval(markerX - 6, markerY - 6, 12, 12);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeOval(markerX - 6, markerY - 6, 12, 12);
    }

    public record ColorPreset(String name, Color color) {}

    public String getTitle() {
        return title;
    }

    public Canvas getColorWheel() {
        return colorWheelCanvas;
    }

    public List<Button> getColorPresets() {
        return colorPresets;
    }

    public Color getSelectedColor() {
        return selectedColor.get();
    }

    public ObjectProperty<Color> selectedColorProperty() {
        return selectedColor;
    }
}

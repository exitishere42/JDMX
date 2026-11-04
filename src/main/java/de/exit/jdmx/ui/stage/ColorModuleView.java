package de.exit.jdmx.ui.stage;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Kompaktes Steuerungsmodul für Farbe (DMXControl-Stil).
 * <p>
 * Scharfkantig (0px Radius auf der Modulkarte) mit großem HSV-Farbkreis
 * (nahezu bündig bis zum Rand ohne störende Schieberegler oder HSV-Labels)
 * sowie je 4 anpassbaren Schnellwahl-/Preset-Buttons links und rechts vom Rad.
 * </p>
 */
public class ColorModuleView extends VBox {

    private final String title = "Farbe";

    private final ObjectProperty<Color> selectedColor = new SimpleObjectProperty<>(Color.WHITE);
    private final Canvas colorWheelCanvas;

    private final List<Button> leftPresetButtons = new ArrayList<>();
    private final List<Color> leftColors = new ArrayList<>();

    private final List<Button> rightPresetButtons = new ArrayList<>();
    private final List<Color> rightColors = new ArrayList<>();

    private double currentHue = 0.0;
    private double currentSat = 0.0;

    public ColorModuleView() {
        super(2);
        getStyleClass().add("control-module-card");
        setMinWidth(170.0);
        setPrefWidth(210.0);

        // 1. Schlichter Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("module-card-header");
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("module-card-title");
        header.getChildren().add(lblTitle);

        // 2. Großer HSV-Farbkreis (98x98 px - bis ca. 0.5cm an den Rand)
        this.colorWheelCanvas = new Canvas(98, 98);
        colorWheelCanvas.setOnMousePressed(this::handleColorWheelClick);
        colorWheelCanvas.setOnMouseDragged(this::handleColorWheelClick);

        StackPane wheelContainer = new StackPane(colorWheelCanvas);
        wheelContainer.setAlignment(Pos.CENTER);
        VBox.setVgrow(wheelContainer, Priority.ALWAYS);

        // 3. Linke 4 Preset-Buttons
        Color[] defaultLeft = {
                Color.web("#FFFFFF"), // Weiß
                Color.web("#FFF0D4"), // Warmweiß
                Color.web("#EF4444"), // Rot
                Color.web("#F59E0B")  // Gelb
        };
        VBox leftBox = new VBox(3);
        leftBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(leftBox, Priority.ALWAYS);

        for (int i = 0; i < defaultLeft.length; i++) {
            final int index = i;
            leftColors.add(defaultLeft[i]);
            Button btn = createPresetButton(defaultLeft[i], () -> selectColor(leftColors.get(index)), () -> {
                setLeftPresetColor(index, selectedColor.get());
            });
            leftPresetButtons.add(btn);
            leftBox.getChildren().add(btn);
        }

        // 4. Rechte 4 Preset-Buttons
        Color[] defaultRight = {
                Color.web("#10B981"), // Grün
                Color.web("#06B6D4"), // Cyan
                Color.web("#3B82F6"), // Blau
                Color.web("#EC4899")  // Magenta
        };
        VBox rightBox = new VBox(3);
        rightBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(rightBox, Priority.ALWAYS);

        for (int i = 0; i < defaultRight.length; i++) {
            final int index = i;
            rightColors.add(defaultRight[i]);
            Button btn = createPresetButton(defaultRight[i], () -> selectColor(rightColors.get(index)), () -> {
                setRightPresetColor(index, selectedColor.get());
            });
            rightPresetButtons.add(btn);
            rightBox.getChildren().add(btn);
        }

        // Farbkreis mit je ca. 10px Abstand zu den Buttons links und rechts
        HBox body = new HBox(8, leftBox, wheelContainer, rightBox);
        body.setAlignment(Pos.CENTER);
        VBox.setVgrow(body, Priority.ALWAYS);

        drawColorWheel();

        getChildren().addAll(header, body);
    }

    private Button createPresetButton(Color initialColor, Runnable onClick, Runnable onSaveCurrentColor) {
        Button btn = new Button();
        btn.getStyleClass().add("color-chip-btn");
        btn.setPrefSize(22, 20);
        btn.setMinSize(18, 16);
        btn.setMaxSize(26, 24);
        VBox.setVgrow(btn, Priority.ALWAYS);
        applyColorToButton(btn, initialColor);

        Tooltip tip = new Tooltip("Klick: Farbe wählen | Rechtsklick: Aktuelle Farbe speichern");
        Tooltip.install(btn, tip);

        btn.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                onClick.run();
            } else if (e.getButton() == MouseButton.SECONDARY) {
                onSaveCurrentColor.run();
            }
        });

        // Kontextmenü für Rechtsklick als Alternative
        ContextMenu contextMenu = new ContextMenu();
        MenuItem itemSave = new MenuItem("Aktuelle Farbe speichern");
        itemSave.setOnAction(e -> onSaveCurrentColor.run());
        contextMenu.getItems().add(itemSave);
        btn.setContextMenu(contextMenu);

        return btn;
    }

    private void applyColorToButton(Button btn, Color color) {
        String hex = toHex(color);
        btn.setStyle("-fx-background-color: " + hex + "; -fx-border-color: #333333; -fx-border-radius: 3px; -fx-background-radius: 3px;");
    }

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255));
    }

    private void handleColorWheelClick(MouseEvent event) {
        double w = colorWheelCanvas.getWidth();
        double h = colorWheelCanvas.getHeight();
        double cx = w / 2.0;
        double cy = h / 2.0;
        double radius = Math.min(cx, cy) - 2.0;

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
        double cx = w / 2.0;
        double cy = h / 2.0;
        double radius = Math.min(cx, cy) - 2.0;

        gc.clearRect(0, 0, w, h);

        int steps = 180;
        double angleStep = 360.0 / steps;

        for (int i = 0; i < steps; i++) {
            double angle = i * angleStep;
            double startAngle = angle - 0.5;
            double sweep = angleStep + 1.0;

            Color edgeColor = Color.hsb(angle, 1.0, 1.0);
            gc.setFill(edgeColor);
            gc.fillArc(cx - radius, cy - radius, radius * 2, radius * 2, -startAngle, -sweep, javafx.scene.shape.ArcType.ROUND);
        }

        // Weiß-Verlauf zur Mitte
        javafx.scene.paint.RadialGradient centerWhite = new javafx.scene.paint.RadialGradient(
                0, 0, cx, cy, radius, false, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0.0, Color.WHITE),
                new javafx.scene.paint.Stop(0.2, Color.rgb(255, 255, 255, 0.9)),
                new javafx.scene.paint.Stop(0.8, Color.rgb(255, 255, 255, 0.1)),
                new javafx.scene.paint.Stop(1.0, Color.TRANSPARENT)
        );
        gc.setFill(centerWhite);
        gc.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);

        // Subtiler Rahmen
        gc.setStroke(Color.web("#383838"));
        gc.setLineWidth(1.2);
        gc.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);

        // Marker Position
        double radAngle = Math.toRadians(currentHue);
        double dist = currentSat * radius;
        double markerX = cx + Math.cos(radAngle) * dist;
        double markerY = cy + Math.sin(radAngle) * dist;

        gc.setFill(selectedColor.get());
        gc.fillOval(markerX - 4, markerY - 4, 8, 8);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1.5);
        gc.strokeOval(markerX - 4, markerY - 4, 8, 8);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.8);
        gc.strokeOval(markerX - 5, markerY - 5, 10, 10);
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

    public List<Button> getLeftPresetButtons() {
        return leftPresetButtons;
    }

    public List<Button> getRightPresetButtons() {
        return rightPresetButtons;
    }

    public Color getLeftPresetColor(int index) {
        if (index >= 0 && index < leftColors.size()) {
            return leftColors.get(index);
        }
        return Color.WHITE;
    }

    public void setLeftPresetColor(int index, Color color) {
        if (index >= 0 && index < leftColors.size() && color != null) {
            leftColors.set(index, color);
            applyColorToButton(leftPresetButtons.get(index), color);
        }
    }

    public Color getRightPresetColor(int index) {
        if (index >= 0 && index < rightColors.size()) {
            return rightColors.get(index);
        }
        return Color.WHITE;
    }

    public void setRightPresetColor(int index, Color color) {
        if (index >= 0 && index < rightColors.size() && color != null) {
            rightColors.set(index, color);
            applyColorToButton(rightPresetButtons.get(index), color);
        }
    }
}

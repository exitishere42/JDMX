package de.exit.jdmx.ui.stage;

import de.exit.jdmx.core.i18n.I18n;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
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
 * (reicht bis auf ca. 0.5cm an den Rand) und je 4 anpassbaren Farb-Buttons
 * links und rechts mit 1cm Abstand vom Rand.
 * </p>
 */
public class ColorModuleView extends VBox {

    private final Label lblTitle;

    private final ObjectProperty<Color> selectedColor = new SimpleObjectProperty<>(Color.WHITE);
    private final Canvas colorWheelCanvas;

    private final List<Button> leftPresetButtons = new ArrayList<>();
    private final List<Color> leftColors = new ArrayList<>();

    private final List<Button> rightPresetButtons = new ArrayList<>();
    private final List<Color> rightColors = new ArrayList<>();

    private final List<MenuItem> saveMenuItems = new ArrayList<>();

    private double currentHue = 0.0;
    private double currentSat = 0.0;

    public ColorModuleView() {
        super(2);
        getStyleClass().add("control-module-card");
        setMinWidth(170.0);
        setPrefWidth(220.0);

        // 1. Schlichter Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("module-card-header");
        this.lblTitle = new Label(I18n.get("control.color"));
        lblTitle.getStyleClass().add("module-card-title");
        header.getChildren().add(lblTitle);

        // 2. Großer HSV-Farbkreis (Standard 104x104 px, passt sich dynamisch an)
        this.colorWheelCanvas = new Canvas(104, 104);
        colorWheelCanvas.setOnMousePressed(this::handleColorWheelClick);
        colorWheelCanvas.setOnMouseDragged(this::handleColorWheelClick);

        StackPane wheelContainer = new StackPane(colorWheelCanvas);
        wheelContainer.setAlignment(Pos.CENTER);
        VBox.setVgrow(wheelContainer, Priority.ALWAYS);
        HBox.setHgrow(wheelContainer, Priority.ALWAYS);

        // Dynamisches Mitwachsen mit der Modulhöhe (bis auf ca. 5mm an den Rand)
        wheelContainer.heightProperty().addListener((obs, oldV, newV) -> {
            double h = newV.doubleValue();
            if (h > 50) {
                double size = Math.max(96.0, Math.min(130.0, h - 8.0));
                colorWheelCanvas.setWidth(size);
                colorWheelCanvas.setHeight(size);
                drawColorWheel();
            }
        });

        // 3. Linke 4 Preset-Buttons (ca. 1cm vom Rand entfernt)
        Color[] defaultLeft = {
                Color.web("#FFFFFF"), // Weiß
                Color.web("#000000"), // Schwarz
                Color.web("#EF4444"), // Rot
                Color.web("#10B981")  // Grün
        };
        VBox leftBox = new VBox(4);
        leftBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(leftBox, Priority.ALWAYS);
        HBox.setMargin(leftBox, new Insets(0, 0, 0, 14)); // ca. 1cm vom linken Rand

        for (int i = 0; i < defaultLeft.length; i++) {
            final int index = i;
            leftColors.add(defaultLeft[i]);
            Button btn = createPresetButton(defaultLeft[i], () -> selectColor(leftColors.get(index)), () -> {
                setLeftPresetColor(index, selectedColor.get());
            });
            leftPresetButtons.add(btn);
            leftBox.getChildren().add(btn);
        }

        // 4. Rechte 4 Preset-Buttons (ca. 1cm vom Rand entfernt)
        Color[] defaultRight = {
                Color.web("#3B82F6"), // Blau
                null,                 // Leer
                null,                 // Leer
                null                  // Leer
        };
        VBox rightBox = new VBox(4);
        rightBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(rightBox, Priority.ALWAYS);
        HBox.setMargin(rightBox, new Insets(0, 14, 0, 0)); // ca. 1cm vom rechten Rand

        for (int i = 0; i < defaultRight.length; i++) {
            final int index = i;
            rightColors.add(defaultRight[i]);
            Button btn = createPresetButton(defaultRight[i], () -> selectColor(rightColors.get(index)), () -> {
                setRightPresetColor(index, selectedColor.get());
            });
            rightPresetButtons.add(btn);
            rightBox.getChildren().add(btn);
        }

        HBox body = new HBox(8, leftBox, wheelContainer, rightBox);
        body.setAlignment(Pos.CENTER);
        VBox.setVgrow(body, Priority.ALWAYS);

        drawColorWheel();

        getChildren().addAll(header, body);

        // Dynamische Mehrsprachigkeit
        I18n.addLanguageChangeListener(lang -> {
            if (Platform.isFxApplicationThread()) {
                updateLocalization();
            } else {
                Platform.runLater(this::updateLocalization);
            }
        });
    }

    private Button createPresetButton(Color initialColor, Runnable onClick, Runnable onSaveCurrentColor) {
        Button btn = new Button();
        btn.getStyleClass().add("color-chip-btn");
        btn.setPrefSize(22, 20);
        btn.setMinSize(20, 18);
        btn.setMaxSize(26, 24);
        VBox.setVgrow(btn, Priority.ALWAYS);
        applyColorToButton(btn, initialColor);

        Tooltip tip = new Tooltip(initialColor == null
                ? I18n.get("control.color_empty_slot_tip")
                : I18n.get("control.color_slot_tip"));
        btn.setTooltip(tip);

        btn.setOnAction(e -> onClick.run());

        btn.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                onSaveCurrentColor.run();
            }
        });

        ContextMenu contextMenu = new ContextMenu();
        MenuItem itemSave = new MenuItem(I18n.get("control.save_color"));
        itemSave.setOnAction(e -> onSaveCurrentColor.run());
        contextMenu.getItems().add(itemSave);
        saveMenuItems.add(itemSave);
        btn.setContextMenu(contextMenu);

        return btn;
    }

    private void applyColorToButton(Button btn, Color color) {
        if (color == null) {
            btn.setStyle("-fx-background-color: #242427; -fx-border-color: #38383C; -fx-border-style: dashed; -fx-border-radius: 3px; -fx-background-radius: 3px;");
        } else {
            String hex = toHex(color);
            btn.setStyle("-fx-background-color: " + hex + "; -fx-border-color: #383838; -fx-border-style: solid; -fx-border-radius: 3px; -fx-background-radius: 3px;");
        }
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

    /**
     * Setzt die aktive Farbe des Farbmoduls (Alias für {@link #selectColor(Color)}).
     *
     * @param color Die auszuwählende Farbe
     */
    public void setColor(Color color) {
        selectColor(color);
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
        return I18n.get("control.color");
    }

    /**
     * Aktualisiert alle lokalisierten Texte bei einem Sprachwechsel.
     */
    public void updateLocalization() {
        lblTitle.setText(I18n.get("control.color"));
        for (MenuItem item : saveMenuItems) {
            item.setText(I18n.get("control.save_color"));
        }
        for (int i = 0; i < leftPresetButtons.size(); i++) {
            Button btn = leftPresetButtons.get(i);
            if (btn.getTooltip() != null) {
                Color c = (i < leftColors.size()) ? leftColors.get(i) : null;
                btn.getTooltip().setText(c == null
                        ? I18n.get("control.color_empty_slot_tip")
                        : I18n.get("control.color_slot_tip"));
            }
        }
        for (int i = 0; i < rightPresetButtons.size(); i++) {
            Button btn = rightPresetButtons.get(i);
            if (btn.getTooltip() != null) {
                Color c = (i < rightColors.size()) ? rightColors.get(i) : null;
                btn.getTooltip().setText(c == null
                        ? I18n.get("control.color_empty_slot_tip")
                        : I18n.get("control.color_slot_tip"));
            }
        }
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
        return null;
    }

    public void setRightPresetColor(int index, Color color) {
        if (index >= 0 && index < rightColors.size() && color != null) {
            rightColors.set(index, color);
            applyColorToButton(rightPresetButtons.get(index), color);
        }
    }
}

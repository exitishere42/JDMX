package de.exit.jdmx.ui.timeline;

import de.exit.jdmx.ui.icon.LucideIcon;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Kaskadierendes 2. Dropdown-Untermenü (Flyout) zur schnellen Farbauswahl für Timeline-Clips.
 * <p>
 * Öffnet sich nahtlos rechts neben dem Kontextmenü-Eintrag "Farbe".
 * Zeigt die 6 kuratierten Studio-Farben in einem kompakten 2x3-Palette-Raster
 * mit klaren Kanten und ohne störenden Glow-Effekt.
 * </p>
 * <p>
 * <b>Thread-Sicherheit:</b> Alle Modifikationen und Callbacks laufen auf dem JavaFX-Thread.
 * </p>
 */
public class TimelineColorFlyout extends Popup {

    /** Die 6 kuratierten Studio-Standardfarben für Szenen-Clips. */
    public static final List<String> STANDARD_COLORS = List.of(
            "#FACC15", // 1. Daslight Gelb (Klassiker)
            "#38BDF8", // 2. Cyan / Himmelblau
            "#10B981", // 3. Smaragdgrün
            "#EF4444", // 4. Signalrot
            "#8B5CF6", // 5. Violett / Lila
            "#EC4899"  // 6. Pink / Magenta
    );

    private final VBox root;
    private final List<Button> colorSwatches = new ArrayList<>();
    private Consumer<String> onColorSelected;
    private Runnable onDismiss;
    private String activeColorHex = "#FACC15";

    /**
     * Erstellt ein neues kaskadierendes Farb-Flyout.
     */
    public TimelineColorFlyout() {
        setAutoHide(true);
        setHideOnEscape(true);

        setOnShowing(e -> enforceTransparentBackground());

        this.root = new VBox(0);
        root.getStyleClass().add("action-menu-popup");
        root.setPadding(new Insets(6));

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(6);
        grid.setVgap(6);

        for (int i = 0; i < STANDARD_COLORS.size(); i++) {
            String hex = STANDARD_COLORS.get(i);
            Button swatch = createSwatch(hex);
            colorSwatches.add(swatch);

            int col = i % 3;
            int row = i / 3;
            grid.add(swatch, col, row);
        }

        root.getChildren().add(grid);
        getContent().add(root);

        updateActiveSwatches();
    }

    private Button createSwatch(String hex) {
        Button btn = new Button();
        btn.setMinSize(32, 32);
        btn.setPrefSize(32, 32);
        btn.setMaxSize(32, 32);
        btn.setCursor(Cursor.HAND);

        boolean isBright = isBrightColor(hex);
        Color checkColor = isBright ? Color.web("#000000") : Color.web("#FFFFFF");

        String normalBorder = "rgba(255, 255, 255, 0.20)";
        String hoverBorder = "#FFFFFF";

        btn.setStyle(computeSwatchStyle(hex, normalBorder, false));

        // Schlichter, präziser Hover-Effekt: Weißer Rand OHNE diffusen Glow-Schatten
        btn.setOnMouseEntered(e -> {
            boolean isActive = hex.equalsIgnoreCase(activeColorHex);
            btn.setStyle(computeSwatchStyle(hex, hoverBorder, isActive));
        });

        btn.setOnMouseExited(e -> {
            boolean isActive = hex.equalsIgnoreCase(activeColorHex);
            btn.setStyle(computeSwatchStyle(hex, isActive ? "#FFFFFF" : normalBorder, isActive));
        });

        btn.setOnAction(e -> {
            safeHide();
            if (onColorSelected != null) {
                onColorSelected.accept(hex);
            }
            if (onDismiss != null) {
                onDismiss.run();
            }
        });

        return btn;
    }

    private String computeSwatchStyle(String hex, String borderColor, boolean isActive) {
        return "-fx-background-color: " + hex + "; " +
                "-fx-background-radius: 4px; " +
                "-fx-border-color: " + borderColor + "; " +
                "-fx-border-width: " + (isActive ? "2px" : "1px") + "; " +
                "-fx-border-radius: 4px; " +
                "-fx-focus-color: transparent; " +
                "-fx-faint-focus-color: transparent;";
    }

    private void updateActiveSwatches() {
        for (int i = 0; i < STANDARD_COLORS.size(); i++) {
            String hex = STANDARD_COLORS.get(i);
            Button btn = colorSwatches.get(i);
            boolean isActive = hex.equalsIgnoreCase(activeColorHex);

            boolean isBright = isBrightColor(hex);
            Color checkColor = isBright ? Color.web("#000000") : Color.web("#FFFFFF");

            if (isActive) {
                LucideIcon check = new LucideIcon("check", 14, checkColor);
                btn.setGraphic(check);
                btn.setStyle(computeSwatchStyle(hex, "#FFFFFF", true));
            } else {
                btn.setGraphic(null);
                btn.setStyle(computeSwatchStyle(hex, "rgba(255, 255, 255, 0.20)", false));
            }
        }
    }

    /**
     * Liefert den Haupt-Container des Flyouts.
     *
     * @return die {@link VBox}-Wurzel
     */
    public VBox getMenuContainer() {
        return root;
    }

    /**
     * Ermittelt anhand der WCAG-Relativluminanz, ob eine Farbe hell ist.
     * <p>
     * Helle Farben (wie Gelb oder Cyan) erfordern schwarzen Text für optimalen Kontrast,
     * dunklere/kräftigere Farben (wie Rot, Violett oder Pink) erfordern weißen Text.
     * </p>
     *
     * @param hex Hex-Farbcode (z. B. "#FACC15")
     * @return {@code true}, wenn hell (Luminanz > 0.35), sonst {@code false}
     */
    public static boolean isBrightColor(String hex) {
        if (hex == null || hex.length() < 7) {
            return true;
        }
        try {
            Color c = Color.web(hex);
            double r = linearize(c.getRed());
            double g = linearize(c.getGreen());
            double b = linearize(c.getBlue());
            double luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b;
            return luminance > 0.35;
        } catch (Exception e) {
            return true;
        }
    }

    private static double linearize(double val) {
        return (val <= 0.04045) ? val / 12.92 : Math.pow((val + 0.055) / 1.055, 2.4);
    }

    /**
     * Setzt die aktuell aktive Farbe, um das Häkchen entsprechend zu setzen.
     *
     * @param hex Hex-Farbcode (z. B. "#38BDF8")
     */
    public void setActiveColor(String hex) {
        this.activeColorHex = hex != null ? hex : "#FACC15";
        updateActiveSwatches();
    }

    /**
     * Schließt das Flyout sicher auf dem JavaFX-Thread.
     */
    public void safeHide() {
        if (isShowing()) {
            if (Platform.isFxApplicationThread()) {
                hide();
            } else {
                Platform.runLater(this::hide);
            }
        }
    }

    /**
     * Liefert die Liste aller Farbkachel-Buttons für Tests.
     *
     * @return unmodifizierbare Liste der {@link Button}-Swatches
     */
    public List<Button> getColorSwatches() {
        return Collections.unmodifiableList(colorSwatches);
    }

    /**
     * Registriert den Callback für eine Farbauswahl.
     *
     * @param onColorSelected Consumer mit Hex-Code
     */
    public void setOnColorSelected(Consumer<String> onColorSelected) {
        this.onColorSelected = onColorSelected;
    }

    /**
     * Registriert einen Callback, der ausgeführt wird, wenn eine Auswahl getroffen wurde
     * (zum Schließen des übergeordneten Hauptmenüs).
     *
     * @param onDismiss Dismiss-Runnable
     */
    public void setOnDismiss(Runnable onDismiss) {
        this.onDismiss = onDismiss;
    }

    /**
     * Öffnet das Flyout-Untermenü bündig an den angegebenen Bildschirmkoordinaten.
     *
     * @param window        Übergeordnetes Fenster
     * @param anchorScreenX Ziel-X-Koordinate auf dem Bildschirm (sichtbarer rechter Rand des Hauptmenüs)
     * @param anchorScreenY Ziel-Y-Koordinate auf dem Bildschirm (sichtbare Oberkante des Farbe-Buttons)
     */
    public void showFlyout(Window window, double anchorScreenX, double anchorScreenY) {
        if (window != null && window.isShowing()) {
            root.applyCss();
            root.layout();

            // Dropshadow-Kompensation des Flyout-Popup-Fensters (15px X, 7px Y)
            double shadowOffsetX = 15.0;
            double shadowOffsetY = 7.0;

            // Minimaler, eleganter Studio-Abstand von exakt 2px direkt neben dem sichtbaren Rand des Hauptmenüs
            double targetX = (anchorScreenX + 2.0) - shadowOffsetX;
            double targetY = anchorScreenY - shadowOffsetY;

            // Bildschirmgrenzen-Prüfung: Falls rechts kein Platz ist, links neben dem Menü öffnen
            javafx.stage.Screen screen = javafx.stage.Screen.getScreensForRectangle(anchorScreenX, anchorScreenY, 1, 1).stream()
                    .findFirst()
                    .orElse(javafx.stage.Screen.getPrimary());
            if (screen != null) {
                javafx.geometry.Rectangle2D bounds = screen.getVisualBounds();
                double flyoutW = 120.0;
                double flyoutH = 80.0;
                if (anchorScreenX + 2.0 + flyoutW > bounds.getMaxX()) {
                    targetX = anchorScreenX - flyoutW - 144.0 - shadowOffsetX;
                }
                if (anchorScreenY + flyoutH > bounds.getMaxY()) {
                    targetY = anchorScreenY - flyoutH + 30.0 - shadowOffsetY;
                }
            }

            show(window, targetX, targetY);
            enforceTransparentBackground();
        }
    }

    /**
     * Stellt sicher, dass das interne JavaFX-PopupScene-Fenster und dessen Root
     * vollständig transparent sind, damit Schatten und abgerundete Ecken nicht
     * durch ein eckiges Fenster-Rechteck überdeckt werden.
     */
    private void enforceTransparentBackground() {
        if (getScene() != null) {
            getScene().setFill(Color.TRANSPARENT);
            if (getScene().getRoot() != null) {
                getScene().getRoot().setStyle("-fx-background-color: transparent !important; -fx-background: transparent !important;");
            }
        }
    }
}

package de.exit.jdmx.ui.timeline;

import de.exit.jdmx.ui.icon.LucideIcon;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Window;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Modernes Custom-Kontextmenü für die Show-Timeline.
 * <p>
 * Basiert auf der bestehenden {@link Popup}-Architektur der Action- und Explorer-Menüs
 * im einheitlichen Studio Dark Theme ({@code .action-menu-popup} und {@code .action-menu-item}).
 * Unterstützt kaskadierende Untermenüs (wie {@link TimelineColorFlyout}) beim Hovern.
 * </p>
 * <p>
 * <b>Thread-Sicherheit:</b> Alle visuellen Modifikationen und Callbacks laufen
 * auf dem JavaFX-Application-Thread.
 * </p>
 */
public class TimelineContextMenu extends Popup {

    /** Map aller registrierten Menü-Buttons anhand ihres Keys. */
    private final Map<String, Button> items = new HashMap<>();

    /** Map aller registrierten Callbacks. */
    private final Map<String, Runnable> actions = new HashMap<>();

    /** Kaskadierendes Flyout-Untermenü zur Farbauswahl. */
    private final TimelineColorFlyout colorFlyout = new TimelineColorFlyout();

    /** Root-Container des Kontextmenüs. */
    private final VBox root;

    /**
     * Erstellt ein neues Timeline-Kontextmenü mit Test-Button und Farbe-Flyout.
     */
    public TimelineContextMenu() {
        setAutoHide(true);
        setHideOnEscape(true);

        this.root = new VBox(2);
        root.getStyleClass().add("action-menu-popup");
        root.setMinWidth(140);
        root.setPrefWidth(150);

        // Initialer Test-Button mit Labor-Icon (flask)
        Button btnTest = createMenuItem("test", "Test", "flask", Color.web("#A855F7"));
        btnTest.setOnMouseEntered(e -> colorFlyout.safeHide());

        // Szenen-Clip-Farbe anpassen mit kaskadierendem Flyout (Palette + Chevron)
        Button btnColor = createSubmenuItem("color", "Farbe", "palette", Color.web("#EC4899"));

        colorFlyout.setOnDismiss(this::safeHide);

        root.getChildren().addAll(btnTest, btnColor);
        getContent().add(root);
    }

    /**
     * Erzeugt einen Menüeintrag mit voller Breite, Lucide-Icon und Styling.
     *
     * @param key       Eindeutiger Schlüssel
     * @param title     Anzeigetext des Buttons
     * @param iconName  Name des Lucide-Icons
     * @param iconColor Vektor-Farbe des Icons
     * @return der konfigurierte {@link Button}
     */
    private Button createMenuItem(String key, String title, String iconName, Color iconColor) {
        Button btn = new Button(title);
        btn.getStyleClass().add("action-menu-item");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setCursor(Cursor.HAND);
        btn.setContentDisplay(ContentDisplay.LEFT);
        btn.setGraphicTextGap(10);

        LucideIcon icon = new LucideIcon(iconName, 15, iconColor);
        btn.setGraphic(icon);

        btn.setOnAction(e -> {
            safeHide();
            Runnable action = actions.get(key);
            if (action != null) {
                action.run();
            }
        });

        items.put(key, btn);
        return btn;
    }

    /**
     * Erzeugt einen Menüeintrag mit aufklappendem Untermenü-Pfeil (Chevron).
     *
     * @param key       Eindeutiger Schlüssel
     * @param title     Anzeigetext des Buttons
     * @param iconName  Name des Lucide-Icons
     * @param iconColor Vektor-Farbe des Icons
     * @return der konfigurierte {@link Button}
     */
    private Button createSubmenuItem(String key, String title, String iconName, Color iconColor) {
        Button btn = new Button(title);
        btn.getStyleClass().add("action-menu-item");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setCursor(Cursor.HAND);

        LucideIcon icon = new LucideIcon(iconName, 15, iconColor);
        LucideIcon chevron = new LucideIcon("chevron-right", 12, Color.web("#9CA3AF"));

        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: #D1D5DB; -fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox graphicBox = new HBox(8, icon, lbl, spacer, chevron);
        graphicBox.setAlignment(Pos.CENTER_LEFT);
        graphicBox.setMaxWidth(Double.MAX_VALUE);

        btn.setGraphic(graphicBox);
        btn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        btn.setOnAction(e -> {
            Bounds bounds = btn.localToScreen(btn.getBoundsInLocal());
            if (bounds != null && getOwnerWindow() != null) {
                colorFlyout.showFlyout(getOwnerWindow(), bounds.getMaxX() - 2.0, bounds.getMinY());
            }
        });

        btn.setOnMouseEntered(e -> {
            lbl.setStyle("-fx-text-fill: #F3F4F6; -fx-font-size: 13px;");
            chevron.setColor(Color.web("#F3F4F6"));
            Bounds bounds = btn.localToScreen(btn.getBoundsInLocal());
            if (bounds != null && getOwnerWindow() != null) {
                colorFlyout.showFlyout(getOwnerWindow(), bounds.getMaxX() - 2.0, bounds.getMinY());
            }
        });

        btn.setOnMouseExited(e -> {
            lbl.setStyle("-fx-text-fill: #D1D5DB; -fx-font-size: 13px;");
            chevron.setColor(Color.web("#9CA3AF"));
        });

        items.put(key, btn);
        return btn;
    }

    /**
     * Erzeugt eine schlanke Trennlinie im Menü-Design.
     *
     * @return die konfigurierte {@link Separator}-Instanz
     */
    public Separator createMenuSeparator() {
        Separator sep = new Separator();
        sep.getStyleClass().add("action-menu-separator");
        return sep;
    }

    /**
     * Schließt das Kontextmenü und alle aktiven Untermenüs sicher auf dem JavaFX-Thread.
     */
    public void safeHide() {
        colorFlyout.safeHide();
        if (isShowing()) {
            if (Platform.isFxApplicationThread()) {
                hide();
            } else {
                Platform.runLater(this::hide);
            }
        }
    }

    /**
     * Passt den Skalierungsfaktor für HiDPI-Monitore an.
     *
     * @param scale Skalierungsfaktor (z. B. 1.0, 1.25, 1.5)
     */
    public void setScale(double scale) {
        if (!getContent().isEmpty()) {
            getContent().getFirst().getTransforms().setAll(new javafx.scene.transform.Scale(scale, scale, 0, 0));
        }
    }

    /**
     * Liefert den Menü-Button für den angegebenen Schlüssel.
     *
     * @param key Menü-Schlüssel (z. B. "test")
     * @return {@link Button} oder {@code null}
     */
    public Button getItemButton(String key) {
        return items.get(key);
    }

    /**
     * Registriert eine Aktion für einen Menü-Schlüssel.
     *
     * @param key    Menü-Schlüssel
     * @param action Auszuführender Callback
     */
    public void setOnAction(String key, Runnable action) {
        this.actions.put(key, action);
    }

    /**
     * Steuert die Sichtbarkeit von Szenen-spezifischen Aktionen (wie Farbe...).
     *
     * @param visible {@code true}, wenn ein Szenen-Clip das Ziel ist
     */
    public void setSceneActionsVisible(boolean visible) {
        Button btnColor = items.get("color");
        if (btnColor != null) {
            btnColor.setVisible(visible);
            btnColor.setManaged(visible);
            if (!visible) {
                colorFlyout.safeHide();
            }
        }
    }

    /**
     * Liefert das kaskadierende Flyout-Untermenü zur Farbauswahl.
     *
     * @return das {@link TimelineColorFlyout}
     */
    public TimelineColorFlyout getColorFlyout() {
        return colorFlyout;
    }

    /**
     * Registriert den Callback für die Farbauswahl im Untermenü.
     *
     * @param onColorSelected Consumer mit dem gewählten Hex-Code
     */
    public void setOnColorSelected(Consumer<String> onColorSelected) {
        this.colorFlyout.setOnColorSelected(onColorSelected);
    }

    /**
     * Aktualisiert die aktive Farbe des Clips für das Häkchen im Untermenü.
     *
     * @param colorHex Hex-Code (z. B. "#38BDF8")
     */
    public void setCurrentColorHex(String colorHex) {
        this.colorFlyout.setActiveColor(colorHex);
    }

    /**
     * Öffnet das Kontextmenü so, dass die sichtbare linke obere Ecke des Menüs
     * exakt am Mauszeiger (screenX, screenY) positioniert wird.
     *
     * @param window  Übergeordnetes Fenster
     * @param screenX X-Koordinate auf dem Bildschirm (Mausposition)
     * @param screenY Y-Koordinate auf dem Bildschirm (Mausposition)
     */
    public void showMenu(Window window, double screenX, double screenY) {
        if (window != null && window.isShowing()) {
            root.applyCss();
            root.layout();

            // Dropshadow-Kompensation: Die sichtbare VBox (0,0) liegt bei (+15, +7) im Popup-Fenster
            double shadowOffsetX = 15.0;
            double shadowOffsetY = 7.0;

            double targetX = screenX - shadowOffsetX;
            double targetY = screenY - shadowOffsetY;

            // Bildschirmgrenzen-Prüfung gegen Abschneiden am Rand
            javafx.stage.Screen screen = javafx.stage.Screen.getScreensForRectangle(screenX, screenY, 1, 1).stream()
                    .findFirst()
                    .orElse(javafx.stage.Screen.getPrimary());
            if (screen != null) {
                javafx.geometry.Rectangle2D bounds = screen.getVisualBounds();
                double menuW = root.prefWidth(-1);
                double menuH = root.prefHeight(-1);
                if (screenX + menuW > bounds.getMaxX()) {
                    targetX = screenX - menuW + shadowOffsetX;
                }
                if (screenY + menuH > bounds.getMaxY()) {
                    targetY = screenY - menuH + shadowOffsetY;
                }
            }

            show(window, targetX, targetY);
        }
    }
}

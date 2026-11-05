package de.exit.jdmx.ui.menu;

import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.theme.StudioTheme;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Popup;

import java.util.HashMap;
import java.util.Map;

/**
 * Action-Dropdown-Menü für das Hinzufügen neuer Elemente im Project Explorer.
 * <p>
 * Öffnet sich beim Klick auf das grüne Plus-Zeichen im Header des Explorers
 * und bietet schnelle Auswahl zwischen:
 * <ul>
 *     <li>Gerät (Fixture importieren / patchen)</li>
 *     <li>Gerätegruppe</li>
 *     <li>Chaser</li>
 *     <li>Effekt</li>
 *     <li>Preset</li>
 * </ul>
 * Gestaltet im einheitlichen Studio-Dark-Design analog zum BrandActionMenu.
 * </p>
 */
public class ExplorerAddMenu extends Popup {

    private final Map<String, Button> items = new HashMap<>();
    private final Map<String, Runnable> actions = new HashMap<>();

    public ExplorerAddMenu() {
        setAutoHide(true);
        setHideOnEscape(true);

        VBox root = new VBox(2);
        root.getStyleClass().add("action-menu-popup");
        root.setMinWidth(155);
        root.setPrefWidth(160);

        Button btnFixture = createMenuItem("fixture", "Gerät", "box", StudioTheme.COLOR_ICON_STAGE);
        Button btnGroup = createMenuItem("group", "Gerätegruppe", "layers", StudioTheme.COLOR_ICON_TIMELINE);
        Button btnChaser = createMenuItem("chaser", "Chaser", "list-music", StudioTheme.COLOR_ICON_FADER);
        Button btnEffect = createMenuItem("effect", "Effekt", "sparkles", StudioTheme.COLOR_ICON_MATRIX);
        Button btnPreset = createMenuItem("preset", "Preset", "star", Color.web("#FB923C"));

        root.getChildren().addAll(
                btnFixture,
                createMenuSeparator(),
                btnGroup,
                createMenuSeparator(),
                btnChaser,
                btnEffect,
                btnPreset
        );

        getContent().add(root);
    }

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

    private Separator createMenuSeparator() {
        Separator sep = new Separator();
        sep.getStyleClass().add("action-menu-separator");
        return sep;
    }

    public void safeHide() {
        if (isShowing()) {
            if (Platform.isFxApplicationThread()) {
                hide();
            } else {
                Platform.runLater(this::hide);
            }
        }
    }

    public Button getItem(String key) {
        return items.get(key);
    }

    public void setOnAction(String key, Runnable action) {
        this.actions.put(key, action);
    }

    public void triggerAction(String key) {
        Runnable action = actions.get(key);
        if (action != null) {
            action.run();
        }
    }
}

package de.exit.jdmx.ui.menu;

import de.exit.jdmx.core.i18n.I18n;
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

        Button btnFixture = createMenuItem("fixture", I18n.get("explorer.add.fixture"), "box", StudioTheme.COLOR_ICON_STAGE);
        Button btnGroup = createMenuItem("group", I18n.get("explorer.add.group"), "layers", StudioTheme.COLOR_ICON_TIMELINE);
        Button btnChaser = createMenuItem("chaser", I18n.get("explorer.add.chaser"), "list-music", StudioTheme.COLOR_ICON_FADER);
        Button btnEffect = createMenuItem("effect", I18n.get("explorer.add.effect"), "sparkles", StudioTheme.COLOR_ICON_MATRIX);
        Button btnPreset = createMenuItem("preset", I18n.get("explorer.add.preset"), "star", Color.web("#FB923C"));

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

        // Dynamischer Sprachwechsel-Listener
        I18n.addLanguageChangeListener(lang -> {
            if (Platform.isFxApplicationThread() || !isShowing()) {
                updateLocalization();
            } else {
                Platform.runLater(this::updateLocalization);
            }
        });
    }

    /**
     * Aktualisiert alle Menüeinträge reaktiv bei Sprachwechsel.
     */
    public void updateLocalization() {
        updateButtonText("fixture", "explorer.add.fixture");
        updateButtonText("group", "explorer.add.group");
        updateButtonText("chaser", "explorer.add.chaser");
        updateButtonText("effect", "explorer.add.effect");
        updateButtonText("preset", "explorer.add.preset");
    }

    private void updateButtonText(String key, String i18nKey) {
        Button btn = items.get(key);
        if (btn != null) {
            btn.setText(I18n.get(i18nKey));
        }
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

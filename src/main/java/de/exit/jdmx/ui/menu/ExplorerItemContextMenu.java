package de.exit.jdmx.ui.menu;

import de.exit.jdmx.core.i18n.I18n;
import de.exit.jdmx.ui.icon.LucideIcon;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Window;

import java.util.HashMap;
import java.util.Map;

/**
 * Kontextmenü (Rechtsklick) für Elemente im Project Explorer.
 * <p>
 * Erlaubt das schnelle Umbenennen oder Entfernen von Geräten, Gruppen, Chasern
 * oder Szenen. Gestaltet im einheitlichen Studio-Dark-Design analog zu den
 * übrigen Action-Menüs.
 * </p>
 */
public class ExplorerItemContextMenu extends Popup {

    private final Map<String, Button> items = new HashMap<>();
    private final Map<String, Runnable> actions = new HashMap<>();

    /**
     * Erstellt ein neues Kontextmenü mit Optionen zum Umbenennen und Entfernen.
     */
    public ExplorerItemContextMenu() {
        setAutoHide(true);
        setHideOnEscape(true);

        VBox root = new VBox(2);
        root.getStyleClass().add("action-menu-popup");
        root.setMinWidth(140);
        root.setPrefWidth(150);

        Button btnRename = createMenuItem("rename", I18n.get("dialog.rename"), "pencil", Color.web("#38BDF8"));
        Button btnDelete = createMenuItem("delete", I18n.get("dialog.delete"), "trash-2", Color.web("#EF4444"));

        root.getChildren().addAll(
                btnRename,
                createMenuSeparator(),
                btnDelete
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
        Button btnRename = items.get("rename");
        if (btnRename != null) {
            btnRename.setText(I18n.get("dialog.rename"));
        }
        Button btnDelete = items.get("delete");
        if (btnDelete != null) {
            btnDelete.setText(I18n.get("dialog.delete"));
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

    /**
     * Schließt das Kontextmenü sicher auf dem JavaFX-Anwendungs-Thread.
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
     * Liefert den Menü-Button für den angegebenen Schlüssel ("rename", "delete").
     *
     * @param key Menü-Schlüssel
     * @return Button oder null
     */
    public Button getItemButton(String key) {
        return items.get(key);
    }

    /**
     * Registriert die Aktion für das Umbenennen.
     *
     * @param onRename Callback
     */
    public void setOnRename(Runnable onRename) {
        this.actions.put("rename", onRename);
    }

    /**
     * Registriert die Aktion für das Entfernen.
     *
     * @param onDelete Callback
     */
    public void setOnDelete(Runnable onDelete) {
        this.actions.put("delete", onDelete);
    }

    /**
     * Öffnet das Kontextmenü an den angegebenen Bildschirmkoordinaten.
     *
     * @param window  Übergeordnetes Fenster
     * @param screenX X-Koordinate auf dem Bildschirm
     * @param screenY Y-Koordinate auf dem Bildschirm
     */
    public void showMenu(Window window, double screenX, double screenY) {
        if (window != null && window.isShowing()) {
            show(window, screenX, screenY);
        }
    }
}

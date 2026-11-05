package de.exit.jdmx.ui.menu;

import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.theme.StudioTheme;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Popup;

import java.util.HashMap;
import java.util.Map;

/**
 * Modernes Action-Dropdown-Menü für den Brand-Button von JDMX.
 * <p>
 * Inspiriert vom QLC+ "Actions"-Menü, jedoch im Studio Dark Theme gestaltet
 * mit bunten Lucide-Vektor-Icons und dezenten Schriften.
 * Bietet schnellen Zugriff auf Projektaktionen, Undo/Redo, Settings und Fullscreen.
 * </p>
 */
public class BrandActionMenu extends Popup {

    /** Map aller registrierten Menü-Buttons anhand ihres Keys. */
    private final Map<String, Button> items = new HashMap<>();

    /** Map aller registrierten Callbacks. */
    private final Map<String, Runnable> actions = new HashMap<>();

    /**
     * Erstellt das Dropdown-Menü und initialisiert alle Einträge.
     */
    public BrandActionMenu() {
        setAutoHide(true);
        setHideOnEscape(true);

        VBox root = new VBox(2);
        root.getStyleClass().add("action-menu-popup");
        root.setMinWidth(175);
        root.setPrefWidth(178);

        // 1. Projekt-Operationen
        Button btnNew = createMenuItem("new_project", "New Project", "file-plus", StudioTheme.COLOR_MENU_NEW);
        Button btnOpen = createMenuItem("open_file", "Open File", "folder-open", StudioTheme.COLOR_MENU_OPEN);
        Button btnSave = createMenuItem("save", "Save", "save", StudioTheme.COLOR_MENU_SAVE);
        Button btnSaveAs = createMenuItem("save_as", "Save as...", "file-pen", StudioTheme.COLOR_MENU_SAVE_AS);

        // 2. Undo / Redo in kompakter Zweispalten-Zeile
        HBox undoRedoRow = createUndoRedoRow();

        // 3. System & Ansicht
        Button btnSettings = createMenuItem("settings", "Settings", "settings", StudioTheme.COLOR_MENU_SETTINGS);
        Button btnUpdates = createMenuItem("check_updates", "Updates", "arrow-up-circle", Color.web("#38BDF8"));
        Button btnFullscreen = createMenuItem("fullscreen", "Fullscreen", "maximize", StudioTheme.COLOR_MENU_FULLSCREEN);

        root.getChildren().addAll(
                btnNew,
                btnOpen,
                btnSave,
                btnSaveAs,
                createMenuSeparator(),
                undoRedoRow,
                createMenuSeparator(),
                btnSettings,
                btnUpdates,
                btnFullscreen
        );

        getContent().add(root);
    }

    /**
     * Erzeugt einen Standard-Menüeintrag mit voller Breite.
     */
    private Button createMenuItem(String key, String title, String iconName, Color iconColor) {
        Button btn = new Button(title);
        btn.getStyleClass().add("action-menu-item");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setCursor(Cursor.HAND);
        btn.setContentDisplay(ContentDisplay.LEFT);
        btn.setGraphicTextGap(10);

        LucideIcon icon = new LucideIcon(iconName, 16, iconColor);
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
     * Erzeugt die kombinierte Undo/Redo-Zeile mit zwei gleich breiten Buttons.
     */
    private HBox createUndoRedoRow() {
        HBox row = new HBox(4);
        row.setAlignment(Pos.CENTER);

        Button btnUndo = createHalfMenuItem("undo", "Undo", "undo-2", StudioTheme.COLOR_MENU_UNDO);
        Button btnRedo = createHalfMenuItem("redo", "Redo", "redo-2", StudioTheme.COLOR_MENU_REDO);

        HBox.setHgrow(btnUndo, Priority.ALWAYS);
        HBox.setHgrow(btnRedo, Priority.ALWAYS);

        row.getChildren().addAll(btnUndo, btnRedo);
        return row;
    }

    /**
     * Erzeugt einen halbreitigen Menüeintrag für die Undo/Redo-Zeile.
     */
    private Button createHalfMenuItem(String key, String title, String iconName, Color iconColor) {
        Button btn = new Button(title);
        btn.getStyleClass().addAll("action-menu-item", "action-menu-half-item");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setCursor(Cursor.HAND);
        btn.setContentDisplay(ContentDisplay.LEFT);
        btn.setGraphicTextGap(6);

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
     * Schließt das Dropdown-Menü sicher und thread-konform, falls es aktuell angezeigt wird.
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
     * Erzeugt einen dezenten horizontalen Separator.
     */
    private Separator createMenuSeparator() {
        Separator sep = new Separator();
        sep.getStyleClass().add("action-menu-separator");
        return sep;
    }

    /**
     * Liefert den Button eines Eintrags anhand seines Keys.
     *
     * @param key Eindeutiger Key (z. B. "new_project", "save")
     * @return Button des Eintrags oder {@code null}
     */
    public Button getItem(String key) {
        return items.get(key);
    }

    /**
     * Registriert einen Aktions-Callback für einen Menüeintrag.
     *
     * @param key    Eindeutiger Key
     * @param action Auszuführende Aktion
     */
    public void setOnAction(String key, Runnable action) {
        this.actions.put(key, action);
    }

    /**
     * Löst die hinterlegte Aktion eines Menüeintrags programmgesteuert aus (z. B. via Shortcut).
     *
     * @param key Eindeutiger Key (z. B. "new_project", "save")
     */
    public void triggerAction(String key) {
        Runnable action = actions.get(key);
        if (action != null) {
            action.run();
        }
    }
}

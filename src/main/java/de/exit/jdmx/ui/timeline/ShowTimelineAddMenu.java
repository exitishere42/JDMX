package de.exit.jdmx.ui.timeline;

import de.exit.jdmx.core.i18n.I18n;
import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.theme.StudioTheme;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

import java.util.HashMap;
import java.util.Map;

/**
 * Dropdown-Menü für das Hinzufügen neuer Elemente in der Show-Timeline.
 * <p>
 * Öffnet sich beim Klick auf den [+]-Button im Tab-Header des Timeline-Explorers
 * und bietet aktuell einen Eintrag:
 * <ul>
 *     <li>Song (MP3/WAV aus Datei-Dialog laden)</li>
 * </ul>
 * Gestaltet im einheitlichen Studio-Dark-Design analog zum ExplorerAddMenu.
 * </p>
 */
public class ShowTimelineAddMenu extends Popup {

    private final Map<String, Button> items = new HashMap<>();
    private final Map<String, Runnable> actions = new HashMap<>();

    /**
     * Erstellt das Add-Menü mit dem "Song"-Eintrag.
     */
    public ShowTimelineAddMenu() {
        setAutoHide(true);
        setHideOnEscape(true);

        VBox root = new VBox(2);
        root.getStyleClass().add("action-menu-popup");
        root.setMinWidth(130);
        root.setPrefWidth(140);

        Button btnSong = createMenuItem("song", I18n.get("timeline.explorer.add.song"),
                "music", StudioTheme.COLOR_ICON_TIMELINE);

        root.getChildren().add(btnSong);
        getContent().add(root);

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
        Button btn = items.get("song");
        if (btn != null) {
            btn.setText(I18n.get("timeline.explorer.add.song"));
        }
    }

    private Button createMenuItem(String key, String title, String iconName, javafx.scene.paint.Color iconColor) {
        Button btn = new Button(title);
        btn.getStyleClass().add("action-menu-item");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setCursor(Cursor.HAND);
        btn.setContentDisplay(ContentDisplay.LEFT);
        btn.setGraphicTextGap(10);
        btn.setGraphic(new LucideIcon(iconName, 15, iconColor));

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
     * Versteckt das Menü thread-sicher.
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
     * Gibt den Button-Eintrag für den angegebenen Schlüssel zurück.
     *
     * @param key Schlüssel des Eintrags (z. B. "song")
     * @return der Button oder {@code null}, wenn nicht vorhanden
     */
    public Button getItem(String key) {
        return items.get(key);
    }

    /**
     * Registriert eine Aktion für den angegebenen Schlüssel.
     *
     * @param key    Schlüssel des Eintrags
     * @param action Aktion, die beim Klick ausgeführt wird
     */
    public void setOnAction(String key, Runnable action) {
        this.actions.put(key, action);
    }
}

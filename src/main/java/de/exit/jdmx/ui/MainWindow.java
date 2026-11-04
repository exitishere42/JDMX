package de.exit.jdmx.ui;

import de.exit.jdmx.service.ShortcutAction;
import de.exit.jdmx.service.ShortcutService;
import de.exit.jdmx.ui.dialog.SettingsDialog;
import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.theme.StudioTheme;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;

/**
 * Das primäre Hauptfenster (Main View) der JDMX Lichtsteuerungs-Suite.
 * <p>
 * Beherbergt die allumfassende All-in-One {@link HeaderBar} im oberen Bereich,
 * den dynamisch umschaltbaren Arbeitsbereich (Viewport) für die 5 Workspace-Tabs
 * sowie die In-App Modal Overlay Schicht für dialogbasierte Interaktionen ("Fenster im Fenster").
 * </p>
 */
public class MainWindow extends StackPane {

    /** Der primäre Inhaltsbereich mit HeaderBar und Viewport. */
    private final BorderPane contentPane;

    /** Die obere All-in-One Kopfzeile. */
    private final HeaderBar headerBar;

    /** Der zentrale Viewport-Container für die Tab-Inhalte. */
    private final StackPane viewport;

    /** Die 5 Ansichten für die Workspace-Tabs. */
    private final Node[] tabViews = new Node[5];

    /** In-App Modal Overlay Schicht (Halbtransparenter Schleier für Dialoge). */
    private final StackPane overlayPane;

    /** HUD-Container für schnelle, unaufdringliche Status-Benachrichtigungen (z. B. Vollbild). */
    private final StackPane hudPane;

    /** Zentraler Shortcut-Service für konfigurierbare Tastaturkürzel. */
    private final ShortcutService shortcutService;

    /**
     * Erstellt das Hauptfenster und initialisiert alle Basis-Komponenten.
     */
    public MainWindow() {
        this(new ShortcutService());
    }

    /**
     * Erstellt das Hauptfenster mit einem spezifischen ShortcutService.
     */
    public MainWindow(ShortcutService shortcutService) {
        this.shortcutService = shortcutService;
        getStyleClass().add("main-window");

        this.contentPane = new BorderPane();

        // 1. All-in-One HeaderBar oben verankern
        this.headerBar = new HeaderBar();
        contentPane.setTop(headerBar);

        // 2. Zentraler Viewport
        this.viewport = new StackPane();
        viewport.getStyleClass().add("main-viewport");
        contentPane.setCenter(viewport);

        // 3. Tab-Inhalte initialisieren
        initTabViews();

        // 4. Modal Overlay Schicht vorbereiten
        this.overlayPane = new StackPane();
        overlayPane.setVisible(false);
        overlayPane.setManaged(false);
        overlayPane.getStyleClass().add("modal-overlay-scrim");
        overlayPane.setAlignment(Pos.CENTER);
        overlayPane.setOnMouseClicked(e -> {
            if (e.getTarget() == overlayPane) {
                hideOverlay();
            }
        });

        // 5. Schnelles HUD-Overlay (Vollbild / Status)
        this.hudPane = new StackPane();
        hudPane.setPickOnBounds(false);
        hudPane.setAlignment(Pos.TOP_CENTER);

        getChildren().addAll(contentPane, overlayPane, hudPane);

        // 6. Header-Events verdrahten
        headerBar.setOnTabChange(this::switchToTab);
        headerBar.setOnBlackout(active -> {
            // Signal-Event für DmxEngine / Core
        });
        headerBar.setOnStopAll(() -> {
            // Not-Release für alle Chaser, Fades und Overrides
        });

        // Brand Action-Menü Events
        headerBar.getActionMenu().setOnAction("fullscreen", this::toggleFullscreen);

        headerBar.getActionMenu().setOnAction("settings", () -> {
            SettingsDialog.show(this);
        });

        // 7. Globale Tastenkürzel und Overlay-Steuerung
        addEventFilter(KeyEvent.KEY_PRESSED, this::handleGlobalKeyEvent);

        // Initialen Tab anzeigen (Tab 0 = Stage & Fixtures)
        switchToTab(0);
    }

    /**
     * Blendet einen modalen In-App-Dialog über der Arbeitsfläche ein.
     *
     * @param modalContent Die anzuzeigende Dialog-Karte
     */
    public void showOverlay(Node modalContent) {
        overlayPane.getChildren().setAll(modalContent);
        overlayPane.setVisible(true);
        overlayPane.setManaged(true);

        modalContent.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(140), modalContent);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    /**
     * Schließt das aktive Modal-Overlay und gibt die Arbeitsfläche frei.
     */
    public void hideOverlay() {
        overlayPane.setVisible(false);
        overlayPane.setManaged(false);
        overlayPane.getChildren().clear();
    }

    /**
     * Prüft, ob aktuell ein Modal-Overlay eingeblendet ist.
     */
    public boolean isOverlayShowing() {
        return overlayPane.isVisible();
    }

    /**
     * Liefert die obere Header-Bar.
     */
    public HeaderBar getHeaderBar() {
        return headerBar;
    }

    /**
     * Initialisiert die 5 Workspace-Tabs.
     */
    private void initTabViews() {
        tabViews[0] = createTabPlaceholder("Stage & Fixtures", "stage", StudioTheme.COLOR_ICON_STAGE);
        tabViews[1] = createTabPlaceholder("Simple Desk", "sliders", StudioTheme.COLOR_ICON_FADER);
        tabViews[2] = createTabPlaceholder("Button Matrix", "layout-grid", StudioTheme.COLOR_ICON_MATRIX);
        tabViews[3] = createTabPlaceholder("Show-Timeline", "audio-lines", StudioTheme.COLOR_ICON_TIMELINE);
        tabViews[4] = createTabPlaceholder("Input/Output", "arrow-right-left", StudioTheme.COLOR_ICON_IO);
    }

    /**
     * Erzeugt eine strukturierte Vorschau-Kachel für einen Tab mit farbigem Lucide-Icon.
     */
    private Node createTabPlaceholder(String title, String iconName, javafx.scene.paint.Color iconColor) {
        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("workspace-placeholder");

        LucideIcon icon = new LucideIcon(iconName, 48, iconColor);

        Text titleText = new Text(title);
        titleText.getStyleClass().add("placeholder-title");

        box.getChildren().addAll(icon, titleText);
        return box;
    }

    /**
     * Schaltet den zentralen Viewport auf den gewünschten Tab um.
     *
     * @param index Tab-Index (0 bis 4)
     */
    public void switchToTab(int index) {
        if (index >= 0 && index < tabViews.length) {
            viewport.getChildren().clear();
            viewport.getChildren().add(tabViews[index]);
            headerBar.setActiveTab(index);
        }
    }

    /**
     * Schaltet den Vollbildmodus des Anwendungsfensters um.
     * Beim Aktivieren wird JavaFX-seitig der träge native Hinweis unterdrückt
     * und stattdessen ein extrem schnelles, dezentes Studio-HUD eingeblendet.
     */
    public void toggleFullscreen() {
        if (getScene() != null && getScene().getWindow() instanceof javafx.stage.Stage stage) {
            stage.setFullScreenExitHint("");
            stage.setFullScreenExitKeyCombination(javafx.scene.input.KeyCombination.NO_MATCH);

            boolean newState = !stage.isFullScreen();
            stage.setFullScreen(newState);

            if (newState) {
                showFullscreenHud();
            }
        }
    }

    /**
     * Zeigt eine schlanke, elegante HUD-Benachrichtigung beim Aktivieren des Vollbildmodus,
     * die nach kurzer Zeit (~600ms) automatisch und sanft wieder ausgeblendet wird.
     */
    public void showFullscreenHud() {
        hudPane.getChildren().clear();

        HBox hud = new HBox(8);
        hud.setAlignment(Pos.CENTER);
        hud.getStyleClass().add("fullscreen-hud");

        LucideIcon icon = new LucideIcon("maximize", 14, StudioTheme.COLOR_MENU_FULLSCREEN);
        Text text = new Text("Vollbildmodus (F11)");
        text.getStyleClass().add("fullscreen-hud-text");

        hud.getChildren().addAll(icon, text);
        StackPane.setMargin(hud, new Insets(16, 0, 0, 0));

        hudPane.getChildren().add(hud);

        hud.setOpacity(0.0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(80), hud);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        PauseTransition pause = new PauseTransition(Duration.millis(550));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), hud);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        SequentialTransition seq = new SequentialTransition(fadeIn, pause, fadeOut);
        seq.setOnFinished(e -> hudPane.getChildren().remove(hud));
        seq.play();
    }

    /**
     * Verarbeitet globale Tastatureingaben unter Berücksichtigung aktiver Overlays.
     */
    private void handleGlobalKeyEvent(KeyEvent event) {
        // 1. Wenn ein Modal-Overlay offen ist: Escape schließt das Overlay
        if (isOverlayShowing()) {
            if (event.getCode() == KeyCode.ESCAPE) {
                hideOverlay();
                event.consume();
            }
            return;
        }

        // 2. Escape verlässt den Vollbildmodus (wenn kein Overlay geöffnet ist)
        if (event.getCode() == KeyCode.ESCAPE) {
            if (getScene() != null && getScene().getWindow() instanceof javafx.stage.Stage stage && stage.isFullScreen()) {
                stage.setFullScreen(false);
                event.consume();
                return;
            }
        }

        // 3. F11 / Konfigurierter Shortcut für Vollbild umschalten
        if (shortcutService.matches(ShortcutAction.FULLSCREEN, event)) {
            toggleFullscreen();
            event.consume();
            return;
        }

        // 4. Tab-Wechsel (1-5 bzw. Numpad 1-5)
        if (shortcutService.matches(ShortcutAction.TAB_STAGE, event)) {
            switchToTab(0);
            event.consume();
            return;
        }
        if (shortcutService.matches(ShortcutAction.TAB_DESK, event)) {
            switchToTab(1);
            event.consume();
            return;
        }
        if (shortcutService.matches(ShortcutAction.TAB_MATRIX, event)) {
            switchToTab(2);
            event.consume();
            return;
        }
        if (shortcutService.matches(ShortcutAction.TAB_TIMELINE, event)) {
            switchToTab(3);
            event.consume();
            return;
        }
        if (shortcutService.matches(ShortcutAction.TAB_IO, event)) {
            switchToTab(4);
            event.consume();
            return;
        }

        // 5. Menü-Aktionen (Strg+N, Strg+O, Strg+S, Strg+Umschalt+S, Strg+Z, Strg+Y)
        if (shortcutService.matches(ShortcutAction.NEW_PROJECT, event)) {
            headerBar.getActionMenu().triggerAction("new_project");
            event.consume();
            return;
        }
        if (shortcutService.matches(ShortcutAction.OPEN_PROJECT, event)) {
            headerBar.getActionMenu().triggerAction("open_file");
            event.consume();
            return;
        }
        if (shortcutService.matches(ShortcutAction.SAVE_PROJECT, event)) {
            headerBar.getActionMenu().triggerAction("save");
            event.consume();
            return;
        }
        if (shortcutService.matches(ShortcutAction.SAVE_AS_PROJECT, event)) {
            headerBar.getActionMenu().triggerAction("save_as");
            event.consume();
            return;
        }
        if (shortcutService.matches(ShortcutAction.UNDO, event)) {
            headerBar.getActionMenu().triggerAction("undo");
            event.consume();
            return;
        }
        if (shortcutService.matches(ShortcutAction.REDO, event)) {
            headerBar.getActionMenu().triggerAction("redo");
            event.consume();
            return;
        }
    }

    /**
     * Liefert den zentralen ShortcutService des Hauptfensters.
     */
    public ShortcutService getShortcutService() {
        return shortcutService;
    }
}

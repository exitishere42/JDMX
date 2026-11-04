package de.exit.jdmx.ui;

import de.exit.jdmx.ui.dialog.SettingsDialog;
import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.theme.StudioTheme;
import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
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

    /**
     * Erstellt das Hauptfenster und initialisiert alle Basis-Komponenten.
     */
    public MainWindow() {
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

        getChildren().addAll(contentPane, overlayPane);

        // 5. Header-Events verdrahten
        headerBar.setOnTabChange(this::switchToTab);
        headerBar.setOnBlackout(active -> {
            // Signal-Event für DmxEngine / Core
        });
        headerBar.setOnStopAll(() -> {
            // Not-Release für alle Chaser, Fades und Overrides
        });

        // Brand Action-Menü Events
        headerBar.getActionMenu().setOnAction("fullscreen", () -> {
            if (getScene() != null && getScene().getWindow() instanceof javafx.stage.Stage stage) {
                stage.setFullScreen(!stage.isFullScreen());
            }
        });

        headerBar.getActionMenu().setOnAction("settings", () -> {
            SettingsDialog.show(this);
        });

        // Globale Escape-Taste schließt aktive Overlays
        addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE && isOverlayShowing()) {
                hideOverlay();
                e.consume();
            }
        });

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
        }
    }
}

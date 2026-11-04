package de.exit.jdmx.ui;

import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.theme.StudioTheme;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

/**
 * Das primäre Hauptfenster (Main View) der JDMX Lichtsteuerungs-Suite.
 * <p>
 * Beherbergt die allumfassende All-in-One {@link HeaderBar} im oberen Bereich
 * sowie den dynamisch umschaltbaren Arbeitsbereich (Viewport) für die 5 Workspace-Tabs.
 * </p>
 */
public class MainWindow extends BorderPane {

    /** Die obere All-in-One Kopfzeile. */
    private final HeaderBar headerBar;

    /** Der zentrale Viewport-Container für die Tab-Inhalte. */
    private final StackPane viewport;

    /** Die 5 Ansichten für die Workspace-Tabs. */
    private final Node[] tabViews = new Node[5];

    /**
     * Erstellt das Hauptfenster und initialisiert alle Basis-Komponenten.
     */
    public MainWindow() {
        getStyleClass().add("main-window");

        // 1. All-in-One HeaderBar oben verankern
        this.headerBar = new HeaderBar();
        setTop(headerBar);

        // 2. Zentraler Viewport
        this.viewport = new StackPane();
        viewport.getStyleClass().add("main-viewport");
        setCenter(viewport);

        // 3. Tab-Inhalte initialisieren
        initTabViews();

        // 4. Header-Events verdrahten
        headerBar.setOnTabChange(this::switchToTab);
        headerBar.setOnBlackout(active -> {
            // Signal-Event für DmxEngine / Core
        });
        headerBar.setOnStopAll(() -> {
            // Not-Release für alle Chaser, Fades und Overrides
        });

        // Initialen Tab anzeigen (Tab 0 = Stage & Fixtures)
        switchToTab(0);
    }

    /**
     * Initialisiert die 5 Workspace-Tabs.
     */
    private void initTabViews() {
        tabViews[0] = createTabPlaceholder(
                "Stage & Fixtures",
                "2D Bühnenplan mit U-Traverse gegen Projektionswand & DMXControl-3 Device Control",
                "stage"
        );
        tabViews[1] = createTabPlaceholder(
                "Simple Desk",
                "QLC+ Style 512-Kanal Fader-Pult mit Custom Scrollbar & Fixture List",
                "sliders"
        );
        tabViews[2] = createTabPlaceholder(
                "Button Matrix",
                "3x16 / 3x25 Live Virtual Console für Show-Busking & Selective Cues",
                "layout-grid"
        );
        tabViews[3] = createTabPlaceholder(
                "Show-Timeline",
                "FL Studio Style Multi-Line Playlist mit integrierter Musik-Wiedergabe",
                "audio-lines"
        );
        tabViews[4] = createTabPlaceholder(
                "Input/Output",
                "Art-Net UDP, DMX2USB (FTDI/Enttec), MIDI-Subsystem & DMX-Bus-Traffic Diagnostik",
                "cable"
        );
    }

    /**
     * Erzeugt eine strukturierte Vorschau-Kachel für einen Tab.
     */
    private Node createTabPlaceholder(String title, String description, String iconName) {
        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("workspace-placeholder");

        LucideIcon icon = new LucideIcon(iconName, 48, StudioTheme.COLOR_CYAN);

        Text titleText = new Text(title);
        titleText.getStyleClass().add("placeholder-title");

        Text descText = new Text(description);
        descText.getStyleClass().add("placeholder-sub");

        box.getChildren().addAll(icon, titleText, descText);
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

    public HeaderBar getHeaderBar() {
        return headerBar;
    }
}

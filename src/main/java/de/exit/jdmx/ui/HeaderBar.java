package de.exit.jdmx.ui;

import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.menu.BrandActionMenu;
import de.exit.jdmx.ui.theme.StudioTheme;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;

import java.util.function.Consumer;

/**
 * Die allumfassende, ultra-kompakte All-in-One Kopfzeile (Single Top-Bar) von JDMX.
 * <p>
 * Primärfarbe ist ein dezentes, unaufdringliches Hellgrau.
 * Die Tabs und Funktionen werden durch farbige Lucide-Vektor-Icons visualisiert.
 * Der STOP-Button ist ein reines Symbol ohne Text.
 * </p>
 */
public class HeaderBar extends HBox {

    /** Index des aktuell aktiven Tabs. */
    private final IntegerProperty activeTabIndex = new SimpleIntegerProperty(0);

    /** Zustand des globalen Blackouts. */
    private final BooleanProperty blackoutActive = new SimpleBooleanProperty(false);

    /** Callback-Empfänger bei Tab-Wechsel. */
    private Consumer<Integer> tabChangeHandler;

    /** Callback-Empfänger bei Druck auf den STOP-Button. */
    private Runnable stopAllHandler;

    /** Callback-Empfänger bei Blackout-Toggle. */
    private Consumer<Boolean> blackoutHandler;

    /** Label zur Visualisierung der aktuellen DMX-Paketrate. */
    private final Text trafficText = new Text("40 DMX/s");

    /** Aufklappbares Aktions-Menü für Projektaktionen, Settings und Fullscreen. */
    private final BrandActionMenu actionMenu = new BrandActionMenu();

    /**
     * Erstellt die All-in-One HeaderBar mit dezenter Hellgrau-Primärfarbe und bunten Icons.
     */
    public HeaderBar() {
        getStyleClass().add("header-bar");
        setAlignment(Pos.CENTER_LEFT);

        // 1. Brand Button (Logo links neben Text, ohne Border)
        Button brandButton = createBrandButton();

        // Trennlinie
        Line separator = new Line(0, 0, 0, 20);
        separator.getStyleClass().add("header-separator");

        // 2. Die 5 Workspace Tab-Buttons mit bunten Icons
        HBox tabsBox = createTabGroup();

        // Flexibler Platzhalter zur Ausrichtung nach rechts
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 3. Rechte Steuerungs- & Statusleiste
        ToggleButton blackoutBtn = createBlackoutButton();
        HBox trafficPill = createTrafficPill();
        Button stopBtn = createStopButton();

        HBox rightBox = new HBox(8, blackoutBtn, trafficPill, stopBtn);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(brandButton, separator, tabsBox, spacer, rightBox);
    }

    /**
     * Erzeugt den klickbaren Brand-Button mit dezentem Glühbirnen-Symbol (Warm Amber-Gold #FEDC94) links
     * und dem Markennamen `jdmx` in Hellgrau, ohne Umrandung.
     * Öffnet beim Klick das schlanke Action-Dropdown-Menü.
     */
    private Button createBrandButton() {
        Button btn = new Button("jdmx");
        btn.getStyleClass().add("brand-button");
        btn.setCursor(Cursor.HAND);

        LucideIcon brandIcon = new LucideIcon("lightbulb", 18, StudioTheme.COLOR_BRAND_LOGO);
        btn.setGraphic(brandIcon);
        btn.setContentDisplay(ContentDisplay.LEFT);
        btn.setGraphicTextGap(8);

        // Klick toggelt das Action-Dropdown-Menü direkt unter dem Button
        btn.setOnAction(e -> {
            if (actionMenu.isShowing()) {
                actionMenu.hide();
            } else {
                Bounds bounds = btn.localToScreen(btn.getBoundsInLocal());
                if (bounds != null && btn.getScene() != null && btn.getScene().getWindow() != null) {
                    actionMenu.show(btn.getScene().getWindow(), bounds.getMinX(), bounds.getMaxY() + 4);
                }
            }
        });

        return btn;
    }

    /**
     * Liefert das aufklappbare Aktions-Menü des Brand-Buttons.
     *
     * @return BrandActionMenu Instanz
     */
    public BrandActionMenu getActionMenu() {
        return actionMenu;
    }

    /**
     * Erzeugt die 5 Workspace-Umschalter mit individuellen bunten Lucide-Icons.
     */
    private HBox createTabGroup() {
        HBox box = new HBox(4);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("tab-button-group");

        ToggleGroup group = new ToggleGroup();

        String[] tabNames = {
                "Stage & Fixtures",
                "Simple Desk",
                "Button Matrix",
                "Show-Timeline",
                "Input/Output"
        };

        String[] tabIcons = {
                "stage",
                "sliders",
                "layout-grid",
                "audio-lines",
                "arrow-right-left"
        };

        Color[] tabColors = {
                StudioTheme.COLOR_ICON_STAGE,    // Cyan / Blau
                StudioTheme.COLOR_ICON_FADER,    // Warm Amber / Orange
                StudioTheme.COLOR_ICON_MATRIX,   // Violett / Purple
                StudioTheme.COLOR_ICON_TIMELINE, // Smaragd-Grün
                StudioTheme.COLOR_ICON_IO        // Türkis / Teal
        };

        for (int i = 0; i < tabNames.length; i++) {
            final int index = i;
            ToggleButton tabBtn = new ToggleButton(tabNames[i]);
            tabBtn.setToggleGroup(group);
            tabBtn.getStyleClass().add("nav-tab-button");

            // Echtes Lucide-Vektor-Icon in seiner individuellen Signalfarbe
            LucideIcon icon = new LucideIcon(tabIcons[i], 16, tabColors[i]);
            tabBtn.setGraphic(icon);

            if (i == 0) {
                tabBtn.setSelected(true);
            }

            tabBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (Boolean.TRUE.equals(newVal)) {
                    activeTabIndex.set(index);
                    if (tabChangeHandler != null) {
                        tabChangeHandler.accept(index);
                    }
                }
            });

            box.getChildren().add(tabBtn);
        }

        return box;
    }

    /**
     * Erzeugt den Blackout-Toggle Button mit rotem Power-Symbol.
     */
    private ToggleButton createBlackoutButton() {
        ToggleButton btn = new ToggleButton("BLACKOUT");
        btn.getStyleClass().add("btn-blackout");

        LucideIcon powerIcon = new LucideIcon("power", 14, StudioTheme.COLOR_ICON_RED);
        btn.setGraphic(powerIcon);

        btn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            boolean active = Boolean.TRUE.equals(newVal);
            blackoutActive.set(active);
            if (active) {
                btn.getStyleClass().add("active");
                powerIcon.setColor(Color.WHITE);
            } else {
                btn.getStyleClass().remove("active");
                powerIcon.setColor(StudioTheme.COLOR_ICON_RED);
            }
            if (blackoutHandler != null) {
                blackoutHandler.accept(active);
            }
        });

        return btn;
    }

    /**
     * Erzeugt die DMX/s Traffic-Pille mit grünem Aktivitäts-Puls.
     */
    private HBox createTrafficPill() {
        HBox pill = new HBox(6);
        pill.getStyleClass().add("traffic-pill");
        pill.setAlignment(Pos.CENTER);

        LucideIcon activityIcon = new LucideIcon("activity", 14, StudioTheme.COLOR_ICON_GREEN);

        trafficText.getStyleClass().add("traffic-text");

        pill.getChildren().addAll(activityIcon, trafficText);
        return pill;
    }

    /**
     * Erzeugt den reinen Symbol-STOP-Button (ohne Text, nur rotes Quadrat-Icon).
     */
    private Button createStopButton() {
        Button btn = new Button();
        btn.getStyleClass().add("btn-stop-symbol");

        // Reines Lucide-Quadrat-Symbol in Weiß auf rotem Grund
        LucideIcon stopIcon = new LucideIcon("square", 16, Color.WHITE);
        btn.setGraphic(stopIcon);

        btn.setOnAction(e -> {
            if (stopAllHandler != null) {
                stopAllHandler.run();
            }
        });

        return btn;
    }

    /**
     * Aktualisiert den angezeigten DMX-Paketrate-Text.
     *
     * @param packetsPerSecond Aktuelle Paketrate
     */
    public void setDmxRate(int packetsPerSecond) {
        trafficText.setText(packetsPerSecond + " DMX/s");
    }

    public void setOnTabChange(Consumer<Integer> handler) {
        this.tabChangeHandler = handler;
    }

    public void setOnStopAll(Runnable handler) {
        this.stopAllHandler = handler;
    }

    public void setOnBlackout(Consumer<Boolean> handler) {
        this.blackoutHandler = handler;
    }

    public int getActiveTabIndex() {
        return activeTabIndex.get();
    }

    public IntegerProperty activeTabIndexProperty() {
        return activeTabIndex;
    }

    public boolean isBlackoutActive() {
        return blackoutActive.get();
    }

    public BooleanProperty blackoutActiveProperty() {
        return blackoutActive;
    }
}

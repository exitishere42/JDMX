package de.exit.jdmx.ui;

import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.theme.StudioTheme;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
 * Vereint das Markenlogo, die Umschalter für alle 5 Workspace-Tabs, den globalen
 * Blackout-Toggle, die DMX-Traffic-Anzeige (DMX/s) und den roten STOP-Button in
 * einer einzigen, platzsparenden Zeile.
 * </p>
 */
public class HeaderBar extends HBox {

    /** Index des aktuell aktiven Tabs (0 = Stage & Fixtures, 1 = Simple Desk, ...). */
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

    /**
     * Erstellt die All-in-One HeaderBar mit allen interaktiven Komponenten.
     */
    public HeaderBar() {
        getStyleClass().add("header-bar");
        setAlignment(Pos.CENTER_LEFT);

        // 1. Brand Logo
        HBox brandBox = createBrandBox();

        // Trennlinie
        Line separator = new Line(0, 0, 0, 22);
        separator.getStyleClass().add("header-separator");

        // 2. Die 5 Workspace Tab-Buttons
        HBox tabsBox = createTabGroup();

        // Flexibler Platzhalter zur Ausrichtung nach rechts
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 3. Rechte Steuerungs- & Statusleiste
        ToggleButton blackoutBtn = createBlackoutButton();
        HBox trafficPill = createTrafficPill();
        Button stopBtn = createStopButton();

        HBox rightBox = new HBox(10, blackoutBtn, trafficPill, stopBtn);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(brandBox, separator, tabsBox, spacer, rightBox);
    }

    /**
     * Erzeugt das Marken-Logo `jdmx`.
     */
    private HBox createBrandBox() {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("brand-box");

        Text title = new Text("jdmx");
        title.getStyleClass().add("brand-text");

        Text sub = new Text("2.0");
        sub.getStyleClass().add("brand-sub");

        box.getChildren().addAll(title, sub);
        return box;
    }

    /**
     * Erzeugt die 5 Workspace-Umschalter als exklusive Toggle-Gruppe.
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
                "cable"
        };

        for (int i = 0; i < tabNames.length; i++) {
            final int index = i;
            ToggleButton tabBtn = new ToggleButton(tabNames[i]);
            tabBtn.setToggleGroup(group);
            tabBtn.getStyleClass().add("nav-tab-button");

            LucideIcon icon = new LucideIcon(tabIcons[i], 14, StudioTheme.COLOR_TEXT_MED);
            tabBtn.setGraphic(icon);

            if (i == 0) {
                tabBtn.setSelected(true);
                icon.setColor(StudioTheme.COLOR_CYAN);
            }

            tabBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (Boolean.TRUE.equals(newVal)) {
                    icon.setColor(StudioTheme.COLOR_CYAN);
                    activeTabIndex.set(index);
                    if (tabChangeHandler != null) {
                        tabChangeHandler.accept(index);
                    }
                } else {
                    icon.setColor(StudioTheme.COLOR_TEXT_MED);
                }
            });

            box.getChildren().add(tabBtn);
        }

        return box;
    }

    /**
     * Erzeugt den Blackout-Toggle Button.
     */
    private ToggleButton createBlackoutButton() {
        ToggleButton btn = new ToggleButton("BLACKOUT");
        btn.getStyleClass().add("btn-blackout");

        LucideIcon powerIcon = new LucideIcon("power", 14, StudioTheme.COLOR_TEXT_HIGH);
        btn.setGraphic(powerIcon);

        btn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            boolean active = Boolean.TRUE.equals(newVal);
            blackoutActive.set(active);
            if (active) {
                btn.getStyleClass().add("active");
                powerIcon.setColor(Color.WHITE);
            } else {
                btn.getStyleClass().remove("active");
                powerIcon.setColor(StudioTheme.COLOR_TEXT_HIGH);
            }
            if (blackoutHandler != null) {
                blackoutHandler.accept(active);
            }
        });

        return btn;
    }

    /**
     * Erzeugt die DMX/s Traffic-Pille mit Puls-Symbol.
     */
    private HBox createTrafficPill() {
        HBox pill = new HBox(6);
        pill.getStyleClass().add("traffic-pill");
        pill.setAlignment(Pos.CENTER);

        LucideIcon activityIcon = new LucideIcon("activity", 12, StudioTheme.COLOR_GREEN);

        trafficText.getStyleClass().add("traffic-text");
        pill.getChildren().addAll(activityIcon, trafficText);
        return pill;
    }

    /**
     * Erzeugt den roten globalen STOP-Button.
     */
    private Button createStopButton() {
        Button btn = new Button("STOP");
        btn.getStyleClass().add("btn-stop-all");

        LucideIcon stopIcon = new LucideIcon("square", 12, Color.WHITE);
        btn.setGraphic(stopIcon);

        btn.setOnAction(e -> {
            if (stopAllHandler != null) {
                stopAllHandler.run();
            }
        });

        return btn;
    }

    /**
     * Aktualisiert den angezeigten DMX-Paketrate-Text (z. B. "44 DMX/s").
     *
     * @param packetsPerSecond Aktuelle Paketrate
     */
    public void setDmxRate(int packetsPerSecond) {
        if (trafficText != null) {
            trafficText.setText(packetsPerSecond + " DMX/s");
        }
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

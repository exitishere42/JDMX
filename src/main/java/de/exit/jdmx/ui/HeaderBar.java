package de.exit.jdmx.ui;

import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.menu.BrandActionMenu;
import de.exit.jdmx.ui.theme.StudioTheme;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    /** Zurück- und Vor-Knopf (Undo & Redo). */
    private final Button btnUndo = new Button();
    private final Button btnRedo = new Button();
    private Runnable undoHandler;
    private Runnable redoHandler;

    /** Callback-Empfänger bei Tab-Wechsel. */
    private Consumer<Integer> tabChangeHandler;

    /** Callback-Empfänger bei Druck auf den STOP-Button. */
    private Runnable stopAllHandler;

    /** Callback-Empfänger bei Blackout-Toggle. */
    private Consumer<Boolean> blackoutHandler;

    /** Liste der 5 ToggleButtons für die Workspace-Tabs. */
    private final List<ToggleButton> tabButtons = new ArrayList<>();

    /** Label zur Visualisierung der aktuellen DMX-Paketrate. */
    private final Text trafficText = new Text("40 DMX/s");

    /** Aufklappbares Aktions-Menü für Projektaktionen, Settings und Fullscreen. */
    private final BrandActionMenu actionMenu = new BrandActionMenu();

    /** Container für die Statusanzeige mit den zwei gekürzten Trennlinien. */
    private final HBox statusArea = new HBox(8);
    private final HBox statusPill = new HBox(6);
    private final StackPane statusIconContainer = new StackPane();
    private final Label statusLabel = new Label();
    private SequentialTransition statusTransition;

    /**
     * Erstellt die All-in-One HeaderBar mit dezenter Hellgrau-Primärfarbe und bunten Icons.
     */
    public HeaderBar() {
        getStyleClass().add("header-bar");
        setAlignment(Pos.CENTER_LEFT);

        // 1. Brand Button (Logo links neben Text, ohne Border)
        Button brandButton = createBrandButton();

        // 1b. Zurück- und Vor-Knopf (Undo & Redo)
        HBox undoRedoBox = createUndoRedoButtons();

        // Trennlinie
        Line separator = new Line(0, 0, 0, 20);
        separator.getStyleClass().add("header-separator");

        // 2. Die 5 Workspace Tab-Buttons mit bunten Icons
        HBox tabsBox = createTabGroup();

        // Flexibler Platzhalter zur Ausrichtung nach rechts
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 3. Statusbereich zwischen zwei gekürzten Trennlinien (wie im I/O-Patch)
        HBox headerStatusBox = createStatusArea();
        HBox.setMargin(headerStatusBox, new Insets(0, 8, 0, 0));

        // 4. Rechte Steuerungs- & Statusleiste
        ToggleButton blackoutBtn = createBlackoutButton();
        HBox trafficPill = createTrafficPill();
        Button stopBtn = createStopButton();

        HBox rightBox = new HBox(8, blackoutBtn, trafficPill, stopBtn);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(brandButton, undoRedoBox, separator, tabsBox, spacer, headerStatusBox, rightBox);
    }

    private HBox createStatusArea() {
        statusArea.setAlignment(Pos.CENTER);
        statusArea.setOpacity(0.0);
        statusArea.setVisible(false);

        // Status-Pille ohne Rahmen oder Trennstriche
        statusPill.setAlignment(Pos.CENTER);
        statusPill.getStyleClass().add("header-status-pill");

        statusLabel.getStyleClass().add("header-status-text");

        statusPill.getChildren().addAll(statusIconContainer, statusLabel);

        statusArea.getChildren().add(statusPill);
        return statusArea;
    }

    /**
     * Zeigt eine flüssig ein- und ausblendende Statusnachricht in der HeaderBar an.
     *
     * @param text      Der Statustext
     * @param iconName  Lucide-Icon Name (z. B. "check", "maximize", "folder-open")
     * @param iconColor Farbe des Icons
     * @param duration  Sichtbarkeitsdauer
     */
    public void showStatus(String text, String iconName, Color iconColor, Duration duration) {
        if (text == null || text.isBlank()) {
            return;
        }

        if (statusTransition != null) {
            statusTransition.stop();
        }

        statusIconContainer.getChildren().clear();
        if (iconName != null && !iconName.isBlank()) {
            LucideIcon icon = new LucideIcon(iconName, 13, iconColor != null ? iconColor : Color.web("#9CA3AF"));
            statusIconContainer.getChildren().add(icon);
        }

        statusLabel.setText(text);
        if (iconColor != null) {
            statusLabel.setTextFill(iconColor);
        } else {
            statusLabel.setTextFill(Color.web("#E5E7EB"));
        }

        statusArea.setVisible(true);
        statusArea.setOpacity(0.0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(120), statusArea);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        PauseTransition pause = new PauseTransition(duration != null ? duration : Duration.millis(2500));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), statusArea);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        statusTransition = new SequentialTransition(fadeIn, pause, fadeOut);
        statusTransition.setOnFinished(e -> {
            statusArea.setVisible(false);
            statusArea.setOpacity(0.0);
        });
        statusTransition.play();
    }

    public HBox getStatusArea() {
        return statusArea;
    }

    public Label getStatusLabel() {
        return statusLabel;
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
                "LightDesk",
                "Button Matrix",
                "Show-Timeline",
                "I/O Patch"
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
            tabButtons.add(tabBtn);
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

    /**
     * Wählt den Tab-Button programmgesteuert aus (z. B. via Shortcut 1-5).
     *
     * @param index Der Tab-Index (0 bis 4)
     */
    public void setActiveTab(int index) {
        if (index >= 0 && index < tabButtons.size()) {
            ToggleButton btn = tabButtons.get(index);
            if (!btn.isSelected()) {
                btn.setSelected(true);
            }
        }
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

    public List<ToggleButton> getTabButtons() {
        return Collections.unmodifiableList(tabButtons);
    }

    private HBox createUndoRedoButtons() {
        HBox box = new HBox(2);
        box.setAlignment(Pos.CENTER);

        btnUndo.getStyleClass().add("header-tool-btn");
        btnUndo.setGraphic(new LucideIcon("undo-2", 14, Color.web("#D1D5DB")));
        btnUndo.setTooltip(new javafx.scene.control.Tooltip("Rückgängig (Strg+Z)"));
        btnUndo.setDisable(true);
        btnUndo.setOnAction(e -> {
            if (undoHandler != null) {
                undoHandler.run();
            }
        });

        btnRedo.getStyleClass().add("header-tool-btn");
        btnRedo.setGraphic(new LucideIcon("redo-2", 14, Color.web("#D1D5DB")));
        btnRedo.setTooltip(new javafx.scene.control.Tooltip("Wiederholen (Strg+Y)"));
        btnRedo.setDisable(true);
        btnRedo.setOnAction(e -> {
            if (redoHandler != null) {
                redoHandler.run();
            }
        });

        box.getChildren().addAll(btnUndo, btnRedo);
        return box;
    }

    public Button getUndoButton() {
        return btnUndo;
    }

    public Button getRedoButton() {
        return btnRedo;
    }

    public void setOnUndo(Runnable handler) {
        this.undoHandler = handler;
    }

    public void setOnRedo(Runnable handler) {
        this.redoHandler = handler;
    }

    /**
     * Aktualisiert den Aktivierungszustand der Undo- und Redo-Schaltflächen
     * sowohl in der Kopfzeile als auch im Brand-Aktionsmenü.
     *
     * @param canUndo true, wenn Rückgängig verfügbar ist
     * @param canRedo true, wenn Wiederholen verfügbar ist
     */
    public void updateUndoRedoState(boolean canUndo, boolean canRedo) {
        btnUndo.setDisable(!canUndo);
        btnRedo.setDisable(!canRedo);
        if (actionMenu.getItem("undo") != null) {
            actionMenu.getItem("undo").setDisable(!canUndo);
        }
        if (actionMenu.getItem("redo") != null) {
            actionMenu.getItem("redo").setDisable(!canRedo);
        }
    }
}

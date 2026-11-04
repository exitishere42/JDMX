package de.exit.jdmx.ui.dialog;

import de.exit.jdmx.service.ShortcutAction;
import de.exit.jdmx.service.ShortcutItem;
import de.exit.jdmx.service.ShortcutService;
import de.exit.jdmx.ui.MainWindow;
import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.theme.StudioTheme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import java.io.File;
import java.util.List;

/**
 * In-App Modal Dialog für die globalen Einstellungen ("Fenster im Fenster").
 * <p>
 * Enthält zwei saubere Sektionen:
 * <ul>
 *     <li><b>Allgemein:</b> Startverhalten, Sicherheitsabfragen, Auto-Save und Projektordner.</li>
 *     <li><b>Shortcuts:</b> Vollständig interaktiv anpassbare Tastaturbelegungen mit Aufnahme-Modus.</li>
 * </ul>
 * Schließt bei Klick auf das Schließen-Kreuz, Escape oder Klick auf den abgedunkelten Hintergrund.
 * </p>
 */
public class SettingsDialog {

    private final MainWindow mainWindow;
    private final ShortcutService shortcutService;
    private final VBox card;

    private Button btnTabGeneral;
    private Button btnTabShortcuts;
    private StackPane contentArea;

    private VBox generalView;
    private VBox shortcutsView;

    // Aktuell zur Aufnahme ausgewählte Shortcut-Aktion (null, wenn inaktiv)
    private ShortcutAction recordingAction = null;
    private Button recordingButton = null;

    /**
     * Öffnet den Einstellungs-Dialog als modales In-App Overlay.
     *
     * @param mainWindow Hauptfenster
     */
    public static void show(MainWindow mainWindow) {
        SettingsDialog dialog = new SettingsDialog(mainWindow, new ShortcutService());
        mainWindow.showOverlay(dialog.getView());
    }

    /**
     * Erstellt den Dialog mit übergebenem Shortcut-Service.
     */
    public SettingsDialog(MainWindow mainWindow, ShortcutService shortcutService) {
        this.mainWindow = mainWindow;
        this.shortcutService = shortcutService;

        this.card = new VBox(12);
        card.getStyleClass().add("settings-dialog-card");
        card.setMinWidth(540);
        card.setMaxWidth(600);
        card.setPrefWidth(560);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        buildUi();
    }

    private void buildUi() {
        // 1. Header (Icon + Titel + Schließen-Button)
        HBox header = buildHeader();

        // 2. Tab-Umschalter (Allgemein | Shortcuts)
        HBox tabBar = buildTabBar();

        // 3. Inhaltsbereich
        this.contentArea = new StackPane();
        contentArea.getStyleClass().add("settings-content-area");

        this.generalView = buildGeneralView();
        this.shortcutsView = buildShortcutsView();

        contentArea.getChildren().setAll(generalView);

        // 4. Footer (Schließen Button)
        HBox footer = buildFooter();

        card.getChildren().addAll(header, tabBar, contentArea, footer);
    }

    private HBox buildHeader() {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        LucideIcon icon = new LucideIcon("settings", 20, StudioTheme.COLOR_MENU_SETTINGS);

        Text title = new Text("Settings");
        title.getStyleClass().add("dialog-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnClose = new Button();
        btnClose.getStyleClass().add("dialog-close-button");
        btnClose.setGraphic(new LucideIcon("x", 16, StudioTheme.COLOR_TEXT_MED));
        btnClose.setCursor(Cursor.HAND);
        btnClose.setOnAction(e -> mainWindow.hideOverlay());

        header.getChildren().addAll(icon, title, spacer, btnClose);
        return header;
    }

    private HBox buildTabBar() {
        HBox tabBar = new HBox(8);
        tabBar.setAlignment(Pos.CENTER_LEFT);
        tabBar.getStyleClass().add("dialog-tab-bar");

        btnTabGeneral = new Button("Allgemein");
        btnTabGeneral.getStyleClass().addAll("dialog-tab-button", "active");
        btnTabGeneral.setGraphic(new LucideIcon("sliders", 14, StudioTheme.COLOR_TEXT_HIGH));
        btnTabGeneral.setCursor(Cursor.HAND);
        btnTabGeneral.setOnAction(e -> switchTab(true));

        btnTabShortcuts = new Button("Shortcuts");
        btnTabShortcuts.getStyleClass().add("dialog-tab-button");
        btnTabShortcuts.setGraphic(new LucideIcon("keyboard", 14, StudioTheme.COLOR_TEXT_MED));
        btnTabShortcuts.setCursor(Cursor.HAND);
        btnTabShortcuts.setOnAction(e -> switchTab(false));

        tabBar.getChildren().addAll(btnTabGeneral, btnTabShortcuts);
        return tabBar;
    }

    private void switchTab(boolean showGeneral) {
        cancelRecording();
        if (showGeneral) {
            btnTabGeneral.getStyleClass().add("active");
            btnTabShortcuts.getStyleClass().remove("active");
            contentArea.getChildren().setAll(generalView);
        } else {
            btnTabShortcuts.getStyleClass().add("active");
            btnTabGeneral.getStyleClass().remove("active");
            contentArea.getChildren().setAll(shortcutsView);
        }
    }

    private VBox buildGeneralView() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(10, 4, 10, 4));

        // Vollbild beim Start
        CheckBox cbFullscreen = new CheckBox("Vollbild beim Programmstart");
        cbFullscreen.getStyleClass().add("settings-checkbox");
        cbFullscreen.setSelected(false);

        // Sicherheitsabfrage vor Beenden
        CheckBox cbConfirmExit = new CheckBox("Bestätigung vor dem Schließen bei ungespeicherten Szenen");
        cbConfirmExit.getStyleClass().add("settings-checkbox");
        cbConfirmExit.setSelected(true);

        // Auto-Save
        HBox autoSaveRow = new HBox(12);
        autoSaveRow.setAlignment(Pos.CENTER_LEFT);
        Label lblAutoSave = new Label("Automatisches Speichern:");
        lblAutoSave.getStyleClass().add("settings-label");
        ComboBox<String> cbAutoSave = new ComboBox<>();
        cbAutoSave.getItems().addAll("Deaktiviert", "Alle 5 Minuten", "Alle 10 Minuten", "Alle 15 Minuten");
        cbAutoSave.setValue("Alle 10 Minuten");
        cbAutoSave.getStyleClass().add("settings-combo");
        autoSaveRow.getChildren().addAll(lblAutoSave, cbAutoSave);

        // Standard-Projektverzeichnis
        VBox folderBox = new VBox(6);
        Label lblFolder = new Label("Standard-Projektordner:");
        lblFolder.getStyleClass().add("settings-label");

        HBox folderRow = new HBox(8);
        folderRow.setAlignment(Pos.CENTER_LEFT);
        TextField tfFolder = new TextField(System.getProperty("user.home") + File.separator + "jdmx-shows");
        tfFolder.getStyleClass().add("settings-textfield");
        HBox.setHgrow(tfFolder, Priority.ALWAYS);

        Button btnBrowse = new Button("Durchsuchen...");
        btnBrowse.getStyleClass().add("settings-btn-secondary");
        btnBrowse.setGraphic(new LucideIcon("folder-open", 14, StudioTheme.COLOR_ICON_FADER));
        btnBrowse.setCursor(Cursor.HAND);

        folderRow.getChildren().addAll(tfFolder, btnBrowse);
        folderBox.getChildren().addAll(lblFolder, folderRow);

        box.getChildren().addAll(cbFullscreen, cbConfirmExit, autoSaveRow, folderBox);
        return box;
    }

    private VBox buildShortcutsView() {
        VBox box = new VBox(6);
        box.setPadding(new Insets(4, 0, 4, 0));

        // Tabellen-Kopfzeile: HOTKEY | DESCRIPTION | RESET
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.getStyleClass().add("shortcut-table-header");

        Label colHotkey = new Label("HOTKEY");
        colHotkey.getStyleClass().add("shortcut-col-title");
        colHotkey.setMinWidth(135);
        colHotkey.setPrefWidth(135);

        Label colDesc = new Label("DESCRIPTION");
        colDesc.getStyleClass().add("shortcut-col-title");
        HBox.setHgrow(colDesc, Priority.ALWAYS);

        Label colReset = new Label("RESET");
        colReset.getStyleClass().add("shortcut-col-title");
        colReset.setMinWidth(36);
        colReset.setAlignment(Pos.CENTER);

        headerRow.getChildren().addAll(colHotkey, colDesc, colReset);
        box.getChildren().add(headerRow);

        // Tabellen-Zeilen (Direkt und sauber ohne Scrollbar)
        VBox table = new VBox(2);
        List<ShortcutItem> items = shortcutService.getAllShortcuts();

        for (ShortcutItem item : items) {
            HBox row = createShortcutRow(item);
            table.getChildren().add(row);
        }

        box.getChildren().add(table);
        return box;
    }

    private HBox createShortcutRow(ShortcutItem item) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("shortcut-table-row");

        // 1. Spalte: Hotkey Badge Button
        Button btnKey = new Button(shortcutService.getDisplayText(item.action()));
        btnKey.getStyleClass().add("shortcut-key-badge");
        btnKey.setCursor(Cursor.HAND);

        // 2. Spalte: Description (Icon + Erklärung)
        HBox descBox = new HBox(8);
        descBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(descBox, Priority.ALWAYS);

        LucideIcon icon = new LucideIcon(item.action().getIconName(), 14, StudioTheme.COLOR_TEXT_HIGH);
        Label lblDesc = new Label(item.action().getLabel());
        lblDesc.getStyleClass().add("shortcut-description");

        descBox.getChildren().addAll(icon, lblDesc);

        // 3. Spalte: Reset Button
        Button btnReset = new Button();
        btnReset.getStyleClass().add("shortcut-reset-btn");
        btnReset.setGraphic(new LucideIcon("rotate-ccw", 12, StudioTheme.COLOR_TEXT_LOW));
        btnReset.setCursor(Cursor.HAND);
        btnReset.setTooltip(new Tooltip("Auf Standard zurücksetzen"));
        btnReset.setOnAction(e -> {
            cancelRecording();
            shortcutService.resetShortcut(item.action());
            btnKey.setText(shortcutService.getDisplayText(item.action()));
            btnKey.getStyleClass().remove("recording");
        });

        // Klick auf Badge startet Aufnahme-Modus
        btnKey.setOnAction(e -> startRecording(item.action(), btnKey));

        // Key-Event Listener für Tastendruck
        btnKey.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (recordingAction == item.action()) {
                handleKeyRecording(item.action(), btnKey, event);
                event.consume();
            }
        });

        row.getChildren().addAll(btnKey, descBox, btnReset);
        return row;
    }

    private void startRecording(ShortcutAction action, Button badgeBtn) {
        if (recordingButton != null && recordingAction != null) {
            cancelRecording();
        }

        this.recordingAction = action;
        this.recordingButton = badgeBtn;

        badgeBtn.setText("Taste drücken...");
        badgeBtn.getStyleClass().add("recording");
        badgeBtn.requestFocus();
    }

    private void cancelRecording() {
        if (recordingAction != null && recordingButton != null) {
            recordingButton.setText(shortcutService.getDisplayText(recordingAction));
            recordingButton.getStyleClass().remove("recording");
            this.recordingAction = null;
            this.recordingButton = null;
        }
    }

    private void handleKeyRecording(ShortcutAction action, Button badgeBtn, KeyEvent event) {
        KeyCode code = event.getCode();

        // Escape bricht Aufnahme ab
        if (code == KeyCode.ESCAPE) {
            cancelRecording();
            return;
        }

        // Reine Modifier-Tasten alleine ignorieren
        if (code.isModifierKey()) {
            return;
        }

        // Neue KeyCombination bauen
        KeyCombination.ModifierValue ctrl = event.isControlDown() ? KeyCombination.ModifierValue.DOWN : KeyCombination.ModifierValue.UP;
        KeyCombination.ModifierValue alt = event.isAltDown() ? KeyCombination.ModifierValue.DOWN : KeyCombination.ModifierValue.UP;
        KeyCombination.ModifierValue shift = event.isShiftDown() ? KeyCombination.ModifierValue.DOWN : KeyCombination.ModifierValue.UP;
        KeyCombination.ModifierValue meta = event.isMetaDown() ? KeyCombination.ModifierValue.DOWN : KeyCombination.ModifierValue.UP;

        KeyCodeCombination newComb = new KeyCodeCombination(code, shift, ctrl, alt, meta, KeyCombination.ModifierValue.UP);

        // Übernehmen
        shortcutService.setShortcut(action, newComb);
        badgeBtn.setText(shortcutService.getDisplayText(action));
        badgeBtn.getStyleClass().remove("recording");

        this.recordingAction = null;
        this.recordingButton = null;
    }

    private HBox buildFooter() {
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(8, 0, 0, 0));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnDone = new Button("Fertig");
        btnDone.getStyleClass().add("settings-btn-primary");
        btnDone.setGraphic(new LucideIcon("check", 14, StudioTheme.COLOR_ICON_GREEN));
        btnDone.setCursor(Cursor.HAND);
        btnDone.setOnAction(e -> {
            cancelRecording();
            mainWindow.hideOverlay();
        });

        footer.getChildren().addAll(spacer, btnDone);
        return footer;
    }

    /**
     * Liefert die Dialog-Karte als JavaFX Node für das Overlay.
     */
    public Node getView() {
        return card;
    }
}

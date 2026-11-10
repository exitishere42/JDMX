package de.exit.jdmx.ui.dialog;

import de.exit.jdmx.core.i18n.I18n;
import de.exit.jdmx.core.log.AppLogger;
import de.exit.jdmx.service.SettingsService;
import de.exit.jdmx.service.ShortcutAction;
import de.exit.jdmx.service.ShortcutItem;
import de.exit.jdmx.service.ShortcutService;
import de.exit.jdmx.ui.MainWindow;
import de.exit.jdmx.ui.icon.FlagIcon;
import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.theme.StudioTheme;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;

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
    private final SettingsService settingsService;
    private final VBox card;

    private Button btnTabGeneral;
    private Button btnTabShortcuts;
    private StackPane contentArea;

    private VBox generalView;
    private VBox shortcutsView;
    private Button btnClose;

    private Text titleText;
    private Label lblLang;
    private CheckBox cbFullscreen;
    private CheckBox cbConfirmExit;
    private Label lblAutoSave;
    private Label lblFolder;
    private Button btnBrowse;
    private Label lblLog;
    private Button btnOpenLog;
    private Button btnCopyLog;
    private Button btnDone;

    // Aktuell zur Aufnahme ausgewählte Shortcut-Aktion (null, wenn inaktiv)
    private ShortcutAction recordingAction = null;
    private Button recordingButton = null;

    /**
     * Öffnet den Einstellungs-Dialog als modales In-App Overlay.
     *
     * @param mainWindow Hauptfenster
     */
    public static void show(MainWindow mainWindow) {
        SettingsDialog dialog = new SettingsDialog(
                mainWindow,
                mainWindow.getShortcutService(),
                mainWindow.getSettingsService()
        );
        mainWindow.showOverlay(dialog.getView());
    }

    /**
     * Erstellt den Dialog mit übergebenem Shortcut-Service.
     */
    public SettingsDialog(MainWindow mainWindow, ShortcutService shortcutService) {
        this(mainWindow, shortcutService, mainWindow != null ? mainWindow.getSettingsService() : new SettingsService());
    }

    /**
     * Erstellt den Dialog mit übergebenem Shortcut- und Settings-Service.
     */
    public SettingsDialog(MainWindow mainWindow, ShortcutService shortcutService, SettingsService settingsService) {
        this.mainWindow = mainWindow;
        this.shortcutService = shortcutService;
        this.settingsService = settingsService != null ? settingsService : new SettingsService();

        this.card = new VBox(12);
        card.getStyleClass().add("settings-dialog-card");
        card.setMinWidth(480);
        card.setPrefWidth(510);
        card.setMaxWidth(530);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        buildUi();

        // Dynamischer Sprachwechsel
        I18n.addLanguageChangeListener(lang -> {
            if (Platform.isFxApplicationThread()) {
                updateLocalization();
            } else {
                Platform.runLater(this::updateLocalization);
            }
        });
    }

    private void buildUi() {
        // 1. Header (Icon + Titel + Schließen-Button)
        HBox header = buildHeader();

        // 2. Tab-Umschalter (Allgemein | Shortcuts)
        HBox tabBar = buildTabBar();

        // 3. Inhaltsbereich (mit identischer Höhe für Allgemein und Shortcuts)
        this.contentArea = new StackPane();
        contentArea.getStyleClass().add("settings-content-area");
        contentArea.setMinHeight(255);
        contentArea.setPrefHeight(255);
        contentArea.setMaxHeight(255);

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

        this.titleText = new Text(I18n.get("settings.title"));
        titleText.getStyleClass().add("dialog-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        this.btnClose = new Button();
        btnClose.getStyleClass().add("dialog-close-button");
        btnClose.setGraphic(new LucideIcon("x", 15, StudioTheme.COLOR_ICON_RED));
        btnClose.setCursor(Cursor.HAND);
        btnClose.setOnAction(e -> mainWindow.hideOverlay());

        header.getChildren().addAll(icon, titleText, spacer, btnClose);
        return header;
    }

    private HBox buildTabBar() {
        HBox tabBar = new HBox(8);
        tabBar.setAlignment(Pos.CENTER_LEFT);
        tabBar.getStyleClass().add("dialog-tab-bar");

        btnTabGeneral = new Button(I18n.get("settings.tab.general"));
        btnTabGeneral.getStyleClass().addAll("dialog-tab-button", "active");
        btnTabGeneral.setGraphic(new LucideIcon("sliders", 14, StudioTheme.COLOR_TEXT_HIGH));
        btnTabGeneral.setCursor(Cursor.HAND);
        btnTabGeneral.setOnAction(e -> switchTab(true));

        btnTabShortcuts = new Button(I18n.get("settings.tab.shortcuts"));
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

    /**
     * Repräsentiert eine auswählbare Sprache mit Vektor-Flagge.
     */
    public record LanguageOption(String code, String displayName) {
        @Override
        public String toString() {
            return displayName;
        }
    }

    private VBox buildGeneralView() {
        VBox box = new VBox(14);
        box.setPadding(new Insets(10, 4, 10, 4));

        // 1. Sprache (Language mit echten Vektor-Flaggen für de, en, ru)
        HBox langRow = new HBox(12);
        langRow.setAlignment(Pos.CENTER_LEFT);
        this.lblLang = new Label(I18n.get("settings.language"));
        lblLang.getStyleClass().add("settings-label");
        lblLang.setMinWidth(160);

        ComboBox<LanguageOption> cbLanguage = createLanguageComboBox();
        langRow.getChildren().addAll(lblLang, cbLanguage);

        // 2. Vollbild beim Start
        this.cbFullscreen = new CheckBox(I18n.get("settings.fullscreen"));
        cbFullscreen.getStyleClass().add("settings-checkbox");
        cbFullscreen.setSelected(settingsService.isFullscreen());
        cbFullscreen.selectedProperty().addListener((obs, o, n) -> {
            settingsService.setFullscreen(n);
            if (mainWindow != null) mainWindow.autoSave();
        });

        // 3. Sicherheitsabfrage vor Beenden
        this.cbConfirmExit = new CheckBox(I18n.get("settings.confirm_exit"));
        cbConfirmExit.getStyleClass().add("settings-checkbox");
        cbConfirmExit.setSelected(settingsService.isConfirmExit());
        cbConfirmExit.selectedProperty().addListener((obs, o, n) -> {
            settingsService.setConfirmExit(n);
            if (mainWindow != null) mainWindow.autoSave();
        });

        // 4. Auto-Save
        HBox autoSaveRow = new HBox(12);
        autoSaveRow.setAlignment(Pos.CENTER_LEFT);
        this.lblAutoSave = new Label(I18n.get("settings.autosave"));
        lblAutoSave.getStyleClass().add("settings-label");
        lblAutoSave.setMinWidth(160);
        ComboBox<String> cbAutoSave = new ComboBox<>();
        cbAutoSave.getItems().addAll("Deaktiviert", "Alle 5 Minuten", "Alle 10 Minuten", "Alle 15 Minuten");
        cbAutoSave.setValue(settingsService.getAutoSaveInterval());
        cbAutoSave.getStyleClass().add("settings-combo");
        cbAutoSave.valueProperty().addListener((obs, o, n) -> {
            if (n != null) {
                settingsService.setAutoSaveInterval(n);
                if (mainWindow != null) mainWindow.autoSave();
            }
        });
        autoSaveRow.getChildren().addAll(lblAutoSave, cbAutoSave);

        // 5. Standard-Projektverzeichnis
        VBox folderBox = new VBox(6);
        this.lblFolder = new Label(I18n.get("settings.default_folder"));
        lblFolder.getStyleClass().add("settings-label");

        HBox folderRow = new HBox(8);
        folderRow.setAlignment(Pos.CENTER_LEFT);
        TextField tfFolder = new TextField(settingsService.getDefaultFolder());
        tfFolder.getStyleClass().add("settings-textfield");
        HBox.setHgrow(tfFolder, Priority.ALWAYS);
        tfFolder.textProperty().addListener((obs, o, n) -> {
            if (n != null) {
                settingsService.setDefaultFolder(n);
                if (mainWindow != null) mainWindow.autoSave();
            }
        });

        this.btnBrowse = new Button(I18n.get("settings.browse"));
        btnBrowse.getStyleClass().add("settings-btn-secondary");
        btnBrowse.setGraphic(new LucideIcon("folder-open", 14, StudioTheme.COLOR_ICON_FADER));
        btnBrowse.setCursor(Cursor.HAND);
        btnBrowse.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Standard-Projektverzeichnis wählen");
            File current = new File(tfFolder.getText());
            if (current.exists() && current.isDirectory()) {
                chooser.setInitialDirectory(current);
            }
            File chosen = chooser.showDialog(mainWindow != null && mainWindow.getScene() != null ? mainWindow.getScene().getWindow() : null);
            if (chosen != null) {
                tfFolder.setText(chosen.getAbsolutePath());
                settingsService.setDefaultFolder(chosen.getAbsolutePath());
                if (mainWindow != null) mainWindow.autoSave();
            }
        });

        folderRow.getChildren().addAll(tfFolder, btnBrowse);
        folderBox.getChildren().addAll(lblFolder, folderRow);

        // 6. Diagnose & Log-Datei (für Fehleranalyse)
        VBox logBox = new VBox(6);
        this.lblLog = new Label(I18n.get("settings.diagnostics"));
        lblLog.getStyleClass().add("settings-label");

        HBox logRow = new HBox(8);
        logRow.setAlignment(Pos.CENTER_LEFT);
        TextField tfLog = new TextField(AppLogger.getLogFilePath().toAbsolutePath().toString());
        tfLog.setEditable(false);
        tfLog.getStyleClass().add("settings-textfield");
        HBox.setHgrow(tfLog, Priority.ALWAYS);

        this.btnOpenLog = new Button(I18n.get("settings.open_log_folder"));
        btnOpenLog.getStyleClass().add("settings-btn-secondary");
        btnOpenLog.setGraphic(new LucideIcon("folder", 14, StudioTheme.COLOR_ICON_FADER));
        btnOpenLog.setCursor(Cursor.HAND);
        btnOpenLog.setOnAction(e -> AppLogger.openLogFolder());

        this.btnCopyLog = new Button(I18n.get("settings.copy_path"));
        btnCopyLog.getStyleClass().add("settings-btn-secondary");
        btnCopyLog.setGraphic(new LucideIcon("copy", 14, StudioTheme.COLOR_ICON_FADER));
        btnCopyLog.setCursor(Cursor.HAND);
        btnCopyLog.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(tfLog.getText());
            Clipboard.getSystemClipboard().setContent(content);
            btnCopyLog.setText("Kopiert!");
        });

        logRow.getChildren().addAll(tfLog, btnOpenLog, btnCopyLog);
        logBox.getChildren().addAll(lblLog, logRow);

        box.getChildren().addAll(langRow, cbFullscreen, cbConfirmExit, autoSaveRow, folderBox, logBox);
        return box;
    }

    private ComboBox<LanguageOption> createLanguageComboBox() {
        ComboBox<LanguageOption> combo = new ComboBox<>();
        combo.getStyleClass().add("settings-combo");
        combo.getItems().addAll(
                new LanguageOption("de", "Deutsch"),
                new LanguageOption("en", "English"),
                new LanguageOption("ru", "Русский")
        );
        String currentLang = I18n.getCurrentLanguage();
        LanguageOption initial = combo.getItems().stream()
                .filter(opt -> opt.code().equalsIgnoreCase(currentLang))
                .findFirst()
                .orElse(combo.getItems().getFirst());
        combo.setValue(initial);

        combo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                I18n.setLanguage(newVal.code());
                settingsService.setLanguage(newVal.code());
                if (mainWindow != null) {
                    mainWindow.autoSave();
                }
            }
        });

        // Custom Cell Factory für Flagge + Text
        combo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(LanguageOption item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.displayName());
                    setGraphic(FlagIcon.create(item.code()));
                    setGraphicTextGap(10);
                }
            }
        });

        // Button Cell für die geschlossene Ansicht
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(LanguageOption item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.displayName());
                    setGraphic(FlagIcon.create(item.code()));
                    setGraphicTextGap(10);
                }
            }
        });

        return combo;
    }

    private VBox buildShortcutsView() {
        VBox box = new VBox(4);
        box.setPadding(new Insets(4, 0, 4, 0));

        // Tabellen-Header: HOTKEY | DESCRIPTION (ohne störendes Reload-Icon im Header)
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.getStyleClass().add("shortcut-table-header");

        Label colHotkey = new Label("HOTKEY");
        colHotkey.getStyleClass().add("shortcut-col-title");
        colHotkey.setMinWidth(140);
        colHotkey.setPrefWidth(140);

        Label colDesc = new Label("DESCRIPTION");
        colDesc.getStyleClass().add("shortcut-col-title");
        HBox.setHgrow(colDesc, Priority.ALWAYS);

        // Unsichtbarer Platzhalter für die Ausrichtung der Reset-Buttons der Zeilen
        Region spacerReset = new Region();
        spacerReset.setMinWidth(28);
        spacerReset.setPrefWidth(28);

        headerRow.getChildren().addAll(colHotkey, colDesc, spacerReset);

        // Tabellen-Zeilen im ScrollPane mit passender Dark-Scrollbar
        VBox rows = new VBox(2);
        rows.setPadding(new Insets(2, 4, 2, 0));
        for (ShortcutItem item : shortcutService.getAllShortcuts()) {
            rows.getChildren().add(createShortcutRow(item));
        }

        ScrollPane scrollPane = new ScrollPane(rows);
        scrollPane.getStyleClass().add("settings-scrollpane");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        box.getChildren().addAll(headerRow, scrollPane);
        return box;
    }

    private HBox createShortcutRow(ShortcutItem item) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("shortcut-table-row");

        // 1. Spalte: Hotkey Button (ohne Umrandung, nur feiner Hover-Effekt)
        Button btnKey = new Button(shortcutService.getDisplayText(item.action()));
        btnKey.getStyleClass().add("shortcut-key-badge");
        btnKey.setCursor(Cursor.HAND);
        btnKey.setMinWidth(140);
        btnKey.setPrefWidth(140);

        // 2. Spalte: Description (Icon + Text)
        HBox descBox = new HBox(8);
        descBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(descBox, Priority.ALWAYS);

        LucideIcon icon = new LucideIcon(item.action().getIconName(), 14, StudioTheme.COLOR_TEXT_HIGH);
        Label lblDesc = new Label(item.action().getLabel());
        lblDesc.getStyleClass().add("shortcut-description");

        descBox.getChildren().addAll(icon, lblDesc);

        // 3. Spalte: Reset-Button
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
            if (mainWindow != null) {
                mainWindow.autoSave();
            }
        });

        // Klick auf Hotkey startet Aufnahme-Modus
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
        if (mainWindow != null) {
            mainWindow.autoSave();
        }
    }

    private HBox buildFooter() {
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(8, 0, 0, 0));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        this.btnDone = new Button(I18n.get("action.close"));
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
     * Aktualisiert alle Beschriftungen reaktiv bei Sprachwechsel.
     */
    public void updateLocalization() {
        if (titleText != null) titleText.setText(I18n.get("settings.title"));
        if (btnTabGeneral != null) btnTabGeneral.setText(I18n.get("settings.tab.general"));
        if (btnTabShortcuts != null) btnTabShortcuts.setText(I18n.get("settings.tab.shortcuts"));
        if (lblLang != null) lblLang.setText(I18n.get("settings.language"));
        if (cbFullscreen != null) cbFullscreen.setText(I18n.get("settings.fullscreen"));
        if (cbConfirmExit != null) cbConfirmExit.setText(I18n.get("settings.confirm_exit"));
        if (lblAutoSave != null) lblAutoSave.setText(I18n.get("settings.autosave"));
        if (lblFolder != null) lblFolder.setText(I18n.get("settings.default_folder"));
        if (btnBrowse != null) btnBrowse.setText(I18n.get("settings.browse"));
        if (lblLog != null) lblLog.setText(I18n.get("settings.diagnostics"));
        if (btnOpenLog != null) btnOpenLog.setText(I18n.get("settings.open_log_folder"));
        if (btnCopyLog != null) btnCopyLog.setText(I18n.get("settings.copy_path"));
        if (btnDone != null) btnDone.setText(I18n.get("action.close"));
    }

    /**
     * Liefert die Dialog-Karte als JavaFX Node für das Overlay.
     */
    public Node getView() {
        return card;
    }

    public Button getBtnClose() {
        return btnClose;
    }
}

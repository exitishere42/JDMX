package de.exit.jdmx.ui;

import de.exit.jdmx.core.model.ChaserDefinition;
import de.exit.jdmx.core.model.ChaserDirection;
import de.exit.jdmx.core.model.ChaserStep;
import de.exit.jdmx.core.model.ExplorerItem;
import de.exit.jdmx.core.model.ExplorerItemType;
import de.exit.jdmx.core.model.FixtureDefinition;
import de.exit.jdmx.core.model.DmxMode;
import de.exit.jdmx.core.model.FixtureInstance;
import de.exit.jdmx.core.model.SceneDefinition;
import de.exit.jdmx.core.model.UniverseConfig;
import de.exit.jdmx.core.project.model.ChaserDataDto;
import de.exit.jdmx.core.project.model.ChaserStepDto;
import de.exit.jdmx.core.project.model.FixtureDataDto;
import de.exit.jdmx.core.project.model.GroupDataDto;
import de.exit.jdmx.core.project.model.ProjectData;
import de.exit.jdmx.core.project.model.SceneDataDto;
import de.exit.jdmx.core.project.model.UniverseDataDto;
import de.exit.jdmx.service.DmxService;
import de.exit.jdmx.service.ProjectStorageService;
import de.exit.jdmx.service.ProjectExplorerService;
import de.exit.jdmx.service.ShortcutAction;
import de.exit.jdmx.service.FixtureLibraryService;
import de.exit.jdmx.service.HardwareDiscoveryService;
import de.exit.jdmx.service.ShortcutService;
import de.exit.jdmx.service.UndoRedoService;
import de.exit.jdmx.ui.desk.LightDeskWorkspaceView;
import de.exit.jdmx.ui.dialog.AboutUpdateDialog;
import de.exit.jdmx.ui.dialog.FixturePatchDialog;
import de.exit.jdmx.ui.dialog.SettingsDialog;
import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.io.IOWorkspaceView;
import de.exit.jdmx.ui.io.UniversePatchTable;
import de.exit.jdmx.ui.stage.StageFixtureNode;
import de.exit.jdmx.ui.stage.StageCanvasView;
import de.exit.jdmx.ui.stage.StageWorkspaceView;
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
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

    private static final Logger log = LoggerFactory.getLogger(MainWindow.class);

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

    /** Service zur Projekt-Persistierung (*.jdmx). */
    private final ProjectStorageService projectStorageService = new ProjectStorageService();

    /** Dienst zur Verwaltung von Undo- und Redo-Schritten. */
    private final UndoRedoService undoRedoService = new UndoRedoService();

    /** Aktuelle Projektdatei (default.jdmx oder geladene/gespeicherte Datei). */
    private File currentProjectFile;

    /** Referenz auf den Stage-Workspace (Tab 1). */
    private StageWorkspaceView stageWorkspaceView;

    /** Referenz auf den LightDesk-Workspace (Tab 2). */
    private LightDeskWorkspaceView lightDeskWorkspaceView;

    /** Referenz auf den IO-Workspace (Tab 5). */
    private IOWorkspaceView ioWorkspaceView;

    /** Scheinwerfer-Bibliothek (vorinstallierte Standardgeräte und importierte Profile). */
    private final FixtureLibraryService fixtureLibraryService;

    /** Zentraler DMX- und Art-Net-Service der Core-Schicht. */
    private final DmxService dmxService;

    /**
     * Erstellt das Hauptfenster und initialisiert alle Basis-Komponenten.
     */
    public MainWindow() {
        this(new ShortcutService(), new DmxService());
    }

    /**
     * Erstellt das Hauptfenster mit einem spezifischen ShortcutService.
     */
    public MainWindow(ShortcutService shortcutService) {
        this(shortcutService, new DmxService());
    }

    /**
     * Erstellt das Hauptfenster mit ShortcutService und DmxService.
     *
     * @param shortcutService Shortcut-Dienst
     * @param dmxService      DMX- und Art-Net-Dienst
     */
    public MainWindow(ShortcutService shortcutService, DmxService dmxService) {
        this.shortcutService = Objects.requireNonNull(shortcutService, "shortcutService must not be null");
        this.dmxService = Objects.requireNonNull(dmxService, "dmxService must not be null");
        this.fixtureLibraryService = new FixtureLibraryService();
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
        hudPane.setAlignment(Pos.CENTER);

        getChildren().addAll(contentPane, overlayPane, hudPane);

        // Stage-Initialisierung für sauberes Fullscreen-Handling
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((wObs, oldWin, newWin) -> {
                    if (newWin instanceof javafx.stage.Stage stage) {
                        stage.setFullScreenExitHint("");
                        stage.setFullScreenExitKeyCombination(javafx.scene.input.KeyCombination.NO_MATCH);
                        stage.setOnCloseRequest(e -> {
                            if (currentProjectFile != null) {
                                autoSave();
                            }
                        });
                        stage.fullScreenProperty().addListener((fObs, wasFull, isFull) -> {
                            if (Boolean.TRUE.equals(isFull)) {
                                showFullscreenHint();
                            } else {
                                hudPane.getChildren().clear();
                            }
                        });
                    }
                });
            }
        });

        // 6. Header-Events verdrahten
        headerBar.setOnTabChange(this::switchToTab);
        headerBar.setOnBlackout(active -> {
            if (active) {
                dmxService.blackout();
            }
        });
        headerBar.setOnStopAll(() -> {
            dmxService.blackout();
        });

        // Live DMX-Übertragungsrate an Header-Pill koppeln
        dmxService.setOnRateUpdated(rate -> javafx.application.Platform.runLater(() -> headerBar.setDmxRate(rate)));

        // Brand Action-Menü Events
        headerBar.getActionMenu().setOnAction("new_project", this::newProject);
        headerBar.getActionMenu().setOnAction("open_file", this::handleOpenProject);
        headerBar.getActionMenu().setOnAction("save", this::handleSaveProject);
        headerBar.getActionMenu().setOnAction("save_as", this::handleSaveAsProject);
        headerBar.getActionMenu().setOnAction("fullscreen", this::toggleFullscreen);

        headerBar.getActionMenu().setOnAction("settings", () -> {
            SettingsDialog.show(this);
        });

        headerBar.getActionMenu().setOnAction("check_updates", () -> {
            AboutUpdateDialog.show(this);
        });

        // Undo & Redo Verwaltung und Synchronisation mit Kopfzeile und Aktionsmenü
        headerBar.setOnUndo(this::undo);
        headerBar.setOnRedo(this::redo);
        headerBar.getActionMenu().setOnAction("undo", this::undo);
        headerBar.getActionMenu().setOnAction("redo", this::redo);
        undoRedoService.addChangeListener(() -> {
            javafx.application.Platform.runLater(() -> {
                headerBar.updateUndoRedoState(undoRedoService.canUndo(), undoRedoService.canRedo());
            });
        });

        // 7. Globale Tastenkürzel und Overlay-Steuerung
        addEventFilter(KeyEvent.KEY_PRESSED, this::handleGlobalKeyEvent);

        // Initialen Tab anzeigen (Tab 0 = Stage & Fixtures)
        switchToTab(0);

        // Initiales leeres Projekt starten (ohne default.jdmx)
        newProject();
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
        this.stageWorkspaceView = new StageWorkspaceView(dmxService);
        stageWorkspaceView.getProjectExplorerView().setMainWindow(this);
        stageWorkspaceView.getProjectExplorerView().getAddMenu().setOnAction("fixture", () -> {
            FixturePatchDialog.show(this, fixtureLibraryService, stageWorkspaceView.getExplorerService(), stageWorkspaceView.getStageCanvasView());
        });
        stageWorkspaceView.getStageCanvasView().setOnFixtureMoveStarted(this::recordUndoSnapshot);
        stageWorkspaceView.getStageCanvasView().setOnFixtureMoved((item, pt) -> autoSave());
        stageWorkspaceView.getStageCanvasView().setOnFixturePlaced((item, pt) -> autoSave());
        stageWorkspaceView.getStageCanvasView().setOnFixturesDeleted(() -> {
            recordUndoSnapshot();
            autoSave();
        });
        tabViews[0] = stageWorkspaceView;

        this.lightDeskWorkspaceView = new LightDeskWorkspaceView(dmxService, stageWorkspaceView.getExplorerService());
        tabViews[1] = lightDeskWorkspaceView;
        tabViews[2] = createTabPlaceholder("Button Matrix", "layout-grid", StudioTheme.COLOR_ICON_MATRIX);
        tabViews[3] = createTabPlaceholder("Show-Timeline", "audio-lines", StudioTheme.COLOR_ICON_TIMELINE);

        this.ioWorkspaceView = new IOWorkspaceView(new HardwareDiscoveryService(), this);
        ioWorkspaceView.getPatchTable().setOnConfigChanged(this::autoSave);
        tabViews[4] = ioWorkspaceView;
    }

    public DmxService getDmxService() {
        return dmxService;
    }

    public StageWorkspaceView getStageWorkspaceView() {
        return stageWorkspaceView;
    }

    public LightDeskWorkspaceView getLightDeskWorkspaceView() {
        return lightDeskWorkspaceView;
    }

    public FixtureLibraryService getFixtureLibraryService() {
        return fixtureLibraryService;
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
     * Beim Aktivieren wird JavaFX-seitig der träge 5s-Hinweis unterdrückt
     * und stattdessen die authentische Meldung schnell ein- und ausgeblendet (~1s).
     */
    public void toggleFullscreen() {
        if (getScene() != null && getScene().getWindow() instanceof javafx.stage.Stage stage) {
            stage.setFullScreenExitHint("");
            stage.setFullScreenExitKeyCombination(javafx.scene.input.KeyCombination.NO_MATCH);

            boolean newState = !stage.isFullScreen();
            stage.setFullScreen(newState);

            if (newState) {
                showFullscreenHint();
            } else {
                hudPane.getChildren().clear();
            }
        }
    }

    /**
     * Zeigt die Vollbild-Meldung ("Drücken Sie ESC, um den Vollbildmodus zu verlassen")
     * zentriert an, blendet sie aber deutlich schneller aus (~1s statt 5s im JavaFX-Standard).
     */
    public void showFullscreenHint() {
        headerBar.showStatus("Drücke ESC zum Verlassen", "maximize", Color.web("#38BDF8"), Duration.millis(3000));
    }

    /**
     * Zeigt eine unaufdringliche Status-Meldung im Header-Statusbereich an.
     *
     * @param message Der anzuzeigende Hinweistext
     */
    public void showHudNotification(String message) {
        if (message == null || message.isBlank()) {
            return;
        }

        String lower = message.toLowerCase();
        String iconName = "info";
        Color iconColor = Color.web("#E5E7EB");

        if (lower.contains("gespeichert")) {
            iconName = "check";
            iconColor = Color.web("#34D399"); // Grün
        } else if (lower.contains("geladen")) {
            iconName = "folder-open";
            iconColor = Color.web("#60A5FA"); // Blau
        } else if (lower.contains("erstellt")) {
            iconName = "file-plus";
            iconColor = Color.web("#FBBF24"); // Amber
        } else if (lower.contains("fehler")) {
            iconName = "alert-triangle";
            iconColor = Color.web("#F87171"); // Rot
        } else if (lower.contains("rückgängig")) {
            iconName = "undo-2";
            iconColor = Color.web("#A855F7"); // Violett / Purple
        } else if (lower.contains("wiederholen")) {
            iconName = "redo-2";
            iconColor = Color.web("#EC4899"); // Pink
        }

        headerBar.showStatus(message, iconName, iconColor, Duration.millis(2500));
    }

    /**
     * Setzt alle Services und Puffer (Szenen, Chaser, Scheinwerfer, DMX-Kanäle, Undo-Stack)
     * vollständig und rückstandslos zurück.
     */
    public void resetProjectState() {
        if (stageWorkspaceView != null) {
            if (stageWorkspaceView.getChaserService() != null) {
                stageWorkspaceView.getChaserService().clearChasers();
            }
            if (stageWorkspaceView.getSceneService() != null) {
                stageWorkspaceView.getSceneService().clearScenes();
            }
            stageWorkspaceView.getStageCanvasView().clearFixtures();
            stageWorkspaceView.getExplorerService().clearItems();
        }
        if (dmxService != null) {
            dmxService.blackout();
        }
        if (undoRedoService != null) {
            undoRedoService.clear();
        }
    }

    /**
     * Speichert das Projekt automatisch und geräuschlos im Hintergrund.
     * <p>
     * Ist dem aktuellen Projekt noch keine Datei zugeordnet (unbenannt),
     * findet kein Schreibvorgang statt (keine automatische default.jdmx).
     * </p>
     */
    public void autoSave() {
        if (currentProjectFile == null) {
            return;
        }
        saveProjectSilently(currentProjectFile);
    }

    /**
     * Schreibt die aktuellen Projektdaten ohne UI-Hinweis in eine Datei.
     *
     * @param file Zieldatei
     */
    public void saveProjectSilently(File file) {
        if (file == null) {
            return;
        }
        try {
            ProjectData data = buildCurrentProjectData();
            projectStorageService.saveProject(data, file);
            this.currentProjectFile = file;
        } catch (Exception ex) {
            log.warn("Fehler beim Speichern des Projekts nach " + file.getAbsolutePath(), ex);
        }
    }

    /**
     * Speichert das aktuelle Projekt (Strg+S). Wenn noch kein Dateiname existiert,
     * wird der Nutzer per 'Speichern unter...'-Dialog zur Namens- und Pfadwahl aufgefordert.
     */
    public void handleSaveProject() {
        if (currentProjectFile != null) {
            saveCurrentProject(currentProjectFile);
        } else {
            handleSaveAsProject();
        }
    }

    /**
     * Speichert das Projekt in die angegebene Datei, aktualisiert den Fenstertitel
     * und zeigt eine Bestätigung im HUD an.
     *
     * @param file Zieldatei
     */
    public void saveCurrentProject(File file) {
        saveProjectSilently(file);
        this.currentProjectFile = file;
        updateWindowTitle();
        showHudNotification("Projekt gespeichert: " + file.getName());
    }

    /**
     * Lädt ein Projekt aus einer Datei und aktualisiert die Benutzeroberfläche.
     * Vor dem Laden werden alte Szenen, Chaser und Scheinwerfer restlos bereinigt.
     *
     * @param file Zu ladende Projektdatei
     */
    public void loadProject(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        try {
            resetProjectState();
            ProjectData data = projectStorageService.loadProject(file);
            applyProjectData(data);
            this.currentProjectFile = file;
            updateWindowTitle();
            showHudNotification("Projekt geladen: " + file.getName());
        } catch (Exception ex) {
            log.error("Fehler beim Laden von " + file.getAbsolutePath(), ex);
            showHudNotification("Fehler beim Laden!");
        }
    }

    /**
     * Setzt die Universen und die Bühne auf ein leeres Neu-Projekt zurück (currentProjectFile = null).
     */
    public void newProject() {
        resetProjectState();
        for (int i = 0; i < 4; i++) {
            UniverseConfig defaultCfg = UniverseConfig.defaultFor(i);
            dmxService.setUniverseConfig(i, defaultCfg);
            if (ioWorkspaceView != null) {
                List<UniversePatchTable.UniverseRow> rows = ioWorkspaceView.getPatchTable().getUniverseRows();
                if (i < rows.size()) {
                    UniversePatchTable.UniverseRow row = rows.get(i);
                    row.setConfig(defaultCfg);
                    row.getInputDropdown().selectItem(defaultCfg.inputSource());
                    row.getOutputDropdown().selectItem(defaultCfg.outputTarget());
                }
            }
        }
        this.currentProjectFile = null;
        updateWindowTitle();
        showHudNotification("Neues Projekt erstellt");
    }

    public void handleSaveAsProject() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Projekt speichern unter...");
        File initialDir = (currentProjectFile != null && currentProjectFile.getParentFile() != null && currentProjectFile.getParentFile().exists())
                ? currentProjectFile.getParentFile()
                : projectStorageService.getProjectsDirectory();
        if (initialDir.exists()) {
            chooser.setInitialDirectory(initialDir);
        }
        if (currentProjectFile != null) {
            chooser.setInitialFileName(currentProjectFile.getName());
        }
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JDMX Projekt (*.jdmx)", "*.jdmx"));
        File chosen = chooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (chosen != null) {
            if (!chosen.getName().toLowerCase().endsWith(".jdmx")) {
                chosen = new File(chosen.getParentFile(), chosen.getName() + ".jdmx");
            }
            saveCurrentProject(chosen);
        }
    }

    public void handleOpenProject() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Projekt öffnen...");
        File initialDir = (currentProjectFile != null && currentProjectFile.getParentFile() != null && currentProjectFile.getParentFile().exists())
                ? currentProjectFile.getParentFile()
                : projectStorageService.getProjectsDirectory();
        if (initialDir.exists()) {
            chooser.setInitialDirectory(initialDir);
        }
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JDMX Projekt (*.jdmx)", "*.jdmx"));
        File chosen = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (chosen != null) {
            loadProject(chosen);
        }
    }

    /**
     * Synchronisiert den Titel des Hauptfensters mit dem aktuellen Projektnamen.
     */
    public void updateWindowTitle() {
        if (getScene() != null && getScene().getWindow() instanceof javafx.stage.Stage stage) {
            if (currentProjectFile != null) {
                stage.setTitle("jdmx - " + currentProjectFile.getName());
            } else {
                stage.setTitle(de.exit.jdmx.JdmxApplication.APP_TITLE);
            }
        }
    }

    /**
     * Liefert die aktuell geöffnete/gespeicherte Projektdatei oder {@code null},
     * wenn das Projekt noch unbenannt ist.
     */
    public File getCurrentProjectFile() {
        return currentProjectFile;
    }

    /**
     * Liefert den Projekt-Storage-Service.
     */
    public ProjectStorageService getProjectStorageService() {
        return projectStorageService;
    }

    /**
     * Erstellt ein Snapshot-Projektmodell aus dem aktuellen Systemzustand.
     *
     * @return Vollständige Projektdaten
     */
    public ProjectData buildCurrentProjectData() {
        List<UniverseDataDto> universes = new ArrayList<>();
        if (ioWorkspaceView != null) {
            for (UniversePatchTable.UniverseRow row : ioWorkspaceView.getPatchTable().getUniverseRows()) {
                UniverseConfig cfg = row.getConfig();
                String in = row.getInputDropdown().getSelectedItem();
                String out = row.getOutputDropdown().getSelectedItem();
                universes.add(UniverseDataDto.of(
                        row.getUniverseIndex(),
                        in != null ? in : cfg.inputSource(),
                        out != null ? out : cfg.outputTarget(),
                        cfg.artNetIp(),
                        cfg.artNetUniverse(),
                        cfg.artNetSubnet(),
                        cfg.artNetNet()
                ));
            }
        } else {
            for (int i = 0; i < 4; i++) {
                UniverseConfig cfg = dmxService.getUniverseConfig(i);
                universes.add(UniverseDataDto.of(
                        i,
                        cfg.inputSource(),
                        cfg.outputTarget(),
                        cfg.artNetIp(),
                        cfg.artNetUniverse(),
                        cfg.artNetSubnet(),
                        cfg.artNetNet()
                ));
            }
        }

        List<FixtureDataDto> fixtures = new ArrayList<>();
        if (stageWorkspaceView != null) {
            for (StageFixtureNode node : stageWorkspaceView.getStageCanvasView().getPlacedFixtures()) {
                ExplorerItem item = node.getItem();
                FixtureInstance inst = item != null ? item.getFixtureInstance() : null;
                if (inst != null) {
                    fixtures.add(new FixtureDataDto(
                            item.getId(),
                            item.getName(),
                            inst.getDefinition() != null ? inst.getDefinition().getId() : "",
                            inst.getDefinition() != null ? inst.getDefinition().getManufacturer() : "",
                            inst.getDefinition() != null ? inst.getDefinition().getModel() : "",
                            inst.getActiveMode() != null ? inst.getActiveMode().getName() : "",
                            inst.getUniverse(),
                            inst.getStartAddress(),
                            node.getLayoutX(),
                            node.getLayoutY()
                    ));
                }
            }
        }

        List<GroupDataDto> groupDtos = new ArrayList<>();
        if (stageWorkspaceView != null) {
            for (ExplorerItem groupItem : stageWorkspaceView.getExplorerService().getItemsInFolder(ProjectExplorerService.FOLDER_GROUPS_ID)) {
                groupDtos.add(new GroupDataDto(groupItem.getId(), groupItem.getName(), groupItem.getMemberFixtureIds()));
            }
        }

        List<SceneDataDto> sceneDtos = new ArrayList<>();
        if (stageWorkspaceView != null && stageWorkspaceView.getSceneService() != null) {
            for (SceneDefinition scene : stageWorkspaceView.getSceneService().getAllScenes()) {
                sceneDtos.add(new SceneDataDto(
                        scene.getId(),
                        scene.getName(),
                        scene.getDefaultFadeTimeMs(),
                        scene.getChannelValues()
                ));
            }
        }

        List<ChaserDataDto> chaserDtos = new ArrayList<>();
        if (stageWorkspaceView != null && stageWorkspaceView.getChaserService() != null) {
            for (ChaserDefinition chaser : stageWorkspaceView.getChaserService().getAllChasers()) {
                List<ChaserStepDto> stepDtos = new ArrayList<>();
                if (chaser.getSteps() != null) {
                    for (ChaserStep step : chaser.getSteps()) {
                        stepDtos.add(new ChaserStepDto(step.getStepId(), step.getSceneId(), step.getHoldTimeMs(), step.getFadeTimeMs()));
                    }
                }
                chaserDtos.add(new ChaserDataDto(
                        chaser.getId(),
                        chaser.getName(),
                        chaser.getBpm(),
                        chaser.getDirection() != null ? chaser.getDirection().name() : ChaserDirection.FORWARD.name(),
                        stepDtos
                ));
            }
        }

        String name = currentProjectFile != null ? currentProjectFile.getName().replace(".jdmx", "") : "Default Project";
        return new ProjectData(
                ProjectData.CURRENT_VERSION,
                name,
                java.time.Instant.now().toString(),
                universes,
                fixtures,
                groupDtos,
                sceneDtos,
                chaserDtos
        );
    }

    /**
     * Wendet geladene Projektdaten auf die DMX-Universen, die Bühne und den Explorer an.
     *
     * @param data Zu aktivierende Projektdaten
     */
    public void applyProjectData(ProjectData data) {
        if (data == null) {
            return;
        }

        // 1. Universen wiederherstellen
        if (data.universes() != null) {
            for (UniverseDataDto uDto : data.universes()) {
                UniverseConfig cfg = UniverseConfig.defaultFor(uDto.universeIndex())
                        .withInputSource(uDto.inputSource() != null ? uDto.inputSource() : "Kein Input")
                        .withOutputTarget(uDto.outputTarget() != null ? uDto.outputTarget() : "Kein Output")
                        .withArtNetIp(uDto.artNetIp() != null ? uDto.artNetIp() : "127.0.0.1")
                        .withArtNetUniverse(uDto.artNetUniverse())
                        .withArtNetSubnet(uDto.artNetSubnet());
                dmxService.setUniverseConfig(uDto.universeIndex(), cfg);

                if (ioWorkspaceView != null) {
                    List<UniversePatchTable.UniverseRow> rows = ioWorkspaceView.getPatchTable().getUniverseRows();
                    if (uDto.universeIndex() >= 0 && uDto.universeIndex() < rows.size()) {
                        UniversePatchTable.UniverseRow row = rows.get(uDto.universeIndex());
                        row.setConfig(cfg);
                        row.getInputDropdown().selectItem(cfg.inputSource());
                        row.getOutputDropdown().selectItem(cfg.outputTarget());
                    }
                }
            }
        }

        // 2. Fixtures wiederherstellen
        if (stageWorkspaceView != null && data.fixtures() != null) {
            StageCanvasView canvas = stageWorkspaceView.getStageCanvasView();
            ProjectExplorerService explorer = stageWorkspaceView.getExplorerService();
            canvas.clearFixtures();
            explorer.clearItems();

            for (FixtureDataDto fDto : data.fixtures()) {
                FixtureDefinition def = resolveFixtureDefinition(fDto);
                DmxMode mode = null;
                if (def != null && def.getModes() != null && !def.getModes().isEmpty()) {
                    mode = def.getModes().stream()
                            .filter(m -> m.getName().equalsIgnoreCase(fDto.modeName()))
                            .findFirst()
                            .orElse(def.getModes().get(0));
                }

                int targetUniverse = Math.max(1, fDto.universe());
                int targetAddress = Math.max(1, Math.min(512, fDto.startAddress() > 0 ? fDto.startAddress() : 1));

                FixtureInstance instance = new FixtureInstance(
                        fDto.id(),
                        fDto.name(),
                        def,
                        mode,
                        targetUniverse,
                        targetAddress,
                        fDto.stageX(),
                        fDto.stageY()
                );

                String iconName = (def != null && def.getModel() != null && def.getModel().toLowerCase().contains("dimmer"))
                        ? "sun" : "lamp";
                ExplorerItem item = new ExplorerItem(fDto.id(), fDto.name(), ExplorerItemType.FIXTURE, false, iconName, Color.web("#60A5FA"), instance);
                explorer.addItemToFolder(ProjectExplorerService.FOLDER_DEVICES_ID, item);
                canvas.placeFixture(item, fDto.stageX(), fDto.stageY());
            }
        }

        // 3. Gerätegruppen wiederherstellen
        if (data.groups() != null && stageWorkspaceView != null) {
            ProjectExplorerService explorer = stageWorkspaceView.getExplorerService();
            for (GroupDataDto gDto : data.groups()) {
                ExplorerItem group = new ExplorerItem(
                        gDto.id(),
                        gDto.name(),
                        ExplorerItemType.GROUP,
                        false,
                        "layers",
                        Color.web("#34D399")
                );
                if (gDto.fixtureIds() != null) {
                    group.setMemberFixtureIds(gDto.fixtureIds());
                }
                explorer.addItemToFolder(ProjectExplorerService.FOLDER_GROUPS_ID, group);
            }
        }

        // 4. Szenen wiederherstellen
        if (data.scenes() != null && stageWorkspaceView != null && stageWorkspaceView.getSceneService() != null) {
            ProjectExplorerService explorer = stageWorkspaceView.getExplorerService();
            for (SceneDataDto sDto : data.scenes()) {
                SceneDefinition sceneDef = new SceneDefinition(
                        sDto.id(),
                        sDto.name(),
                        (int) sDto.fadeTimeMs(),
                        sDto.channelValues()
                );
                stageWorkspaceView.getSceneService().registerScene(sceneDef);
                ExplorerItem item = new ExplorerItem(
                        sDto.id(),
                        sDto.name(),
                        ExplorerItemType.CUELIST,
                        false,
                        "bookmark",
                        StudioTheme.COLOR_PRIMARY_ORANGE
                );
                explorer.addItemToFolder(ProjectExplorerService.FOLDER_CUELISTS_ID, item);
            }
        }

        // 5. Chaser wiederherstellen
        if (data.chasers() != null && stageWorkspaceView != null && stageWorkspaceView.getChaserService() != null) {
            ProjectExplorerService explorer = stageWorkspaceView.getExplorerService();
            for (ChaserDataDto cDto : data.chasers()) {
                ChaserDirection dir = ChaserDirection.FORWARD;
                if (cDto.direction() != null) {
                    try {
                        dir = ChaserDirection.valueOf(cDto.direction());
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                ChaserDefinition chaserDef = new ChaserDefinition(cDto.id(), cDto.name(), cDto.bpm(), dir);
                if (cDto.steps() != null) {
                    for (ChaserStepDto stepDto : cDto.steps()) {
                        chaserDef.addStep(new ChaserStep(stepDto.stepId(), stepDto.sceneId(), (int) stepDto.holdTimeMs(), (int) stepDto.fadeTimeMs()));
                    }
                }
                stageWorkspaceView.getChaserService().registerChaser(chaserDef);
                ExplorerItem item = new ExplorerItem(
                        cDto.id(),
                        cDto.name(),
                        ExplorerItemType.CUELIST,
                        false,
                        "list-music",
                        Color.web("#A78BFA")
                );
                explorer.addItemToFolder(ProjectExplorerService.FOLDER_CUELISTS_ID, item);
            }
        }
    }

    private FixtureDefinition resolveFixtureDefinition(FixtureDataDto dto) {
        if (dto == null) {
            return null;
        }

        // 1. Suche nach exakter ID
        if (dto.definitionId() != null && !dto.definitionId().isBlank()) {
            Optional<FixtureDefinition> byId = fixtureLibraryService.getDefinitionById(dto.definitionId());
            if (byId.isPresent()) {
                return byId.get();
            }
        }

        // 2. Suche nach Modell & Hersteller
        if (dto.model() != null && !dto.model().isBlank()) {
            for (FixtureDefinition def : fixtureLibraryService.getAllDefinitions()) {
                if (def.getModel().equalsIgnoreCase(dto.model().trim())) {
                    if (dto.manufacturer() == null || dto.manufacturer().isBlank()
                            || def.getManufacturer().equalsIgnoreCase(dto.manufacturer().trim())) {
                        return def;
                    }
                }
            }
        }

        // 3. Fallback auf Suchergebnis oder erstes verfügbares Profil
        List<FixtureDefinition> matches = fixtureLibraryService.search(dto.model() != null && !dto.model().isBlank() ? dto.model() : dto.name());
        if (!matches.isEmpty()) {
            return matches.get(0);
        }

        return fixtureLibraryService.getAllDefinitions().stream().findFirst().orElse(null);
    }

    /**
     * Abwärtskompatible Methode für vorhandene Aufrufe.
     */
    public void showFullscreenHud() {
        showFullscreenHint();
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
            undo();
            event.consume();
            return;
        }
        if (shortcutService.matches(ShortcutAction.REDO, event)) {
            redo();
            event.consume();
            return;
        }
    }

    /**
     * Zeichnet den aktuellen Projektzustand auf dem Undo-Stack auf.
     */
    public void recordUndoSnapshot() {
        if (undoRedoService != null) {
            undoRedoService.pushUndoState(buildCurrentProjectData());
        }
    }

    /**
     * Macht den letzten Bearbeitungsschritt rückgängig.
     */
    public void undo() {
        if (!undoRedoService.canUndo()) {
            return;
        }
        ProjectData current = buildCurrentProjectData();
        ProjectData prev = undoRedoService.undo(current);
        if (prev != null) {
            applyProjectData(prev);
            if (currentProjectFile != null) {
                saveProjectSilently(currentProjectFile);
            }
            showHudNotification("Rückgängig ausgeführt");
        }
    }

    /**
     * Stellt den zuletzt rückgängig gemachten Bearbeitungsschritt wieder her.
     */
    public void redo() {
        if (!undoRedoService.canRedo()) {
            return;
        }
        ProjectData current = buildCurrentProjectData();
        ProjectData next = undoRedoService.redo(current);
        if (next != null) {
            applyProjectData(next);
            if (currentProjectFile != null) {
                saveProjectSilently(currentProjectFile);
            }
            showHudNotification("Wiederholen ausgeführt");
        }
    }

    /**
     * Liefert den zentralen UndoRedoService des Hauptfensters.
     */
    public UndoRedoService getUndoRedoService() {
        return undoRedoService;
    }

    /**
     * Liefert den zentralen ShortcutService des Hauptfensters.
     */
    public ShortcutService getShortcutService() {
        return shortcutService;
    }
}

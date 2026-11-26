package de.exit.jdmx.ui.timeline;

import de.exit.jdmx.core.i18n.I18n;
import de.exit.jdmx.core.log.AppLogger;
import de.exit.jdmx.core.model.ExplorerItem;
import de.exit.jdmx.core.model.ExplorerItemType;
import de.exit.jdmx.core.model.SceneDefinition;
import de.exit.jdmx.service.ProjectExplorerService;
import de.exit.jdmx.service.SceneService;
import de.exit.jdmx.service.TimelineService;
import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.theme.StudioTheme;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Rechte Seitenleiste der Show-Timeline (Tab 4).
 * <p>
 * Enthält einen kompakten Tab-Header mit:
 * <ul>
 *     <li>Dem [Inspektor]-Tab zur Anzeige und Konfiguration selektierter Elemente</li>
 *     <li>Dem [Explorer]-Tab mit TreeView zur Strukturierung importierter Audio-Songs</li>
 *     <li>Dem [+]-Button (im Stage-Stil ohne Outline), der per Dropdown neue Songs importieren kann</li>
 * </ul>
 * </p>
 */
public class ShowTimelineExplorerView extends VBox {

    /** Standard-Mindestbreite der Seitenleiste in Pixeln (identisch zu ProjectExplorerView im Stage-Tab: 300px). */
    public static final double DEFAULT_MIN_WIDTH = 300.0;

    /** Standard-Vorzugsbreite der Seitenleiste in Pixeln (identisch zu ProjectExplorerView im Stage-Tab: 300px). */
    public static final double DEFAULT_PREF_WIDTH = 300.0;

    /** Icon-Größe für Einträge in der TreeView. */
    public static final double ITEM_ICON_SIZE = 16.0;

    /** Ordner-ID für den Songs-Ordner in der TreeView. */
    public static final String FOLDER_SONGS_ID = "timeline-folder-songs";

    /** Ordner-ID für den Szenen-Ordner in der TreeView. */
    public static final String FOLDER_SCENES_ID = "timeline-folder-scenes";

    private final TimelineService timelineService;
    private final ProjectExplorerService explorerService;
    private final SceneService sceneService;

    private final Button btnTabInspector;
    private final Button btnTabExplorer;
    private final Button btnAdd;
    private final ShowTimelineAddMenu addMenu;

    private final StackPane contentStack;
    private final TimelineInspectorView inspectorView;
    private final TreeView<ExplorerItem> treeView;
    private final TreeItem<ExplorerItem> rootItem;
    private TreeItem<ExplorerItem> songsFolderItem;
    private TreeItem<ExplorerItem> scenesFolderItem;

    /**
     * Erstellt den Show-Timeline Explorer mit Standard-TimelineService und leerem ProjectExplorerService.
     */
    public ShowTimelineExplorerView() {
        this(new TimelineService(), new ProjectExplorerService(), null);
    }

    /**
     * Erstellt den Show-Timeline Explorer mit dem angegebenen TimelineService.
     *
     * @param service der {@link TimelineService} (darf nicht {@code null} sein)
     */
    public ShowTimelineExplorerView(TimelineService service) {
        this(service, new ProjectExplorerService(), null);
    }

    /**
     * Erstellt den Show-Timeline Explorer mit TimelineService, ProjectExplorerService und SceneService.
     *
     * @param service         der {@link TimelineService} (darf nicht {@code null} sein)
     * @param explorerService der {@link ProjectExplorerService} zur Szenen-Synchronisation
     * @param sceneService    der {@link SceneService} zur Anzeige und Ausführung aller angelegten Szenen
     */
    public ShowTimelineExplorerView(TimelineService service, ProjectExplorerService explorerService, SceneService sceneService) {
        super(0);
        this.timelineService = Objects.requireNonNull(service, "service must not be null");
        this.explorerService = explorerService != null ? explorerService : new ProjectExplorerService();
        this.sceneService = sceneService;

        getStyleClass().add("timeline-explorer-view");
        setMinWidth(DEFAULT_MIN_WIDTH);
        setPrefWidth(DEFAULT_PREF_WIDTH);

        // --- Add-Menü & Aktionen ---
        this.addMenu = new ShowTimelineAddMenu();
        addMenu.setOnAction("song", this::handleImportSong);

        // --- [+] Button (ohne Outline im Stage-Stil) ---
        this.btnAdd = new Button();
        btnAdd.getStyleClass().add("timeline-explorer-add-btn");
        btnAdd.setGraphic(new LucideIcon("plus", 13, StudioTheme.COLOR_ICON_TIMELINE));
        btnAdd.setTooltip(new javafx.scene.control.Tooltip(I18n.get("timeline.explorer.add")));
        btnAdd.setOnAction(e -> {
            showExplorerTab();
            if (addMenu.isShowing()) {
                addMenu.safeHide();
            } else {
                Bounds bounds = btnAdd.localToScreen(btnAdd.getBoundsInLocal());
                if (bounds != null && btnAdd.getScene() != null && btnAdd.getScene().getWindow() != null) {
                    double menuX = Math.max(0, bounds.getMaxX() - 140);
                    addMenu.show(btnAdd.getScene().getWindow(), menuX, bounds.getMaxY() + 4);
                }
            }
        });

        // --- Tab-Buttons (kompakt) ---
        this.btnTabInspector = new Button(I18n.get("timeline.explorer.tab.inspector"));
        btnTabInspector.getStyleClass().add("timeline-explorer-tab-btn");
        btnTabInspector.setOnAction(e -> showInspectorTab());

        this.btnTabExplorer = new Button(I18n.get("timeline.explorer.tab.explorer"));
        btnTabExplorer.getStyleClass().addAll("timeline-explorer-tab-btn", "active");
        btnTabExplorer.setOnAction(e -> showExplorerTab());

        // --- Tab-Header (HBox) ---
        HBox header = new HBox(4);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("timeline-explorer-header");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(btnTabInspector, btnTabExplorer, spacer, btnAdd);

        // --- 1. Inspektor-Ansicht ---
        this.inspectorView = new TimelineInspectorView();
        inspectorView.setVisible(false);
        inspectorView.setManaged(false);

        // --- 2. TreeView (Songs- und Szenen-Ordner) ---
        ExplorerItem rootObj = new ExplorerItem(
                "timeline-root", "Timeline", ExplorerItemType.FOLDER, false, "audio-lines",
                StudioTheme.COLOR_ICON_TIMELINE
        );
        this.rootItem = new TreeItem<>(rootObj);
        rootItem.setExpanded(true);

        ExplorerItem songsFolder = new ExplorerItem(
                FOLDER_SONGS_ID,
                I18n.get("timeline.explorer.folder.songs"),
                ExplorerItemType.FOLDER,
                true,
                "music",
                StudioTheme.COLOR_ICON_TIMELINE
        );
        this.songsFolderItem = new TreeItem<>(songsFolder, new LucideIcon("music", ITEM_ICON_SIZE, StudioTheme.COLOR_ICON_TIMELINE));
        this.songsFolderItem.setExpanded(true);

        ExplorerItem scenesFolder = new ExplorerItem(
                FOLDER_SCENES_ID,
                I18n.get("timeline.explorer.folder.scenes"),
                ExplorerItemType.FOLDER,
                true,
                "bookmark",
                Color.web("#A78BFA")
        );
        this.scenesFolderItem = new TreeItem<>(scenesFolder, new LucideIcon("bookmark", ITEM_ICON_SIZE, Color.web("#A78BFA")));
        this.scenesFolderItem.setExpanded(true);

        this.rootItem.getChildren().setAll(songsFolderItem, scenesFolderItem);

        this.treeView = new TreeView<>(rootItem);
        treeView.setShowRoot(false);
        treeView.getStyleClass().add("project-explorer-tree");
        treeView.setCellFactory(tv -> new TimelineExplorerTreeCell(this));
        VBox.setVgrow(treeView, Priority.ALWAYS);

        buildTree();

        // StackPane für Umschaltung zwischen Inspektor und TreeView
        this.contentStack = new StackPane(treeView, inspectorView);
        VBox.setVgrow(contentStack, Priority.ALWAYS);

        // Externes Drag & Drop für Audiodateien direkt in den Explorer
        setupExternalDragAndDrop();

        // Auf Änderungen im TimelineService lauschen (z. B. importierte Songs)
        timelineService.addChangeListener(this::scheduleBuildTree);

        // Auf Änderungen im ProjectExplorerService lauschen (z. B. neue/umbenannte/gelöschte Szenen)
        if (this.explorerService != null) {
            this.explorerService.addChangeListener(this::scheduleBuildTree);
        }

        // Auf Änderungen im SceneService lauschen (z. B. Szenen-Snapshots)
        if (this.sceneService != null) {
            this.sceneService.addChangeListener(this::scheduleBuildTree);
        }

        // Sprachwechsel reaktiv
        I18n.addLanguageChangeListener(lang -> {
            if (Platform.isFxApplicationThread()) {
                updateLocalization();
            } else {
                Platform.runLater(this::updateLocalization);
            }
        });

        getChildren().addAll(header, contentStack);
    }

    /**
     * Öffnet den Dateidialog zum Importieren einer Audiodatei (MP3/WAV).
     */
    public void handleImportSong() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.get("timeline.song.import_title"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                I18n.get("timeline.song.audio_filter"), "*.mp3", "*.wav"
        ));
        File chosen = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (chosen != null) {
            timelineService.importSong(chosen);
        }
    }

    private void scheduleBuildTree() {
        if (getScene() != null && !Platform.isFxApplicationThread()) {
            Platform.runLater(this::buildTree);
        } else {
            buildTree();
        }
    }

    /**
     * Baut die TreeView mit dem Songs- und Szenen-Standardordner auf.
     */
    public void buildTree() {
        // --- 1. Songs aktualisieren ---
        List<TreeItem<ExplorerItem>> songTreeItems = new java.util.ArrayList<>();
        for (File song : timelineService.getImportedSongs()) {
            ExplorerItem songItem = new ExplorerItem(
                    "song-" + song.getName(),
                    song.getName(),
                    ExplorerItemType.SONG,
                    false,
                    "music",
                    StudioTheme.COLOR_ICON_TIMELINE
            );
            songItem.setSongFile(song);
            LucideIcon songIcon = new LucideIcon("music", ITEM_ICON_SIZE, StudioTheme.COLOR_ICON_TIMELINE);
            songTreeItems.add(new TreeItem<>(songItem, songIcon));
        }
        songsFolderItem.getChildren().setAll(songTreeItems);

        // --- 2. Szenen aktualisieren ---
        List<TreeItem<ExplorerItem>> sceneTreeItems = new java.util.ArrayList<>();
        Set<String> addedSceneIds = new HashSet<>();

        // A) Szenen aus ProjectExplorerService (Ordner Szenenlisten / Cuelists)
        if (explorerService != null) {
            List<ExplorerItem> cuelistItems = explorerService.getItemsInFolder(ProjectExplorerService.FOLDER_CUELISTS_ID);
            for (ExplorerItem item : cuelistItems) {
                if (item != null && (item.getType() == ExplorerItemType.CUELIST || "bookmark".equals(item.getIconName()))) {
                    addedSceneIds.add(item.getId());
                    LucideIcon sceneIcon = new LucideIcon("bookmark", ITEM_ICON_SIZE, Color.web("#A78BFA"));
                    sceneTreeItems.add(new TreeItem<>(item, sceneIcon));
                }
            }
        }

        // B) Szenen aus SceneService (falls direkt erfasst oder registriert)
        if (sceneService != null) {
            for (SceneDefinition sceneDef : sceneService.getAllScenes()) {
                if (sceneDef != null && !addedSceneIds.contains(sceneDef.getId())) {
                    addedSceneIds.add(sceneDef.getId());
                    ExplorerItem sceneItem = new ExplorerItem(
                            sceneDef.getId(),
                            sceneDef.getName(),
                            ExplorerItemType.CUELIST,
                            false,
                            "bookmark",
                            Color.web("#A78BFA")
                    );
                    LucideIcon sceneIcon = new LucideIcon("bookmark", ITEM_ICON_SIZE, Color.web("#A78BFA"));
                    sceneTreeItems.add(new TreeItem<>(sceneItem, sceneIcon));
                }
            }
        }
        scenesFolderItem.getChildren().setAll(sceneTreeItems);

        if (!rootItem.getChildren().contains(songsFolderItem) || !rootItem.getChildren().contains(scenesFolderItem)) {
            rootItem.getChildren().setAll(songsFolderItem, scenesFolderItem);
        }
    }

    private void setupExternalDragAndDrop() {
        setOnDragOver(e -> {
            if (TimelineDragHelper.canAcceptDrag(e.getDragboard())) {
                e.acceptTransferModes(javafx.scene.input.TransferMode.COPY_OR_MOVE);
            }
            e.consume();
        });

        setOnDragDropped(e -> {
            java.util.List<File> files = TimelineDragHelper.extractAudioFiles(e.getDragboard(), timelineService);
            if (!files.isEmpty()) {
                for (File f : files) {
                    AppLogger.info("[TIMELINE-EXPLORER] Externe Audiodatei via DND importiert: %s", f.getAbsolutePath());
                    timelineService.importSong(f);
                }
                e.setDropCompleted(true);
            } else {
                AppLogger.warn("[TIMELINE-EXPLORER] Externes DND fehlgeschlagen: Keine gültigen Audiodateien im Dragboard.");
            }
            e.consume();
        });
    }

    /**
     * Weist ein Song-ExplorerItem einer Spur der Timeline zu.
     * Falls {@code trackId} {@code null} ist, wird bevorzugt die erste leere Spur gewählt oder Spur 0.
     *
     * @param item    das Song-Item
     * @param trackId Zielspur-ID oder {@code null} für automatische Wahl
     * @return {@code true}, wenn erfolgreich zugewiesen
     */
    public boolean assignSongToTimeline(ExplorerItem item, String trackId) {
        if (item == null) return false;
        AppLogger.info("[TIMELINE-EXPLORER] assignSongToTimeline: item='%s', trackId='%s'", item.getName(), trackId);

        File songFile = item.getSongFile();
        if (songFile == null) {
            java.util.Optional<File> opt = timelineService.findSongFileByName(item.getName());
            if (opt.isPresent()) {
                songFile = opt.get();
            }
        }
        if (songFile == null) {
            AppLogger.warn("[TIMELINE-EXPLORER] Zuweisung fehlgeschlagen: Keine physische Datei für '%s' gefunden.", item.getName());
            return false;
        }

        java.util.List<de.exit.jdmx.core.model.TimelineTrack> tracks = timelineService.getTracks();
        if (tracks.isEmpty()) {
            de.exit.jdmx.core.model.TimelineTrack newTrack = timelineService.addTrack();
            return timelineService.assignSongToTrack(newTrack.getId(), songFile);
        }

        if (trackId != null) {
            return timelineService.assignSongToTrack(trackId, songFile);
        }

        // Erste Spur ohne Song suchen, sonst Spur 0
        de.exit.jdmx.core.model.TimelineTrack target = tracks.stream()
                .filter(t -> !t.hasSong())
                .findFirst()
                .orElse(tracks.get(0));

        return timelineService.assignSongToTrack(target.getId(), songFile);
    }

    /**
     * Aktiviert die Inspektor-Ansicht.
     */
    public void showInspectorTab() {
        btnTabExplorer.getStyleClass().remove("active");
        if (!btnTabInspector.getStyleClass().contains("active")) {
            btnTabInspector.getStyleClass().add("active");
        }
        treeView.setVisible(false);
        treeView.setManaged(false);
        inspectorView.setVisible(true);
        inspectorView.setManaged(true);
    }

    /**
     * Aktiviert die Explorer-Ansicht.
     */
    public void showExplorerTab() {
        btnTabInspector.getStyleClass().remove("active");
        if (!btnTabExplorer.getStyleClass().contains("active")) {
            btnTabExplorer.getStyleClass().add("active");
        }
        inspectorView.setVisible(false);
        inspectorView.setManaged(false);
        treeView.setVisible(true);
        treeView.setManaged(true);
    }

    /**
     * Prüft, ob der Inspektor-Tab aktiv ist.
     *
     * @return {@code true}, wenn Inspektor aktiv
     */
    public boolean isInspectorTabActive() {
        return inspectorView.isVisible();
    }

    /**
     * Aktualisiert Texte und Tooltips bei Sprachwechsel.
     */
    public void updateLocalization() {
        btnTabInspector.setText(I18n.get("timeline.explorer.tab.inspector"));
        btnTabExplorer.setText(I18n.get("timeline.explorer.tab.explorer"));
        btnAdd.setTooltip(new javafx.scene.control.Tooltip(I18n.get("timeline.explorer.add")));
        addMenu.updateLocalization();
        buildTree();
    }

    /**
     * Gibt den Inspektor-Tab-Button zurück.
     *
     * @return der Tab-Button
     */
    public Button getBtnTabInspector() {
        return btnTabInspector;
    }

    /**
     * Gibt den Explorer-Tab-Button zurück.
     *
     * @return der Tab-Button
     */
    public Button getBtnTabExplorer() {
        return btnTabExplorer;
    }

    /**
     * Gibt den [+]-Button im Tab-Header zurück.
     *
     * @return der Add-Button
     */
    public Button getBtnAdd() {
        return btnAdd;
    }

    /**
     * Gibt das Add-Dropdown-Menü zurück.
     *
     * @return das {@link ShowTimelineAddMenu}
     */
    public ShowTimelineAddMenu getAddMenu() {
        return addMenu;
    }

    /**
     * Gibt die Inspektor-Ansicht zurück.
     *
     * @return der {@link TimelineInspectorView}
     */
    public TimelineInspectorView getInspectorView() {
        return inspectorView;
    }

    /**
     * Gibt die TreeView zurück.
     *
     * @return die Baum-Ansicht
     */
    public TreeView<ExplorerItem> getTreeView() {
        return treeView;
    }

    /**
     * Gibt den Root-Knoten der TreeView zurück.
     *
     * @return der Root-TreeItem
     */
    public TreeItem<ExplorerItem> getRootItem() {
        return rootItem;
    }

    /**
     * Gibt den Songs-Ordner-TreeItem zurück.
     *
     * @return der Songs-Ordner-TreeItem
     */
    public TreeItem<ExplorerItem> getSongsFolderItem() {
        return songsFolderItem;
    }

    /**
     * Gibt den Szenen-Ordner-TreeItem zurück.
     *
     * @return der Szenen-Ordner-TreeItem
     */
    public TreeItem<ExplorerItem> getScenesFolderItem() {
        return scenesFolderItem;
    }

    /**
     * Gibt den ProjectExplorerService zurück.
     *
     * @return der {@link ProjectExplorerService}
     */
    public ProjectExplorerService getExplorerService() {
        return explorerService;
    }

    /**
     * Gibt den SceneService zurück.
     *
     * @return der {@link SceneService} oder {@code null}
     */
    public SceneService getSceneService() {
        return sceneService;
    }

    /**
     * Gibt den zugrunde liegenden TimelineService zurück.
     *
     * @return der {@link TimelineService}
     */
    public TimelineService getTimelineService() {
        return timelineService;
    }

    /**
     * TreeCell für den Timeline Explorer mit korrekter Disclosure-Node Zentrierung,
     * nativer Drag & Drop Unterstützung für Songs, Doppelklick-Zuweisung und Kontextmenü.
     */
    public static class TimelineExplorerTreeCell extends javafx.scene.control.TreeCell<ExplorerItem> {

        private final ShowTimelineExplorerView parentView;
        private javafx.scene.Node defaultDisclosureNode;

        public TimelineExplorerTreeCell() {
            this(null);
        }

        public TimelineExplorerTreeCell(ShowTimelineExplorerView parentView) {
            this.parentView = parentView;
            setupDragAndDrop();
            setupClickHandler();
            setupContextMenu();
        }

        private void setupDragAndDrop() {
            setOnDragDetected(event -> {
                if (isEmpty() || getItem() == null) return;
                ExplorerItem item = getItem();
                if (item.isStandardFolder() || item.getType() == ExplorerItemType.FOLDER) return;

                File songFile = item.getSongFile();
                if (songFile == null && parentView != null) {
                    songFile = parentView.getTimelineService().findSongFileByName(item.getName()).orElse(null);
                }

                javafx.scene.input.Dragboard db = startDragAndDrop(javafx.scene.input.TransferMode.COPY_OR_MOVE);
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.put(de.exit.jdmx.ui.stage.ProjectExplorerView.EXPLORER_ITEM_FORMAT, item.getId());
                content.putString(item.getName());

                if (songFile != null && songFile.exists()) {
                    content.putFiles(java.util.List.of(songFile));
                    try {
                        content.putUrl(songFile.toURI().toString());
                    } catch (Exception ignored) {}
                    content.putString(songFile.getAbsolutePath());
                    AppLogger.info("[TIMELINE-EXPLORER] Drag gestartet für Song: %s (%s)", item.getName(), songFile.getAbsolutePath());
                } else {
                    AppLogger.info("[TIMELINE-EXPLORER] Drag gestartet für Item: %s", item.getName());
                }

                db.setContent(content);
                event.consume();
            });
        }

        private void setupClickHandler() {
            setOnMouseClicked(event -> {
                if (isEmpty() || getItem() == null) return;
                TreeItem<ExplorerItem> ti = getTreeItem();
                if (ti == null) return;

                if (!ti.getChildren().isEmpty() && event.getClickCount() == 1) {
                    ti.setExpanded(!ti.isExpanded());
                    event.consume();
                } else if (event.getClickCount() == 2) {
                    if (getItem().getType() == ExplorerItemType.SONG) {
                        if (parentView != null) {
                            AppLogger.info("[TIMELINE-EXPLORER] Doppelklick auf Song '%s' -> Zuweisung...", getItem().getName());
                            parentView.assignSongToTimeline(getItem(), null);
                            event.consume();
                        }
                    } else if (getItem().getType() == ExplorerItemType.CUELIST || "bookmark".equals(getItem().getIconName())) {
                        if (parentView != null && parentView.getSceneService() != null) {
                            AppLogger.info("[TIMELINE-EXPLORER] Doppelklick auf Szene '%s' -> aktiviert.", getItem().getName());
                            parentView.getSceneService().applyScene(getItem().getId(), -1);
                            event.consume();
                        }
                    }
                }
            });
        }

        private void setupContextMenu() {
            javafx.scene.control.ContextMenu contextMenuSong = new javafx.scene.control.ContextMenu();

            javafx.scene.control.MenuItem itemAssign = new javafx.scene.control.MenuItem("In Timeline auf Spur 1 legen");
            itemAssign.setOnAction(e -> {
                if (parentView != null && getItem() != null) {
                    parentView.assignSongToTimeline(getItem(), null);
                }
            });

            javafx.scene.control.MenuItem itemNewTrack = new javafx.scene.control.MenuItem("Auf neuer Spur einfügen");
            itemNewTrack.setOnAction(e -> {
                if (parentView != null && getItem() != null) {
                    de.exit.jdmx.core.model.TimelineTrack newTrack = parentView.getTimelineService().addTrack();
                    parentView.assignSongToTimeline(getItem(), newTrack.getId());
                }
            });

            javafx.scene.control.MenuItem itemRemove = new javafx.scene.control.MenuItem("Aus Explorer entfernen");
            itemRemove.setOnAction(e -> {
                if (parentView != null && getItem() != null) {
                    File file = getItem().getSongFile();
                    if (file == null) {
                        file = parentView.getTimelineService().findSongFileByName(getItem().getName()).orElse(null);
                    }
                    if (file != null) {
                        parentView.getTimelineService().removeSong(file);
                    }
                }
            });

            contextMenuSong.getItems().addAll(itemAssign, itemNewTrack, new javafx.scene.control.SeparatorMenuItem(), itemRemove);

            javafx.scene.control.ContextMenu contextMenuScene = new javafx.scene.control.ContextMenu();
            javafx.scene.control.MenuItem itemActivateScene = new javafx.scene.control.MenuItem(I18n.get("timeline.explorer.scene.activate"));
            itemActivateScene.setOnAction(e -> {
                if (parentView != null && parentView.getSceneService() != null && getItem() != null) {
                    parentView.getSceneService().applyScene(getItem().getId(), -1);
                }
            });

            javafx.scene.control.MenuItem itemInsertTimeline = new javafx.scene.control.MenuItem("In Timeline einfügen");
            itemInsertTimeline.setOnAction(e -> {
                if (parentView != null && getItem() != null) {
                    long pos = parentView.getTimelineService().getCurrentPositionMs();
                    parentView.getTimelineService().addSceneTrack(getItem().getId(), getItem().getName(), pos, 2000L);
                }
            });

            contextMenuScene.getItems().addAll(itemActivateScene, itemInsertTimeline);

            setOnContextMenuRequested(e -> {
                if (!isEmpty() && getItem() != null) {
                    if (getItem().getType() == ExplorerItemType.SONG) {
                        contextMenuSong.show(this, e.getScreenX(), e.getScreenY());
                    } else if (getItem().getType() == ExplorerItemType.CUELIST || "bookmark".equals(getItem().getIconName())) {
                        contextMenuScene.show(this, e.getScreenX(), e.getScreenY());
                    }
                } else {
                    contextMenuSong.hide();
                    contextMenuScene.hide();
                }
            });
        }

        @Override
        public void updateItem(ExplorerItem item, boolean empty) {
            super.updateItem(item, empty);
            if (defaultDisclosureNode == null) {
                if (getDisclosureNode() != null) {
                    defaultDisclosureNode = getDisclosureNode();
                } else {
                    StackPane arrow = new StackPane();
                    arrow.getStyleClass().setAll("arrow");
                    defaultDisclosureNode = new StackPane(arrow);
                    defaultDisclosureNode.getStyleClass().setAll("tree-disclosure-node");
                }
            }

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setMouseTransparent(true);
            } else {
                if (item.isStandardFolder()) {
                    if (FOLDER_SONGS_ID.equals(item.getId())) {
                        setText(I18n.get("timeline.explorer.folder.songs"));
                    } else if (FOLDER_SCENES_ID.equals(item.getId())) {
                        setText(I18n.get("timeline.explorer.folder.scenes"));
                    } else {
                        setText(item.getName());
                    }
                } else {
                    setText(item.getName());
                }
                TreeItem<ExplorerItem> ti = getTreeItem();
                setGraphic(ti != null ? ti.getGraphic() : null);
                setMouseTransparent(false);

                boolean hasChildren = ti != null && !ti.getChildren().isEmpty();
                if (hasChildren) {
                    setDisclosureNode(defaultDisclosureNode);
                } else {
                    setDisclosureNode(null);
                }
            }
        }

        @Override
        protected void layoutChildren() {
            super.layoutChildren();
            javafx.scene.Node disclosure = getDisclosureNode();
            if (disclosure != null && disclosure.isVisible()) {
                double dh = disclosure.getLayoutBounds().getHeight();
                double centeredY = Math.round((getHeight() - dh) / 2.0);
                disclosure.setLayoutY(centeredY);
            }
        }
    }
}

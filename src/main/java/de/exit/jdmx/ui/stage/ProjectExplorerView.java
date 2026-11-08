package de.exit.jdmx.ui.stage;

import de.exit.jdmx.core.i18n.I18n;
import de.exit.jdmx.core.model.ChaserDefinition;
import de.exit.jdmx.core.model.ExplorerItem;
import de.exit.jdmx.core.model.ExplorerItemType;
import de.exit.jdmx.core.model.FixtureInstance;
import de.exit.jdmx.core.model.SceneDefinition;
import de.exit.jdmx.service.ChaserService;
import de.exit.jdmx.service.DmxService;
import de.exit.jdmx.service.ProjectExplorerService;
import de.exit.jdmx.service.SceneService;
import de.exit.jdmx.ui.MainWindow;
import de.exit.jdmx.ui.dialog.RenameItemDialog;
import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.menu.ExplorerAddMenu;
import de.exit.jdmx.ui.menu.ExplorerItemContextMenu;
import de.exit.jdmx.ui.theme.StudioTheme;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Der Project Explorer für Tab 1 (Stage & Fixtures).
 * <p>
 * Beherbergt auf der gesamten rechten Seite der Arbeitsfläche die strukturierte
 * Baumansicht aller Geräte, Gruppen, Presets, Szenenlisten und Effekte.
 * Unterstützt:
 * <ul>
 *     <li>5 aufklappbare Standard-Ordner mit Ordner-Icons und Studio-Farbakzenten</li>
 *     <li>Freie Auflistung neuer, unzugeordneter Items unterhalb der Ordner</li>
 *     <li>Drag & Drop zum Sortieren und Einsortieren in Ordner</li>
 *     <li>Einklappen über das Symbol (arrow-right-to-line)</li>
 * </ul>
 * </p>
 */
public class ProjectExplorerView extends VBox {

    /** Standardmäßige Vektor-Icongröße der Kategorie-Einträge (1,2x vergrößert auf 16px). */
    public static final double ITEM_ICON_SIZE = 16.0;

    /** Universelles Drag & Drop Datenformat für Elemente des Project Explorers. */
    public static final DataFormat EXPLORER_ITEM_FORMAT = new DataFormat("application/x-jdmx-explorer-item");

    private final ProjectExplorerService service;
    private final SceneService sceneService;
    private final ChaserService chaserService;
    private final HBox headerBox;
    private Label lblTitle;
    private final Button btnAdd;
    private final Button btnSceneDump;
    private final Button btnCollapse;
    private final ExplorerAddMenu addMenu;
    private final TreeView<ExplorerItem> treeView;
    private final TreeItem<ExplorerItem> rootItem;
    private final StackPane dockContentPane;
    private final ChaserDockEditorView chaserDockEditorView;

    private Runnable onToggleCollapse;
    private MainWindow mainWindow;
    private StageCanvasView stageCanvasView;
    private ExplorerItemContextMenu contextMenu;
    private Consumer<ExplorerItem> onItemRenameRequested;
    private Consumer<ExplorerItem> onItemDeleteRequested;

    public ProjectExplorerView() {
        this(new ProjectExplorerService(), null, null);
    }

    public ProjectExplorerView(ProjectExplorerService service) {
        this(service, null, null);
    }

    public ProjectExplorerView(ProjectExplorerService service, SceneService sceneService, ChaserService chaserService) {
        super(4);
        this.service = Objects.requireNonNull(service, "service must not be null");
        this.sceneService = sceneService != null ? sceneService : new SceneService(new DmxService());
        this.chaserService = chaserService != null ? chaserService : new ChaserService(this.sceneService);

        getStyleClass().add("project-explorer-view");
        setMinWidth(140.0);
        setPrefWidth(260.0);

        // 1. Header (Titel + grünes Plus-Zeichen + Bookmark Scene-Dump + Einklapp-Button)
        this.addMenu = new ExplorerAddMenu();
        initDefaultAddActions();

        this.btnAdd = new Button();
        btnAdd.getStyleClass().add("project-explorer-add-btn");
        btnAdd.setGraphic(new LucideIcon("plus", 13, StudioTheme.COLOR_ICON_TIMELINE));
        btnAdd.setTooltip(new Tooltip(I18n.get("explorer.add")));
        btnAdd.setOnAction(e -> {
            if (addMenu.isShowing()) {
                addMenu.hide();
            } else {
                Bounds bounds = btnAdd.localToScreen(btnAdd.getBoundsInLocal());
                if (bounds != null && btnAdd.getScene() != null && btnAdd.getScene().getWindow() != null) {
                    addMenu.show(btnAdd.getScene().getWindow(), bounds.getMinX(), bounds.getMaxY() + 4);
                }
            }
        });

        this.btnSceneDump = new Button();
        btnSceneDump.getStyleClass().add("project-explorer-dump-btn");
        btnSceneDump.setGraphic(new LucideIcon("bookmark", 13, StudioTheme.COLOR_PRIMARY_ORANGE));
        btnSceneDump.setTooltip(new Tooltip(I18n.get("explorer.scene_dump")));
        btnSceneDump.setOnAction(e -> dumpCurrentScene());

        this.btnCollapse = new Button();
        btnCollapse.getStyleClass().add("project-explorer-toggle-btn");
        btnCollapse.setGraphic(new LucideIcon("arrow-right-to-line", 14, javafx.scene.paint.Color.web("#9CA3AF")));
        btnCollapse.setTooltip(new Tooltip(I18n.get("explorer.collapse")));
        btnCollapse.setOnAction(e -> {
            if (onToggleCollapse != null) {
                onToggleCollapse.run();
            }
        });

        this.headerBox = createHeader();

        // 2. Baumstruktur (TreeView)
        ExplorerItem rootObj = new ExplorerItem("root", "Project", ExplorerItemType.FOLDER, false, "box", javafx.scene.paint.Color.WHITE);
        this.rootItem = new TreeItem<>(rootObj);
        rootItem.setExpanded(true);

        this.treeView = new TreeView<>(rootItem);
        treeView.setShowRoot(false);
        treeView.getStyleClass().add("project-explorer-tree");
        VBox.setVgrow(treeView, Priority.ALWAYS);

        // Saubere Zellen-Konfiguration mit Drag & Drop und Rechtsklick-Kontextmenü
        treeView.setCellFactory(tv -> new ProjectExplorerTreeCell(service, this));

        // 3. Chaser-Editor im Explorer-Dock (integrierte Leiste)
        this.chaserDockEditorView = new ChaserDockEditorView(this.sceneService, this.chaserService);
        chaserDockEditorView.getBtnBack().setOnAction(e -> showTreeView());
        chaserDockEditorView.setVisible(false);
        chaserDockEditorView.managedProperty().bind(chaserDockEditorView.visibleProperty());
        treeView.managedProperty().bind(treeView.visibleProperty());

        this.dockContentPane = new StackPane(treeView, chaserDockEditorView);
        VBox.setVgrow(dockContentPane, Priority.ALWAYS);

        // Klick auf leere Fläche des Baums deselektiert das aktive Element
        treeView.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getTarget() instanceof Node target) {
                TreeCell<?> cell = null;
                while (target != null && target != treeView) {
                    if (target instanceof TreeCell<?> tc) {
                        cell = tc;
                        break;
                    }
                    target = target.getParent();
                }
                if (cell == null || cell.isEmpty() || cell.getItem() == null) {
                    treeView.getSelectionModel().clearSelection();
                }
            }
        });

        // Drag & Drop auf freie Fläche der TreeView (zieht Item zurück auf Root-Ebene)
        setupTreeViewDropTarget();
        setupKeyboardInteractions();

        // Initialer Baumaufbau und Registrierung am Service
        rebuildTree();
        service.addChangeListener(() -> {
            if (getScene() != null && !Platform.isFxApplicationThread()) {
                Platform.runLater(this::rebuildTree);
            } else {
                rebuildTree();
            }
        });

        // Dynamischer Sprachwechsel-Listener
        I18n.addLanguageChangeListener(lang -> {
            if (Platform.isFxApplicationThread()) {
                updateLocalization();
            } else {
                Platform.runLater(this::updateLocalization);
            }
        });

        getChildren().addAll(headerBox, dockContentPane);
    }

    private void setupTreeViewDropTarget() {
        treeView.setOnDragOver(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasContent(EXPLORER_ITEM_FORMAT) || db.hasString()) {
                e.acceptTransferModes(TransferMode.MOVE);
            }
            e.consume();
        });

        treeView.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            String draggedId = (String) (db.hasContent(EXPLORER_ITEM_FORMAT) ? db.getContent(EXPLORER_ITEM_FORMAT) : db.getString());
            if (draggedId != null) {
                service.moveItemToUnassigned(draggedId);
                e.setDropCompleted(true);
            }
            e.consume();
        });
    }

    /**
     * Baut den Baum reaktiv anhand des aktuellen Zustands im {@link ProjectExplorerService} auf.
     * Erhält den Aufklapp-Zustand (Expanded) bestehender Ordner und synchronisiert atomar auf dem JavaFX-Thread.
     */
    public void rebuildTree() {
        Map<String, Boolean> expandedStates = new HashMap<>();
        for (TreeItem<ExplorerItem> child : rootItem.getChildren()) {
            if (child.getValue() != null && child.getValue().getType() == ExplorerItemType.FOLDER) {
                expandedStates.put(child.getValue().getId(), child.isExpanded());
                for (TreeItem<ExplorerItem> subChild : child.getChildren()) {
                    if (subChild.getValue() != null && subChild.getValue().getType() == ExplorerItemType.GROUP) {
                        expandedStates.put(subChild.getValue().getId(), subChild.isExpanded());
                    }
                }
            }
        }

        List<TreeItem<ExplorerItem>> newChildren = new ArrayList<>();

        // 1. Die 5 Standard-Ordner hinzufügen
        for (ExplorerItem folder : service.getStandardFolders()) {
            LucideIcon folderIcon = new LucideIcon(folder.getIconName(), ITEM_ICON_SIZE, folder.getColor());
            TreeItem<ExplorerItem> folderNode = new TreeItem<>(folder, folderIcon);

            boolean wasExpanded = expandedStates.getOrDefault(folder.getId(), true);
            folderNode.setExpanded(wasExpanded);

            // Unterelemente des Ordners einfügen
            List<TreeItem<ExplorerItem>> folderChildren = new ArrayList<>();
            for (ExplorerItem item : service.getItemsInFolder(folder.getId())) {
                LucideIcon itemIcon = new LucideIcon(item.getIconName(), ITEM_ICON_SIZE, item.getColor());
                TreeItem<ExplorerItem> itemNode = new TreeItem<>(item, itemIcon);

                // Wenn es sich um eine Gerätegruppe handelt: Enthaltene Fixtures als Kinder darstellen
                if (item.getType() == ExplorerItemType.GROUP) {
                    boolean groupExpanded = expandedStates.getOrDefault(item.getId(), true);
                    itemNode.setExpanded(groupExpanded);
                    List<TreeItem<ExplorerItem>> memberNodes = new ArrayList<>();
                    for (String memberId : item.getMemberFixtureIds()) {
                        ExplorerItem memberItem = service.findItemById(memberId);
                        if (memberItem != null) {
                            LucideIcon memberIcon = new LucideIcon(memberItem.getIconName(), ITEM_ICON_SIZE, memberItem.getColor());
                            memberNodes.add(new TreeItem<>(memberItem, memberIcon));
                        }
                    }
                    itemNode.getChildren().setAll(memberNodes);
                }

                folderChildren.add(itemNode);
            }
            folderNode.getChildren().setAll(folderChildren);

            newChildren.add(folderNode);
        }

        // 2. Unzugeordnete Items frei auf Root-Ebene auflisten (Nutzeranforderung)
        for (ExplorerItem item : service.getUnassignedItems()) {
            LucideIcon itemIcon = new LucideIcon(item.getIconName(), ITEM_ICON_SIZE, item.getColor());
            TreeItem<ExplorerItem> itemNode = new TreeItem<>(item, itemIcon);
            newChildren.add(itemNode);
        }

        rootItem.getChildren().setAll(newChildren);
    }

    /**
     * Liefert den unsichtbaren Root-Knoten des TreeViews.
     */
    public TreeItem<ExplorerItem> getRootItem() {
        return rootItem;
    }

    private void initDefaultAddActions() {
        addMenu.setOnAction("group", () -> {
            if (mainWindow != null) {
                mainWindow.recordUndoSnapshot();
            }
            List<String> memberIds = new ArrayList<>();
            if (stageCanvasView != null) {
                for (StageFixtureNode node : stageCanvasView.getSelectedFixtures()) {
                    if (node.getItem() != null) {
                        memberIds.add(node.getItem().getId());
                    }
                }
            }
            String groupName = service.generateUniqueFixtureName("Gruppe");
            ExplorerItem group = new ExplorerItem(
                    "group-" + java.util.UUID.randomUUID().toString().substring(0, 8),
                    groupName,
                    ExplorerItemType.GROUP,
                    false,
                    "layers",
                    javafx.scene.paint.Color.web("#34D399")
            );
            group.setMemberFixtureIds(memberIds);
            service.addItemToFolder(ProjectExplorerService.FOLDER_GROUPS_ID, group);
            if (mainWindow != null) {
                mainWindow.autoSave();
            }
        });
        addMenu.setOnAction("chaser", () -> {
            String name = service.generateUniqueFixtureName("Chaser");
            String id = "chaser-" + java.util.UUID.randomUUID().toString().substring(0, 8);
            service.addItemToFolder(ProjectExplorerService.FOLDER_CUELISTS_ID, new ExplorerItem(
                    id,
                    name,
                    ExplorerItemType.CUELIST,
                    false,
                    "list-music",
                    StudioTheme.COLOR_ICON_FADER
            ));
            if (chaserService != null) {
                chaserService.registerChaser(new ChaserDefinition(id, name));
            }
            if (mainWindow != null) {
                mainWindow.autoSave();
            }
            openChaserInDock(id, name);
        });
        addMenu.setOnAction("effect", () -> {
            String name = service.generateUniqueFixtureName("Effekt");
            service.addItemToFolder(ProjectExplorerService.FOLDER_EFFECTS_ID, new ExplorerItem(
                    "effect-" + java.util.UUID.randomUUID().toString().substring(0, 8),
                    name,
                    ExplorerItemType.EFFECT,
                    false,
                    "sparkles",
                    StudioTheme.COLOR_ICON_MATRIX
            ));
            if (mainWindow != null) {
                mainWindow.autoSave();
            }
        });
        addMenu.setOnAction("preset", () -> {
            String name = service.generateUniqueFixtureName("Preset");
            service.addItemToFolder(ProjectExplorerService.FOLDER_PRESETS_ID, new ExplorerItem(
                    "preset-" + java.util.UUID.randomUUID().toString().substring(0, 8),
                    name,
                    ExplorerItemType.PRESET,
                    false,
                    "star",
                    javafx.scene.paint.Color.web("#FB923C")
            ));
            if (mainWindow != null) {
                mainWindow.autoSave();
            }
        });
    }

    private HBox createHeader() {
        HBox header = new HBox(6);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("project-explorer-header");

        this.lblTitle = new Label(I18n.get("explorer.title"));
        lblTitle.getStyleClass().add("project-explorer-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(lblTitle, btnAdd, btnSceneDump, spacer, btnCollapse);
        return header;
    }

    public Button getBtnSceneDump() {
        return btnSceneDump;
    }

    public ChaserDockEditorView getChaserDockEditorView() {
        return chaserDockEditorView;
    }

    public boolean isChaserEditorOpen() {
        return chaserDockEditorView != null && chaserDockEditorView.isVisible();
    }

    public void openChaserInDock(String chaserId, String chaserName) {
        if (chaserDockEditorView == null) return;
        ChaserDefinition chaser = null;
        if (chaserService != null) {
            chaser = chaserService.getChaser(chaserId);
            if (chaser == null) {
                chaser = new ChaserDefinition(chaserId, chaserName);
                chaserService.registerChaser(chaser);
            }
        } else {
            chaser = new ChaserDefinition(chaserId, chaserName);
        }
        chaserDockEditorView.setChaser(chaser);
        headerBox.setVisible(false);
        headerBox.setManaged(false);
        treeView.setVisible(false);
        chaserDockEditorView.setVisible(true);
    }

    public void showTreeView() {
        if (chaserDockEditorView != null) {
            chaserDockEditorView.setVisible(false);
        }
        headerBox.setVisible(true);
        headerBox.setManaged(true);
        treeView.setVisible(true);
    }

    public void dumpCurrentScene() {
        String name = service.generateUniqueFixtureName(I18n.get("explorer.default_scene_name"));
        String sceneId = "scene-" + java.util.UUID.randomUUID().toString().substring(0, 8);

        List<FixtureInstance> activeFixtures = new ArrayList<>();
        if (stageCanvasView != null) {
            for (StageFixtureNode node : stageCanvasView.getPlacedFixtures()) {
                if (node.getFixtureInstance() != null) {
                    activeFixtures.add(node.getFixtureInstance());
                }
            }
        }

        if (sceneService != null) {
            sceneService.captureScene(name, 500, activeFixtures);
        }

        service.addItemToFolder(ProjectExplorerService.FOLDER_CUELISTS_ID, new ExplorerItem(
                sceneId,
                name,
                ExplorerItemType.CUELIST,
                false,
                "bookmark",
                StudioTheme.COLOR_PRIMARY_ORANGE
        ));

        if (mainWindow != null) {
            mainWindow.autoSave();
            mainWindow.showHudNotification(I18n.get("explorer.scene_saved", name));
        }
    }

    public void activateScene(String sceneId) {
        if (sceneService != null) {
            sceneService.applyScene(sceneId, -1);
            if (mainWindow != null) {
                ExplorerItem item = service.findItemById(sceneId);
                String name = item != null ? item.getName() : sceneId;
                mainWindow.showHudNotification(I18n.get("explorer.scene_activated", name));
            }
        }
    }

    public String getTitle() {
        return I18n.get("explorer.title");
    }

    /**
     * Aktualisiert Titel, Tooltips und alle Ordner reaktiv bei Sprachwechsel.
     */
    public void updateLocalization() {
        if (lblTitle != null) {
            lblTitle.setText(I18n.get("explorer.title"));
        }
        if (btnAdd != null) {
            btnAdd.setTooltip(new Tooltip(I18n.get("explorer.add")));
        }
        if (btnSceneDump != null) {
            btnSceneDump.setTooltip(new Tooltip(I18n.get("explorer.scene_dump")));
        }
        if (btnCollapse != null) {
            btnCollapse.setTooltip(new Tooltip(I18n.get("explorer.collapse")));
        }
        rebuildTree();
    }

    public HBox getHeaderBox() {
        return headerBox;
    }

    public Button getBtnAdd() {
        return btnAdd;
    }

    public ExplorerAddMenu getAddMenu() {
        return addMenu;
    }

    public Button getBtnCollapse() {
        return btnCollapse;
    }

    public void setOnToggleCollapse(Runnable onToggleCollapse) {
        this.onToggleCollapse = onToggleCollapse;
    }

    public TreeView<ExplorerItem> getTreeView() {
        return treeView;
    }

    public ProjectExplorerService getService() {
        return service;
    }

    public MainWindow getMainWindow() {
        return mainWindow;
    }

    public void setMainWindow(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
    }

    public StageCanvasView getStageCanvasView() {
        return stageCanvasView;
    }

    public void setStageCanvasView(StageCanvasView stageCanvasView) {
        this.stageCanvasView = stageCanvasView;
    }

    public ExplorerItemContextMenu getContextMenu() {
        return contextMenu;
    }

    public void setOnItemRenameRequested(Consumer<ExplorerItem> onItemRenameRequested) {
        this.onItemRenameRequested = onItemRenameRequested;
    }

    public void setOnItemDeleteRequested(Consumer<ExplorerItem> onItemDeleteRequested) {
        this.onItemDeleteRequested = onItemDeleteRequested;
    }

    private void setupKeyboardInteractions() {
        treeView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
                TreeItem<ExplorerItem> selectedTreeItem = treeView.getSelectionModel().getSelectedItem();
                if (selectedTreeItem != null && selectedTreeItem.getValue() != null) {
                    ExplorerItem item = selectedTreeItem.getValue();
                    if (!item.isStandardFolder()) {
                        // Wenn das Element innerhalb einer Gruppe ausgewählt ist: Aus Gruppe entfernen
                        if (selectedTreeItem.getParent() != null && selectedTreeItem.getParent().getValue() != null
                                && selectedTreeItem.getParent().getValue().getType() == ExplorerItemType.GROUP) {
                            if (mainWindow != null) {
                                mainWindow.recordUndoSnapshot();
                            }
                            selectedTreeItem.getParent().getValue().removeMemberFixtureId(item.getId());
                            service.notifyChange();
                            if (mainWindow != null) {
                                mainWindow.autoSave();
                            }
                            event.consume();
                            return;
                        }

                        if (mainWindow != null) {
                            mainWindow.recordUndoSnapshot();
                        }
                        service.removeItem(item.getId());
                        if (stageCanvasView != null) {
                            stageCanvasView.removeFixture(item.getId());
                        }
                        if (mainWindow != null) {
                            mainWindow.autoSave();
                        }
                        event.consume();
                    }
                }
            }
        });
    }

    /**
     * Wählt ein Item anhand seiner ID im TreeView aus, klappt übergeordnete Ordner auf
     * und scrollt das Element in den sichtbaren Bereich.
     *
     * @param itemId ID des auszuwählenden Elements
     * @return true, wenn das Element gefunden und ausgewählt wurde
     */
    public boolean selectItem(String itemId) {
        if (itemId == null) {
            treeView.getSelectionModel().clearSelection();
            return false;
        }
        TreeItem<ExplorerItem> found = findTreeItemById(rootItem, itemId);
        if (found != null) {
            // Übergeordnete Ordner aufklappen
            TreeItem<ExplorerItem> p = found.getParent();
            while (p != null) {
                p.setExpanded(true);
                p = p.getParent();
            }
            treeView.getSelectionModel().select(found);
            treeView.scrollTo(treeView.getRow(found));
            return true;
        }
        return false;
    }

    private TreeItem<ExplorerItem> findTreeItemById(TreeItem<ExplorerItem> parent, String id) {
        if (parent == null || id == null) {
            return null;
        }
        if (parent.getValue() != null && id.equals(parent.getValue().getId())) {
            return parent;
        }
        for (TreeItem<ExplorerItem> child : parent.getChildren()) {
            TreeItem<ExplorerItem> match = findTreeItemById(child, id);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    /**
     * Spezifische TreeCell für den Project Explorer mit Drag & Drop Unterstützung
     * sowie Rechtsklick-Kontextmenü zum Umbenennen und Entfernen von Elementen.
     */
    public static class ProjectExplorerTreeCell extends TreeCell<ExplorerItem> {

        private final ProjectExplorerService service;
        private final ProjectExplorerView parentView;
        private Node defaultDisclosureNode;
        private ExplorerItemContextMenu currentContextMenu;

        public ProjectExplorerTreeCell(ProjectExplorerService service) {
            this(service, null);
        }

        public ProjectExplorerTreeCell(ProjectExplorerService service, ProjectExplorerView parentView) {
            this.service = service;
            this.parentView = parentView;
            setupDragAndDrop();
            setupClickHandler();
            setupContextMenu();
        }

        public ExplorerItemContextMenu getCurrentContextMenu() {
            return currentContextMenu;
        }

        private void setupContextMenu() {
            setOnContextMenuRequested(event -> {
                ExplorerItem item = getItem();
                if (item == null || item.isStandardFolder()) {
                    return;
                }

                ExplorerItemContextMenu menu = new ExplorerItemContextMenu();
                currentContextMenu = menu;
                if (parentView != null) {
                    parentView.contextMenu = menu;
                }

                menu.setOnRename(() -> {
                    if (parentView != null && parentView.onItemRenameRequested != null) {
                        parentView.onItemRenameRequested.accept(item);
                    } else if (parentView != null && parentView.mainWindow != null) {
                        RenameItemDialog.show(parentView.mainWindow, item, service, parentView.stageCanvasView);
                    }
                });

                menu.setOnDelete(() -> {
                    if (parentView != null && parentView.onItemDeleteRequested != null) {
                        parentView.onItemDeleteRequested.accept(item);
                    } else {
                        TreeItem<ExplorerItem> ti = getTreeItem();
                        if (ti != null && ti.getParent() != null && ti.getParent().getValue() != null
                                && ti.getParent().getValue().getType() == ExplorerItemType.GROUP) {
                            if (parentView != null && parentView.mainWindow != null) {
                                parentView.mainWindow.recordUndoSnapshot();
                            }
                            ti.getParent().getValue().removeMemberFixtureId(item.getId());
                            service.notifyChange();
                            if (parentView != null && parentView.mainWindow != null) {
                                parentView.mainWindow.autoSave();
                            }
                            return;
                        }

                        if (parentView != null && parentView.mainWindow != null) {
                            parentView.mainWindow.recordUndoSnapshot();
                        }
                        if (service != null) {
                            service.removeItem(item.getId());
                        }
                        if (parentView != null && parentView.stageCanvasView != null) {
                            parentView.stageCanvasView.removeFixture(item.getId());
                        }
                        if (parentView != null && parentView.mainWindow != null) {
                            parentView.mainWindow.autoSave();
                        }
                    }
                });

                if (getScene() != null && getScene().getWindow() != null) {
                    menu.showMenu(getScene().getWindow(), event.getScreenX(), event.getScreenY());
                }
                event.consume();
            });
        }

        private void setupClickHandler() {
            setOnMouseClicked(event -> {
                if (isEmpty() || getItem() == null) {
                    return;
                }
                ExplorerItem item = getItem();
                if (event.getClickCount() == 2 && parentView != null && item != null) {
                    if (item.getType() == ExplorerItemType.CUELIST) {
                        if ("bookmark".equals(item.getIconName())) {
                            parentView.activateScene(item.getId());
                        } else {
                            parentView.openChaserInDock(item.getId(), item.getName());
                        }
                        event.consume();
                        return;
                    } else if (item.getType() == ExplorerItemType.EFFECT) {
                        parentView.openChaserInDock(item.getId(), item.getName());
                        event.consume();
                        return;
                    }
                }
                TreeItem<ExplorerItem> ti = getTreeItem();
                if (ti != null && !ti.getChildren().isEmpty() && event.getClickCount() == 1) {
                    ti.setExpanded(!ti.isExpanded());
                    event.consume();
                }
            });
        }

        private void setupDragAndDrop() {
            // 1. Drag Start
            setOnDragDetected(event -> {
                if (isEmpty() || getItem() == null) {
                    return;
                }
                ExplorerItem item = getItem();
                // Standard-Ordner sind fest und können nicht verschoben werden
                if (item.isStandardFolder()) {
                    return;
                }

                Dragboard db = startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.put(EXPLORER_ITEM_FORMAT, item.getId());
                content.putString(item.getId());
                db.setContent(content);
                event.consume();
            });

            // 2. Drag Over (Gültiges Ziel prüfen)
            setOnDragOver(event -> {
                Dragboard db = event.getDragboard();
                if ((db.hasContent(EXPLORER_ITEM_FORMAT) || db.hasString()) && getItem() != null) {
                    String draggedId = (String) (db.hasContent(EXPLORER_ITEM_FORMAT) ? db.getContent(EXPLORER_ITEM_FORMAT) : db.getString());
                    if (!getItem().getId().equals(draggedId)) {
                        event.acceptTransferModes(TransferMode.MOVE);
                        if (!getStyleClass().contains("tree-cell-drop-target")) {
                            getStyleClass().add("tree-cell-drop-target");
                        }
                    }
                }
                event.consume();
            });

            // 3. Drag Exited (Hervorhebung entfernen)
            setOnDragExited(event -> {
                getStyleClass().remove("tree-cell-drop-target");
                event.consume();
            });

            // 4. Drop Action
            setOnDragDropped(event -> {
                getStyleClass().remove("tree-cell-drop-target");
                if (service == null) {
                    return;
                }

                Dragboard db = event.getDragboard();
                String draggedId = (String) (db.hasContent(EXPLORER_ITEM_FORMAT) ? db.getContent(EXPLORER_ITEM_FORMAT) : db.getString());
                if (draggedId != null && getItem() != null && !getItem().getId().equals(draggedId)) {
                    ExplorerItem target = getItem();

                    if (target.getType() == ExplorerItemType.FOLDER) {
                        // In Zielordner verschieben
                        service.moveItemToFolder(draggedId, target.getId());
                    } else if (target.getType() == ExplorerItemType.GROUP) {
                        // Zur Gerätegruppe hinzufügen
                        if (parentView != null && parentView.mainWindow != null) {
                            parentView.mainWindow.recordUndoSnapshot();
                        }
                        target.addMemberFixtureId(draggedId);
                        service.notifyChange();
                        if (parentView != null && parentView.mainWindow != null) {
                            parentView.mainWindow.autoSave();
                        }
                    } else {
                        // Prüfen, ob target ein Element innerhalb einer Gruppe ist
                        TreeItem<ExplorerItem> cellTi = getTreeItem();
                        if (cellTi != null && cellTi.getParent() != null && cellTi.getParent().getValue() != null
                                && cellTi.getParent().getValue().getType() == ExplorerItemType.GROUP) {
                            if (parentView != null && parentView.mainWindow != null) {
                                parentView.mainWindow.recordUndoSnapshot();
                            }
                            cellTi.getParent().getValue().addMemberFixtureId(draggedId);
                            service.notifyChange();
                            if (parentView != null && parentView.mainWindow != null) {
                                parentView.mainWindow.autoSave();
                            }
                            event.setDropCompleted(true);
                            event.consume();
                            return;
                        }

                        // Neben anderes Item verschieben (gleicher Ordner oder freie Liste)
                        String parentFolderId = service.findParentFolderId(target.getId());
                        if (parentFolderId != null) {
                            service.moveItemToFolder(draggedId, parentFolderId);
                            int targetIdx = service.getItemsInFolder(parentFolderId).indexOf(target);
                            if (targetIdx >= 0) {
                                service.reorderInFolder(parentFolderId, draggedId, targetIdx);
                            }
                        } else {
                            service.moveItemToUnassigned(draggedId);
                            int targetIdx = service.getUnassignedItems().indexOf(target);
                            if (targetIdx >= 0) {
                                service.reorderUnassigned(draggedId, targetIdx);
                            }
                        }
                    }
                    event.setDropCompleted(true);
                }
                event.consume();
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
                    String localized = switch (item.getId()) {
                        case ProjectExplorerService.FOLDER_DEVICES_ID -> I18n.get("explorer.folder.devices");
                        case ProjectExplorerService.FOLDER_GROUPS_ID -> I18n.get("explorer.folder.groups");
                        case ProjectExplorerService.FOLDER_PRESETS_ID -> I18n.get("explorer.folder.presets");
                        case ProjectExplorerService.FOLDER_CUELISTS_ID -> I18n.get("explorer.folder.cuelists");
                        case ProjectExplorerService.FOLDER_EFFECTS_ID -> I18n.get("explorer.folder.effects");
                        default -> item.getName();
                    };
                    setText(localized);
                } else {
                    setText(item.getName());
                }
                TreeItem<ExplorerItem> ti = getTreeItem();
                setGraphic(ti != null ? ti.getGraphic() : null);
                setMouseTransparent(false);

                // Wenn ein Knoten keine Unterelemente besitzt (noch leere Standard-Ordner oder Leaf-Items),
                // Disclosure-Node zwingend auf null setzen, damit Icon und Text direkt am linken Rand anliegen.
                // Sobald Unterelemente vorhanden sind, wird der kompakte Disclosure-Pfeil eingeblendet.
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

            // Ausklapppfeil (Disclosure-Node) mathematisch exakt vertikal im Hover-Kasten zentrieren
            Node disclosure = getDisclosureNode();
            if (disclosure != null && disclosure.isVisible()) {
                double dh = disclosure.getLayoutBounds().getHeight();
                double centeredY = Math.round((getHeight() - dh) / 2.0);
                disclosure.setLayoutY(centeredY);
            }

            Node graphic = getGraphic();
            if (graphic != null && !isEmpty()) {
                TreeItem<ExplorerItem> ti = getTreeItem();
                boolean hasChildren = ti != null && !ti.getChildren().isEmpty();
                // Wenn keine Unterelemente vorhanden sind (leere Standard-Ordner oder Leaf-Items),
                // zwingend exakt linksbündig gemäß Hierarchie-Level ausrichten (umgeht OpenJFX TreeCellSkin Hardcoding)!
                if (!hasChildren) {
                    int level = (getTreeView() != null && ti != null) ? getTreeView().getTreeItemLevel(ti) : 0;
                    if (getTreeView() != null && !getTreeView().isShowRoot()) {
                        level = Math.max(0, level - 1);
                    }
                    double indent = 16.0;
                    double left = snappedLeftInset() + (level * indent);
                    double top = snappedTopInset();
                    double availH = getHeight() - top - snappedBottomInset();
                    double gh = graphic.getLayoutBounds().getHeight();
                    double gw = graphic.getLayoutBounds().getWidth();
                    graphic.relocate(left, top + Math.max(0, (availH - gh) / 2.0));

                    for (Node child : getChildrenUnmodifiable()) {
                        if (child != graphic && child.getStyleClass().contains("text")) {
                            double gap = getGraphicTextGap();
                            double th = child.getLayoutBounds().getHeight();
                            child.relocate(left + gw + gap, top + Math.max(0, (availH - th) / 2.0));
                        }
                    }
                }
            }
        }
    }
}

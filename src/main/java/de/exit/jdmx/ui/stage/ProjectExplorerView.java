package de.exit.jdmx.ui.stage;

import de.exit.jdmx.core.model.ExplorerItem;
import de.exit.jdmx.core.model.ExplorerItemType;
import de.exit.jdmx.service.ProjectExplorerService;
import de.exit.jdmx.ui.icon.LucideIcon;
import javafx.application.Platform;
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
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    private final String title = "Project Explorer";

    private final ProjectExplorerService service;
    private final HBox headerBox;
    private final Button btnCollapse;
    private final TreeView<ExplorerItem> treeView;
    private final TreeItem<ExplorerItem> rootItem;

    private Runnable onToggleCollapse;

    public ProjectExplorerView() {
        this(new ProjectExplorerService());
    }

    public ProjectExplorerView(ProjectExplorerService service) {
        super(4);
        this.service = Objects.requireNonNull(service, "service must not be null");

        getStyleClass().add("project-explorer-view");
        setMinWidth(140.0);
        setPrefWidth(260.0);

        // 1. Header (Titel ohne störendes Vor-Icon + Einklapp-Button mit arrow-right-to-line)
        this.btnCollapse = new Button();
        btnCollapse.getStyleClass().add("project-explorer-toggle-btn");
        btnCollapse.setGraphic(new LucideIcon("arrow-right-to-line", 14, javafx.scene.paint.Color.web("#9CA3AF")));
        btnCollapse.setTooltip(new Tooltip("Einklappen"));
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

        // Saubere Zellen-Konfiguration mit Drag & Drop
        treeView.setCellFactory(tv -> new ProjectExplorerTreeCell(service));

        // Drag & Drop auf freie Fläche der TreeView (zieht Item zurück auf Root-Ebene)
        setupTreeViewDropTarget();

        // Initialer Baumaufbau und Registrierung am Service
        rebuildTree();
        service.addChangeListener(() -> {
            if (getScene() != null && !Platform.isFxApplicationThread()) {
                Platform.runLater(this::rebuildTree);
            } else {
                rebuildTree();
            }
        });

        getChildren().addAll(headerBox, treeView);
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

    private HBox createHeader() {
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("project-explorer-header");

        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("project-explorer-title");

        header.getChildren().addAll(lblTitle, btnCollapse);
        return header;
    }

    public String getTitle() {
        return title;
    }

    public HBox getHeaderBox() {
        return headerBox;
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

    /**
     * Spezifische TreeCell für den Project Explorer mit Drag & Drop Unterstützung.
     */
    public static class ProjectExplorerTreeCell extends TreeCell<ExplorerItem> {

        private final ProjectExplorerService service;
        private Node defaultDisclosureNode;

        public ProjectExplorerTreeCell(ProjectExplorerService service) {
            this.service = service;
            setupDragAndDrop();
            setupClickHandler();
        }

        private void setupClickHandler() {
            setOnMouseClicked(event -> {
                if (isEmpty() || getItem() == null) {
                    return;
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
                    } else {
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
                setText(item.getName());
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
    }
}

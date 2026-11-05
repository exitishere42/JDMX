package de.exit.jdmx.ui.stage;

import de.exit.jdmx.ui.icon.LucideIcon;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Der Project Explorer für Tab 1 (Stage & Fixtures).
 * <p>
 * Beherbergt auf der gesamten rechten Seite der Arbeitsfläche die strukturierte
 * Baumansicht aller Geräte, Gruppen, Presets, Szenenlisten und Effekte.
 * Lässt sich flexibel in der Breite skalieren oder über das Symbol (arrow-right-to-line) zuklappen.
 * Die Items sind ergonomisch um 1,2x vergrößert, und leere Zellen werden nicht gehovert.
 * </p>
 */
public class ProjectExplorerView extends VBox {

    /** Standardmäßige Vektor-Icongröße der Kategorie-Einträge (1,2x vergrößert auf 16px). */
    public static final double ITEM_ICON_SIZE = 16.0;

    private final String title = "Project Explorer";

    private final HBox headerBox;
    private final Button btnCollapse;
    private final TreeView<String> treeView;
    private final TreeItem<String> rootItem;

    private Runnable onToggleCollapse;

    public ProjectExplorerView() {
        super(4);
        getStyleClass().add("project-explorer-view");
        setMinWidth(140.0);
        setPrefWidth(260.0);

        // 1. Header (Titel ohne störendes Vor-Icon + Einklapp-Button mit arrow-right-to-line)
        this.btnCollapse = new Button();
        btnCollapse.getStyleClass().add("project-explorer-toggle-btn");
        btnCollapse.setGraphic(new LucideIcon("arrow-right-to-line", 14, Color.web("#9CA3AF")));
        btnCollapse.setTooltip(new Tooltip("Einklappen"));
        btnCollapse.setOnAction(e -> {
            if (onToggleCollapse != null) {
                onToggleCollapse.run();
            }
        });

        this.headerBox = createHeader();

        // 2. Baumstruktur (TreeView)
        this.rootItem = new TreeItem<>("Project");
        rootItem.setExpanded(true);

        TreeItem<String> devicesItem = createCategoryNode("Geräte", "lamp", Color.web("#60A5FA"));
        TreeItem<String> groupsItem = createCategoryNode("Gerätegruppen", "layers", Color.web("#34D399"));
        TreeItem<String> presetsItem = createCategoryNode("Presets", "palette", Color.web("#FBBF24"));
        TreeItem<String> cuelistsItem = createCategoryNode("Szenenlisten", "list-music", Color.web("#A78BFA"));
        TreeItem<String> effectsItem = createCategoryNode("Effekte", "sparkles", Color.web("#F472B6"));

        rootItem.getChildren().addAll(devicesItem, groupsItem, presetsItem, cuelistsItem, effectsItem);

        this.treeView = new TreeView<>(rootItem);
        treeView.setShowRoot(false);
        treeView.getStyleClass().add("project-explorer-tree");
        VBox.setVgrow(treeView, Priority.ALWAYS);

        // Saubere Zellen-Konfiguration: Leere Zellen sind maus-transparent und werden nicht gehovert
        treeView.setCellFactory(tv -> new ProjectExplorerTreeCell());

        getChildren().addAll(headerBox, treeView);
    }

    /**
     * Spezifische TreeCell für den Project Explorer.
     * <p>
     * Setzt leere Zellen auf {@code mouseTransparent = true}, damit diese
     * weder gehovert noch geklickt werden können.
     * </p>
     */
    public static class ProjectExplorerTreeCell extends TreeCell<String> {
        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setMouseTransparent(true);
            } else {
                setText(item);
                TreeItem<String> ti = getTreeItem();
                setGraphic(ti != null ? ti.getGraphic() : null);
                setMouseTransparent(false);
            }
        }
    }

    private HBox createHeader() {
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("project-explorer-header");

        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("project-explorer-title");

        // Titel und direkt daneben der Einklapp-Button (kein störender Kreis / Vor-Icon mehr)
        header.getChildren().addAll(lblTitle, btnCollapse);
        return header;
    }

    private TreeItem<String> createCategoryNode(String name, String iconName, Color iconColor) {
        LucideIcon icon = new LucideIcon(iconName, ITEM_ICON_SIZE, iconColor);
        TreeItem<String> item = new TreeItem<>(name, icon);
        item.setExpanded(true);
        return item;
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

    public TreeView<String> getTreeView() {
        return treeView;
    }
}

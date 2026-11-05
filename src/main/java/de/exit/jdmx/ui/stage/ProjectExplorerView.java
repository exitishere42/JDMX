package de.exit.jdmx.ui.stage;

import de.exit.jdmx.ui.icon.LucideIcon;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
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
 * Lässt sich flexibel in der Breite skalieren oder per Klick auf die Dragbar zuklappen.
 * </p>
 */
public class ProjectExplorerView extends VBox {

    private final String title = "Project Explorer";

    private final TreeView<String> treeView;
    private final TreeItem<String> rootItem;

    public ProjectExplorerView() {
        super(4);
        getStyleClass().add("project-explorer-view");
        setMinWidth(140.0);
        setPrefWidth(260.0);

        // 1. Header (Icon + Titel)
        HBox header = createHeader();

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

        getChildren().addAll(header, treeView);
    }

    private HBox createHeader() {
        HBox header = new HBox(6);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("project-explorer-header");

        LucideIcon icon = new LucideIcon("folder-tree", 14, Color.web("#9CA3AF"));
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("project-explorer-title");

        header.getChildren().addAll(icon, lblTitle);
        return header;
    }

    private TreeItem<String> createCategoryNode(String name, String iconName, Color iconColor) {
        LucideIcon icon = new LucideIcon(iconName, 13, iconColor);
        TreeItem<String> item = new TreeItem<>(name, icon);
        item.setExpanded(true);
        return item;
    }

    public String getTitle() {
        return title;
    }

    public TreeView<String> getTreeView() {
        return treeView;
    }
}

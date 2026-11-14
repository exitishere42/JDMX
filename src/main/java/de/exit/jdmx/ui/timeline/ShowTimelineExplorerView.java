package de.exit.jdmx.ui.timeline;

import de.exit.jdmx.core.i18n.I18n;
import de.exit.jdmx.core.model.ExplorerItem;
import de.exit.jdmx.core.model.ExplorerItemType;
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
import javafx.scene.layout.VBox;

/**
 * Rechte Seitenleiste der Show-Timeline (Tab 4).
 * <p>
 * Enthält einen kompakten Tab-Header mit dem [Explorer]-Tab und dem [+]-Button
 * sowie eine TreeView, die Songs und Tracks der Show auflistet.
 * </p>
 * <p>
 * Layout:
 * <pre>
 * ┌────────────────────────────┐
 * │ [Explorer]              [+]│  ← kompakter Tab-Header
 * ├────────────────────────────┤
 * │ ▸ Songs                   │  ← TreeView
 * │   ├─ showtanz26.mp3       │
 * │   └─ fanfare.mp3          │
 * └────────────────────────────┘
 * </pre>
 * </p>
 */
public class ShowTimelineExplorerView extends VBox {

    /** Standard-Mindestbreite der Seitenleiste in Pixeln. */
    public static final double DEFAULT_MIN_WIDTH = 260.0;

    /** Standard-Vorzugsbreite der Seitenleiste in Pixeln. */
    public static final double DEFAULT_PREF_WIDTH = 280.0;

    /** Icon-Größe für Einträge in der TreeView. */
    public static final double ITEM_ICON_SIZE = 16.0;

    /** Ordner-ID für den Songs-Ordner in der TreeView. */
    public static final String FOLDER_SONGS_ID = "timeline-folder-songs";

    private final Button btnTabExplorer;
    private final Button btnAdd;
    private final ShowTimelineAddMenu addMenu;
    private final TreeView<ExplorerItem> treeView;
    private final TreeItem<ExplorerItem> rootItem;

    /**
     * Erstellt den Show-Timeline Explorer mit Tab-Header, [+]-Button und TreeView.
     */
    public ShowTimelineExplorerView() {
        super(0);
        getStyleClass().add("timeline-explorer-view");
        setMinWidth(DEFAULT_MIN_WIDTH);
        setPrefWidth(DEFAULT_PREF_WIDTH);

        // --- Add-Menü ---
        this.addMenu = new ShowTimelineAddMenu();

        // --- [+] Button ---
        this.btnAdd = new Button();
        btnAdd.getStyleClass().add("timeline-explorer-add-btn");
        btnAdd.setGraphic(new LucideIcon("plus", 12, StudioTheme.COLOR_ICON_TIMELINE));
        btnAdd.setTooltip(new javafx.scene.control.Tooltip(I18n.get("timeline.explorer.add")));
        btnAdd.setOnAction(e -> {
            if (addMenu.isShowing()) {
                addMenu.safeHide();
            } else {
                Bounds bounds = btnAdd.localToScreen(btnAdd.getBoundsInLocal());
                if (bounds != null && btnAdd.getScene() != null && btnAdd.getScene().getWindow() != null) {
                    addMenu.show(btnAdd.getScene().getWindow(), bounds.getMinX(), bounds.getMaxY() + 4);
                }
            }
        });

        // --- Explorer Tab-Button (kompakt, aktiv) ---
        this.btnTabExplorer = new Button(I18n.get("timeline.explorer.tab.explorer"));
        btnTabExplorer.getStyleClass().addAll("timeline-explorer-tab-btn", "active");
        btnTabExplorer.setOnAction(e -> selectTab(btnTabExplorer));

        // --- Tab-Header (HBox) ---
        HBox header = new HBox(0);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("timeline-explorer-header");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(btnTabExplorer, spacer, btnAdd);

        // --- TreeView (Songs-Ordner) ---
        ExplorerItem rootObj = new ExplorerItem(
                "timeline-root", "Timeline", ExplorerItemType.FOLDER, false, "audio-lines",
                StudioTheme.COLOR_ICON_TIMELINE
        );
        this.rootItem = new TreeItem<>(rootObj);
        rootItem.setExpanded(true);

        this.treeView = new TreeView<>(rootItem);
        treeView.setShowRoot(false);
        treeView.getStyleClass().add("project-explorer-tree");
        VBox.setVgrow(treeView, Priority.ALWAYS);

        // Songs-Ordner initial einrichten
        buildTree();

        // Sprachwechsel reaktiv
        I18n.addLanguageChangeListener(lang -> {
            if (Platform.isFxApplicationThread()) {
                updateLocalization();
            } else {
                Platform.runLater(this::updateLocalization);
            }
        });

        getChildren().addAll(header, treeView);
    }

    /**
     * Baut die TreeView mit dem Songs-Standardordner auf.
     */
    private void buildTree() {
        ExplorerItem songsFolder = new ExplorerItem(
                FOLDER_SONGS_ID,
                I18n.get("timeline.explorer.folder.songs"),
                ExplorerItemType.FOLDER,
                true,
                "music",
                StudioTheme.COLOR_ICON_TIMELINE
        );
        LucideIcon folderIcon = new LucideIcon("music", ITEM_ICON_SIZE, StudioTheme.COLOR_ICON_TIMELINE);
        TreeItem<ExplorerItem> songsFolderItem = new TreeItem<>(songsFolder, folderIcon);
        songsFolderItem.setExpanded(true);

        rootItem.getChildren().setAll(songsFolderItem);
    }

    /**
     * Aktiviert den angeklickten Tab (setzt "active"-StyleClass).
     * Vorbereitet für spätere Inspector-Tab-Erweiterung.
     *
     * @param selected der zu aktivierende Tab-Button
     */
    private void selectTab(Button selected) {
        btnTabExplorer.getStyleClass().remove("active");
        selected.getStyleClass().add("active");
    }

    /**
     * Aktualisiert Texte und Tooltips bei Sprachwechsel.
     */
    public void updateLocalization() {
        btnTabExplorer.setText(I18n.get("timeline.explorer.tab.explorer"));
        btnAdd.setTooltip(new javafx.scene.control.Tooltip(I18n.get("timeline.explorer.add")));
        addMenu.updateLocalization();
        buildTree();
    }

    /**
     * Gibt den Explorer-Tab-Button zurück.
     *
     * @return der kompakte Tab-Button
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
     * Gibt die TreeView zurück.
     *
     * @return die Baum-Ansicht
     */
    public TreeView<ExplorerItem> getTreeView() {
        return treeView;
    }

    /**
     * Gibt den unsichtbaren Root-Knoten der TreeView zurück.
     *
     * @return der Root-TreeItem
     */
    public TreeItem<ExplorerItem> getRootItem() {
        return rootItem;
    }
}

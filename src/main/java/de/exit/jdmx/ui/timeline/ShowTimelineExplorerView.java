package de.exit.jdmx.ui.timeline;

import de.exit.jdmx.core.i18n.I18n;
import de.exit.jdmx.core.model.ExplorerItem;
import de.exit.jdmx.core.model.ExplorerItemType;
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
import javafx.stage.FileChooser;

import java.io.File;
import java.util.Objects;

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

    /** Standard-Mindestbreite der Seitenleiste in Pixeln. */
    public static final double DEFAULT_MIN_WIDTH = 260.0;

    /** Standard-Vorzugsbreite der Seitenleiste in Pixeln. */
    public static final double DEFAULT_PREF_WIDTH = 280.0;

    /** Icon-Größe für Einträge in der TreeView. */
    public static final double ITEM_ICON_SIZE = 16.0;

    /** Ordner-ID für den Songs-Ordner in der TreeView. */
    public static final String FOLDER_SONGS_ID = "timeline-folder-songs";

    private final TimelineService timelineService;

    private final Button btnTabInspector;
    private final Button btnTabExplorer;
    private final Button btnAdd;
    private final ShowTimelineAddMenu addMenu;

    private final StackPane contentStack;
    private final TimelineInspectorView inspectorView;
    private final TreeView<ExplorerItem> treeView;
    private final TreeItem<ExplorerItem> rootItem;
    private TreeItem<ExplorerItem> songsFolderItem;

    /**
     * Erstellt den Show-Timeline Explorer mit Standard-TimelineService.
     */
    public ShowTimelineExplorerView() {
        this(new TimelineService());
    }

    /**
     * Erstellt den Show-Timeline Explorer mit dem angegebenen TimelineService.
     *
     * @param service der {@link TimelineService} (darf nicht {@code null} sein)
     */
    public ShowTimelineExplorerView(TimelineService service) {
        super(0);
        this.timelineService = Objects.requireNonNull(service, "service must not be null");

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
                    addMenu.show(btnAdd.getScene().getWindow(), bounds.getMinX(), bounds.getMaxY() + 4);
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

        // --- 2. TreeView (Songs-Ordner) ---
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

        buildTree();

        // StackPane für Umschaltung zwischen Inspektor und TreeView
        this.contentStack = new StackPane(treeView, inspectorView);
        VBox.setVgrow(contentStack, Priority.ALWAYS);

        // Auf Änderungen im TimelineService lauschen (z. B. importierte Songs)
        timelineService.addChangeListener(() -> {
            if (getScene() != null && !Platform.isFxApplicationThread()) {
                Platform.runLater(this::buildTree);
            } else {
                buildTree();
            }
        });

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

    /**
     * Baut die TreeView mit dem Songs-Standardordner und importierten Dateien auf.
     */
    public void buildTree() {
        ExplorerItem songsFolder = new ExplorerItem(
                FOLDER_SONGS_ID,
                I18n.get("timeline.explorer.folder.songs"),
                ExplorerItemType.FOLDER,
                true,
                "music",
                StudioTheme.COLOR_ICON_TIMELINE
        );
        LucideIcon folderIcon = new LucideIcon("music", ITEM_ICON_SIZE, StudioTheme.COLOR_ICON_TIMELINE);
        this.songsFolderItem = new TreeItem<>(songsFolder, folderIcon);
        songsFolderItem.setExpanded(true);

        for (File song : timelineService.getImportedSongs()) {
            ExplorerItem songItem = new ExplorerItem(
                    "song-" + song.getName(),
                    song.getName(),
                    ExplorerItemType.SONG,
                    false,
                    "music",
                    StudioTheme.COLOR_ICON_TIMELINE
            );
            LucideIcon songIcon = new LucideIcon("music", ITEM_ICON_SIZE, StudioTheme.COLOR_ICON_TIMELINE);
            songsFolderItem.getChildren().add(new TreeItem<>(songItem, songIcon));
        }

        rootItem.getChildren().setAll(songsFolderItem);
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
     * Gibt den zugrunde liegenden TimelineService zurück.
     *
     * @return der {@link TimelineService}
     */
    public TimelineService getTimelineService() {
        return timelineService;
    }
}

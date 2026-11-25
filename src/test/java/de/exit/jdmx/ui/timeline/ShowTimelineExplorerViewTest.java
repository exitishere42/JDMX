package de.exit.jdmx.ui.timeline;

import de.exit.jdmx.core.model.ExplorerItem;
import de.exit.jdmx.core.model.ExplorerItemType;
import de.exit.jdmx.core.model.SceneDefinition;
import de.exit.jdmx.service.DmxService;
import de.exit.jdmx.service.ProjectExplorerService;
import de.exit.jdmx.service.SceneService;
import de.exit.jdmx.service.TimelineService;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für die Show-Timeline Explorer- und Inspektor-Seitenleiste.
 * Prüft den kompakten Tab-Header mit [Inspektor]- und [Explorer]-Tab und [+]-Button
 * sowie den Umschalt-Mechanismus und die TreeView-Struktur.
 */
class ShowTimelineExplorerViewTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit bereits aktiv
        }
    }

    @Test
    @DisplayName("Sollte Inspektor- und Explorer-Tab-Buttons erstellen")
    void shouldCreateInspectorAndExplorerTabButtons() {
        ShowTimelineExplorerView view = new ShowTimelineExplorerView();
        assertThat(view.getBtnTabInspector()).isNotNull();
        assertThat(view.getBtnTabInspector().getStyleClass()).contains("timeline-explorer-tab-btn");

        assertThat(view.getBtnTabExplorer()).isNotNull();
        assertThat(view.getBtnTabExplorer().getStyleClass()).contains("timeline-explorer-tab-btn");
    }

    @Test
    @DisplayName("Sollte Plus-Button ohne Outline wie in Stage erstellen")
    void shouldCreateAddButtonWithoutOutline() {
        ShowTimelineExplorerView view = new ShowTimelineExplorerView();
        Button btnAdd = view.getBtnAdd();
        assertThat(btnAdd).isNotNull();
        assertThat(btnAdd.getStyleClass()).contains("timeline-explorer-add-btn");
    }

    @Test
    @DisplayName("Sollte TreeView mit Songs-Ordner initialisieren")
    void shouldInitializeTreeViewWithSongsFolder() {
        ShowTimelineExplorerView view = new ShowTimelineExplorerView();
        TreeView<?> treeView = view.getTreeView();
        assertThat(treeView).isNotNull();
        assertThat(view.getRootItem()).isNotNull();
        assertThat(view.getRootItem().getChildren()).isNotEmpty();
    }

    @Test
    @DisplayName("Sollte AddMenu mit Song-Eintrag bereitstellen")
    void shouldProvideAddMenuWithSongEntry() {
        ShowTimelineExplorerView view = new ShowTimelineExplorerView();
        assertThat(view.getAddMenu()).isNotNull();
        assertThat(view.getAddMenu().getItem("song")).isNotNull();
    }

    @Test
    @DisplayName("Sollte dieselbe Mindest- und Vorzugsbreite wie der Stage Project Explorer besitzen (300px)")
    void shouldHaveSameWidthAsStageProjectExplorer() {
        ShowTimelineExplorerView view = new ShowTimelineExplorerView();
        assertThat(ShowTimelineExplorerView.DEFAULT_MIN_WIDTH).isEqualTo(300.0);
        assertThat(ShowTimelineExplorerView.DEFAULT_PREF_WIDTH).isEqualTo(300.0);
        assertThat(view.getMinWidth()).isEqualTo(300.0);
        assertThat(view.getPrefWidth()).isEqualTo(300.0);
    }

    @Test
    @DisplayName("Sollte Tabs umschalten können und bei Plus-Klick automatisch zu Explorer wechseln")
    void shouldSwitchTabsAndAutoSwitchToExplorerOnAdd() {
        ShowTimelineExplorerView view = new ShowTimelineExplorerView();

        // 1. Zu Inspektor wechseln
        view.getBtnTabInspector().fire();
        assertThat(view.isInspectorTabActive()).isTrue();
        assertThat(view.getInspectorView().isVisible()).isTrue();
        assertThat(view.getTreeView().isVisible()).isFalse();

        // 2. Klick auf Plus wechselt automatisch zurück zum Explorer-Tab
        view.getBtnAdd().fire();
        assertThat(view.isInspectorTabActive()).isFalse();
        assertThat(view.getTreeView().isVisible()).isTrue();
        assertThat(view.getInspectorView().isVisible()).isFalse();
    }

    @Test
    @DisplayName("Sollte importierte Songs in den Songs-Ordner der TreeView aufnehmen")
    void shouldAddImportedSongToSongsFolder() {
        TimelineService service = new TimelineService();
        ShowTimelineExplorerView view = new ShowTimelineExplorerView(service);

        File songFile = new File("fanfare.mp3");
        service.importSong(songFile);

        TreeItem<ExplorerItem> songsFolder = view.getSongsFolderItem();
        assertThat(songsFolder).isNotNull();
        assertThat(songsFolder.getChildren()).anyMatch(child -> child.getValue().getName().equals("fanfare.mp3"));
    }

    @Test
    @DisplayName("Sollte Song bei Doppel-Aktivierung auf den Track der Timeline legen")
    void shouldAssignSongToTrackOnDirectAction() {
        TimelineService service = new TimelineService();
        ShowTimelineExplorerView view = new ShowTimelineExplorerView(service);

        File songFile = new File("rcv_garde.mp3");
        service.importSong(songFile);

        ExplorerItem item = view.getSongsFolderItem().getChildren().get(0).getValue();
        assertThat(item.getSongFile()).isEqualTo(songFile);

        boolean assigned = view.assignSongToTimeline(item, null);
        assertThat(assigned).isTrue();
        assertThat(service.getTracks().get(0).hasSong()).isTrue();
        assertThat(service.getTracks().get(0).getSongFile()).isEqualTo(songFile);
    }

    @Test
    @DisplayName("Sollte TreeView mit Szenen-Ordner und Songs-Ordner initialisieren")
    void shouldInitializeTreeViewWithScenesFolder() {
        ShowTimelineExplorerView view = new ShowTimelineExplorerView();
        TreeItem<ExplorerItem> scenesFolder = view.getScenesFolderItem();
        assertThat(scenesFolder).isNotNull();
        assertThat(scenesFolder.getValue().getId()).isEqualTo(ShowTimelineExplorerView.FOLDER_SCENES_ID);
        assertThat(view.getRootItem().getChildren()).contains(scenesFolder);
        assertThat(view.getRootItem().getChildren()).contains(view.getSongsFolderItem());
    }

    @Test
    @DisplayName("Sollte alle angelegten Szenen aus SceneService und ProjectExplorerService anzeigen")
    void shouldDisplayAllCreatedScenesInTimelineExplorer() {
        TimelineService timelineService = new TimelineService();
        ProjectExplorerService explorerService = new ProjectExplorerService();
        DmxService dmxService = new DmxService();
        SceneService sceneService = new SceneService(dmxService);

        // 1. Szene in SceneService erfassen / registrieren
        SceneDefinition scene1 = new SceneDefinition("scene-1", "Intro Blau", 1000, Map.of(0, Map.of(1, 255)));
        sceneService.registerScene(scene1);

        // 2. Szene in ProjectExplorerService anlegen
        ExplorerItem sceneItem2 = new ExplorerItem("scene-2", "Refrain Strobe", ExplorerItemType.CUELIST, false, "bookmark", javafx.scene.paint.Color.web("#A78BFA"));
        explorerService.addItemToFolder(ProjectExplorerService.FOLDER_CUELISTS_ID, sceneItem2);

        // View initialisieren
        ShowTimelineExplorerView view = new ShowTimelineExplorerView(timelineService, explorerService, sceneService);

        TreeItem<ExplorerItem> scenesFolder = view.getScenesFolderItem();
        assertThat(scenesFolder).isNotNull();
        assertThat(scenesFolder.getChildren()).hasSize(2);
        assertThat(scenesFolder.getChildren()).extracting(ti -> ti.getValue().getName())
                .containsExactlyInAnyOrder("Intro Blau", "Refrain Strobe");
    }

    @Test
    @DisplayName("Sollte Szenen-Ordner reaktiv aktualisieren wenn neue Szenen angelegt oder gelöscht werden")
    void shouldReactivelyUpdateWhenScenesAreAddedOrRemoved() {
        TimelineService timelineService = new TimelineService();
        ProjectExplorerService explorerService = new ProjectExplorerService();
        DmxService dmxService = new DmxService();
        SceneService sceneService = new SceneService(dmxService);

        ShowTimelineExplorerView view = new ShowTimelineExplorerView(timelineService, explorerService, sceneService);
        TreeItem<ExplorerItem> scenesFolder = view.getScenesFolderItem();
        assertThat(scenesFolder.getChildren()).isEmpty();

        // Neue Szene in SceneService erfassen
        SceneDefinition sceneA = new SceneDefinition("scene-a", "Blackout Scene", 0, Map.of());
        sceneService.registerScene(sceneA);

        assertThat(scenesFolder.getChildren()).hasSize(1);
        assertThat(scenesFolder.getChildren().get(0).getValue().getName()).isEqualTo("Blackout Scene");

        // Szene wieder löschen
        sceneService.deleteScene("scene-a");
        assertThat(scenesFolder.getChildren()).isEmpty();
    }
}

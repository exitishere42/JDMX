package de.exit.jdmx.ui.timeline;

import de.exit.jdmx.core.model.ExplorerItem;
import de.exit.jdmx.service.TimelineService;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

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
}

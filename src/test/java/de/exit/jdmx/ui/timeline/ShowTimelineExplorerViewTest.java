package de.exit.jdmx.ui.timeline;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.TreeView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für die Show-Timeline Explorer-Seitenleiste.
 * Prüft den kompakten Tab-Header mit [Explorer]-Tab und [+]-Button
 * sowie die TreeView-Struktur mit dem Songs-Ordner.
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
    @DisplayName("Sollte Explorer-Tab-Button erstellen")
    void shouldCreateExplorerTabButton() {
        ShowTimelineExplorerView view = new ShowTimelineExplorerView();
        assertThat(view.getBtnTabExplorer()).isNotNull();
        assertThat(view.getBtnTabExplorer().getStyleClass()).contains("timeline-explorer-tab-btn");
    }

    @Test
    @DisplayName("Sollte Plus-Button mit Outline-Stil erstellen")
    void shouldCreateAddButton() {
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
    @DisplayName("Sollte Explorer-Tab standardmäßig aktiv sein")
    void shouldHaveExplorerTabActiveByDefault() {
        ShowTimelineExplorerView view = new ShowTimelineExplorerView();
        assertThat(view.getBtnTabExplorer().getStyleClass()).contains("active");
    }
}

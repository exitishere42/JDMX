package de.exit.jdmx.ui.stage;

import de.exit.jdmx.core.i18n.I18n;
import de.exit.jdmx.core.model.ExplorerItem;
import de.exit.jdmx.core.model.ExplorerItemType;
import de.exit.jdmx.service.ChaserService;
import de.exit.jdmx.service.DmxService;
import de.exit.jdmx.service.ProjectExplorerService;
import de.exit.jdmx.service.SceneService;
import javafx.application.Platform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests für den Scene-Dump Button (Bookmark-Icon) und den integrierten Chaser-Editor
 * im Dock des {@link ProjectExplorerView}.
 */
class ProjectExplorerSceneDumpAndChaserDockTest {

    private DmxService dmxService;
    private SceneService sceneService;
    private ChaserService chaserService;
    private ProjectExplorerService explorerService;
    private ProjectExplorerView explorerView;

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Bereits initialisiert
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        I18n.setLanguage("de");
        dmxService = new DmxService();
        sceneService = new SceneService(dmxService);
        chaserService = new ChaserService(sceneService);
        explorerService = new ProjectExplorerService();

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            explorerView = new ProjectExplorerView(explorerService, sceneService, chaserService);
            latch.countDown();
        });
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (chaserService != null) chaserService.close();
        if (sceneService != null) sceneService.close();
        if (dmxService != null) dmxService.close();
    }

    @Test
    @DisplayName("Sollte Scene-Dump Button mit Bookmark-Icon im Header neben dem Plus-Button besitzen")
    void shouldHaveSceneDumpButtonInHeader() {
        assertThat(explorerView.getBtnSceneDump()).isNotNull();
        assertThat(explorerView.getBtnSceneDump().getGraphic()).isNotNull();
        assertThat(explorerView.getHeaderBox().getChildren()).contains(explorerView.getBtnSceneDump());
        // Neben btnAdd
        int idxAdd = explorerView.getHeaderBox().getChildren().indexOf(explorerView.getBtnAdd());
        int idxDump = explorerView.getHeaderBox().getChildren().indexOf(explorerView.getBtnSceneDump());
        assertThat(idxDump).isEqualTo(idxAdd + 1);
    }

    @Test
    @DisplayName("Klick auf Scene-Dump erstellt eine neue Szene unter Szenenlisten")
    void shouldDumpCurrentSceneIntoExplorer() throws Exception {
        dmxService.setChannel(0, 1, 255);
        dmxService.setChannel(0, 2, 100);

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            explorerView.getBtnSceneDump().fire();
            latch.countDown();
        });
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

        List<ExplorerItem> scenes = explorerService.getItemsInFolder(ProjectExplorerService.FOLDER_CUELISTS_ID);
        assertThat(scenes).isNotEmpty();
        ExplorerItem latestScene = scenes.getLast();
        assertThat(latestScene.getType()).isEqualTo(ExplorerItemType.CUELIST);
        assertThat(latestScene.getIconName()).isEqualTo("bookmark");
    }

    @Test
    @DisplayName("Sollte Chaser-Editor im Explorer-Dock öffnen und mit Zurück-Button schließen können")
    void shouldOpenAndCloseChaserEditorInDock() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            assertThat(explorerView.isChaserEditorOpen()).isFalse();

            // Chaser im Dock öffnen
            explorerView.openChaserInDock("chaser-test-1", "Mein Lauflicht");
            assertThat(explorerView.isChaserEditorOpen()).isTrue();
            assertThat(explorerView.getChaserDockEditorView()).isNotNull();

            // Zurück zum Baum
            explorerView.getChaserDockEditorView().getBtnBack().fire();
            assertThat(explorerView.isChaserEditorOpen()).isFalse();

            latch.countDown();
        });
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }
}

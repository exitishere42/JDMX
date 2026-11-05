package de.exit.jdmx.ui.dialog;

import de.exit.jdmx.core.model.FixtureDefinition;
import de.exit.jdmx.service.FixtureLibraryService;
import de.exit.jdmx.service.ProjectExplorerService;
import de.exit.jdmx.ui.stage.StageCanvasView;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für den In-App Modal Dialog {@link FixturePatchDialog}.
 */
class FixturePatchDialogTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit aktiv
        }
    }

    @Test
    @DisplayName("Sollte Dialog mit Hersteller, Modell, Modus und Adressfeldern initialisieren")
    void shouldInitializePatchDialog() {
        FixtureLibraryService libraryService = new FixtureLibraryService();
        ProjectExplorerService explorerService = new ProjectExplorerService();
        StageCanvasView stageCanvas = new StageCanvasView(explorerService);

        FixturePatchDialog dialog = new FixturePatchDialog(null, libraryService, explorerService, stageCanvas);

        assertThat(dialog.getView()).isNotNull();
        assertThat(dialog.getSearchField()).isNotNull();
        assertThat(dialog.getComboManufacturer()).isNotNull();
        assertThat(dialog.getComboModel()).isNotNull();
        assertThat(dialog.getComboMode()).isNotNull();
        assertThat(dialog.getSpinnerAddress()).isNotNull();
        assertThat(dialog.getSpinnerQuantity()).isNotNull();
        assertThat(dialog.getBtnImportFile()).isNotNull();
        assertThat(dialog.getBtnPatch()).isNotNull();

        // Herstellerliste sollte Generic enthalten
        assertThat(dialog.getComboManufacturer().getItems()).contains("Generic");
    }

    @Test
    @DisplayName("Sollte beim Patchen Fixtures im Explorer und auf der Bühne anlegen")
    void shouldPatchFixturesToExplorerAndStage() {
        FixtureLibraryService libraryService = new FixtureLibraryService();
        ProjectExplorerService explorerService = new ProjectExplorerService();
        StageCanvasView stageCanvas = new StageCanvasView(explorerService);

        FixturePatchDialog dialog = new FixturePatchDialog(null, libraryService, explorerService, stageCanvas);

        // Wähle Generic -> Dimmer -> 1CH -> 2x Scheinwerfer
        dialog.getComboManufacturer().getSelectionModel().select("Generic");
        dialog.getSpinnerQuantity().getValueFactory().setValue(2);
        dialog.getSpinnerAddress().getValueFactory().setValue(1);

        int initialExplorerItems = explorerService.getItemsInFolder("folder-devices").size();
        int initialPlacedFixtures = stageCanvas.getPlacedFixtures().size();

        // Ausführen
        dialog.getBtnPatch().fire();

        // 2 neue Fixtures im Explorer-Ordner 'folder-devices'
        assertThat(explorerService.getItemsInFolder("folder-devices")).hasSize(initialExplorerItems + 2);
        // 2 neue Fixtures auf der Bühne platziert
        assertThat(stageCanvas.getPlacedFixtures()).hasSize(initialPlacedFixtures + 2);
    }
}

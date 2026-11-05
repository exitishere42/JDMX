package de.exit.jdmx.ui.stage;

import de.exit.jdmx.core.model.ExplorerItem;
import de.exit.jdmx.core.model.ExplorerItemType;
import de.exit.jdmx.service.ProjectExplorerService;
import de.exit.jdmx.ui.icon.LucideIcon;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für den Project Explorer ({@link ProjectExplorerView}).
 */
class ProjectExplorerViewTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit aktiv
        }
    }

    @Test
    @DisplayName("Sollte Titel 'Project Explorer' und 5 aufklappbare Standard-Ordner besitzen")
    void shouldInitializeWithStandardFolders() {
        ProjectExplorerView explorer = new ProjectExplorerView();

        assertThat(explorer.getTitle()).isEqualTo("Project Explorer");
        assertThat(explorer.getTreeView()).isNotNull();

        TreeItem<ExplorerItem> root = explorer.getTreeView().getRoot();
        assertThat(root).isNotNull();
        assertThat(root.getChildren()).hasSize(5);

        // Prüfe die 5 Standard-Kategorien als aufklappbare Ordner
        assertThat(root.getChildren().get(0).getValue().getName()).isEqualTo("Geräte");
        assertThat(root.getChildren().get(0).getValue().isStandardFolder()).isTrue();
        assertThat(root.getChildren().get(0).getValue().getType()).isEqualTo(ExplorerItemType.FOLDER);

        assertThat(root.getChildren().get(1).getValue().getName()).isEqualTo("Gerätegruppen");
        assertThat(root.getChildren().get(2).getValue().getName()).isEqualTo("Presets");
        assertThat(root.getChildren().get(3).getValue().getName()).isEqualTo("Szenenlisten");
        assertThat(root.getChildren().get(4).getValue().getName()).isEqualTo("Effekte");
    }

    @Test
    @DisplayName("Sollte flexible Mindest- und Standardbreite besitzen")
    void shouldHaveFlexibleWidths() {
        ProjectExplorerView explorer = new ProjectExplorerView();

        assertThat(explorer.getMinWidth()).isLessThanOrEqualTo(150.0);
        assertThat(explorer.getPrefWidth()).isGreaterThanOrEqualTo(220.0);
    }

    @Test
    @DisplayName("Sollte Einklapp-Button mit arrow-right-to-line und grünen Add-Button neben dem Titel besitzen")
    void shouldHaveCollapseButtonNextToTitleWithoutLeadingIcon() {
        ProjectExplorerView explorer = new ProjectExplorerView();

        // Header enthält Titel, grünen Add-Button, Spacer und Einklapp-Button
        assertThat(explorer.getHeaderBox().getChildren()).hasSize(4);
        assertThat(explorer.getHeaderBox().getChildren().get(0)).isInstanceOf(Label.class);
        assertThat(((Label) explorer.getHeaderBox().getChildren().get(0)).getText())
                .isEqualTo("Project Explorer");
        assertThat(explorer.getHeaderBox().getChildren().get(1)).isEqualTo(explorer.getBtnAdd());
        assertThat(explorer.getBtnAdd().getGraphic()).isInstanceOf(LucideIcon.class);
        assertThat(((LucideIcon) explorer.getBtnAdd().getGraphic()).getName()).isEqualTo("plus");

        // Einklapp-Button mit arrow-right-to-line
        assertThat(explorer.getBtnCollapse()).isNotNull();
        assertThat(explorer.getBtnCollapse().getGraphic()).isInstanceOf(LucideIcon.class);
        LucideIcon icon = (LucideIcon) explorer.getBtnCollapse().getGraphic();
        assertThat(icon.getName()).isEqualTo("arrow-right-to-line");

        // Event-Auslösung prüfen
        boolean[] called = new boolean[1];
        explorer.setOnToggleCollapse(() -> called[0] = true);
        explorer.getBtnCollapse().fire();
        assertThat(called[0]).isTrue();
    }

    @Test
    @DisplayName("Sollte grünen Plus-Button mit ExplorerAddMenu besitzen")
    void shouldHaveGreenAddButtonWithMenu() {
        ProjectExplorerView explorer = new ProjectExplorerView();

        assertThat(explorer.getBtnAdd()).isNotNull();
        assertThat(explorer.getAddMenu()).isNotNull();

        boolean[] fixtureTriggered = new boolean[1];
        explorer.getAddMenu().setOnAction("fixture", () -> fixtureTriggered[0] = true);
        explorer.getAddMenu().triggerAction("fixture");
        assertThat(fixtureTriggered[0]).isTrue();
    }

    @Test
    @DisplayName("Sollte Tree-Item-Icons um 1,2x vergrößert auf 16px skalieren")
    void shouldScaleItemIconsByFactor1Point2() {
        ProjectExplorerView explorer = new ProjectExplorerView();

        TreeItem<ExplorerItem> firstCategory = explorer.getTreeView().getRoot().getChildren().get(0);
        assertThat(firstCategory.getGraphic()).isInstanceOf(LucideIcon.class);
        LucideIcon icon = (LucideIcon) firstCategory.getGraphic();
        assertThat(icon.getSize()).isEqualTo(16.0);
    }

    @Test
    @DisplayName("Sollte freie Items unterhalb der Standard-Ordner auf Root-Ebene auflisten")
    void shouldDisplayUnassignedItemsBelowStandardFolders() {
        ProjectExplorerService service = new ProjectExplorerService();
        ProjectExplorerView explorer = new ProjectExplorerView(service);

        // 1. Neues unzugeordnetes Item anlegen
        ExplorerItem spot = new ExplorerItem("spot-1", "Spot 1", ExplorerItemType.FIXTURE, false, "lamp", Color.web("#60A5FA"));
        service.addItem(spot);

        TreeItem<ExplorerItem> root = explorer.getTreeView().getRoot();
        // 5 Standard-Ordner + 1 freies Item = 6 Kinder
        assertThat(root.getChildren()).hasSize(6);
        assertThat(root.getChildren().get(5).getValue().getName()).isEqualTo("Spot 1");

        // 2. Item per Service in Geräte-Ordner verschieben
        String devicesFolderId = service.getStandardFolders().get(0).getId();
        service.moveItemToFolder("spot-1", devicesFolderId);

        // Root hat wieder 5 Kinder (die Ordner)
        assertThat(root.getChildren()).hasSize(5);
        // Geräte-Ordner hat nun das Kind
        TreeItem<ExplorerItem> devicesNode = root.getChildren().get(0);
        assertThat(devicesNode.getChildren()).hasSize(1);
        assertThat(devicesNode.getChildren().get(0).getValue().getName()).isEqualTo("Spot 1");
    }

    @Test
    @DisplayName("Sollte leere TreeCells als maus-transparent markieren, damit sie nicht gehovert werden")
    void shouldMakeEmptyCellsMouseTransparent() {
        ProjectExplorerView.ProjectExplorerTreeCell cell = new ProjectExplorerView.ProjectExplorerTreeCell(null);

        // 1. Leer -> mouseTransparent = true
        cell.updateItem(null, true);
        assertThat(cell.isMouseTransparent()).isTrue();
        assertThat(cell.getText()).isNull();
        assertThat(cell.getGraphic()).isNull();

        // 2. Gefüllt -> mouseTransparent = false
        ExplorerItem item = new ExplorerItem("test-1", "Geräte", ExplorerItemType.FOLDER, true, "folder", Color.BLUE);
        cell.updateItem(item, false);
        assertThat(cell.isMouseTransparent()).isFalse();
        assertThat(cell.getText()).isEqualTo("Geräte");
    }

    @Test
    @DisplayName("Sollte für Knoten ohne Unterknoten (leere Ordner oder Leaf-Items) keinen Disclosure-Node reservieren, um Links-Ausrichtung zu sichern")
    void shouldRemoveDisclosureNodeForLeafItems() {
        ProjectExplorerView.ProjectExplorerTreeCell cell = new ProjectExplorerView.ProjectExplorerTreeCell(null);

        // 1. Leaf-Item (Fixture) -> kein Disclosure-Node
        ExplorerItem leaf = new ExplorerItem("spot-1", "Spot 1", ExplorerItemType.FIXTURE, false, "lamp", Color.BLUE);
        cell.updateItem(leaf, false);
        assertThat(cell.getDisclosureNode()).isNull();

        // 2. Noch leerer Standard-Ordner -> ebenfalls kein Disclosure-Node, liegt direkt links am Rand an
        ExplorerItem emptyFolder = new ExplorerItem("folder-dev", "Geräte", ExplorerItemType.FOLDER, true, "folder", Color.CYAN);
        cell.updateItem(emptyFolder, false);
        assertThat(cell.getDisclosureNode()).isNull();
    }

    @Test
    @DisplayName("Sollte Top-Level Items im Explorer extrem nah am linken Rand ausrichten (minX <= 6px)")
    void shouldAlignTopLevelItemsTightlyToLeftMargin() throws Exception {
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        boolean[] success = new boolean[1];
        Platform.runLater(() -> {
            try {
                ProjectExplorerView view = new ProjectExplorerView();
                javafx.scene.Scene scene = new javafx.scene.Scene(view, 300, 600);
                scene.getStylesheets().add(getClass().getResource("/de/exit/jdmx/styles/studio-dark.css").toExternalForm());
                view.applyCss();
                view.layout();

                int checked = 0;
                for (javafx.scene.Node n : view.getTreeView().lookupAll(".tree-cell")) {
                    if (n instanceof ProjectExplorerView.ProjectExplorerTreeCell cell && cell.getItem() != null) {
                        assertThat(cell.getGraphic().getBoundsInParent().getMinX()).isLessThanOrEqualTo(6.0);
                        checked++;
                    }
                }
                assertThat(checked).isGreaterThan(0);
                success[0] = true;
            } finally {
                latch.countDown();
            }
        });
        latch.await(3, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(success[0]).isTrue();
    }
}

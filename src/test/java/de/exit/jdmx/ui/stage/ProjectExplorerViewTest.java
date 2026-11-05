package de.exit.jdmx.ui.stage;

import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für den Project Explorer (ProjectExplorerView).
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
    @DisplayName("Sollte Titel 'Project Explorer' und strukturierte Baumknoten besitzen")
    void shouldInitializeWithStandardTreeNodes() {
        ProjectExplorerView explorer = new ProjectExplorerView();

        assertThat(explorer.getTitle()).isEqualTo("Project Explorer");
        assertThat(explorer.getTreeView()).isNotNull();

        TreeItem<String> root = explorer.getTreeView().getRoot();
        assertThat(root).isNotNull();
        assertThat(root.getChildren()).hasSize(5);

        // Prüfe die 5 Standard-Kategorien
        assertThat(root.getChildren().get(0).getValue()).isEqualTo("Geräte");
        assertThat(root.getChildren().get(1).getValue()).isEqualTo("Gerätegruppen");
        assertThat(root.getChildren().get(2).getValue()).isEqualTo("Presets");
        assertThat(root.getChildren().get(3).getValue()).isEqualTo("Szenenlisten");
        assertThat(root.getChildren().get(4).getValue()).isEqualTo("Effekte");
    }

    @Test
    @DisplayName("Sollte flexible Mindest- und Standardbreite besitzen")
    void shouldHaveFlexibleWidths() {
        ProjectExplorerView explorer = new ProjectExplorerView();

        assertThat(explorer.getMinWidth()).isLessThanOrEqualTo(150.0);
        assertThat(explorer.getPrefWidth()).isGreaterThanOrEqualTo(220.0);
    }

    @Test
    @DisplayName("Sollte Einklapp-Button mit arrow-right-to-line neben dem Titel ohne störendes Vor-Icon besitzen")
    void shouldHaveCollapseButtonNextToTitleWithoutLeadingIcon() {
        ProjectExplorerView explorer = new ProjectExplorerView();

        // Kein Vor-Icon vor dem Titel
        assertThat(explorer.getHeaderBox().getChildren()).hasSize(2);
        assertThat(explorer.getHeaderBox().getChildren().get(0)).isInstanceOf(javafx.scene.control.Label.class);
        assertThat(((javafx.scene.control.Label) explorer.getHeaderBox().getChildren().get(0)).getText())
                .isEqualTo("Project Explorer");

        // Einklapp-Button mit arrow-right-to-line direkt daneben
        assertThat(explorer.getBtnCollapse()).isNotNull();
        assertThat(explorer.getBtnCollapse().getGraphic()).isInstanceOf(de.exit.jdmx.ui.icon.LucideIcon.class);
        de.exit.jdmx.ui.icon.LucideIcon icon = (de.exit.jdmx.ui.icon.LucideIcon) explorer.getBtnCollapse().getGraphic();
        assertThat(icon.getName()).isEqualTo("arrow-right-to-line");

        // Event-Auslösung prüfen
        boolean[] called = new boolean[1];
        explorer.setOnToggleCollapse(() -> called[0] = true);
        explorer.getBtnCollapse().fire();
        assertThat(called[0]).isTrue();
    }

    @Test
    @DisplayName("Sollte Tree-Item-Icons um 1,2x vergrößert auf 16px skalieren")
    void shouldScaleItemIconsByFactor1Point2() {
        ProjectExplorerView explorer = new ProjectExplorerView();

        TreeItem<String> firstCategory = explorer.getTreeView().getRoot().getChildren().get(0);
        assertThat(firstCategory.getGraphic()).isInstanceOf(de.exit.jdmx.ui.icon.LucideIcon.class);
        de.exit.jdmx.ui.icon.LucideIcon icon = (de.exit.jdmx.ui.icon.LucideIcon) firstCategory.getGraphic();
        assertThat(icon.getSize()).isEqualTo(16.0);
    }

    @Test
    @DisplayName("Sollte leere TreeCells als maus-transparent markieren, damit sie nicht gehovert werden")
    void shouldMakeEmptyCellsMouseTransparent() {
        ProjectExplorerView.ProjectExplorerTreeCell cell = new ProjectExplorerView.ProjectExplorerTreeCell();

        // 1. Leer -> mouseTransparent = true
        cell.updateItem(null, true);
        assertThat(cell.isMouseTransparent()).isTrue();
        assertThat(cell.getText()).isNull();
        assertThat(cell.getGraphic()).isNull();

        // 2. Gefüllt -> mouseTransparent = false
        cell.updateItem("Geräte", false);
        assertThat(cell.isMouseTransparent()).isFalse();
        assertThat(cell.getText()).isEqualTo("Geräte");
    }
}

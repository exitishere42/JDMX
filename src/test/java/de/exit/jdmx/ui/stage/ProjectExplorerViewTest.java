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
}

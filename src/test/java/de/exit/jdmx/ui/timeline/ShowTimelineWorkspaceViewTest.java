package de.exit.jdmx.ui.timeline;

import javafx.application.Platform;
import javafx.geometry.Orientation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für den Show-Timeline Workspace (Tab 4).
 * Prüft das horizontale SplitPane mit dem Explorer auf der rechten Seite.
 */
class ShowTimelineWorkspaceViewTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit bereits aktiv
        }
    }

    @Test
    @DisplayName("Sollte horizontales SplitPane aufbauen")
    void shouldBuildHorizontalSplitPane() {
        ShowTimelineWorkspaceView workspace = new ShowTimelineWorkspaceView();
        assertThat(workspace.getOrientation()).isEqualTo(Orientation.HORIZONTAL);
        assertThat(workspace.getItems()).hasSize(2);
    }

    @Test
    @DisplayName("Sollte Show-Timeline Explorer auf der rechten Seite bereitstellen")
    void shouldProvideExplorerOnRightSide() {
        ShowTimelineWorkspaceView workspace = new ShowTimelineWorkspaceView();
        assertThat(workspace.getExplorerView()).isNotNull();
        assertThat(workspace.getItems().get(1)).isEqualTo(workspace.getExplorerView());
    }

    @Test
    @DisplayName("Sollte Standard-Split-Ratio setzen")
    void shouldSetDefaultSplitRatio() {
        ShowTimelineWorkspaceView workspace = new ShowTimelineWorkspaceView();
        double[] dividers = workspace.getDividerPositions();
        assertThat(dividers).hasSize(1);
        assertThat(dividers[0]).isGreaterThan(0.5);
    }
}

package de.exit.jdmx.ui;

import de.exit.jdmx.ui.stage.StageWorkspaceView;
import de.exit.jdmx.ui.timeline.ShowTimelineWorkspaceView;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für die synchrone Übernahme der Project-Explorer-Breite zwischen Stage-Tab und Show-Timeline-Tab.
 */
class MainWindowTabSwitchSyncTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Bereits initialisiert
        }
    }

    @Test
    @DisplayName("Sollte Divider-Position des Project Explorers vom Stage-Tab auf den Show-Timeline-Tab übertragen")
    void shouldSyncDividerFromStageToTimeline() {
        MainWindow mainWindow = new MainWindow();
        StageWorkspaceView stageView = mainWindow.getStageWorkspaceView();
        ShowTimelineWorkspaceView timelineView = mainWindow.getShowTimelineWorkspaceView();

        assertThat(stageView).isNotNull();
        assertThat(timelineView).isNotNull();

        // 1. Stage-Explorer-Breite verändern (z.B. Divider auf 0.82)
        stageView.setHorizontalDividerPosition(0.82);
        assertThat(stageView.getHorizontalDividerPosition()).isEqualTo(0.82);

        // 2. Zu Tab 3 (Show-Timeline) wechseln
        mainWindow.switchToTab(3);

        // 3. Timeline-Workspace muss denselben Divider besitzen
        assertThat(timelineView.getHorizontalDividerPosition()).isEqualTo(0.82);
    }

    @Test
    @DisplayName("Sollte Divider-Position des Project Explorers von der Timeline zurück auf Stage übertragen")
    void shouldSyncDividerFromTimelineToStage() {
        MainWindow mainWindow = new MainWindow();
        StageWorkspaceView stageView = mainWindow.getStageWorkspaceView();
        ShowTimelineWorkspaceView timelineView = mainWindow.getShowTimelineWorkspaceView();

        // Zu Tab 3 wechseln
        mainWindow.switchToTab(3);

        // Timeline-Divider verändern
        timelineView.setHorizontalDividerPosition(0.75);
        assertThat(timelineView.getHorizontalDividerPosition()).isEqualTo(0.75);

        // Zurück zu Tab 0 (Stage) wechseln
        mainWindow.switchToTab(0);

        // Stage muss denselben Divider besitzen
        assertThat(stageView.getHorizontalDividerPosition()).isEqualTo(0.75);
    }
}

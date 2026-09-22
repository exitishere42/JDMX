package de.exit.jdmx.ui;

import de.exit.jdmx.ui.icons.LucideIcon;
import de.exit.jdmx.ui.tabs.*;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * MainView for jdmx JavaFX Application.
 * Assembles the permanent Global Header, the 5 Workspace Tab Buttons,
 * and the Tab Content Viewport.
 */
public class MainView extends BorderPane {

    private final StackPane contentArea = new StackPane();
    private final List<Button> tabButtons = new ArrayList<>();
    private final List<Node> tabPanes = new ArrayList<>();
    private final List<String> tabIcons = new ArrayList<>();

    private final LiveMatrixTab liveMatrixTab;
    private final StageTab stageTab;
    private final FaderUniverseTab faderUniverseTab;
    private final TimelineTab timelineTab;
    private final DiagnosticsTab diagnosticsTab;

    /**
     * Constructs the MainView containing all tabs and the header bar.
     */
    public MainView() {
        liveMatrixTab = new LiveMatrixTab();
        stageTab = new StageTab();
        faderUniverseTab = new FaderUniverseTab();
        timelineTab = new TimelineTab();
        diagnosticsTab = new DiagnosticsTab();

        tabPanes.add(stageTab);
        tabPanes.add(faderUniverseTab);
        tabPanes.add(liveMatrixTab);
        tabPanes.add(timelineTab);
        tabPanes.add(diagnosticsTab);
        
        tabIcons.add(LucideIcon.BOX);
        tabIcons.add(LucideIcon.SLIDERS);
        tabIcons.add(LucideIcon.GRID);
        tabIcons.add(LucideIcon.AUDIO_LINES);
        tabIcons.add(LucideIcon.CABLE);

        // Header
        HeaderBar headerBar = new HeaderBar(
                () -> liveMatrixTab.deactivateAllPads(),
                () -> {}
        );

        // Tab Navigation Bar
        HBox tabBar = buildTabBar();

        VBox topContainer = new VBox(headerBar, tabBar);
        setTop(topContainer);

        // Center Content
        VBox.setVgrow(contentArea, Priority.ALWAYS);
        setCenter(contentArea);

        // Select Tab 3 (Live Matrix) by default as requested
        selectTab(2);
    }

    /**
     * Builds the tab bar navigation.
     *
     * @return HBox containing the tab buttons.
     */
    private HBox buildTabBar() {
        HBox tabBar = new HBox(6);
        tabBar.getStyleClass().add("main-tab-bar");
        tabBar.setAlignment(Pos.CENTER_LEFT);

        Button tab1 = createTabButton("1", "2D Stage & Fixtures", tabIcons.get(0), 0);
        Button tab2 = createTabButton("2", "Fader & Universum", tabIcons.get(1), 1);
        Button tab3 = createTabButton("3", "Live Matrix", tabIcons.get(2), 2);
        Button tab4 = createTabButton("4", "Timeline & Music Sync", tabIcons.get(3), 3);
        Button tab5 = createTabButton("5", "Input / Output & Diagnostics", tabIcons.get(4), 4);

        tabButtons.add(tab1);
        tabButtons.add(tab2);
        tabButtons.add(tab3);
        tabButtons.add(tab4);
        tabButtons.add(tab5);

        tabBar.getChildren().addAll(tab1, tab2, tab3, tab4, tab5);
        return tabBar;
    }

    /**
     * Creates a single tab button.
     *
     * @param num      The tab number.
     * @param title    The tab title.
     * @param iconPath The SVG path for the icon.
     * @param tabIndex The index of the tab.
     * @return The constructed Button.
     */
    private Button createTabButton(String num, String title, String iconPath, int tabIndex) {
        Button btn = new Button(num + "  " + title);
        btn.getStyleClass().add("tab-button");
        btn.setGraphic(LucideIcon.create(iconPath, LucideIcon.COLOR_HANDLE, 0.7));
        btn.setOnAction(e -> selectTab(tabIndex));
        return btn;
    }

    /**
     * Selects a tab by index and updates the UI.
     *
     * @param index The index of the tab to select.
     */
    private void selectTab(int index) {
        for (int i = 0; i < tabButtons.size(); i++) {
            Button btn = tabButtons.get(i);
            if (i == index) {
                btn.getStyleClass().add("active");
                btn.setGraphic(LucideIcon.create(tabIcons.get(i), LucideIcon.COLOR_PRIMARY, 0.7));
            } else {
                btn.getStyleClass().remove("active");
                btn.setGraphic(LucideIcon.create(tabIcons.get(i), LucideIcon.COLOR_HANDLE, 0.7));
            }
        }

        contentArea.getChildren().clear();
        if (index >= 0 && index < tabPanes.size()) {
            contentArea.getChildren().add(tabPanes.get(index));
        }
    }
}

package de.exit.jdmx.ui.desk;

import de.exit.jdmx.core.model.ChannelDefinition;
import de.exit.jdmx.core.model.ExplorerItem;
import de.exit.jdmx.core.model.FixtureInstance;
import de.exit.jdmx.service.DmxService;
import de.exit.jdmx.service.ProjectExplorerService;
import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.theme.StudioTheme;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Objects;

/**
 * Arbeitsbereich für Tab 2 (LightDesk - Fader-Pult).
 * <p>
 * Bietet ein professionelles Lichtmischpult-Layout mit 50/50-Teilung:
 * <ul>
 *     <li><b>Kopfzeile:</b> Universums-Umschaltung (Universum 1–4) und Schnell-Reset.</li>
 *     <li><b>Oberes Fader-Dock (50 % Höhe):</b> Puristische Canvas-Balken ohne Text-Overhead mit Kanalnummern am Fuß und rechteckiger Custom-Scrollbar.</li>
 *     <li><b>Unterer Bereich (50 % Höhe):</b> Strukturierte Arbeitsfläche für die nächsten Ausbauschritte.</li>
 * </ul>
 * </p>
 */
public class LightDeskWorkspaceView extends VBox {

    public static final int TOTAL_CHANNELS = CanvasDeskView.TOTAL_CHANNELS;

    private final DmxService dmxService;
    private final ProjectExplorerService explorerService;

    // Header Controls
    private final ComboBox<String> comboUniverse;

    // SplitPane mit 50/50 Aufteilung
    private final SplitPane splitPane;
    private final VBox topFaderDock;
    private final StackPane bottomWorkspaceArea;
    private final CanvasDeskView canvasDeskView;

    /**
     * Erstellt einen neuen LightDesk-Arbeitsbereich.
     *
     * @param dmxService      DMX-Service für künftige Universumsdaten
     * @param explorerService Projekt-Explorer-Service für gepatchte Scheinwerfer
     */
    public LightDeskWorkspaceView(DmxService dmxService, ProjectExplorerService explorerService) {
        this.dmxService = Objects.requireNonNull(dmxService, "dmxService darf nicht null sein");
        this.explorerService = explorerService;

        getStyleClass().add("lightdesk-workspace");
        setSpacing(0);
        VBox.setVgrow(this, Priority.ALWAYS);

        // 1. Headerzeile initialisieren
        this.comboUniverse = new ComboBox<>();
        HBox headerBar = buildHeaderBar();

        // 2. Oberes Fader-Dock (50 % der Bildschirmhöhe)
        this.canvasDeskView = new CanvasDeskView();

        this.topFaderDock = new VBox(canvasDeskView);
        topFaderDock.getStyleClass().add("desk-top-dock");
        VBox.setVgrow(canvasDeskView, Priority.ALWAYS);

        // 3. Unterer Bereich (50 % der Bildschirmhöhe) - reserviert für Schritt 2
        this.bottomWorkspaceArea = new StackPane();
        bottomWorkspaceArea.getStyleClass().add("desk-bottom-workspace");
        Label lblPlaceholder = new Label("LightDesk Arbeitsbereich (Schritt 2)");
        lblPlaceholder.getStyleClass().add("desk-placeholder-label");
        bottomWorkspaceArea.getChildren().add(lblPlaceholder);

        // 4. Vertikaler SplitPane (50% / 50%)
        this.splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.getStyleClass().add("desk-vertical-split");
        splitPane.getItems().addAll(topFaderDock, bottomWorkspaceArea);
        splitPane.setDividerPositions(0.5);
        SplitPane.setResizableWithParent(topFaderDock, true);
        SplitPane.setResizableWithParent(bottomWorkspaceArea, true);
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        getChildren().addAll(headerBar, splitPane);

        // Gepatchte Geräte laden & Event-Listener verdrahten
        refreshPatchedFixtures();
        if (explorerService != null) {
            explorerService.addChangeListener(() -> Platform.runLater(this::refreshPatchedFixtures));
        }
    }

    private HBox buildHeaderBar() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(8, 14, 8, 14));
        header.getStyleClass().add("desk-header-bar");

        // Universums-Dropdown
        comboUniverse.getItems().addAll("Universum 1", "Universum 2", "Universum 3", "Universum 4");
        comboUniverse.setValue("Universum 1");
        comboUniverse.getStyleClass().add("desk-universe-combo");
        comboUniverse.valueProperty().addListener((obs, oldVal, newVal) -> refreshPatchedFixtures());

        header.getChildren().addAll(new Label("Universum:"), comboUniverse);
        return header;
    }

    /**
     * Scrollt die Faderbank horizontal so, dass der Zielkanal sichtbar und zentriert wird.
     *
     * @param channel 1-basierter Kanal (1 bis 512)
     */
    public void scrollToChannel(int channel) {
        canvasDeskView.scrollToChannel(channel);
    }

    /**
     * Setzt alle 512 Kanäle im Desk auf den DMX-Wert 0 zurück.
     */
    public void resetAllChannels() {
        canvasDeskView.resetAll();
    }

    /**
     * Aktualisiert die gepatchten Scheinwerfer und beschriftet die zugehörigen Kanalstreifen.
     */
    public void refreshPatchedFixtures() {
        canvasDeskView.clearFixtureAssignments();

        if (explorerService == null) {
            return;
        }

        List<ExplorerItem> devices = explorerService.getItemsInFolder(ProjectExplorerService.FOLDER_DEVICES_ID);
        int selectedUniverse = getSelectedUniverseIndex(); // 0-basiert (0 bis 3)

        for (ExplorerItem item : devices) {
            FixtureInstance instance = item.getFixtureInstance();
            if (instance == null) {
                continue;
            }

            // Nur Scheinwerfer des aktuell gewählten Universums zuordnen
            if (instance.getUniverse() == selectedUniverse + 1) {
                int startAddr = instance.getStartAddress();
                if (instance.getActiveMode() != null && instance.getActiveMode().getChannels() != null) {
                    List<ChannelDefinition> channels = instance.getActiveMode().getChannels();
                    for (int i = 0; i < channels.size(); i++) {
                        int ch = startAddr + i;
                        if (ch >= 1 && ch <= TOTAL_CHANNELS) {
                            ChannelDefinition chDef = channels.get(i);
                            canvasDeskView.assignFixtureChannel(ch, instance.getName(), chDef.type(), item.getColor());
                        }
                    }
                }
            }
        }
    }

    private int getSelectedUniverseIndex() {
        String val = comboUniverse.getValue();
        if (val == null) return 0;
        return switch (val) {
            case "Universum 2" -> 1;
            case "Universum 3" -> 2;
            case "Universum 4" -> 3;
            default -> 0;
        };
    }

    public ComboBox<String> getUniverseComboBox() {
        return comboUniverse;
    }

    public CanvasDeskView getCanvasDeskView() {
        return canvasDeskView;
    }

    public SplitPane getSplitPane() {
        return splitPane;
    }

    public VBox getTopFaderDock() {
        return topFaderDock;
    }

    public StackPane getBottomWorkspaceArea() {
        return bottomWorkspaceArea;
    }
}

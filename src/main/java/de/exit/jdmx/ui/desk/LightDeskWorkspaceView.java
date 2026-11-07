package de.exit.jdmx.ui.desk;

import de.exit.jdmx.core.model.ChannelDefinition;
import de.exit.jdmx.core.model.ExplorerItem;
import de.exit.jdmx.core.model.FixtureInstance;
import de.exit.jdmx.service.DmxService;
import de.exit.jdmx.service.ProjectExplorerService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Objects;

/**
 * Arbeitsbereich für Tab 2 (LightDesk - Fader-Pult & Live-Steuerung).
 * <p>
 * Bietet ein professionelles Lichtmischpult-Layout mit 50/50-Teilung und voller DMX-Backend-Verdrahtung:
 * <ul>
 *     <li><b>Kopfzeile:</b> Universums-Umschaltung (Universum 1–4) mit autom. DMX-Puffer-Synchronisation.</li>
 *     <li><b>Oberes Fader-Dock (50 % Höhe):</b> Puristische Canvas-Balken mit DMX-Pegelwert (0..255), Kanalnummer,
 *         Funktions-Icons von lucide.dev, Custom-Scrollbar und Doppel-/Rechtsklick-Schnellnullung.</li>
 *     <li><b>Unterer Arbeitsbereich (50 % Höhe):</b> Interaktive {@link DeskFixtureListView} mit Scheinwerfer-Karten,
 *         Auto-Scroll beim Anklicken, DMX-Kanal-Inspektor und Quick Actions ([Locate], [Full], [Zero]).</li>
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
    private final DeskFixtureListView deskFixtureListView;

    private final DmxService.ChannelChangeListener dmxChannelListener;
    private boolean updatingFromDmx = false;

    /**
     * Erstellt einen neuen LightDesk-Arbeitsbereich mit voller DMX- und Projekt-Synchronisation.
     *
     * @param dmxService      DMX-Service für reale Universumsdaten & Netzwerk-Output
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

        // 3. Unterer Bereich: Vollwertige DeskFixtureListView (50 % der Bildschirmhöhe)
        this.deskFixtureListView = new DeskFixtureListView();
        this.bottomWorkspaceArea = new StackPane(deskFixtureListView);
        bottomWorkspaceArea.getStyleClass().add("desk-bottom-workspace");

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

        // ==========================================
        // 5. Bidirektionale DMX- & UI-Logik verdrahten
        // ==========================================

        // A. Fader-Änderung im Canvas -> DMX-Ausgabe über DmxService (nur wenn durch Benutzer ausgelöst)
        this.canvasDeskView.setOnChannelValueChanged((channel, value) -> {
            if (updatingFromDmx) {
                return;
            }
            int uniIdx = getSelectedUniverseIndex();
            dmxService.setChannel(uniIdx, channel, value);
            deskFixtureListView.updateChannelValue(uniIdx, channel, value);
        });

        // B. Externe DMX-Änderungen (z. B. durch Stage, Chaser, Blackout) -> Fader im Desk synchronisieren
        this.dmxChannelListener = (uniIdx, channel, value) -> {
            if (uniIdx == getSelectedUniverseIndex()) {
                Platform.runLater(() -> {
                    if (canvasDeskView.getChannelValue(channel) != value) {
                        updatingFromDmx = true;
                        try {
                            canvasDeskView.setChannelValue(channel, value);
                        } finally {
                            updatingFromDmx = false;
                        }
                    }
                    deskFixtureListView.updateChannelValue(uniIdx, channel, value);
                });
            }
        };
        this.dmxService.addChannelChangeListener(dmxChannelListener);

        // C. DeskFixtureListView Callbacks verdrahten
        this.deskFixtureListView.setOnJumpToChannel(this::scrollToChannel);
        this.deskFixtureListView.setOnLocateFixture(this::locateFixture);
        this.deskFixtureListView.setOnFullFixture(this::fullFixture);
        this.deskFixtureListView.setOnZeroFixture(this::zeroFixture);

        // Gepatchte Geräte laden & Event-Listener verdrahten
        refreshPatchedFixtures();
        loadUniverseValues(getSelectedUniverseIndex());

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
        comboUniverse.valueProperty().addListener((obs, oldVal, newVal) -> {
            refreshPatchedFixtures();
            loadUniverseValues(getSelectedUniverseIndex());
        });

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
     * Setzt alle 512 Kanäle des aktuellen Universums in der UI und im DMX-Backend auf 0 zurück.
     */
    public void resetAllChannels() {
        int uniIdx = getSelectedUniverseIndex();
        for (int ch = 1; ch <= TOTAL_CHANNELS; ch++) {
            dmxService.setChannel(uniIdx, ch, 0);
        }
        canvasDeskView.resetAll();
    }

    /**
     * Lädt die aktuellen DMX-Pufferwerte aus dem Service in das Fader-Desk.
     *
     * @param universeIndex Null-basierter Universums-Index (0 bis 3)
     */
    public void loadUniverseValues(int universeIndex) {
        if (dmxService == null || universeIndex < 0 || universeIndex >= dmxService.getUniverseCount()) {
            return;
        }
        byte[] data = dmxService.getUniverse(universeIndex).getDmxData();
        for (int i = 0; i < TOTAL_CHANNELS; i++) {
            int val = data[i] & 0xFF;
            canvasDeskView.setChannelValue(i + 1, val);
            deskFixtureListView.updateChannelValue(universeIndex, i + 1, val);
        }
    }

    /**
     * Führt eine standardkonforme Locate-Funktion für das gewählte Fixture aus.
     * (Dimmer 100%, Pan/Tilt Mitte, Shutter offen, RGBW 100%).
     *
     * @param fixture Scheinwerfer-Instanz
     */
    public void locateFixture(FixtureInstance fixture) {
        if (fixture == null || fixture.getActiveMode() == null) {
            return;
        }
        int uniIdx = fixture.getUniverse() - 1;
        int start = fixture.getStartAddress();

        for (ChannelDefinition ch : fixture.getActiveMode().getChannels()) {
            int absAddr = start + ch.index() - 1;
            boolean isStrobe = ch.name().toLowerCase(java.util.Locale.ROOT).contains("strobe")
                    || ch.name().toLowerCase(java.util.Locale.ROOT).contains("strobo");
            int val = switch (ch.type()) {
                case DIMMER -> 255;
                case SHUTTER -> isStrobe ? 0 : 255;
                case PAN, TILT -> 128;
                case RED, GREEN, BLUE, WHITE, AMBER, UV -> 255;
                default -> 0;
            };
            dmxService.setChannel(uniIdx, absAddr, val);
        }
    }

    /**
     * Setzt die Intensität des Scheinwerfers auf 100 % (Full).
     *
     * @param fixture Scheinwerfer-Instanz
     */
    public void fullFixture(FixtureInstance fixture) {
        if (fixture == null) return;
        dmxService.setIntensity(fixture, 1.0);
    }

    /**
     * Setzt die Intensität des Scheinwerfers auf 0 % (Zero / Aus).
     *
     * @param fixture Scheinwerfer-Instanz
     */
    public void zeroFixture(FixtureInstance fixture) {
        if (fixture == null) return;
        dmxService.setIntensity(fixture, 0.0);
    }

    /**
     * Aktualisiert die gepatchten Scheinwerfer und beschriftet die zugehörigen Kanalstreifen.
     */
    public void refreshPatchedFixtures() {
        canvasDeskView.clearFixtureAssignments();

        if (explorerService == null) {
            deskFixtureListView.setFixtures(List.of(), getSelectedUniverseIndex() + 1);
            return;
        }

        List<ExplorerItem> devices = explorerService.getItemsInFolder(ProjectExplorerService.FOLDER_DEVICES_ID);
        int selectedUniverse = getSelectedUniverseIndex(); // 0-basiert (0 bis 3)

        deskFixtureListView.setFixtures(devices, selectedUniverse + 1);

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

    public DeskFixtureListView getDeskFixtureListView() {
        return deskFixtureListView;
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

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
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Arbeitsbereich für Tab 2 (LightDesk - Fader-Pult).
 * <p>
 * Bietet ein professionelles Lichtmischpult-Layout im QLC+ Fader-Desk-Stil:
 * <ul>
 *     <li>Kopfzeile mit Universums-Umschaltung (Universum 1–4), Schnell-Reset und Bank-Navigation (1–32, 33–64...).</li>
 *     <li>Zentrale 512-Kanal-Faderbank mit reibungslosem horizontalen Scrolling und Dirty-Rendering.</li>
 *     <li>Untere Schnellzugriffsleiste mit gepatchten Scheinwerfern und Auto-Scroll zum Startkanal.</li>
 * </ul>
 * </p>
 */
public class LightDeskWorkspaceView extends VBox {

    public static final int TOTAL_CHANNELS = 512;
    public static final int CHANNELS_PER_BANK = 32;
    public static final int TOTAL_BANKS = TOTAL_CHANNELS / CHANNELS_PER_BANK; // 16 Bänke

    private final DmxService dmxService;
    private final ProjectExplorerService explorerService;

    // Header Controls
    private final ComboBox<String> comboUniverse;
    private final Button btnResetAll;
    private final HBox bankButtonContainer;
    private final List<Button> bankButtons = new ArrayList<>();
    private int currentBank = 1;

    // Faderbank
    private final ScrollPane faderScrollPane;
    private final HBox faderContainer;
    private final List<DeskChannelStrip> channelStrips = new ArrayList<>(TOTAL_CHANNELS);

    // Untere Leiste: Gepatchte Scheinwerfer
    private final ScrollPane fixtureScrollPane;
    private final HBox fixtureListContainer;
    private final List<LightDeskFixtureCard> fixtureCards = new ArrayList<>();

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
        this.btnResetAll = new Button("Alle Kanäle 0");
        this.bankButtonContainer = new HBox(4);
        HBox headerBar = buildHeaderBar();

        // 2. Faderbank (512 Kanäle) initialisieren
        this.faderContainer = new HBox(2);
        this.faderContainer.getStyleClass().add("desk-fader-container");
        this.faderContainer.setAlignment(Pos.CENTER_LEFT);
        this.faderContainer.setPadding(new Insets(8, 12, 8, 12));

        for (int ch = 1; ch <= TOTAL_CHANNELS; ch++) {
            DeskChannelStrip strip = new DeskChannelStrip(ch);
            channelStrips.add(strip);
            faderContainer.getChildren().add(strip);
        }

        this.faderScrollPane = new ScrollPane(faderContainer);
        this.faderScrollPane.getStyleClass().addAll("desk-fader-scrollpane", "edge-to-edge");
        this.faderScrollPane.setFitToHeight(true);
        this.faderScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        this.faderScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        VBox.setVgrow(faderScrollPane, Priority.ALWAYS);

        // 3. Untere Leiste: Gepatchte Scheinwerfer
        this.fixtureListContainer = new HBox(6);
        this.fixtureListContainer.getStyleClass().add("desk-fixture-container");
        this.fixtureListContainer.setAlignment(Pos.CENTER_LEFT);
        this.fixtureListContainer.setPadding(new Insets(6, 12, 6, 12));

        this.fixtureScrollPane = new ScrollPane(fixtureListContainer);
        this.fixtureScrollPane.getStyleClass().addAll("desk-fixture-scrollpane", "edge-to-edge");
        this.fixtureScrollPane.setFitToHeight(true);
        this.fixtureScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        this.fixtureScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        this.fixtureScrollPane.setMinHeight(52);
        this.fixtureScrollPane.setPrefHeight(52);
        this.fixtureScrollPane.setMaxHeight(52);

        HBox bottomBar = buildBottomBar();

        getChildren().addAll(headerBar, faderScrollPane, bottomBar);

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

        // Alle Kanäle auf 0 zurücksetzen
        btnResetAll.getStyleClass().add("desk-action-btn");
        btnResetAll.setGraphic(new LucideIcon("rotate-ccw", 13, StudioTheme.COLOR_ICON_RED));
        btnResetAll.setOnAction(e -> resetAllChannels());

        // Bänke Navigation
        bankButtonContainer.setAlignment(Pos.CENTER_LEFT);
        for (int b = 1; b <= TOTAL_BANKS; b++) {
            final int bankIndex = b;
            int startCh = (b - 1) * CHANNELS_PER_BANK + 1;
            int endCh = b * CHANNELS_PER_BANK;
            Button btnBank = new Button(startCh + "–" + endCh);
            btnBank.getStyleClass().add("desk-bank-btn");
            if (b == currentBank) {
                btnBank.getStyleClass().add("active");
            }
            btnBank.setOnAction(e -> selectBank(bankIndex));
            bankButtons.add(btnBank);
            bankButtonContainer.getChildren().add(btnBank);
        }

        ScrollPane bankScrollPane = new ScrollPane(bankButtonContainer);
        bankScrollPane.getStyleClass().addAll("desk-bank-scrollpane", "edge-to-edge");
        bankScrollPane.setFitToHeight(true);
        bankScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        bankScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        HBox.setHgrow(bankScrollPane, Priority.ALWAYS);

        header.getChildren().addAll(new Label("Universum:"), comboUniverse, btnResetAll, new Separator(javafx.geometry.Orientation.VERTICAL), new Label("Bänke:"), bankScrollPane);
        return header;
    }

    private HBox buildBottomBar() {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("desk-bottom-bar");

        Label lblSection = new Label("Gepatchte Geräte:");
        lblSection.getStyleClass().add("desk-bottom-label");
        lblSection.setPadding(new Insets(0, 0, 0, 14));

        HBox.setHgrow(fixtureScrollPane, Priority.ALWAYS);
        bar.getChildren().addAll(lblSection, fixtureScrollPane);
        return bar;
    }

    /**
     * Wählt eine Faderbank (1 bis 16) aus und scrollt die Faderbank dorthin.
     *
     * @param bankIndex 1-basierter Bank-Index (1 bis 16)
     */
    public void selectBank(int bankIndex) {
        if (bankIndex < 1 || bankIndex > TOTAL_BANKS) {
            return;
        }
        this.currentBank = bankIndex;

        // Button-Highlightings aktualisieren
        for (int i = 0; i < bankButtons.size(); i++) {
            Button btn = bankButtons.get(i);
            btn.getStyleClass().remove("active");
            if (i == bankIndex - 1) {
                btn.getStyleClass().add("active");
            }
        }

        int targetChannel = (bankIndex - 1) * CHANNELS_PER_BANK + 1;
        scrollToChannel(targetChannel);
    }

    /**
     * Scrollt die Faderbank horizontal so, dass der Zielkanal sichtbar und zentriert wird.
     *
     * @param channel 1-basierter Kanal (1 bis 512)
     */
    public void scrollToChannel(int channel) {
        int clamped = Math.max(1, Math.min(TOTAL_CHANNELS, channel));
        double targetH = (double) (clamped - 1) / (TOTAL_CHANNELS - 1);
        faderScrollPane.setHvalue(targetH);

        // Bank-Highlight aktualisieren
        int bank = ((clamped - 1) / CHANNELS_PER_BANK) + 1;
        this.currentBank = bank;
        for (int i = 0; i < bankButtons.size(); i++) {
            Button btn = bankButtons.get(i);
            btn.getStyleClass().remove("active");
            if (i == bank - 1) {
                btn.getStyleClass().add("active");
            }
        }
    }

    /**
     * Setzt alle 512 Kanäle im Desk auf den DMX-Wert 0 zurück.
     */
    public void resetAllChannels() {
        for (DeskChannelStrip strip : channelStrips) {
            strip.reset();
        }
    }

    /**
     * Aktualisiert die gepatchten Scheinwerfer und beschriftet die zugehörigen Kanalstreifen.
     */
    public void refreshPatchedFixtures() {
        fixtureCards.clear();
        fixtureListContainer.getChildren().clear();

        // Zuerst alle Kanalstreifen zurücksetzen
        for (DeskChannelStrip strip : channelStrips) {
            strip.clearFixtureAssignment();
        }

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

            // Nur Scheinwerfer des aktuell gewählten Universums anzeigen
            if (instance.getUniverse() == selectedUniverse + 1) {
                LightDeskFixtureCard card = new LightDeskFixtureCard(item, this::scrollToChannel);
                fixtureCards.add(card);
                fixtureListContainer.getChildren().add(card);

                // Kanäle beschriften
                int startAddr = instance.getStartAddress();
                if (instance.getActiveMode() != null && instance.getActiveMode().getChannels() != null) {
                    List<ChannelDefinition> channels = instance.getActiveMode().getChannels();
                    for (int i = 0; i < channels.size(); i++) {
                        int chIndex = startAddr - 1 + i;
                        if (chIndex >= 0 && chIndex < channelStrips.size()) {
                            ChannelDefinition chDef = channels.get(i);
                            DeskChannelStrip strip = channelStrips.get(chIndex);
                            strip.assignFixtureChannel(instance.getName(), chDef.type(), item.getColor());
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

    public HBox getFaderContainer() {
        return faderContainer;
    }

    public List<DeskChannelStrip> getChannelStrips() {
        return Collections.unmodifiableList(channelStrips);
    }

    public ScrollPane getFaderScrollPane() {
        return faderScrollPane;
    }

    public HBox getFixtureListContainer() {
        return fixtureListContainer;
    }

    public List<LightDeskFixtureCard> getFixtureCards() {
        return Collections.unmodifiableList(fixtureCards);
    }

    public int getCurrentBank() {
        return currentBank;
    }

    public Button getBtnResetAll() {
        return btnResetAll;
    }
}

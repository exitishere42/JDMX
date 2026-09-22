package de.exit.jdmx.ui.tabs;

import de.exit.jdmx.ui.icons.LucideIcon;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Tab 2: Fader & Universum (QLC+ Simple Desk JavaFX Re-implementation).
 * 
 * Spezifikation:
 * 1. Top Section: Universe Selection Bar (HBox, ComboBox "Universe 1", Reset-Button "X")
 * 2. Center Section: DMX Fader Grid (ScrollPane mit HBox, 35+ vertikale Channel Strips)
 *    Jeder Strip (VBox, 36px breit):
 *    - Vertical Slider (0-255, dynamische VGrow Höhe)
 *    - Current Value Label (0-255, ganzzahlig)
 *    - Channel Number Label ("1", "2", ..., "35")
 *    - Channel Flash Button (Lucide ZAP Icon, gedrückt = 255, losgelassen = 0)
 * 3. Bottom Section: Fixture List Panel (Header "#2c3e50", Content "#181818")
 */
public class FaderUniverseTab extends VBox {

    public static final int DEFAULT_CHANNEL_COUNT = 35;
    private final List<ChannelStripHolder> channelStrips = new ArrayList<>();
    private final ComboBox<String> universeSelector = new ComboBox<>();
    private final ScrollPane faderScrollPane = new ScrollPane();
    private final HBox faderGridHBox = new HBox(2);

    private static class ChannelStripHolder {
        public int channelNumber; // 1-based
        public Slider slider;
        public Label valLabel;
        public Label chNumLabel;
        public Button flashBtn;
        public VBox stripUI;
    }

    /**
     * Konstruiert das Fader & Universum Tab nach der QLC+ Simple Desk Spezifikation.
     */
    public FaderUniverseTab() {
        setSpacing(0);
        setStyle("-fx-background-color: #1e1e1e;");
        VBox.setVgrow(this, Priority.ALWAYS);

        // 1. Top Section: Universe Selection Bar
        HBox topBar = buildUniverseBar();

        // 2. Center Section: DMX Fader Grid
        Region faderGridSection = buildFaderGrid();
        VBox.setVgrow(faderGridSection, Priority.ALWAYS);

        // 3. Bottom Section: Fixture List Panel
        VBox fixtureListSection = buildFixtureListPanel();
        fixtureListSection.setPrefHeight(180);
        fixtureListSection.setMinHeight(140);

        getChildren().addAll(topBar, faderGridSection, fixtureListSection);
    }

    /**
     * Baut die obere Leiste zur Auswahl des Universums (Universe Selection Bar).
     *
     * @return HBox Leiste.
     */
    private HBox buildUniverseBar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(5, 10, 5, 10));
        bar.setStyle("-fx-background-color: #222222; -fx-border-color: #333333; -fx-border-width: 0 0 1px 0;");

        Label universeLabel = new Label("Universe");
        universeLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 13px;");

        universeSelector.getItems().addAll("Universe 1", "Universe 2", "Universe 3", "Universe 4");
        universeSelector.getSelectionModel().select("Universe 1");
        universeSelector.setStyle("-fx-background-color: #2c2c2c; -fx-text-fill: #ffffff; -fx-border-color: #444444; -fx-border-radius: 3px;");

        Button resetAllBtn = new Button("X");
        resetAllBtn.setPrefSize(25, 25);
        resetAllBtn.setMinSize(25, 25);
        resetAllBtn.setMaxSize(25, 25);
        resetAllBtn.setStyle("-fx-background-color: #3a2222; -fx-text-fill: #ff5555; -fx-font-weight: bold; -fx-padding: 0; -fx-cursor: hand; -fx-border-color: #552222; -fx-border-radius: 3px;");
        resetAllBtn.setOnAction(e -> resetAllFaders());

        bar.getChildren().addAll(universeLabel, universeSelector, resetAllBtn);
        return bar;
    }

    /**
     * Baut den Hauptbereich (DMX Fader Grid) mit ScrollPane und HBox.
     *
     * @return ScrollPane Container.
     */
    private Region buildFaderGrid() {
        faderScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        faderScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        faderScrollPane.setFitToHeight(true);
        faderScrollPane.setStyle("-fx-background-color: #1e1e1e; -fx-viewport-background-color: #1e1e1e; -fx-border-color: transparent;");

        faderGridHBox.setAlignment(Pos.BOTTOM_LEFT);
        faderGridHBox.setPadding(new Insets(6, 6, 6, 6));
        faderGridHBox.setStyle("-fx-background-color: #1e1e1e;");

        // Erzeuge 35 dynamische Channel Strips nach Spezifikation
        for (int ch = 1; ch <= DEFAULT_CHANNEL_COUNT; ch++) {
            ChannelStripHolder holder = createChannelStrip(ch);
            channelStrips.add(holder);
            faderGridHBox.getChildren().add(holder.stripUI);
        }

        faderScrollPane.setContent(faderGridHBox);
        return faderScrollPane;
    }

    /**
     * Erstellt einen einzelnen Channel Strip (VBox, PrefWidth: 36px).
     *
     * @param channelNum 1-basierte Kanalnummer.
     * @return ChannelStripHolder mit UI und Datenbindung.
     */
    private ChannelStripHolder createChannelStrip(int channelNum) {
        ChannelStripHolder holder = new ChannelStripHolder();
        holder.channelNumber = channelNum;

        VBox strip = new VBox(6);
        strip.setAlignment(Pos.BOTTOM_CENTER);
        strip.setPrefWidth(36);
        strip.setMinWidth(36);
        strip.setMaxWidth(36);
        strip.setStyle("-fx-background-color: #242424; -fx-border-color: #2d2d2d; -fx-border-width: 1px; -fx-padding: 6px 2px 6px 2px; -fx-border-radius: 2px;");
        VBox.setVgrow(strip, Priority.ALWAYS);

        // 1. Vertical Slider (Erweitert sich dynamisch mit VGrow ALWAYS)
        holder.slider = new Slider(0, 255, 0);
        holder.slider.setOrientation(Orientation.VERTICAL);
        holder.slider.setMinHeight(200);
        holder.slider.setStyle(".slider .track { -fx-background-color: #555555; -fx-pref-width: 3px; } .slider .thumb { -fx-background-color: #dddddd; -fx-border-color: #888888; }");
        VBox.setVgrow(holder.slider, Priority.ALWAYS);

        // 2. Current Value Label (Bindung an Ganzzahl 0-255)
        holder.valLabel = new Label("0");
        holder.valLabel.setAlignment(Pos.CENTER);
        holder.valLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 11px; -fx-font-family: monospace;");
        holder.valLabel.setMaxWidth(Double.MAX_VALUE);

        holder.slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            holder.valLabel.setText(String.valueOf(newVal.intValue()));
        });

        // 3. Channel Number Label (Fett 12pt)
        holder.chNumLabel = new Label(String.valueOf(channelNum));
        holder.chNumLabel.setAlignment(Pos.CENTER);
        holder.chNumLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 12px;");
        holder.chNumLabel.setMaxWidth(Double.MAX_VALUE);

        // 4. Channel Flash Button (Quadratisch mit Lucide ZAP Icon)
        holder.flashBtn = new Button();
        holder.flashBtn.setAlignment(Pos.CENTER);
        holder.flashBtn.setPrefSize(26, 26);
        holder.flashBtn.setMinSize(26, 26);
        holder.flashBtn.setMaxSize(26, 26);
        holder.flashBtn.setGraphic(LucideIcon.create(LucideIcon.ZAP, Color.web("#03DAC6"), 0.65));
        holder.flashBtn.setStyle("-fx-background-color: #333333; -fx-padding: 0; -fx-cursor: hand; -fx-border-color: #444444; -fx-border-radius: 2px;");
        
        holder.flashBtn.setOnMousePressed(e -> {
            holder.slider.setValue(255);
            holder.flashBtn.setStyle("-fx-background-color: #03DAC6; -fx-padding: 0; -fx-cursor: hand; -fx-border-color: #03DAC6; -fx-border-radius: 2px;");
            holder.flashBtn.setGraphic(LucideIcon.create(LucideIcon.ZAP, Color.BLACK, 0.65));
        });
        holder.flashBtn.setOnMouseReleased(e -> {
            holder.slider.setValue(0);
            holder.flashBtn.setStyle("-fx-background-color: #333333; -fx-padding: 0; -fx-cursor: hand; -fx-border-color: #444444; -fx-border-radius: 2px;");
            holder.flashBtn.setGraphic(LucideIcon.create(LucideIcon.ZAP, Color.web("#03DAC6"), 0.65));
        });

        strip.getChildren().addAll(holder.slider, holder.valLabel, holder.chNumLabel, holder.flashBtn);
        holder.stripUI = strip;
        return holder;
    }

    /**
     * Setzt alle Fader auf 0 zurück.
     */
    public void resetAllFaders() {
        for (ChannelStripHolder holder : channelStrips) {
            holder.slider.setValue(0);
        }
    }

    /**
     * Baut das Fixture List Panel im unteren Bereich.
     *
     * @return VBox Panel.
     */
    private VBox buildFixtureListPanel() {
        VBox panel = new VBox(0);
        panel.setStyle("-fx-background-color: #181818; -fx-border-color: #2c3e50; -fx-border-width: 1px 0 0 0;");

        // Header Bar: Dunkelblau #2c3e50
        HBox headerBar = new HBox();
        headerBar.setAlignment(Pos.CENTER_LEFT);
        headerBar.setPadding(new Insets(6, 12, 6, 12));
        headerBar.setStyle("-fx-background-color: #2c3e50;");

        Label titleLabel = new Label("Fixture List");
        titleLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 12px;");
        headerBar.getChildren().add(titleLabel);

        // Content Area: ListView / Placeholder Pane
        ListView<String> fixtureListView = new ListView<>();
        fixtureListView.setStyle("-fx-background-color: #181818; -fx-control-inner-background: #181818; -fx-border-color: transparent;");
        VBox.setVgrow(fixtureListView, Priority.ALWAYS);

        panel.getChildren().addAll(headerBar, fixtureListView);
        return panel;
    }
}

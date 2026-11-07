package de.exit.jdmx.ui.desk;

import de.exit.jdmx.core.model.*;
import de.exit.jdmx.ui.icon.LucideIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.*;
import java.util.function.Consumer;

/**
 * Interaktive Scheinwerfer-Liste und Geräte-Inspektor für den unteren Bereich des LightDesk (Tab 2).
 * <p>
 * Bietet eine strukturierte Übersicht aller im ausgewählten Universum gepatchten Geräte:
 * <ul>
 *     <li><b>Kompakte Geräteliste:</b> Name, Kanalbereich (z. B. DMX 1–16), Kanalanzahl und Typ mit Farbkennung.</li>
 *     <li><b>Auto-Scroll & Fokus:</b> Klick auf ein Fixture scrollt das obere Fader-Dock sofort zum Startkanal.</li>
 *     <li><b>Quick Actions:</b> Schnelltaster für [Locate], [Full (100%)] und [Zero (0%)].</li>
 *     <li><b>Kanal-Inspektor:</b> Live-Anzeige aller DMX-Funktionen des gewählten Geräts mit Sprungfunktion.</li>
 * </ul>
 * </p>
 */
public class DeskFixtureListView extends HBox {

    private final VBox listPanel;
    private final VBox inspectorPanel;
    private final VBox cardsContainer;
    private final VBox channelsContainer;

    private final Label lblListTitle;
    private final Label lblListCount;
    private final TextField txtFilter;

    private final Label lblInspectorTitle;
    private final Label lblInspectorDetails;
    private final HBox actionBar;
    private final Button btnLocate;
    private final Button btnFull;
    private final Button btnZero;
    private final Label lblNoSelection;
    private final Label lblEmptyList;

    private final List<ExplorerItem> currentFixtures = new ArrayList<>();
    private final Map<Integer, Integer> liveChannelValues = new HashMap<>();
    private int currentUniverse = 1;
    private FixtureInstance selectedFixture;

    private Consumer<Integer> onJumpToChannel;
    private Consumer<FixtureInstance> onLocateFixture;
    private Consumer<FixtureInstance> onFullFixture;
    private Consumer<FixtureInstance> onZeroFixture;

    public DeskFixtureListView() {
        setSpacing(12);
        setPadding(new Insets(10, 14, 10, 14));
        setStyle("-fx-background-color: #121212;");
        VBox.setVgrow(this, Priority.ALWAYS);

        // ==========================================
        // 1. Linkes Panel: Gepatchte Scheinwerfer
        // ==========================================
        this.listPanel = new VBox(8);
        listPanel.setStyle("-fx-background-color: #181818; -fx-background-radius: 4px; -fx-border-color: #262626; -fx-border-radius: 4px;");
        listPanel.setPadding(new Insets(10));
        HBox.setHgrow(listPanel, Priority.ALWAYS);

        // Kopfzeile links
        HBox listHeader = new HBox(8);
        listHeader.setAlignment(Pos.CENTER_LEFT);

        this.lblListTitle = new Label("Gepatchte Scheinwerfer");
        lblListTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblListTitle.setTextFill(Color.web("#E4E4E7"));

        this.lblListCount = new Label("0");
        lblListCount.setStyle("-fx-background-color: #27272A; -fx-text-fill: #A1A1AA; -fx-padding: 2 6 2 6; -fx-background-radius: 10px; -fx-font-size: 11px;");

        Region spacerL = new Region();
        HBox.setHgrow(spacerL, Priority.ALWAYS);

        this.txtFilter = new TextField();
        txtFilter.setPromptText("Filter...");
        txtFilter.setStyle("-fx-background-color: #222222; -fx-text-fill: #FFFFFF; -fx-prompt-text-fill: #71717A; -fx-border-color: #333333; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 4 8;");
        txtFilter.setPrefWidth(120);
        txtFilter.textProperty().addListener((obs, oldVal, newVal) -> rebuildCards());

        listHeader.getChildren().addAll(lblListTitle, lblListCount, spacerL, txtFilter);

        // Scrollbare Liste der Scheinwerfer-Karten
        this.cardsContainer = new VBox(6);
        ScrollPane listScroll = new ScrollPane(cardsContainer);
        listScroll.setFitToWidth(true);
        listScroll.setStyle("-fx-background: #181818; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(listScroll, Priority.ALWAYS);

        this.lblEmptyList = new Label("Keine Scheinwerfer in diesem Universum gepatcht");
        lblEmptyList.setTextFill(Color.web("#71717A"));
        lblEmptyList.setFont(Font.font("Segoe UI", 12));
        lblEmptyList.setAlignment(Pos.CENTER);
        lblEmptyList.setPadding(new Insets(20));

        listPanel.getChildren().addAll(listHeader, listScroll);

        // ==========================================
        // 2. Rechtes Panel: Geräte-Inspektor & Aktionen
        // ==========================================
        this.inspectorPanel = new VBox(8);
        inspectorPanel.setStyle("-fx-background-color: #181818; -fx-background-radius: 4px; -fx-border-color: #262626; -fx-border-radius: 4px;");
        inspectorPanel.setPadding(new Insets(10));
        HBox.setHgrow(inspectorPanel, Priority.ALWAYS);

        this.lblInspectorTitle = new Label("Geräte-Inspektor");
        lblInspectorTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblInspectorTitle.setTextFill(Color.web("#E4E4E7"));

        this.lblInspectorDetails = new Label("Wähle ein Gerät aus");
        lblInspectorDetails.setFont(Font.font("Segoe UI", 11));
        lblInspectorDetails.setTextFill(Color.web("#A1A1AA"));

        // Schnelltasten
        this.actionBar = new HBox(8);
        actionBar.setAlignment(Pos.CENTER_LEFT);
        actionBar.setPadding(new Insets(4, 0, 4, 0));

        this.btnLocate = new Button("Locate");
        btnLocate.setGraphic(new LucideIcon("sun", 12, Color.web("#F59E0B")));
        btnLocate.setStyle("-fx-background-color: #27272A; -fx-text-fill: #E4E4E7; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 4px; -fx-cursor: hand;");
        btnLocate.setOnAction(e -> triggerLocate());

        this.btnFull = new Button("100%");
        btnFull.setGraphic(new LucideIcon("zap", 12, Color.web("#EAB308")));
        btnFull.setStyle("-fx-background-color: #27272A; -fx-text-fill: #E4E4E7; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 4px; -fx-cursor: hand;");
        btnFull.setOnAction(e -> triggerFull());

        this.btnZero = new Button("0%");
        btnZero.setGraphic(new LucideIcon("power", 12, Color.web("#EF4444")));
        btnZero.setStyle("-fx-background-color: #27272A; -fx-text-fill: #E4E4E7; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 4px; -fx-cursor: hand;");
        btnZero.setOnAction(e -> triggerZero());

        actionBar.getChildren().addAll(btnLocate, btnFull, btnZero);

        // Kanal-Liste des selektierten Geräts
        this.channelsContainer = new VBox(4);
        ScrollPane channelsScroll = new ScrollPane(channelsContainer);
        channelsScroll.setFitToWidth(true);
        channelsScroll.setStyle("-fx-background: #181818; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(channelsScroll, Priority.ALWAYS);

        this.lblNoSelection = new Label("Wähle einen Scheinwerfer aus der Liste, um Kanäle und Schnellsteuerungen anzuzeigen.");
        lblNoSelection.setTextFill(Color.web("#71717A"));
        lblNoSelection.setFont(Font.font("Segoe UI", 12));
        lblNoSelection.setWrapText(true);
        lblNoSelection.setAlignment(Pos.CENTER);
        lblNoSelection.setPadding(new Insets(20));

        inspectorPanel.getChildren().addAll(lblInspectorTitle, lblInspectorDetails, lblNoSelection);

        getChildren().addAll(listPanel, inspectorPanel);
    }

    /**
     * Übergibt die aktuelle Liste aller Explorer-Elemente und filtert auf das aktive Universum.
     *
     * @param items          Alle Explorer-Elemente (Geräteordner)
     * @param activeUniverse Aktives Universum (1-basiert, 1 bis 4)
     */
    public void setFixtures(List<ExplorerItem> items, int activeUniverse) {
        this.currentUniverse = activeUniverse;
        this.currentFixtures.clear();
        if (items != null) {
            for (ExplorerItem it : items) {
                if (it.getFixtureInstance() != null && it.getFixtureInstance().getUniverse() == activeUniverse) {
                    currentFixtures.add(it);
                }
            }
        }
        rebuildCards();

        // Wenn vorherige Selektion nicht mehr im Universum ist, zurücksetzen
        if (selectedFixture != null) {
            boolean stillPresent = currentFixtures.stream()
                    .anyMatch(it -> it.getFixtureInstance() != null && it.getFixtureInstance().getId().equals(selectedFixture.getId()));
            if (!stillPresent) {
                deselectFixture();
            } else {
                updateInspector();
            }
        }
    }

    private void rebuildCards() {
        cardsContainer.getChildren().clear();
        String filter = txtFilter.getText() != null ? txtFilter.getText().toLowerCase(Locale.ROOT).trim() : "";

        List<ExplorerItem> matched = currentFixtures.stream()
                .filter(it -> {
                    if (filter.isEmpty()) return true;
                    FixtureInstance fi = it.getFixtureInstance();
                    return it.getName().toLowerCase(Locale.ROOT).contains(filter)
                            || (fi != null && fi.getDefinition().getModel().toLowerCase(Locale.ROOT).contains(filter))
                            || (fi != null && String.valueOf(fi.getStartAddress()).contains(filter));
                })
                .sorted(Comparator.comparingInt(it -> it.getFixtureInstance() != null ? it.getFixtureInstance().getStartAddress() : 0))
                .toList();

        lblListCount.setText(String.valueOf(matched.size()));

        if (matched.isEmpty()) {
            cardsContainer.getChildren().add(lblEmptyList);
            return;
        }

        for (ExplorerItem it : matched) {
            FixtureInstance fi = it.getFixtureInstance();
            if (fi == null) continue;

            HBox card = buildFixtureCard(it, fi);
            cardsContainer.getChildren().add(card);
        }
    }

    private HBox buildFixtureCard(ExplorerItem it, FixtureInstance fi) {
        HBox card = new HBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(7, 10, 7, 10));

        boolean isSelected = selectedFixture != null && selectedFixture.getId().equals(fi.getId());
        if (isSelected) {
            card.setStyle("-fx-background-color: #27272A; -fx-background-radius: 4px; -fx-border-color: #F59E0B; -fx-border-radius: 4px; -fx-border-width: 1px; -fx-cursor: hand;");
        } else {
            card.setStyle("-fx-background-color: #1E1E1E; -fx-background-radius: 4px; -fx-border-color: #2A2A2A; -fx-border-radius: 4px; -fx-cursor: hand;");
        }

        Circle colorDot = new Circle(4, it.getColor() != null ? it.getColor() : Color.web("#A1A1AA"));

        VBox textCol = new VBox(2);
        Label lblName = new Label(fi.getName());
        lblName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblName.setTextFill(Color.web("#F4F4F5"));

        int start = fi.getStartAddress();
        int count = fi.getActiveMode() != null ? fi.getActiveMode().getChannels().size() : 1;
        int end = start + count - 1;
        String typeName = fi.getDefinition() != null ? fi.getDefinition().getModel() : "Scheinwerfer";

        Label lblSub = new Label("DMX " + start + "–" + end + " (" + count + " Kanäle) • " + typeName);
        lblSub.setFont(Font.font("Segoe UI", 10.5));
        lblSub.setTextFill(Color.web("#A1A1AA"));
        textCol.getChildren().addAll(lblName, lblSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnJump = new Button();
        btnJump.setGraphic(new LucideIcon("arrow-up-right", 12, Color.web("#A1A1AA")));
        btnJump.setStyle("-fx-background-color: transparent; -fx-padding: 4; -fx-cursor: hand;");
        btnJump.setTooltip(new Tooltip("Im Fader-Desk anzeigen"));
        btnJump.setOnAction(e -> {
            e.consume();
            selectFixture(fi);
        });

        card.getChildren().addAll(colorDot, textCol, spacer, btnJump);

        card.setOnMouseClicked(e -> selectFixture(fi));

        return card;
    }

    /**
     * Wählt ein Fixture aus, aktualisiert den Inspektor und scrollt das Fader-Dock zum Startkanal.
     *
     * @param fixture Ausgewählte Scheinwerfer-Instanz
     */
    public void selectFixture(FixtureInstance fixture) {
        this.selectedFixture = fixture;
        rebuildCards();
        updateInspector();

        if (fixture != null && onJumpToChannel != null) {
            onJumpToChannel.accept(fixture.getStartAddress());
        }
    }

    public void deselectFixture() {
        this.selectedFixture = null;
        inspectorPanel.getChildren().setAll(lblInspectorTitle, lblInspectorDetails, lblNoSelection);
        rebuildCards();
    }

    private void updateInspector() {
        if (selectedFixture == null) {
            deselectFixture();
            return;
        }

        lblInspectorTitle.setText(selectedFixture.getName());
        int start = selectedFixture.getStartAddress();
        int count = selectedFixture.getActiveMode() != null ? selectedFixture.getActiveMode().getChannels().size() : 0;
        int end = start + count - 1;
        String def = selectedFixture.getDefinition().getManufacturer() + " " + selectedFixture.getDefinition().getModel();
        lblInspectorDetails.setText("DMX " + start + "–" + end + " (" + count + " Kanäle) • " + def);

        rebuildChannelList();

        inspectorPanel.getChildren().setAll(lblInspectorTitle, lblInspectorDetails, actionBar, channelsContainer);
    }

    private void rebuildChannelList() {
        channelsContainer.getChildren().clear();
        if (selectedFixture == null || selectedFixture.getActiveMode() == null) {
            return;
        }

        int start = selectedFixture.getStartAddress();
        List<ChannelDefinition> channels = selectedFixture.getActiveMode().getChannels();

        for (int i = 0; i < channels.size(); i++) {
            ChannelDefinition chDef = channels.get(i);
            int absAddr = start + i;
            int val = liveChannelValues.getOrDefault(absAddr, 0);

            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(5, 8, 5, 8));
            row.setStyle("-fx-background-color: #222222; -fx-background-radius: 4px; -fx-cursor: hand;");

            Label lblChNum = new Label("CH " + (i + 1));
            lblChNum.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
            lblChNum.setTextFill(Color.web("#71717A"));
            lblChNum.setPrefWidth(36);

            Label lblName = new Label(chDef.name());
            lblName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            lblName.setTextFill(Color.web("#E4E4E7"));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label lblAddr = new Label("DMX " + absAddr);
            lblAddr.setFont(Font.font("Segoe UI", 10.5));
            lblAddr.setTextFill(Color.web("#A1A1AA"));

            Label lblVal = new Label(val + " (" + (val * 100 / 255) + "%)");
            lblVal.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            lblVal.setTextFill(val > 0 ? Color.web("#F59E0B") : Color.web("#52525B"));
            lblVal.setPrefWidth(65);
            lblVal.setAlignment(Pos.CENTER_RIGHT);

            row.getChildren().addAll(lblChNum, lblName, spacer, lblAddr, lblVal);

            // Klick auf eine Kanalzeile springt exakt zu diesem Fader
            row.setOnMouseClicked(e -> {
                if (onJumpToChannel != null) {
                    onJumpToChannel.accept(absAddr);
                }
            });

            channelsContainer.getChildren().add(row);
        }
    }

    /**
     * Aktualisiert den Live-Wert eines DMX-Kanals für die Anzeige im Inspektor.
     *
     * @param universeIndex Universum (0-basiert)
     * @param channel       1-basierter Kanal (1 bis 512)
     * @param value         DMX-Wert (0 bis 255)
     */
    public void updateChannelValue(int universeIndex, int channel, int value) {
        if (universeIndex + 1 == currentUniverse) {
            liveChannelValues.put(channel, value);
            if (selectedFixture != null) {
                int start = selectedFixture.getStartAddress();
                int count = selectedFixture.getActiveMode() != null ? selectedFixture.getActiveMode().getChannels().size() : 0;
                if (channel >= start && channel < start + count) {
                    rebuildChannelList();
                }
            }
        }
    }

    public void triggerLocate() {
        if (selectedFixture != null && onLocateFixture != null) {
            onLocateFixture.accept(selectedFixture);
        }
    }

    public void triggerFull() {
        if (selectedFixture != null && onFullFixture != null) {
            onFullFixture.accept(selectedFixture);
        }
    }

    public void triggerZero() {
        if (selectedFixture != null && onZeroFixture != null) {
            onZeroFixture.accept(selectedFixture);
        }
    }

    public FixtureInstance getSelectedFixture() {
        return selectedFixture;
    }

    public int getDisplayedFixturesCount() {
        return currentFixtures.size();
    }

    public void setOnJumpToChannel(Consumer<Integer> onJumpToChannel) {
        this.onJumpToChannel = onJumpToChannel;
    }

    public void setOnLocateFixture(Consumer<FixtureInstance> onLocateFixture) {
        this.onLocateFixture = onLocateFixture;
    }

    public void setOnFullFixture(Consumer<FixtureInstance> onFullFixture) {
        this.onFullFixture = onFullFixture;
    }

    public void setOnZeroFixture(Consumer<FixtureInstance> onZeroFixture) {
        this.onZeroFixture = onZeroFixture;
    }
}

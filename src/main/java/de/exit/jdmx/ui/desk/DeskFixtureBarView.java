package de.exit.jdmx.ui.desk;

import de.exit.jdmx.core.i18n.I18n;
import de.exit.jdmx.core.model.ExplorerItem;
import de.exit.jdmx.core.model.FixtureDefinition;
import de.exit.jdmx.core.model.FixtureInstance;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Schlanke, einzeilige Fixture-Leiste für den unteren Bereich des LightDesk (Tab 2).
 * <p>
 * Ersetzt die vorherige zweigeteilte Ansicht (große Kacheln + klobiger Kanal-Inspektor) durch
 * eine kompakte, horizontale Liste:
 * <ul>
 *     <li><b>Format:</b> {@code [Brand] [Model/Name]   │   [Start]–[End]   │   [N] Kanäle}</li>
 *     <li><b>Dezente Trennstriche:</b> Gekürzte vertikale Linien (#3F3F46, 14 px Höhe) identisch zur HeaderBar.</li>
 *     <li><b>Direktsprung:</b> Ein Klick auf eine Zeile scrollt das obere Faderdock direkt zur Startadresse.</li>
 *     <li><b>I18n-Reaktivität:</b> Reagiert dynamisch auf Sprachwechsel (DE, EN, RU).</li>
 * </ul>
 * </p>
 */
public class DeskFixtureBarView extends VBox {

    private final Label titleLabel;
    private final VBox rowsContainer;
    private final ScrollPane scrollPane;

    private final List<ExplorerItem> cachedItems = new ArrayList<>();
    private int cachedUniverse = 1;

    private Consumer<Integer> onJumpToChannel;
    private final Consumer<String> languageListener;

    /**
     * Erstellt die schlanke Fixture-Leiste.
     */
    public DeskFixtureBarView() {
        setSpacing(6);
        setPadding(new Insets(8, 14, 8, 14));
        setStyle("-fx-background-color: #141416; -fx-border-color: #27272A; -fx-border-width: 1 0 0 0;");
        setPrefHeight(140);
        setMinHeight(80);
        setMaxHeight(220);

        // Überschrift (Fixture List / Fixture-Liste / Список приборов)
        this.titleLabel = new Label(I18n.get("fixture.list.title"));
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        titleLabel.setStyle("-fx-text-fill: #71717A; -fx-font-weight: bold; -fx-font-size: 11px;");

        // Zeilen-Container
        this.rowsContainer = new VBox(4);
        rowsContainer.setAlignment(Pos.CENTER_LEFT);

        // ScrollPane für viele gepatchte Geräte
        this.scrollPane = new ScrollPane(rowsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(titleLabel, scrollPane);

        // Dynamischer Sprachwechsel-Listener (sofort ausführen wenn bereits auf dem FX-Thread)
        this.languageListener = lang -> {
            if (Platform.isFxApplicationThread()) {
                updateLocalization();
            } else {
                Platform.runLater(this::updateLocalization);
            }
        };
        I18n.addLanguageChangeListener(languageListener);
    }

    /**
     * Setzt die gepatchten Fixtures für das angegebene Universum.
     *
     * @param items           Liste aller Explorer-Items
     * @param currentUniverse 1-basiertes Universum (1 bis 4)
     */
    public void setFixtures(List<ExplorerItem> items, int currentUniverse) {
        this.cachedItems.clear();
        if (items != null) {
            this.cachedItems.addAll(items);
        }
        this.cachedUniverse = currentUniverse;
        rebuildRows();
    }

    /**
     * Setzt den Callback für den Klick auf ein Fixture zur Fader-Positionierung.
     *
     * @param onJumpToChannel Callback mit 1-basierter Startadresse
     */
    public void setOnJumpToChannel(Consumer<Integer> onJumpToChannel) {
        this.onJumpToChannel = onJumpToChannel;
    }

    /**
     * Liefert das Überschriften-Label (für Tests & Bindings).
     *
     * @return Label
     */
    public Label getTitleLabel() {
        return titleLabel;
    }

    /**
     * Liefert die Anzahl der aktuell dargestellten Fixture-Zeilen.
     *
     * @return Zeilenanzahl
     */
    public int getFixtureRowCount() {
        return rowsContainer.getChildren().size();
    }

    /**
     * Liefert die Fixture-Zeile am angegebenen Index.
     *
     * @param index Null-basierter Zeilenindex
     * @return Zeilen-HBox
     */
    public HBox getFixtureRow(int index) {
        if (index >= 0 && index < rowsContainer.getChildren().size()) {
            return (HBox) rowsContainer.getChildren().get(index);
        }
        return null;
    }

    private void updateLocalization() {
        titleLabel.setText(I18n.get("fixture.list.title"));
        rebuildRows();
    }

    private void rebuildRows() {
        rowsContainer.getChildren().clear();

        List<FixtureInstance> matchedInstances = new ArrayList<>();
        for (ExplorerItem item : cachedItems) {
            FixtureInstance inst = item.getFixtureInstance();
            if (inst != null && inst.getUniverse() == cachedUniverse) {
                matchedInstances.add(inst);
            }
        }

        matchedInstances.sort(Comparator.comparingInt(FixtureInstance::getStartAddress));

        for (FixtureInstance inst : matchedInstances) {
            HBox row = createFixtureRow(inst);
            rowsContainer.getChildren().add(row);
        }
    }

    private HBox createFixtureRow(FixtureInstance inst) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 10, 4, 10));
        row.setStyle("-fx-background-color: #18181B; -fx-background-radius: 4px; -fx-cursor: hand;");

        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #27272A; -fx-background-radius: 4px; -fx-cursor: hand;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: #18181B; -fx-background-radius: 4px; -fx-cursor: hand;"));

        // Name mit Brand-Präfix
        String displayName = formatDisplayName(inst);
        Label lblName = new Label(displayName);
        lblName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblName.setTextFill(Color.web("#F4F4F5"));

        // Trennlinie 1
        Line sep1 = createSeparator();

        // Adressbereich (Start–End) mit en-dash
        int start = inst.getStartAddress();
        int chCount = (inst.getActiveMode() != null && inst.getActiveMode().getChannels() != null)
                ? inst.getActiveMode().getChannels().size() : 1;
        int end = start + chCount - 1;

        Label lblAddr = new Label(start + "–" + end);
        lblAddr.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 11.5));
        lblAddr.setTextFill(Color.web("#F59E0B"));

        // Trennlinie 2
        Line sep2 = createSeparator();

        // Kanalanzahl
        Label lblChannels = new Label(I18n.get("fixture.channels", chCount));
        lblChannels.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11));
        lblChannels.setTextFill(Color.web("#A1A1AA"));

        row.getChildren().addAll(lblName, sep1, lblAddr, sep2, lblChannels);

        row.setOnMouseClicked(e -> {
            if (onJumpToChannel != null) {
                onJumpToChannel.accept(start);
            }
        });

        return row;
    }

    private String formatDisplayName(FixtureInstance inst) {
        FixtureDefinition def = inst.getDefinition();
        String brand = def != null ? def.getManufacturer() : "";
        String model = def != null ? def.getModel() : "";
        String name = inst.getName();

        if (brand != null && !brand.isBlank()) {
            if (model != null && !model.isBlank() && !model.equalsIgnoreCase(brand)) {
                return brand + " " + model;
            }
            if (name != null && !name.toLowerCase().contains(brand.toLowerCase())) {
                return brand + " " + name;
            }
        }
        return (name != null && !name.isBlank()) ? name : "Fixture";
    }

    private Line createSeparator() {
        Line line = new Line(0, 0, 0, 14);
        line.setStyle("-fx-stroke: #3F3F46; -fx-stroke-width: 1;");
        return line;
    }
}

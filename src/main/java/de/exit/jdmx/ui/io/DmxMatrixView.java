package de.exit.jdmx.ui.io;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 512-Kanal Universe Live-Monitor (DMX-Viewer).
 * <p>
 * Zeigt alle 512 DMX-Kanäle in einer responsiven Viereck-Matrix an.
 * Bietet Universums-Umschaltung (1–4), Filterung (Alle vs. Nur Aktive)
 * und Echtzeit-Aktualisierung der Kanal-Auslastung.
 * </p>
 */
public class DmxMatrixView extends VBox {

    private final List<DmxChannelCell> channelCells = new ArrayList<>(512);
    private final FlowPane gridPane;
    private final ScrollPane scrollPane;

    private int selectedUniverse = 1;
    private boolean onlyActiveFilter = false;

    private final ToggleButton btnFilterAll;
    private final ToggleButton btnFilterActive;

    public DmxMatrixView() {
        getStyleClass().add("dmx-matrix-view");
        setSpacing(8.0);
        setPadding(new Insets(10.0));
        VBox.setVgrow(this, Priority.ALWAYS);

        // 1. Toolbar: Universen-Tabs & Filter
        HBox toolbar = buildToolbar();

        // 2. 512-Kanal Matrix aus Vierecken
        this.gridPane = new FlowPane();
        gridPane.getStyleClass().add("dmx-matrix-grid");
        gridPane.setHgap(3.0);
        gridPane.setVgap(3.0);
        gridPane.setAlignment(Pos.TOP_LEFT);
        gridPane.setPadding(new Insets(6.0));

        for (int i = 1; i <= 512; i++) {
            DmxChannelCell cell = new DmxChannelCell(i);
            channelCells.add(cell);
            gridPane.getChildren().add(cell);
        }

        this.scrollPane = new ScrollPane(gridPane);
        scrollPane.getStyleClass().add("dmx-matrix-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Filter Toggle Buttons initialisieren
        this.btnFilterAll = new ToggleButton("Alle (512)");
        this.btnFilterActive = new ToggleButton("Nur Aktive");

        getChildren().addAll(toolbar, scrollPane);
    }

    private HBox buildToolbar() {
        HBox bar = new HBox(10.0);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 0, 4, 0));

        Label lblTitle = new Label("DMX MATRIX:");
        lblTitle.getStyleClass().add("matrix-title-label");

        // Universums-Umschaltung (Tabs 1 bis 4)
        ToggleGroup uniGroup = new ToggleGroup();
        HBox uniBox = new HBox(4.0);
        uniBox.setAlignment(Pos.CENTER_LEFT);

        for (int u = 1; u <= 4; u++) {
            int universeIndex = u;
            ToggleButton btnUni = new ToggleButton("Universum " + u);
            btnUni.getStyleClass().add("matrix-universe-tab");
            btnUni.setToggleGroup(uniGroup);
            if (u == 1) {
                btnUni.setSelected(true);
            }
            btnUni.setOnAction(e -> selectUniverse(universeIndex));
            uniBox.getChildren().add(btnUni);
        }

        // Filter (Alle vs Nur Aktive)
        ToggleGroup filterGroup = new ToggleGroup();
        ToggleButton btnAll = new ToggleButton("Alle (512)");
        btnAll.getStyleClass().add("matrix-filter-btn");
        btnAll.setToggleGroup(filterGroup);
        btnAll.setSelected(true);
        btnAll.setOnAction(e -> setOnlyActiveFilter(false));

        ToggleButton btnActive = new ToggleButton("Nur Aktive");
        btnActive.getStyleClass().add("matrix-filter-btn");
        btnActive.setToggleGroup(filterGroup);
        btnActive.setOnAction(e -> setOnlyActiveFilter(true));

        HBox filterBox = new HBox(4.0, btnAll, btnActive);
        filterBox.setAlignment(Pos.CENTER_LEFT);

        // Rate Pill rechts
        Label lblRate = new Label("40 DMX/s");
        lblRate.getStyleClass().add("matrix-rate-pill");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(lblTitle, uniBox, new Label("|"), filterBox, spacer, lblRate);
        return bar;
    }

    /**
     * Schaltet das aktive DMX-Universum um (1–4).
     *
     * @param universe Universums-Index (1..4)
     */
    public void selectUniverse(int universe) {
        this.selectedUniverse = Math.max(1, Math.min(4, universe));
    }

    /**
     * Schaltet den Filter für aktive Kanäle (> 0) ein oder aus.
     *
     * @param onlyActive true = nur Kanäle mit Wert > 0 anzeigen
     */
    public void setOnlyActiveFilter(boolean onlyActive) {
        this.onlyActiveFilter = onlyActive;
        applyFilter();
    }

    private void applyFilter() {
        for (DmxChannelCell cell : channelCells) {
            boolean visible = !onlyActiveFilter || cell.isActive();
            cell.setVisible(visible);
            cell.setManaged(visible);
        }
    }

    /**
     * Aktualisiert den Wert eines einzelnen Kanals.
     *
     * @param channel Kanalnummer (1..512)
     * @param value   Neuer DMX-Wert (0..255)
     */
    public void setChannelValue(int channel, int value) {
        if (channel >= 1 && channel <= 512) {
            DmxChannelCell cell = channelCells.get(channel - 1);
            cell.setValue(value);
            if (onlyActiveFilter) {
                cell.setVisible(cell.isActive());
                cell.setManaged(cell.isActive());
            }
        }
    }

    /**
     * Setzt alle 512 Kanäle anhand eines DMX-Frame-Byte-Arrays.
     *
     * @param channels Byte-Array (mindestens 512 Bytes)
     */
    public void setChannels(byte[] channels) {
        if (channels == null) {
            return;
        }
        int len = Math.min(channels.length, 512);
        for (int i = 0; i < len; i++) {
            int val = channels[i] & 0xFF;
            DmxChannelCell cell = channelCells.get(i);
            cell.setValue(val);
        }
        if (onlyActiveFilter) {
            applyFilter();
        }
    }

    public DmxChannelCell getCell(int channel) {
        if (channel >= 1 && channel <= 512) {
            return channelCells.get(channel - 1);
        }
        return null;
    }

    public List<DmxChannelCell> getChannelCells() {
        return Collections.unmodifiableList(channelCells);
    }

    public int getSelectedUniverse() {
        return selectedUniverse;
    }

    public boolean isOnlyActiveFilter() {
        return onlyActiveFilter;
    }
}

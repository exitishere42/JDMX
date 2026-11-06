package de.exit.jdmx.ui.io;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 512-Kanal Universe Live-Monitor (DMX-Viewer).
 * <p>
 * Zeigt alle 512 DMX-Kanäle im standardmäßigen Mischpult-Raster an:
 * Genau 16 Spalten pro Zeile (unterteilt in 4er-Faderbänke), 32 Zeilen insgesamt.
 * Jede Zeile verfügt über eine Kanalnummerierungs-Beschriftung links (z. B. 001:, 017: ... 497:).
 * Bietet Universums-Umschaltung (1–4), Filterung (Alle vs. Nur Aktive)
 * und Echtzeit-Aktualisierung der Kanal-Auslastung.
 * </p>
 */
public class DmxMatrixView extends VBox {

    public static final int COLUMNS_PER_ROW = 16;
    public static final int TOTAL_CHANNELS = 512;
    public static final int ROW_COUNT = TOTAL_CHANNELS / COLUMNS_PER_ROW; // 32

    private final List<DmxChannelCell> channelCells = new ArrayList<>(TOTAL_CHANNELS);
    private final List<HBox> rowContainers = new ArrayList<>(ROW_COUNT);
    private final VBox matrixRowsBox;
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

        // Filter Toggle Buttons initialisieren
        this.btnFilterAll = new ToggleButton("Alle (512)");
        this.btnFilterActive = new ToggleButton("Nur Aktive");

        // 1. Toolbar: Universen-Tabs & Filter
        HBox toolbar = buildToolbar();

        // 2. 512-Kanal Matrix im 16-Spalten-Raster (32 Zeilen à 4x4 Blöcke)
        this.matrixRowsBox = new VBox(4.0);
        matrixRowsBox.getStyleClass().add("dmx-matrix-rows-container");
        matrixRowsBox.setAlignment(Pos.TOP_CENTER);
        matrixRowsBox.setPadding(new Insets(6.0, 10.0, 10.0, 10.0));

        for (int r = 0; r < ROW_COUNT; r++) {
            int startChannel = (r * COLUMNS_PER_ROW) + 1;
            HBox rowBox = buildRow(startChannel);
            rowContainers.add(rowBox);
            matrixRowsBox.getChildren().add(rowBox);
        }

        this.scrollPane = new ScrollPane(matrixRowsBox);
        scrollPane.getStyleClass().add("dmx-matrix-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(toolbar, scrollPane);
    }

    private HBox buildRow(int startChannel) {
        HBox row = new HBox(6.0);
        row.getStyleClass().add("dmx-matrix-row");
        row.setAlignment(Pos.CENTER_LEFT);

        // Zeilenbeschriftung links (z. B. "001:", "017:")
        Label rowLabel = new Label(String.format("%03d:", startChannel));
        rowLabel.getStyleClass().add("matrix-row-label");
        rowLabel.setMinWidth(42.0);
        rowLabel.setPrefWidth(42.0);
        rowLabel.setAlignment(Pos.CENTER_RIGHT);
        row.getChildren().add(rowLabel);

        // 4 Faderbänke à 4 Kanäle
        for (int bank = 0; bank < 4; bank++) {
            HBox bankBox = new HBox(4.0);
            bankBox.setAlignment(Pos.CENTER_LEFT);
            bankBox.getStyleClass().add("dmx-matrix-bank");

            for (int col = 0; col < 4; col++) {
                int ch = startChannel + (bank * 4) + col;
                DmxChannelCell cell = new DmxChannelCell(ch);
                channelCells.add(cell);
                bankBox.getChildren().add(cell);
            }

            row.getChildren().add(bankBox);

            // Abstand zwischen den Bänken (außer nach der letzten Bank)
            if (bank < 3) {
                HBox spacer = new HBox();
                spacer.setMinWidth(10.0);
                spacer.setPrefWidth(10.0);
                row.getChildren().add(spacer);
            }
        }

        return row;
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
        btnFilterAll.getStyleClass().add("matrix-filter-btn");
        btnFilterAll.setToggleGroup(filterGroup);
        btnFilterAll.setSelected(true);
        btnFilterAll.setOnAction(e -> setOnlyActiveFilter(false));

        btnFilterActive.getStyleClass().add("matrix-filter-btn");
        btnFilterActive.setToggleGroup(filterGroup);
        btnFilterActive.setOnAction(e -> setOnlyActiveFilter(true));

        HBox filterBox = new HBox(4.0, btnFilterAll, btnFilterActive);
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
        for (int r = 0; r < ROW_COUNT; r++) {
            HBox row = rowContainers.get(r);
            boolean rowHasActive = false;
            int startIdx = r * COLUMNS_PER_ROW;

            for (int c = 0; c < COLUMNS_PER_ROW; c++) {
                DmxChannelCell cell = channelCells.get(startIdx + c);
                boolean visible = !onlyActiveFilter || cell.isActive();
                cell.setVisible(visible);
                cell.setManaged(visible);
                if (cell.isActive()) {
                    rowHasActive = true;
                }
            }

            if (onlyActiveFilter) {
                row.setVisible(rowHasActive);
                row.setManaged(rowHasActive);
            } else {
                row.setVisible(true);
                row.setManaged(true);
            }
        }
    }

    /**
     * Aktualisiert den Wert eines einzelnen Kanals.
     *
     * @param channel Kanalnummer (1..512)
     * @param value   Neuer DMX-Wert (0..255)
     */
    public void setChannelValue(int channel, int value) {
        if (channel >= 1 && channel <= TOTAL_CHANNELS) {
            DmxChannelCell cell = channelCells.get(channel - 1);
            cell.setValue(value);
            if (onlyActiveFilter) {
                applyFilter();
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
        int len = Math.min(channels.length, TOTAL_CHANNELS);
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
        if (channel >= 1 && channel <= TOTAL_CHANNELS) {
            return channelCells.get(channel - 1);
        }
        return null;
    }

    public List<DmxChannelCell> getChannelCells() {
        return Collections.unmodifiableList(channelCells);
    }

    public int getColumnsPerRow() {
        return COLUMNS_PER_ROW;
    }

    public int getRowCount() {
        return ROW_COUNT;
    }

    public int getSelectedUniverse() {
        return selectedUniverse;
    }

    public boolean isOnlyActiveFilter() {
        return onlyActiveFilter;
    }
}

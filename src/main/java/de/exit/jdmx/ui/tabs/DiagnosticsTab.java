package de.exit.jdmx.ui.tabs;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

/**
 * Tab 5: Input / Output & Diagnostics.
 * Art-Net UDP, USB-DMX, MIDI-Mapping, and DMX-Traffic bus monitor.
 */
public class DiagnosticsTab extends VBox {

    /**
     * Constructs the Diagnostics Tab.
     */
    public DiagnosticsTab() {
        setSpacing(12);
        getStyleClass().add("jdmx-bg");
        VBox.setVgrow(this, Priority.ALWAYS);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        // Art-Net Card
        VBox artNetCard = createCard("Art-Net DMX Output (UDP: 6454)", "Status: Aktiv (Broadcast 255.255.255.255)");
        // USB Card
        VBox usbCard = createCard("USB DMX Interface (jSerialComm)", "Status: Simulation / FTDI Bereit");
        // MIDI Card
        VBox midiCard = createCard("MIDI Controller Input", "Kein Controller verbunden (Auto-Detect)");
        // Traffic Card
        VBox trafficCard = createCard("DMX Bus Traffic Monitor", "FPS: 44.2 Hz | Frame-Zeit: 1.4 ms | Changed CH: 12/512");

        grid.add(artNetCard, 0, 0);
        grid.add(usbCard, 1, 0);
        grid.add(midiCard, 0, 1);
        grid.add(trafficCard, 1, 1);

        for (int i = 0; i < 2; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(50);
            col.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(col);
        }

        getChildren().add(grid);
    }

    /**
     * Creates a diagnostic card.
     *
     * @param titleText  The title text for the card.
     * @param statusText The status text for the card.
     * @return VBox representing the card.
     */
    private VBox createCard(String titleText, String statusText) {
        VBox card = new VBox(10);
        card.getStyleClass().add("diagnostics-card");
        card.setPrefHeight(180);

        Label title = new Label(titleText);
        title.getStyleClass().add("jdmx-section-title");

        Label status = new Label(statusText);
        status.getStyleClass().add("jdmx-status-text");

        card.getChildren().addAll(title, status);
        return card;
    }
}

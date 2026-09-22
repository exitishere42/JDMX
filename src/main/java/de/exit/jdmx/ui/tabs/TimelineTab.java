package de.exit.jdmx.ui.tabs;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

/**
 * Tab 4: Timeline & Music Sync.
 * Audio waveform visualization, timecode counter, and parallel DMX light tracks.
 */
public class TimelineTab extends VBox {

    /**
     * Constructs the Timeline Tab.
     */
    public TimelineTab() {
        setSpacing(12);
        getStyleClass().add("jdmx-bg");
        VBox.setVgrow(this, Priority.ALWAYS);

        // Transport Bar
        HBox transportBar = new HBox(12);
        transportBar.setAlignment(Pos.CENTER_LEFT);
        transportBar.getStyleClass().add("transport-bar");

        Button playBtn = new Button("PLAY");
        playBtn.getStyleClass().add("btn-transport-play");

        Button stopBtn = new Button("STOP");
        stopBtn.getStyleClass().add("btn-transport-stop");

        Label timecode = new Label("00:01:24.450 / 00:03:42.000");
        timecode.getStyleClass().add("timecode-display");

        transportBar.getChildren().addAll(playBtn, stopBtn, timecode);

        // Waveform Placeholder
        VBox waveformPanel = new VBox(8);
        VBox.setVgrow(waveformPanel, Priority.ALWAYS);
        waveformPanel.getStyleClass().add("jdmx-card");

        Label trackInfo = new Label("Audio Spur: Showtanz_Mix_2026.mp3 (128 BPM)");
        trackInfo.getStyleClass().add("jdmx-section-title");

        Pane waveBox = new Pane();
        VBox.setVgrow(waveBox, Priority.ALWAYS);
        waveBox.setStyle("-fx-background-color: -jdmx-bg; -fx-border-color: -jdmx-border; -fx-border-radius: 4px; -fx-background-radius: 4px;");

        waveformPanel.getChildren().addAll(trackInfo, waveBox);

        getChildren().addAll(transportBar, waveformPanel);
    }
}

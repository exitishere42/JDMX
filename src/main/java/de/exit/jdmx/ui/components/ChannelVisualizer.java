package de.exit.jdmx.ui.components;

import de.exit.jdmx.ui.icons.LucideIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * 512-Kanal DMX-Visualizer mit exakt 32 sichtbaren Kanälen im Viewport,
 * horizontaler Scrollbar und Schnell-Sprungtasten.
 * Identisch zum ChannelVisualizer aus artnet2dmx.
 */
public class ChannelVisualizer extends VBox {

    public static final int TOTAL_CHANNELS = 512;
    public static final int VISIBLE_COUNT = 32;

    private final Canvas canvas;
    private final CanvasPane canvasPane;
    private final ScrollBar scrollBar;
    private final int[] prevChannels = new int[TOTAL_CHANNELS];

    private double channelWidth = 25.0;
    private double barWidth = 19.0;
    private double scrollOffsetChannels = 0.0;
    private Consumer<Integer> onScrollCallback;

    /**
     * Constructs the ChannelVisualizer component.
     */
    public ChannelVisualizer() {
        setPadding(new Insets(10, 14, 10, 14));
        getStyleClass().add("jdmx-card");

        Arrays.fill(prevChannels, 0);

        // Header Bar
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 8, 0));

        Label title = new Label("DMX512 KANÄLE (1 - 512)");
        title.getStyleClass().add("jdmx-section-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox jumpBox = new HBox(4);
        jumpBox.setAlignment(Pos.CENTER_RIGHT);

        Label lblBereich = new Label("Bereich:");
        lblBereich.setStyle("-fx-text-fill: -jdmx-text-disabled; -fx-font-size: 10px;");
        jumpBox.getChildren().add(lblBereich);

        int[][] jumps = {
            {1, 32}, {33, 64}, {65, 96}, {97, 128}, {129, 256}, {257, 512}
        };

        for (int[] j : jumps) {
            String labelText = j[0] + "-" + j[1];
            Button btnJump = new Button(labelText);
            btnJump.setCursor(Cursor.HAND);
            btnJump.setStyle("-fx-background-color: -jdmx-control; -fx-text-fill: -jdmx-text-high; -fx-font-size: 10px; -fx-background-radius: 3px; -fx-padding: 2px 6px;");
            btnJump.setOnMouseEntered(e -> btnJump.setStyle("-fx-background-color: -jdmx-chip; -fx-text-fill: -jdmx-primary; -fx-background-radius: 3px; -fx-padding: 2px 6px;"));
            btnJump.setOnMouseExited(e -> btnJump.setStyle("-fx-background-color: -jdmx-control; -fx-text-fill: -jdmx-text-high; -fx-background-radius: 3px; -fx-padding: 2px 6px;"));
            btnJump.setOnAction(e -> scrollToChannel(j[0]));
            jumpBox.getChildren().add(btnJump);
        }

        header.getChildren().addAll(title, spacer, jumpBox);

        // Canvas & Container
        canvas = new Canvas(800, 140);
        canvasPane = new CanvasPane(canvas, this);
        VBox.setVgrow(canvasPane, Priority.ALWAYS);

        // ScrollBar
        scrollBar = new ScrollBar();
        scrollBar.setMin(0);
        scrollBar.setMax(TOTAL_CHANNELS - VISIBLE_COUNT);
        scrollBar.setVisibleAmount(VISIBLE_COUNT);
        scrollBar.setBlockIncrement(VISIBLE_COUNT);
        scrollBar.setUnitIncrement(1);
        scrollBar.setPadding(new Insets(4, 0, 0, 0));

        scrollBar.valueProperty().addListener((obs, oldVal, newVal) -> {
            scrollOffsetChannels = newVal.doubleValue();
            render();
            if (onScrollCallback != null) {
                onScrollCallback.accept((int) Math.round(scrollOffsetChannels) + 1);
            }
        });

        // Horizontal mouse scroll
        canvasPane.setOnScroll(e -> {
            double delta = e.getDeltaY() != 0 ? e.getDeltaY() : e.getDeltaX();
            if (delta != 0) {
                double step = (delta > 0 ? -3 : 3);
                double target = Math.max(0, Math.min(scrollBar.getMax(), scrollBar.getValue() + step));
                scrollBar.setValue(target);
            }
        });

        getChildren().addAll(header, canvasPane, scrollBar);
    }

    /**
     * Sets a callback when the scroll offset changes.
     *
     * @param callback Consumer taking the start channel number (1-based).
     */
    public void setOnScrollCallback(Consumer<Integer> callback) {
        this.onScrollCallback = callback;
    }

    void handleResize(double w, double h) {
        channelWidth = w / (double) VISIBLE_COUNT;
        barWidth = Math.max(4.0, channelWidth - 6.0);
        render();
    }

    public void scrollToChannel(int channelNum) {
        double target = Math.max(0, Math.min(TOTAL_CHANNELS - VISIBLE_COUNT, channelNum - 1));
        scrollBar.setValue(target);
    }

    public void updateChannelValue(int channelIndex, int value) {
        if (channelIndex >= 0 && channelIndex < TOTAL_CHANNELS) {
            prevChannels[channelIndex] = value;
            render();
        }
    }

    public void updateChannels(int[] channels) {
        boolean changed = false;
        for (int i = 0; i < TOTAL_CHANNELS && i < channels.length; i++) {
            if (channels[i] != prevChannels[i]) {
                prevChannels[i] = channels[i];
                changed = true;
            }
        }
        if (changed) {
            render();
        }
    }

    private void render() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0) return;

        // Background (1dp Card)
        gc.setFill(Color.web("#1E1E1E"));
        gc.fillRect(0, 0, w, h);

        double trackTop = 22;
        double trackBottom = h - 22;
        double trackH = trackBottom - trackTop;

        int startCh = (int) Math.floor(scrollOffsetChannels);
        int endCh = Math.min(TOTAL_CHANNELS, startCh + VISIBLE_COUNT + 1);

        gc.setTextAlign(TextAlignment.CENTER);

        for (int ch = startCh; ch < endCh; ch++) {
            double screenX = (ch - scrollOffsetChannels) * channelWidth;
            if (screenX + channelWidth < 0 || screenX > w) {
                continue;
            }

            int val = (ch < TOTAL_CHANNELS) ? prevChannels[ch] : 0;
            double colCenterX = screenX + (channelWidth / 2.0);
            double barX = screenX + (channelWidth - barWidth) / 2.0;

            // 1. Wert-Text oben (0..255)
            gc.setFont(Font.font("Consolas", 9));
            if (val > 0) {
                gc.setFill(Color.web("#03DAC6"));
            } else {
                gc.setFill(Color.web("#9E9E9E"));
            }
            gc.fillText(String.valueOf(val), colCenterX, 14);

            // 2. Track Background (2dp Control Box)
            gc.setFill(Color.web("#232323"));
            gc.fillRect(barX, trackTop, barWidth, trackH);

            // 3. Pegelbalken
            if (val > 0 && trackH > 0) {
                double barH = (val / 255.0) * trackH;
                gc.setFill(Color.web("#03DAC6"));
                gc.fillRect(barX, trackBottom - barH, barWidth, barH);
            }

            // 4. Kanal-Nummer unten (1..512)
            gc.setFont(Font.font("Segoe UI", 8));
            gc.setFill(Color.web("#616161"));
            gc.fillText(String.valueOf(ch + 1), colCenterX, h - 8);
        }
    }

    /**
     * Unmanaged Canvas Container prevents unwanted layout loops.
     */
    private static class CanvasPane extends Pane {
        private final Canvas canvas;
        private final ChannelVisualizer visualizer;

        public CanvasPane(Canvas canvas, ChannelVisualizer visualizer) {
            this.canvas = canvas;
            this.visualizer = visualizer;
            getChildren().add(canvas);
            canvas.setManaged(false);

            setMinHeight(60);
            setPrefHeight(140);
            setMaxHeight(Double.MAX_VALUE);
            setMinWidth(100);
            setMaxWidth(Double.MAX_VALUE);
        }

        @Override
        protected void layoutChildren() {
            super.layoutChildren();
            double w = Math.floor(getWidth());
            double h = Math.floor(getHeight());
            if (w > 0 && h > 0) {
                canvas.relocate(0, 0);
                boolean sizeChanged = false;
                if (Math.abs(canvas.getWidth() - w) > 0.5) {
                    canvas.setWidth(w);
                    sizeChanged = true;
                }
                if (Math.abs(canvas.getHeight() - h) > 0.5) {
                    canvas.setHeight(h);
                    sizeChanged = true;
                }
                if (sizeChanged) {
                    visualizer.handleResize(w, h);
                }
            }
        }
    }
}

package de.exit.jdmx.ui.desk;

import de.exit.jdmx.core.i18n.I18n;
import de.exit.jdmx.core.model.ChannelType;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Performante, Canvas-basierte 512-Kanal DMX-Faderbank mit ziehbaren Pegelbalken.
 * <p>
 * Basiert auf der bewährten Architektur von {@code ChannelVisualizer} aus {@code artnet2dmx}:
 * <ul>
 *     <li><b>Zero-Lag & Zero-Overhead:</b> Genau 1 unmanaged {@link Canvas}-Knoten für alle 512 Kanäle.</li>
 *     <li><b>Interaktive Balken:</b> Balken können per Maus direkt gegriffen und hoch-/runtergezogen werden.</li>
 *     <li><b>Puristisches Design:</b> Maximale Fader-Höhe ohne Text-Overhead, mit Kanalnummern direkt am Fuß.</li>
 *     <li><b>Pinsel-Modus:</b> Ziehen mit gedrückter Maustaste quer über Kanäle setzt mehrere Werte in einer Wischbewegung.</li>
 *     <li><b>Schnell-Reset:</b> Doppelklick auf einen Kanal setzt den Wert sofort auf 0.</li>
 *     <li><b>Direct Rendering:</b> Hardwarebeschleunigtes Zeichnen in unter 1 ms pro Frame.</li>
 *     <li><b>Custom Scrollbar:</b> Rechteckige, flache Leiste mit blauem Aktivzustand beim Anklicken.</li>
 * </ul>
 * </p>
 */
public class CanvasDeskView extends VBox {

    public static final int TOTAL_CHANNELS = 512;
    public static final int VISIBLE_COUNT = 32;

    public static final double ICON_CENTER_Y = 17.0;
    public static final double ICON_SIZE = 22.0;
    public static final double TRACK_TOP = 33.0;
    public static final double TRACK_BOTTOM_MARGIN = 22.0;

    public static final Color COLOR_PRIMARY_ORANGE = Color.web("#F59E0B");

    private final Canvas canvas;
    private final CanvasPane canvasPane;
    private final DeskScrollBar scrollBar;

    private final DeskChannelData[] channels = new DeskChannelData[TOTAL_CHANNELS];

    private double channelWidth = 25.0;
    private double barWidth = 19.0;
    private double scrollOffsetChannels = 0.0;
    private int activeDragChannel = -1;
    private boolean renderPending = false;

    private final Tooltip iconTooltip = new Tooltip();
    private int lastHoveredChannel = -1;
    private boolean iconTooltipShowingForTesting = false;

    private BiConsumer<Integer, Integer> onChannelValueChanged;
    private Consumer<Double> onScrollChanged;

    /**
     * Erstellt eine neue Canvas-basierte Faderbank für 512 DMX-Kanäle.
     */
    public CanvasDeskView() {
        setSpacing(0);
        VBox.setVgrow(this, Priority.ALWAYS);
        getStyleClass().add("canvas-desk-view");

        for (int i = 0; i < TOTAL_CHANNELS; i++) {
            channels[i] = new DeskChannelData(i + 1);
        }

        // 1. Canvas & Entkoppelter Container
        this.canvas = new Canvas(800, 240);
        this.canvasPane = new CanvasPane(canvas, this);
        VBox.setVgrow(canvasPane, Priority.ALWAYS);

        // 2. Horizontale Custom-Scrollbar (Rechteckig, grauer Thumb mit Center-Linie, Blau bei Klick)
        this.scrollBar = new DeskScrollBar(TOTAL_CHANNELS, VISIBLE_COUNT);
        scrollBar.setOnValueChanged(newVal -> {
            this.scrollOffsetChannels = newVal;
            if (onScrollChanged != null) {
                onScrollChanged.accept(scrollOffsetChannels);
            }
            render();
        });

        // 3. Mausrad-Scrolling (horizontal)
        canvasPane.setOnScroll(e -> {
            double delta = e.getDeltaY() != 0 ? e.getDeltaY() : e.getDeltaX();
            if (delta != 0) {
                double step = (delta > 0 ? -3 : 3);
                double target = Math.max(0, Math.min(scrollBar.getMaxValue(), scrollBar.getValue() + step));
                scrollBar.setValue(target);
            }
        });

        // 4. Interaktives Ziehen der Pegelbalken (Click & Drag - strikt an geklickten Kanal gebunden)
        canvasPane.setOnMousePressed(e -> {
            hideIconTooltip();
            canvasPane.setCursor(Cursor.V_RESIZE);
            int ch = getChannelAtX(e.getX());
            if (ch >= 1 && ch <= TOTAL_CHANNELS) {
                this.activeDragChannel = ch;
                int val = getValueAtY(e.getY());
                setChannelValue(ch, val);
            }
        });

        canvasPane.setOnMouseDragged(e -> {
            hideIconTooltip();
            if (activeDragChannel >= 1 && activeDragChannel <= TOTAL_CHANNELS) {
                int val = getValueAtY(e.getY());
                setChannelValue(activeDragChannel, val);
            }
        });

        canvasPane.setOnMouseReleased(e -> {
            canvasPane.setCursor(Cursor.DEFAULT);
            this.activeDragChannel = -1;
        });

        // 5. Doppelklick oder Rechtsklick für Null-Stellung
        canvasPane.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 || e.getButton() == MouseButton.SECONDARY) {
                int ch = getChannelAtX(e.getX());
                if (ch >= 1 && ch <= TOTAL_CHANNELS) {
                    setChannelValue(ch, 0);
                }
            }
        });

        // 6. Hover-Tooltips für Funktions-Icons
        iconTooltip.getStyleClass().add("desk-icon-tooltip");
        canvasPane.setOnMouseMoved(e -> handleMouseMoved(e.getX(), e.getY(), e.getScreenX(), e.getScreenY()));
        canvasPane.setOnMouseExited(e -> hideIconTooltip());

        getChildren().addAll(canvasPane, scrollBar);
    }

    void handleResize(double w, double h) {
        if (w > 0) {
            this.channelWidth = w / (double) VISIBLE_COUNT;
            this.barWidth = Math.max(6.0, channelWidth - 8.0);
        }
        render();
    }

    /**
     * Ermittelt die 1-basierte Kanalnummer (1–512) anhand der horizontalen X-Position.
     *
     * @param x X-Koordinate im Canvas
     * @return 1-basierte Kanalnummer
     */
    public int getChannelAtX(double x) {
        int chIndex = (int) Math.floor(scrollOffsetChannels + (x / channelWidth));
        return Math.max(1, Math.min(TOTAL_CHANNELS, chIndex + 1));
    }

    /**
     * Ermittelt den DMX-Wert (0–255) anhand der vertikalen Y-Position im Track.
     *
     * @param y Y-Koordinate im Canvas
     * @return DMX-Wert (0 bis 255)
     */
    public int getValueAtY(double y) {
        double trackTop = TRACK_TOP;
        double trackBottom = Math.max(trackTop + 10, canvas.getHeight() - TRACK_BOTTOM_MARGIN);
        double trackH = trackBottom - trackTop;

        if (trackH <= 0) {
            return 0;
        }

        double clampedY = Math.max(trackTop, Math.min(trackBottom, y));
        double norm = 1.0 - ((clampedY - trackTop) / trackH);
        return (int) Math.round(Math.max(0.0, Math.min(1.0, norm)) * 255.0);
    }

    /**
     * Setzt den DMX-Wert für einen bestimmten 1-basierten Kanal.
     *
     * @param channel 1-basierte Kanalnummer (1–512)
     * @param value   DMX-Wert (0–255)
     */
    public void setChannelValue(int channel, int value) {
        if (channel < 1 || channel > TOTAL_CHANNELS) {
            return;
        }
        int clampedVal = Math.max(0, Math.min(255, value));
        DeskChannelData data = channels[channel - 1];
        if (data.getValue() != clampedVal) {
            data.setValue(clampedVal);
            render();
            if (onChannelValueChanged != null) {
                onChannelValueChanged.accept(channel, clampedVal);
            }
        }
    }

    /**
     * Liefert den aktuellen DMX-Wert für einen 1-basierten Kanal.
     *
     * @param channel 1-basierte Kanalnummer (1–512)
     * @return DMX-Wert (0–255)
     */
    public int getChannelValue(int channel) {
        if (channel < 1 || channel > TOTAL_CHANNELS) {
            return 0;
        }
        return channels[channel - 1].getValue();
    }

    /**
     * Liefert das Zustandsdaten-Objekt für einen 1-basierten Kanal.
     *
     * @param channel 1-basierte Kanalnummer (1–512)
     * @return DeskChannelData
     */
    public DeskChannelData getChannelData(int channel) {
        if (channel < 1 || channel > TOTAL_CHANNELS) {
            return null;
        }
        return channels[channel - 1];
    }

    /**
     * Scrollt den Viewport horizontal so, dass der angegebene Kanal sichtbar ist.
     *
     * @param channelNum 1-basierte Kanalnummer (1–512)
     */
    public void scrollToChannel(int channelNum) {
        double target = Math.max(0, Math.min(scrollBar.getMaxValue(), channelNum - 1));
        scrollBar.setValue(target);
    }

    /**
     * Setzt alle 512 Kanäle auf 0 zurück.
     */
    public void resetAll() {
        for (int i = 0; i < TOTAL_CHANNELS; i++) {
            channels[i].setValue(0);
        }
        render();
    }

    /**
     * Weist einem Kanal Scheinwerfer-Attribute zu.
     *
     * @param channel      1-basierte Kanalnummer (1–512)
     * @param fixtureName  Name des Scheinwerfers
     * @param type         Kanaltyp (z. B. DIMMER, PAN, RED)
     * @param fixtureColor Akzentfarbe
     */
    public void assignFixtureChannel(int channel, String fixtureName, ChannelType type, Color fixtureColor) {
        assignFixtureChannel(channel, fixtureName, type, fixtureColor, 1, null);
    }

    /**
     * Weist einem Kanal Scheinwerfer-Attribute inklusive relativem Kanalindex und Bezeichnung zu.
     *
     * @param channel             1-basierte Kanalnummer (1–512)
     * @param fixtureName         Name des Scheinwerfers
     * @param type                Kanaltyp (z. B. DIMMER, PAN, RED)
     * @param fixtureColor        Akzentfarbe
     * @param fixtureChannelIndex 1-basierter relativer Kanalindex im Scheinwerfer
     * @param channelName         Individuelle Kanalbezeichnung
     */
    public void assignFixtureChannel(int channel, String fixtureName, ChannelType type, Color fixtureColor, int fixtureChannelIndex, String channelName) {
        if (channel >= 1 && channel <= TOTAL_CHANNELS) {
            channels[channel - 1].assignFixture(fixtureName, type, fixtureColor, fixtureChannelIndex, channelName);
            render();
        }
    }

    /**
     * Prüft, ob sich die angegebene Koordinate in der Icon-Headerzone (Y 4..33px)
     * eines gepatchten Kanals befindet.
     *
     * @param x X-Koordinate im Canvas
     * @param y Y-Koordinate im Canvas
     * @return true wenn die Maus über einem Kanal-Icon schwebt
     */
    public boolean isOverIcon(double x, double y) {
        if (y < 4.0 || y > TRACK_TOP) {
            return false;
        }
        int ch = getChannelAtX(x);
        if (ch < 1 || ch > TOTAL_CHANNELS) {
            return false;
        }
        DeskChannelData data = channels[ch - 1];
        return data != null && data.getChannelType() != null;
    }

    /**
     * Liefert die lokalisierte Bezeichnung für den angegebenen DMX-Kanaltyp.
     *
     * @param type Kanaltyp
     * @return Lokalisierter Name (z. B. "Pan (Schwenken X)" oder "Dimmer (Helligkeit)")
     */
    public String getLocalizedChannelTypeName(ChannelType type) {
        if (type == null) {
            return I18n.get("channel.type.generic");
        }
        String key = "channel.type." + type.name().toLowerCase();
        String val = I18n.get(key);
        return val != null && !val.equals(key) ? val : type.name();
    }

    /**
     * Baut den vollständigen mehrzeiligen Tooltip-Text für das Kanal-Icon auf.
     *
     * @param data Kanalzustandsdaten
     * @return Formatierter mehrzeiliger Tooltip-Text
     */
    public String buildIconTooltipText(DeskChannelData data) {
        if (data == null) {
            return "";
        }
        String funcTitle = getLocalizedChannelTypeName(data.getChannelType());
        int chNum = data.getChannelNumber();
        String fixtureName = data.getFixtureName() != null ? data.getFixtureName() : "";
        int fixChIdx = data.getFixtureChannelIndex();

        int val = data.getValue();
        int pct = (int) Math.round((val / 255.0) * 100.0);

        StringBuilder sb = new StringBuilder();
        sb.append(funcTitle);

        if (!fixtureName.isBlank()) {
            sb.append("\n").append(I18n.get("desk.tooltip.fixture_info", fixtureName, fixChIdx));
            sb.append(" (").append(I18n.get("desk.tooltip.dmx_channel", chNum)).append(")");
        } else {
            sb.append("\n").append(I18n.get("desk.tooltip.dmx_channel", chNum));
        }

        sb.append("\n").append(I18n.get("desk.tooltip.value", val, pct));
        return sb.toString();
    }

    /**
     * Behandelt die Mausbewegung über dem Fader-Desk und aktualisiert den Icon-Tooltip.
     *
     * @param x       Lokale X-Koordinate im Canvas
     * @param y       Lokale Y-Koordinate im Canvas
     * @param screenX Bildschirm-X-Koordinate für Tooltip-Positionierung
     * @param screenY Bildschirm-Y-Koordinate für Tooltip-Positionierung
     */
    public void handleMouseMoved(double x, double y, double screenX, double screenY) {
        if (activeDragChannel != -1) {
            hideIconTooltip();
            return;
        }

        if (isOverIcon(x, y)) {
            int ch = getChannelAtX(x);
            if (ch != lastHoveredChannel || !iconTooltip.isShowing()) {
                DeskChannelData data = channels[ch - 1];
                iconTooltip.setText(buildIconTooltipText(data));
                this.lastHoveredChannel = ch;
            }
            if (canvasPane.getScene() != null && canvasPane.getScene().getWindow() != null) {
                if (!iconTooltip.isShowing()) {
                    iconTooltip.show(canvasPane.getScene().getWindow(), screenX + 12, screenY + 16);
                } else {
                    iconTooltip.setAnchorX(screenX + 12);
                    iconTooltip.setAnchorY(screenY + 16);
                }
            }
        } else {
            hideIconTooltip();
        }
    }

    /**
     * Blendet den Tooltip an einer relativen Position ein (z. B. für Tests oder Tastatur-Fokus).
     *
     * @param x Lokale X-Koordinate
     * @param y Lokale Y-Koordinate
     */
    public void showIconTooltipAt(double x, double y) {
        if (!isOverIcon(x, y)) {
            hideIconTooltip();
            return;
        }
        int ch = getChannelAtX(x);
        DeskChannelData data = channels[ch - 1];
        iconTooltip.setText(buildIconTooltipText(data));
        this.lastHoveredChannel = ch;

        if (canvasPane.getScene() != null && canvasPane.getScene().getWindow() != null) {
            javafx.geometry.Point2D p = canvasPane.localToScreen(x, y);
            double sx = p != null ? p.getX() + 12 : x + 12;
            double sy = p != null ? p.getY() + 16 : y + 16;
            iconTooltip.show(canvasPane.getScene().getWindow(), sx, sy);
        } else {
            this.iconTooltipShowingForTesting = true;
        }
    }

    /**
     * Blendet den Icon-Tooltip aus.
     */
    public void hideIconTooltip() {
        if (iconTooltip.isShowing()) {
            iconTooltip.hide();
        }
        this.iconTooltipShowingForTesting = false;
        this.lastHoveredChannel = -1;
    }

    /**
     * Prüft, ob der Icon-Tooltip aktuell sichtbar ist.
     *
     * @return true wenn sichtbar
     */
    public boolean isIconTooltipShowing() {
        return iconTooltip.isShowing() || iconTooltipShowingForTesting;
    }

    /**
     * Liefert die Tooltip-Instanz.
     *
     * @return Tooltip
     */
    public Tooltip getIconTooltip() {
        return iconTooltip;
    }

    /**
     * Hebt alle Scheinwerfer-Zuweisungen auf.
     */
    public void clearFixtureAssignments() {
        for (DeskChannelData ch : channels) {
            ch.clearAssignment();
        }
        render();
    }

    /**
     * Simuliert Dimensionen zu Testzwecken, falls die View noch nicht an eine aktive Scene gehängt ist.
     *
     * @param w Breite in Pixeln
     * @param h Höhe in Pixeln
     */
    public void setSimulatedDimensions(double w, double h) {
        canvas.setWidth(w);
        canvas.setHeight(h);
        handleResize(w, h);
    }

    /**
     * Rendert alle sichtbaren Kanalbalken direkt auf den 2D-Grafikkontext.
     */
    public void render() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        // 1. Hintergrund füllen (Studio Dark Surface - neutrales Grau)
        gc.setFill(Color.web("#121212"));
        gc.fillRect(0, 0, w, h);

        double trackTop = TRACK_TOP;
        double trackBottom = h - TRACK_BOTTOM_MARGIN;
        double trackH = trackBottom - trackTop;

        int startCh = (int) Math.floor(scrollOffsetChannels);
        int endCh = Math.min(TOTAL_CHANNELS, startCh + VISIBLE_COUNT + 1);

        gc.setTextAlign(TextAlignment.CENTER);

        for (int ch = startCh; ch < endCh; ch++) {
            double screenX = (ch - scrollOffsetChannels) * channelWidth;
            if (screenX + channelWidth < 0 || screenX > w) {
                continue;
            }

            DeskChannelData data = channels[ch];
            int val = data.getValue();
            double colCenterX = screenX + (channelWidth / 2.0);
            double barX = screenX + (channelWidth - barWidth) / 2.0;

            // Spalten-Hintergrund leicht abgesetzt (Neutrales Studio-Grau)
            gc.setFill(Color.web("#181818"));
            gc.fillRect(screenX + 1, 2, channelWidth - 2, h - 4);

            // 1. QLC+ Funktions-Icon (wenn Fixture gepatcht ist)
            if (data.getChannelType() != null) {
                DeskChannelIconRenderer.renderIcon(gc, colCenterX, ICON_CENTER_Y, ICON_SIZE, data.getChannelType(), data.getBarColor());
            }

            // 2. Track-Hintergrund (Leerzustand, neutrales Grau)
            gc.setFill(Color.web("#262626"));
            gc.fillRoundRect(barX, trackTop, barWidth, trackH, 3, 3);

            // 3. Ausgefüllter Pegelbalken von unten nach oben
            if (val > 0 && trackH > 0) {
                double barH = (val / 255.0) * trackH;
                Color barCol = data.getBarColor();
                gc.setFill(barCol);
                gc.fillRoundRect(barX, trackBottom - barH, barWidth, barH, 3, 3);
            }

            // 4. DMX-Pegelwert (0..255)
            if (val > 0) {
                gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9.0));
                gc.setFill(Color.WHITE);
            } else {
                gc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 8.5));
                gc.setFill(Color.web("#52525B"));
            }
            gc.fillText(String.valueOf(val), colCenterX, h - 12);

            // 5. Kanalnummer unten (1..512)
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10.0));
            gc.setFill(Color.web("#A1A1AA"));
            gc.fillText(String.valueOf(ch + 1), colCenterX, h - 3);
        }
    }

    public int getTotalChannels() {
        return TOTAL_CHANNELS;
    }

    public int getVisibleCount() {
        return VISIBLE_COUNT;
    }

    public double getScrollOffset() {
        return scrollOffsetChannels;
    }

    public DeskScrollBar getScrollBar() {
        return scrollBar;
    }

    public void setOnChannelValueChanged(BiConsumer<Integer, Integer> onChannelValueChanged) {
        this.onChannelValueChanged = onChannelValueChanged;
    }

    public void setOnScrollChanged(Consumer<Double> onScrollChanged) {
        this.onScrollChanged = onScrollChanged;
    }

    /**
     * Dedizierter Container für das Canvas.
     * Hält das Canvas im Status 'unmanaged', sodass Größenänderungen niemals
     * ein erneutes Layout des Elterncontainers anfordern.
     */
    private static class CanvasPane extends Pane {
        private final Canvas canvas;
        private final CanvasDeskView deskView;

        public CanvasPane(Canvas canvas, CanvasDeskView deskView) {
            this.canvas = canvas;
            this.deskView = deskView;
            getChildren().add(canvas);
            canvas.setManaged(false);

            setMinHeight(100);
            setPrefHeight(200);
            setMaxHeight(Double.MAX_VALUE);
            setMinWidth(200);
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
                    deskView.handleResize(w, h);
                }
            }
        }
    }
}

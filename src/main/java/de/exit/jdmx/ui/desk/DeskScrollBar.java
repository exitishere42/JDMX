package de.exit.jdmx.ui.desk;

import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.util.function.Consumer;

/**
 * Längliche, rechteckige Custom-Scrollbar für die DMX-Faderbank im LightDesk.
 * <p>
 * Erfüllt exakt die gestalterischen Vorgaben des Pult-Designs:
 * <ul>
 *     <li><b>Rechteckig & länglich:</b> Dunkler, flacher Track mit 14 px Höhe ohne abgerundete Kanten.</li>
 *     <li><b>Grauer Fader-Thumb:</b> Rechteckiger Schieber in dezentem Pult-Grau ({@code #3A3A40}).</li>
 *     <li><b>Vertikale Center-Linie:</b> Crisp gezeichnete, hellgraue Mittellinie ({@code #D1D5DB}) als Griffmarkierung.</li>
 *     <li><b>Blau beim Anklicken:</b> Wechselt beim Anklicken oder Ziehen sofort in sattes Pult-Blau ({@code #2563EB}).</li>
 * </ul>
 * </p>
 */
public class DeskScrollBar extends Pane {

    public static final double BAR_HEIGHT = 14.0;
    public static final double MIN_THUMB_WIDTH = 48.0;

    public static final Color COLOR_TRACK_BG = Color.web("#161619");
    public static final Color COLOR_TRACK_BORDER = Color.web("#27272A");
    public static final Color COLOR_THUMB_NORMAL = Color.web("#3A3A40");
    public static final Color COLOR_THUMB_HOVER = Color.web("#4A4A52");
    public static final Color COLOR_THUMB_PRESSED = Color.web("#2563EB"); // Blau beim Anklicken!
    public static final Color COLOR_CENTER_LINE = Color.web("#D1D5DB");  // Hellgraue Mittellinie

    private final int totalChannels;
    private final int visibleChannels;
    private final Canvas canvas;

    private double value = 0.0; // 0 bis (totalChannels - visibleChannels)
    private boolean isPressed = false;
    private boolean isHovered = false;

    private double dragStartX = 0.0;
    private double dragStartThumbX = 0.0;

    private Consumer<Double> onValueChanged;

    /**
     * Erstellt eine neue DeskScrollBar für 512 Gesamtkanäle und 32 sichtbare Kanäle.
     */
    public DeskScrollBar() {
        this(512, 32);
    }

    /**
     * Erstellt eine neue DeskScrollBar mit individueller Kanalanzahl.
     *
     * @param totalChannels   Gesamtzahl der Kanäle (z. B. 512)
     * @param visibleChannels Anzahl sichtbarer Kanäle im Viewport (z. B. 32)
     */
    public DeskScrollBar(int totalChannels, int visibleChannels) {
        this.totalChannels = Math.max(1, totalChannels);
        this.visibleChannels = Math.min(totalChannels, Math.max(1, visibleChannels));

        setMinHeight(BAR_HEIGHT);
        setPrefHeight(BAR_HEIGHT);
        setMaxHeight(BAR_HEIGHT);

        this.canvas = new Canvas(800, BAR_HEIGHT);
        canvas.setManaged(false);
        getChildren().add(canvas);

        setCursor(Cursor.HAND);

        // Maus-Events verdrahten
        setOnMousePressed(e -> handlePress(e.getX()));
        setOnMouseDragged(e -> handleDrag(e.getX()));
        setOnMouseReleased(e -> handleRelease());

        setOnMouseMoved(e -> {
            boolean overThumb = isOverThumb(e.getX());
            if (overThumb != isHovered) {
                isHovered = overThumb;
                render();
            }
        });

        setOnMouseExited(e -> {
            if (isHovered) {
                isHovered = false;
                render();
            }
        });
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        double w = Math.floor(getWidth());
        double h = BAR_HEIGHT;
        if (w > 0) {
            canvas.relocate(0, 0);
            if (Math.abs(canvas.getWidth() - w) > 0.5) {
                canvas.setWidth(w);
                render();
            }
            if (Math.abs(canvas.getHeight() - h) > 0.5) {
                canvas.setHeight(h);
                render();
            }
        }
    }

    /**
     * Behandelt das Drücken der Maustaste (Klick auf Thumb oder Track).
     *
     * @param x Klick-X-Position
     */
    public void handlePress(double x) {
        this.isPressed = true;
        double trackW = getEffectiveWidth();
        double thumbW = getThumbWidth(trackW);
        double thumbX = getThumbX(trackW, thumbW);

        if (x >= thumbX && x <= thumbX + thumbW) {
            this.dragStartX = x;
            this.dragStartThumbX = thumbX;
        } else {
            // Sprung an die Klick-Position
            double targetThumbX = Math.max(0, Math.min(trackW - thumbW, x - (thumbW / 2.0)));
            updateValueFromThumbX(targetThumbX, trackW, thumbW);
            this.dragStartX = x;
            this.dragStartThumbX = targetThumbX;
        }
        render();
    }

    /**
     * Behandelt das Ziehen der Maustaste.
     *
     * @param x Aktuelle Maus-X-Position
     */
    public void handleDrag(double x) {
        if (!isPressed) {
            return;
        }
        double trackW = getEffectiveWidth();
        double thumbW = getThumbWidth(trackW);
        double deltaX = x - dragStartX;
        double newThumbX = Math.max(0, Math.min(trackW - thumbW, dragStartThumbX + deltaX));
        updateValueFromThumbX(newThumbX, trackW, thumbW);
    }

    /**
     * Behandelt das Loslassen der Maustaste.
     */
    public void handleRelease() {
        this.isPressed = false;
        render();
    }

    private void updateValueFromThumbX(double thumbX, double trackW, double thumbW) {
        double maxScroll = trackW - thumbW;
        if (maxScroll <= 0) {
            setValue(0.0);
            return;
        }
        double fraction = thumbX / maxScroll;
        setValue(fraction * getMaxValue());
    }

    private boolean isOverThumb(double x) {
        double trackW = getEffectiveWidth();
        double thumbW = getThumbWidth(trackW);
        double thumbX = getThumbX(trackW, thumbW);
        return x >= thumbX && x <= thumbX + thumbW;
    }

    /**
     * Setzt den aktuellen Scroll-Wert (0 bis getMaxValue()).
     *
     * @param newVal Neuer Scrollwert in Kanälen
     */
    public void setValue(double newVal) {
        double clamped = Math.max(0.0, Math.min(getMaxValue(), newVal));
        if (Math.abs(this.value - clamped) > 0.001) {
            this.value = clamped;
            render();
            if (onValueChanged != null) {
                onValueChanged.accept(this.value);
            }
        }
    }

    /**
     * Liefert den aktuellen Scrollwert in Kanälen.
     *
     * @return Aktueller Offset
     */
    public double getValue() {
        return value;
    }

    /**
     * Liefert den maximalen Offset-Wert (totalChannels - visibleChannels).
     *
     * @return Maximaler Wert
     */
    public double getMaxValue() {
        return Math.max(0, totalChannels - visibleChannels);
    }

    public int getTotalChannels() {
        return totalChannels;
    }

    public int getVisibleChannels() {
        return visibleChannels;
    }

    public boolean isPressedState() {
        return isPressed;
    }

    public Color getCurrentThumbColor() {
        if (isPressed) {
            return COLOR_THUMB_PRESSED;
        }
        if (isHovered) {
            return COLOR_THUMB_HOVER;
        }
        return COLOR_THUMB_NORMAL;
    }

    private double getEffectiveWidth() {
        return Math.max(100.0, canvas.getWidth());
    }

    private double getThumbWidth(double trackW) {
        double proportion = (double) visibleChannels / totalChannels;
        return Math.max(MIN_THUMB_WIDTH, trackW * proportion);
    }

    private double getThumbX(double trackW, double thumbW) {
        double maxScroll = trackW - thumbW;
        if (maxScroll <= 0 || getMaxValue() <= 0) {
            return 0.0;
        }
        return (value / getMaxValue()) * maxScroll;
    }

    /**
     * Rendert die rechteckige Custom-Scrollbar mit vertikaler Center-Linie.
     */
    public void render() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        // 1. Rechteckiger Track
        gc.setFill(COLOR_TRACK_BG);
        gc.fillRect(0, 0, w, h);

        gc.setStroke(COLOR_TRACK_BORDER);
        gc.setLineWidth(1.0);
        gc.strokeLine(0, 0, w, 0);

        // 2. Rechteckiger Thumb (Grau, bei Klick Blau)
        double thumbW = getThumbWidth(w);
        double thumbX = getThumbX(w, thumbW);

        gc.setFill(getCurrentThumbColor());
        gc.fillRect(thumbX, 1, thumbW, h - 2);

        // 3. Vertikale hellgraue Center-Linie in der Mitte
        double midX = Math.round(thumbX + (thumbW / 2.0)) + 0.5;
        gc.setStroke(isPressed ? Color.WHITE : COLOR_CENTER_LINE);
        gc.setLineWidth(1.5);
        gc.strokeLine(midX, 2.5, midX, h - 2.5);
    }

    /**
     * Setzt simulierte Dimensionen zu Testzwecken.
     *
     * @param w Breite in Pixeln
     * @param h Höhe in Pixeln
     */
    public void setSimulatedDimensions(double w, double h) {
        canvas.setWidth(w);
        canvas.setHeight(h);
        render();
    }

    public void setOnValueChanged(Consumer<Double> onValueChanged) {
        this.onValueChanged = onValueChanged;
    }
}

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
 * Exakt nach dem Vorbild des Simple-Desk-Drag-Parts (1:1 Pixel- und Farb-Referenz):
 * <ul>
 *     <li><b>18 px flacher Track:</b> Dunkelgrauer Track ({@code #333338}) mit feinen 1px Begrenzungslinien.</li>
 *     <li><b>Rechteckiger Drag-Thumb:</b> Scharfkantiger, satter Mittelgrau-Schieber ({@code #555558}), der die gesamte Höhe füllt.</li>
 *     <li><b>Vertikaler Center-Grip:</b> Exakt zentrierte, 2.5 px breite Grip-Linie ({@code #727276}) mit dezentem Randabstand.</li>
 *     <li><b>Aktiv-Zustand:</b> Wechselt bei Klick & Drag in leuchtendes Pult-Blau ({@code #2563EB}) mit weißer Grip-Linie.</li>
 * </ul>
 * </p>
 */
public class DeskScrollBar extends Pane {

    public static final double BAR_HEIGHT = 18.0;
    public static final double MIN_THUMB_WIDTH = 48.0;

    public static final Color COLOR_TRACK_BG = Color.web("#333338");
    public static final Color COLOR_TRACK_BORDER = Color.web("#222225");
    public static final Color COLOR_THUMB_NORMAL = Color.web("#555558");
    public static final Color COLOR_THUMB_HOVER = Color.web("#636368");
    public static final Color COLOR_THUMB_PRESSED = Color.web("#2563EB"); // Blau beim Anklicken!
    public static final Color COLOR_CENTER_LINE = Color.web("#727276");  // Grip-Linie normal
    public static final Color COLOR_CENTER_LINE_PRESSED = Color.WHITE;    // Grip-Linie aktiv

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
        gc.strokeLine(0, 0.5, w, 0.5);
        gc.strokeLine(0, h - 0.5, w, h - 0.5);

        // 2. Rechteckiger Thumb (Schieber füllt die gesamte Höhe nahtlos aus)
        double thumbW = getThumbWidth(w);
        double thumbX = getThumbX(w, thumbW);

        gc.setFill(getCurrentThumbColor());
        gc.fillRect(thumbX, 0, thumbW, h);

        // 3. Vertikale Center-Grip-Linie (2.5 px breit, 3 px Randabstand)
        double lineW = 2.5;
        double lineH = Math.max(4.0, h - 6.0);
        double lineX = Math.round(thumbX + (thumbW - lineW) / 2.0);
        double lineY = 3.0;

        gc.setFill(isPressed ? COLOR_CENTER_LINE_PRESSED : COLOR_CENTER_LINE);
        gc.fillRect(lineX, lineY, lineW, lineH);
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

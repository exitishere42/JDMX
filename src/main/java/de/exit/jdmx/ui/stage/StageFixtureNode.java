package de.exit.jdmx.ui.stage;

import de.exit.jdmx.core.model.ChannelType;
import de.exit.jdmx.core.model.ExplorerItem;
import de.exit.jdmx.core.model.FixtureInstance;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.Objects;

/**
 * 2D-Bühnen-Repräsentation eines Scheinwerfers (Fixture) auf der Arbeitsfläche.
 * <p>
 * Visuelle Eigenschaften:
 * <ul>
 *     <li><b>Gehäuse:</b> 36x36px Quadrat mit exakt 2px Eckenrundung und Hardware-Rahmen</li>
 *     <li><b>Linse:</b> 34px Kreis, zentriert und bündig an allen 4 Innenkanten</li>
 *     <li><b>Farbe & Dimmer:</b> Helligkeit (0–100%) und RGB-Farbe. Reine Dimmer leuchten im warmen Halogen-Ton (3200K)</li>
 *     <li><b>Strobe:</b> Optisches Pulsieren zwischen Lichtfarbe und Dunkel</li>
 *     <li><b>Positions-Punkt:</b> Nur bei Moving Lights (Pan/Tilt) aktiv mit dynamischem Schwarz/Weiß-Kontrast</li>
 *     <li><b>Klick-basierter Namens-Badge:</b> Im Ruhezustand unsichtbar für eine blendfreie Bühne, erscheint nur bei Selektion mit eigenem Rahmen</li>
 * </ul>
 * </p>
 */
public class StageFixtureNode extends Pane {

    /** Gehäuse-Größe in Pixeln. */
    public static final double SQUARE_SIZE = 36.0;

    /** Radius der Innenlinse in Pixeln (34px Durchmesser). */
    public static final double LENS_RADIUS = 17.0;

    /** Radius des Positions-Tracker-Punkts in Pixeln (5px Durchmesser). */
    public static final double TRACKER_DOT_RADIUS = 2.5;

    /** Maximaler Bewegungsradius des Tracker-Punkts im Linsenkreis. */
    public static final double MAX_TRACKER_OFFSET = 13.5;

    /** Standard-Halogen-Warmweiß (3200K Moccasin) für reine Dimmer ohne Farbmischung. */
    public static final Color COLOR_WARM_WHITE = Color.web("#FFE4B5");

    /** Dunkler Ruhezustand bei geschlossenem Dimmer / Shutter. */
    public static final Color COLOR_DARK_OFF = Color.web("#141414");

    private final ExplorerItem item;
    private final FixtureInstance fixtureInstance;

    private final StackPane square;
    private final Circle lens;
    private final Circle trackerDot;
    private final Label nameBadge;

    private final boolean panTiltSupported;
    private final boolean colorChannelsSupported;
    private final boolean strobeSupported;

    private Color baseColor = Color.WHITE;
    private double dimmer = 0.0;
    private boolean strobing = false;
    private boolean strobePhase = false;

    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    private boolean hoveredState = false;

    private double dragStartX;
    private double dragStartY;
    private boolean hasDragged = false;
    private Runnable onDragFinished;
    private java.util.function.BiConsumer<StageFixtureNode, javafx.scene.input.MouseEvent> onFixtureClicked;

    /**
     * Erstellt einen neuen 2D-Bühnen-Scheinwerferknoten.
     *
     * @param item     Das zugrundeliegende ExplorerItem
     * @param initialX Horizontale Startposition auf der Bühne
     * @param initialY Vertikale Startposition auf der Bühne
     */
    public StageFixtureNode(ExplorerItem item, double initialX, double initialY) {
        this.item = Objects.requireNonNull(item, "item must not be null");
        this.fixtureInstance = item.getFixtureInstance();

        setLayoutX(initialX);
        setLayoutY(initialY);
        setPrefSize(SQUARE_SIZE, SQUARE_SIZE);
        setPickOnBounds(false);
        getStyleClass().add("stage-fixture-node");

        // 1. Capability-Erkennung
        this.panTiltSupported = detectPanTiltSupport();
        this.colorChannelsSupported = detectColorSupport();
        this.strobeSupported = detectStrobeSupport();

        if (!colorChannelsSupported) {
            this.baseColor = COLOR_WARM_WHITE;
        }

        // 2. Gehäuse (36x36px Quadrat mit 2px Eckenrundung)
        this.square = new StackPane();
        square.setPrefSize(SQUARE_SIZE, SQUARE_SIZE);
        square.setMinSize(SQUARE_SIZE, SQUARE_SIZE);
        square.setMaxSize(SQUARE_SIZE, SQUARE_SIZE);
        square.setLayoutX(0);
        square.setLayoutY(0);
        square.getStyleClass().add("stage-fixture-square");

        // 3. Linse (34px Kreis bündig zentriert)
        this.lens = new Circle(LENS_RADIUS);
        lens.getStyleClass().add("stage-fixture-lens");
        lens.setMouseTransparent(true);

        // 4. Tracker-Punkt (Pan/Tilt)
        this.trackerDot = new Circle(TRACKER_DOT_RADIUS);
        trackerDot.getStyleClass().add("stage-fixture-tracker-dot");
        trackerDot.setVisible(panTiltSupported);
        trackerDot.setMouseTransparent(true);

        square.getChildren().addAll(lens, trackerDot);
        StackPane.setAlignment(lens, Pos.CENTER);
        StackPane.setAlignment(trackerDot, Pos.CENTER);

        // 5. Namens-Badge mit Border (erscheint bei Hover oder dauerhaft bei Selektion)
        String badgeText = buildBadgeText();
        this.nameBadge = new Label(badgeText);
        nameBadge.getStyleClass().add("stage-fixture-badge");
        nameBadge.setLayoutY(SQUARE_SIZE + 3.0);
        nameBadge.setVisible(false);
        nameBadge.setMouseTransparent(true);
        nameBadge.setManaged(true);

        // Zentrierung des Badges unter dem 36px Gehäuse
        nameBadge.layoutBoundsProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.getWidth() > 0) {
                nameBadge.setLayoutX((SQUARE_SIZE - newVal.getWidth()) / 2.0);
            }
        });

        getChildren().addAll(square, nameBadge);

        // 6. Selektions-Listener
        selected.addListener((obs, oldVal, isSel) -> updateSelectionVisuals(isSel));

        // 7. Maus-Interaktionen (Klick zur Selektion, Drag zum Verschieben)
        setupInteractions();

        // 8. Initiale Farben und Kontraste berechnen
        updateLensColor();
    }

    private boolean detectPanTiltSupport() {
        if (fixtureInstance != null && fixtureInstance.getActiveMode() != null) {
            return fixtureInstance.getActiveMode().getChannels().stream()
                    .anyMatch(c -> c.type() == ChannelType.PAN || c.type() == ChannelType.TILT);
        }
        String name = item.getName().toLowerCase();
        return name.contains("spot") || name.contains("moving") || name.contains("mh") || "box".equals(item.getIconName());
    }

    private boolean detectColorSupport() {
        if (fixtureInstance != null && fixtureInstance.getActiveMode() != null) {
            return fixtureInstance.getActiveMode().getChannels().stream()
                    .anyMatch(c -> c.type() == ChannelType.RED || c.type() == ChannelType.GREEN
                            || c.type() == ChannelType.BLUE || c.type() == ChannelType.COLOR_WHEEL);
        }
        String name = item.getName().toLowerCase();
        return !name.contains("dimmer") && !"sun".equals(item.getIconName());
    }

    private boolean detectStrobeSupport() {
        if (fixtureInstance != null && fixtureInstance.getActiveMode() != null) {
            return fixtureInstance.getActiveMode().getChannels().stream()
                    .anyMatch(c -> c.type() == ChannelType.SHUTTER || c.name().toLowerCase().contains("strobe"));
        }
        return true;
    }

    private String buildBadgeText() {
        if (fixtureInstance != null) {
            return item.getName() + " [" + fixtureInstance.getUniverse() + "." + String.format("%03d", fixtureInstance.getStartAddress()) + "]";
        }
        return item.getName();
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (nameBadge.getWidth() > 0) {
            nameBadge.setLayoutX((SQUARE_SIZE - nameBadge.getWidth()) / 2.0);
        }
    }

    private void setupInteractions() {
        // Drag- und Klick-Interaktionen direkt auf dem 36x36px Gehäuse
        square.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                Point2D parentPoint = getParent() != null
                        ? getParent().sceneToLocal(e.getSceneX(), e.getSceneY())
                        : new Point2D(e.getSceneX(), e.getSceneY());
                dragStartX = parentPoint.getX() - getLayoutX();
                dragStartY = parentPoint.getY() - getLayoutY();
                hasDragged = false;
                toFront();
                e.consume();
            }
        });

        square.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                hasDragged = true;
                Point2D parentPoint = getParent() != null
                        ? getParent().sceneToLocal(e.getSceneX(), e.getSceneY())
                        : new Point2D(e.getSceneX(), e.getSceneY());
                double newX = Math.max(0, parentPoint.getX() - dragStartX);
                double newY = Math.max(0, parentPoint.getY() - dragStartY);
                setLayoutX(newX);
                setLayoutY(newY);

                if (fixtureInstance != null) {
                    fixtureInstance.setStageX(newX);
                    fixtureInstance.setStageY(newY);
                }
                e.consume();
            }
        });

        square.setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.PRIMARY && hasDragged) {
                hasDragged = false;
                if (onDragFinished != null) {
                    onDragFinished.run();
                }
            }
        });

        square.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && !hasDragged) {
                if (onFixtureClicked != null) {
                    onFixtureClicked.accept(this, e);
                } else {
                    setSelected(!isSelected());
                }
                e.consume();
            }
        });

        // Hover-Events auf dem 36x36px Gehäuse
        square.setOnMouseEntered(e -> {
            hoveredState = true;
            updateBadgeVisibility();
        });

        square.setOnMouseExited(e -> {
            hoveredState = false;
            updateBadgeVisibility();
        });

        // Event-Weiterleitung für synthetische Events auf dem Node selbst (z. B. in Unit-Tests)
        setOnMouseEntered(e -> {
            hoveredState = true;
            updateBadgeVisibility();
        });

        setOnMouseExited(e -> {
            hoveredState = false;
            updateBadgeVisibility();
        });
    }

    private void updateBadgeVisibility() {
        boolean show = isSelected() || hoveredState;
        nameBadge.setVisible(show);
        if (show) {
            nameBadge.toFront();
            if (nameBadge.getWidth() > 0) {
                nameBadge.setLayoutX((SQUARE_SIZE - nameBadge.getWidth()) / 2.0);
            }
        }
    }

    private void updateSelectionVisuals(boolean isSel) {
        if (isSel) {
            square.getStyleClass().setAll("stage-fixture-square-selected");
        } else {
            square.getStyleClass().setAll("stage-fixture-square");
        }
        updateBadgeVisibility();
    }

    /**
     * Setzt die Position des Tracker-Punkts (Pan/Tilt) im Bereich 0.0 bis 1.0.
     *
     * @param pan  Horizontale Pan-Position (0.5 = Mitte)
     * @param tilt Vertikale Tilt-Position (0.5 = Mitte)
     */
    public void setPosition(double pan, double tilt) {
        if (!panTiltSupported) {
            return;
        }

        double dx = (pan - 0.5) * 2.0 * MAX_TRACKER_OFFSET;
        double dy = (tilt - 0.5) * 2.0 * MAX_TRACKER_OFFSET;

        // Auf Kreisradius begrenzen
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist > MAX_TRACKER_OFFSET) {
            double scale = MAX_TRACKER_OFFSET / dist;
            dx *= scale;
            dy *= scale;
        }

        trackerDot.setTranslateX(dx);
        trackerDot.setTranslateY(dy);
    }

    /**
     * Setzt die Basisfarbe des Scheinwerfers.
     *
     * @param color Gewünschte Lichtfarbe
     */
    public void setColor(Color color) {
        this.baseColor = Objects.requireNonNull(color, "color must not be null");
        updateLensColor();
    }

    /**
     * Setzt die Helligkeit (Dimmer) im Bereich 0.0 bis 1.0.
     *
     * @param dimmer Helligkeitswert (0.0 = dunkel, 1.0 = voll)
     */
    public void setDimmer(double dimmer) {
        this.dimmer = Math.clamp(dimmer, 0.0, 1.0);
        updateLensColor();
    }

    /**
     * Aktiviert oder deaktiviert den Strobe-Effekt.
     *
     * @param strobing True, falls der Strobe aktiv ist
     */
    public void setStrobing(boolean strobing) {
        this.strobing = strobing;
        updateLensColor();
    }

    /**
     * Setzt die aktuelle Phase des Strobe-Blitzes (für Animation & Tests).
     *
     * @param darkPhase True für Dunkelphase, False für Blitzphase
     */
    public void setStrobePhase(boolean darkPhase) {
        this.strobePhase = darkPhase;
        updateLensColor();
    }

    private void updateLensColor() {
        Color litColor;
        if (strobing && strobePhase) {
            litColor = COLOR_DARK_OFF;
        } else {
            litColor = COLOR_DARK_OFF.interpolate(baseColor, dimmer);
        }

        lens.setFill(litColor);
        updateTrackerDotContrast(litColor);
    }

    private void updateTrackerDotContrast(Color background) {
        if (!panTiltSupported) {
            return;
        }

        // Relative WCAG-Luminanz: L = 0.2126*R + 0.7152*G + 0.0722*B
        double lum = 0.2126 * background.getRed() + 0.7152 * background.getGreen() + 0.0722 * background.getBlue();

        if (lum >= 0.45) {
            // Heller Hintergrund -> Schwarzer Punkt mit hellem Rand
            trackerDot.setFill(Color.web("#121212"));
            trackerDot.setStroke(Color.web("#E5E7EB"));
            trackerDot.setStrokeWidth(0.8);
        } else {
            // Dunkler Hintergrund -> Weißer Punkt mit dunklem Rand
            trackerDot.setFill(Color.web("#FFFFFF"));
            trackerDot.setStroke(Color.web("#1F2937"));
            trackerDot.setStrokeWidth(0.8);
        }
    }

    public void setOnDragFinished(Runnable onDragFinished) {
        this.onDragFinished = onDragFinished;
    }

    public void setOnFixtureClicked(java.util.function.BiConsumer<StageFixtureNode, javafx.scene.input.MouseEvent> onFixtureClicked) {
        this.onFixtureClicked = onFixtureClicked;
    }

    public StackPane getSquare() {
        return square;
    }

    public ExplorerItem getItem() {
        return item;
    }

    public FixtureInstance getFixtureInstance() {
        return fixtureInstance;
    }

    public Circle getLens() {
        return lens;
    }

    public Circle getTrackerDot() {
        return trackerDot;
    }

    public Label getNameBadge() {
        return nameBadge;
    }

    public boolean hasPanTilt() {
        return panTiltSupported;
    }

    public boolean hasColorChannels() {
        return colorChannelsSupported;
    }

    public boolean hasStrobe() {
        return strobeSupported;
    }

    public Color getBaseColor() {
        return baseColor;
    }

    public double getDimmer() {
        return dimmer;
    }

    public boolean isStrobing() {
        return strobing;
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean sel) {
        selected.set(sel);
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }
}

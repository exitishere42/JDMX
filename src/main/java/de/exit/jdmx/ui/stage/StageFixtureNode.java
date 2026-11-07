package de.exit.jdmx.ui.stage;

import de.exit.jdmx.core.model.ChannelType;
import de.exit.jdmx.core.model.ExplorerItem;
import de.exit.jdmx.core.model.FixtureInstance;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;

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

    private final Group gizmoXArrow = new Group();
    private final Group gizmoYArrow = new Group();
    private MultiDragListener multiDragListener;

    private Point2D dragStartMouse;
    private double dragStartLayoutX;
    private double dragStartLayoutY;
    private boolean hasDragged = false;

    private Point2D gizmoDragStartMouse;
    private double gizmoDragStartLayoutX;
    private double gizmoDragStartLayoutY;
    private boolean hasDraggedGizmo = false;

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

        // 6. Transformations-Gizmo (X- und Y-Pfeile zur achsenbasierten Bewegung)
        setupGizmoArrows();

        getChildren().addAll(square, nameBadge, gizmoXArrow, gizmoYArrow);

        // 7. Selektions-Listener
        selected.addListener((obs, oldVal, isSel) -> updateSelectionVisuals(isSel));

        // 8. Maus-Interaktionen (Klick zur Selektion, Drag zum Verschieben)
        setupInteractions();

        // 9. Initiale Farben und Kontraste berechnen
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

    private void setupGizmoArrows() {
        // --- X-Pfeil (Horizontal nach rechts, Signalrot #EF4444) ---
        gizmoXArrow.setVisible(false);
        gizmoXArrow.setCursor(Cursor.H_RESIZE);
        gizmoXArrow.getStyleClass().add("stage-gizmo-x-arrow");

        Line xHitLine = new Line(SQUARE_SIZE, SQUARE_SIZE / 2.0, SQUARE_SIZE + 32.0, SQUARE_SIZE / 2.0);
        xHitLine.setStroke(Color.TRANSPARENT);
        xHitLine.setStrokeWidth(14.0);

        Line xVisibleLine = new Line(SQUARE_SIZE, SQUARE_SIZE / 2.0, SQUARE_SIZE + 24.0, SQUARE_SIZE / 2.0);
        xVisibleLine.setStroke(Color.web("#EF4444"));
        xVisibleLine.setStrokeWidth(2.0);
        xVisibleLine.setMouseTransparent(true);

        Polygon xArrowHead = new Polygon(
                SQUARE_SIZE + 32.0, SQUARE_SIZE / 2.0,
                SQUARE_SIZE + 24.0, (SQUARE_SIZE / 2.0) - 4.5,
                SQUARE_SIZE + 24.0, (SQUARE_SIZE / 2.0) + 4.5
        );
        xArrowHead.setFill(Color.web("#EF4444"));
        xArrowHead.setMouseTransparent(true);

        gizmoXArrow.getChildren().addAll(xHitLine, xVisibleLine, xArrowHead);

        gizmoXArrow.setOnMouseEntered(e -> {
            xVisibleLine.setStroke(Color.web("#F87171"));
            xArrowHead.setFill(Color.web("#F87171"));
        });
        gizmoXArrow.setOnMouseExited(e -> {
            xVisibleLine.setStroke(Color.web("#EF4444"));
            xArrowHead.setFill(Color.web("#EF4444"));
        });

        gizmoXArrow.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                Point2D parentPoint = getParent() != null
                        ? getParent().sceneToLocal(e.getSceneX(), e.getSceneY())
                        : new Point2D(e.getSceneX(), e.getSceneY());
                gizmoDragStartMouse = parentPoint;
                gizmoDragStartLayoutX = getLayoutX();
                hasDraggedGizmo = false;
                toFront();

                if (multiDragListener != null) {
                    multiDragListener.onMultiDragStart(this);
                }
                e.consume();
            }
        });

        gizmoXArrow.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY && gizmoDragStartMouse != null) {
                hasDraggedGizmo = true;
                Point2D parentPoint = getParent() != null
                        ? getParent().sceneToLocal(e.getSceneX(), e.getSceneY())
                        : new Point2D(e.getSceneX(), e.getSceneY());
                double deltaX = parentPoint.getX() - gizmoDragStartMouse.getX();

                if (multiDragListener != null) {
                    multiDragListener.onMultiDrag(this, deltaX, 0.0);
                } else {
                    double newX = Math.max(0, gizmoDragStartLayoutX + deltaX);
                    setLayoutX(newX);
                    if (fixtureInstance != null) {
                        fixtureInstance.setStageX(newX);
                    }
                }
                e.consume();
            }
        });

        gizmoXArrow.setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.PRIMARY && hasDraggedGizmo) {
                hasDraggedGizmo = false;
                gizmoDragStartMouse = null;
                if (multiDragListener != null) {
                    multiDragListener.onMultiDragEnd(this);
                }
                if (onDragFinished != null) {
                    onDragFinished.run();
                }
                e.consume();
            }
        });

        // --- Y-Pfeil (Vertikal nach oben, Signalgrün #22C55E) ---
        gizmoYArrow.setVisible(false);
        gizmoYArrow.setCursor(Cursor.V_RESIZE);
        gizmoYArrow.getStyleClass().add("stage-gizmo-y-arrow");

        Line yHitLine = new Line(SQUARE_SIZE / 2.0, 0, SQUARE_SIZE / 2.0, -32.0);
        yHitLine.setStroke(Color.TRANSPARENT);
        yHitLine.setStrokeWidth(14.0);

        Line yVisibleLine = new Line(SQUARE_SIZE / 2.0, 0, SQUARE_SIZE / 2.0, -24.0);
        yVisibleLine.setStroke(Color.web("#22C55E"));
        yVisibleLine.setStrokeWidth(2.0);
        yVisibleLine.setMouseTransparent(true);

        Polygon yArrowHead = new Polygon(
                SQUARE_SIZE / 2.0, -32.0,
                (SQUARE_SIZE / 2.0) - 4.5, -24.0,
                (SQUARE_SIZE / 2.0) + 4.5, -24.0
        );
        yArrowHead.setFill(Color.web("#22C55E"));
        yArrowHead.setMouseTransparent(true);

        gizmoYArrow.getChildren().addAll(yHitLine, yVisibleLine, yArrowHead);

        gizmoYArrow.setOnMouseEntered(e -> {
            yVisibleLine.setStroke(Color.web("#4ADE80"));
            yArrowHead.setFill(Color.web("#4ADE80"));
        });
        gizmoYArrow.setOnMouseExited(e -> {
            yVisibleLine.setStroke(Color.web("#22C55E"));
            yArrowHead.setFill(Color.web("#22C55E"));
        });

        gizmoYArrow.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                Point2D parentPoint = getParent() != null
                        ? getParent().sceneToLocal(e.getSceneX(), e.getSceneY())
                        : new Point2D(e.getSceneX(), e.getSceneY());
                gizmoDragStartMouse = parentPoint;
                gizmoDragStartLayoutY = getLayoutY();
                hasDraggedGizmo = false;
                toFront();

                if (multiDragListener != null) {
                    multiDragListener.onMultiDragStart(this);
                }
                e.consume();
            }
        });

        gizmoYArrow.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY && gizmoDragStartMouse != null) {
                hasDraggedGizmo = true;
                Point2D parentPoint = getParent() != null
                        ? getParent().sceneToLocal(e.getSceneX(), e.getSceneY())
                        : new Point2D(e.getSceneX(), e.getSceneY());
                double deltaY = parentPoint.getY() - gizmoDragStartMouse.getY();

                if (multiDragListener != null) {
                    multiDragListener.onMultiDrag(this, 0.0, deltaY);
                } else {
                    double newY = Math.max(0, gizmoDragStartLayoutY + deltaY);
                    setLayoutY(newY);
                    if (fixtureInstance != null) {
                        fixtureInstance.setStageY(newY);
                    }
                }
                e.consume();
            }
        });

        gizmoYArrow.setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.PRIMARY && hasDraggedGizmo) {
                hasDraggedGizmo = false;
                gizmoDragStartMouse = null;
                if (multiDragListener != null) {
                    multiDragListener.onMultiDragEnd(this);
                }
                if (onDragFinished != null) {
                    onDragFinished.run();
                }
                e.consume();
            }
        });
    }

    private void setupInteractions() {
        // Drag- und Klick-Interaktionen direkt auf dem 36x36px Gehäuse
        square.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                Point2D parentPoint = getParent() != null
                        ? getParent().sceneToLocal(e.getSceneX(), e.getSceneY())
                        : new Point2D(e.getSceneX(), e.getSceneY());
                dragStartMouse = parentPoint;
                dragStartLayoutX = getLayoutX();
                dragStartLayoutY = getLayoutY();
                hasDragged = false;
                toFront();

                if (multiDragListener != null) {
                    multiDragListener.onMultiDragStart(this);
                }
                e.consume();
            }
        });

        square.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY && dragStartMouse != null) {
                hasDragged = true;
                Point2D parentPoint = getParent() != null
                        ? getParent().sceneToLocal(e.getSceneX(), e.getSceneY())
                        : new Point2D(e.getSceneX(), e.getSceneY());
                double deltaX = parentPoint.getX() - dragStartMouse.getX();
                double deltaY = parentPoint.getY() - dragStartMouse.getY();

                if (multiDragListener != null) {
                    multiDragListener.onMultiDrag(this, deltaX, deltaY);
                } else {
                    double newX = Math.max(0, dragStartLayoutX + deltaX);
                    double newY = Math.max(0, dragStartLayoutY + deltaY);
                    setLayoutX(newX);
                    setLayoutY(newY);

                    if (fixtureInstance != null) {
                        fixtureInstance.setStageX(newX);
                        fixtureInstance.setStageY(newY);
                    }
                }
                e.consume();
            }
        });

        square.setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.PRIMARY && hasDragged) {
                hasDragged = false;
                dragStartMouse = null;
                if (multiDragListener != null) {
                    multiDragListener.onMultiDragEnd(this);
                }
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
        gizmoXArrow.setVisible(isSel);
        gizmoYArrow.setVisible(isSel);
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

    /**
     * Aktualisiert den Text des Namens-Badges mit dem aktuellen Namen des Items und DMX-Adresse.
     */
    public void updateBadgeName() {
        if (nameBadge != null) {
            nameBadge.setText(buildBadgeText());
        }
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

    /**
     * Liefert den X-Achsen-Pfeil (Signalrot) des Transformations-Gizmos.
     *
     * @return X-Gizmo Node
     */
    public Node getGizmoXArrow() {
        return gizmoXArrow;
    }

    /**
     * Liefert den Y-Achsen-Pfeil (Signalgrün) des Transformations-Gizmos.
     *
     * @return Y-Gizmo Node
     */
    public Node getGizmoYArrow() {
        return gizmoYArrow;
    }

    public void setMultiDragListener(MultiDragListener listener) {
        this.multiDragListener = listener;
    }

    public MultiDragListener getMultiDragListener() {
        return multiDragListener;
    }

    /**
     * Callback-Schnittstelle zur synchronen Verschiebung mehrerer selektierter Fixtures.
     */
    @FunctionalInterface
    public interface MultiDragListener {
        /**
         * Wird während eines aktiven Drags kontinuierlich mit dem Gesamtoffset aufgerufen.
         *
         * @param initiator Der Knoten, an dem der Drag ausgeführt wird
         * @param deltaX    Horizontale Verschiebung relativ zum Drag-Start
         * @param deltaY    Vertikale Verschiebung relativ zum Drag-Start
         */
        void onMultiDrag(StageFixtureNode initiator, double deltaX, double deltaY);

        /**
         * Signalisiert den Beginn eines Drag-Vorgangs.
         *
         * @param initiator Der Knoten, an dem der Drag gestartet wurde
         */
        default void onMultiDragStart(StageFixtureNode initiator) {}

        /**
         * Signalisiert das Beenden eines Drag-Vorgangs.
         *
         * @param initiator Der Knoten, an dem der Drag beendet wurde
         */
        default void onMultiDragEnd(StageFixtureNode initiator) {}
    }
}

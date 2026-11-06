package de.exit.jdmx.ui.stage;

import de.exit.jdmx.core.model.ExplorerItem;
import de.exit.jdmx.core.model.ExplorerItemType;
import de.exit.jdmx.service.ProjectExplorerService;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * 2D-Arbeitsbereich für die Bühne (Tab 1: Stage & Fixtures).
 * <p>
 * Unterstützt:
 * <ul>
 *     <li>Stufenlosen Mausrad-Zoom (0.5x bis 3.5x), zentriert an der aktuellen Mauszeigerposition</li>
 *     <li>Kamera-Verschiebung (Pan) über Ziehen mit der mittleren Maustaste (Mausrad-Klick)</li>
 *     <li>Platzierung von Scheinwerfern (Fixtures) per Drag & Drop aus dem {@link ProjectExplorerView}
 *         mit automatischer Koordinatenumrechnung (sceneToLocal) auch bei aktivem Zoom und Pan</li>
 *     <li>Nachträgliches freies Verschieben platzierter Scheinwerfer auf der Bühne per Maus</li>
 *     <li>Reaktive Benachrichtigung über den Platzierungs-Callback</li>
 *     <li>Deselektion aller Scheinwerfer beim Klick auf die leere Bühnenfläche</li>
 * </ul>
 * </p>
 */
public class StageCanvasView extends StackPane {

    /** Minimaler Zoomfaktor (50%). */
    public static final double MIN_ZOOM = 0.5;

    /** Maximaler Zoomfaktor (350%). */
    public static final double MAX_ZOOM = 3.5;

    /** Zoom-Schrittfaktor pro Mausrad-Rastung. */
    public static final double ZOOM_FACTOR = 1.15;

    private final Pane stageSurface;
    private final Scale scaleTransform;
    private final List<StageFixtureNode> placedFixtures = new ArrayList<>();

    private ProjectExplorerService explorerService;
    private BiConsumer<ExplorerItem, Point2D> onFixturePlaced;
    private BiConsumer<ExplorerItem, Point2D> onFixtureMoved;

    // Zustand für Kamera-Verschiebung (Pan) mit mittlerer Maustaste
    private double panStartMouseX;
    private double panStartMouseY;
    private double panStartTranslateX;
    private double panStartTranslateY;
    private boolean isPanning = false;

    public StageCanvasView() {
        this(new ProjectExplorerService());
    }

    public StageCanvasView(ProjectExplorerService explorerService) {
        this.explorerService = explorerService;

        getStyleClass().add("stage-canvas-view");
        setMinHeight(150.0);
        setAlignment(Pos.TOP_LEFT);

        // Ansicht auf die Canvas-Größe beschneiden (Clip), damit gezoomte Elemente nicht überlaufen
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);

        this.stageSurface = new Pane();
        stageSurface.getStyleClass().add("stage-surface-pane");

        // Skalierungstransformation mit festem Ursprung (0, 0)
        this.scaleTransform = new Scale(1.0, 1.0, 0.0, 0.0);
        stageSurface.getTransforms().add(scaleTransform);

        // Klick auf den leeren Hintergrund deselektiert alle Fixtures (Bühne wird sauber)
        stageSurface.setOnMouseClicked(event -> {
            if (event.getTarget() == stageSurface && event.getButton() == MouseButton.PRIMARY) {
                deselectAll();
            }
        });

        setupNavigation();
        setupDropTarget();

        getChildren().add(stageSurface);
    }

    private void setupNavigation() {
        // 1. Stufenloser Mausrad-Zoom zentriert am Mauszeiger
        setOnScroll((ScrollEvent event) -> {
            double delta = event.getDeltaY();
            if (delta == 0) {
                return;
            }
            double factor = delta > 0 ? ZOOM_FACTOR : (1.0 / ZOOM_FACTOR);
            zoomAt(event.getX(), event.getY(), factor);
            event.consume();
        });

        // 2. Kamera-Verschiebung (Pan) mit mittlerer Maustaste
        addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() == MouseButton.MIDDLE) {
                isPanning = true;
                panStartMouseX = event.getX();
                panStartMouseY = event.getY();
                panStartTranslateX = stageSurface.getTranslateX();
                panStartTranslateY = stageSurface.getTranslateY();
                setCursor(Cursor.MOVE);
                event.consume();
            }
        });

        addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (isPanning && event.getButton() == MouseButton.MIDDLE) {
                double deltaX = event.getX() - panStartMouseX;
                double deltaY = event.getY() - panStartMouseY;
                stageSurface.setTranslateX(panStartTranslateX + deltaX);
                stageSurface.setTranslateY(panStartTranslateY + deltaY);
                event.consume();
            }
        });

        addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (event.getButton() == MouseButton.MIDDLE) {
                isPanning = false;
                setCursor(Cursor.DEFAULT);
                event.consume();
            }
        });
    }

    /**
     * Führt einen stufenlosen Zoom zentriert an den übergebenen Viewport-Koordinaten aus.
     *
     * @param viewportX Horizontale Mauszeiger-Position im Arbeitsbereich
     * @param viewportY Vertikale Mauszeiger-Position im Arbeitsbereich
     * @param factor    Multiplikativer Skalierungsfaktor (z. B. 1.15 für Hineinzoomen)
     */
    public void zoomAt(double viewportX, double viewportY, double factor) {
        double oldZoom = getZoom();
        double targetZoom = oldZoom * factor;
        double newZoom = Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, targetZoom));
        if (Math.abs(newZoom - oldZoom) < 0.0001) {
            return;
        }

        // Lokaler Bühnenpunkt unter der Maus vor dem Zoomen
        double localX = (viewportX - stageSurface.getTranslateX()) / oldZoom;
        double localY = (viewportY - stageSurface.getTranslateY()) / oldZoom;

        // Neuen Zoom setzen
        setZoom(newZoom);

        // Translation so anpassen, dass (localX, localY) exakt unter (viewportX, viewportY) bleibt
        double newTranslateX = viewportX - (localX * newZoom);
        double newTranslateY = viewportY - (localY * newZoom);
        stageSurface.setTranslateX(newTranslateX);
        stageSurface.setTranslateY(newTranslateY);
    }

    /**
     * Gibt den aktuellen Vergrößerungsfaktor der Bühnenansicht zurück (Standard: 1.0).
     *
     * @return Der Zoomfaktor
     */
    public double getZoom() {
        return scaleTransform.getX();
    }

    /**
     * Setzt den Zoomfaktor der Bühnenansicht (wird zwischen {@link #MIN_ZOOM} und {@link #MAX_ZOOM} begrenzt).
     *
     * @param zoom Der gewünschte Zoomfaktor
     */
    public void setZoom(double zoom) {
        double clamped = Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, zoom));
        scaleTransform.setX(clamped);
        scaleTransform.setY(clamped);
    }

    /**
     * Gibt die aktuelle horizontale Kameraverschiebung zurück.
     *
     * @return Horizontale Verschiebung in Pixeln
     */
    public double getPanX() {
        return stageSurface.getTranslateX();
    }

    /**
     * Gibt die aktuelle vertikale Kameraverschiebung zurück.
     *
     * @return Vertikale Verschiebung in Pixeln
     */
    public double getPanY() {
        return stageSurface.getTranslateY();
    }

    /**
     * Setzt die Kameraverschiebung auf die angegebenen Werte.
     *
     * @param panX Horizontale Zielverschiebung
     * @param panY Vertikale Zielverschiebung
     */
    public void setPan(double panX, double panY) {
        stageSurface.setTranslateX(panX);
        stageSurface.setTranslateY(panY);
    }

    /**
     * Verschiebt die Kamera um die angegebenen Relativwerte.
     *
     * @param deltaX Horizontales Inkrement
     * @param deltaY Vertikales Inkrement
     */
    public void panBy(double deltaX, double deltaY) {
        stageSurface.setTranslateX(stageSurface.getTranslateX() + deltaX);
        stageSurface.setTranslateY(stageSurface.getTranslateY() + deltaY);
    }

    /**
     * Setzt Zoom (1.0x) und Kamera-Position (0, 0) auf die Ausgangsansicht zurück.
     */
    public void resetView() {
        setZoom(1.0);
        setPan(0.0, 0.0);
    }

    private void setupDropTarget() {
        setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasContent(ProjectExplorerView.EXPLORER_ITEM_FORMAT) || db.hasString()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                if (!getStyleClass().contains("stage-canvas-drag-over")) {
                    getStyleClass().add("stage-canvas-drag-over");
                }
            }
            event.consume();
        });

        setOnDragExited(event -> {
            getStyleClass().remove("stage-canvas-drag-over");
            event.consume();
        });

        setOnDragDropped(event -> {
            getStyleClass().remove("stage-canvas-drag-over");

            Dragboard db = event.getDragboard();
            String itemId = (String) (db.hasContent(ProjectExplorerView.EXPLORER_ITEM_FORMAT)
                    ? db.getContent(ProjectExplorerView.EXPLORER_ITEM_FORMAT)
                    : db.getString());

            if (itemId != null && explorerService != null) {
                ExplorerItem item = explorerService.findItemById(itemId);
                if (item != null && item.getType() == ExplorerItemType.FIXTURE) {
                    Point2D localPoint = stageSurface.sceneToLocal(event.getSceneX(), event.getSceneY());
                    double targetX = Math.max(10, localPoint.getX() - (StageFixtureNode.SQUARE_SIZE / 2.0));
                    double targetY = Math.max(10, localPoint.getY() - (StageFixtureNode.SQUARE_SIZE / 2.0));
                    placeFixture(item, targetX, targetY);
                    event.setDropCompleted(true);
                }
            }
            event.consume();
        });
    }

    /**
     * Platziert ein Fixture an den angegebenen Koordinaten auf der 2D-Bühne.
     *
     * @param item Das zu platzierende ExplorerItem
     * @param x    Horizontale Zielposition
     * @param y    Vertikale Zielposition
     * @return Der erzeugte Knoten auf der Bühne
     */
    public StageFixtureNode placeFixture(ExplorerItem item, double x, double y) {
        Objects.requireNonNull(item, "item must not be null");
        StageFixtureNode node = new StageFixtureNode(item, x, y);

        // Exklusive Selektion: Klick auf ein Fixture schließt Badges anderer Fixtures
        node.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (Boolean.TRUE.equals(newVal)) {
                for (StageFixtureNode other : placedFixtures) {
                    if (other != node) {
                        other.setSelected(false);
                    }
                }
            }
        });

        placedFixtures.add(node);
        stageSurface.getChildren().add(node);

        node.setOnDragFinished(() -> {
            if (onFixtureMoved != null) {
                onFixtureMoved.accept(item, new Point2D(node.getLayoutX(), node.getLayoutY()));
            }
        });

        if (onFixturePlaced != null) {
            onFixturePlaced.accept(item, new Point2D(x, y));
        }

        return node;
    }

    /**
     * Entfernt alle platzierten Scheinwerfer von der 2D-Bühne.
     */
    public void clearFixtures() {
        stageSurface.getChildren().removeAll(placedFixtures);
        placedFixtures.clear();
    }

    /**
     * Hebt die Selektion aller platzierten Scheinwerfer auf und blendet deren Badges aus.
     */
    public void deselectAll() {
        for (StageFixtureNode node : placedFixtures) {
            node.setSelected(false);
        }
    }

    public List<StageFixtureNode> getPlacedFixtures() {
        return Collections.unmodifiableList(placedFixtures);
    }

    /**
     * Liefert alle aktuell auf der Bühne ausgewählten Scheinwerfer.
     *
     * @return Liste der selektierten Scheinwerferknoten
     */
    public List<StageFixtureNode> getSelectedFixtures() {
        return placedFixtures.stream()
                .filter(StageFixtureNode::isSelected)
                .toList();
    }

    /**
     * Liefert die aktuell zu steuernden Scheinwerfer.
     * Falls Scheinwerfer selektiert sind, werden diese zurückgegeben;
     * falls keines selektiert ist, alle platzierten Scheinwerfer.
     *
     * @return Liste der zu steuernden Scheinwerferknoten
     */
    public List<StageFixtureNode> getActiveControlledFixtures() {
        List<StageFixtureNode> selected = getSelectedFixtures();
        if (!selected.isEmpty()) {
            return selected;
        }
        return getPlacedFixtures();
    }

    public void setOnFixturePlaced(BiConsumer<ExplorerItem, Point2D> onFixturePlaced) {
        this.onFixturePlaced = onFixturePlaced;
    }

    public void setOnFixtureMoved(BiConsumer<ExplorerItem, Point2D> onFixtureMoved) {
        this.onFixtureMoved = onFixtureMoved;
    }

    public BiConsumer<ExplorerItem, Point2D> getOnFixtureMoved() {
        return onFixtureMoved;
    }

    public void setExplorerService(ProjectExplorerService explorerService) {
        this.explorerService = explorerService;
    }

    public ProjectExplorerService getExplorerService() {
        return explorerService;
    }

    public Pane getStageSurface() {
        return stageSurface;
    }

    /**
     * Abwärtskompatible Wrapper-Klasse für zuvor verwendete PlacedFixtureNodes.
     */
    public static class PlacedFixtureNode extends StageFixtureNode {
        public PlacedFixtureNode(ExplorerItem item, double initialX, double initialY) {
            super(item, initialX, initialY);
        }
    }
}

package de.exit.jdmx.ui.stage;

import de.exit.jdmx.core.model.ExplorerItem;
import de.exit.jdmx.service.ProjectExplorerService;
import de.exit.jdmx.ui.icon.LucideIcon;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * 2D-Arbeitsbereich für die Bühne (Tab 1: Stage & Fixtures).
 * <p>
 * Unterstützt:
 * <ul>
 *     <li>Platzierung von Scheinwerfern (Fixtures) per Drag & Drop aus dem {@link ProjectExplorerView}</li>
 *     <li>Exakte Positionierung an den Zielkoordinaten auf der Bühnenoberfläche</li>
 *     <li>Nachträgliches freies Verschieben platzierter Scheinwerfer auf der Bühne</li>
 *     <li>Reaktive Benachrichtigung über den Platzierungs-Callback</li>
 * </ul>
 * </p>
 */
public class StageCanvasView extends StackPane {

    private final Pane stageSurface;
    private final List<PlacedFixtureNode> placedFixtures = new ArrayList<>();

    private ProjectExplorerService explorerService;
    private BiConsumer<ExplorerItem, Point2D> onFixturePlaced;

    public StageCanvasView() {
        this(null);
    }

    public StageCanvasView(ProjectExplorerService explorerService) {
        this.explorerService = explorerService;

        getStyleClass().add("stage-canvas-view");
        setMinHeight(150.0);
        setAlignment(Pos.TOP_LEFT);

        this.stageSurface = new Pane();
        stageSurface.getStyleClass().add("stage-surface-pane");

        getChildren().add(stageSurface);
    }

    /**
     * Platziert ein Fixture an den angegebenen Koordinaten auf der 2D-Bühne.
     *
     * @param item Das zu platzierende ExplorerItem
     * @param x    Horizontale Zielposition
     * @param y    Vertikale Zielposition
     * @return Der erzeugte Knoten auf der Bühne
     */
    public PlacedFixtureNode placeFixture(ExplorerItem item, double x, double y) {
        PlacedFixtureNode node = new PlacedFixtureNode(item, x, y);
        placedFixtures.add(node);
        stageSurface.getChildren().add(node);

        if (onFixturePlaced != null) {
            onFixturePlaced.accept(item, new Point2D(x, y));
        }

        return node;
    }

    public List<PlacedFixtureNode> getPlacedFixtures() {
        return Collections.unmodifiableList(placedFixtures);
    }

    public void setOnFixturePlaced(BiConsumer<ExplorerItem, Point2D> onFixturePlaced) {
        this.onFixturePlaced = onFixturePlaced;
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
     * Visuelle Repräsentation eines platzierten Scheinwerfers auf der 2D-Bühne.
     * Lässt sich frei per Maus auf der Bühne verschieben.
     */
    public static class PlacedFixtureNode extends HBox {

        private final ExplorerItem item;
        private double dragStartX;
        private double dragStartY;

        public PlacedFixtureNode(ExplorerItem item, double initialX, double initialY) {
            super(6);
            this.item = item;

            getStyleClass().add("stage-fixture-node");
            setAlignment(Pos.CENTER_LEFT);
            setLayoutX(initialX);
            setLayoutY(initialY);

            LucideIcon icon = new LucideIcon(item.getIconName(), 14, item.getColor());
            Label label = new Label(item.getName());
            label.getStyleClass().add("stage-fixture-label");

            getChildren().addAll(icon, label);

            setupCanvasDragging();
        }

        private void setupCanvasDragging() {
            setOnMousePressed(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    dragStartX = e.getSceneX() - getLayoutX();
                    dragStartY = e.getSceneY() - getLayoutY();
                    toFront();
                    e.consume();
                }
            });

            setOnMouseDragged(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    double newX = Math.max(0, e.getSceneX() - dragStartX);
                    double newY = Math.max(0, e.getSceneY() - dragStartY);
                    setLayoutX(newX);
                    setLayoutY(newY);
                    e.consume();
                }
            });
        }

        public ExplorerItem getItem() {
            return item;
        }
    }
}

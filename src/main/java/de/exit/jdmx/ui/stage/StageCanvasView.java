package de.exit.jdmx.ui.stage;

import de.exit.jdmx.core.model.ExplorerItem;
import de.exit.jdmx.core.model.ExplorerItemType;
import de.exit.jdmx.service.ProjectExplorerService;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

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
 *     <li>Platzierung von Scheinwerfern (Fixtures) per Drag & Drop aus dem {@link ProjectExplorerView}</li>
 *     <li>Exakte Positionierung an den Zielkoordinaten auf der Bühnenoberfläche</li>
 *     <li>Nachträgliches freies Verschieben platzierter Scheinwerfer auf der Bühne per Maus</li>
 *     <li>Reaktive Benachrichtigung über den Platzierungs-Callback</li>
 *     <li>Deselektion aller Scheinwerfer beim Klick auf die leere Bühnenfläche</li>
 * </ul>
 * </p>
 */
public class StageCanvasView extends StackPane {

    private final Pane stageSurface;
    private final List<StageFixtureNode> placedFixtures = new ArrayList<>();

    private ProjectExplorerService explorerService;
    private BiConsumer<ExplorerItem, Point2D> onFixturePlaced;

    public StageCanvasView() {
        this(new ProjectExplorerService());
    }

    public StageCanvasView(ProjectExplorerService explorerService) {
        this.explorerService = explorerService;

        getStyleClass().add("stage-canvas-view");
        setMinHeight(150.0);
        setAlignment(Pos.TOP_LEFT);

        this.stageSurface = new Pane();
        stageSurface.getStyleClass().add("stage-surface-pane");

        // Klick auf den leeren Hintergrund deselektiert alle Fixtures (Bühne wird sauber)
        stageSurface.setOnMouseClicked(event -> {
            if (event.getTarget() == stageSurface) {
                deselectAll();
            }
        });

        setupDropTarget();

        getChildren().add(stageSurface);
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
                    double targetX = Math.max(10, event.getX() - (StageFixtureNode.SQUARE_SIZE / 2.0));
                    double targetY = Math.max(10, event.getY() - (StageFixtureNode.SQUARE_SIZE / 2.0));
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

        if (onFixturePlaced != null) {
            onFixturePlaced.accept(item, new Point2D(x, y));
        }

        return node;
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
     * Abwärtskompatible Wrapper-Klasse für zuvor verwendete PlacedFixtureNodes.
     */
    public static class PlacedFixtureNode extends StageFixtureNode {
        public PlacedFixtureNode(ExplorerItem item, double initialX, double initialY) {
            super(item, initialX, initialY);
        }
    }
}

package de.exit.jdmx.ui.matrix;

import de.exit.jdmx.core.model.ExplorerItem;
import de.exit.jdmx.core.model.ExplorerItemType;
import de.exit.jdmx.core.model.MatrixGridDefinition;
import de.exit.jdmx.core.model.MatrixTargetType;
import de.exit.jdmx.service.MatrixService;
import de.exit.jdmx.service.ProjectExplorerService;
import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.theme.StudioTheme;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Visualisiert das Tastenraster der Button Matrix in Tab 3.
 * <p>
 * Ordnet {@link MatrixButtonNode}s im responsiven 2D-Grid an, reagiert auf
 * Zustandsänderungen des {@link MatrixService} und ermöglicht Drag &amp; Drop Zuweisungen.
 * </p>
 */
public class MatrixGridView extends VBox {

    private final MatrixService matrixService;
    private final ProjectExplorerService explorerService;
    private final List<MatrixButtonNode> buttonNodes = new ArrayList<>();
    private final GridPane gridPane;
    private final ScrollPane scrollPane;

    private int selectedButtonIndex = -1;
    private Consumer<Integer> onButtonSelectedListener;

    /**
     * Erstellt ein neues {@link MatrixGridView} mit dem gegebenen MatrixService.
     *
     * @param matrixService der {@link MatrixService} (darf nicht {@code null} sein)
     */
    public MatrixGridView(MatrixService matrixService) {
        this(matrixService, null);
    }

    /**
     * Erstellt ein neues {@link MatrixGridView} mit MatrixService und optionalem ExplorerService.
     *
     * @param matrixService   der {@link MatrixService} (darf nicht {@code null} sein)
     * @param explorerService der {@link ProjectExplorerService} für Drag &amp; Drop Zuweisungen
     */
    public MatrixGridView(MatrixService matrixService, ProjectExplorerService explorerService) {
        this.matrixService = Objects.requireNonNull(matrixService, "matrixService darf nicht null sein");
        this.explorerService = explorerService;

        getStyleClass().add("matrix-grid-view");
        setStyle("-fx-background-color: " + StudioTheme.HEX_BG_BASE + ";");

        // 1. Obere Toolbar
        HBox topToolbar = new HBox(12);
        topToolbar.setAlignment(Pos.CENTER_LEFT);
        topToolbar.setPadding(new Insets(10, 16, 10, 16));
        topToolbar.setStyle("-fx-background-color: " + StudioTheme.HEX_SURFACE_1 + "; -fx-border-color: " + StudioTheme.HEX_BORDER + "; -fx-border-width: 0 0 1px 0;");

        Label lblTitle = new Label("Button Matrix");
        lblTitle.setStyle("-fx-text-fill: " + StudioTheme.HEX_TEXT_HIGH + "; -fx-font-size: 13px; -fx-font-weight: bold;");

        Label lblGridInfo = new Label(matrixService.getGrid().getRows() + " × " + matrixService.getGrid().getCols() + " (" + matrixService.getGrid().getTotalButtons() + " Buttons)");
        lblGridInfo.setStyle("-fx-text-fill: " + StudioTheme.HEX_TEXT_MED + "; -fx-font-size: 11px;");

        VBox titleBox = new VBox(2, lblTitle, lblGridInfo);

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnClearAll = new Button("Alle leeren");
        btnClearAll.setGraphic(new LucideIcon("trash-2", 13, Color.web(StudioTheme.HEX_ICON_RED)));
        btnClearAll.setOnAction(e -> matrixService.clearAllButtons());

        Button btnReleaseAll = new Button("Freigabe");
        btnReleaseAll.setGraphic(new LucideIcon("stop-circle", 13, Color.web(StudioTheme.HEX_ICON_RED)));
        btnReleaseAll.setOnAction(e -> matrixService.stopAll());

        topToolbar.getChildren().addAll(titleBox, spacer, btnReleaseAll, btnClearAll);

        // 2. Tasten-Raster im ScrollPane
        gridPane = new GridPane();
        gridPane.setHgap(8);
        gridPane.setVgap(8);
        gridPane.setPadding(new Insets(16));
        gridPane.setAlignment(Pos.TOP_LEFT);

        scrollPane = new ScrollPane(gridPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: " + StudioTheme.HEX_BG_BASE + ";");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(topToolbar, scrollPane);

        rebuildGrid();

        // Listener für Modell-Änderungen
        matrixService.addMatrixChangeListener(() -> Platform.runLater(this::updateAllButtons));
    }

    private void rebuildGrid() {
        gridPane.getChildren().clear();
        buttonNodes.clear();

        MatrixGridDefinition grid = matrixService.getGrid();
        int total = grid.getTotalButtons();
        int cols = grid.getCols();

        for (int i = 0; i < total; i++) {
            int row = i / cols;
            int col = i % cols;

            MatrixButtonNode node = new MatrixButtonNode(i, matrixService);
            node.setOnSelectListener(this::selectButton);
            node.setOnDropListener(this::handleItemDropped);

            buttonNodes.add(node);
            gridPane.add(node, col, row);
        }
    }

    private void handleItemDropped(int index, String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return;
        }

        MatrixTargetType type = MatrixTargetType.SCENE;
        String name = itemId;

        if (explorerService != null) {
            ExplorerItem item = explorerService.findItemById(itemId);
            if (item != null) {
                name = item.getName();
                if (item.getType() == ExplorerItemType.EFFECT) {
                    type = MatrixTargetType.CHASER;
                } else {
                    type = MatrixTargetType.SCENE;
                }
            }
        }

        matrixService.assignTarget(index, type, itemId, name);
        selectButton(index);
    }

    /**
     * Wählt den Button am angegebenen Index aus.
     *
     * @param index der Button-Index
     */
    public void selectButton(int index) {
        this.selectedButtonIndex = index;
        for (int i = 0; i < buttonNodes.size(); i++) {
            buttonNodes.get(i).setSelected(i == index);
        }
        if (onButtonSelectedListener != null) {
            onButtonSelectedListener.accept(index);
        }
    }

    private void updateAllButtons() {
        for (MatrixButtonNode node : buttonNodes) {
            node.updateState();
        }
    }

    /**
     * Gibt eine unmodifizierbare Liste aller Kacheln zurück.
     *
     * @return Liste der {@link MatrixButtonNode}s
     */
    public List<MatrixButtonNode> getButtonNodes() {
        return Collections.unmodifiableList(buttonNodes);
    }

    /**
     * Gibt den Button-Node am angegebenen Index zurück.
     *
     * @param index der Button-Index
     * @return der {@link MatrixButtonNode} oder {@code null}
     */
    public MatrixButtonNode getButtonNode(int index) {
        if (index >= 0 && index < buttonNodes.size()) {
            return buttonNodes.get(index);
        }
        return null;
    }

    /**
     * Gibt den Index des aktuell ausgewählten Buttons zurück.
     *
     * @return der Button-Index oder -1
     */
    public int getSelectedButtonIndex() {
        return selectedButtonIndex;
    }

    /**
     * Setzt den Callback bei Button-Auswahl.
     *
     * @param listener der Selektions-Listener
     */
    public void setOnButtonSelected(Consumer<Integer> listener) {
        this.onButtonSelectedListener = listener;
    }
}

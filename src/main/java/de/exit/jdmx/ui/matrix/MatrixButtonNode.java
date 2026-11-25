package de.exit.jdmx.ui.matrix;

import de.exit.jdmx.core.model.MatrixButtonDefinition;
import de.exit.jdmx.core.model.MatrixTargetType;
import de.exit.jdmx.core.model.MatrixTriggerMode;
import de.exit.jdmx.service.MatrixService;
import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.stage.ProjectExplorerView;
import de.exit.jdmx.ui.theme.StudioTheme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Taktile Kachel (Button-Node) für ein einzelnes Pad in der Button Matrix.
 * <p>
 * Visualisiert den zugewiesenen Namen, den Zieltyp, den Trigger-Modus (Toggle / Flash)
 * sowie den aktiven Leucht-Zustand nach Material Design 2 Dark Standards.
 * Unterstützt Drag &amp; Drop aus dem {@link ProjectExplorerView} sowie Maus-Pressed/Released.
 * </p>
 */
public class MatrixButtonNode extends StackPane {

    private final int index;
    private final MatrixService matrixService;
    private Consumer<Integer> onSelectListener;
    private BiConsumer<Integer, String> onDropListener;

    private final VBox contentBox;
    private final HBox topBox;
    private final Label lblIndex;
    private final StackPane iconContainer;
    private final Label lblName;
    private final Label lblBadge;

    private boolean selected = false;

    /**
     * Erstellt einen neuen {@link MatrixButtonNode}.
     *
     * @param index         der Button-Index im Grid
     * @param matrixService der {@link MatrixService} (darf nicht {@code null} sein)
     */
    public MatrixButtonNode(int index, MatrixService matrixService) {
        this.index = index;
        this.matrixService = Objects.requireNonNull(matrixService, "matrixService darf nicht null sein");

        getStyleClass().add("matrix-button-node");
        setMinSize(90, 75);
        setPrefSize(110, 85);

        contentBox = new VBox(4);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(6, 8, 6, 8));

        // 1. Obere Leiste (Index links, Icon rechts)
        topBox = new HBox();
        topBox.setAlignment(Pos.CENTER_LEFT);

        lblIndex = new Label("#" + (index + 1));
        lblIndex.setStyle("-fx-text-fill: " + StudioTheme.HEX_TEXT_LOW + "; -fx-font-size: 10px; -fx-font-weight: bold;");

        StackPane spacer = new StackPane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        iconContainer = new StackPane();
        topBox.getChildren().addAll(lblIndex, spacer, iconContainer);

        // 2. Mittiger Name
        lblName = new Label();
        lblName.setStyle("-fx-text-fill: " + StudioTheme.HEX_TEXT_HIGH + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        lblName.setAlignment(Pos.CENTER);
        lblName.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(lblName, Priority.ALWAYS);

        // 3. Unterer Trigger-Modus-Badge
        lblBadge = new Label();
        lblBadge.setStyle("-fx-text-fill: " + StudioTheme.HEX_TEXT_MED + "; -fx-font-size: 9px; -fx-font-weight: bold;");

        contentBox.getChildren().addAll(topBox, lblName, lblBadge);
        getChildren().add(contentBox);

        setupEventHandlers();
        updateState();
    }

    private void setupEventHandlers() {
        setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                if (onSelectListener != null) {
                    onSelectListener.accept(index);
                }
                matrixService.pressButton(index);
            }
        });

        setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                matrixService.releaseButton(index);
            }
        });

        // Kontextmenü per Rechtsklick zur Schnellkonfiguration
        setOnContextMenuRequested(e -> {
            showContextMenu(e.getScreenX(), e.getScreenY());
            e.consume();
        });

        // Drag & Drop aus Project Explorer
        setOnDragOver(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasContent(ProjectExplorerView.EXPLORER_ITEM_FORMAT) || db.hasString()) {
                e.acceptTransferModes(TransferMode.COPY, TransferMode.MOVE);
            }
            e.consume();
        });

        setOnDragEntered(e -> {
            setStyle(getCustomStyle(true, matrixService.isButtonActive(index)));
            e.consume();
        });

        setOnDragExited(e -> {
            updateState();
            e.consume();
        });

        setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            String id = null;
            if (db.hasContent(ProjectExplorerView.EXPLORER_ITEM_FORMAT)) {
                id = (String) db.getContent(ProjectExplorerView.EXPLORER_ITEM_FORMAT);
            } else if (db.hasString()) {
                id = db.getString();
            }

            boolean success = false;
            if (id != null && onDropListener != null) {
                onDropListener.accept(index, id);
                success = true;
            }
            e.setDropCompleted(success);
            e.consume();
        });
    }

    /**
     * Aktualisiert das Aussehen des Buttons basierend auf dem Zustand im {@link MatrixService}.
     */
    public void updateState() {
        MatrixButtonDefinition btn = matrixService.getGrid().getButton(index);
        boolean active = matrixService.isButtonActive(index);

        if (btn == null || !btn.isAssigned()) {
            lblName.setText("Leerer Button");
            lblName.setStyle("-fx-text-fill: " + StudioTheme.HEX_TEXT_LOW + "; -fx-font-size: 11px;");
            lblBadge.setText("");
            iconContainer.getChildren().clear();
            iconContainer.getChildren().add(new LucideIcon("plus", 12, Color.web(StudioTheme.HEX_TEXT_LOW)));
        } else {
            lblName.setText(btn.name());
            lblName.setStyle("-fx-text-fill: " + StudioTheme.HEX_TEXT_HIGH + "; -fx-font-size: 12px; -fx-font-weight: bold;");
            lblBadge.setText(btn.triggerMode() == MatrixTriggerMode.FLASH ? "FLASH" : "TOGGLE");

            iconContainer.getChildren().clear();
            Color iconColor = active ? Color.web(StudioTheme.HEX_BG_BASE) : Color.web(btn.color().equals(MatrixButtonDefinition.DEFAULT_COLOR) ? StudioTheme.HEX_TEXT_MED : btn.color());
            String iconName = btn.targetType() == MatrixTargetType.CHASER ? "play" : "sparkles";
            iconContainer.getChildren().add(new LucideIcon(iconName, 13, iconColor));
        }

        setStyle(getCustomStyle(false, active));
    }

    private String getCustomStyle(boolean dragHover, boolean active) {
        MatrixButtonDefinition btn = matrixService.getGrid().getButton(index);
        String btnColor = (btn != null && !btn.color().isBlank()) ? btn.color() : StudioTheme.HEX_SURFACE_2;

        if (dragHover) {
            return "-fx-background-color: #2D3748; -fx-background-radius: 4px; -fx-border-color: #38BDF8; -fx-border-width: 2px; -fx-border-radius: 4px;";
        }

        if (active) {
            String activeBg = btnColor.equals(MatrixButtonDefinition.DEFAULT_COLOR) ? StudioTheme.HEX_ICON_TIMELINE : btnColor;
            return "-fx-background-color: " + activeBg + "; -fx-background-radius: 4px; -fx-border-color: #FFFFFF; -fx-border-width: 2px; -fx-border-radius: 4px; -fx-effect: dropshadow(gaussian, " + activeBg + ", 12, 0.4, 0, 0);";
        }

        String borderColor = selected ? StudioTheme.HEX_PRIMARY_ORANGE : (btn != null && btn.isAssigned() ? btnColor : StudioTheme.HEX_BORDER);
        double borderWidth = selected ? 2.0 : 1.0;
        return "-fx-background-color: " + StudioTheme.HEX_SURFACE_1 + "; -fx-background-radius: 4px; -fx-border-color: " + borderColor + "; -fx-border-width: " + borderWidth + "px; -fx-border-radius: 4px;";
    }

    /**
     * Markiert diesen Button als aktuell im ControlDock selektiert.
     *
     * @param selected {@code true}, wenn selektiert
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
        updateState();
    }

    /**
     * Gibt den linearen Index dieses Buttons zurück.
     *
     * @return Button-Index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Setzt den Listener, der bei Klick zur Selektion des Buttons aufgerufen wird.
     *
     * @param listener der Selektions-Listener
     */
    public void setOnSelectListener(Consumer<Integer> listener) {
        this.onSelectListener = listener;
    }

    /**
     * Setzt den Listener für Drag &amp; Drop Zuweisungen.
     *
     * @param listener der Zuweisungs-Listener
     */
    public void setOnDropListener(BiConsumer<Integer, String> listener) {
        this.onDropListener = listener;
    }

    private void showContextMenu(double screenX, double screenY) {
        MatrixButtonDefinition btn = matrixService.getGrid().getButton(index);
        ContextMenu menu = new ContextMenu();

        if (btn != null && btn.isAssigned()) {
            RadioMenuItem itemToggle = new RadioMenuItem("Umschalten (Toggle)");
            itemToggle.setSelected(btn.triggerMode() == MatrixTriggerMode.TOGGLE);
            itemToggle.setOnAction(ev -> matrixService.setTriggerMode(index, MatrixTriggerMode.TOGGLE));

            RadioMenuItem itemFlash = new RadioMenuItem("Taster (Flash)");
            itemFlash.setSelected(btn.triggerMode() == MatrixTriggerMode.FLASH);
            itemFlash.setOnAction(ev -> matrixService.setTriggerMode(index, MatrixTriggerMode.FLASH));

            Menu menuColor = new Menu("Farbe");
            String[] colors = {"#03DAC6", "#38BDF8", "#EF4444", "#F59E0B", "#10B981", "#A855F7", "#FB923C", "#E2E8F0"};
            String[] colorNames = {"Teal", "Blau", "Rot", "Amber", "Grün", "Lila", "Orange", "Weiß"};
            for (int i = 0; i < colors.length; i++) {
                String c = colors[i];
                MenuItem colorItem = new MenuItem(colorNames[i]);
                colorItem.setGraphic(new LucideIcon("circle", 12, Color.web(c)));
                colorItem.setOnAction(ev -> matrixService.setButtonColor(index, c));
                menuColor.getItems().add(colorItem);
            }

            MenuItem itemClear = new MenuItem("Zuweisung löschen");
            itemClear.setGraphic(new LucideIcon("trash-2", 12, Color.web(StudioTheme.HEX_ICON_RED)));
            itemClear.setOnAction(ev -> matrixService.clearButton(index));

            menu.getItems().addAll(itemToggle, itemFlash, new SeparatorMenuItem(), menuColor, new SeparatorMenuItem(), itemClear);
        } else {
            MenuItem emptyItem = new MenuItem("Nicht zugewiesen");
            emptyItem.setDisable(true);
            menu.getItems().add(emptyItem);
        }

        menu.show(this, screenX, screenY);
    }
}

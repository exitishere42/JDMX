package de.exit.jdmx.ui.stage;

import de.exit.jdmx.ui.icon.LucideIcon;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

import java.util.function.Consumer;

/**
 * Schwebende Werkzeugleiste für die 2D-Bühne im Figma-Toolbar-Stil.
 * <p>
 * Oben mittig über dem Canvas platziert mit:
 * <ul>
 *     <li>[ + ] Button zum Hinzufügen / Importieren von Scheinwerfern</li>
 *     <li>Figma-Style Canvas-Werkzeugen (Auswahl, Gitter/Grid, Lichtkegel)</li>
 *     <li>Einklapp- & Ausklapp-Mechanik für uneingeschränkte Sicht auf die Traverse</li>
 * </ul>
 * </p>
 */
public class StageFloatingToolbar extends HBox {

    private static final Color ICON_COLOR = Color.web("#D1D5DB");
    private static final Color ACCENT_COLOR = Color.web("#38BDF8");
    private static final Color ACTIVE_ICON_COLOR = Color.WHITE;

    private final Button btnAddFixture;
    private final Button btnSelect;
    private final Button btnGrid;
    private final Button btnBeams;
    private final Button btnCollapse;
    private final Button btnExpand;

    private final Node divider1;
    private final Node divider2;

    private boolean collapsed = false;
    private boolean gridActive = false;
    private boolean beamsActive = true;

    private Runnable onAddFixture;
    private Consumer<Boolean> onToggleGrid;
    private Consumer<Boolean> onToggleBeams;

    public StageFloatingToolbar() {
        super(4);
        setAlignment(Pos.CENTER);
        getStyleClass().add("stage-floating-toolbar");
        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        setPrefHeight(40);

        // 1. Plus-Button (Scheinwerfer patchen / importieren)
        this.btnAddFixture = createToolbarButton("plus", 16, ACCENT_COLOR, "Scheinwerfer hinzufügen");
        btnAddFixture.getStyleClass().add("btn-pill-add");
        btnAddFixture.setOnAction(e -> {
            if (onAddFixture != null) {
                onAddFixture.run();
            }
        });

        this.divider1 = createDivider();

        // 2. Canvas-Tools (Figma-Stil)
        this.btnSelect = createToolbarButton("mouse-pointer-2", 15, ACTIVE_ICON_COLOR, "Auswahlwerkzeug");
        btnSelect.getStyleClass().add("active");

        this.btnGrid = createToolbarButton("grid-3x3", 15, ICON_COLOR, "Raster anzeigen");
        btnGrid.setOnAction(e -> {
            gridActive = !gridActive;
            updateToggleState(btnGrid, gridActive, "grid-3x3");
            if (onToggleGrid != null) {
                onToggleGrid.accept(gridActive);
            }
        });

        this.btnBeams = createToolbarButton("sun-dim", 15, ACTIVE_ICON_COLOR, "Lichtkegel anzeigen");
        updateToggleState(btnBeams, beamsActive, "sun-dim");
        btnBeams.setOnAction(e -> {
            beamsActive = !beamsActive;
            updateToggleState(btnBeams, beamsActive, "sun-dim");
            if (onToggleBeams != null) {
                onToggleBeams.accept(beamsActive);
            }
        });

        this.divider2 = createDivider();

        // 3. Einklapp- & Ausklapp-Buttons
        this.btnCollapse = createToolbarButton("chevron-up", 15, Color.web("#9CA3AF"), "Werkzeugleiste einklappen");
        btnCollapse.setOnAction(e -> collapse());

        this.btnExpand = createToolbarButton("chevron-down", 15, Color.web("#9CA3AF"), "Werkzeugleiste ausklappen");
        btnExpand.setOnAction(e -> expand());

        showExpandedLayout();
    }

    private Button createToolbarButton(String iconName, double size, Color color, String tooltipText) {
        Button btn = new Button();
        btn.getStyleClass().add("pill-toolbar-btn");
        btn.setMinSize(32, 32);
        btn.setPrefSize(32, 32);
        btn.setMaxSize(32, 32);
        btn.setGraphic(new LucideIcon(iconName, size, color));
        btn.setTooltip(new Tooltip(tooltipText));
        return btn;
    }

    private Node createDivider() {
        Separator sep = new Separator(Orientation.VERTICAL);
        sep.getStyleClass().add("pill-toolbar-divider");
        sep.setMinHeight(20);
        sep.setPrefHeight(20);
        sep.setMaxHeight(20);
        return sep;
    }

    private void updateToggleState(Button btn, boolean active, String iconName) {
        if (active) {
            if (!btn.getStyleClass().contains("active")) {
                btn.getStyleClass().add("active");
            }
            if (btn.getGraphic() instanceof LucideIcon icon) {
                icon.setColor(ACTIVE_ICON_COLOR);
            }
        } else {
            btn.getStyleClass().remove("active");
            if (btn.getGraphic() instanceof LucideIcon icon) {
                icon.setColor(ICON_COLOR);
            }
        }
    }

    /**
     * Klappt die Pill-Toolbar auf einen kompakten Mini-Button zusammen.
     */
    public void collapse() {
        if (!collapsed) {
            collapsed = true;
            getStyleClass().add("collapsed");
            showCollapsedLayout();
        }
    }

    /**
     * Klappt die Pill-Toolbar wieder auf die volle Breite mit allen Werkzeugen aus.
     */
    public void expand() {
        if (collapsed) {
            collapsed = false;
            getStyleClass().remove("collapsed");
            showExpandedLayout();
        }
    }

    /**
     * Schaltet zwischen ein- und ausgeklapptem Zustand um.
     */
    public void toggleCollapse() {
        if (collapsed) {
            expand();
        } else {
            collapse();
        }
    }

    private void showExpandedLayout() {
        getChildren().setAll(btnAddFixture, divider1, btnSelect, btnGrid, btnBeams, divider2, btnCollapse);
    }

    private void showCollapsedLayout() {
        getChildren().setAll(btnAddFixture, divider1, btnExpand);
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public Button getBtnAddFixture() {
        return btnAddFixture;
    }

    public Button getBtnSelect() {
        return btnSelect;
    }

    public Button getBtnGrid() {
        return btnGrid;
    }

    public Button getBtnBeams() {
        return btnBeams;
    }

    public Button getBtnCollapse() {
        return btnCollapse;
    }

    public Button getBtnExpand() {
        return btnExpand;
    }

    public void setOnAddFixture(Runnable onAddFixture) {
        this.onAddFixture = onAddFixture;
    }

    public void setOnToggleGrid(Consumer<Boolean> onToggleGrid) {
        this.onToggleGrid = onToggleGrid;
    }

    public void setOnToggleBeams(Consumer<Boolean> onToggleBeams) {
        this.onToggleBeams = onToggleBeams;
    }
}

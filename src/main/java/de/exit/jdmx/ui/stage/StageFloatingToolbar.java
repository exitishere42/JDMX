package de.exit.jdmx.ui.stage;

import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.theme.StudioTheme;
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
 * Schwebende Werkzeugleiste für die 2D-Bühne im JDMX Studio Dark Design.
 * <p>
 * Oben mittig über dem Canvas platziert mit:
 * <ul>
 *     <li>[ + ] Button zum Hinzufügen / Importieren von Scheinwerfern (Stage-Cyan)</li>
 *     <li>Canvas-Werkzeugen (Auswahl, Gitter/Grid, Lichtkegel/Beams) in Studio-Farben</li>
 *     <li>Einklapp- & Ausklapp-Mechanik mit 4px Radius passend zum Gesamtsystem</li>
 * </ul>
 * </p>
 */
public class StageFloatingToolbar extends HBox {

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
        setPrefHeight(36);

        // 1. Plus-Button (Scheinwerfer patchen / importieren) in JDMX Stage-Cyan
        this.btnAddFixture = createToolbarButton("plus", 14, StudioTheme.COLOR_ICON_STAGE, "Scheinwerfer hinzufügen");
        btnAddFixture.getStyleClass().add("btn-pill-add");
        btnAddFixture.setOnAction(e -> {
            if (onAddFixture != null) {
                onAddFixture.run();
            }
        });

        this.divider1 = createDivider();

        // 2. Canvas-Tools in JDMX Studio Farben
        this.btnSelect = createToolbarButton("mouse-pointer-2", 14, StudioTheme.COLOR_TEXT_HIGH, "Auswahlwerkzeug");
        btnSelect.getStyleClass().add("active");

        this.btnGrid = createToolbarButton("grid-3x3", 14, StudioTheme.COLOR_TEXT_MED, "Raster anzeigen");
        btnGrid.setOnAction(e -> {
            gridActive = !gridActive;
            updateToggleState(btnGrid, gridActive, StudioTheme.COLOR_ICON_STAGE, StudioTheme.COLOR_TEXT_MED);
            if (onToggleGrid != null) {
                onToggleGrid.accept(gridActive);
            }
        });

        this.btnBeams = createToolbarButton("sun-dim", 14, StudioTheme.COLOR_ICON_FADER, "Lichtkegel anzeigen");
        updateToggleState(btnBeams, beamsActive, StudioTheme.COLOR_ICON_FADER, StudioTheme.COLOR_TEXT_LOW);
        btnBeams.setOnAction(e -> {
            beamsActive = !beamsActive;
            updateToggleState(btnBeams, beamsActive, StudioTheme.COLOR_ICON_FADER, StudioTheme.COLOR_TEXT_LOW);
            if (onToggleBeams != null) {
                onToggleBeams.accept(beamsActive);
            }
        });

        this.divider2 = createDivider();

        // 3. Einklapp- & Ausklapp-Buttons
        this.btnCollapse = createToolbarButton("chevron-up", 14, StudioTheme.COLOR_TEXT_MED, "Werkzeugleiste einklappen");
        btnCollapse.setOnAction(e -> collapse());

        this.btnExpand = createToolbarButton("chevron-down", 14, StudioTheme.COLOR_TEXT_MED, "Werkzeugleiste ausklappen");
        btnExpand.setOnAction(e -> expand());

        showExpandedLayout();
    }

    private Button createToolbarButton(String iconName, double size, Color color, String tooltipText) {
        Button btn = new Button();
        btn.getStyleClass().add("pill-toolbar-btn");
        btn.setMinSize(28, 28);
        btn.setPrefSize(28, 28);
        btn.setMaxSize(28, 28);
        btn.setGraphic(new LucideIcon(iconName, size, color));
        btn.setTooltip(new Tooltip(tooltipText));
        return btn;
    }

    private Node createDivider() {
        Separator sep = new Separator(Orientation.VERTICAL);
        sep.getStyleClass().add("pill-toolbar-divider");
        sep.setMinHeight(16);
        sep.setPrefHeight(16);
        sep.setMaxHeight(16);
        return sep;
    }

    private void updateToggleState(Button btn, boolean active, Color activeColor, Color inactiveColor) {
        if (active) {
            if (!btn.getStyleClass().contains("active")) {
                btn.getStyleClass().add("active");
            }
            if (btn.getGraphic() instanceof LucideIcon icon) {
                icon.setColor(activeColor);
            }
        } else {
            btn.getStyleClass().remove("active");
            if (btn.getGraphic() instanceof LucideIcon icon) {
                icon.setColor(inactiveColor);
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

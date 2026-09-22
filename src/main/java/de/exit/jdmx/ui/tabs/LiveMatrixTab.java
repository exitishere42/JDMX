package de.exit.jdmx.ui.tabs;

import de.exit.jdmx.ui.icons.LucideIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;

import java.util.ArrayList;
import java.util.List;

/**
 * Tab 3: Live Matrix / Virtual Console (3x16 Pad Desk).
 * - Exactly 3 rows x 16 columns = 48 pads per page (Pages 1 to 4).
 * - Pads are strictly SQUARE buttons.
 * - Empty slots feature a fine dashed border.
 * - Assigned slots feature flat solid colors.
 * - Flash pads display a small Lucide star icon in the top-left corner.
 * - Left sidebar with action button, tools, and color palette.
 * - Styled according to artnet2dmx Material Dark theme.
 */
public class LiveMatrixTab extends VBox {

    private static final int COLS = 16;
    private static final int ROWS = 3;
    private static final int TOTAL_PADS = COLS * ROWS; // 48
    private static final int TOTAL_PAGES = 4;

    private int currentPage = 1;
    private boolean isEditMode = false;
    private String currentTool = "flash"; // "flash", "toggle", "color", "clear"
    private Color currentColor = Color.web("#ffff00");

    /**
     * Data structure representing the state of a single pad.
     */
    public static class PadData {
        public boolean isEmpty = true;
        public boolean isFlash = false;
        public Color color = Color.web("#ffff00");
        public boolean active = false;
    }

    private final List<List<PadData>> pages = new ArrayList<>();
    private final GridPane matrixGrid = new GridPane();
    private final Label modeBadge = new Label("LIVE");
    private final Label pageBadge = new Label("1 / 4");
    private final Label statFlashLabel = new Label("0");
    private final Label statActiveLabel = new Label("0");
    private final Button editToggleButton = new Button("Bearbeiten");

    /**
     * Constructs the LiveMatrixTab.
     */
    public LiveMatrixTab() {
        getStyleClass().add("matrix-workspace");
        setSpacing(10);
        VBox.setVgrow(this, Priority.ALWAYS);

        initPagesData();
        buildUI();
        renderGrid();
        updateStats();
    }

    /**
     * Initializes dummy data for all 4 pages.
     */
    private void initPagesData() {
        for (int p = 0; p < TOTAL_PAGES; p++) {
            List<PadData> page = new ArrayList<>();
            for (int i = 0; i < TOTAL_PADS; i++) {
                PadData pad = new PadData();
                if (p == 0) {
                    if (i == 0 || i == 1) {
                        pad.isEmpty = false;
                        pad.color = Color.web("#ffff00"); 
                        pad.isFlash = false;
                    } else if (i == 3) {
                        pad.isEmpty = false;
                        pad.color = Color.web("#ffff00"); 
                        pad.isFlash = true;
                    } else if (i == 7) {
                        pad.isEmpty = false;
                        pad.color = Color.web("#ff453a"); 
                        pad.isFlash = true;
                    } else if (i == 11) {
                        pad.isEmpty = false;
                        pad.color = Color.web("#ff9f0a"); 
                        pad.isFlash = false;
                    } else if (i == 15) {
                        pad.isEmpty = false;
                        pad.color = Color.web("#30d158"); 
                        pad.isFlash = true;
                    }
                }
                page.add(pad);
            }
            pages.add(page);
        }
    }

    /**
     * Builds the main UI components of the tab.
     */
    private void buildUI() {
        // --- 1. Top Bar: Mode, Page Navigator, Deactivate All ---
        HBox topBar = new HBox(12);
        topBar.getStyleClass().add("matrix-top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Live Virtual Console");
        title.getStyleClass().add("jdmx-section-title");

        HBox modePill = createPill("Modus:", modeBadge);
        HBox pagePill = createPill("Seite:", pageBadge);

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        // Page Navigator
        HBox pageNav = new HBox(4);
        pageNav.setAlignment(Pos.CENTER);
        pageNav.getStyleClass().add("page-nav");

        Button prevBtn = new Button();
        prevBtn.setGraphic(LucideIcon.create(LucideIcon.CHEVRON_LEFT, LucideIcon.COLOR_HANDLE, 0.7));
        prevBtn.getStyleClass().add("page-nav-btn");
        prevBtn.setOnAction(e -> {
            if (currentPage > 1) {
                currentPage--;
                renderGrid();
                updateStats();
            }
        });

        HBox pageBtnsBox = new HBox(4);
        for (int i = 1; i <= TOTAL_PAGES; i++) {
            final int pageNum = i;
            Button pBtn = new Button(String.valueOf(pageNum));
            pBtn.setPrefSize(32, 28);
            updatePageButtonStyle(pBtn, pageNum == currentPage);
            pBtn.setOnAction(e -> {
                currentPage = pageNum;
                for (var node : pageBtnsBox.getChildren()) {
                    if (node instanceof Button b) {
                        updatePageButtonStyle(b, b.getText().equals(String.valueOf(currentPage)));
                    }
                }
                renderGrid();
                updateStats();
            });
            pageBtnsBox.getChildren().add(pBtn);
        }

        Button nextBtn = new Button();
        nextBtn.setGraphic(LucideIcon.create(LucideIcon.CHEVRON_RIGHT, LucideIcon.COLOR_HANDLE, 0.7));
        nextBtn.getStyleClass().add("page-nav-btn");
        nextBtn.setOnAction(e -> {
            if (currentPage < TOTAL_PAGES) {
                currentPage++;
                renderGrid();
                updateStats();
            }
        });

        pageNav.getChildren().addAll(prevBtn, pageBtnsBox, nextBtn);

        Button deactivateAllBtn = new Button("Alle deaktivieren");
        deactivateAllBtn.setGraphic(LucideIcon.create(LucideIcon.SQUARE_X, LucideIcon.COLOR_HANDLE, 0.6));
        deactivateAllBtn.getStyleClass().add("btn-blackout");
        deactivateAllBtn.setOnAction(e -> deactivateAllPads());

        topBar.getChildren().addAll(title, modePill, pagePill, topSpacer, pageNav, deactivateAllBtn);

        // --- 2. Main Workspace: Sidebar + Matrix Grid ---
        HBox mainArea = new HBox(12);
        VBox.setVgrow(mainArea, Priority.ALWAYS);

        VBox sidebar = buildSidebar();
        VBox desk = buildDesk();
        HBox.setHgrow(desk, Priority.ALWAYS);

        mainArea.getChildren().addAll(sidebar, desk);

        getChildren().addAll(topBar, mainArea);
    }

    /**
     * Updates the styling of page buttons based on active state.
     *
     * @param btn      The button to style.
     * @param isActive True if active, false otherwise.
     */
    private void updatePageButtonStyle(Button btn, boolean isActive) {
        btn.getStyleClass().removeAll("page-btn-active", "page-btn");
        if (isActive) {
            btn.getStyleClass().add("page-btn-active");
        } else {
            btn.getStyleClass().add("page-btn");
        }
    }

    /**
     * Builds the sidebar with tools and color palette.
     *
     * @return VBox containing sidebar elements.
     */
    private VBox buildSidebar() {
        VBox sidebar = new VBox(14);
        sidebar.getStyleClass().add("matrix-sidebar");
        sidebar.setPrefWidth(220);
        sidebar.setMinWidth(220);

        // Big Action Button
        editToggleButton.getStyleClass().add("btn-jdmx-action");
        editToggleButton.setMaxWidth(Double.MAX_VALUE);
        editToggleButton.setGraphic(LucideIcon.create(LucideIcon.EDIT, Color.WHITE, 0.8));
        editToggleButton.setOnAction(e -> {
            isEditMode = !isEditMode;
            if (isEditMode) {
                editToggleButton.getStyleClass().add("active");
                editToggleButton.setText("Modus: EDIT");
                modeBadge.setText("EDIT");
                modeBadge.setStyle("-fx-text-fill: -jdmx-primary; -fx-font-weight: bold;");
            } else {
                editToggleButton.getStyleClass().remove("active");
                editToggleButton.setText("Bearbeiten");
                modeBadge.setText("LIVE");
                modeBadge.setStyle("-fx-text-fill: -jdmx-text-high; -fx-font-weight: bold;");
            }
        });

        // Tools Section
        Label toolsLabel = new Label("WERKZEUGE");
        toolsLabel.getStyleClass().add("sidebar-section-title");

        VBox toolList = new VBox(6);

        Button toolFlash = createToolButton("Flash Tool", LucideIcon.STAR, Color.web("#facc15"), "flash");
        Button toolToggle = createToolButton("Toggle Tool", LucideIcon.TOGGLE, LucideIcon.COLOR_HANDLE, "toggle");
        Button toolColor = createToolButton("Farbe zuweisen", LucideIcon.PALETTE, LucideIcon.COLOR_HANDLE, "color");
        Button toolClear = createToolButton("Slot leeren", LucideIcon.ERASER, LucideIcon.COLOR_HANDLE, "clear");

        toolList.getChildren().addAll(toolFlash, toolToggle, toolColor, toolClear);

        // Color Palette
        Label palLabel = new Label("AKZENTFARBE");
        palLabel.getStyleClass().add("sidebar-section-title");

        HBox colorPalette = new HBox(6);
        colorPalette.setAlignment(Pos.CENTER_LEFT);

        Color[] colors = new Color[] {
            Color.web("#ffff00"), // Yellow
            Color.web("#30d158"), // Green
            Color.web("#ff453a"), // Red
            Color.web("#ff9f0a"), // Peach
            Color.web("#03DAC6"), // Teal Primary
            Color.web("#bf5af2"), // Purple
            Color.web("#ffffff")  // White
        };

        for (Color c : colors) {
            Circle swatch = new Circle(11, c);
            swatch.getStyleClass().add("swatch-circle");
            swatch.setStroke(Color.web("#2C2C2C"));
            swatch.setStrokeWidth(1.5);
            swatch.setOnMouseClicked(e -> {
                currentColor = c;
                for (var node : colorPalette.getChildren()) {
                    if (node instanceof Circle s) {
                        s.getStyleClass().remove("selected");
                        s.setStroke(Color.web("#2C2C2C"));
                    }
                }
                swatch.getStyleClass().add("selected");
                swatch.setStroke(Color.WHITE);
            });
            colorPalette.getChildren().add(swatch);
        }

        // Stats section at bottom
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox statsBox = new VBox(4);
        statsBox.getStyleClass().add("stats-box");
        HBox stat1 = createPill("Flash-Pads:", statFlashLabel);
        HBox stat2 = createPill("Aktive Pads:", statActiveLabel);
        statsBox.getChildren().addAll(stat1, stat2);

        sidebar.getChildren().addAll(editToggleButton, toolsLabel, toolList, palLabel, colorPalette, spacer, statsBox);
        return sidebar;
    }

    /**
     * Creates a tool button.
     *
     * @param text      The text for the tool.
     * @param iconPath  The SVG path for the tool icon.
     * @param iconColor The color of the icon.
     * @param toolId    The unique ID for the tool.
     * @return Button for the tool.
     */
    private Button createToolButton(String text, String iconPath, Color iconColor, String toolId) {
        Button btn = new Button(text);
        btn.getStyleClass().add("tool-btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setGraphic(LucideIcon.create(iconPath, iconColor, 0.7));
        if (toolId.equals(currentTool)) btn.getStyleClass().add("active");

        btn.setOnAction(e -> {
            currentTool = toolId;
            VBox parent = (VBox) btn.getParent();
            for (var node : parent.getChildren()) {
                if (node instanceof Button b) b.getStyleClass().remove("active");
            }
            btn.getStyleClass().add("active");
        });
        return btn;
    }

    /**
     * Builds the central matrix desk with strictly SQUARE pads.
     *
     * @return VBox representing the desk.
     */
    private VBox buildDesk() {
        VBox desk = new VBox();
        desk.getStyleClass().add("matrix-desk");
        VBox.setVgrow(desk, Priority.ALWAYS);

        matrixGrid.setHgap(8);
        matrixGrid.setVgap(8);
        matrixGrid.setAlignment(Pos.CENTER);
        VBox.setVgrow(matrixGrid, Priority.ALWAYS);

        // Define 16 columns (each taking equal width)
        for (int c = 0; c < COLS; c++) {
            ColumnConstraints colConst = new ColumnConstraints();
            colConst.setPercentWidth(100.0 / COLS);
            colConst.setHgrow(Priority.ALWAYS);
            matrixGrid.getColumnConstraints().add(colConst);
        }

        // Define 3 rows
        for (int r = 0; r < ROWS; r++) {
            RowConstraints rowConst = new RowConstraints();
            rowConst.setVgrow(Priority.ALWAYS);
            matrixGrid.getRowConstraints().add(rowConst);
        }

        desk.getChildren().add(matrixGrid);
        return desk;
    }

    /**
     * Renders the matrix grid for the current page with strictly SQUARE pads.
     */
    private void renderGrid() {
        matrixGrid.getChildren().clear();
        List<PadData> currentPads = pages.get(currentPage - 1);

        for (int idx = 0; idx < TOTAL_PADS; idx++) {
            final PadData data = currentPads.get(idx);
            int col = idx % COLS;
            int row = idx / COLS;

            StackPane padPane = new StackPane();
            
            // Enforce strictly SQUARE aspect ratio for pad buttons
            padPane.minWidthProperty().bind(padPane.heightProperty());
            padPane.maxWidthProperty().bind(padPane.heightProperty());
            padPane.prefWidthProperty().bind(padPane.heightProperty());

            applyPadStyle(padPane, data);

            // Live interaction: Momentary for Flash pads, Latching for Toggle pads
            padPane.setOnMousePressed(e -> {
                if (isEditMode || data.isEmpty) return;
                if (data.isFlash) {
                    data.active = true;
                    applyPadStyle(padPane, data);
                    updateStats();
                }
            });

            padPane.setOnMouseReleased(e -> {
                if (isEditMode || data.isEmpty) return;
                if (data.isFlash && data.active) {
                    data.active = false;
                    applyPadStyle(padPane, data);
                    updateStats();
                }
            });

            padPane.setOnMouseClicked(e -> {
                if (isEditMode) {
                    handleEditTool(data, padPane);
                } else {
                    if (!data.isEmpty && !data.isFlash) {
                        data.active = !data.active;
                        applyPadStyle(padPane, data);
                        updateStats();
                    }
                }
            });

            matrixGrid.add(padPane, col, row);
        }

        pageBadge.setText(currentPage + " / " + TOTAL_PAGES);
    }

    /**
     * Applies the appropriate style and children elements to a pad based on its state.
     *
     * @param padPane The StackPane representing the pad.
     * @param data    The state data for the pad.
     */
    private void applyPadStyle(StackPane padPane, PadData data) {
        padPane.getChildren().clear();

        if (data.isEmpty) {
            padPane.getStyleClass().setAll("matrix-pad-empty");
            padPane.setStyle("");
        } else {
            padPane.getStyleClass().setAll("matrix-pad-assigned");
            if (data.active) padPane.getStyleClass().add("active");

            String hex = toHexString(data.color);
            padPane.setStyle("-fx-background-color: " + hex + ";");

            // Automatic WCAG contrast for star icon
            boolean isLight = isColorLight(data.color);
            Color starColor = isLight ? Color.BLACK : Color.WHITE;

            // Flash star top-left
            if (data.isFlash) {
                SVGPath star = LucideIcon.createStar(starColor, 0.65);
                StackPane.setAlignment(star, Pos.TOP_LEFT);
                StackPane.setMargin(star, new Insets(6, 0, 0, 6));
                padPane.getChildren().add(star);
            }

            // LED indicator top-right
            Circle led = new Circle(3);
            led.getStyleClass().add("pad-led");
            if (data.active) led.getStyleClass().add("active");
            StackPane.setAlignment(led, Pos.TOP_RIGHT);
            StackPane.setMargin(led, new Insets(6, 6, 0, 0));
            padPane.getChildren().add(led);
        }
    }

    /**
     * Handles clicking a pad with an edit tool.
     *
     * @param data    The data for the pad.
     * @param padPane The pad's UI element.
     */
    private void handleEditTool(PadData data, StackPane padPane) {
        switch (currentTool) {
            case "flash" -> {
                if (data.isEmpty) {
                    data.isEmpty = false;
                    data.color = currentColor;
                    data.isFlash = true;
                } else {
                    data.isFlash = !data.isFlash;
                }
            }
            case "toggle" -> {
                if (data.isEmpty) {
                    data.isEmpty = false;
                    data.color = currentColor;
                    data.isFlash = false;
                } else {
                    data.isFlash = false;
                }
            }
            case "color" -> {
                data.isEmpty = false;
                data.color = currentColor;
            }
            case "clear" -> {
                data.isEmpty = true;
                data.isFlash = false;
                data.active = false;
            }
        }
        applyPadStyle(padPane, data);
        updateStats();
    }

    /**
     * Deactivates all active pads across all pages.
     */
    public void deactivateAllPads() {
        for (List<PadData> page : pages) {
            for (PadData pad : page) {
                pad.active = false;
            }
        }
        renderGrid();
        updateStats();
    }

    /**
     * Updates the stat labels for flash and active pads.
     */
    private void updateStats() {
        int flashCount = 0;
        int activeCount = 0;
        List<PadData> currentPads = pages.get(currentPage - 1);
        for (PadData pad : currentPads) {
            if (!pad.isEmpty && pad.isFlash) flashCount++;
            if (!pad.isEmpty && pad.active) activeCount++;
        }
        statFlashLabel.setText(String.valueOf(flashCount));
        statActiveLabel.setText(String.valueOf(activeCount));
    }

    /**
     * Creates a pill indicator.
     *
     * @param labelText  The label string.
     * @param valueLabel The value label to update.
     * @return HBox representing the pill.
     */
    private HBox createPill(String labelText, Label valueLabel) {
        HBox pill = new HBox(6);
        pill.getStyleClass().add("jdmx-pill");
        pill.setAlignment(Pos.CENTER);
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("jdmx-pill-label");
        valueLabel.getStyleClass().add("jdmx-pill-value");
        pill.getChildren().addAll(lbl, valueLabel);
        return pill;
    }

    /**
     * Determines if a color is considered light for contrast purposes.
     *
     * @param c The color to evaluate.
     * @return True if light, false otherwise.
     */
    private boolean isColorLight(Color c) {
        double brightness = (c.getRed() * 299 + c.getGreen() * 587 + c.getBlue() * 114) / 1000;
        return brightness > 0.6;
    }

    /**
     * Converts a Color to a hex string.
     *
     * @param c The color to convert.
     * @return Hex string representation.
     */
    private String toHexString(Color c) {
        return String.format("#%02X%02X%02X",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }
}

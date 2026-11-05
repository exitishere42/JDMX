package de.exit.jdmx.ui.dialog;

import de.exit.jdmx.core.model.*;
import de.exit.jdmx.service.FixtureLibraryService;
import de.exit.jdmx.service.ProjectExplorerService;
import de.exit.jdmx.ui.MainWindow;
import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.stage.StageCanvasView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * In-App Modal Dialog für das Importieren und Patchen von Scheinwerfern (QLC+ .qxf & GDTF .gdtf).
 * <p>
 * 3-Schritte Workflow:
 * <ul>
 *     <li>1. Hersteller- und Modell-Auswahl mit Durchsuchen und Direkt-Dateiimport</li>
 *     <li>2. DMX-Kanalmodus, Startadresse und Anzahl</li>
 *     <li>3. Automatisches Patchen in den Project Explorer und Spawning auf der 2D-Bühne</li>
 * </ul>
 * </p>
 */
public class FixturePatchDialog {

    private final MainWindow mainWindow;
    private final FixtureLibraryService libraryService;
    private final ProjectExplorerService explorerService;
    private final StageCanvasView stageCanvas;

    private final VBox card;

    private TextField searchField;
    private ComboBox<String> comboManufacturer;
    private ComboBox<FixtureDefinition> comboModel;
    private ComboBox<DmxMode> comboMode;

    private Spinner<Integer> spinnerUniverse;
    private Spinner<Integer> spinnerAddress;
    private Spinner<Integer> spinnerQuantity;

    private Button btnImportFile;
    private Button btnCancel;
    private Button btnPatch;

    /**
     * Öffnet den Fixture-Patch-Dialog im modalen Overlay des Hauptfensters.
     */
    public static void show(MainWindow mainWindow, FixtureLibraryService libraryService, ProjectExplorerService explorerService, StageCanvasView stageCanvas) {
        FixturePatchDialog dialog = new FixturePatchDialog(mainWindow, libraryService, explorerService, stageCanvas);
        mainWindow.showOverlay(dialog.getView());
    }

    public FixturePatchDialog(MainWindow mainWindow, FixtureLibraryService libraryService, ProjectExplorerService explorerService, StageCanvasView stageCanvas) {
        this.mainWindow = mainWindow;
        this.libraryService = Objects.requireNonNull(libraryService, "libraryService must not be null");
        this.explorerService = Objects.requireNonNull(explorerService, "explorerService must not be null");
        this.stageCanvas = Objects.requireNonNull(stageCanvas, "stageCanvas must not be null");

        this.card = new VBox(12);
        card.getStyleClass().add("settings-dialog-card");
        card.setMinWidth(480);
        card.setPrefWidth(520);
        card.setMaxWidth(540);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        buildUi();
    }

    private void buildUi() {
        // 1. Header
        HBox header = buildHeader();

        // 2. Formular-Raster
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 0, 10, 0));

        // Suchfeld & Import-Button oben
        this.searchField = new TextField();
        searchField.setPromptText("Hersteller oder Modell suchen...");
        searchField.getStyleClass().add("settings-input");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> onSearchChanged(newVal));

        this.btnImportFile = new Button("Datei importieren...");
        btnImportFile.getStyleClass().add("settings-btn-secondary");
        btnImportFile.setGraphic(new LucideIcon("folder-open", 14, Color.web("#D1D5DB")));
        btnImportFile.setOnAction(e -> onImportFileClicked());

        HBox searchRow = new HBox(8, searchField, btnImportFile);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        grid.add(searchRow, 0, 0, 2, 1);

        // Hersteller
        Label lblMfg = new Label("Hersteller:");
        lblMfg.getStyleClass().add("settings-label");
        this.comboManufacturer = new ComboBox<>();
        comboManufacturer.getStyleClass().add("settings-combo");
        comboManufacturer.setMaxWidth(Double.MAX_VALUE);
        comboManufacturer.getItems().setAll(libraryService.getManufacturers());
        comboManufacturer.valueProperty().addListener((obs, oldV, newV) -> onManufacturerSelected(newV));
        grid.add(lblMfg, 0, 1);
        grid.add(comboManufacturer, 1, 1);

        // Modell
        Label lblModel = new Label("Modell:");
        lblModel.getStyleClass().add("settings-label");
        this.comboModel = new ComboBox<>();
        comboModel.getStyleClass().add("settings-combo");
        comboModel.setMaxWidth(Double.MAX_VALUE);
        comboModel.valueProperty().addListener((obs, oldV, newV) -> onModelSelected(newV));
        grid.add(lblModel, 0, 2);
        grid.add(comboModel, 1, 2);

        // DMX Modus
        Label lblMode = new Label("DMX-Modus:");
        lblMode.getStyleClass().add("settings-label");
        this.comboMode = new ComboBox<>();
        comboMode.getStyleClass().add("settings-combo");
        comboMode.setMaxWidth(Double.MAX_VALUE);
        grid.add(lblMode, 0, 3);
        grid.add(comboMode, 1, 3);

        // Universum & Startadresse
        Label lblAddr = new Label("Startadresse:");
        lblAddr.getStyleClass().add("settings-label");

        this.spinnerUniverse = new Spinner<>(1, 16, 1);
        spinnerUniverse.setEditable(true);
        spinnerUniverse.setPrefWidth(80);

        this.spinnerAddress = new Spinner<>(1, 512, 1);
        spinnerAddress.setEditable(true);
        spinnerAddress.setPrefWidth(100);

        HBox addrBox = new HBox(8, new Label("U:"), spinnerUniverse, new Label("Adr:"), spinnerAddress);
        addrBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(lblAddr, 0, 4);
        grid.add(addrBox, 1, 4);

        // Anzahl
        Label lblQty = new Label("Anzahl:");
        lblQty.getStyleClass().add("settings-label");
        this.spinnerQuantity = new Spinner<>(1, 32, 1);
        spinnerQuantity.setEditable(true);
        spinnerQuantity.setPrefWidth(100);
        grid.add(lblQty, 0, 5);
        grid.add(spinnerQuantity, 1, 5);

        // Standardmäßig ersten Hersteller selektieren
        if (!comboManufacturer.getItems().isEmpty()) {
            comboManufacturer.getSelectionModel().selectFirst();
            onManufacturerSelected(comboManufacturer.getValue());
        }

        // 3. Footer
        HBox footer = buildFooter();

        card.getChildren().addAll(header, grid, footer);
    }

    private HBox buildHeader() {
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("settings-dialog-header");

        LucideIcon icon = new LucideIcon("plus", 18, Color.web("#60A5FA"));
        Label title = new Label("Scheinwerfer hinzufügen");
        title.getStyleClass().add("settings-dialog-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnClose = new Button();
        btnClose.getStyleClass().add("dialog-close-btn");
        btnClose.setGraphic(new LucideIcon("x", 14, Color.web("#9CA3AF")));
        btnClose.setOnAction(e -> close());

        header.getChildren().addAll(icon, title, spacer, btnClose);
        return header;
    }

    private HBox buildFooter() {
        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.getStyleClass().add("settings-dialog-footer");

        this.btnCancel = new Button("Abbrechen");
        btnCancel.getStyleClass().add("settings-btn-secondary");
        btnCancel.setOnAction(e -> close());

        this.btnPatch = new Button("Scheinwerfer patchen");
        btnPatch.getStyleClass().add("settings-btn-primary");
        btnPatch.setGraphic(new LucideIcon("plus", 14, Color.WHITE));
        btnPatch.setOnAction(e -> executePatch());

        footer.getChildren().addAll(btnCancel, btnPatch);
        return footer;
    }

    private void onSearchChanged(String query) {
        if (query == null || query.isBlank()) {
            comboManufacturer.getItems().setAll(libraryService.getManufacturers());
            if (!comboManufacturer.getItems().isEmpty()) {
                comboManufacturer.getSelectionModel().selectFirst();
            }
        } else {
            List<FixtureDefinition> results = libraryService.search(query);
            comboModel.getItems().setAll(results);
            if (!results.isEmpty()) {
                comboModel.getSelectionModel().selectFirst();
            }
        }
    }

    private void onManufacturerSelected(String mfg) {
        if (mfg != null) {
            List<FixtureDefinition> models = libraryService.getByManufacturer(mfg);
            comboModel.getItems().setAll(models);
            if (!models.isEmpty()) {
                comboModel.getSelectionModel().selectFirst();
            }
        }
    }

    private void onModelSelected(FixtureDefinition def) {
        if (def != null) {
            comboMode.getItems().setAll(def.getModes());
            if (!def.getModes().isEmpty()) {
                comboMode.getSelectionModel().selectFirst();
            }
        } else {
            comboMode.getItems().clear();
        }
    }

    private void onImportFileClicked() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Scheinwerfer-Profil importieren");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Unterstützte Profile (*.qxf, *.gdtf)", "*.qxf", "*.gdtf"),
                new FileChooser.ExtensionFilter("QLC+ Profile (*.qxf)", "*.qxf"),
                new FileChooser.ExtensionFilter("GDTF Profile (*.gdtf)", "*.gdtf")
        );

        File file = chooser.showOpenDialog(card.getScene() != null ? card.getScene().getWindow() : null);
        if (file != null) {
            try {
                FixtureDefinition imported = libraryService.importFixture(file);
                comboManufacturer.getItems().setAll(libraryService.getManufacturers());
                comboManufacturer.getSelectionModel().select(imported.getManufacturer());
                comboModel.getSelectionModel().select(imported);
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Fehler beim Importieren: " + ex.getMessage(), ButtonType.OK);
                alert.showAndWait();
            }
        }
    }

    private void executePatch() {
        FixtureDefinition def = comboModel.getValue();
        DmxMode mode = comboMode.getValue();
        if (def == null || mode == null) {
            return;
        }

        int universe = spinnerUniverse.getValue();
        int startAddr = spinnerAddress.getValue();
        int quantity = spinnerQuantity.getValue();

        for (int i = 0; i < quantity; i++) {
            int currentAddr = startAddr + (i * mode.getChannelCount());
            if (currentAddr > 512) {
                break;
            }

            String id = "fixture-" + UUID.randomUUID().toString().substring(0, 8);
            String name = def.getModel() + (quantity > 1 ? " " + (i + 1) : "");

            // 1. In Project Explorer eintragen (unter Geräte-Ordner)
            String iconName = def.getModel().toLowerCase().contains("dimmer") ? "sun" : "lamp";
            ExplorerItem item = new ExplorerItem(id, name, ExplorerItemType.FIXTURE, false, iconName, Color.web("#60A5FA"));
            explorerService.addItemToFolder("folder-devices", item);

            // 2. Auf 2D-Bühne platzieren (sauber nebeneinander entlang der Traverse)
            double startX = 180.0 + (i * 85.0);
            double startY = 160.0;
            stageCanvas.placeFixture(item, startX, startY);
        }

        close();
    }

    private void close() {
        if (mainWindow != null) {
            mainWindow.hideOverlay();
        }
    }

    public Node getView() {
        return card;
    }

    public TextField getSearchField() {
        return searchField;
    }

    public ComboBox<String> getComboManufacturer() {
        return comboManufacturer;
    }

    public ComboBox<FixtureDefinition> getComboModel() {
        return comboModel;
    }

    public ComboBox<DmxMode> getComboMode() {
        return comboMode;
    }

    public Spinner<Integer> getSpinnerUniverse() {
        return spinnerUniverse;
    }

    public Spinner<Integer> getSpinnerAddress() {
        return spinnerAddress;
    }

    public Spinner<Integer> getSpinnerQuantity() {
        return spinnerQuantity;
    }

    public Button getBtnImportFile() {
        return btnImportFile;
    }

    public Button getBtnPatch() {
        return btnPatch;
    }
}

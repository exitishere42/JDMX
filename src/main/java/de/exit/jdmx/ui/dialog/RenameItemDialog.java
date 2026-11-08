package de.exit.jdmx.ui.dialog;

import de.exit.jdmx.core.i18n.I18n;
import de.exit.jdmx.core.model.ExplorerItem;
import de.exit.jdmx.service.ProjectExplorerService;
import de.exit.jdmx.ui.MainWindow;
import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.theme.StudioTheme;
import de.exit.jdmx.ui.stage.StageCanvasView;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.Objects;

/**
 * Modaler In-App Dialog zum Umbenennen eines Elements (Fixture, Gruppe, Chaser etc.)
 * im Project Explorer ("Fenster im Fenster").
 * <p>
 * Erlaubt die Eingabe eines neuen Namens und synchronisiert die Änderung
 * direkt mit dem {@link ProjectExplorerService}, der 2D-Bühne ({@link StageCanvasView})
 * sowie dem automatischen Projekt-Speicherstand.
 * </p>
 */
public class RenameItemDialog {

    private final MainWindow mainWindow;
    private final ExplorerItem item;
    private final ProjectExplorerService explorerService;
    private final StageCanvasView stageCanvasView;

    private final VBox card;
    private TextField txtName;
    private Button btnCancel;
    private Button btnSave;

    /**
     * Öffnet den Umbenennen-Dialog als modales In-App Overlay.
     *
     * @param mainWindow      Das Hauptfenster
     * @param item            Das umzubenennde Element
     * @param explorerService Der Explorer-Service zur Datenverwaltung
     * @param stageCanvasView Die Bühnenansicht zur visuellen Aktualisierung
     */
    public static void show(MainWindow mainWindow,
                            ExplorerItem item,
                            ProjectExplorerService explorerService,
                            StageCanvasView stageCanvasView) {
        if (mainWindow == null || item == null || explorerService == null) {
            return;
        }
        RenameItemDialog dialog = new RenameItemDialog(mainWindow, item, explorerService, stageCanvasView);
        mainWindow.showOverlay(dialog.getView());
    }

    /**
     * Erstellt einen neuen Umbenennen-Dialog.
     *
     * @param mainWindow      Das Hauptfenster
     * @param item            Das umzubenennde Element
     * @param explorerService Der Explorer-Service
     * @param stageCanvasView Die Bühnenansicht
     */
    public RenameItemDialog(MainWindow mainWindow,
                            ExplorerItem item,
                            ProjectExplorerService explorerService,
                            StageCanvasView stageCanvasView) {
        this.mainWindow = mainWindow;
        this.item = Objects.requireNonNull(item, "item must not be null");
        this.explorerService = Objects.requireNonNull(explorerService, "explorerService must not be null");
        this.stageCanvasView = stageCanvasView;

        this.card = new VBox(14);
        card.getStyleClass().add("settings-dialog-card");
        card.setMinWidth(320);
        card.setPrefWidth(340);
        card.setMaxWidth(360);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        buildUi();
    }

    private void buildUi() {
        // 1. Header
        HBox header = buildHeader();

        // 2. Eingabefeld
        VBox inputSection = buildInputSection();

        // 3. Footer-Aktionen
        HBox footer = buildFooter();

        card.getChildren().addAll(header, inputSection, footer);

        card.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                executeSave();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                close();
                event.consume();
            }
        });
    }

    private HBox buildHeader() {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        LucideIcon icon = new LucideIcon("pencil", 18, Color.web("#38BDF8"));

        Label title = new Label(I18n.get("dialog.rename.title"));
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #F9FAFB;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnClose = new Button();
        btnClose.getStyleClass().add("dialog-close-button");
        btnClose.setGraphic(new LucideIcon("x", 14, StudioTheme.COLOR_ICON_RED));
        btnClose.setOnAction(e -> close());

        header.getChildren().addAll(icon, title, spacer, btnClose);
        return header;
    }

    private VBox buildInputSection() {
        VBox section = new VBox(6);

        Label lbl = new Label(I18n.get("dialog.rename.prompt"));
        lbl.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 12px; -fx-font-weight: bold;");

        txtName = new TextField(item.getName());
        txtName.getStyleClass().add("settings-textfield");
        txtName.setPrefHeight(32);

        txtName.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isBlank()) {
                txtName.setStyle("");
            }
        });

        // Automatisch fokussieren und Text markieren
        Platform.runLater(() -> {
            txtName.requestFocus();
            txtName.selectAll();
        });

        section.getChildren().addAll(lbl, txtName);
        return section;
    }

    private HBox buildFooter() {
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(6, 0, 0, 0));

        btnCancel = new Button(I18n.get("action.cancel"));
        btnCancel.getStyleClass().add("settings-btn-secondary");
        btnCancel.setPrefHeight(32);
        btnCancel.setOnAction(e -> close());

        btnSave = new Button(I18n.get("action.save"));
        btnSave.getStyleClass().add("settings-btn-primary");
        btnSave.setGraphic(new LucideIcon("check", 14, StudioTheme.COLOR_ICON_GREEN));
        btnSave.setPrefHeight(32);
        btnSave.setOnAction(e -> executeSave());

        footer.getChildren().addAll(btnCancel, btnSave);
        return footer;
    }

    private void executeSave() {
        String newName = txtName.getText() != null ? txtName.getText().trim() : "";
        if (newName.isBlank()) {
            txtName.setStyle("-fx-border-color: #EF4444;");
            return;
        }

        if (!newName.equals(item.getName())) {
            if (mainWindow != null) {
                mainWindow.recordUndoSnapshot();
            }
            explorerService.renameItem(item.getId(), newName);
            if (stageCanvasView != null) {
                stageCanvasView.updateFixtureName(item.getId(), newName);
            }
            if (mainWindow != null) {
                mainWindow.autoSave();
            }
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

    public TextField getTxtName() {
        return txtName;
    }

    public Button getBtnSave() {
        return btnSave;
    }

    public Button getBtnCancel() {
        return btnCancel;
    }
}

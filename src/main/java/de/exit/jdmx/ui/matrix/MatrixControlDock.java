package de.exit.jdmx.ui.matrix;

import de.exit.jdmx.core.model.MatrixButtonDefinition;
import de.exit.jdmx.core.model.MatrixTriggerMode;
import de.exit.jdmx.service.MatrixService;
import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.theme.StudioTheme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.Objects;

/**
 * Das untere 220px Steuerungs-Dock für Tab 3 (Button Matrix).
 * <p>
 * Zeigt bei Klick auf eine Button-Kachel deren Live-Parameter an und erlaubt
 * das Einstellen von Trigger-Modus, Name, Farbe sowie Testen und Zurücksetzen.
 * </p>
 */
public class MatrixControlDock extends HBox {

    /** Feste Mindest- und Standardhöhe des Docks (analog zur Stage View). */
    public static final double DOCK_HEIGHT = 220;

    private static final String[] PRESET_COLORS = {
            "#03DAC6", "#38BDF8", "#EF4444", "#F59E0B", "#10B981", "#A855F7", "#FB923C", "#E2E8F0"
    };

    private final MatrixService matrixService;
    private int selectedButtonIndex = -1;

    // Modul 1: Trigger-Modus
    private final ToggleButton btnToggle;
    private final ToggleButton btnFlash;
    private final ToggleGroup triggerGroup;

    // Modul 2: Eigenschaften
    private final TextField txtName;
    private final Label lblTargetInfo;
    private final HBox colorChipsBox;

    // Modul 3: Aktionen
    private final Button btnTest;
    private final Button btnClear;

    /**
     * Erstellt ein neues {@link MatrixControlDock}.
     *
     * @param matrixService der Matrix-Service (darf nicht {@code null} sein)
     */
    public MatrixControlDock(MatrixService matrixService) {
        this.matrixService = Objects.requireNonNull(matrixService, "matrixService darf nicht null sein");

        getStyleClass().add("matrix-control-dock");
        setMinHeight(DOCK_HEIGHT);
        setPrefHeight(DOCK_HEIGHT);
        setStyle("-fx-background-color: " + StudioTheme.HEX_SURFACE_1 + "; -fx-border-color: " + StudioTheme.HEX_BORDER + "; -fx-border-width: 1px 0 0 0;");
        setPadding(new Insets(12, 16, 12, 16));
        setSpacing(16);

        // --- Modul 1: Trigger-Modus (fest 220px) ---
        VBox modTrigger = createModuleContainer("Trigger-Modus", 220);

        triggerGroup = new ToggleGroup();
        btnToggle = new ToggleButton("Umschalten");
        btnToggle.setToggleGroup(triggerGroup);
        btnToggle.setMaxWidth(Double.MAX_VALUE);
        btnToggle.setOnAction(e -> applyTriggerMode(MatrixTriggerMode.TOGGLE));

        btnFlash = new ToggleButton("Taster");
        btnFlash.setToggleGroup(triggerGroup);
        btnFlash.setMaxWidth(Double.MAX_VALUE);
        btnFlash.setOnAction(e -> applyTriggerMode(MatrixTriggerMode.FLASH));

        VBox triggerBtnBox = new VBox(8, btnToggle, btnFlash);
        VBox.setVgrow(triggerBtnBox, Priority.ALWAYS);
        modTrigger.getChildren().add(triggerBtnBox);

        // --- Modul 2: Eigenschaften (flexibel) ---
        VBox modProps = createModuleContainer("Eigenschaften", 300);
        HBox.setHgrow(modProps, Priority.ALWAYS);

        txtName = new TextField();
        txtName.setPromptText("Button-Name");
        txtName.textProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedButtonIndex >= 0 && newVal != null) {
                matrixService.setButtonName(selectedButtonIndex, newVal);
            }
        });

        lblTargetInfo = new Label("Kein Ziel zugewiesen");
        lblTargetInfo.setStyle("-fx-text-fill: " + StudioTheme.HEX_TEXT_MED + "; -fx-font-size: 11px;");

        colorChipsBox = new HBox(6);
        colorChipsBox.setAlignment(Pos.CENTER_LEFT);
        for (String hex : PRESET_COLORS) {
            Button chip = new Button();
            chip.setMinSize(22, 22);
            chip.setPrefSize(22, 22);
            chip.setStyle("-fx-background-color: " + hex + "; -fx-background-radius: 4px; -fx-cursor: hand;");
            chip.setOnAction(e -> {
                if (selectedButtonIndex >= 0) {
                    matrixService.setButtonColor(selectedButtonIndex, hex);
                }
            });
            colorChipsBox.getChildren().add(chip);
        }

        VBox propsContent = new VBox(8, txtName, lblTargetInfo, colorChipsBox);
        VBox.setVgrow(propsContent, Priority.ALWAYS);
        modProps.getChildren().add(propsContent);

        // --- Modul 3: Aktionen (fest 200px) ---
        VBox modActions = createModuleContainer("Aktionen", 200);

        btnTest = new Button("Button testen");
        btnTest.setGraphic(new LucideIcon("play", 14, Color.web(StudioTheme.HEX_ICON_TIMELINE)));
        btnTest.setMaxWidth(Double.MAX_VALUE);
        btnTest.setOnAction(e -> {
            if (selectedButtonIndex >= 0) {
                matrixService.pressButton(selectedButtonIndex);
            }
        });

        btnClear = new Button("Zuweisung löschen");
        btnClear.setGraphic(new LucideIcon("trash-2", 14, Color.web(StudioTheme.HEX_ICON_RED)));
        btnClear.setMaxWidth(Double.MAX_VALUE);
        btnClear.setOnAction(e -> clearCurrentButton());

        VBox actionsContent = new VBox(8, btnTest, btnClear);
        VBox.setVgrow(actionsContent, Priority.ALWAYS);
        modActions.getChildren().add(actionsContent);

        getChildren().addAll(modTrigger, modProps, modActions);

        // Initial deaktiviert bis ein Button selektiert wird
        updateControlsDisabled(true);
    }

    private VBox createModuleContainer(String title, double minWidth) {
        VBox box = new VBox(8);
        box.setMinWidth(minWidth);
        box.setPadding(new Insets(8, 12, 8, 12));
        box.setStyle("-fx-background-color: " + StudioTheme.HEX_SURFACE_2 + "; -fx-background-radius: 4px; -fx-border-color: " + StudioTheme.HEX_BORDER + "; -fx-border-width: 1px; -fx-border-radius: 4px;");

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-text-fill: " + StudioTheme.HEX_TEXT_HIGH + "; -fx-font-size: 11px; -fx-font-weight: bold;");
        box.getChildren().add(lblTitle);

        return box;
    }

    /**
     * Wählt den Button am angegebenen Index aus und lädt dessen Daten in die Steuerelemente.
     *
     * @param index der Button-Index
     */
    public void selectButton(int index) {
        this.selectedButtonIndex = index;
        MatrixButtonDefinition btn = matrixService.getGrid().getButton(index);
        if (btn == null) {
            updateControlsDisabled(true);
            return;
        }

        updateControlsDisabled(false);
        txtName.setText(btn.name());

        if (btn.triggerMode() == MatrixTriggerMode.FLASH) {
            btnFlash.setSelected(true);
        } else {
            btnToggle.setSelected(true);
        }

        if (btn.isAssigned()) {
            lblTargetInfo.setText("Zugewiesen: " + btn.targetType() + " (" + btn.targetId() + ")");
        } else {
            lblTargetInfo.setText("Kein Ziel zugewiesen (Drag & Drop aus Explorer)");
        }
    }

    private void applyTriggerMode(MatrixTriggerMode mode) {
        if (selectedButtonIndex >= 0) {
            matrixService.setTriggerMode(selectedButtonIndex, mode);
        }
    }

    /**
     * Setzt den aktuell selektierten Trigger-Modus programmatisch.
     *
     * @param mode der {@link MatrixTriggerMode}
     */
    public void setTriggerMode(MatrixTriggerMode mode) {
        if (mode == MatrixTriggerMode.FLASH) {
            btnFlash.setSelected(true);
        } else {
            btnToggle.setSelected(true);
        }
        applyTriggerMode(mode);
    }

    /**
     * Setzt den aktuell selektierten Button zurück.
     */
    public void clearCurrentButton() {
        if (selectedButtonIndex >= 0) {
            matrixService.clearButton(selectedButtonIndex);
            selectButton(selectedButtonIndex);
        }
    }

    private void updateControlsDisabled(boolean disabled) {
        btnToggle.setDisable(disabled);
        btnFlash.setDisable(disabled);
        txtName.setDisable(disabled);
        btnTest.setDisable(disabled);
        btnClear.setDisable(disabled);
        colorChipsBox.setDisable(disabled);
    }

    /**
     * Gibt den Index des aktuell ausgewählten Buttons zurück.
     *
     * @return Button-Index oder -1
     */
    public int getSelectedButtonIndex() {
        return selectedButtonIndex;
    }

    /**
     * Gibt das Namens-Eingabefeld zurück.
     *
     * @return das {@link TextField}
     */
    public TextField getNameTextField() {
        return txtName;
    }

    /**
     * Gibt den aktuell im ToggleGroup selektierten Trigger-Modus zurück.
     *
     * @return der {@link MatrixTriggerMode}
     */
    public MatrixTriggerMode getSelectedTriggerMode() {
        return btnFlash.isSelected() ? MatrixTriggerMode.FLASH : MatrixTriggerMode.TOGGLE;
    }
}

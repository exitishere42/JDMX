package de.exit.jdmx.ui.io;

import de.exit.jdmx.core.i18n.I18n;
import de.exit.jdmx.core.model.MergeMode;
import de.exit.jdmx.core.model.UniverseConfig;
import de.exit.jdmx.service.HardwareDiscoveryService;
import de.exit.jdmx.ui.MainWindow;
import de.exit.jdmx.ui.component.CustomDropdownButton;
import de.exit.jdmx.ui.dialog.UniverseConfigDialog;
import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.theme.StudioTheme;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Universum-Routing-Tabelle im Studio-Dark-Design.
 * <p>
 * Verwaltet DMX-Universen 1 bis 4 mit dynamisch erkannten Ein- und Ausgängen,
 * konfigurierbarem Merging-Modus (HTP, LTP, Override), Live-Statusanzeige
 * und 1-Klick Art-Net Detail-Konfiguration.
 * </p>
 */
public class UniversePatchTable extends VBox {

    private static final int COL_WIDTH_UNIVERSE = 105;
    private static final int COL_WIDTH_DROPDOWN = 190;
    private static final int COL_WIDTH_MERGE = 80;
    private static final int COL_WIDTH_STATUS = 80;
    private static final int COL_WIDTH_CONFIG = 36;
    private static final int COL_SPACING = 14;

    private final HardwareDiscoveryService hardwareService;
    private final List<UniverseRow> universeRows = new ArrayList<>();
    private final List<HBox> dividers = new ArrayList<>();
    private final Label titleLabel;
    private final Button btnRefresh;
    private Label lblUni;
    private Label lblIn;
    private Label lblOut;
    private Label lblMerge;
    private Label lblStatus;
    private MainWindow mainWindow;
    private Runnable onConfigChanged;

    /**
     * Erstellt die Universum-Routing-Tabelle mit Standard-Hardwareerkennung.
     */
    public UniversePatchTable() {
        this(new HardwareDiscoveryService(), null);
    }

    /**
     * Erstellt die Universum-Routing-Tabelle mit einem bereitgestellten {@link HardwareDiscoveryService}.
     *
     * @param hardwareService Service zur Hardware-Erkennung
     */
    public UniversePatchTable(HardwareDiscoveryService hardwareService) {
        this(hardwareService, null);
    }

    /**
     * Erstellt die Universum-Routing-Tabelle mit {@link HardwareDiscoveryService} und Referenz auf {@link MainWindow}.
     *
     * @param hardwareService Service zur Hardware-Erkennung
     * @param mainWindow      Hauptfenster für In-App Overlays
     */
    public UniversePatchTable(HardwareDiscoveryService hardwareService, MainWindow mainWindow) {
        super(10);
        this.hardwareService = Objects.requireNonNull(hardwareService, "hardwareService darf nicht null sein");
        this.mainWindow = mainWindow;

        getStyleClass().add("universe-patch-table");
        setMaxWidth(800);

        // 1. Top-Toolbar mit Titel und Aktualisieren-Button
        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(2, 4, 6, 4));

        this.titleLabel = new Label(I18n.get("io.table.title"));
        titleLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px; -fx-font-weight: 700; -fx-letter-spacing: 0.5px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        this.btnRefresh = new Button(I18n.get("io.refresh"));
        btnRefresh.getStyleClass().add("io-refresh-btn");
        LucideIcon refreshIcon = new LucideIcon("refresh-cw", 12, Color.web("#9CA3AF"));
        btnRefresh.setGraphic(refreshIcon);
        btnRefresh.setCursor(Cursor.HAND);
        btnRefresh.setStyle("-fx-background-color: #242427; -fx-border-color: #38383C; " +
                "-fx-border-radius: 4px; -fx-background-radius: 4px; -fx-text-fill: #E5E7EB; " +
                "-fx-font-size: 11px; -fx-font-weight: 500; -fx-padding: 4 10 4 8;");
        btnRefresh.setOnMouseEntered(e -> {
            btnRefresh.setStyle("-fx-background-color: #2F2F33; -fx-border-color: #52525B; " +
                    "-fx-border-radius: 4px; -fx-background-radius: 4px; -fx-text-fill: #FFFFFF; " +
                    "-fx-font-size: 11px; -fx-font-weight: 500; -fx-padding: 4 10 4 8;");
            refreshIcon.setColor(Color.WHITE);
        });
        btnRefresh.setOnMouseExited(e -> {
            btnRefresh.setStyle("-fx-background-color: #242427; -fx-border-color: #38383C; " +
                    "-fx-border-radius: 4px; -fx-background-radius: 4px; -fx-text-fill: #E5E7EB; " +
                    "-fx-font-size: 11px; -fx-font-weight: 500; -fx-padding: 4 10 4 8;");
            refreshIcon.setColor(Color.web("#9CA3AF"));
        });
        btnRefresh.setOnAction(e -> refreshHardware());

        topBar.getChildren().addAll(titleLabel, spacer, btnRefresh);
        getChildren().add(topBar);

        // 2. Tabellenkopf mit pixelgenauen Breiten
        HBox headerRow = createHeaderRow();
        getChildren().add(headerRow);

        // Kopf-Trennstrich
        HBox headerDivider = createIndentedDivider();
        getChildren().add(headerDivider);

        // 3. Zeilen für Universum 1 bis 4
        for (int i = 0; i < 4; i++) {
            UniverseRow row = new UniverseRow(i + 1, i, hardwareService, () -> this.mainWindow, this::notifyConfigChanged);
            universeRows.add(row);
            getChildren().add(row);

            if (i < 3) {
                HBox divider = createIndentedDivider();
                dividers.add(divider);
                getChildren().add(divider);
            }
        }

        // Dynamischer Sprachwechsel
        I18n.addLanguageChangeListener(lang -> {
            if (Platform.isFxApplicationThread()) {
                updateLocalization();
            } else {
                Platform.runLater(this::updateLocalization);
            }
        });
    }

    public void setMainWindow(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
    }

    public MainWindow getMainWindow() {
        return mainWindow;
    }

    private HBox createHeaderRow() {
        HBox header = new HBox(COL_SPACING);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(2, 0, 4, 0));

        this.lblUni = createHeaderLabel(I18n.get("io.col.universe"), COL_WIDTH_UNIVERSE, Pos.CENTER_LEFT);
        this.lblIn = createHeaderLabel(I18n.get("io.col.input"), COL_WIDTH_DROPDOWN, Pos.CENTER_LEFT);
        this.lblOut = createHeaderLabel(I18n.get("io.col.output"), COL_WIDTH_DROPDOWN, Pos.CENTER_LEFT);
        this.lblMerge = createHeaderLabel(I18n.get("io.col.merge"), COL_WIDTH_MERGE, Pos.CENTER_LEFT);
        this.lblStatus = createHeaderLabel(I18n.get("io.col.status"), COL_WIDTH_STATUS, Pos.CENTER_LEFT);
        Label lblCfg = createHeaderLabel("", COL_WIDTH_CONFIG, Pos.CENTER);

        header.getChildren().addAll(lblUni, lblIn, lblOut, lblMerge, lblStatus, lblCfg);
        return header;
    }

    public void updateLocalization() {
        titleLabel.setText(I18n.get("io.table.title"));
        btnRefresh.setText(I18n.get("io.refresh"));
        if (lblUni != null) lblUni.setText(I18n.get("io.col.universe"));
        if (lblIn != null) lblIn.setText(I18n.get("io.col.input"));
        if (lblOut != null) lblOut.setText(I18n.get("io.col.output"));
        if (lblMerge != null) lblMerge.setText(I18n.get("io.col.merge"));
        if (lblStatus != null) lblStatus.setText(I18n.get("io.col.status"));
        for (UniverseRow row : universeRows) {
            row.updateLocalization();
        }
    }

    private Label createHeaderLabel(String text, double width, Pos alignment) {
        Label lbl = new Label(text);
        lbl.setMinWidth(width);
        lbl.setPrefWidth(width);
        lbl.setMaxWidth(width);
        lbl.setAlignment(alignment);
        lbl.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px; -fx-font-weight: 600; -fx-text-transform: uppercase;");
        return lbl;
    }

    private HBox createIndentedDivider() {
        HBox wrapper = new HBox();
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPadding(new Insets(2, 45, 2, 45));

        Region line = new Region();
        line.setMinHeight(1);
        line.setPrefHeight(1);
        line.setMaxHeight(1);
        line.setStyle("-fx-background-color: #282828;");
        HBox.setHgrow(line, Priority.ALWAYS);

        wrapper.getChildren().add(line);
        return wrapper;
    }

    public List<UniverseRow> getUniverseRows() {
        return List.copyOf(universeRows);
    }

    public List<HBox> getDividers() {
        return List.copyOf(dividers);
    }

    public Button getBtnRefresh() {
        return btnRefresh;
    }

    public void refreshHardware() {
        for (UniverseRow row : universeRows) {
            row.refreshOptions(hardwareService);
        }
    }

    public void setOnConfigChanged(Runnable onConfigChanged) {
        this.onConfigChanged = onConfigChanged;
    }

    private void notifyConfigChanged() {
        if (onConfigChanged != null) {
            onConfigChanged.run();
        }
    }

    /**
     * Repräsentiert eine Zeile für ein DMX-Universum.
     */
    public static class UniverseRow extends HBox {

        private final int universeNumber;
        private final int universeIndex;
        private final java.util.function.Supplier<MainWindow> mainWindowSupplier;
        private final Runnable changeNotifier;
        private final Label uniLabel;
        private final CustomDropdownButton inputDropdown;
        private final CustomDropdownButton outputDropdown;
        private final CustomDropdownButton mergeDropdown;
        private final Circle statusDot;
        private final Label statusText;
        private final Button configBtn;
        private final BooleanProperty active = new SimpleBooleanProperty(false);
        private UniverseConfig config;

        public UniverseRow(int universeNumber, int universeIndex, HardwareDiscoveryService hardwareService, java.util.function.Supplier<MainWindow> mainWindowSupplier) {
            this(universeNumber, universeIndex, hardwareService, mainWindowSupplier, null);
        }

        public UniverseRow(int universeNumber, int universeIndex, HardwareDiscoveryService hardwareService, java.util.function.Supplier<MainWindow> mainWindowSupplier, Runnable changeNotifier) {
            super(COL_SPACING);
            this.universeNumber = universeNumber;
            this.universeIndex = universeIndex;
            this.mainWindowSupplier = mainWindowSupplier;
            this.changeNotifier = changeNotifier;
            this.config = UniverseConfig.defaultFor(universeIndex);

            setAlignment(Pos.CENTER_LEFT);
            setPadding(new Insets(4, 0, 4, 0));

            // Spalte 1: Universum-Label
            this.uniLabel = new Label(I18n.get("fixture.universe_n", universeNumber));
            uniLabel.setMinWidth(COL_WIDTH_UNIVERSE);
            uniLabel.setPrefWidth(COL_WIDTH_UNIVERSE);
            uniLabel.setMaxWidth(COL_WIDTH_UNIVERSE);
            uniLabel.setStyle("-fx-text-fill: #F3F4F6; -fx-font-size: 13px; -fx-font-weight: 600;");

            // Spalte 2: Input Dropdown
            List<String> inputOptions = hardwareService.getAvailableInputOptions(universeIndex);
            inputDropdown = new CustomDropdownButton("Kein Input", inputOptions);
            inputDropdown.setMinWidth(COL_WIDTH_DROPDOWN);
            inputDropdown.setPrefWidth(COL_WIDTH_DROPDOWN);
            inputDropdown.setMaxWidth(COL_WIDTH_DROPDOWN);

            // Spalte 3: Output Dropdown
            List<String> outputOptions = hardwareService.getAvailableOutputOptions(universeIndex);
            String defaultOutput = (universeIndex == 0)
                    ? "Art-Net (127.0.0.1, Uni 0)"
                    : "Kein Output";
            outputDropdown = new CustomDropdownButton(defaultOutput, outputOptions);
            outputDropdown.setMinWidth(COL_WIDTH_DROPDOWN);
            outputDropdown.setPrefWidth(COL_WIDTH_DROPDOWN);
            outputDropdown.setMaxWidth(COL_WIDTH_DROPDOWN);

            // Spalte 4: Merge Dropdown (HTP, LTP, Override)
            mergeDropdown = new CustomDropdownButton(config.mergeMode().getDisplayName(), List.of("HTP", "LTP", "Override"));
            mergeDropdown.setMinWidth(COL_WIDTH_MERGE);
            mergeDropdown.setPrefWidth(COL_WIDTH_MERGE);
            mergeDropdown.setMaxWidth(COL_WIDTH_MERGE);

            // Spalte 5: Live-Status (Dot + Text)
            HBox statusBox = new HBox(6);
            statusBox.setAlignment(Pos.CENTER_LEFT);
            statusBox.setMinWidth(COL_WIDTH_STATUS);
            statusBox.setPrefWidth(COL_WIDTH_STATUS);
            statusBox.setMaxWidth(COL_WIDTH_STATUS);

            statusDot = new Circle(4.5);
            statusText = new Label();
            statusText.setStyle("-fx-font-size: 12px; -fx-font-weight: 500;");
            statusBox.getChildren().addAll(statusDot, statusText);

            // Status-Reaktivität
            inputDropdown.selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                updateStatus();
                MainWindow mw = mainWindowSupplier != null ? mainWindowSupplier.get() : null;
                if (mw != null && mw.getDmxService() != null) {
                    this.config = this.config.withInputSource(newVal != null ? newVal : "Kein Input");
                    mw.getDmxService().setUniverseConfig(this.universeIndex, this.config);
                }
                if (changeNotifier != null) {
                    changeNotifier.run();
                }
            });
            outputDropdown.selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                updateStatus();
                MainWindow mw = mainWindowSupplier != null ? mainWindowSupplier.get() : null;
                if (mw != null && mw.getDmxService() != null) {
                    this.config = this.config.withOutputTarget(newVal != null ? newVal : "Kein Output");
                    mw.getDmxService().setUniverseConfig(this.universeIndex, this.config);
                }
                if (changeNotifier != null) {
                    changeNotifier.run();
                }
            });
            mergeDropdown.selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                MergeMode mode = MergeMode.fromString(newVal);
                this.config = this.config.withMergeMode(mode);
                MainWindow mw = mainWindowSupplier != null ? mainWindowSupplier.get() : null;
                if (mw != null && mw.getDmxService() != null) {
                    mw.getDmxService().setUniverseConfig(this.universeIndex, this.config);
                }
                if (changeNotifier != null) {
                    changeNotifier.run();
                }
            });
            updateStatus();

            // Spalte 6: Detail-Config Button
            configBtn = new Button();
            configBtn.setGraphic(new LucideIcon("settings", 15, StudioTheme.COLOR_PRIMARY_GRAY));
            configBtn.setCursor(Cursor.HAND);
            configBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #2E2E2E; " +
                    "-fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 5px;");
            configBtn.setOnMouseEntered(e -> configBtn.setStyle("-fx-background-color: #2A2A2A; -fx-border-color: #3E3E3E; " +
                    "-fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 5px;"));
            configBtn.setOnMouseExited(e -> configBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #2E2E2E; " +
                    "-fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 5px;"));
            configBtn.setOnAction(e -> showConfigDialog());

            getChildren().addAll(uniLabel, inputDropdown, outputDropdown, mergeDropdown, statusBox, configBtn);
        }

        private void updateStatus() {
            boolean hasInput = inputDropdown.getSelectedItem() != null
                    && !inputDropdown.getSelectedItem().equals("Kein Input")
                    && !inputDropdown.getSelectedItem().equals("No Input")
                    && !inputDropdown.getSelectedItem().equals("Без входа");
            boolean hasOutput = outputDropdown.getSelectedItem() != null
                    && !outputDropdown.getSelectedItem().equals("Kein Output")
                    && !outputDropdown.getSelectedItem().equals("No Output")
                    && !outputDropdown.getSelectedItem().equals("Без выхода");
            boolean isActive = hasInput || hasOutput;
            active.set(isActive);

            if (isActive) {
                statusDot.setFill(Color.web("#10B981"));
                statusText.setText(I18n.get("io.status.active"));
                statusText.setStyle("-fx-text-fill: #10B981; -fx-font-size: 12px; -fx-font-weight: 600;");
            } else {
                statusDot.setFill(Color.web("#6B7280"));
                statusText.setText(I18n.get("io.status.inactive"));
                statusText.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px; -fx-font-weight: 500;");
            }
        }

        public void updateLocalization() {
            uniLabel.setText(I18n.get("fixture.universe_n", universeNumber));
            updateStatus();
        }

        public void showConfigDialog() {
            MainWindow mw = mainWindowSupplier != null ? mainWindowSupplier.get() : null;
            if (mw != null) {
                String in = inputDropdown.getSelectedItem();
                String out = outputDropdown.getSelectedItem();
                UniverseConfig currentConfig = this.config
                        .withInputSource(in != null ? in : "Kein Input")
                        .withOutputTarget(out != null ? out : "Kein Output");

                UniverseConfigDialog.show(mw, currentConfig, updated -> {
                    this.config = updated;
                    if (mw.getDmxService() != null) {
                        mw.getDmxService().setUniverseConfig(this.universeIndex, updated);
                    }
                    if (outputDropdown.getSelectedItem() != null && outputDropdown.getSelectedItem().startsWith("Art-Net")) {
                        String newLabel = "Art-Net (" + updated.artNetIp() + ", Uni " + updated.artNetUniverse() + ")";
                        outputDropdown.selectItem(newLabel);
                    }
                    if (changeNotifier != null) {
                        changeNotifier.run();
                    }
                });
            }
        }

        public int getUniverseNumber() {
            return universeNumber;
        }

        public int getUniverseIndex() {
            return universeIndex;
        }

        public UniverseConfig getConfig() {
            return config;
        }

        public void setConfig(UniverseConfig config) {
            this.config = Objects.requireNonNull(config, "config darf nicht null sein");
            if (mergeDropdown != null && config.mergeMode() != null) {
                mergeDropdown.selectItem(config.mergeMode().getDisplayName());
            }
        }

        public CustomDropdownButton getInputDropdown() {
            return inputDropdown;
        }

        public CustomDropdownButton getOutputDropdown() {
            return outputDropdown;
        }

        public CustomDropdownButton getMergeDropdown() {
            return mergeDropdown;
        }

        public boolean isActive() {
            return active.get();
        }

        public BooleanProperty activeProperty() {
            return active;
        }

        public Button getConfigBtn() {
            return configBtn;
        }

        public void refreshOptions(HardwareDiscoveryService hardwareService) {
            String currentInput = inputDropdown.getSelectedItem();
            List<String> newInputOptions = hardwareService.getAvailableInputOptions(universeIndex);
            inputDropdown.setItems(newInputOptions);
            if (currentInput != null && newInputOptions.contains(currentInput)) {
                inputDropdown.selectItem(currentInput);
            } else {
                inputDropdown.selectItem("Kein Input");
            }

            String currentOutput = outputDropdown.getSelectedItem();
            List<String> newOutputOptions = hardwareService.getAvailableOutputOptions(universeIndex);
            outputDropdown.setItems(newOutputOptions);
            if (currentOutput != null && newOutputOptions.contains(currentOutput)) {
                outputDropdown.selectItem(currentOutput);
            } else if (currentOutput != null && currentOutput.startsWith("Art-Net")) {
                outputDropdown.selectItem(currentOutput);
            } else {
                outputDropdown.selectItem("Kein Output");
            }
            updateStatus();
        }
    }
}

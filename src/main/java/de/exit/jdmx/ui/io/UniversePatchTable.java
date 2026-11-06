package de.exit.jdmx.ui.io;

import de.exit.jdmx.core.model.UniverseConfig;
import de.exit.jdmx.service.HardwareDiscoveryService;
import de.exit.jdmx.ui.MainWindow;
import de.exit.jdmx.ui.component.CustomDropdownButton;
import de.exit.jdmx.ui.dialog.UniverseConfigDialog;
import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.theme.StudioTheme;
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
 * Universum-Zuordnungstabelle im QLC+-Stil, angepasst an das Studio-Dark-Design.
 * <p>
 * Verwaltet 4 DMX-Universen mit dynamisch erkannten Ein- und Ausgängen,
 * kompakten Custom-Dropdowns, exakter Spaltenausrichtung und dezenten,
 * eingerückten Trennstrichen.
 * </p>
 */
public class UniversePatchTable extends VBox {

    private static final int COL_WIDTH_UNIVERSE = 110;
    private static final int COL_WIDTH_DROPDOWN = 210;
    private static final int COL_WIDTH_STATUS = 85;
    private static final int COL_WIDTH_CONFIG = 36;
    private static final int COL_SPACING = 16;

    private final HardwareDiscoveryService hardwareService;
    private final List<UniverseRow> universeRows = new ArrayList<>();
    private final List<HBox> dividers = new ArrayList<>();
    private MainWindow mainWindow;

    /**
     * Erstellt die Universum-Patch-Tabelle mit Standard-Hardwareerkennung.
     */
    public UniversePatchTable() {
        this(new HardwareDiscoveryService(), null);
    }

    /**
     * Erstellt die Universum-Patch-Tabelle mit einem bereitgestellten {@link HardwareDiscoveryService}.
     *
     * @param hardwareService Service zur Hardware-Erkennung
     */
    public UniversePatchTable(HardwareDiscoveryService hardwareService) {
        this(hardwareService, null);
    }

    /**
     * Erstellt die Universum-Patch-Tabelle mit {@link HardwareDiscoveryService} und Referenz auf {@link MainWindow}.
     *
     * @param hardwareService Service zur Hardware-Erkennung
     * @param mainWindow      Hauptfenster für In-App Overlays
     */
    public UniversePatchTable(HardwareDiscoveryService hardwareService, MainWindow mainWindow) {
        super(10);
        this.hardwareService = Objects.requireNonNull(hardwareService, "hardwareService darf nicht null sein");
        this.mainWindow = mainWindow;

        // Styling als elegante Studio-Dark Karte (12px Ecken-Rundung)
        getStyleClass().add("universe-patch-table");
        setMaxWidth(760);

        // 2. Tabellenkopf mit pixelgenauen Breiten
        HBox headerRow = createHeaderRow();
        getChildren().add(headerRow);

        // Kopf-Trennstrich
        HBox headerDivider = createIndentedDivider();
        getChildren().add(headerDivider);

        // 3. Zeilen für Universum 1 bis 4
        for (int i = 0; i < 4; i++) {
            UniverseRow row = new UniverseRow(i + 1, i, hardwareService, () -> this.mainWindow);
            universeRows.add(row);
            getChildren().add(row);

            // Unter jeder Zeile ein kleiner, eingerückter Trennstrich (ca. 2cm / 45px Rand)
            if (i < 3) {
                HBox divider = createIndentedDivider();
                dividers.add(divider);
                getChildren().add(divider);
            }
        }
    }

    /**
     * Setzt die Referenz auf das übergeordnete Hauptfenster für Modal-Overlays.
     *
     * @param mainWindow Hauptfenster
     */
    public void setMainWindow(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
    }

    /**
     * Liefert das verknüpfte Hauptfenster.
     *
     * @return Hauptfenster oder null
     */
    public MainWindow getMainWindow() {
        return mainWindow;
    }

    /**
     * Erzeugt die Header-Zeile mit exakt identischen Spaltenbreiten wie die Datenzeilen.
     */
    private HBox createHeaderRow() {
        HBox header = new HBox(COL_SPACING);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(2, 0, 4, 0));

        Label lblUni = createHeaderLabel("Universum", COL_WIDTH_UNIVERSE, Pos.CENTER_LEFT);
        Label lblIn = createHeaderLabel("Input (Dropdown)", COL_WIDTH_DROPDOWN, Pos.CENTER_LEFT);
        Label lblOut = createHeaderLabel("Output (Dropdown)", COL_WIDTH_DROPDOWN, Pos.CENTER_LEFT);
        Label lblStatus = createHeaderLabel("Status", COL_WIDTH_STATUS, Pos.CENTER_LEFT);
        Label lblCfg = createHeaderLabel("", COL_WIDTH_CONFIG, Pos.CENTER);

        header.getChildren().addAll(lblUni, lblIn, lblOut, lblStatus, lblCfg);
        return header;
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

    /**
     * Erzeugt einen feinen Trennstrich, der ca. 45px (~2 cm) von links und rechts eingerückt ist.
     */
    private HBox createIndentedDivider() {
        HBox wrapper = new HBox();
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPadding(new Insets(2, 45, 2, 45)); // 45px Randabstand links & rechts

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

    /**
     * Repräsentiert eine Zeile für ein DMX-Universum.
     */
    public static class UniverseRow extends HBox {

        private final int universeNumber;
        private final int universeIndex;
        private final java.util.function.Supplier<MainWindow> mainWindowSupplier;
        private final CustomDropdownButton inputDropdown;
        private final CustomDropdownButton outputDropdown;
        private final Circle statusDot;
        private final Label statusText;
        private final Button configBtn;
        private final BooleanProperty active = new SimpleBooleanProperty(false);
        private UniverseConfig config;

        public UniverseRow(int universeNumber, int universeIndex, HardwareDiscoveryService hardwareService, java.util.function.Supplier<MainWindow> mainWindowSupplier) {
            super(COL_SPACING);
            this.universeNumber = universeNumber;
            this.universeIndex = universeIndex;
            this.mainWindowSupplier = mainWindowSupplier;
            this.config = UniverseConfig.defaultFor(universeIndex);

            setAlignment(Pos.CENTER_LEFT);
            setPadding(new Insets(4, 0, 4, 0));

            // Spalte 1: Universum-Label
            Label uniLabel = new Label("Universum " + universeNumber);
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

            // Spalte 3: Output Dropdown (Universum 1 hat Art-Net default)
            List<String> outputOptions = hardwareService.getAvailableOutputOptions(universeIndex);
            String defaultOutput = (universeIndex == 0)
                    ? "Art-Net (127.0.0.1, Uni 0)"
                    : "Kein Output";
            outputDropdown = new CustomDropdownButton(defaultOutput, outputOptions);
            outputDropdown.setMinWidth(COL_WIDTH_DROPDOWN);
            outputDropdown.setPrefWidth(COL_WIDTH_DROPDOWN);
            outputDropdown.setMaxWidth(COL_WIDTH_DROPDOWN);

            // Spalte 4: Live-Status (Dot + Text)
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
            });
            outputDropdown.selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                updateStatus();
                MainWindow mw = mainWindowSupplier != null ? mainWindowSupplier.get() : null;
                if (mw != null && mw.getDmxService() != null) {
                    this.config = this.config.withOutputTarget(newVal != null ? newVal : "Kein Output");
                    mw.getDmxService().setUniverseConfig(this.universeIndex, this.config);
                }
            });
            updateStatus();

            // Spalte 5: Detail-Config Button (Fenster-im-Fenster Modal)
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

            getChildren().addAll(uniLabel, inputDropdown, outputDropdown, statusBox, configBtn);
        }

        private void updateStatus() {
            boolean hasInput = inputDropdown.getSelectedItem() != null && !inputDropdown.getSelectedItem().equals("Kein Input");
            boolean hasOutput = outputDropdown.getSelectedItem() != null && !outputDropdown.getSelectedItem().equals("Kein Output");
            boolean isActive = hasInput || hasOutput;
            active.set(isActive);

            if (isActive) {
                statusDot.setFill(Color.web("#10B981")); // Grün
                statusText.setText("Aktiv");
                statusText.setStyle("-fx-text-fill: #10B981; -fx-font-size: 12px; -fx-font-weight: 600;");
            } else {
                statusDot.setFill(Color.web("#6B7280")); // Grau
                statusText.setText("Aus");
                statusText.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px; -fx-font-weight: 500;");
            }
        }

        /**
         * Öffnet den In-App Modal Dialog ("Fenster im Fenster") zur Art-Net- und Universum-Konfiguration.
         */
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
        }

        public CustomDropdownButton getInputDropdown() {
            return inputDropdown;
        }

        public CustomDropdownButton getOutputDropdown() {
            return outputDropdown;
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
    }
}

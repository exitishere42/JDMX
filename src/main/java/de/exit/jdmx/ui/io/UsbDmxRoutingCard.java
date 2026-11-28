package de.exit.jdmx.ui.io;

import de.exit.jdmx.core.i18n.I18n;
import de.exit.jdmx.service.HardwareDiscoveryService;
import de.exit.jdmx.ui.component.CustomDropdownButton;
import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.theme.StudioTheme;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.List;
import java.util.Objects;

/**
 * Modulare Hardware-Karte für USB-DMX-Interfaces im I/O-Routing Workspace.
 * <p>
 * Harmonisch abgestimmt auf das Studio-Dark Design (identischer Rahmen #2E2E2E,
 * 4px Eckenradius, echtes Lucide-USB-Icon und dezenter Status-Punkt).
 * </p>
 */
public class UsbDmxRoutingCard extends VBox {

    private final HardwareDiscoveryService hardwareService;
    private final Label titleLabel;
    private final Circle statusDot;
    private final Label badgeLabel;
    private final CustomDropdownButton portDropdown;

    private Label lblPortTitle;
    private Label lblBaudTitle;
    private Label lblStatusTitle;

    private final Label baudLabel;
    private final Label statusLabel;
    private final Button btnTestSignal;

    public UsbDmxRoutingCard(HardwareDiscoveryService hardwareService) {
        super(10);
        this.hardwareService = Objects.requireNonNull(hardwareService, "hardwareService darf nicht null sein");

        getStyleClass().add("io-hardware-card");
        setStyle("-fx-background-color: #1A1A1A; -fx-border-color: #2E2E2E; -fx-border-width: 1px; " +
                "-fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 14 16;");
        setMinWidth(240);
        setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(this, Priority.ALWAYS);

        // 1. Header (Teal-Icon, einheitlich zum I/O-Tab)
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        LucideIcon icon = new LucideIcon("usb", 15, StudioTheme.COLOR_ICON_IO);
        this.titleLabel = new Label(I18n.get("io.card.usb_dmx"));
        titleLabel.setStyle("-fx-text-fill: #F3F4F6; -fx-font-size: 12px; -fx-font-weight: 700;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Dezenter Status-Indikator (Punkt + Text wie in UniversePatchTable)
        HBox statusBox = new HBox(6);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        this.statusDot = new Circle(4.5, Color.web("#10B981"));
        this.badgeLabel = new Label(I18n.get("io.usb.status_connected"));
        badgeLabel.setStyle("-fx-text-fill: #10B981; -fx-font-size: 12px; -fx-font-weight: 600;");
        statusBox.getChildren().addAll(statusDot, badgeLabel);

        header.getChildren().addAll(icon, titleLabel, spacer, statusBox);

        // 2. Dropdown & Details
        this.portDropdown = new CustomDropdownButton(I18n.get("io.usb.no_device"), List.of());
        portDropdown.setMaxWidth(Double.MAX_VALUE);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(6);
        grid.setPadding(new Insets(4, 0, 0, 0));

        this.lblPortTitle = createFormLabel(I18n.get("io.usb.port"));
        this.lblBaudTitle = createFormLabel(I18n.get("io.usb.baudrate"));
        this.lblStatusTitle = createFormLabel(I18n.get("io.col.status") + ":");

        this.baudLabel = createValueLabel("250.000 Baud (DMX512)");
        this.statusLabel = createValueLabel(I18n.get("io.status.inactive"));

        grid.add(lblPortTitle, 0, 0);
        grid.add(portDropdown, 1, 0);

        grid.add(lblBaudTitle, 0, 1);
        grid.add(baudLabel, 1, 1);

        grid.add(lblStatusTitle, 0, 2);
        grid.add(statusLabel, 1, 2);

        // 3. Test-Signal Button (gestylt wie der Scan-Hardware-Button)
        this.btnTestSignal = new Button(I18n.get("io.usb.test_btn"));
        LucideIcon zapIcon = new LucideIcon("zap", 13, Color.web("#F59E0B"));
        btnTestSignal.setGraphic(zapIcon);
        btnTestSignal.setCursor(Cursor.HAND);
        btnTestSignal.setMaxWidth(Double.MAX_VALUE);
        btnTestSignal.setStyle("-fx-background-color: #242427; -fx-border-color: #38383C; " +
                "-fx-border-radius: 4px; -fx-background-radius: 4px; -fx-text-fill: #E5E7EB; " +
                "-fx-font-size: 11px; -fx-font-weight: 500; -fx-padding: 5 12;");
        btnTestSignal.setOnMouseEntered(e -> btnTestSignal.setStyle("-fx-background-color: #2F2F33; -fx-border-color: #52525B; " +
                "-fx-border-radius: 4px; -fx-background-radius: 4px; -fx-text-fill: #FFFFFF; " +
                "-fx-font-size: 11px; -fx-font-weight: 500; -fx-padding: 5 12;"));
        btnTestSignal.setOnMouseExited(e -> btnTestSignal.setStyle("-fx-background-color: #242427; -fx-border-color: #38383C; " +
                "-fx-border-radius: 4px; -fx-background-radius: 4px; -fx-text-fill: #E5E7EB; " +
                "-fx-font-size: 11px; -fx-font-weight: 500; -fx-padding: 5 12;"));

        btnTestSignal.setOnAction(e -> triggerTestSignal());

        getChildren().addAll(header, grid, btnTestSignal);

        refreshHardware();

        I18n.addLanguageChangeListener(lang -> {
            if (Platform.isFxApplicationThread()) {
                updateLocalization();
            } else {
                Platform.runLater(this::updateLocalization);
            }
        });
    }

    public void refreshHardware() {
        List<String> devices = hardwareService.getDetectedUsbDmxDevices();
        portDropdown.setItems(devices);

        if (!devices.isEmpty()) {
            portDropdown.selectItem(devices.get(0));
            statusDot.setFill(Color.web("#10B981"));
            badgeLabel.setText(I18n.get("io.usb.status_connected"));
            badgeLabel.setStyle("-fx-text-fill: #10B981; -fx-font-size: 12px; -fx-font-weight: 600;");
            statusLabel.setText(I18n.get("io.status.active"));
            statusLabel.setStyle("-fx-text-fill: #10B981; -fx-font-size: 11px; -fx-font-weight: 600;");
            btnTestSignal.setDisable(false);
        } else {
            portDropdown.selectItem(I18n.get("io.usb.no_device"));
            statusDot.setFill(Color.web("#6B7280"));
            badgeLabel.setText(I18n.get("io.status.inactive"));
            badgeLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px; -fx-font-weight: 500;");
            statusLabel.setText(I18n.get("io.usb.no_device"));
            statusLabel.setStyle("-fx-text-fill: #71717A; -fx-font-size: 11px; -fx-font-weight: 500;");
            btnTestSignal.setDisable(true);
        }
    }

    private void triggerTestSignal() {
        String port = portDropdown.getSelectedItem();
        hardwareService.sendUsbDmxTestSignal(port);
        btnTestSignal.setText(I18n.get("io.usb.test_active"));
        btnTestSignal.setStyle("-fx-background-color: #2F2F33; -fx-border-color: #F59E0B; " +
                "-fx-border-radius: 4px; -fx-background-radius: 4px; -fx-text-fill: #FBBF24; " +
                "-fx-font-size: 11px; -fx-font-weight: 600; -fx-padding: 5 12;");

        PauseTransition pause = new PauseTransition(Duration.seconds(1.2));
        pause.setOnFinished(ev -> {
            btnTestSignal.setText(I18n.get("io.usb.test_btn"));
            btnTestSignal.setStyle("-fx-background-color: #242427; -fx-border-color: #38383C; " +
                    "-fx-border-radius: 4px; -fx-background-radius: 4px; -fx-text-fill: #E5E7EB; " +
                    "-fx-font-size: 11px; -fx-font-weight: 500; -fx-padding: 5 12;");
        });
        pause.play();
    }

    public void updateLocalization() {
        titleLabel.setText(I18n.get("io.card.usb_dmx"));
        lblPortTitle.setText(I18n.get("io.usb.port"));
        lblBaudTitle.setText(I18n.get("io.usb.baudrate"));
        lblStatusTitle.setText(I18n.get("io.col.status") + ":");
        btnTestSignal.setText(I18n.get("io.usb.test_btn"));
        refreshHardware();
    }

    private Label createFormLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px; -fx-font-weight: 500; -fx-min-width: 85px;");
        return lbl;
    }

    private Label createValueLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #E5E7EB; -fx-font-size: 11px; -fx-font-weight: 500;");
        return lbl;
    }

    public CustomDropdownButton getPortDropdown() {
        return portDropdown;
    }

    public Button getBtnTestSignal() {
        return btnTestSignal;
    }
}

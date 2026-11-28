package de.exit.jdmx.ui.io;

import de.exit.jdmx.core.i18n.I18n;
import de.exit.jdmx.core.model.NetworkInterfaceInfo;
import de.exit.jdmx.service.HardwareDiscoveryService;
import de.exit.jdmx.ui.component.CustomDropdownButton;
import de.exit.jdmx.ui.icon.LucideIcon;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Modulare Hardware-Karte für Netzwerk- und Art-Net-Schnittstellen im I/O-Routing Workspace.
 * <p>
 * Zeigt alle erkannten Netzwerkadapter (Ethernet, Wi-Fi, Loopback), IP-Adresse,
 * Subnetzmaske, Broadcast-Adresse sowie Art-Net UDP-Port und Sendedatenrate an.
 * </p>
 */
public class NetworkRoutingCard extends VBox {

    private final HardwareDiscoveryService hardwareService;
    private final Label titleLabel;
    private final Label badgeLabel;
    private final CustomDropdownButton interfaceDropdown;

    private Label lblInterfaceTitle;
    private Label lblIpTitle;
    private Label lblSubnetTitle;
    private Label lblBroadcastTitle;
    private Label lblPortTitle;
    private Label lblTrafficTitle;

    private final Label ipLabel;
    private final Label subnetLabel;
    private final Label broadcastLabel;
    private final Label portLabel;
    private final Label trafficLabel;

    private List<NetworkInterfaceInfo> interfaces = new ArrayList<>();

    public NetworkRoutingCard(HardwareDiscoveryService hardwareService) {
        super(10);
        this.hardwareService = Objects.requireNonNull(hardwareService, "hardwareService darf nicht null sein");

        getStyleClass().add("io-hardware-card");
        setStyle("-fx-background-color: #1A1A1E; -fx-border-color: #2D2D32; -fx-border-width: 1px; " +
                "-fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 12 14;");
        setMinWidth(240);
        setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(this, Priority.ALWAYS);

        // 1. Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        LucideIcon icon = new LucideIcon("network", 15, Color.web("#38BDF8"));
        this.titleLabel = new Label(I18n.get("io.card.network"));
        titleLabel.setStyle("-fx-text-fill: #F3F4F6; -fx-font-size: 12px; -fx-font-weight: 700;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        this.badgeLabel = new Label(I18n.get("io.status.active"));
        badgeLabel.setStyle("-fx-background-color: rgba(16, 185, 129, 0.15); -fx-text-fill: #10B981; " +
                "-fx-font-size: 10px; -fx-font-weight: 600; -fx-padding: 2 6; -fx-background-radius: 3px; -fx-border-color: #065F46; -fx-border-radius: 3px;");

        header.getChildren().addAll(icon, titleLabel, spacer, badgeLabel);

        // 2. Dropdown & Details
        this.interfaceDropdown = new CustomDropdownButton("Schnittstelle wählen", List.of());
        interfaceDropdown.setMaxWidth(Double.MAX_VALUE);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);
        grid.setPadding(new Insets(4, 0, 0, 0));

        this.lblInterfaceTitle = createFormLabel(I18n.get("io.network.interface"));
        this.lblIpTitle = createFormLabel(I18n.get("io.network.ip"));
        this.lblSubnetTitle = createFormLabel(I18n.get("io.network.subnet"));
        this.lblBroadcastTitle = createFormLabel(I18n.get("io.network.broadcast"));
        this.lblPortTitle = createFormLabel(I18n.get("io.network.port"));
        this.lblTrafficTitle = createFormLabel(I18n.get("io.network.traffic"));

        this.ipLabel = createValueLabel("-");
        this.subnetLabel = createValueLabel("-");
        this.broadcastLabel = createValueLabel("-");
        this.portLabel = createValueLabel("6454 UDP");
        this.trafficLabel = createValueLabel("40 DMX/s");
        trafficLabel.setStyle("-fx-text-fill: #10B981; -fx-font-size: 11px; -fx-font-weight: 600;");

        grid.add(lblInterfaceTitle, 0, 0);
        grid.add(interfaceDropdown, 1, 0);

        grid.add(lblIpTitle, 0, 1);
        grid.add(ipLabel, 1, 1);

        grid.add(lblSubnetTitle, 0, 2);
        grid.add(subnetLabel, 1, 2);

        grid.add(lblBroadcastTitle, 0, 3);
        grid.add(broadcastLabel, 1, 3);

        grid.add(lblPortTitle, 0, 4);
        grid.add(portLabel, 1, 4);

        grid.add(lblTrafficTitle, 0, 5);
        grid.add(trafficLabel, 1, 5);

        getChildren().addAll(header, grid);

        interfaceDropdown.selectedItemProperty().addListener((obs, oldVal, newVal) -> updateSelectedInterface(newVal));

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
        this.interfaces = hardwareService.getDetectedNetworkInterfaces();
        List<String> items = new ArrayList<>();
        for (NetworkInterfaceInfo iface : interfaces) {
            items.add(iface.getSummary());
        }
        interfaceDropdown.setItems(items);

        if (!items.isEmpty()) {
            // Erstes nicht-Loopback wählen, falls vorhanden
            String selected = items.get(0);
            for (int i = 0; i < interfaces.size(); i++) {
                if (!interfaces.get(i).isLoopback()) {
                    selected = items.get(i);
                    break;
                }
            }
            interfaceDropdown.selectItem(selected);
            updateSelectedInterface(selected);
        } else {
            interfaceDropdown.selectItem("Kein Netzwerk");
            ipLabel.setText("-");
            subnetLabel.setText("-");
            broadcastLabel.setText("-");
        }
    }

    private void updateSelectedInterface(String summary) {
        if (summary == null) return;
        for (NetworkInterfaceInfo iface : interfaces) {
            if (iface.getSummary().equals(summary)) {
                ipLabel.setText(iface.ipAddress());
                subnetLabel.setText(iface.subnetMask());
                broadcastLabel.setText(iface.broadcastAddress());
                return;
            }
        }
    }

    public void updateLocalization() {
        titleLabel.setText(I18n.get("io.card.network"));
        badgeLabel.setText(I18n.get("io.status.active"));
        lblInterfaceTitle.setText(I18n.get("io.network.interface"));
        lblIpTitle.setText(I18n.get("io.network.ip"));
        lblSubnetTitle.setText(I18n.get("io.network.subnet"));
        lblBroadcastTitle.setText(I18n.get("io.network.broadcast"));
        lblPortTitle.setText(I18n.get("io.network.port"));
        lblTrafficTitle.setText(I18n.get("io.network.traffic"));
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

    public CustomDropdownButton getInterfaceDropdown() {
        return interfaceDropdown;
    }

    public Label getIpLabel() {
        return ipLabel;
    }

    public Label getSubnetLabel() {
        return subnetLabel;
    }

    public Label getBroadcastLabel() {
        return broadcastLabel;
    }
}

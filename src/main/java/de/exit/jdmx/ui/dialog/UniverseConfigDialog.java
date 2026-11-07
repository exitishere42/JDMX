package de.exit.jdmx.ui.dialog;

import de.exit.jdmx.core.model.UniverseConfig;
import de.exit.jdmx.ui.MainWindow;
import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.theme.StudioTheme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Modaler In-App Dialog für die Konfiguration eines DMX-Universums ("Fenster im Fenster").
 * <p>
 * Erlaubt die genaue Konfiguration von Art-Net 4 Netzwerkparametern (Ziel-IP, Port,
 * Subnet, Universe, Broadcast) sowie der DMX-Ausgaberate.
 * Ersetzt alle nativen Betriebssystem-Dialoge vollständig.
 * </p>
 */
public class UniverseConfigDialog {

    private final MainWindow mainWindow;
    private final UniverseConfig initialConfig;
    private final Consumer<UniverseConfig> onSave;

    private final VBox card;
    private Label titleLabel;
    private Button btnClose;

    // Art-Net Formularfelder
    private TextField txtIp;
    private TextField txtPort;
    private TextField txtSubnet;
    private TextField txtUniverse;
    private CheckBox chkBroadcast;

    // Timing
    private TextField txtRefreshRate;

    // Footer Buttons
    private Button btnCancel;
    private Button btnSave;

    /**
     * Öffnet den Universums-Konfigurations-Dialog als In-App Modal Overlay.
     *
     * @param mainWindow Hauptfenster
     * @param config     Aktuelle Universums-Konfiguration
     * @param onSave     Callback nach erfolgreichem Speichern
     */
    public static void show(MainWindow mainWindow, UniverseConfig config, Consumer<UniverseConfig> onSave) {
        UniverseConfigDialog dialog = new UniverseConfigDialog(mainWindow, config, onSave);
        if (mainWindow != null) {
            mainWindow.showOverlay(dialog.getView());
        }
    }

    public UniverseConfigDialog(MainWindow mainWindow, UniverseConfig config, Consumer<UniverseConfig> onSave) {
        this.mainWindow = mainWindow;
        this.initialConfig = Objects.requireNonNullElseGet(config, () -> UniverseConfig.defaultFor(0));
        this.onSave = onSave;

        this.card = new VBox(14);
        card.getStyleClass().add("settings-dialog-card");
        card.setMinWidth(480);
        card.setPrefWidth(500);
        card.setMaxWidth(520);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        buildUi();
    }

    private void buildUi() {
        // 1. Header
        HBox header = buildHeader();

        // 2. Art-Net Netzwerk Sektion
        VBox artNetSection = buildArtNetSection();

        // 3. Routing & Timing Sektion
        VBox timingSection = buildTimingSection();

        // 4. Footer Aktionen
        HBox footer = buildFooter();

        card.getChildren().addAll(header, artNetSection, timingSection, footer);
    }

    private HBox buildHeader() {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("settings-dialog-header");

        LucideIcon icon = new LucideIcon("settings", 20, Color.web("#38BDF8"));
        titleLabel = new Label("Universum " + initialConfig.universeNumber() + " Konfiguration");
        titleLabel.getStyleClass().add("settings-dialog-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnClose = new Button();
        btnClose.getStyleClass().add("dialog-close-button");
        LucideIcon closeIcon = new LucideIcon("x", 14, StudioTheme.COLOR_ICON_RED);
        btnClose.setGraphic(closeIcon);
        btnClose.setOnAction(e -> close());

        header.getChildren().addAll(icon, titleLabel, spacer, btnClose);
        return header;
    }

    private VBox buildArtNetSection() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(12, 16, 12, 16));
        box.getStyleClass().add("universe-config-section");
        box.setStyle("-fx-background-color: #242424; -fx-border-color: #383838; -fx-border-width: 1px; " +
                "-fx-background-radius: 4px; -fx-border-radius: 4px;");

        Label sectionTitle = new Label("ART-NET 4 NETZWERK EINSTELLUNGEN");
        sectionTitle.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 10px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);

        // Ziel-IP
        Label lblIp = createFieldLabel("Ziel-IP-Adresse:");
        txtIp = new TextField(initialConfig.artNetIp());
        txtIp.getStyleClass().add("settings-textfield");
        grid.add(lblIp, 0, 0);
        grid.add(txtIp, 1, 0);

        // Port
        Label lblPort = createFieldLabel("UDP-Port:");
        txtPort = new TextField(String.valueOf(initialConfig.artNetPort()));
        txtPort.getStyleClass().add("settings-textfield");
        grid.add(lblPort, 2, 0);
        grid.add(txtPort, 3, 0);

        // Subnet
        Label lblSubnet = createFieldLabel("Subnet (0-15):");
        txtSubnet = new TextField(String.valueOf(initialConfig.artNetSubnet()));
        txtSubnet.getStyleClass().add("settings-textfield");
        grid.add(lblSubnet, 0, 1);
        grid.add(txtSubnet, 1, 1);

        // Universe
        Label lblUniverse = createFieldLabel("Universe (0-15):");
        txtUniverse = new TextField(String.valueOf(initialConfig.artNetUniverse()));
        txtUniverse.getStyleClass().add("settings-textfield");
        grid.add(lblUniverse, 2, 1);
        grid.add(txtUniverse, 3, 1);

        // Broadcast Checkbox
        chkBroadcast = new CheckBox("Broadcast an gesamtes Subnetz senden");
        chkBroadcast.setSelected(initialConfig.isBroadcast());
        chkBroadcast.setStyle("-fx-text-fill: #D1D5DB; -fx-font-size: 11.5px; -fx-padding: 4 0 0 0;");

        box.getChildren().addAll(sectionTitle, grid, chkBroadcast);
        return box;
    }

    private VBox buildTimingSection() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(12, 16, 12, 16));
        box.getStyleClass().add("universe-config-section");
        box.setStyle("-fx-background-color: #242424; -fx-border-color: #383838; -fx-border-width: 1px; " +
                "-fx-background-radius: 4px; -fx-border-radius: 4px;");

        Label sectionTitle = new Label("HARDWARE TIMING & ROUTING");
        sectionTitle.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 10px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);

        // DMX Rate
        Label lblRate = createFieldLabel("Ausgaberate (Hz):");
        txtRefreshRate = new TextField(String.valueOf(initialConfig.refreshRateHz()));
        txtRefreshRate.getStyleClass().add("settings-textfield");
        grid.add(lblRate, 0, 0);
        grid.add(txtRefreshRate, 1, 0);

        // Routing Info
        Label lblIn = createFieldLabel("Gewählter Input:");
        Label lblInVal = new Label(initialConfig.inputSource());
        lblInVal.setStyle("-fx-text-fill: #F3F4F6; -fx-font-size: 12px;");
        grid.add(lblIn, 0, 1);
        grid.add(lblInVal, 1, 1);

        Label lblOut = createFieldLabel("Gewählter Output:");
        Label lblOutVal = new Label(initialConfig.outputTarget());
        lblOutVal.setStyle("-fx-text-fill: #38BDF8; -fx-font-size: 12px; -fx-font-weight: 600;");
        grid.add(lblOut, 0, 2);
        grid.add(lblOutVal, 1, 2);

        box.getChildren().addAll(sectionTitle, grid);
        return box;
    }

    private HBox buildFooter() {
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(4, 0, 0, 0));

        btnCancel = new Button("Abbrechen");
        btnCancel.getStyleClass().add("settings-btn-secondary");
        btnCancel.setOnAction(e -> close());

        btnSave = new Button("Speichern");
        btnSave.getStyleClass().add("settings-btn-primary");
        btnSave.setGraphic(new LucideIcon("check", 14, StudioTheme.COLOR_ICON_GREEN));
        btnSave.setOnAction(e -> handleSave());

        footer.getChildren().addAll(btnCancel, btnSave);
        return footer;
    }

    private void handleSave() {
        try {
            int port = Integer.parseInt(txtPort.getText().trim());
            int subnet = Integer.parseInt(txtSubnet.getText().trim());
            int uni = Integer.parseInt(txtUniverse.getText().trim());
            int rate = Integer.parseInt(txtRefreshRate.getText().trim());

            UniverseConfig updated = initialConfig
                    .withArtNetIp(txtIp.getText().trim())
                    .withArtNetPort(port)
                    .withArtNetSubnet(subnet)
                    .withArtNetUniverse(uni)
                    .withBroadcast(chkBroadcast.isSelected())
                    .withRefreshRateHz(rate);

            if (onSave != null) {
                onSave.accept(updated);
            }
            close();
        } catch (NumberFormatException ex) {
            // Bei ungültigen Ziffern das IP/Port Feld markieren
            txtPort.setStyle("-fx-border-color: #EF4444;");
        }
    }

    private void close() {
        if (mainWindow != null) {
            mainWindow.hideOverlay();
        }
    }

    private Label createFieldLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #D1D5DB; -fx-font-size: 12px;");
        return lbl;
    }

    public Node getView() {
        return card;
    }

    public Label getTitleLabel() {
        return titleLabel;
    }

    public TextField getTxtIp() {
        return txtIp;
    }

    public TextField getTxtPort() {
        return txtPort;
    }

    public TextField getTxtSubnet() {
        return txtSubnet;
    }

    public TextField getTxtUniverse() {
        return txtUniverse;
    }

    public CheckBox getChkBroadcast() {
        return chkBroadcast;
    }

    public TextField getTxtRefreshRate() {
        return txtRefreshRate;
    }

    public Button getBtnCancel() {
        return btnCancel;
    }

    public Button getBtnSave() {
        return btnSave;
    }

    public Button getBtnClose() {
        return btnClose;
    }
}

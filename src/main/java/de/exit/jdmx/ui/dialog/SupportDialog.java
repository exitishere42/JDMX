package de.exit.jdmx.ui.dialog;

import de.exit.jdmx.core.i18n.I18n;
import de.exit.jdmx.core.log.AppLogger;
import de.exit.jdmx.service.UpdateService;
import de.exit.jdmx.service.support.SupportCategory;
import de.exit.jdmx.service.support.SupportMessage;
import de.exit.jdmx.service.support.SupportService;
import de.exit.jdmx.service.support.SupportSystemInfo;
import de.exit.jdmx.ui.MainWindow;
import de.exit.jdmx.ui.icon.LucideIcon;
import de.exit.jdmx.ui.theme.StudioTheme;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Modaler In-App Dialog für Support, Feedback und Bug-Reporting ("Fenster im Fenster").
 * <p>
 * Bietet eine aufgeräumte Studio-Oberfläche mit dynamischem Info-Banner, Underline-Tab-Navigation
 * (Feedback & Bugs), klarer Formular-Eingabe und einer Historien-Liste übermittelter Berichte.
 * </p>
 */
public class SupportDialog {

    private static final Logger LOGGER = Logger.getLogger(SupportDialog.class.getName());
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault());

    /** Weiches, lesbares Rot für Akzente ("weißes Rot" / Tailwind Red-400). */
    public static final Color COLOR_ACCENT_RED = Color.web("#F87171");

    /** JDMX Helles Logo-Gelb (#FEDC94). */
    public static final Color COLOR_ACCENT_YELLOW = Color.web("#FEDC94");

    private final MainWindow mainWindow;
    private final SupportService supportService;
    private final VBox card;

    private Label lblTitle;
    private Button btnClose;
    private Button btnTabFeedback;
    private Button btnTabBugs;

    private LucideIcon iconBanner;
    private Label lblBanner;

    private TextField txtName;
    private TextField txtContact;
    private CheckBox cbIncludeDiagnostics;
    private TextArea txtMessage;
    private Label lblStatus;
    private Button btnSend;

    private Label lblHistoryTitle;
    private VBox historyContainer;
    private ScrollPane historyScrollPane;

    private SupportCategory activeCategory = SupportCategory.FEEDBACK;

    /**
     * Öffnet den Support-Dialog als modales In-App Overlay über dem Hauptfenster.
     *
     * @param mainWindow Hauptfenster
     */
    public static void show(MainWindow mainWindow) {
        SupportService service = (mainWindow != null && mainWindow.getSupportService() != null)
                ? mainWindow.getSupportService()
                : new SupportService();

        SupportDialog dialog = new SupportDialog(mainWindow, service);
        if (mainWindow != null) {
            mainWindow.showOverlay(dialog.getView());
        }
    }

    /**
     * Erstellt einen neuen Support-Dialog.
     *
     * @param mainWindow     Hauptfenster (oder null im Testbetrieb)
     * @param supportService Service für den Nachrichtenversand
     */
    public SupportDialog(MainWindow mainWindow, SupportService supportService) {
        this.mainWindow = mainWindow;
        this.supportService = Objects.requireNonNull(supportService, "SupportService darf nicht null sein");

        this.card = new VBox(12);
        card.getStyleClass().add("support-dialog-card");
        card.setMinWidth(520);
        card.setPrefWidth(560);
        card.setMaxWidth(600);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        buildUi();
        refreshHistoryView();

        // Dynamischer Sprachwechsel
        I18n.addLanguageChangeListener(lang -> {
            if (Platform.isFxApplicationThread()) {
                updateLocalization();
            } else {
                Platform.runLater(this::updateLocalization);
            }
        });
    }

    /**
     * Liefert den Wurzelknoten der Dialog-Karte.
     *
     * @return Dialog-Node
     */
    public Node getView() {
        return card;
    }

    private void buildUi() {
        // 1. Header (Icon + Titel + Schließen-Button)
        HBox header = buildHeader();

        // 2. Underline-Tab-Leiste (Feedback | Bugs)
        HBox tabBar = buildUnderlineTabBar();

        // 3. Info-Banner (erklärt die aktive Sektion)
        HBox infoBanner = buildInfoBanner();

        // 4. Formular- & Eingabebereich
        VBox inputSection = buildInputSection();

        // 5. Historien-Bereich (übermittelte Berichte dieser Sitzung)
        VBox historySection = buildHistorySection();

        card.getChildren().addAll(header, tabBar, infoBanner, inputSection, historySection);
    }

    private HBox buildHeader() {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        LucideIcon icon = new LucideIcon("life-buoy", 20, COLOR_ACCENT_RED);

        this.lblTitle = new Label(I18n.get("support.title"));
        lblTitle.getStyleClass().add("dialog-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        this.btnClose = new Button();
        btnClose.getStyleClass().add("modal-close-btn");
        btnClose.setGraphic(new LucideIcon("x", 14, Color.web("#EF4444")));
        btnClose.setCursor(Cursor.HAND);
        btnClose.setOnAction(e -> close());

        header.getChildren().addAll(icon, lblTitle, spacer, btnClose);
        return header;
    }

    private HBox buildUnderlineTabBar() {
        HBox tabBar = new HBox(12);
        tabBar.setAlignment(Pos.CENTER_LEFT);
        tabBar.getStyleClass().add("dialog-underline-tab-bar");

        btnTabFeedback = new Button(I18n.get("support.tab.feedback"));
        btnTabFeedback.getStyleClass().addAll("dialog-underline-tab", "active");
        btnTabFeedback.setGraphic(new LucideIcon("sparkles", 14, COLOR_ACCENT_YELLOW));
        btnTabFeedback.setCursor(Cursor.HAND);
        btnTabFeedback.setOnAction(e -> switchTab(SupportCategory.FEEDBACK));

        btnTabBugs = new Button(I18n.get("support.tab.bugs"));
        btnTabBugs.getStyleClass().add("dialog-underline-tab");
        btnTabBugs.setGraphic(new LucideIcon("alert-triangle", 14, Color.web("#9CA3AF")));
        btnTabBugs.setCursor(Cursor.HAND);
        btnTabBugs.setOnAction(e -> switchTab(SupportCategory.BUG));

        tabBar.getChildren().addAll(btnTabFeedback, btnTabBugs);
        return tabBar;
    }

    private HBox buildInfoBanner() {
        HBox banner = new HBox(10);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.getStyleClass().add("support-info-banner");

        this.iconBanner = new LucideIcon("sparkles", 16, COLOR_ACCENT_YELLOW);

        this.lblBanner = new Label(I18n.get("support.banner.feedback"));
        lblBanner.setWrapText(true);
        lblBanner.getStyleClass().add("support-banner-text");
        HBox.setHgrow(lblBanner, Priority.ALWAYS);

        banner.getChildren().addAll(iconBanner, lblBanner);
        return banner;
    }

    private VBox buildInputSection() {
        VBox box = new VBox(8);

        // Zeile 1: Name & Kontaktfelder
        HBox contactRow = new HBox(8);
        this.txtName = new TextField();
        txtName.setPromptText(I18n.get("support.field.name"));
        txtName.getStyleClass().add("support-input-text");
        HBox.setHgrow(txtName, Priority.ALWAYS);

        this.txtContact = new TextField();
        txtContact.setPromptText(I18n.get("support.field.contact"));
        txtContact.getStyleClass().add("support-input-text");
        HBox.setHgrow(txtContact, Priority.ALWAYS);

        contactRow.getChildren().addAll(txtName, txtContact);

        // Optionale Checkbox für Systeminfos (nur bei Bug aktiv sichtbar)
        this.cbIncludeDiagnostics = new CheckBox(I18n.get("support.field.sysinfo_checkbox"));
        cbIncludeDiagnostics.setSelected(true);
        cbIncludeDiagnostics.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;");
        cbIncludeDiagnostics.setVisible(false);
        cbIncludeDiagnostics.setManaged(false);

        // Zeile 2: Mehrzeiliges Eingabefeld
        this.txtMessage = new TextArea();
        txtMessage.setPromptText(I18n.get("support.field.message"));
        txtMessage.getStyleClass().add("support-textarea");
        txtMessage.setPrefRowCount(3);
        txtMessage.setWrapText(true);

        // Zeile 3: Status & Sende-Button
        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        this.lblStatus = new Label();
        lblStatus.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;");
        lblStatus.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(lblStatus, Priority.ALWAYS);

        this.btnSend = new Button(I18n.get("support.btn.send"));
        btnSend.getStyleClass().add("settings-btn-primary");
        btnSend.setGraphic(new LucideIcon("send", 14, StudioTheme.COLOR_ICON_GREEN));
        btnSend.setCursor(Cursor.HAND);
        btnSend.setOnAction(e -> handleSendMessage());

        actionRow.getChildren().addAll(lblStatus, btnSend);

        box.getChildren().addAll(contactRow, cbIncludeDiagnostics, txtMessage, actionRow);
        return box;
    }

    private VBox buildHistorySection() {
        VBox box = new VBox(6);

        this.lblHistoryTitle = new Label(I18n.get("support.history.title"));
        lblHistoryTitle.getStyleClass().add("support-history-title");

        this.historyContainer = new VBox(6);
        historyContainer.setPadding(new Insets(4, 2, 4, 2));

        this.historyScrollPane = new ScrollPane(historyContainer);
        historyScrollPane.getStyleClass().add("support-history-scroll");
        historyScrollPane.setMinHeight(85);
        historyScrollPane.setPrefHeight(105);
        historyScrollPane.setMaxHeight(125);
        historyScrollPane.setFitToWidth(true);
        historyScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        historyScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        box.getChildren().addAll(lblHistoryTitle, historyScrollPane);
        return box;
    }

    /**
     * Schaltet zwischen Feedback- und Bug-Tab um.
     *
     * @param category Ziel-Kategorie
     */
    public void switchTab(SupportCategory category) {
        this.activeCategory = category;

        if (category == SupportCategory.FEEDBACK) {
            btnTabFeedback.getStyleClass().add("active");
            btnTabBugs.getStyleClass().remove("active");
            ((LucideIcon) btnTabFeedback.getGraphic()).setColor(COLOR_ACCENT_YELLOW);
            ((LucideIcon) btnTabBugs.getGraphic()).setColor(Color.web("#9CA3AF"));

            iconBanner.setIcon("sparkles", COLOR_ACCENT_YELLOW);
            lblBanner.setText(I18n.get("support.banner.feedback"));

            cbIncludeDiagnostics.setVisible(false);
            cbIncludeDiagnostics.setManaged(false);
        } else {
            btnTabBugs.getStyleClass().add("active");
            btnTabFeedback.getStyleClass().remove("active");
            ((LucideIcon) btnTabBugs.getGraphic()).setColor(COLOR_ACCENT_RED);
            ((LucideIcon) btnTabFeedback.getGraphic()).setColor(Color.web("#9CA3AF"));

            iconBanner.setIcon("alert-triangle", COLOR_ACCENT_RED);
            lblBanner.setText(I18n.get("support.banner.bugs"));

            cbIncludeDiagnostics.setVisible(true);
            cbIncludeDiagnostics.setManaged(true);
        }

        lblStatus.setText("");
        refreshHistoryView();
    }

    /**
     * Baut die Verlaufsansicht übermittelter Berichte für die aktive Kategorie neu auf.
     */
    private void refreshHistoryView() {
        historyContainer.getChildren().clear();

        List<SupportMessage> history = supportService.getHistory(activeCategory);
        if (history.isEmpty()) {
            Label emptyLbl = new Label(I18n.get("support.history.empty"));
            emptyLbl.setStyle("-fx-text-fill: #71717A; -fx-font-style: italic; -fx-font-size: 11px;");
            historyContainer.getChildren().add(emptyLbl);
        } else {
            for (SupportMessage msg : history) {
                historyContainer.getChildren().add(createHistoryCard(msg));
            }
        }

        // Automatisch nach unten scrollen
        Platform.runLater(() -> historyScrollPane.setVvalue(1.0));
    }

    private Node createHistoryCard(SupportMessage msg) {
        VBox card = new VBox(4);
        card.getStyleClass().add("support-history-card");

        HBox topRow = new HBox(6);
        topRow.setAlignment(Pos.CENTER_LEFT);

        LucideIcon checkIcon = new LucideIcon("check", 12, Color.web("#10B981"));
        Label lblBadge = new Label(I18n.get("support.history.sent"));
        lblBadge.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold; -fx-font-size: 10px;");

        Label lblDot = new Label("•");
        lblDot.setStyle("-fx-text-fill: #71717A; -fx-font-size: 10px;");

        String timeStr = TIME_FORMATTER.format(msg.timestamp());
        Label lblTime = new Label(timeStr);
        lblTime.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 10px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblAuthor = new Label(msg.senderName());
        lblAuthor.setStyle("-fx-text-fill: #E4E4E7; -fx-font-weight: bold; -fx-font-size: 11px;");

        topRow.getChildren().addAll(checkIcon, lblBadge, lblDot, lblTime, spacer, lblAuthor);

        if (msg.systemInfo() != null) {
            Label lblFile = new Label("📄 jdmx-diagnostic.log");
            lblFile.setStyle("-fx-text-fill: #FEDC94; -fx-font-size: 10px; -fx-background-color: rgba(254, 220, 148, 0.12); -fx-background-radius: 4px; -fx-padding: 2px 7px;");
            topRow.getChildren().add(lblFile);
        }

        Label lblContent = new Label(msg.messageText());
        lblContent.setWrapText(true);
        lblContent.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 12px;");

        card.getChildren().addAll(topRow, lblContent);

        return card;
    }

    private void handleSendMessage() {
        String text = txtMessage.getText();
        if (text == null || text.isBlank()) {
            lblStatus.setStyle("-fx-text-fill: #F87171; -fx-font-size: 11px;");
            lblStatus.setText(I18n.get("support.status.empty"));
            return;
        }

        String name = txtName.getText();
        String contact = txtContact.getText();

        SupportSystemInfo sysInfo = null;
        if (activeCategory == SupportCategory.BUG && cbIncludeDiagnostics.isSelected()) {
            int dmxRate = (mainWindow != null && mainWindow.getDmxService() != null)
                    ? mainWindow.getDmxService().getCurrentRate()
                    : 0;
            String recentLogs = AppLogger.getRecentLogs(200);
            if (recentLogs == null || recentLogs.isBlank()) {
                recentLogs = "=== JDMX Diagnostic Log Export ===\n" +
                        "Zeitstempel: " + java.time.LocalDateTime.now() + "\n" +
                        "Betriebssystem: " + System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")\n" +
                        "Java: " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")\n" +
                        "JDMX Version: v" + UpdateService.CURRENT_VERSION + "\n" +
                        "DMX Framerate: " + dmxRate + " DMX/s\n" +
                        "Speicher (Max/Frei): " + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + " MB / " + (Runtime.getRuntime().freeMemory() / 1024 / 1024) + " MB\n" +
                        "Hinweis: Keine weiteren DMX-Ereignisse im aktuellen Logfile verzeichnet.\n";
            }
            sysInfo = supportService.collectSystemInfo(dmxRate, recentLogs);
        }

        SupportMessage message = SupportMessage.create(activeCategory, name, contact, text, sysInfo);

        // UI vorbereiten & sperren
        btnSend.setDisable(true);
        lblStatus.setStyle("-fx-text-fill: #FEDC94; -fx-font-size: 11px;");
        lblStatus.setText(I18n.get("support.status.sending"));

        // Asynchron über SupportService senden
        supportService.sendMessage(message).thenAccept(success -> Platform.runLater(() -> {
            btnSend.setDisable(false);
            if (Boolean.TRUE.equals(success)) {
                lblStatus.setStyle("-fx-text-fill: #10B981; -fx-font-size: 11px;");
                lblStatus.setText(I18n.get("support.status.sent"));
                txtMessage.clear();
                refreshHistoryView();
            } else {
                lblStatus.setStyle("-fx-text-fill: #F87171; -fx-font-size: 11px;");
                lblStatus.setText(I18n.get("support.status.failed"));
            }
        })).exceptionally(ex -> {
            Platform.runLater(() -> {
                btnSend.setDisable(false);
                lblStatus.setStyle("-fx-text-fill: #F87171; -fx-font-size: 11px;");
                lblStatus.setText(I18n.get("support.status.failed"));
            });
            return null;
        });
    }

    private void updateLocalization() {
        lblTitle.setText(I18n.get("support.title"));
        btnTabFeedback.setText(I18n.get("support.tab.feedback"));
        btnTabBugs.setText(I18n.get("support.tab.bugs"));
        lblBanner.setText(I18n.get(activeCategory == SupportCategory.BUG ? "support.banner.bugs" : "support.banner.feedback"));
        lblHistoryTitle.setText(I18n.get("support.history.title"));
        txtName.setPromptText(I18n.get("support.field.name"));
        txtContact.setPromptText(I18n.get("support.field.contact"));
        cbIncludeDiagnostics.setText(I18n.get("support.field.sysinfo_checkbox"));
        txtMessage.setPromptText(I18n.get("support.field.message"));
        btnSend.setText(I18n.get("support.btn.send"));
        refreshHistoryView();
    }

    /**
     * Schließt das Support-Overlay.
     */
    public void close() {
        if (mainWindow != null) {
            mainWindow.hideOverlay();
        }
    }

    // --- Getter für Unit-Tests ---

    public Button getBtnTabFeedback() {
        return btnTabFeedback;
    }

    public Button getBtnTabBugs() {
        return btnTabBugs;
    }

    public Button getBtnClose() {
        return btnClose;
    }

    public TextField getTxtName() {
        return txtName;
    }

    public TextField getTxtContact() {
        return txtContact;
    }

    public TextArea getTxtMessage() {
        return txtMessage;
    }

    public CheckBox getCbIncludeDiagnostics() {
        return cbIncludeDiagnostics;
    }

    public Label getLblStatus() {
        return lblStatus;
    }

    public Button getBtnSend() {
        return btnSend;
    }

    public SupportCategory getActiveCategory() {
        return activeCategory;
    }

    public Label getLblBanner() {
        return lblBanner;
    }

    public Label getLblHistoryTitle() {
        return lblHistoryTitle;
    }
}

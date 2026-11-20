package de.exit.jdmx.ui.dialog;

import de.exit.jdmx.core.i18n.I18n;
import de.exit.jdmx.core.log.AppLogger;
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
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Modaler In-App Dialog für Support, Feedback und Bug-Reporting ("Fenster im Fenster").
 * <p>
 * Bietet ein elegantes Chat-Interface mit Underline-Tab-Navigation (Feedback & Bugs),
 * Live-Validierung und direkter asynchroner Übertragung an das Entwicklerteam via Discord-Webhook.
 * </p>
 */
public class SupportDialog {

    private static final Logger LOGGER = Logger.getLogger(SupportDialog.class.getName());
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault());

    private final MainWindow mainWindow;
    private final SupportService supportService;
    private final VBox card;

    private Label lblTitle;
    private Button btnClose;
    private Button btnTabFeedback;
    private Button btnTabBugs;

    private VBox chatContainer;
    private ScrollPane chatScrollPane;

    private TextField txtName;
    private TextField txtContact;
    private CheckBox cbIncludeDiagnostics;
    private TextArea txtMessage;
    private Label lblStatus;
    private Button btnSend;

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
        refreshChatView();

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

        // 3. Chat-Verlauf
        this.chatContainer = new VBox(10);
        chatContainer.setPadding(new Insets(10));

        this.chatScrollPane = new ScrollPane(chatContainer);
        chatScrollPane.getStyleClass().add("support-chat-scroll");
        chatScrollPane.setMinHeight(200);
        chatScrollPane.setPrefHeight(220);
        chatScrollPane.setMaxHeight(240);
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        chatScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // 4. Formular- & Eingabebereich
        VBox inputSection = buildInputSection();

        card.getChildren().addAll(header, tabBar, chatScrollPane, inputSection);
    }

    private HBox buildHeader() {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        LucideIcon icon = new LucideIcon("life-buoy", 20, Color.web("#38BDF8"));

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
        btnTabFeedback.setGraphic(new LucideIcon("sparkles", 14, Color.web("#38BDF8")));
        btnTabFeedback.setCursor(Cursor.HAND);
        btnTabFeedback.setOnAction(e -> switchTab(SupportCategory.FEEDBACK));

        btnTabBugs = new Button(I18n.get("support.tab.bugs"));
        btnTabBugs.getStyleClass().add("dialog-underline-tab");
        btnTabBugs.setGraphic(new LucideIcon("bug", 14, Color.web("#9CA3AF")));
        btnTabBugs.setCursor(Cursor.HAND);
        btnTabBugs.setOnAction(e -> switchTab(SupportCategory.BUG));

        tabBar.getChildren().addAll(btnTabFeedback, btnTabBugs);
        return tabBar;
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
        btnSend.getStyleClass().add("primary-button");
        btnSend.setGraphic(new LucideIcon("send", 14, Color.web("#18181B")));
        btnSend.setCursor(Cursor.HAND);
        btnSend.setOnAction(e -> handleSendMessage());

        actionRow.getChildren().addAll(lblStatus, btnSend);

        box.getChildren().addAll(contactRow, cbIncludeDiagnostics, txtMessage, actionRow);
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
            ((LucideIcon) btnTabFeedback.getGraphic()).setColor(Color.web("#38BDF8"));
            ((LucideIcon) btnTabBugs.getGraphic()).setColor(Color.web("#9CA3AF"));

            cbIncludeDiagnostics.setVisible(false);
            cbIncludeDiagnostics.setManaged(false);
        } else {
            btnTabBugs.getStyleClass().add("active");
            btnTabFeedback.getStyleClass().remove("active");
            ((LucideIcon) btnTabBugs.getGraphic()).setColor(Color.web("#EF4444"));
            ((LucideIcon) btnTabFeedback.getGraphic()).setColor(Color.web("#9CA3AF"));

            cbIncludeDiagnostics.setVisible(true);
            cbIncludeDiagnostics.setManaged(true);
        }

        lblStatus.setText("");
        refreshChatView();
    }

    /**
     * Baut den Chatverlauf für die aktive Kategorie neu auf.
     */
    private void refreshChatView() {
        chatContainer.getChildren().clear();

        // 1. Willkommensnachricht vom Bot
        String greetingKey = (activeCategory == SupportCategory.BUG)
                ? "support.chat.greeting_bugs"
                : "support.chat.greeting_feedback";
        Node botBubble = createSystemBubble(I18n.get(greetingKey));
        chatContainer.getChildren().add(botBubble);

        // 2. Bisherige Nachrichten der Kategorie
        for (SupportMessage msg : supportService.getHistory(activeCategory)) {
            chatContainer.getChildren().add(createUserBubble(msg));
        }

        // Automatisch nach unten scrollen
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }

    private Node createSystemBubble(String text) {
        VBox bubble = new VBox(4);
        bubble.getStyleClass().add("support-bubble-system");

        HBox authorRow = new HBox(6);
        authorRow.setAlignment(Pos.CENTER_LEFT);
        LucideIcon botIcon = new LucideIcon("life-buoy", 14, Color.web("#38BDF8"));
        Label lblAuthor = new Label(I18n.get("support.chat.bot_name"));
        lblAuthor.setStyle("-fx-text-fill: #38BDF8; -fx-font-weight: bold; -fx-font-size: 11px;");
        authorRow.getChildren().addAll(botIcon, lblAuthor);

        Label lblContent = new Label(text);
        lblContent.setWrapText(true);
        lblContent.setStyle("-fx-text-fill: #E4E4E7; -fx-font-size: 12px;");

        bubble.getChildren().addAll(authorRow, lblContent);
        return bubble;
    }

    private Node createUserBubble(SupportMessage msg) {
        VBox bubble = new VBox(4);
        bubble.getStyleClass().add("support-bubble-user");

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label lblAuthor = new Label(msg.senderName());
        lblAuthor.setStyle("-fx-text-fill: #38BDF8; -fx-font-weight: bold; -fx-font-size: 11px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String timeStr = TIME_FORMATTER.format(msg.timestamp());
        Label lblTime = new Label(timeStr);
        lblTime.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 10px;");

        topRow.getChildren().addAll(lblAuthor, spacer, lblTime);

        Label lblContent = new Label(msg.messageText());
        lblContent.setWrapText(true);
        lblContent.setStyle("-fx-text-fill: #F3F4F6; -fx-font-size: 12px;");

        bubble.getChildren().addAll(topRow, lblContent);
        return bubble;
    }

    private void handleSendMessage() {
        String text = txtMessage.getText();
        if (text == null || text.isBlank()) {
            lblStatus.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 11px;");
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
            String recentLogs = AppLogger.getRecentLogs(25);
            sysInfo = supportService.collectSystemInfo(dmxRate, recentLogs);
        }

        SupportMessage message = SupportMessage.create(activeCategory, name, contact, text, sysInfo);

        // UI vorbereiten & sperren
        btnSend.setDisable(true);
        lblStatus.setStyle("-fx-text-fill: #38BDF8; -fx-font-size: 11px;");
        lblStatus.setText(I18n.get("support.status.sending"));

        // Asynchron über SupportService senden
        supportService.sendMessage(message).thenAccept(success -> Platform.runLater(() -> {
            btnSend.setDisable(false);
            if (Boolean.TRUE.equals(success)) {
                lblStatus.setStyle("-fx-text-fill: #10B981; -fx-font-size: 11px;");
                lblStatus.setText(I18n.get("support.status.sent"));
                txtMessage.clear();
                refreshChatView();
            } else {
                lblStatus.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 11px;");
                lblStatus.setText(I18n.get("support.status.failed"));
            }
        })).exceptionally(ex -> {
            Platform.runLater(() -> {
                btnSend.setDisable(false);
                lblStatus.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 11px;");
                lblStatus.setText(I18n.get("support.status.failed"));
            });
            return null;
        });
    }

    private void updateLocalization() {
        lblTitle.setText(I18n.get("support.title"));
        btnTabFeedback.setText(I18n.get("support.tab.feedback"));
        btnTabBugs.setText(I18n.get("support.tab.bugs"));
        txtName.setPromptText(I18n.get("support.field.name"));
        txtContact.setPromptText(I18n.get("support.field.contact"));
        cbIncludeDiagnostics.setText(I18n.get("support.field.sysinfo_checkbox"));
        txtMessage.setPromptText(I18n.get("support.field.message"));
        btnSend.setText(I18n.get("support.btn.send"));
        refreshChatView();
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
}

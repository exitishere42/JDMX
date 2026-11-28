package de.exit.jdmx.ui.dialog;

import de.exit.jdmx.core.i18n.I18n;
import de.exit.jdmx.core.model.UpdateChannel;
import de.exit.jdmx.service.UpdateService;
import de.exit.jdmx.ui.MainWindow;
import de.exit.jdmx.ui.component.MarkdownView;
import de.exit.jdmx.ui.icon.LucideIcon;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.awt.Desktop;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * In-App Modal Dialog für das Prüfen und Einspielen von Software-Updates.
 * <p>
 * Zeigt die aktuell installierte Version, erlaubt das Umschalten zwischen
 * stabilen Releases und kontinuierlichen Entwicklungs-Snapshots,
 * ruft asynchron die Daten von GitHub ab und erlaubt ein nahtloses In-App-Update
 * ohne manuellen Installer.
 * </p>
 */
public class AboutUpdateDialog {

    private static final Logger LOGGER = Logger.getLogger(AboutUpdateDialog.class.getName());

    private final MainWindow mainWindow;
    private final VBox card;

    private UpdateChannel currentChannel;
    private Label lblTitle;
    private Label lblChannel;
    private Button btnChannelRelease;
    private Button btnChannelSnapshot;
    private Label lblCurTitle;
    private Label lblLatTitle;
    private Label lblCurrentVersion;
    private Label lblLatestVersion;
    private Label lblStatus;
    private ProgressIndicator spinner;
    private MarkdownView markdownChangelog;
    private ProgressBar progressBar;
    private Label lblProgressDetail;
    private HBox actionButtonBox;
    private Button btnClose;
    private Button btnCloseFooter;
    private Button btnUpdate;
    private Button btnRetry;
    private UpdateService.ReleaseInfo lastRelease;
    private boolean isChecking = true;
    private boolean isError = false;

    /**
     * Öffnet den Update-Dialog als modales In-App Overlay.
     *
     * @param mainWindow Hauptfenster
     */
    public static void show(MainWindow mainWindow) {
        AboutUpdateDialog dialog = new AboutUpdateDialog(mainWindow);
        if (mainWindow != null) {
            mainWindow.showOverlay(dialog.getView());
        }
    }

    public AboutUpdateDialog(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        if (mainWindow != null && mainWindow.getSettingsService() != null) {
            this.currentChannel = mainWindow.getSettingsService().getUpdateChannel();
        } else {
            this.currentChannel = UpdateService.isSnapshot() ? UpdateChannel.SNAPSHOT : UpdateChannel.RELEASE;
        }

        this.card = new VBox(14);
        card.getStyleClass().add("settings-dialog-card");
        card.setMinWidth(520);
        card.setPrefWidth(550);
        card.setMaxWidth(580);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        buildUi();
        I18n.addLanguageChangeListener(lang -> updateTexts());
        checkForUpdates();
    }

    private void buildUi() {
        // 1. Header (Icon, Titel, roter Schließen-Button)
        HBox header = buildHeader();

        // 2. Kanal-Umschalter (Release vs. Snapshot)
        HBox channelCard = buildChannelCard();

        // 3. Versions-Kachel (Installiert vs. Neueste Version)
        HBox versionCard = buildVersionCard();

        // 4. Status & Update-Bereich
        VBox statusCard = buildStatusCard();

        // 5. Footer (GitHub Link & Schließen)
        HBox footer = buildFooter();

        card.getChildren().addAll(header, channelCard, versionCard, statusCard, footer);
        updateTexts();
    }

    private HBox buildHeader() {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("settings-dialog-header");

        LucideIcon iconLogo = new LucideIcon("arrow-up-circle", 20, Color.web("#38BDF8"));
        this.lblTitle = new Label(I18n.get("update.title"));
        lblTitle.getStyleClass().add("settings-dialog-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        this.btnClose = new Button();
        btnClose.getStyleClass().add("modal-close-btn");
        LucideIcon closeIcon = new LucideIcon("x", 14, Color.web("#EF4444"));
        btnClose.setGraphic(closeIcon);
        btnClose.setOnAction(e -> close());

        header.getChildren().addAll(iconLogo, lblTitle, spacer, btnClose);
        return header;
    }

    private HBox buildChannelCard() {
        HBox channelCard = new HBox(12);
        channelCard.setAlignment(Pos.CENTER_LEFT);
        channelCard.setPadding(new Insets(10, 16, 10, 16));
        channelCard.setStyle("-fx-background-color: #18181B; -fx-border-color: #27272A; -fx-border-width: 1px; -fx-background-radius: 4px; -fx-border-radius: 4px;");

        this.lblChannel = new Label(I18n.get("update.channel"));
        lblChannel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toggleGroup = new HBox(0);
        toggleGroup.setAlignment(Pos.CENTER_RIGHT);

        this.btnChannelRelease = new Button(I18n.get("update.channel.release"));
        this.btnChannelSnapshot = new Button(I18n.get("update.channel.snapshot"));

        btnChannelRelease.setOnAction(e -> selectChannel(UpdateChannel.RELEASE));
        btnChannelSnapshot.setOnAction(e -> selectChannel(UpdateChannel.SNAPSHOT));

        toggleGroup.getChildren().addAll(btnChannelRelease, btnChannelSnapshot);
        updateChannelToggleStyles();

        channelCard.getChildren().addAll(lblChannel, spacer, toggleGroup);
        return channelCard;
    }

    private void updateChannelToggleStyles() {
        String activeLeft = "-fx-background-color: #38BDF8; -fx-text-fill: #0F172A; -fx-font-weight: bold; -fx-font-size: 11px; -fx-background-radius: 4px 0 0 4px; -fx-border-color: #38BDF8; -fx-border-radius: 4px 0 0 4px; -fx-padding: 5 14; -fx-cursor: hand;";
        String inactiveLeft = "-fx-background-color: #27272A; -fx-text-fill: #9CA3AF; -fx-font-size: 11px; -fx-background-radius: 4px 0 0 4px; -fx-border-color: #3F3F46; -fx-border-radius: 4px 0 0 4px; -fx-padding: 5 14; -fx-cursor: hand;";

        String activeRight = "-fx-background-color: #38BDF8; -fx-text-fill: #0F172A; -fx-font-weight: bold; -fx-font-size: 11px; -fx-background-radius: 0 4px 4px 0; -fx-border-color: #38BDF8; -fx-border-radius: 0 4px 4px 0; -fx-padding: 5 14; -fx-cursor: hand;";
        String inactiveRight = "-fx-background-color: #27272A; -fx-text-fill: #9CA3AF; -fx-font-size: 11px; -fx-background-radius: 0 4px 4px 0; -fx-border-color: #3F3F46; -fx-border-radius: 0 4px 4px 0; -fx-padding: 5 14; -fx-cursor: hand;";

        if (currentChannel == UpdateChannel.RELEASE) {
            btnChannelRelease.setStyle(activeLeft);
            btnChannelSnapshot.setStyle(inactiveRight);
        } else {
            btnChannelRelease.setStyle(inactiveLeft);
            btnChannelSnapshot.setStyle(activeRight);
        }
    }

    private void selectChannel(UpdateChannel newChannel) {
        if (newChannel == null || newChannel == this.currentChannel) {
            return;
        }
        this.currentChannel = newChannel;
        updateChannelToggleStyles();

        if (mainWindow != null && mainWindow.getSettingsService() != null) {
            mainWindow.getSettingsService().setUpdateChannel(newChannel);
            mainWindow.getSettingsService().saveGlobalSettings(null);
        }

        checkForUpdates();
    }

    private HBox buildVersionCard() {
        HBox versionCard = new HBox(16);
        versionCard.setAlignment(Pos.CENTER_LEFT);
        versionCard.setPadding(new Insets(12, 16, 12, 16));
        versionCard.setStyle("-fx-background-color: #18181B; -fx-border-color: #27272A; -fx-border-width: 1px; -fx-background-radius: 4px; -fx-border-radius: 4px;");

        // Installierte Version
        VBox curBox = new VBox(2);
        this.lblCurTitle = new Label(I18n.get("update.installed_version"));
        lblCurTitle.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 10px; -fx-font-weight: bold;");
        this.lblCurrentVersion = new Label("v" + UpdateService.CURRENT_VERSION);
        lblCurrentVersion.setStyle("-fx-text-fill: #F3F4F6; -fx-font-size: 14px; -fx-font-weight: bold;");
        curBox.getChildren().addAll(lblCurTitle, lblCurrentVersion);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Neueste Version auf GitHub
        VBox latBox = new VBox(2);
        latBox.setAlignment(Pos.CENTER_RIGHT);
        this.lblLatTitle = new Label(currentChannel == UpdateChannel.SNAPSHOT ? I18n.get("update.latest_snapshot") : I18n.get("update.latest_release"));
        lblLatTitle.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 10px; -fx-font-weight: bold;");
        this.lblLatestVersion = new Label(I18n.get("update.checking"));
        lblLatestVersion.setStyle("-fx-text-fill: #38BDF8; -fx-font-size: 14px; -fx-font-weight: bold;");
        latBox.getChildren().addAll(lblLatTitle, lblLatestVersion);

        versionCard.getChildren().addAll(curBox, spacer, latBox);
        return versionCard;
    }

    private VBox buildStatusCard() {
        VBox statusCard = new VBox(10);
        statusCard.setPadding(new Insets(12, 16, 12, 16));
        statusCard.setStyle("-fx-background-color: #18181B; -fx-border-color: #27272A; -fx-border-width: 1px; -fx-background-radius: 4px; -fx-border-radius: 4px;");

        // Status-Zeile mit Spinner
        HBox statusRow = new HBox(10);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        this.spinner = new ProgressIndicator();
        spinner.setMaxSize(16, 16);
        spinner.setMinSize(16, 16);
        spinner.setPrefSize(16, 16);
        spinner.setStyle("-fx-progress-color: #38BDF8;");

        this.lblStatus = new Label(I18n.get("update.checking_status"));
        lblStatus.setStyle("-fx-text-fill: #F3F4F6; -fx-font-size: 12px;");
        statusRow.getChildren().addAll(spinner, lblStatus);

        // Changelog / Release Notes als native formatierte MarkdownView
        this.markdownChangelog = new MarkdownView();
        markdownChangelog.setMaxHeight(200);
        markdownChangelog.setPrefHeight(180);
        markdownChangelog.setVisible(false);
        markdownChangelog.setManaged(false);

        // Fortschrittsbalken für Download
        this.progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: #38BDF8;");
        progressBar.setVisible(false);
        progressBar.setManaged(false);

        this.lblProgressDetail = new Label();
        lblProgressDetail.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;");
        lblProgressDetail.setVisible(false);
        lblProgressDetail.setManaged(false);

        // Aktionsbuttons
        this.actionButtonBox = new HBox(8);
        actionButtonBox.setAlignment(Pos.CENTER_RIGHT);

        statusCard.getChildren().addAll(statusRow, markdownChangelog, progressBar, lblProgressDetail, actionButtonBox);
        return statusCard;
    }

    private HBox buildFooter() {
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);

        Button btnGithub = new Button("GitHub");
        btnGithub.getStyleClass().add("patch-cancel-btn");
        btnGithub.setGraphic(new LucideIcon("external-link", 13, Color.web("#9CA3AF")));
        btnGithub.setOnAction(e -> openUrl(UpdateService.REPO_URL));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        this.btnCloseFooter = new Button(I18n.get("action.close"));
        btnCloseFooter.getStyleClass().add("patch-cancel-btn");
        btnCloseFooter.setOnAction(e -> close());

        footer.getChildren().addAll(btnGithub, spacer, btnCloseFooter);
        return footer;
    }

    private void updateTexts() {
        if (lblTitle != null) lblTitle.setText(I18n.get("update.title"));
        if (lblChannel != null) lblChannel.setText(I18n.get("update.channel"));
        if (btnChannelRelease != null) btnChannelRelease.setText(I18n.get("update.channel.release"));
        if (btnChannelSnapshot != null) btnChannelSnapshot.setText(I18n.get("update.channel.snapshot"));
        if (lblCurTitle != null) lblCurTitle.setText(I18n.get("update.installed_version"));
        if (lblLatTitle != null) {
            lblLatTitle.setText(currentChannel == UpdateChannel.SNAPSHOT ? I18n.get("update.latest_snapshot") : I18n.get("update.latest_release"));
        }
        if (btnCloseFooter != null) btnCloseFooter.setText(I18n.get("action.close"));

        if (isChecking) {
            if (lblStatus != null) lblStatus.setText(I18n.get("update.checking_status"));
            if (lblLatestVersion != null) lblLatestVersion.setText(I18n.get("update.checking"));
        } else if (isError) {
            if (lblLatestVersion != null) lblLatestVersion.setText(I18n.get("universe.config.error_title"));
            if (lblStatus != null) lblStatus.setText(I18n.get("update.error"));
        } else if (lastRelease != null) {
            if (lastRelease.isNewer()) {
                if (lastRelease.channel() == UpdateChannel.RELEASE && UpdateService.isSnapshot()) {
                    if (lblStatus != null) lblStatus.setText(I18n.get("update.switch_to_release") + ": " + lastRelease.tagName());
                } else if (lastRelease.channel() == UpdateChannel.SNAPSHOT && !UpdateService.isSnapshot()) {
                    if (lblStatus != null) lblStatus.setText(I18n.get("update.switch_to_snapshot"));
                } else {
                    if (lblStatus != null) lblStatus.setText(I18n.get("update.available", lastRelease.tagName()));
                }
            } else {
                if (lblStatus != null) lblStatus.setText(I18n.get("update.up_to_date"));
            }
        }
        if (btnUpdate != null && lastRelease != null) {
            if (lastRelease.channel() == UpdateChannel.RELEASE && UpdateService.isSnapshot()) {
                btnUpdate.setText(I18n.get("update.switch_to_release"));
            } else if (lastRelease.channel() == UpdateChannel.SNAPSHOT && !UpdateService.isSnapshot()) {
                btnUpdate.setText(I18n.get("update.switch_to_snapshot"));
            } else {
                btnUpdate.setText(I18n.get("update.btn_update"));
            }
        }
        if (btnRetry != null) btnRetry.setText(I18n.get("update.btn_retry"));
    }

    /**
     * Startet die asynchrone Prüfung gegen die GitHub Releases API für den aktiven Kanal.
     */
    public void checkForUpdates() {
        this.isChecking = true;
        this.isError = false;
        spinner.setVisible(true);
        spinner.setManaged(true);
        lblStatus.setText(I18n.get("update.checking_status"));
        lblStatus.setStyle("-fx-text-fill: #F3F4F6; -fx-font-size: 12px;");
        lblLatestVersion.setText(I18n.get("update.checking"));
        markdownChangelog.setVisible(false);
        markdownChangelog.setManaged(false);
        progressBar.setVisible(false);
        progressBar.setManaged(false);
        lblProgressDetail.setVisible(false);
        lblProgressDetail.setManaged(false);
        actionButtonBox.getChildren().clear();

        UpdateService.checkLatestRelease(currentChannel)
                .thenAccept(release -> Platform.runLater(() -> applyReleaseResult(release)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> applyErrorResult(ex.getMessage()));
                    return null;
                });
    }

    private void applyReleaseResult(UpdateService.ReleaseInfo release) {
        this.isChecking = false;
        this.isError = false;
        this.lastRelease = release;
        spinner.setVisible(false);
        spinner.setManaged(false);

        if (release.channel() == UpdateChannel.SNAPSHOT) {
            String ts = formatTimestamp(release.publishedAt());
            lblLatestVersion.setText("Snapshot" + (ts.isEmpty() ? "" : " (" + ts + ")"));
            lblLatTitle.setText(I18n.get("update.latest_snapshot"));
        } else {
            lblLatestVersion.setText(release.tagName());
            lblLatTitle.setText(I18n.get("update.latest_release"));
        }

        if (release.isNewer()) {
            if (release.channel() == UpdateChannel.RELEASE && UpdateService.isSnapshot()) {
                lblStatus.setText(I18n.get("update.switch_to_release") + ": " + release.tagName());
            } else if (release.channel() == UpdateChannel.SNAPSHOT && !UpdateService.isSnapshot()) {
                lblStatus.setText(I18n.get("update.switch_to_snapshot"));
            } else {
                lblStatus.setText(I18n.get("update.available", release.tagName()));
            }
            lblStatus.setStyle("-fx-text-fill: #38BDF8; -fx-font-size: 12px; -fx-font-weight: bold;");

            if (release.body() != null && !release.body().isBlank()) {
                markdownChangelog.setMarkdown(release.body());
                markdownChangelog.setVisible(true);
                markdownChangelog.setManaged(true);
            }

            String btnText;
            if (release.channel() == UpdateChannel.RELEASE && UpdateService.isSnapshot()) {
                btnText = I18n.get("update.switch_to_release");
            } else if (release.channel() == UpdateChannel.SNAPSHOT && !UpdateService.isSnapshot()) {
                btnText = I18n.get("update.switch_to_snapshot");
            } else {
                btnText = I18n.get("update.btn_update");
            }
            this.btnUpdate = new Button(btnText);
            btnUpdate.setStyle("-fx-background-color: #38BDF8; -fx-text-fill: #0F172A; -fx-font-weight: bold; -fx-background-radius: 4px; -fx-padding: 6 12; -fx-cursor: hand;");
            btnUpdate.setGraphic(new LucideIcon("download", 14, Color.web("#0F172A")));

            btnUpdate.setOnAction(e -> {
                btnUpdate.setDisable(true);
                progressBar.setVisible(true);
                progressBar.setManaged(true);
                lblProgressDetail.setVisible(true);
                lblProgressDetail.setManaged(true);

                UpdateService.performUpdate(
                        release,
                        progress -> Platform.runLater(() -> {
                            if (progress >= 0) {
                                progressBar.setProgress(progress);
                            }
                        }),
                        status -> Platform.runLater(() -> {
                            lblProgressDetail.setText(status);
                        })
                ).exceptionally(ex -> {
                    Platform.runLater(() -> {
                        btnUpdate.setDisable(false);
                        lblProgressDetail.setText(I18n.get("update.failed", ex.getMessage()));
                        lblProgressDetail.setStyle("-fx-text-fill: #EF4444;");
                    });
                    return null;
                });
            });

            actionButtonBox.getChildren().setAll(btnUpdate);
        } else {
            lblStatus.setText(I18n.get("update.up_to_date"));
            lblStatus.setStyle("-fx-text-fill: #10B981; -fx-font-size: 12px; -fx-font-weight: bold;");

            this.btnRetry = new Button(I18n.get("update.btn_retry"));
            btnRetry.getStyleClass().add("patch-cancel-btn");
            btnRetry.setGraphic(new LucideIcon("refresh-cw", 13, Color.web("#9CA3AF")));
            btnRetry.setOnAction(e -> checkForUpdates());

            actionButtonBox.getChildren().setAll(btnRetry);
        }
    }

    private void applyErrorResult(String errorMessage) {
        this.isChecking = false;
        this.isError = true;
        spinner.setVisible(false);
        spinner.setManaged(false);
        lblLatestVersion.setText(I18n.get("universe.config.error_title"));
        lblStatus.setText(I18n.get("update.error"));
        lblStatus.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px;");

        this.btnRetry = new Button(I18n.get("update.btn_retry"));
        btnRetry.getStyleClass().add("patch-cancel-btn");
        btnRetry.setGraphic(new LucideIcon("refresh-cw", 13, Color.web("#9CA3AF")));
        btnRetry.setOnAction(e -> checkForUpdates());

        actionButtonBox.getChildren().setAll(btnRetry);
    }

    private String formatTimestamp(String iso) {
        if (iso == null || iso.isBlank()) return "";
        try {
            Instant instant = Instant.parse(iso);
            DateTimeFormatter dtf = DateTimeFormatter
                    .ofPattern("dd.MM.yyyy HH:mm")
                    .withZone(ZoneId.systemDefault());
            return dtf.format(instant);
        } catch (Exception e) {
            return "";
        }
    }

    private void openUrl(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Konnte URL nicht öffnen: " + url, ex);
        }
    }

    private void close() {
        if (mainWindow != null) {
            mainWindow.hideOverlay();
        }
    }

    public Node getView() {
        return card;
    }

    public Label getLblCurrentVersion() {
        return lblCurrentVersion;
    }

    public Label getLblCurTitle() {
        return lblCurTitle;
    }

    public Label getLblLatestVersion() {
        return lblLatestVersion;
    }

    public Label getLblLatTitle() {
        return lblLatTitle;
    }

    public Label getLblStatus() {
        return lblStatus;
    }

    public Button getBtnUpdate() {
        return btnUpdate;
    }

    public Button getBtnRetry() {
        return btnRetry;
    }

    public UpdateChannel getCurrentChannel() {
        return currentChannel;
    }

    public Button getBtnChannelRelease() {
        return btnChannelRelease;
    }

    public Button getBtnChannelSnapshot() {
        return btnChannelSnapshot;
    }

    public Button getBtnClose() {
        return btnClose;
    }

    public MarkdownView getMarkdownChangelog() {
        return markdownChangelog;
    }
}

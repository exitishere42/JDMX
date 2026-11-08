package de.exit.jdmx.ui.dialog;

import de.exit.jdmx.core.i18n.I18n;
import de.exit.jdmx.core.i18n.I18n;
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
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * In-App Modal Dialog für das Prüfen und Einspielen von Software-Updates.
 * <p>
 * Zeigt die aktuell installierte Version, ruft asynchron die neuesten Releases
 * von GitHub ab und erlaubt ein nahtloses In-App-Update ohne manuellen Installer.
 * </p>
 */
public class AboutUpdateDialog {

    private static final Logger LOGGER = Logger.getLogger(AboutUpdateDialog.class.getName());

    private final MainWindow mainWindow;
    private final VBox card;

    private Label lblTitle;
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

        // 2. Versions-Kachel (Installiert vs. Neueste Version)
        HBox versionCard = buildVersionCard();

        // 3. Status & Update-Bereich
        VBox statusCard = buildStatusCard();

        // 4. Footer (GitHub Link & Schließen)
        HBox footer = buildFooter();

        card.getChildren().addAll(header, versionCard, statusCard, footer);
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
        this.lblLatTitle = new Label(I18n.get("update.latest_version"));
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
        if (lblCurTitle != null) lblCurTitle.setText(I18n.get("update.installed_version"));
        if (lblLatTitle != null) lblLatTitle.setText(I18n.get("update.latest_version"));
        if (btnCloseFooter != null) btnCloseFooter.setText(I18n.get("action.close"));

        if (isChecking) {
            if (lblStatus != null) lblStatus.setText(I18n.get("update.checking_status"));
            if (lblLatestVersion != null) lblLatestVersion.setText(I18n.get("update.checking"));
        } else if (isError) {
            if (lblLatestVersion != null) lblLatestVersion.setText(I18n.get("universe.config.error_title"));
            if (lblStatus != null) lblStatus.setText(I18n.get("update.error"));
        } else if (lastRelease != null) {
            if (lastRelease.isNewer()) {
                if (lblStatus != null) lblStatus.setText(I18n.get("update.available", lastRelease.tagName()));
            } else {
                if (lblStatus != null) lblStatus.setText(I18n.get("update.up_to_date"));
            }
        }
        if (btnUpdate != null) btnUpdate.setText(I18n.get("update.btn_update"));
        if (btnRetry != null) btnRetry.setText(I18n.get("update.btn_retry"));
    }

    /**
     * Startet die asynchrone Prüfung gegen die GitHub Releases API.
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

        UpdateService.checkLatestRelease()
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
        lblLatestVersion.setText(release.tagName());

        if (release.isNewer()) {
            lblStatus.setText(I18n.get("update.available", release.tagName()));
            lblStatus.setStyle("-fx-text-fill: #38BDF8; -fx-font-size: 12px; -fx-font-weight: bold;");

            if (release.body() != null && !release.body().isBlank()) {
                markdownChangelog.setMarkdown(release.body());
                markdownChangelog.setVisible(true);
                markdownChangelog.setManaged(true);
            }

            this.btnUpdate = new Button(I18n.get("update.btn_update"));
            btnUpdate.setStyle("-fx-background-color: #38BDF8; -fx-text-fill: #0F172A; -fx-font-weight: bold; -fx-background-radius: 4px; -fx-padding: 6 12;");
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

    public Button getBtnClose() {
        return btnClose;
    }

    public MarkdownView getMarkdownChangelog() {
        return markdownChangelog;
    }
}

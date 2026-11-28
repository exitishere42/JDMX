package de.exit.jdmx.service;

import de.exit.jdmx.core.i18n.I18n;
import de.exit.jdmx.core.model.UpdateChannel;
import de.exit.jdmx.core.project.model.SettingsDataDto;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für {@link SettingsService}.
 */
class SettingsServiceTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }

    @AfterEach
    void resetLocale() {
        I18n.setLanguage("de");
    }

    @Test
    @DisplayName("Sollte Standardwerte für Einstellungen besitzen")
    void shouldHaveDefaultSettings() {
        SettingsService service = new SettingsService();
        assertThat(service.getLanguage()).isNotEmpty();
        assertThat(service.isFullscreen()).isFalse();
        assertThat(service.isConfirmExit()).isTrue();
        assertThat(service.getAutoSaveInterval()).isEqualTo("Alle 10 Minuten");
        assertThat(service.getDefaultFolder()).contains("jdmx-shows");
    }

    @Test
    @DisplayName("Sollte Einstellungen in DTO exportieren und aus DTO wiederherstellen")
    void shouldExportAndImportDto() {
        ShortcutService shortcutService = new ShortcutService();
        shortcutService.setShortcut(ShortcutAction.NEW_PROJECT, new KeyCodeCombination(KeyCode.M, KeyCombination.CONTROL_DOWN));

        SettingsService service = new SettingsService();
        service.setLanguage("en");
        service.setFullscreen(true);
        service.setConfirmExit(false);
        service.setAutoSaveInterval("Alle 5 Minuten");
        service.setDefaultFolder("C:\\CustomShows");

        SettingsDataDto dto = service.toDto(shortcutService);
        assertThat(dto.language()).isEqualTo("en");
        assertThat(dto.fullscreen()).isTrue();
        assertThat(dto.confirmExit()).isFalse();
        assertThat(dto.autoSaveInterval()).isEqualTo("Alle 5 Minuten");
        assertThat(dto.defaultFolder()).isEqualTo("C:\\CustomShows");
        assertThat(dto.shortcuts()).containsKey("NEW_PROJECT");

        // Auf neuem Service anwenden
        SettingsService newService = new SettingsService();
        ShortcutService newShortcutService = new ShortcutService();
        newService.applyDto(dto, newShortcutService);

        assertThat(newService.getLanguage()).isEqualTo("en");
        assertThat(newService.isFullscreen()).isTrue();
        assertThat(newService.isConfirmExit()).isFalse();
        assertThat(newService.getAutoSaveInterval()).isEqualTo("Alle 5 Minuten");
        assertThat(newService.getDefaultFolder()).isEqualTo("C:\\CustomShows");
        assertThat(newShortcutService.getDisplayText(ShortcutAction.NEW_PROJECT)).contains("Strg + M");
    }

    @Test
    @DisplayName("Sollte Einstellungen auf Standardwerte zurücksetzen")
    void shouldResetDefaults() {
        ShortcutService shortcutService = new ShortcutService();
        shortcutService.setShortcut(ShortcutAction.SAVE_PROJECT, new KeyCodeCombination(KeyCode.K, KeyCombination.CONTROL_DOWN));

        SettingsService service = new SettingsService();
        service.setFullscreen(true);
        service.setConfirmExit(false);
        service.resetDefaults(shortcutService);

        assertThat(service.isFullscreen()).isFalse();
        assertThat(service.isConfirmExit()).isTrue();
        assertThat(shortcutService.getDisplayText(ShortcutAction.SAVE_PROJECT)).contains("Strg + S");
        assertThat(service.getUiScale()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Sollte standardmäßig UI-Skalierung 1.0 (100%) besitzen")
    void shouldDefaultToUiScale100Percent() {
        SettingsService service = new SettingsService();
        assertThat(service.getUiScale()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Sollte UI-Skalierung setzen und auf sinnvolle Werte begrenzen (0.5 bis 2.5)")
    void shouldSetAndClampUiScale() {
        SettingsService service = new SettingsService();
        service.setUiScale(0.8);
        assertThat(service.getUiScale()).isEqualTo(0.8);

        service.setUiScale(1.25);
        assertThat(service.getUiScale()).isEqualTo(1.25);

        // Clamping nach unten
        service.setUiScale(0.1);
        assertThat(service.getUiScale()).isEqualTo(0.5);

        // Clamping nach oben
        service.setUiScale(5.0);
        assertThat(service.getUiScale()).isEqualTo(2.5);
    }

    @Test
    @DisplayName("Sollte Listener bei UI-Skalierungsänderung reaktiv benachrichtigen")
    void shouldNotifyUiScaleListenersWhenScaleChanges() {
        SettingsService service = new SettingsService();
        AtomicReference<Double> capturedScale = new AtomicReference<>();
        service.addUiScaleListener(capturedScale::set);

        service.setUiScale(0.9);
        assertThat(capturedScale.get()).isEqualTo(0.9);
    }

    @Test
    @DisplayName("Sollte globale Konfigurationsdatei laden und speichern können")
    void shouldSaveAndLoadGlobalConfigFile(@TempDir Path tempDir) {
        File configFile = tempDir.resolve("settings.json").toFile();
        ShortcutService shortcutService = new ShortcutService();
        shortcutService.setShortcut(ShortcutAction.UNDO, new KeyCodeCombination(KeyCode.U, KeyCombination.CONTROL_DOWN));

        SettingsService service = new SettingsService(configFile);
        service.setLanguage("en");
        service.setUiScale(0.8);
        service.setFullscreen(true);
        service.setConfirmExit(false);
        service.setAutoSaveInterval("Alle 15 Minuten");
        service.setDefaultFolder("D:\\Shows");
        service.saveGlobalSettings(shortcutService);

        assertThat(configFile).exists().isNotEmpty();

        // Neuen Service mit derselben Datei initialisieren -> muss Einstellungen sofort laden
        ShortcutService newShortcutService = new ShortcutService();
        SettingsService restoredService = new SettingsService(configFile);
        restoredService.loadGlobalSettings(newShortcutService);

        assertThat(restoredService.getLanguage()).isEqualTo("en");
        assertThat(restoredService.getUiScale()).isEqualTo(0.8);
        assertThat(restoredService.isFullscreen()).isTrue();
        assertThat(restoredService.isConfirmExit()).isFalse();
        assertThat(restoredService.getAutoSaveInterval()).isEqualTo("Alle 15 Minuten");
        assertThat(restoredService.getDefaultFolder()).isEqualTo("D:\\Shows");
        assertThat(newShortcutService.getDisplayText(ShortcutAction.UNDO)).contains("Strg + U");
    }

    @Test
    @DisplayName("Sollte Discord Bot Einstellungen speichern und wiederherstellen")
    void shouldSaveAndLoadDiscordBotSettings(@TempDir Path tempDir) {
        File configFile = tempDir.resolve("settings.json").toFile();
        SettingsService service = new SettingsService(configFile);
        service.setDiscordBotToken("test-bot-token-xyz");
        service.setDiscordRecipientUserId("987654321098765432");
        service.saveGlobalSettings(null);

        SettingsService restoredService = new SettingsService(configFile);
        assertThat(restoredService.getDiscordBotToken()).isEqualTo("test-bot-token-xyz");
        assertThat(restoredService.getDiscordRecipientUserId()).isEqualTo("987654321098765432");
    }

    @Test
    @DisplayName("Sollte UpdateChannel setzen, Listener benachrichtigen und in globaler Config speichern")
    void shouldPersistAndNotifyUpdateChannel(@TempDir Path tempDir) {
        File configFile = tempDir.resolve("settings.json").toFile();
        SettingsService service = new SettingsService(configFile);

        AtomicReference<UpdateChannel> notified = new AtomicReference<>();
        service.addUpdateChannelListener(notified::set);

        UpdateChannel target = service.getUpdateChannel() == UpdateChannel.SNAPSHOT ? UpdateChannel.RELEASE : UpdateChannel.SNAPSHOT;
        service.setUpdateChannel(target);
        assertThat(notified.get()).isEqualTo(target);

        service.saveGlobalSettings(null);

        SettingsService loadedService = new SettingsService(configFile);
        assertThat(loadedService.getUpdateChannel()).isEqualTo(target);
    }
}

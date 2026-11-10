package de.exit.jdmx.service;

import de.exit.jdmx.core.project.model.SettingsDataDto;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

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
    }
}

package de.exit.jdmx.service;

import de.exit.jdmx.core.i18n.I18n;
import de.exit.jdmx.core.project.model.SettingsDataDto;

import java.io.File;
import java.util.Objects;

/**
 * Service für die Verwaltung und Persistierung der Profileinstellungen.
 * <p>
 * Verwaltet Anwendungs- und Profileinstellungen wie Sprache, Vollbild beim Start,
 * Beenden-Bestätigung, Auto-Save-Intervall, Show-Speicherverzeichnis sowie
 * die anpassbaren Tastaturkürzel.
 * Diese Einstellungen werden direkt in der {@code *.jdmx}-Projektdatei gespeichert.
 * </p>
 */
public class SettingsService {

    private String language = "de";
    private boolean fullscreen = false;
    private boolean confirmExit = true;
    private String autoSaveInterval = "Alle 10 Minuten";
    private String defaultFolder = System.getProperty("user.home") + File.separator + "jdmx-shows";

    /**
     * Erstellt eine neue Instanz des {@link SettingsService} mit den aktuellen Voreinstellungen.
     */
    public SettingsService() {
        this.language = I18n.getCurrentLanguage();
    }

    /**
     * Erstellt ein Snapshot-DTO der aktuellen Einstellungen inklusive konfigurierter Shortcuts.
     *
     * @param shortcutService Der Shortcut-Service zur Extraktion der Belegungen
     * @return Vollständiges SettingsDataDto
     */
    public SettingsDataDto toDto(ShortcutService shortcutService) {
        return new SettingsDataDto(
                language,
                fullscreen,
                confirmExit,
                autoSaveInterval,
                defaultFolder,
                shortcutService != null ? shortcutService.exportShortcuts() : java.util.Map.of()
        );
    }

    /**
     * Wendet die im DTO gespeicherten Einstellungen auf die Anwendung an.
     *
     * @param dto             Die geladenen Einstellungen
     * @param shortcutService Der Shortcut-Service zum Einpflegen benutzerdefinierter Tasten
     */
    public void applyDto(SettingsDataDto dto, ShortcutService shortcutService) {
        if (dto == null) {
            return;
        }
        if (dto.language() != null && !dto.language().isBlank()) {
            this.language = dto.language();
            I18n.setLanguage(dto.language());
        }
        this.fullscreen = dto.fullscreen();
        this.confirmExit = dto.confirmExit();
        if (dto.autoSaveInterval() != null && !dto.autoSaveInterval().isBlank()) {
            this.autoSaveInterval = dto.autoSaveInterval();
        }
        if (dto.defaultFolder() != null && !dto.defaultFolder().isBlank()) {
            this.defaultFolder = dto.defaultFolder();
        }
        if (shortcutService != null && dto.shortcuts() != null) {
            shortcutService.importShortcuts(dto.shortcuts());
        }
    }

    /**
     * Setzt alle Profileinstellungen und Shortcuts auf die Werkseinstellungen zurück.
     *
     * @param shortcutService Der Shortcut-Service zum Zurücksetzen der Tastenbelegungen
     */
    public void resetDefaults(ShortcutService shortcutService) {
        this.language = "de";
        this.fullscreen = false;
        this.confirmExit = true;
        this.autoSaveInterval = "Alle 10 Minuten";
        this.defaultFolder = System.getProperty("user.home") + File.separator + "jdmx-shows";
        I18n.setLanguage("de");
        if (shortcutService != null) {
            shortcutService.resetAll();
        }
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = Objects.requireNonNullElse(language, "de");
        I18n.setLanguage(this.language);
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
    }

    public boolean isConfirmExit() {
        return confirmExit;
    }

    public void setConfirmExit(boolean confirmExit) {
        this.confirmExit = confirmExit;
    }

    public String getAutoSaveInterval() {
        return autoSaveInterval;
    }

    public void setAutoSaveInterval(String autoSaveInterval) {
        this.autoSaveInterval = Objects.requireNonNullElse(autoSaveInterval, "Alle 10 Minuten");
    }

    public String getDefaultFolder() {
        return defaultFolder;
    }

    public void setDefaultFolder(String defaultFolder) {
        this.defaultFolder = Objects.requireNonNullElse(defaultFolder, System.getProperty("user.home") + File.separator + "jdmx-shows");
    }
}

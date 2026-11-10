package de.exit.jdmx.core.project.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.File;
import java.util.Map;

/**
 * Persistentes Datenmodell für Anwendung- und Profileinstellungen innerhalb einer .jdmx Datei.
 *
 * @param language         Sprachcode (z. B. "de", "en", "ru")
 * @param fullscreen       Vollbildmodus beim Programmstart
 * @param confirmExit      Sicherheitsabfrage vor dem Schließen
 * @param autoSaveInterval Intervall für automatisches Speichern (z. B. "Alle 10 Minuten", "Deaktiviert")
 * @param defaultFolder    Standard-Speicherverzeichnis für Shows
 * @param shortcuts        Map von Shortcut-Aktionsname auf KeyCombination-String (z. B. "SAVE_PROJECT" -> "Ctrl+S")
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SettingsDataDto(
        String language,
        boolean fullscreen,
        boolean confirmExit,
        String autoSaveInterval,
        String defaultFolder,
        Map<String, String> shortcuts
) {
    /**
     * Erstellt ein Standard-Einstellungen-DTO.
     */
    public static SettingsDataDto defaultSettings() {
        return new SettingsDataDto(
                "de",
                false,
                true,
                "Alle 10 Minuten",
                System.getProperty("user.home") + File.separator + "jdmx-shows",
                Map.of()
        );
    }
}

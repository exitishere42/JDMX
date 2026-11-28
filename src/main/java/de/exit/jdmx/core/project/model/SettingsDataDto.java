package de.exit.jdmx.core.project.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.exit.jdmx.core.model.UpdateChannel;

import java.io.File;
import java.util.Map;

/**
 * Persistentes Datenmodell für Anwendungs- und Profileinstellungen innerhalb einer .jdmx Datei oder settings.json.
 *
 * @param language         Sprachcode (z. B. "de", "en", "ru")
 * @param fullscreen       Vollbildmodus beim Programmstart
 * @param confirmExit      Sicherheitsabfrage vor dem Schließen
 * @param autoSaveInterval Intervall für automatisches Speichern (z. B. "Alle 10 Minuten", "Deaktiviert")
 * @param defaultFolder    Standard-Speicherverzeichnis für Shows
 * @param shortcuts        Map von Shortcut-Aktionsname auf KeyCombination-String (z. B. "SAVE_PROJECT" -> "Ctrl+S")
 * @param uiScale          Skalierungsfaktor der Benutzeroberfläche (z. B. 0.8, 1.0, 1.25)
 * @param discordBotToken  Optionaler Discord-Bot-Token für direkten Support- und Feedback-Versand
 * @param discordRecipientUserId Optionale Discord-Benutzer-ID des Empfängers für Direktnachrichten
 * @param updateChannel    Aktiver Update-Kanal (RELEASE oder SNAPSHOT)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SettingsDataDto(
        String language,
        boolean fullscreen,
        boolean confirmExit,
        String autoSaveInterval,
        String defaultFolder,
        Map<String, String> shortcuts,
        double uiScale,
        String discordBotToken,
        String discordRecipientUserId,
        UpdateChannel updateChannel
) {
    /** Standardmäßige Skalierung (100%). */
    public static final double DEFAULT_UI_SCALE = 1.0;

    /**
     * Kompakter Konstruktor zur Normalisierung von ungültigen oder fehlenden Werten (z. B. aus älterem JSON).
     */
    public SettingsDataDto {
        if (uiScale <= 0.1) {
            uiScale = DEFAULT_UI_SCALE;
        }
        if (discordBotToken == null) {
            discordBotToken = "";
        }
        if (discordRecipientUserId == null) {
            discordRecipientUserId = "";
        }
        if (updateChannel == null) {
            updateChannel = UpdateChannel.RELEASE;
        }
    }

    /**
     * Überladener Konstruktor für Abwärtskompatibilität ohne updateChannel.
     */
    public SettingsDataDto(String language, boolean fullscreen, boolean confirmExit, String autoSaveInterval, String defaultFolder, Map<String, String> shortcuts, double uiScale, String discordBotToken, String discordRecipientUserId) {
        this(language, fullscreen, confirmExit, autoSaveInterval, defaultFolder, shortcuts, uiScale, discordBotToken, discordRecipientUserId, UpdateChannel.RELEASE);
    }

    /**
     * Überladener Konstruktor für Abwärtskompatibilität mit uiScale ohne Discord-Felder.
     */
    public SettingsDataDto(String language, boolean fullscreen, boolean confirmExit, String autoSaveInterval, String defaultFolder, Map<String, String> shortcuts, double uiScale) {
        this(language, fullscreen, confirmExit, autoSaveInterval, defaultFolder, shortcuts, uiScale, "", "", UpdateChannel.RELEASE);
    }

    /**
     * Überladener Konstruktor für Abwärtskompatibilität ohne expliziten uiScale.
     */
    public SettingsDataDto(String language, boolean fullscreen, boolean confirmExit, String autoSaveInterval, String defaultFolder, Map<String, String> shortcuts) {
        this(language, fullscreen, confirmExit, autoSaveInterval, defaultFolder, shortcuts, DEFAULT_UI_SCALE, "", "", UpdateChannel.RELEASE);
    }

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
                Map.of(),
                DEFAULT_UI_SCALE,
                "",
                "",
                UpdateChannel.RELEASE
        );
    }
}

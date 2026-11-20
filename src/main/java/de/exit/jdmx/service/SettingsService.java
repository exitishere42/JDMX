package de.exit.jdmx.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.exit.jdmx.core.i18n.I18n;
import de.exit.jdmx.core.project.model.SettingsDataDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Service für die Verwaltung und Persistierung der Profileinstellungen.
 * <p>
 * Verwaltet Anwendungs- und Profileinstellungen wie Sprache, UI-Skalierungsfaktor,
 * Vollbild beim Start, Beenden-Bestätigung, Auto-Save-Intervall, Show-Speicherverzeichnis sowie
 * die anpassbaren Tastaturkürzel.
 * Diese Einstellungen werden sowohl in einer globalen Konfigurationsdatei ({@code settings.json})
 * für den App-Start und neue Projekte gespeichert, als auch in den jeweiligen {@code *.jdmx}-Projektdateien.
 * </p>
 */
public class SettingsService {

    private static final Logger log = LoggerFactory.getLogger(SettingsService.class);

    public static final double DEFAULT_UI_SCALE = 1.0;
    public static final double MIN_UI_SCALE = 0.5;
    public static final double MAX_UI_SCALE = 2.5;

    private final ObjectMapper objectMapper;
    private final File globalConfigFile;

    private String language = "de";
    private double uiScale = DEFAULT_UI_SCALE;
    private boolean fullscreen = false;
    private boolean confirmExit = true;
    private String autoSaveInterval = "Alle 10 Minuten";
    private String defaultFolder = System.getProperty("user.home") + File.separator + "jdmx-shows";

    private final List<Consumer<Double>> uiScaleListeners = new CopyOnWriteArrayList<>();

    /**
     * Erstellt eine neue Instanz des {@link SettingsService} mit der standardmäßigen Konfigurationsdatei.
     */
    public SettingsService() {
        this(resolveGlobalConfigFile());
    }

    /**
     * Erstellt eine Instanz mit einer spezifischen Konfigurationsdatei (z. B. für Tests).
     *
     * @param configFile Die zu verwendende Einstellungsdatei
     */
    public SettingsService(File configFile) {
        this.globalConfigFile = configFile;
        this.language = I18n.getCurrentLanguage();

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        // Globale Einstellungen laden, falls Datei bereits existiert
        if (this.globalConfigFile != null && this.globalConfigFile.exists()) {
            loadGlobalSettings(null);
        }
    }

    /**
     * Ermittelt den betriebssystemspezifischen Pfad zur globalen Einstellungsdatei.
     *
     * @return Konfigurationsdatei settings.json
     */
    public static File resolveGlobalConfigFile() {
        boolean isDevEnvironment = new File("./pom.xml").exists() && new File("./src").exists();
        if (isDevEnvironment) {
            return new File("./projects/settings.json");
        }

        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) {
                return new File(appData, "jdmx/settings.json");
            }
            return new File(System.getProperty("user.home"), "AppData/Roaming/jdmx/settings.json");
        } else {
            return new File(System.getProperty("user.home"), ".config/jdmx/settings.json");
        }
    }

    /**
     * Lädt die globalen Benutzereinstellungen aus der Konfigurationsdatei.
     *
     * @param shortcutService Der Shortcut-Service zum Importieren gespeicherter Shortcuts (optional)
     */
    public void loadGlobalSettings(ShortcutService shortcutService) {
        if (globalConfigFile == null || !globalConfigFile.exists()) {
            return;
        }
        try {
            SettingsDataDto dto = objectMapper.readValue(globalConfigFile, SettingsDataDto.class);
            applyDto(dto, shortcutService);
            log.info("Globale Einstellungen erfolgreich geladen aus {}", globalConfigFile.getAbsolutePath());
        } catch (IOException e) {
            log.warn("Konnte globale Einstellungen aus {} nicht laden: {}", globalConfigFile.getAbsolutePath(), e.getMessage());
        }
    }

    /**
     * Speichert die aktuellen Einstellungen dauerhaft in der globalen Konfigurationsdatei.
     *
     * @param shortcutService Der Shortcut-Service zum Exportieren der Shortcuts (optional)
     */
    public void saveGlobalSettings(ShortcutService shortcutService) {
        if (globalConfigFile == null) {
            return;
        }
        try {
            File parent = globalConfigFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            SettingsDataDto dto = toDto(shortcutService);
            objectMapper.writeValue(globalConfigFile, dto);
            log.info("Globale Einstellungen gespeichert in {}", globalConfigFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Fehler beim Speichern der globalen Einstellungen in {}: {}", globalConfigFile.getAbsolutePath(), e.getMessage(), e);
        }
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
                shortcutService != null ? shortcutService.exportShortcuts() : java.util.Map.of(),
                uiScale
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
        if (dto.uiScale() > 0.1) {
            setUiScale(dto.uiScale());
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
        this.uiScale = DEFAULT_UI_SCALE;
        this.fullscreen = false;
        this.confirmExit = true;
        this.autoSaveInterval = "Alle 10 Minuten";
        this.defaultFolder = System.getProperty("user.home") + File.separator + "jdmx-shows";
        I18n.setLanguage("de");
        notifyUiScaleListeners(this.uiScale);
        if (shortcutService != null) {
            shortcutService.resetAll();
        }
    }

    public double getUiScale() {
        return uiScale;
    }

    public void setUiScale(double scale) {
        double clamped = Math.max(MIN_UI_SCALE, Math.min(MAX_UI_SCALE, scale));
        if (Double.compare(this.uiScale, clamped) != 0) {
            this.uiScale = clamped;
            notifyUiScaleListeners(this.uiScale);
        }
    }

    public void addUiScaleListener(Consumer<Double> listener) {
        if (listener != null && !uiScaleListeners.contains(listener)) {
            uiScaleListeners.add(listener);
        }
    }

    public void removeUiScaleListener(Consumer<Double> listener) {
        uiScaleListeners.remove(listener);
    }

    private void notifyUiScaleListeners(double scale) {
        for (Consumer<Double> listener : uiScaleListeners) {
            try {
                listener.accept(scale);
            } catch (Exception e) {
                log.warn("Fehler im UI-Scale Listener: {}", e.getMessage());
            }
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

    public File getGlobalConfigFile() {
        return globalConfigFile;
    }
}

package de.exit.jdmx.core.i18n;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service für die Internationalisierung (I18n) und Lokalisierung von JDMX.
 * <p>
 * Unterstützt aktuell die drei Kernsprachen:
 * <ul>
 *     <li><b>Deutsch (de)</b> - Standard</li>
 *     <li><b>Englisch (en)</b></li>
 *     <li><b>Russisch (ru)</b></li>
 * </ul>
 * Etablierte Branchen-Fachbegriffe (wie {@code LightDesk}, {@code Button-Matrix}, {@code Stage & Fixtures},
 * {@code Show-Timeline}, {@code I/O Patch}, {@code Fixture}, {@code Pan}, {@code Tilt}) bleiben gemäß
 * Anforderungsvorgabe in allen Sprachen geschützt und einheitlich.
 * </p>
 */
public class I18nService {

    private static final Logger LOGGER = Logger.getLogger(I18nService.class.getName());

    public static final String DEFAULT_LANGUAGE = "de";
    public static final Set<String> SUPPORTED_LANGUAGES = Set.of("de", "en", "ru");

    private String currentLanguage = DEFAULT_LANGUAGE;
    private final Map<String, Properties> bundles = new HashMap<>();
    private final List<java.util.function.Consumer<String>> listeners = new CopyOnWriteArrayList<>();

    public I18nService() {
        loadAllBundles();
    }

    private void loadAllBundles() {
        for (String lang : SUPPORTED_LANGUAGES) {
            String path = "/de/exit/jdmx/i18n/messages_" + lang + ".properties";
            Properties props = new Properties();
            try (InputStream in = getClass().getResourceAsStream(path)) {
                if (in != null) {
                    try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                        props.load(reader);
                    }
                    bundles.put(lang, props);
                } else {
                    LOGGER.warning("I18n-Ressource nicht gefunden: " + path);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Fehler beim Laden von I18n-Ressource: " + path, e);
            }
        }
    }

    /**
     * Setzt die aktive Sprache und benachrichtigt alle registrierten Listener.
     *
     * @param langCode Sprachcode ("de", "en", "ru")
     */
    public void setLanguage(String langCode) {
        if (langCode == null || !SUPPORTED_LANGUAGES.contains(langCode.toLowerCase(Locale.ROOT))) {
            langCode = DEFAULT_LANGUAGE;
        }
        String normalized = langCode.toLowerCase(Locale.ROOT);
        if (!Objects.equals(this.currentLanguage, normalized)) {
            this.currentLanguage = normalized;
            notifyListeners();
        }
    }

    /**
     * Liefert den aktuellen Sprachcode ("de", "en", "ru").
     *
     * @return Aktiver Sprachcode
     */
    public String getCurrentLanguage() {
        return currentLanguage;
    }

    /**
     * Holt die lokalisierte Zeichenkette für den angegebenen Schlüssel.
     *
     * @param key Schlüssel in den Properties
     * @return Lokalisierter Text oder der Schlüssel selbst als Fallback
     */
    public String get(String key) {
        if (key == null) {
            return "";
        }
        Properties props = bundles.get(currentLanguage);
        if (props != null && props.containsKey(key)) {
            return props.getProperty(key);
        }
        // Fallback auf Deutsch
        Properties defProps = bundles.get(DEFAULT_LANGUAGE);
        if (defProps != null && defProps.containsKey(key)) {
            return defProps.getProperty(key);
        }
        return key;
    }

    /**
     * Holt die lokalisierte Zeichenkette und formatiert diese mit Parametern via {@link MessageFormat}.
     *
     * @param key  Schlüssel
     * @param args Format-Argumente ({0}, {1}, ...)
     * @return Formatierter lokalisierter Text
     */
    public String get(String key, Object... args) {
        String pattern = get(key);
        if (args == null || args.length == 0) {
            return pattern;
        }
        try {
            return MessageFormat.format(pattern, args);
        } catch (Exception e) {
            return pattern;
        }
    }

    /**
     * Registriert einen Listener für Sprachänderungen.
     *
     * @param listener Callback mit dem neuen Sprachcode
     */
    public void addLanguageChangeListener(java.util.function.Consumer<String> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Entfernt einen Listener für Sprachänderungen.
     *
     * @param listener Callback
     */
    public void removeLanguageChangeListener(java.util.function.Consumer<String> listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (var listener : listeners) {
            try {
                listener.accept(currentLanguage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Fehler in I18n LanguageChangeListener", e);
            }
        }
    }
}

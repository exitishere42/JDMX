package de.exit.jdmx.core.i18n;

import java.util.function.Consumer;

/**
 * Globale statische Fassade für den Zugriff auf {@link I18nService}.
 */
public final class I18n {

    private static I18nService instance = new I18nService();

    private I18n() {
        // Statischer Zugriff
    }

    /**
     * Setzt die globale I18nService-Instanz (z. B. für Mocking oder Dependency Injection).
     *
     * @param service Der I18nService
     */
    public static void setInstance(I18nService service) {
        if (service != null) {
            instance = service;
        }
    }

    /**
     * Liefert die globale I18nService-Instanz.
     *
     * @return I18nService
     */
    public static I18nService getInstance() {
        return instance;
    }

    /**
     * Holt die lokalisierte Zeichenkette für den angegebenen Schlüssel.
     *
     * @param key Schlüssel
     * @return Lokalisierter Text
     */
    public static String get(String key) {
        return instance.get(key);
    }

    /**
     * Holt die lokalisierte Zeichenkette und formatiert diese mit Argumenten.
     *
     * @param key  Schlüssel
     * @param args Format-Argumente
     * @return Formatierter lokalisierter Text
     */
    public static String get(String key, Object... args) {
        return instance.get(key, args);
    }

    /**
     * Setzt die aktive Sprache.
     *
     * @param langCode Sprachcode ("de", "en", "ru")
     */
    public static void setLanguage(String langCode) {
        instance.setLanguage(langCode);
    }

    /**
     * Liefert den aktuellen Sprachcode.
     *
     * @return Sprachcode
     */
    public static String getCurrentLanguage() {
        return instance.getCurrentLanguage();
    }

    /**
     * Registriert einen Sprachwechsel-Listener.
     *
     * @param listener Callback
     */
    public static void addLanguageChangeListener(Consumer<String> listener) {
        instance.addLanguageChangeListener(listener);
    }

    /**
     * Entfernt einen Sprachwechsel-Listener.
     *
     * @param listener Callback
     */
    public static void removeLanguageChangeListener(Consumer<String> listener) {
        instance.removeLanguageChangeListener(listener);
    }
}

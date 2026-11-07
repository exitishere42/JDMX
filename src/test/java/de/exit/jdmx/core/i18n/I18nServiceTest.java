package de.exit.jdmx.core.i18n;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD-Tests für das Lokalisierungs-Subsystem (I18nService / I18n).
 */
class I18nServiceTest {

    private I18nService i18n;

    @BeforeEach
    void setUp() {
        i18n = new I18nService();
        i18n.setLanguage("de");
    }

    @Test
    @DisplayName("Sollte standardmäßig deutsche Texte zurückliefern")
    void shouldReturnDefaultGermanText() {
        assertThat(i18n.get("settings.title")).isEqualTo("Einstellungen");
        assertThat(i18n.get("fixture.list.title")).isEqualTo("Fixture-Liste");
        assertThat(i18n.get("control.color")).isEqualTo("Farbe");
        assertThat(i18n.get("action.zero_all")).isEqualTo("Alle Kanäle 0");
    }

    @Test
    @DisplayName("Sollte nach Umschaltung auf Englisch englische Texte liefern")
    void shouldReturnEnglishTextWhenSwitched() {
        i18n.setLanguage("en");

        assertThat(i18n.getCurrentLanguage()).isEqualTo("en");
        assertThat(i18n.get("settings.title")).isEqualTo("Settings");
        assertThat(i18n.get("fixture.list.title")).isEqualTo("Fixture List");
        assertThat(i18n.get("control.color")).isEqualTo("Color");
        assertThat(i18n.get("action.zero_all")).isEqualTo("All Channels 0");
    }

    @Test
    @DisplayName("Sollte nach Umschaltung auf Russisch russische Texte liefern")
    void shouldReturnRussianTextWhenSwitched() {
        i18n.setLanguage("ru");

        assertThat(i18n.getCurrentLanguage()).isEqualTo("ru");
        assertThat(i18n.get("settings.title")).isEqualTo("Настройки");
        assertThat(i18n.get("fixture.list.title")).isEqualTo("Список приборов");
        assertThat(i18n.get("control.color")).isEqualTo("Цвет");
        assertThat(i18n.get("action.zero_all")).isEqualTo("Все каналы 0");
    }

    @Test
    @DisplayName("Sollte Platzhalter-Formatierung unterstützen")
    void shouldSupportFormatArguments() {
        i18n.setLanguage("de");
        assertThat(i18n.get("fixture.channels", 13)).isEqualTo("13 Kanäle");

        i18n.setLanguage("en");
        assertThat(i18n.get("fixture.channels", 13)).isEqualTo("13 Channels");

        i18n.setLanguage("ru");
        assertThat(i18n.get("fixture.channels", 13)).isEqualTo("13 Каналов");
    }

    @Test
    @DisplayName("Sollte bei fehlendem Schlüssel den Key selbst als Fallback liefern")
    void shouldFallbackToKeyWhenMissing() {
        assertThat(i18n.get("non.existing.key")).isEqualTo("non.existing.key");
    }

    @Test
    @DisplayName("Sollte Listener bei Sprachwechsel benachrichtigen")
    void shouldNotifyListenersOnLanguageChange() {
        AtomicBoolean notified = new AtomicBoolean(false);
        i18n.addLanguageChangeListener(lang -> {
            if ("en".equals(lang)) {
                notified.set(true);
            }
        });

        i18n.setLanguage("en");
        assertThat(notified.get()).isTrue();
    }

    @Test
    @DisplayName("Sollte Fachbegriffe geschützt unverändert lassen")
    void shouldKeepProtectedDomainTermsIdenticalAcrossLanguages() {
        for (String lang : new String[]{"de", "en", "ru"}) {
            i18n.setLanguage(lang);
            assertThat(i18n.get("term.lightdesk")).isEqualTo("LightDesk");
            assertThat(i18n.get("term.button_matrix")).isEqualTo("Button-Matrix");
            assertThat(i18n.get("term.stage_fixtures")).isEqualTo("Stage & Fixtures");
            assertThat(i18n.get("term.show_timeline")).isEqualTo("Show-Timeline");
            assertThat(i18n.get("term.io_patch")).isEqualTo("I/O Patch");
            assertThat(i18n.get("term.fixture")).isEqualTo("Fixture");
        }
    }
}

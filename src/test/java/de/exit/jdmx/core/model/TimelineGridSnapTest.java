package de.exit.jdmx.core.model;

import de.exit.jdmx.core.i18n.I18n;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für {@link TimelineGridSnap} und dessen Snapping-Berechnung sowie Lokalisierung.
 */
class TimelineGridSnapTest {

    @AfterEach
    void resetLocale() {
        I18n.setLanguage("de");
    }

    @Test
    @DisplayName("Sollte korrekte Intervalle in Millisekunden liefern")
    void shouldReturnCorrectIntervals() {
        assertThat(TimelineGridSnap.FREE.getIntervalMs()).isEqualTo(0L);
        assertThat(TimelineGridSnap.SEC_1.getIntervalMs()).isEqualTo(1000L);
        assertThat(TimelineGridSnap.SEC_5.getIntervalMs()).isEqualTo(5000L);
        assertThat(TimelineGridSnap.SEC_10.getIntervalMs()).isEqualTo(10000L);
        assertThat(TimelineGridSnap.SEC_30.getIntervalMs()).isEqualTo(30000L);
        assertThat(TimelineGridSnap.MIN_1.getIntervalMs()).isEqualTo(60000L);
    }

    @Test
    @DisplayName("Sollte Zeitpunkte exakt auf das gewählte Raster einrasten (Snapping)")
    void shouldSnapTimestampsCorrectly() {
        // FREE: unverändert
        assertThat(TimelineGridSnap.FREE.snap(4231L)).isEqualTo(4231L);

        // SEC_1: Rundung auf 1000ms
        assertThat(TimelineGridSnap.SEC_1.snap(1400L)).isEqualTo(1000L);
        assertThat(TimelineGridSnap.SEC_1.snap(1600L)).isEqualTo(2000L);

        // SEC_5: Rundung auf 5000ms
        assertThat(TimelineGridSnap.SEC_5.snap(2400L)).isEqualTo(0L);
        assertThat(TimelineGridSnap.SEC_5.snap(2600L)).isEqualTo(5000L);
        assertThat(TimelineGridSnap.SEC_5.snap(7499L)).isEqualTo(5000L);
        assertThat(TimelineGridSnap.SEC_5.snap(7500L)).isEqualTo(10000L);

        // SEC_10: Rundung auf 10000ms
        assertThat(TimelineGridSnap.SEC_10.snap(14900L)).isEqualTo(10000L);
        assertThat(TimelineGridSnap.SEC_10.snap(15100L)).isEqualTo(20000L);

        // SEC_30: Rundung auf 30000ms
        assertThat(TimelineGridSnap.SEC_30.snap(14000L)).isEqualTo(0L);
        assertThat(TimelineGridSnap.SEC_30.snap(16000L)).isEqualTo(30000L);

        // MIN_1: Rundung auf 60000ms
        assertThat(TimelineGridSnap.MIN_1.snap(29000L)).isEqualTo(0L);
        assertThat(TimelineGridSnap.MIN_1.snap(31000L)).isEqualTo(60000L);
    }

    @Test
    @DisplayName("Sollte Labels auf Deutsch, Englisch und Russisch reaktiv lokalisieren")
    void shouldLocalizeLabelsInAllLanguages() {
        // Deutsch
        I18n.setLanguage("de");
        assertThat(TimelineGridSnap.FREE.getLabel()).isEqualTo("Frei");
        assertThat(TimelineGridSnap.SEC_1.getLabel()).isEqualTo("1 Sekunde");
        assertThat(TimelineGridSnap.SEC_5.getLabel()).isEqualTo("5 Sekunden");
        assertThat(TimelineGridSnap.SEC_10.getLabel()).isEqualTo("10 Sekunden");
        assertThat(TimelineGridSnap.SEC_30.getLabel()).isEqualTo("30 Sekunden");
        assertThat(TimelineGridSnap.MIN_1.getLabel()).isEqualTo("1 Minute");

        // Englisch
        I18n.setLanguage("en");
        assertThat(TimelineGridSnap.FREE.getLabel()).isEqualTo("Free");
        assertThat(TimelineGridSnap.SEC_1.getLabel()).isEqualTo("1 Second");
        assertThat(TimelineGridSnap.SEC_5.getLabel()).isEqualTo("5 Seconds");
        assertThat(TimelineGridSnap.SEC_10.getLabel()).isEqualTo("10 Seconds");
        assertThat(TimelineGridSnap.SEC_30.getLabel()).isEqualTo("30 Seconds");
        assertThat(TimelineGridSnap.MIN_1.getLabel()).isEqualTo("1 Minute");

        // Russisch
        I18n.setLanguage("ru");
        assertThat(TimelineGridSnap.FREE.getLabel()).isEqualTo("Свободно");
        assertThat(TimelineGridSnap.SEC_1.getLabel()).isEqualTo("1 секунда");
        assertThat(TimelineGridSnap.SEC_5.getLabel()).isEqualTo("5 секунд");
        assertThat(TimelineGridSnap.SEC_10.getLabel()).isEqualTo("10 секунд");
        assertThat(TimelineGridSnap.SEC_30.getLabel()).isEqualTo("30 секунд");
        assertThat(TimelineGridSnap.MIN_1.getLabel()).isEqualTo("1 минута");
    }
}

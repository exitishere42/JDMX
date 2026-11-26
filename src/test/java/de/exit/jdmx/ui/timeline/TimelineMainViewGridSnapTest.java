package de.exit.jdmx.ui.timeline;

import de.exit.jdmx.core.i18n.I18n;
import de.exit.jdmx.core.model.TimelineGridSnap;
import de.exit.jdmx.service.TimelineService;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für das Raster- & Einrast-Dropdown (Grid Snapping) in {@link TimelineMainView}.
 */
class TimelineMainViewGridSnapTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit bereits aktiv
        }
    }

    private void runOnFxThreadAndWait(Runnable action) throws Exception {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                latch.countDown();
            }
        });
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("Sollte Grid-Snap ComboBox in Top-Toolbar initialisieren und anbinden")
    void shouldInitializeGridSnapComboBoxInTopToolbar() throws Exception {
        AtomicReference<TimelineService> serviceRef = new AtomicReference<>();
        AtomicReference<TimelineMainView> viewRef = new AtomicReference<>();

        runOnFxThreadAndWait(() -> {
            TimelineService s = new TimelineService();
            serviceRef.set(s);
            viewRef.set(new TimelineMainView(s));
        });

        TimelineService service = serviceRef.get();
        TimelineMainView view = viewRef.get();

        assertThat(view.getCmbSnap()).isNotNull();
        assertThat(view.getCmbSnap().getStyleClass()).contains("timeline-snap-combo");
        assertThat(view.getCmbSnap().getItems()).containsExactly(TimelineGridSnap.values());
        assertThat(view.getCmbSnap().getValue()).isEqualTo(TimelineGridSnap.SEC_5);
        assertThat(service.getGridSnap()).isEqualTo(TimelineGridSnap.SEC_5);
    }

    @Test
    @DisplayName("Sollte Auswahl-Änderung in ComboBox an TimelineService übertragen")
    void shouldPropagateSelectionChangeToService() throws Exception {
        AtomicReference<TimelineService> serviceRef = new AtomicReference<>();
        AtomicReference<TimelineMainView> viewRef = new AtomicReference<>();

        runOnFxThreadAndWait(() -> {
            TimelineService s = new TimelineService();
            serviceRef.set(s);
            viewRef.set(new TimelineMainView(s));
        });

        TimelineService service = serviceRef.get();
        TimelineMainView view = viewRef.get();

        runOnFxThreadAndWait(() -> view.getCmbSnap().setValue(TimelineGridSnap.SEC_10));
        assertThat(service.getGridSnap()).isEqualTo(TimelineGridSnap.SEC_10);

        runOnFxThreadAndWait(() -> view.getCmbSnap().setValue(TimelineGridSnap.FREE));
        assertThat(service.getGridSnap()).isEqualTo(TimelineGridSnap.FREE);

        runOnFxThreadAndWait(() -> view.getCmbSnap().setValue(TimelineGridSnap.SEC_30));
        assertThat(service.getGridSnap()).isEqualTo(TimelineGridSnap.SEC_30);
    }

    @Test
    @DisplayName("Sollte Tooltip und Beschriftungen bei Sprachwechsel reaktiv aktualisieren")
    void shouldSupportLocalizationOnLanguageChange() throws Exception {
        AtomicReference<TimelineMainView> viewRef = new AtomicReference<>();

        runOnFxThreadAndWait(() -> {
            I18n.setLanguage("de");
            TimelineService service = new TimelineService();
            viewRef.set(new TimelineMainView(service));
        });

        TimelineMainView view = viewRef.get();

        // Deutsch
        runOnFxThreadAndWait(() -> I18n.setLanguage("de"));
        assertThat(view.getCmbSnap().getTooltip().getText()).isEqualTo("Raster & Einrasten");
        assertThat(TimelineGridSnap.FREE.getLabel()).isEqualTo("Frei");
        assertThat(TimelineGridSnap.SEC_1_4.getLabel()).isEqualTo("1/4 Sekunde");
        assertThat(TimelineGridSnap.SEC_1_2.getLabel()).isEqualTo("1/2 Sekunde");
        assertThat(TimelineGridSnap.SEC_5.getLabel()).isEqualTo("5 Sekunden");

        // Englisch
        runOnFxThreadAndWait(() -> I18n.setLanguage("en"));
        assertThat(view.getCmbSnap().getTooltip().getText()).isEqualTo("Grid & Snapping");
        assertThat(TimelineGridSnap.FREE.getLabel()).isEqualTo("Free");
        assertThat(TimelineGridSnap.SEC_1_4.getLabel()).isEqualTo("1/4 Second");
        assertThat(TimelineGridSnap.SEC_1_2.getLabel()).isEqualTo("1/2 Second");
        assertThat(TimelineGridSnap.SEC_5.getLabel()).isEqualTo("5 Seconds");

        // Russisch
        runOnFxThreadAndWait(() -> I18n.setLanguage("ru"));
        assertThat(view.getCmbSnap().getTooltip().getText()).isEqualTo("Сетка и привязка");
        assertThat(TimelineGridSnap.FREE.getLabel()).isEqualTo("Свободно");
        assertThat(TimelineGridSnap.SEC_1_4.getLabel()).isEqualTo("1/4 секунды");
        assertThat(TimelineGridSnap.SEC_1_2.getLabel()).isEqualTo("1/2 секунды");
        assertThat(TimelineGridSnap.SEC_5.getLabel()).isEqualTo("5 секунд");

        // Zurück auf Deutsch
        runOnFxThreadAndWait(() -> I18n.setLanguage("de"));
    }
}

package de.exit.jdmx.ui;

import de.exit.jdmx.core.i18n.I18n;
import de.exit.jdmx.service.DmxService;
import de.exit.jdmx.service.ShortcutService;
import javafx.application.Platform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit- und Integrationstests für die Virtual-Viewport UI-Skalierung in {@link MainWindow}.
 */
class MainWindowScaleTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Bereits initialisiert
        }
    }

    @AfterEach
    void resetLocale() {
        I18n.setLanguage("de");
    }

    private void runOnFxThread(Runnable action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> exRef = new java.util.concurrent.atomic.AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                exRef.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        if (exRef.get() != null) {
            throw new RuntimeException(exRef.get());
        }
    }

    @Test
    @DisplayName("Sollte logische Dimensionen bei 80% Skalierung für Laptop-Auflösungen vergrößern")
    void shouldScaleLogicalDimensionsForLaptopResolution() throws Exception {
        runOnFxThread(() -> {
            MainWindow window = new MainWindow(new ShortcutService(), new DmxService());
            window.resize(1366, 768);

            window.applyUiScale(0.8);
            window.layout();

            // Bei 80% Skalierung muss die logische Arbeitsfläche 1707.5 x 960 betragen
            assertThat(window.getScalableRoot().getWidth()).isCloseTo(1707.5, within(0.1));
            assertThat(window.getScalableRoot().getHeight()).isCloseTo(960.0, within(0.1));
            assertThat(window.getScalableRoot().getTransforms()).hasSize(1);
            assertThat(window.getScalableRoot().getTransforms().get(0)).isInstanceOf(javafx.scene.transform.Scale.class);

            javafx.scene.transform.Scale scale = (javafx.scene.transform.Scale) window.getScalableRoot().getTransforms().get(0);
            assertThat(scale.getX()).isEqualTo(0.8);
            assertThat(scale.getY()).isEqualTo(0.8);
        });
    }

    @Test
    @DisplayName("Sollte logische Dimensionen bei 125% High-DPI Skalierung anpassen")
    void shouldScaleLogicalDimensionsForHighDpi() throws Exception {
        runOnFxThread(() -> {
            MainWindow window = new MainWindow(new ShortcutService(), new DmxService());
            window.resize(3840, 2160);

            window.applyUiScale(1.25);
            window.layout();

            // Bei 125% Skalierung muss die logische Fläche 3072 x 1728 betragen
            assertThat(window.getScalableRoot().getWidth()).isCloseTo(3072.0, within(0.1));
            assertThat(window.getScalableRoot().getHeight()).isCloseTo(1728.0, within(0.1));
        });
    }

    @Test
    @DisplayName("Sollte Benutzereinstellungen (Sprache, UI-Scale) bei 'Neues Projekt' beibehalten")
    void shouldPreserveSettingsAcrossNewProject() throws Exception {
        runOnFxThread(() -> {
            MainWindow window = new MainWindow(new ShortcutService(), new DmxService());
            window.getSettingsService().setLanguage("en");
            window.applyUiScale(0.8);

            // Neues Projekt erstellen (Ctrl+N)
            window.newProject();

            // Einstellungen dürfen NICHT auf Werkseinstellungen zurückgesetzt worden sein
            assertThat(window.getSettingsService().getLanguage()).isEqualTo("en");
            assertThat(window.getSettingsService().getUiScale()).isEqualTo(0.8);
        });
    }
}

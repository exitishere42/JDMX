package de.exit.jdmx.ui.desk;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Unit-Tests für {@link DeskScrollBar} (Rechteckige, längliche Custom-Scrollbar mit vertikaler Center-Linie und Blau-Aktivierung).
 */
class DeskScrollBarTest {

    @BeforeAll
    static void initJavaFx() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            latch.countDown();
        }
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("Sollte DeskScrollBar mit 512 Kanälen und 32 sichtbaren initialisieren")
    void shouldInitializeWithDefaultBounds() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                DeskScrollBar scrollBar = new DeskScrollBar();

                assertThat(scrollBar.getTotalChannels()).isEqualTo(512);
                assertThat(scrollBar.getVisibleChannels()).isEqualTo(32);
                assertThat(scrollBar.getValue()).isEqualTo(0.0);
                assertThat(scrollBar.getMaxValue()).isEqualTo(512.0 - 32.0);
                assertThat(scrollBar.isPressedState()).isFalse();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        if (error.get() != null) throw error.get();
    }

    @Test
    @DisplayName("Sollte Wert setzen, clampen und Listener benachrichtigen")
    void shouldSetAndClampValue() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                DeskScrollBar scrollBar = new DeskScrollBar();

                AtomicReference<Double> notifiedVal = new AtomicReference<>(-1.0);
                scrollBar.setOnValueChanged(notifiedVal::set);

                scrollBar.setValue(64.0);
                assertThat(scrollBar.getValue()).isEqualTo(64.0);
                assertThat(notifiedVal.get()).isEqualTo(64.0);

                // Nach oben clampen (max 480)
                scrollBar.setValue(999.0);
                assertThat(scrollBar.getValue()).isEqualTo(480.0);

                // Nach unten clampen (min 0)
                scrollBar.setValue(-50.0);
                assertThat(scrollBar.getValue()).isEqualTo(0.0);
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        if (error.get() != null) throw error.get();
    }

    @Test
    @DisplayName("Sollte beim Anklicken in den blauen Aktiv-Zustand wechseln")
    void shouldSwitchToBlueWhenPressed() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                DeskScrollBar scrollBar = new DeskScrollBar();
                scrollBar.setSimulatedDimensions(800.0, 14.0);

                assertThat(scrollBar.isPressedState()).isFalse();
                assertThat(scrollBar.getCurrentThumbColor()).isEqualTo(Color.web("#3A3A40")); // Grau

                // Klick simulieren
                scrollBar.handlePress(50.0);
                assertThat(scrollBar.isPressedState()).isTrue();
                assertThat(scrollBar.getCurrentThumbColor()).isEqualTo(Color.web("#2563EB")); // Blau beim Anklicken!

                // Loslassen simulieren
                scrollBar.handleRelease();
                assertThat(scrollBar.isPressedState()).isFalse();
                assertThat(scrollBar.getCurrentThumbColor()).isEqualTo(Color.web("#3A3A40")); // Wieder grau
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        if (error.get() != null) throw error.get();
    }

    @Test
    @DisplayName("Sollte Ziehen der Scrollbar korrekt auf den Kanalbereich umrechnen")
    void shouldDragThumbAcrossChannels() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                DeskScrollBar scrollBar = new DeskScrollBar();
                scrollBar.setSimulatedDimensions(800.0, 14.0);

                // Bei 800px Breite und min 48px Thumb:
                // Wenn man zur Mitte zieht (ca. x = 400), sollte der Wert ca. 240 sein
                scrollBar.handlePress(0.0);
                scrollBar.handleDrag(400.0);

                assertThat(scrollBar.getValue()).isBetween(220.0, 260.0);
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        if (error.get() != null) throw error.get();
    }
}

package de.exit.jdmx.ui.desk;

import de.exit.jdmx.core.model.ChannelType;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Unit-Tests für {@link CanvasDeskView} (die performante Canvas-Balken-Faderbank).
 */
class CanvasDeskViewTest {

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
    @DisplayName("Sollte 512 Kanäle mit Standardwert 0 initialisieren")
    void shouldInitializeWith512ChannelsAtZero() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                CanvasDeskView desk = new CanvasDeskView();

                assertThat(desk.getTotalChannels()).isEqualTo(512);
                assertThat(desk.getVisibleCount()).isEqualTo(32);

                for (int ch = 1; ch <= 512; ch++) {
                    assertThat(desk.getChannelValue(ch)).isEqualTo(0);
                }
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
    @DisplayName("Sollte Kanalwert setzen, abfragen und Listener benachrichtigen")
    void shouldSetAndNotifyChannelValue() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                CanvasDeskView desk = new CanvasDeskView();

                AtomicInteger notifiedChannel = new AtomicInteger(-1);
                AtomicInteger notifiedValue = new AtomicInteger(-1);
                desk.setOnChannelValueChanged((ch, val) -> {
                    notifiedChannel.set(ch);
                    notifiedValue.set(val);
                });

                desk.setChannelValue(10, 185);

                assertThat(desk.getChannelValue(10)).isEqualTo(185);
                assertThat(notifiedChannel.get()).isEqualTo(10);
                assertThat(notifiedValue.get()).isEqualTo(185);

                // Ungültige Kanäle ignorieren oder clampen
                desk.setChannelValue(10, 300);
                assertThat(desk.getChannelValue(10)).isEqualTo(255);

                desk.setChannelValue(10, -50);
                assertThat(desk.getChannelValue(10)).isEqualTo(0);
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
    @DisplayName("Sollte alle Kanäle auf 0 zurücksetzen können")
    void shouldResetAllChannels() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                CanvasDeskView desk = new CanvasDeskView();

                desk.setChannelValue(1, 255);
                desk.setChannelValue(2, 128);
                desk.setChannelValue(512, 64);

                assertThat(desk.getChannelValue(1)).isEqualTo(255);
                assertThat(desk.getChannelValue(2)).isEqualTo(128);

                desk.resetAll();

                assertThat(desk.getChannelValue(1)).isEqualTo(0);
                assertThat(desk.getChannelValue(2)).isEqualTo(0);
                assertThat(desk.getChannelValue(512)).isEqualTo(0);
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
    @DisplayName("Sollte Scrollen zu Kanal unterstützen und ScrollBar aktualisieren")
    void shouldScrollToChannel() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                CanvasDeskView desk = new CanvasDeskView();

                assertThat(desk.getScrollOffset()).isEqualTo(0.0);

                // Zu Kanal 33 scrollen (Bank 2)
                desk.scrollToChannel(33);
                assertThat(desk.getScrollOffset()).isEqualTo(32.0);

                // Zu weit scrollen sollte gecapt werden
                desk.scrollToChannel(512);
                assertThat(desk.getScrollOffset()).isEqualTo(512 - 32);
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
    @DisplayName("Sollte Scheinwerfer-Attribute korrekt zuordnen und Farben ableiten")
    void shouldAssignFixtureChannelsAndColors() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                CanvasDeskView desk = new CanvasDeskView();

                desk.assignFixtureChannel(1, "Moving Head 1", ChannelType.DIMMER, Color.CYAN);
                desk.assignFixtureChannel(2, "Moving Head 1", ChannelType.PAN, Color.CYAN);
                desk.assignFixtureChannel(3, "Moving Head 1", ChannelType.RED, Color.CYAN);

                DeskChannelData ch1 = desk.getChannelData(1);
                DeskChannelData ch2 = desk.getChannelData(2);
                DeskChannelData ch3 = desk.getChannelData(3);
                DeskChannelData ch4 = desk.getChannelData(4); // ungepatcht

                assertThat(ch1.getFunctionName()).isEqualTo("Dimmer");
                assertThat(ch2.getFunctionName()).isEqualTo("Pan");
                assertThat(ch3.getFunctionName()).isEqualTo("Rot");
                assertThat(ch4.getFunctionName()).isEqualTo("-");

                // Dimmer -> Warm Amber
                assertThat(ch1.getBarColor()).isEqualTo(Color.web("#FEDC94"));
                // Pan -> Cyan
                assertThat(ch2.getBarColor()).isEqualTo(Color.web("#38BDF8"));
                // Rot -> Signalrot
                assertThat(ch3.getBarColor()).isEqualTo(Color.web("#EF4444"));
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
    @DisplayName("Sollte Pixel-Koordinaten mathematisch korrekt in Kanal und DMX-Wert umrechnen")
    void shouldCalculateChannelAndValueFromCoordinates() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                CanvasDeskView desk = new CanvasDeskView();
                // Manuelle Dimensionen simulieren: 800 x 200
                desk.setSimulatedDimensions(800.0, 200.0);

                // Spaltenbreite bei 800px und 32 Kanälen = 25px
                // Kanal 0 (Kanal 1): x = 12.5 (Mitte von Spalte 1)
                int channel = desk.getChannelAtX(12.5);
                assertThat(channel).isEqualTo(1);

                // Kanal 1 (Kanal 2): x = 37.5
                int channel2 = desk.getChannelAtX(37.5);
                assertThat(channel2).isEqualTo(2);

                // DMX-Wert Berechnung:
                // TrackTop = 8, TrackBottom = 178, TrackH = 170
                // Bei y = 8 (ganz oben) -> Wert 255
                int valTop = desk.getValueAtY(8.0);
                assertThat(valTop).isEqualTo(255);

                // Bei y = 178 (ganz unten) -> Wert 0
                int valBottom = desk.getValueAtY(178.0);
                assertThat(valBottom).isEqualTo(0);

                // Bei y = 93 (genau in der Mitte) -> Wert ca. 128
                int valMid = desk.getValueAtY(93.0);
                assertThat(valMid).isBetween(126, 129);
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

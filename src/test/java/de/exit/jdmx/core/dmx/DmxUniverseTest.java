package de.exit.jdmx.core.dmx;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TDD Unit-Tests für {@link DmxUniverse}.
 */
class DmxUniverseTest {

    private DmxUniverse universe;

    @BeforeEach
    void setUp() {
        universe = new DmxUniverse(0);
    }

    @Test
    @DisplayName("Initialzustand: Alle 512 Kanäle stehen auf Wert 0")
    void initialState_allChannelsZero() {
        assertThat(universe.getUniverseIndex()).isEqualTo(0);
        byte[] data = universe.getDmxData();
        assertThat(data).hasSize(512);
        for (int i = 1; i <= 512; i++) {
            assertThat(universe.getChannel(i)).isZero();
        }
    }

    @Test
    @DisplayName("Gültige Kanalwerte setzen und abfragen (Grenzen 1 und 512)")
    void setAndGetChannel_validValues() {
        universe.setChannel(1, 255);
        universe.setChannel(512, 128);
        universe.setChannel(256, 42);

        assertThat(universe.getChannel(1)).isEqualTo(255);
        assertThat(universe.getChannel(512)).isEqualTo(128);
        assertThat(universe.getChannel(256)).isEqualTo(42);

        byte[] data = universe.getDmxData();
        assertThat(data[0] & 0xFF).isEqualTo(255);
        assertThat(data[511] & 0xFF).isEqualTo(128);
        assertThat(data[255] & 0xFF).isEqualTo(42);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 513, 1000})
    @DisplayName("Ungültige Kanalnummern werfen IllegalArgumentException")
    void invalidChannel_throwsException(int invalidChannel) {
        assertThatThrownBy(() -> universe.setChannel(invalidChannel, 100))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> universe.getChannel(invalidChannel))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -100, 256, 500})
    @DisplayName("Ungültige DMX-Werte außerhalb von 0-255 werfen IllegalArgumentException")
    void invalidValue_throwsException(int invalidValue) {
        assertThatThrownBy(() -> universe.setChannel(1, invalidValue))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Blackout setzt alle 512 Kanäle auf 0 zurück")
    void blackout_resetsAllChannels() {
        universe.setChannel(1, 255);
        universe.setChannel(100, 200);
        universe.setChannel(512, 255);

        universe.blackout();

        for (int i = 1; i <= 512; i++) {
            assertThat(universe.getChannel(i)).isZero();
        }
        byte[] data = universe.getDmxData();
        for (byte b : data) {
            assertThat(b).isZero();
        }
    }

    @Test
    @DisplayName("Bulk-Update setzt mehrere Kanäle auf einmal")
    void setChannels_bulkUpdate() {
        byte[] values = new byte[]{(byte) 10, (byte) 20, (byte) 30};
        universe.setChannels(10, values);

        assertThat(universe.getChannel(10)).isEqualTo(10);
        assertThat(universe.getChannel(11)).isEqualTo(20);
        assertThat(universe.getChannel(12)).isEqualTo(30);
    }

    @Test
    @DisplayName("Bulk-Update übersteigt Adressbereich wirft Exception")
    void setChannels_overflowThrowsException() {
        byte[] values = new byte[10];
        assertThatThrownBy(() -> universe.setChannels(505, values))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Thread-Sicherheit bei parallelem Schreiben und Lesen")
    void concurrentWriteAndRead_isThreadSafe() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(4);

        for (int t = 0; t < 4; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < 1000; i++) {
                        int ch = (i % 512) + 1;
                        universe.setChannel(ch, (threadId * 50 + i) % 256);
                        universe.getChannel(ch);
                        universe.getDmxData();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        assertThat(finished).isTrue();
    }
}

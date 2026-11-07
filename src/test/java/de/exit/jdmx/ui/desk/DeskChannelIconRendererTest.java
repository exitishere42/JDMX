package de.exit.jdmx.ui.desk;

import de.exit.jdmx.core.model.ChannelType;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * TDD Unit-Tests für {@link DeskChannelIconRenderer}.
 * Prüft das Direct-Rendering aller 19 QLC+-kompatiblen DMX-Funktions-Icons.
 */
class DeskChannelIconRendererTest {

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
    @DisplayName("Sollte für alle 19 ChannelType-Werte fehlerfrei ein Icon auf den Canvas zeichnen")
    void shouldRenderAllChannelTypesWithoutError() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Canvas canvas = new Canvas(100, 100);
                GraphicsContext gc = canvas.getGraphicsContext2D();

                for (ChannelType type : ChannelType.values()) {
                    assertThatCode(() -> DeskChannelIconRenderer.renderIcon(gc, 50.0, 50.0, 16.0, type, Color.WHITE))
                            .doesNotThrowAnyException();
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
    @DisplayName("Sollte bei null als Kanaltyp oder null als GraphicsContext sicher abfangen")
    void shouldHandleNullGracefully() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Canvas canvas = new Canvas(100, 100);
                GraphicsContext gc = canvas.getGraphicsContext2D();

                // null-Type darf keine Exception werfen
                assertThatCode(() -> DeskChannelIconRenderer.renderIcon(gc, 50.0, 50.0, 16.0, null, Color.WHITE))
                        .doesNotThrowAnyException();

                // null-GC darf keine Exception werfen
                assertThatCode(() -> DeskChannelIconRenderer.renderIcon(null, 50.0, 50.0, 16.0, ChannelType.DIMMER, Color.WHITE))
                        .doesNotThrowAnyException();
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
    @DisplayName("Sollte spezifische Icon-Farben für Farbkanäle (RGBW, Amber, UV) und Dimmer korrekt ermitteln")
    void shouldResolveIconColorProperly() {
        assertThat(DeskChannelIconRenderer.resolveIconColor(ChannelType.DIMMER, Color.WHITE)).isEqualTo(Color.web("#F59E0B"));
        assertThat(DeskChannelIconRenderer.resolveIconColor(ChannelType.RED, Color.WHITE)).isEqualTo(Color.web("#EF4444"));
        assertThat(DeskChannelIconRenderer.resolveIconColor(ChannelType.GREEN, Color.WHITE)).isEqualTo(Color.web("#10B981"));
        assertThat(DeskChannelIconRenderer.resolveIconColor(ChannelType.BLUE, Color.WHITE)).isEqualTo(Color.web("#3B82F6"));
        assertThat(DeskChannelIconRenderer.resolveIconColor(ChannelType.AMBER, Color.WHITE)).isEqualTo(Color.web("#F59E0B"));
        assertThat(DeskChannelIconRenderer.resolveIconColor(ChannelType.UV, Color.WHITE)).isEqualTo(Color.web("#A855F7"));
        assertThat(DeskChannelIconRenderer.resolveIconColor(ChannelType.WHITE, Color.WHITE)).isEqualTo(Color.web("#F3F4F6"));
        assertThat(DeskChannelIconRenderer.resolveIconColor(ChannelType.PAN, Color.WHITE)).isEqualTo(Color.web("#38BDF8"));
        assertThat(DeskChannelIconRenderer.resolveIconColor(ChannelType.TILT, Color.WHITE)).isEqualTo(Color.web("#38BDF8"));
        assertThat(DeskChannelIconRenderer.resolveIconColor(ChannelType.SHUTTER, Color.WHITE)).isEqualTo(Color.web("#FACC15"));
        assertThat(DeskChannelIconRenderer.resolveIconColor(ChannelType.PRISM, Color.WHITE)).isEqualTo(Color.web("#2DD4BF"));
    }
}

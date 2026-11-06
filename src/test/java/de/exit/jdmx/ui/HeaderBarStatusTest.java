package de.exit.jdmx.ui;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für den neuen Header-Statusbereich in {@link HeaderBar}.
 */
class HeaderBarStatusTest {

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
    @DisplayName("Soll Statusmeldung mit Icon und Text korrekt setzen und sichtbar schalten")
    void shouldShowStatusMessageCorrectly() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                HeaderBar header = new HeaderBar();
                assertThat(header.getStatusArea().isVisible()).isFalse();

                header.showStatus("Projekt gespeichert", "check", Color.web("#34D399"), Duration.millis(500));

                assertThat(header.getStatusArea().isVisible()).isTrue();
                assertThat(header.getStatusLabel().getText()).isEqualTo("Projekt gespeichert");
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("Soll bei leerem Text keine Änderung durchführen")
    void shouldIgnoreBlankText() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                HeaderBar header = new HeaderBar();
                header.showStatus("", "check", Color.web("#34D399"), Duration.millis(500));
                assertThat(header.getStatusArea().isVisible()).isFalse();
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }
}

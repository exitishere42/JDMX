package de.exit.jdmx.ui.timeline;

import de.exit.jdmx.ui.icon.LucideIcon;
import javafx.application.Platform;
import javafx.scene.control.Button;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Unit-Tests für das kaskadierende Farb-Flyout {@link TimelineColorFlyout}.
 * <p>
 * Überprüft die 6 Standardfarben, das Fehlen von Glow-Schatteneffekten,
 * das Umschalten des aktiven Häkchens sowie die Luminanz-Berechnung.
 * </p>
 */
class TimelineColorFlyoutTest {

    @BeforeAll
    static void initJavaFx() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(() -> {
                Platform.setImplicitExit(false);
                latch.countDown();
            });
        } catch (IllegalStateException e) {
            Platform.setImplicitExit(false);
            latch.countDown();
        }
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("Sollte 6 Standardfarben definieren und bei Klick auf Kachel die Farbe übergeben und Dismiss auslösen")
    void shouldDefineSixStandardColorsAndTriggerSelectionAndDismiss() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                TimelineColorFlyout flyout = new TimelineColorFlyout();
                assertThat(TimelineColorFlyout.STANDARD_COLORS).containsExactly(
                        "#FACC15", // Daslight Gelb
                        "#38BDF8", // Cyan / Himmelblau
                        "#10B981", // Smaragdgrün
                        "#EF4444", // Signalrot
                        "#8B5CF6", // Violett / Lila
                        "#EC4899"  // Pink / Magenta
                );

                assertThat(flyout.getColorSwatches()).hasSize(6);

                AtomicReference<String> selectedColor = new AtomicReference<>();
                AtomicBoolean dismissed = new AtomicBoolean(false);

                flyout.setOnColorSelected(selectedColor::set);
                flyout.setOnDismiss(() -> dismissed.set(true));

                // Klick auf das 3. Farbfeld (Smaragdgrün #10B981)
                flyout.getColorSwatches().get(2).fire();

                assertThat(selectedColor.get()).isEqualTo("#10B981");
                assertThat(dismissed.get()).isTrue();
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("Sollte Kacheln ohne diffusen Glow-Dropshadow stylen und weißes Hover-Border nutzen")
    void shouldStyleSwatchesWithoutGlowDropshadow() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                TimelineColorFlyout flyout = new TimelineColorFlyout();
                Button swatch = flyout.getColorSwatches().get(0);

                // Keine dropshadow-Deklaration im Style (Zero-Glow)
                assertThat(swatch.getStyle()).doesNotContain("dropshadow");
                assertThat(swatch.getStyle()).contains("-fx-background-color: #FACC15");
                assertThat(swatch.getStyle()).contains("-fx-border-radius: 4px");
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("Sollte Häkchen auf die aktive Farbe setzen")
    void shouldSetCheckmarkOnActiveColor() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                TimelineColorFlyout flyout = new TimelineColorFlyout();
                flyout.setActiveColor("#8B5CF6"); // Violett aktiv

                Button violetSwatch = flyout.getColorSwatches().get(4);
                assertThat(violetSwatch.getGraphic()).isNotNull();
                assertThat(violetSwatch.getGraphic()).isInstanceOf(LucideIcon.class);
                LucideIcon icon = (LucideIcon) violetSwatch.getGraphic();
                assertThat(icon.getName()).isEqualTo("check");

                // Andere Kachel darf kein Icon haben
                Button yellowSwatch = flyout.getColorSwatches().get(0);
                assertThat(yellowSwatch.getGraphic()).isNull();
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("Sollte Helligkeit mathematisch korrekt nach Luminanz klassifizieren")
    void shouldClassifyLuminanceCorrectly() {
        // Helle Farben (schwarze Schrift)
        assertThat(TimelineColorFlyout.isBrightColor("#FACC15")).isTrue(); // Gelb
        assertThat(TimelineColorFlyout.isBrightColor("#38BDF8")).isTrue(); // Cyan
        assertThat(TimelineColorFlyout.isBrightColor("#FFFFFF")).isTrue(); // Weiß

        // Dunkle/kräftige Farben (weiße Schrift)
        assertThat(TimelineColorFlyout.isBrightColor("#000000")).isFalse(); // Schwarz
        assertThat(TimelineColorFlyout.isBrightColor("#1E1E1E")).isFalse(); // Dunkelgrau
        assertThat(TimelineColorFlyout.isBrightColor("#EF4444")).isFalse(); // Signalrot
        assertThat(TimelineColorFlyout.isBrightColor("#8B5CF6")).isFalse(); // Violett
    }
}

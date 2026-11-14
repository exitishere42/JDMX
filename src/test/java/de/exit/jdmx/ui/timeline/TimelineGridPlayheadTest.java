package de.exit.jdmx.ui.timeline;

import de.exit.jdmx.service.TimelineService;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für Playhead- und Positionsberechnungen in {@link TimelineGridView}.
 */
class TimelineGridPlayheadTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit aktiv
        }
    }

    @Test
    @DisplayName("Sollte Millisekunden exakt in X-Pixelpositionen umrechnen")
    void shouldConvertMillisToX() {
        TimelineService service = new TimelineService();
        TimelineGridView grid = new TimelineGridView(service);

        assertThat(grid.millisToX(0L)).isEqualTo(0.0);
        assertThat(grid.millisToX(1000L)).isEqualTo(TimelineGridView.PIXELS_PER_SECOND);
        assertThat(grid.millisToX(2000L)).isEqualTo(TimelineGridView.PIXELS_PER_SECOND * 2.0);
    }

    @Test
    @DisplayName("Sollte X-Pixelpositionen exakt in Millisekunden umrechnen")
    void shouldConvertXToMillis() {
        TimelineService service = new TimelineService();
        TimelineGridView grid = new TimelineGridView(service);

        assertThat(grid.xToMillis(0.0)).isEqualTo(0L);
        assertThat(grid.xToMillis(TimelineGridView.PIXELS_PER_SECOND)).isEqualTo(1000L);
        assertThat(grid.xToMillis(TimelineGridView.PIXELS_PER_SECOND * 3.5)).isEqualTo(3500L);
    }

    @Test
    @DisplayName("Sollte negative X-Werte auf 0ms begrenzen")
    void shouldClampNegativeXToZeroMillis() {
        TimelineService service = new TimelineService();
        TimelineGridView grid = new TimelineGridView(service);

        assertThat(grid.xToMillis(-50.0)).isEqualTo(0L);
    }
}

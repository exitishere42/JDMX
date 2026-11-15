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

    @Test
    @DisplayName("Sollte Song-Clips an bestimmten Koordinaten präzise erkennen")
    void shouldFindClipTrackAtCoordinates() {
        TimelineService service = new TimelineService();
        de.exit.jdmx.core.model.TimelineTrack track1 = service.getTracks().get(0);
        track1.setSongName("test.mp3");
        track1.setDurationMs(30_000L);
        track1.setStartTimeMs(5_000L); // Clip von 5s (240px) bis 35s (1680px), y von 0 bis 48px

        TimelineGridView grid = new TimelineGridView(service);

        // Innerhalb des Clips
        assertThat(grid.findClipTrackAt(300.0, 20.0)).isEqualTo(track1);

        // Vor dem Clip (x < 240)
        assertThat(grid.findClipTrackAt(100.0, 20.0)).isNull();

        // Unterhalb des Tracks (y > 48)
        assertThat(grid.findClipTrackAt(300.0, 80.0)).isNull();
    }

    @Test
    @DisplayName("Sollte Playhead nur bewegen wenn Dreieck am oberen Rand gedraggt wird")
    void shouldOnlyMovePlayheadWhenDraggingTopHandle() {
        TimelineService service = new TimelineService();
        service.setPositionMs(0L);
        TimelineGridView grid = new TimelineGridView(service);

        // Klick auf freie Fläche (y = 150) -> Playhead darf sich NICHT verstellen!
        javafx.scene.input.MouseEvent freeClick = new javafx.scene.input.MouseEvent(
                javafx.scene.input.MouseEvent.MOUSE_PRESSED,
                200.0, 150.0, 200.0, 150.0,
                javafx.scene.input.MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, false, false, false, null
        );
        grid.fireEvent(freeClick);
        assertThat(service.getCurrentPositionMs()).isEqualTo(0L);

        // Klick auf oberes Dreieck (x = 0, y = 10) -> Playhead wird aktiviert & bewegt
        javafx.scene.input.MouseEvent handlePress = new javafx.scene.input.MouseEvent(
                javafx.scene.input.MouseEvent.MOUSE_PRESSED,
                2.0, 10.0, 2.0, 10.0,
                javafx.scene.input.MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, false, false, false, null
        );
        grid.fireEvent(handlePress);

        javafx.scene.input.MouseEvent handleDrag = new javafx.scene.input.MouseEvent(
                javafx.scene.input.MouseEvent.MOUSE_DRAGGED,
                240.0, 10.0, 240.0, 10.0,
                javafx.scene.input.MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, false, false, false, null
        );
        grid.fireEvent(handleDrag);

        assertThat(service.getCurrentPositionMs()).isEqualTo(5000L);
    }
}

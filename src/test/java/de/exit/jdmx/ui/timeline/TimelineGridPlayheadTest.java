package de.exit.jdmx.ui.timeline;

import de.exit.jdmx.service.TimelineService;
import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
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

    @Test
    @DisplayName("Sollte beim Loslassen eines gedraggten Clips resyncTrackImmediate aufrufen")
    void shouldResyncTrackImmediateOnClipRelease() {
        TimelineService service = new TimelineService();
        de.exit.jdmx.core.model.TimelineTrack track1 = service.getTracks().get(0);
        track1.setSongName("test.mp3");
        track1.setDurationMs(30_000L);
        track1.setStartTimeMs(0L);

        TimelineGridView grid = new TimelineGridView(service);

        // Klick auf Clip
        javafx.scene.input.MouseEvent clipPress = new javafx.scene.input.MouseEvent(
                javafx.scene.input.MouseEvent.MOUSE_PRESSED,
                50.0, 20.0, 50.0, 20.0,
                javafx.scene.input.MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, false, false, false, null
        );
        grid.fireEvent(clipPress);

        // Drag Clip um 100px
        javafx.scene.input.MouseEvent clipDrag = new javafx.scene.input.MouseEvent(
                javafx.scene.input.MouseEvent.MOUSE_DRAGGED,
                150.0, 20.0, 150.0, 20.0,
                javafx.scene.input.MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, false, false, false, null
        );
        grid.fireEvent(clipDrag);
        assertThat(track1.getStartTimeMs()).isGreaterThan(0L);

        // Release Clip
        javafx.scene.input.MouseEvent clipRelease = new javafx.scene.input.MouseEvent(
                javafx.scene.input.MouseEvent.MOUSE_RELEASED,
                150.0, 20.0, 150.0, 20.0,
                javafx.scene.input.MouseButton.PRIMARY, 1,
                false, false, false, false, false, false, false, false, false, false, null
        );
        grid.fireEvent(clipRelease);

        // Track-Startzeit bleibt gespeichert
        assertThat(track1.getStartTimeMs()).isGreaterThan(0L);
    }

    @Test
    @DisplayName("Sollte Verschieben von Clips während laufender Wiedergabe blockieren")
    void shouldPreventClipDragDuringPlayback() {
        TimelineService service = new TimelineService();
        de.exit.jdmx.core.model.TimelineTrack track1 = service.getTracks().get(0);
        track1.setSongName("test.mp3");
        track1.setDurationMs(30_000L);
        track1.setStartTimeMs(0L);

        TimelineGridView grid = new TimelineGridView(service);

        // Wiedergabe starten
        service.startPlayback();
        assertThat(service.isPlaying()).isTrue();

        // Klick auf Clip während Playback
        MouseEvent clipPress = new MouseEvent(
                MouseEvent.MOUSE_PRESSED,
                50.0, 20.0, 50.0, 20.0,
                MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, false, false, false, null
        );
        grid.fireEvent(clipPress);

        // Drag Clip um 100px
        MouseEvent clipDrag = new MouseEvent(
                MouseEvent.MOUSE_DRAGGED,
                150.0, 20.0, 150.0, 20.0,
                MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, false, false, false, null
        );
        grid.fireEvent(clipDrag);

        // Startzeit darf sich während Wiedergabe NICHT geändert haben
        assertThat(track1.getStartTimeMs()).isEqualTo(0L);
        assertThat(grid.getCursor()).isNotEqualTo(Cursor.MOVE);

        // Release Clip
        MouseEvent clipRelease = new MouseEvent(
                MouseEvent.MOUSE_RELEASED,
                150.0, 20.0, 150.0, 20.0,
                MouseButton.PRIMARY, 1,
                false, false, false, false, false, false, false, false, false, false, null
        );
        grid.fireEvent(clipRelease);

        assertThat(track1.getStartTimeMs()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Sollte über Clips während Playback den Standard-Cursor statt Cursor.HAND anzeigen")
    void shouldShowDefaultCursorOverClipDuringPlayback() {
        TimelineService service = new TimelineService();
        de.exit.jdmx.core.model.TimelineTrack track1 = service.getTracks().get(0);
        track1.setSongName("test.mp3");
        track1.setDurationMs(30_000L);
        track1.setStartTimeMs(0L);

        TimelineGridView grid = new TimelineGridView(service);

        // 1. Wenn gestoppt: Hand-Cursor über Clip
        assertThat(service.isPlaying()).isFalse();
        MouseEvent hoverStopped = new MouseEvent(
                MouseEvent.MOUSE_MOVED,
                50.0, 20.0, 50.0, 20.0,
                MouseButton.NONE, 0,
                false, false, false, false, false, false, false, false, false, false, null
        );
        grid.fireEvent(hoverStopped);
        assertThat(grid.getCursor()).isEqualTo(Cursor.HAND);

        // 2. Wenn Playback läuft: Normaler Cursor über Clip (gesperrt)
        service.startPlayback();
        assertThat(service.isPlaying()).isTrue();

        MouseEvent hoverPlaying = new MouseEvent(
                MouseEvent.MOUSE_MOVED,
                50.0, 20.0, 50.0, 20.0,
                MouseButton.NONE, 0,
                false, false, false, false, false, false, false, false, false, false, null
        );
        grid.fireEvent(hoverPlaying);
        assertThat(grid.getCursor()).isEqualTo(Cursor.DEFAULT);

        // 3. Nach Stop: Wieder Hand-Cursor
        service.stopPlayback();
        grid.fireEvent(hoverStopped);
        assertThat(grid.getCursor()).isEqualTo(Cursor.HAND);
    }

    @Test
    @DisplayName("Sollte während Playhead-Dragging bei laufendem Playback pausieren und beim Loslassen sauber fortsetzen")
    void shouldPauseDuringPlayheadDragAndResumeOnRelease() {
        TimelineService service = new TimelineService();
        TimelineGridView grid = new TimelineGridView(service);
        service.startPlayback();
        assertThat(service.isPlaying()).isTrue();

        // Klick auf Playhead-Griff (x = 0, y = 10)
        MouseEvent handlePress = new MouseEvent(
                MouseEvent.MOUSE_PRESSED,
                2.0, 10.0, 2.0, 10.0,
                MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, false, false, false, null
        );
        grid.fireEvent(handlePress);

        // Während des Draggings ist Playback pausiert (kein Audio-Stacking)
        assertThat(service.isPlaying()).isFalse();

        // Drag an Sekunde 5 (x = 240)
        MouseEvent handleDrag = new MouseEvent(
                MouseEvent.MOUSE_DRAGGED,
                240.0, 10.0, 240.0, 10.0,
                MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, false, false, false, null
        );
        grid.fireEvent(handleDrag);
        assertThat(service.getCurrentPositionMs()).isEqualTo(5000L);
        assertThat(service.isPlaying()).isFalse();

        // Loslassen des Griffs -> Wiedergabe setzt an Zielposition nahtlos fort
        MouseEvent handleRelease = new MouseEvent(
                MouseEvent.MOUSE_RELEASED,
                240.0, 10.0, 240.0, 10.0,
                MouseButton.PRIMARY, 1,
                false, false, false, false, false, false, false, false, false, false, null
        );
        grid.fireEvent(handleRelease);

        assertThat(service.getCurrentPositionMs()).isEqualTo(5000L);
        assertThat(service.isPlaying()).isTrue();
    }
}

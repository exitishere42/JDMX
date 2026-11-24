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

        assertThat(grid.getPixelsPerSecond()).isEqualTo(TimelineGridView.DEFAULT_PIXELS_PER_SECOND);
        assertThat(grid.millisToX(0L)).isEqualTo(0.0);
        assertThat(grid.millisToX(1000L)).isEqualTo(grid.getPixelsPerSecond());
        assertThat(grid.millisToX(2000L)).isEqualTo(grid.getPixelsPerSecond() * 2.0);
    }

    @Test
    @DisplayName("Sollte X-Pixelpositionen exakt in Millisekunden umrechnen")
    void shouldConvertXToMillis() {
        TimelineService service = new TimelineService();
        TimelineGridView grid = new TimelineGridView(service);

        assertThat(grid.xToMillis(0.0)).isEqualTo(0L);
        assertThat(grid.xToMillis(grid.getPixelsPerSecond())).isEqualTo(1000L);
        assertThat(grid.xToMillis(grid.getPixelsPerSecond() * 3.5)).isEqualTo(3500L);
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
        track1.setStartTimeMs(5_000L); // Clip von 5s bis 35s

        TimelineGridView grid = new TimelineGridView(service);
        double startX = grid.millisToX(5_000L);

        // Innerhalb des Clips (startX + 50px)
        assertThat(grid.findClipTrackAt(startX + 50.0, 20.0)).isEqualTo(track1);

        // Vor dem Clip (startX - 50px)
        assertThat(grid.findClipTrackAt(Math.max(0.0, startX - 50.0), 20.0)).isNull();

        // Unterhalb des Tracks (y > track1.getHeight())
        assertThat(grid.findClipTrackAt(startX + 50.0, track1.getHeight() + 10.0)).isNull();
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

        double targetX = grid.millisToX(5000L);
        javafx.scene.input.MouseEvent handleDrag = new javafx.scene.input.MouseEvent(
                javafx.scene.input.MouseEvent.MOUSE_DRAGGED,
                targetX, 10.0, targetX, 10.0,
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

        // Drag an Sekunde 5 (5000L)
        double targetX = grid.millisToX(5000L);
        MouseEvent handleDrag = new MouseEvent(
                MouseEvent.MOUSE_DRAGGED,
                targetX, 10.0, targetX, 10.0,
                MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, false, false, false, null
        );
        grid.fireEvent(handleDrag);
        assertThat(service.getCurrentPositionMs()).isEqualTo(5000L);
        assertThat(service.isPlaying()).isFalse();

        // Loslassen des Griffs -> Wiedergabe setzt an Zielposition nahtlos fort
        MouseEvent handleRelease = new MouseEvent(
                MouseEvent.MOUSE_RELEASED,
                targetX, 10.0, targetX, 10.0,
                MouseButton.PRIMARY, 1,
                false, false, false, false, false, false, false, false, false, false, null
        );
        grid.fireEvent(handleRelease);

        assertThat(service.getCurrentPositionMs()).isEqualTo(5000L);
        assertThat(service.isPlaying()).isTrue();
    }

    @Test
    @DisplayName("Sollte dynamischen horizontalen Zoom (In, Out, Reset) unterstützen")
    void shouldSupportDynamicHorizontalZoom() {
        TimelineService service = new TimelineService();
        TimelineGridView grid = new TimelineGridView(service);

        // Standard 60.0 px/s
        assertThat(grid.getPixelsPerSecond()).isEqualTo(TimelineGridView.DEFAULT_PIXELS_PER_SECOND);
        assertThat(grid.getPixelsPerSecond()).isEqualTo(60.0);

        // Zoom In (+20%: 60.0 * 1.2 = 72.0)
        grid.zoomIn();
        assertThat(grid.getPixelsPerSecond()).isGreaterThan(60.0);
        assertThat(grid.getPixelsPerSecond()).isEqualTo(72.0);

        // Zoom Out (72.0 / 1.2 = 60.0, stellt Ausgangszustand wieder her)
        grid.zoomOut();
        assertThat(grid.getPixelsPerSecond()).isEqualTo(60.0);

        // Weiteres Zoom Out (60.0 / 1.2 = 50.0)
        grid.zoomOut();
        assertThat(grid.getPixelsPerSecond()).isEqualTo(50.0);

        // Reset auf 100% (60.0)
        grid.resetZoom();
        assertThat(grid.getPixelsPerSecond()).isEqualTo(60.0);
    }

    @Test
    @DisplayName("Sollte Zoom-Werte strikt zwischen MIN und MAX begrenzen")
    void shouldClampZoomBetweenMinAndMax() {
        TimelineService service = new TimelineService();
        TimelineGridView grid = new TimelineGridView(service);

        // Exzessiver Zoom-In -> Deckelung bei MAX_PIXELS_PER_SECOND (160.0)
        for (int i = 0; i < 30; i++) {
            grid.zoomIn();
        }
        assertThat(grid.getPixelsPerSecond()).isEqualTo(TimelineGridView.MAX_PIXELS_PER_SECOND);
        assertThat(grid.getPixelsPerSecond()).isEqualTo(160.0);

        // Exzessiver Zoom-Out -> Deckelung bei MIN_PIXELS_PER_SECOND (24.0)
        for (int i = 0; i < 30; i++) {
            grid.zoomOut();
        }
        assertThat(grid.getPixelsPerSecond()).isEqualTo(TimelineGridView.MIN_PIXELS_PER_SECOND);
        assertThat(grid.getPixelsPerSecond()).isEqualTo(24.0);

        // Manuelles Setzen außerhalb der Grenzen
        grid.setPixelsPerSecond(500.0);
        assertThat(grid.getPixelsPerSecond()).isEqualTo(160.0);

        grid.setPixelsPerSecond(5.0);
        assertThat(grid.getPixelsPerSecond()).isEqualTo(24.0);
    }

    @Test
    @DisplayName("Sollte Koordinatenberechnungen nach Zoom-Änderung dynamisch anpassen")
    void shouldRecalculateCoordinatesDynamicallyAfterZoom() {
        TimelineService service = new TimelineService();
        TimelineGridView grid = new TimelineGridView(service);

        // Standard: 60 px/s -> 2000ms = 120px
        assertThat(grid.millisToX(2000L)).isEqualTo(120.0);
        assertThat(grid.xToMillis(120.0)).isEqualTo(2000L);

        // Zoom auf 120 px/s (doppelt so breit)
        grid.setPixelsPerSecond(120.0);
        assertThat(grid.millisToX(2000L)).isEqualTo(240.0);
        assertThat(grid.xToMillis(240.0)).isEqualTo(2000L);

        // Zoom auf 30 px/s (halb so breit)
        grid.setPixelsPerSecond(30.0);
        assertThat(grid.millisToX(2000L)).isEqualTo(60.0);
        assertThat(grid.xToMillis(60.0)).isEqualTo(2000L);
    }
}

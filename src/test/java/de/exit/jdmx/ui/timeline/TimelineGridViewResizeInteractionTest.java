package de.exit.jdmx.ui.timeline;

import de.exit.jdmx.core.model.TimelineTrack;
import de.exit.jdmx.service.TimelineService;
import javafx.application.Platform;
import javafx.scene.Cursor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für Kanten-Erkennung und Cursor-Feedback beim beidseitigen Clip-Resizing in {@link TimelineGridView}.
 */
class TimelineGridViewResizeInteractionTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit aktiv
        }
    }

    @Test
    @DisplayName("Sollte RESIZE_LEFT an linker Kante und RESIZE_RIGHT an rechter Kante erkennen")
    void shouldDetectResizeEdges() {
        TimelineService service = new TimelineService();
        TimelineGridView grid = new TimelineGridView(service);

        // Track mit Szene: 2000ms bis 4000ms (2s Dauer = 2.0 * pps Pixel breit)
        TimelineTrack track = service.addSceneTrack("scene-1", "Test Look", 2000L, 2000L);
        double pps = grid.getPixelsPerSecond();
        double startX = 2.0 * pps;
        double endX = 4.0 * pps;

        // Vor dem Clip
        assertThat(grid.getClipDragModeAt(track, startX - 10.0)).isEqualTo(TimelineGridView.ClipDragMode.NONE);

        // Direkt an der linken Kante (startX + 2px)
        assertThat(grid.getClipDragModeAt(track, startX + 2.0)).isEqualTo(TimelineGridView.ClipDragMode.RESIZE_LEFT);

        // Mitten im Clip
        double midX = (startX + endX) / 2.0;
        assertThat(grid.getClipDragModeAt(track, midX)).isEqualTo(TimelineGridView.ClipDragMode.MOVE);

        // Direkt an der rechten Kante (endX - 2px)
        assertThat(grid.getClipDragModeAt(track, endX - 2.0)).isEqualTo(TimelineGridView.ClipDragMode.RESIZE_RIGHT);

        // Hinter dem Clip
        assertThat(grid.getClipDragModeAt(track, endX + 10.0)).isEqualTo(TimelineGridView.ClipDragMode.NONE);
    }

    @Test
    @DisplayName("Sollte Cursor an den Kanten auf H_RESIZE setzen")
    void shouldSetHResizeCursorAtEdges() {
        TimelineService service = new TimelineService();
        TimelineGridView grid = new TimelineGridView(service);

        // Track 0 hat keine Szene, Track 1 anlegen
        TimelineTrack track = service.addSceneTrack("scene-1", "Intro Look", 1000L, 4000L);
        double track0H = service.getTracks().get(0).getHeight();
        double track1Y = track0H + 15.0; // Innerhalb von Track 1

        double pps = grid.getPixelsPerSecond();
        double startX = 1.0 * pps;
        double endX = 5.0 * pps;

        // Linke Kante
        grid.updateCursorForPosition(startX + 2.0, track1Y);
        assertThat(grid.getCursor()).isEqualTo(Cursor.H_RESIZE);

        // Mitte (Move)
        grid.updateCursorForPosition((startX + endX) / 2.0, track1Y);
        assertThat(grid.getCursor()).isEqualTo(Cursor.HAND);

        // Rechte Kante
        grid.updateCursorForPosition(endX - 2.0, track1Y);
        assertThat(grid.getCursor()).isEqualTo(Cursor.H_RESIZE);
    }

    @Test
    @DisplayName("Sollte Verschieben stufenlos (free) und Resizen gerastert (nach Grid-Snap) durchführen")
    void shouldMoveFreelyAndResizeWithGridSnap() {
        TimelineService service = new TimelineService();
        service.setGridSnap(de.exit.jdmx.core.model.TimelineGridSnap.SEC_5);
        TimelineGridView grid = new TimelineGridView(service);

        TimelineTrack track = service.addSceneTrack("scene-1", "Test", 0L, 5000L);
        double pps = grid.getPixelsPerSecond();
        double trackY = service.getTracks().get(0).getHeight() + 15.0; // Y innerhalb von Track 2

        // 1. Klick in die Mitte des Clips (Move)
        double midX = (0.0 + 5.0 * pps) / 2.0;
        grid.fireEvent(new javafx.scene.input.MouseEvent(
                javafx.scene.input.MouseEvent.MOUSE_PRESSED,
                midX, trackY, midX, trackY,
                javafx.scene.input.MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, false, false, false, null
        ));

        // Drag um 50px (~833 ms) -> Bei stufenlosem Verschieben muss der Wert direkt übernommen werden
        // und darf NICHT auf 0L abgerundet/eingerastet werden!
        double dragX = midX + 50.0;
        grid.fireEvent(new javafx.scene.input.MouseEvent(
                javafx.scene.input.MouseEvent.MOUSE_DRAGGED,
                dragX, trackY, dragX, trackY,
                javafx.scene.input.MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, false, false, false, null
        ));
        assertThat(track.getStartTimeMs()).isGreaterThan(700L).isLessThan(1000L);

        grid.fireEvent(new javafx.scene.input.MouseEvent(
                javafx.scene.input.MouseEvent.MOUSE_RELEASED,
                dragX, trackY, dragX, trackY,
                javafx.scene.input.MouseButton.PRIMARY, 1,
                false, false, false, false, false, false, false, false, false, false, null
        ));

        // 2. Klick auf die rechte Kante (Resize Right)
        track.setStartTimeMs(0L);
        track.setDurationMs(5000L);
        double edgeX = 5.0 * pps - 2.0;
        grid.fireEvent(new javafx.scene.input.MouseEvent(
                javafx.scene.input.MouseEvent.MOUSE_PRESSED,
                edgeX, trackY, edgeX, trackY,
                javafx.scene.input.MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, false, false, false, null
        ));

        // Drag um 60px (+1000ms bei pps=60: 5000ms + 1000ms = 6000ms -> Snapped auf SEC_5 = 5000ms)
        // Drag um 200px (+3333ms: 5000ms + 3333ms = 8333ms -> Snapped auf SEC_5 = 10000ms)
        double resizeDragX = edgeX + 200.0;
        grid.fireEvent(new javafx.scene.input.MouseEvent(
                javafx.scene.input.MouseEvent.MOUSE_DRAGGED,
                resizeDragX, trackY, resizeDragX, trackY,
                javafx.scene.input.MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, false, false, false, null
        ));
        assertThat(track.getDurationMs()).isEqualTo(10000L);
    }
}

package de.exit.jdmx.ui.timeline;

import de.exit.jdmx.core.model.TimelineTrack;
import de.exit.jdmx.service.TimelineService;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für Szenen-Clips in {@link TimelineGridView}.
 */
class TimelineGridViewSceneTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit aktiv
        }
    }

    @Test
    @DisplayName("Sollte Szenen-Clips an horizontalen Koordinaten korrekt erkennen")
    void shouldFindSceneClipAtCoordinates() {
        TimelineService service = new TimelineService();
        TimelineGridView grid = new TimelineGridView(service);

        // Track 0 hat keine Szene
        // Track 1 anlegen: Szene von 2000ms bis 4000ms (2s Clip)
        TimelineTrack sceneTrack = service.addSceneTrack("scene-1", "Intro Strobe", 2000L, 2000L);

        double track0H = service.getTracks().get(0).getHeight();
        double track1Y = track0H + 10.0; // Innerhalb von Track 1

        double pps = grid.getPixelsPerSecond();
        double clipStartX = 2.0 * pps;
        double clipMidX = 3.0 * pps;
        double clipEndX = 4.0 * pps;

        // Vor dem Clip
        assertThat(grid.findClipTrackAt(clipStartX - 10.0, track1Y)).isNull();

        // Mitten im Clip
        assertThat(grid.findClipTrackAt(clipMidX, track1Y)).isEqualTo(sceneTrack);

        // Nach dem Clip
        assertThat(grid.findClipTrackAt(clipEndX + 10.0, track1Y)).isNull();

        // Falsche vertikale Koordinate (auf Track 0)
        assertThat(grid.findClipTrackAt(clipMidX, 10.0)).isNull();
    }

    @Test
    @DisplayName("Sollte Szenen-Clips rendern und Dimensionen anpassen")
    void shouldUpdateDimensionsAndRenderSceneClip() {
        TimelineService service = new TimelineService();
        TimelineGridView grid = new TimelineGridView(service);

        // Vorherige Breite
        double initialW = grid.getContentWidth();

        service.addSceneTrack("scene-long", "Show Look", 100000L, 2000L);

        // Durch notifyChange() im TimelineService wurden die Dimensionen bereits synchron aktualisiert
        assertThat(grid.getContentWidth()).isGreaterThan(initialW);
        assertThat(grid.getContentWidth()).isGreaterThanOrEqualTo(grid.millisToX(102000L));

        // Redraw und Cache-Leerung ohne Ausnahmen
        grid.redraw();
        grid.clearWaveformCache();
    }
}

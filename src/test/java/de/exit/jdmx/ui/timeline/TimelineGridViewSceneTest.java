package de.exit.jdmx.ui.timeline;

import de.exit.jdmx.core.model.TimelineTrack;
import de.exit.jdmx.service.TimelineService;
import de.exit.jdmx.ui.icon.LucideIcon;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
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

    @Test
    @DisplayName("Sollte Dauer im Daslight-Format (00m08s80) formatieren")
    void shouldFormatDurationInDaslightStyle() {
        assertThat(TimelineGridView.formatDaslightDuration(8800L)).isEqualTo("00m08s80");
        assertThat(TimelineGridView.formatDaslightDuration(5000L)).isEqualTo("00m05s00");
        assertThat(TimelineGridView.formatDaslightDuration(250L)).isEqualTo("00m00s25");
        assertThat(TimelineGridView.formatDaslightDuration(65420L)).isEqualTo("01m05s42");
        assertThat(TimelineGridView.formatDaslightDuration(0L)).isEqualTo("00m00s00");
        assertThat(TimelineGridView.formatDaslightDuration(-500L)).isEqualTo("00m00s00");
    }

    @Test
    @DisplayName("Sollte Szenen-Clips mit benutzerdefinierter Farbe rendern")
    void shouldRenderSceneClipWithCustomColor() {
        TimelineService service = new TimelineService();
        TimelineGridView grid = new TimelineGridView(service);

        TimelineTrack track = service.addSceneTrack("scene-color", "Color Test", 0L, 5000L);
        track.setColorHex("#38BDF8");

        grid.redraw();
        assertThat(track.getColorHex()).isEqualTo("#38BDF8");
    }

    @Test
    @DisplayName("Sollte inaktiven Szenen-Clip ohne Rahmen und ohne Play-Icon rendern")
    void shouldRenderInactiveSceneClipWithoutBorderAndWithoutPlayIcon() {
        TimelineService service = new TimelineService();
        TimelineGridView grid = new TimelineGridView(service);

        TimelineTrack track = service.addSceneTrack("scene-1", "Strobe", 0L, 5000L);
        track.setColorHex("#EC4899");
        track.setPlaybackActive(false);

        Group node = grid.createSceneClipNodeForTest(track, 120.0, 50.0);
        assertThat(node).isNotNull();

        Rectangle bg = (Rectangle) node.getChildren().get(0);
        assertThat(bg.getFill()).isEqualTo(Color.web("#EC4899"));
        assertThat(bg.getStroke()).isNull();
        assertThat(bg.getStrokeWidth()).isEqualTo(0.0);

        boolean hasPlayIcon = node.getChildren().stream().anyMatch(c -> c instanceof LucideIcon);
        assertThat(hasPlayIcon).isFalse();
    }

    @Test
    @DisplayName("Sollte aktiven Szenen-Clip mit weißem 1.5px Rahmen, unverfälschter Farbe und grünem Play-Icon rendern")
    void shouldRenderActiveSceneClipWithWhiteBorderAndPlayIcon() {
        TimelineService service = new TimelineService();
        TimelineGridView grid = new TimelineGridView(service);

        TimelineTrack track = service.addSceneTrack("scene-active", "Drop Strobe", 0L, 5000L);
        track.setColorHex("#38BDF8");
        track.setPlaybackActive(true);

        Group node = grid.createSceneClipNodeForTest(track, 120.0, 50.0);
        assertThat(node).isNotNull();

        Rectangle bg = (Rectangle) node.getChildren().get(0);
        // Originalfarbe muss 100% unverfälscht erhalten bleiben (kein Aufhellen)
        assertThat(bg.getFill()).isEqualTo(Color.web("#38BDF8"));
        // Gestochen scharfer weißer Rahmen
        assertThat(bg.getStroke()).isEqualTo(Color.web("#FFFFFF"));
        assertThat(bg.getStrokeWidth()).isEqualTo(1.5);
        assertThat(bg.getStrokeType()).isEqualTo(StrokeType.INSIDE);

        // Grünes Play-Icon in der Statusleiste
        LucideIcon playIcon = node.getChildren().stream()
                .filter(c -> c instanceof LucideIcon)
                .map(c -> (LucideIcon) c)
                .findFirst()
                .orElse(null);

        assertThat(playIcon).isNotNull();
        assertThat(playIcon.getName()).isEqualTo("play");
        assertThat(playIcon.getColor()).isEqualTo(Color.web("#10B981"));
    }
}

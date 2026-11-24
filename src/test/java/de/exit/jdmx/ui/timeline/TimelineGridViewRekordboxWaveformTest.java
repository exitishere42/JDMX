package de.exit.jdmx.ui.timeline;

import de.exit.jdmx.core.model.TimelineTrack;
import de.exit.jdmx.service.AudioAnalysisService;
import de.exit.jdmx.service.TimelineService;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für die Pioneer Rekordbox 3-Band Solid Waveform Engine in {@link TimelineGridView}.
 */
class TimelineGridViewRekordboxWaveformTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit bereits aktiv
        }
    }

    @Test
    @DisplayName("Sollte authentische Pioneer Rekordbox Farbkonstanten (Low=Blau, Mid=Orange, High=Weiß) bereitstellen")
    void shouldProvideAuthenticRekordboxColorConstants() {
        assertThat(TimelineGridView.COLOR_REKORDBOX_LOW).isEqualTo(Color.web("#2253D9"));
        assertThat(TimelineGridView.COLOR_REKORDBOX_MID).isEqualTo(Color.web("#D97706"));
        assertThat(TimelineGridView.COLOR_REKORDBOX_HIGH).isEqualTo(Color.web("#FFFFFF"));
    }

    @Test
    @DisplayName("Sollte 4000 Analyse-Punkte als Standard in AudioAnalysisService für ultrafeine Kurven definieren")
    void shouldDefine4000PointsAsDefaultAnalysisResolution() {
        assertThat(AudioAnalysisService.DEFAULT_WAVEFORM_POINTS).isEqualTo(4000);
    }

    @Test
    @DisplayName("Sollte Solid-Waveform für Tracks mit 3-Band-Daten ohne Fehler rendern")
    void shouldRender3BandSolidWaveformWithoutErrors() {
        TimelineService service = new TimelineService();
        TimelineTrack track = service.getTracks().get(0);
        track.setSongName("festival_drop.mp3");
        track.setDurationMs(180_000L);
        track.setStartTimeMs(0L);

        float[] low = new float[4000];
        float[] mid = new float[4000];
        float[] high = new float[4000];
        for (int i = 0; i < 4000; i++) {
            low[i] = (float) Math.sin(i * 0.05) * 0.8f;
            mid[i] = (float) Math.cos(i * 0.08) * 0.5f;
            high[i] = (i % 20 == 0) ? 0.9f : 0.1f; // Scharfe Transienten
        }
        track.set3BandWaveformData(low, mid, high);

        TimelineGridView grid = new TimelineGridView(service);
        grid.resize(1920, 800);

        // Render-Durchlauf aufrufen
        grid.redraw();

        assertThat(track.has3BandWaveform()).isTrue();
        assertThat(track.getLowBandData()).hasSize(4000);
        assertThat(track.getMidBandData()).hasSize(4000);
        assertThat(track.getHighBandData()).hasSize(4000);
    }

    @Test
    @DisplayName("Sollte Solid-Waveform auch bei extremen Zoom-Stufen und leeren Daten robust zeichnen")
    void shouldHandleExtremeZoomsAndEmptyDataRobustly() {
        TimelineService service = new TimelineService();
        TimelineTrack track = service.getTracks().get(0);
        track.setSongName("empty.mp3");
        track.setDurationMs(10_000L);
        track.set3BandWaveformData(new float[0], new float[0], new float[0]);

        TimelineGridView grid = new TimelineGridView(service);
        grid.resize(800, 400);

        // Minimaler Zoom
        grid.setPixelsPerSecond(TimelineGridView.MIN_PIXELS_PER_SECOND);
        grid.redraw();

        // Maximaler Zoom
        grid.setPixelsPerSecond(TimelineGridView.MAX_PIXELS_PER_SECOND);
        grid.redraw();

        assertThat(grid.getPixelsPerSecond()).isEqualTo(TimelineGridView.MAX_PIXELS_PER_SECOND);
    }
}

package de.exit.jdmx.service;

import de.exit.jdmx.core.model.TimelineTrack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für das beidseitige Resizen (Verlängern & Verkürzen) von Clips im {@link TimelineService}.
 */
class TimelineServiceClipResizeTest {

    private TimelineService timelineService;

    @BeforeEach
    void setUp() {
        timelineService = new TimelineService();
    }

    @AfterEach
    void tearDown() {
        if (timelineService != null) {
            timelineService.stopPlayback();
        }
    }

    @Test
    @DisplayName("Sollte Clip an der rechten Kante verlängern können (Startzeit bleibt fest)")
    void shouldResizeTrackRightExtendingDuration() {
        TimelineTrack track = timelineService.addSceneTrack("scene-1", "Intro Look", 1000L, 2000L);
        AtomicBoolean changeNotified = new AtomicBoolean(false);
        timelineService.addChangeListener(() -> changeNotified.set(true));

        boolean result = timelineService.resizeTrackRight(track.getId(), 5000L);

        assertThat(result).isTrue();
        assertThat(changeNotified).isTrue();
        assertThat(track.getStartTimeMs()).isEqualTo(1000L);
        assertThat(track.getDurationMs()).isEqualTo(4000L); // 5000 - 1000
    }

    @Test
    @DisplayName("Sollte Clip an der rechten Kante verkürzen können")
    void shouldResizeTrackRightShorteningDuration() {
        TimelineTrack track = timelineService.addSceneTrack("scene-1", "Intro Look", 1000L, 4000L);

        boolean result = timelineService.resizeTrackRight(track.getId(), 2500L);

        assertThat(result).isTrue();
        assertThat(track.getStartTimeMs()).isEqualTo(1000L);
        assertThat(track.getDurationMs()).isEqualTo(1500L); // 2500 - 1000
    }

    @Test
    @DisplayName("Sollte rechte Kante auf Mindestdauer von 250ms begrenzen")
    void shouldClampResizeTrackRightToMinimumDuration() {
        TimelineTrack track = timelineService.addSceneTrack("scene-1", "Intro Look", 1000L, 2000L);

        // Versuche Endzeit auf 1100ms zu ziehen (wäre nur 100ms Dauer)
        boolean result = timelineService.resizeTrackRight(track.getId(), 1100L);

        assertThat(result).isTrue();
        assertThat(track.getStartTimeMs()).isEqualTo(1000L);
        assertThat(track.getDurationMs()).isEqualTo(TimelineService.MIN_CLIP_DURATION_MS);
    }

    @Test
    @DisplayName("Sollte Clip an der linken Kante nach links verlängern können (Endzeit bleibt fest)")
    void shouldResizeTrackLeftExtendingDuration() {
        TimelineTrack track = timelineService.addSceneTrack("scene-1", "Intro Look", 2000L, 2000L);
        // Ursprüngliche Endzeit: 4000L
        AtomicBoolean changeNotified = new AtomicBoolean(false);
        timelineService.addChangeListener(() -> changeNotified.set(true));

        boolean result = timelineService.resizeTrackLeft(track.getId(), 1000L);

        assertThat(result).isTrue();
        assertThat(changeNotified).isTrue();
        assertThat(track.getStartTimeMs()).isEqualTo(1000L);
        assertThat(track.getDurationMs()).isEqualTo(3000L); // Endzeit bleibt 4000L (4000 - 1000)
    }

    @Test
    @DisplayName("Sollte Clip an der linken Kante nach rechts verkürzen können (Endzeit bleibt fest)")
    void shouldResizeTrackLeftShorteningDuration() {
        TimelineTrack track = timelineService.addSceneTrack("scene-1", "Intro Look", 1000L, 3000L);
        // Ursprüngliche Endzeit: 4000L

        boolean result = timelineService.resizeTrackLeft(track.getId(), 2500L);

        assertThat(result).isTrue();
        assertThat(track.getStartTimeMs()).isEqualTo(2500L);
        assertThat(track.getDurationMs()).isEqualTo(1500L); // Endzeit bleibt 4000L (4000 - 2500)
    }

    @Test
    @DisplayName("Sollte linke Kante bei negativen Werten auf 0ms begrenzen (Endzeit bleibt fest)")
    void shouldClampResizeTrackLeftToZeroOnNegative() {
        TimelineTrack track = timelineService.addSceneTrack("scene-1", "Intro Look", 1000L, 2000L);
        // Ursprüngliche Endzeit: 3000L

        boolean result = timelineService.resizeTrackLeft(track.getId(), -500L);

        assertThat(result).isTrue();
        assertThat(track.getStartTimeMs()).isEqualTo(0L);
        assertThat(track.getDurationMs()).isEqualTo(3000L); // 3000 - 0
    }

    @Test
    @DisplayName("Sollte linke Kante auf Mindestdauer von 250ms vor der Endzeit begrenzen")
    void shouldClampResizeTrackLeftToMinimumDuration() {
        TimelineTrack track = timelineService.addSceneTrack("scene-1", "Intro Look", 1000L, 2000L);
        // Ursprüngliche Endzeit: 3000L

        // Versuche Startzeit auf 2950ms zu setzen (wäre nur 50ms vor Ende)
        boolean result = timelineService.resizeTrackLeft(track.getId(), 2950L);

        assertThat(result).isTrue();
        assertThat(track.getStartTimeMs()).isEqualTo(3000L - TimelineService.MIN_CLIP_DURATION_MS); // 2750L
        assertThat(track.getDurationMs()).isEqualTo(TimelineService.MIN_CLIP_DURATION_MS); // 250L
    }

    @Test
    @DisplayName("Sollte Resizing während aktiver Wiedergabe abweisen")
    void shouldBlockResizeDuringActivePlayback() {
        TimelineTrack track = timelineService.addSceneTrack("scene-1", "Intro Look", 1000L, 2000L);
        timelineService.startPlayback();

        boolean leftRes = timelineService.resizeTrackLeft(track.getId(), 500L);
        boolean rightRes = timelineService.resizeTrackRight(track.getId(), 4000L);

        assertThat(leftRes).isFalse();
        assertThat(rightRes).isFalse();
        assertThat(track.getStartTimeMs()).isEqualTo(1000L);
        assertThat(track.getDurationMs()).isEqualTo(2000L);
    }
}

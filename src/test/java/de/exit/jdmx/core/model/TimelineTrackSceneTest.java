package de.exit.jdmx.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit-Tests für Szenen-Eigenschaften in {@link TimelineTrack}.
 */
class TimelineTrackSceneTest {

    @Test
    @DisplayName("Sollte initial keine Szene zugewiesen haben")
    void shouldNotHaveSceneInitially() {
        TimelineTrack track = new TimelineTrack("track-1", "Track 1");

        assertThat(track.hasScene()).isFalse();
        assertThat(track.getSceneId()).isNull();
        assertThat(track.getSceneName()).isNull();
        assertThat(track.isPlaybackActive()).isFalse();
    }

    @Test
    @DisplayName("Sollte Standardwerte für Szenen-Dauer (5s) und Szenen-Spurhöhe (50px) definieren")
    void shouldDefineDefaultSceneDurationAndHeight() {
        assertThat(TimelineTrack.DEFAULT_SCENE_DURATION_MS).isEqualTo(5000L);
        assertThat(TimelineTrack.DEFAULT_SCENE_TRACK_HEIGHT).isEqualTo(50.0);
    }

    @Test
    @DisplayName("Sollte Szene mit Standarddauer von 5 Sekunden zuweisen können")
    void shouldAssignSceneWithDefaultDuration() {
        TimelineTrack track = new TimelineTrack("track-1", "Track 1", false, TimelineTrack.DEFAULT_SCENE_TRACK_HEIGHT);
        track.setScene("scene-123", "Intro Look", TimelineTrack.DEFAULT_SCENE_DURATION_MS);

        assertThat(track.hasScene()).isTrue();
        assertThat(track.getHeight()).isEqualTo(50.0);
        assertThat(track.getSceneId()).isEqualTo("scene-123");
        assertThat(track.getSceneName()).isEqualTo("Intro Look");
        assertThat(track.getDurationMs()).isEqualTo(5000L);
    }

    @Test
    @DisplayName("Sollte Szene wieder entfernen können")
    void shouldClearScene() {
        TimelineTrack track = new TimelineTrack("track-1", "Track 1");
        track.setScene("scene-123", "Intro Look", 2000L);
        assertThat(track.hasScene()).isTrue();

        track.clearScene();
        assertThat(track.hasScene()).isFalse();
        assertThat(track.getSceneId()).isNull();
        assertThat(track.getSceneName()).isNull();
    }

    @Test
    @DisplayName("Sollte PlaybackActive Status setzen und lesen können")
    void shouldTogglePlaybackActive() {
        TimelineTrack track = new TimelineTrack("track-1", "Track 1");
        assertThat(track.isPlaybackActive()).isFalse();

        track.setPlaybackActive(true);
        assertThat(track.isPlaybackActive()).isTrue();

        track.setPlaybackActive(false);
        assertThat(track.isPlaybackActive()).isFalse();
    }
}

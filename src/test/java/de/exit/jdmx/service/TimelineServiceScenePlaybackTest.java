package de.exit.jdmx.service;

import de.exit.jdmx.core.model.SceneDefinition;
import de.exit.jdmx.core.model.TimelineTrack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für Szenen-Spuren und deren Playhead-Aktivierung / Deaktivierung im {@link TimelineService}.
 */
class TimelineServiceScenePlaybackTest {

    private DmxService dmxService;
    private SceneService sceneService;
    private TimelineService timelineService;

    @BeforeEach
    void setUp() {
        dmxService = new DmxService();
        sceneService = new SceneService(dmxService);
        timelineService = new TimelineService();
        timelineService.setSceneService(sceneService);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (timelineService != null) {
            timelineService.stopPlayback();
        }
        if (sceneService != null) {
            sceneService.close();
        }
        if (dmxService != null) {
            dmxService.close();
        }
    }

    @Test
    @DisplayName("Sollte neuen Szenen-Track mit 5s Standarddauer und 50px Höhe anlegen")
    void shouldAddSceneTrackWithDefaultDuration() {
        TimelineTrack track = timelineService.addSceneTrack("scene-1", "Intro Strobe", 1000L, 5000L);

        assertThat(track).isNotNull();
        assertThat(track.getName()).isEqualTo("Intro Strobe");
        assertThat(track.hasScene()).isTrue();
        assertThat(track.getHeight()).isEqualTo(50.0);
        assertThat(track.getSceneId()).isEqualTo("scene-1");
        assertThat(track.getSceneName()).isEqualTo("Intro Strobe");
        assertThat(track.getStartTimeMs()).isEqualTo(1000L);
        assertThat(track.getDurationMs()).isEqualTo(5000L);
        assertThat(timelineService.getTracks()).contains(track);
    }

    @Test
    @DisplayName("Sollte maximale Timeline-Dauer anhand von Szenen-Clips berechnen")
    void shouldCalculateMaxTimelineDurationWithScenes() {
        // Initiale Tracks haben 0ms
        timelineService.addSceneTrack("scene-1", "Look 1", 5000L, 2000L); // Ende: 7000ms

        assertThat(timelineService.getMaxTimelineDurationMs()).isEqualTo(7000L);

        timelineService.addSceneTrack("scene-2", "Look 2", 10000L, 2000L); // Ende: 12000ms
        assertThat(timelineService.getMaxTimelineDurationMs()).isEqualTo(12000L);
    }

    @Test
    @DisplayName("Sollte Szene aktivieren wenn Playhead den Block betritt und deaktivieren wenn er ihn verlässt")
    void shouldActivateSceneOnEnterAndDeactivateOnExit() {
        Map<Integer, Map<Integer, Integer>> channels = Map.of(
                0, Map.of(1, 255)
        );
        SceneDefinition scene = new SceneDefinition("scene-flash", "Flash", 0, channels);
        sceneService.registerScene(scene);

        // Szene von 2000ms bis 4000ms (2s Clip)
        TimelineTrack track = timelineService.addSceneTrack("scene-flash", "Flash", 2000L, 2000L);

        // 1. Vor dem Clip (1000 ms): Szene muss inaktiv sein
        timelineService.evaluateScenePlayback(1000L);
        assertThat(track.isPlaybackActive()).isFalse();
        assertThat(dmxService.getChannel(0, 1)).isEqualTo(0);

        // 2. Playhead betritt den Clip (2000 ms): Szene muss aktiv werden!
        timelineService.evaluateScenePlayback(2000L);
        assertThat(track.isPlaybackActive()).isTrue();
        assertThat(dmxService.getChannel(0, 1)).isEqualTo(255);

        // 3. Playhead innerhalb des Clips (3000 ms): Szene bleibt aktiv
        timelineService.evaluateScenePlayback(3000L);
        assertThat(track.isPlaybackActive()).isTrue();
        assertThat(dmxService.getChannel(0, 1)).isEqualTo(255);

        // 4. Playhead verlässt den Clip (4000 ms): Szene muss deaktiviert werden (Kanal -> 0)
        timelineService.evaluateScenePlayback(4000L);
        assertThat(track.isPlaybackActive()).isFalse();
        assertThat(dmxService.getChannel(0, 1)).isEqualTo(0);
    }

    @Test
    @DisplayName("Sollte aktive Szenen bei stopPlayback sofort sauber deaktivieren")
    void shouldDeactivateActiveScenesOnStopPlayback() {
        Map<Integer, Map<Integer, Integer>> channels = Map.of(
                0, Map.of(2, 200)
        );
        SceneDefinition scene = new SceneDefinition("scene-stop", "Stop Test", 0, channels);
        sceneService.registerScene(scene);

        TimelineTrack track = timelineService.addSceneTrack("scene-stop", "Stop Test", 0L, 2000L);

        // Bei 500ms aktivieren
        timelineService.evaluateScenePlayback(500L);
        assertThat(track.isPlaybackActive()).isTrue();
        assertThat(dmxService.getChannel(0, 2)).isEqualTo(200);

        // Stop auslösen
        timelineService.stopPlayback();
        assertThat(track.isPlaybackActive()).isFalse();
        assertThat(dmxService.getChannel(0, 2)).isEqualTo(0);
    }

    @Test
    @DisplayName("Sollte Szene auf gemutetem Track nicht aktivieren")
    void shouldNotActivateSceneWhenTrackIsMuted() {
        Map<Integer, Map<Integer, Integer>> channels = Map.of(
                0, Map.of(3, 255)
        );
        SceneDefinition scene = new SceneDefinition("scene-mute", "Mute Test", 0, channels);
        sceneService.registerScene(scene);

        TimelineTrack track = timelineService.addSceneTrack("scene-mute", "Mute Test", 1000L, 2000L);
        timelineService.setTrackMuted(track.getId(), true);

        // Bei 1500ms: da gemutet, darf Szene nicht aktiv werden
        timelineService.evaluateScenePlayback(1500L);
        assertThat(track.isPlaybackActive()).isFalse();
        assertThat(dmxService.getChannel(0, 3)).isEqualTo(0);
    }
}

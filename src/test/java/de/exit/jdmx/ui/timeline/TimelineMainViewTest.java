package de.exit.jdmx.ui.timeline;

import de.exit.jdmx.service.TimelineService;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für {@link TimelineMainView}.
 */
class TimelineMainViewTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit aktiv
        }
    }

    @Test
    @DisplayName("Sollte Standard-Trackbox und Plus-Button anzeigen")
    void shouldDisplayDefaultTrackBoxAndAddButton() {
        TimelineService service = new TimelineService();
        TimelineMainView view = new TimelineMainView(service);

        assertThat(view.getTrackBoxes()).hasSize(1);
        assertThat(view.getTrackBoxes().get(0).getTrack().getName()).isEqualTo("Track 1");
        assertThat(view.getBtnAddTrack()).isNotNull();
        assertThat(view.getBtnAddTrack().getStyleClass()).contains("timeline-track-add-btn");
    }

    @Test
    @DisplayName("Sollte neuen Track beim Klick auf Plus-Button hinzufügen")
    void shouldAddNewTrackWhenAddButtonClicked() {
        TimelineService service = new TimelineService();
        TimelineMainView view = new TimelineMainView(service);

        assertThat(view.getTrackBoxes()).hasSize(1);

        view.getBtnAddTrack().fire();

        assertThat(view.getTrackBoxes()).hasSize(2);
        assertThat(view.getTrackBoxes().get(1).getTrack().getName()).isEqualTo("Track 2");
    }

    @Test
    @DisplayName("Sollte Timeline-Grid bereitstellen")
    void shouldProvideTimelineGrid() {
        TimelineService service = new TimelineService();
        TimelineMainView view = new TimelineMainView(service);

        assertThat(view.getTimelineGridView()).isNotNull();
    }

    @Test
    @DisplayName("Sollte Play- und Stop-Buttons mit dem TimelineService synchronisieren")
    void shouldSynchronizePlayAndStopWithService() {
        TimelineService service = new TimelineService();
        TimelineMainView view = new TimelineMainView(service);

        assertThat(service.isPlaying()).isFalse();

        // Play drücken
        view.getBtnPlay().fire();
        assertThat(service.isPlaying()).isTrue();

        // Stop drücken
        view.getBtnStop().fire();
        assertThat(service.isPlaying()).isFalse();
        assertThat(service.getCurrentPositionMs()).isEqualTo(0L);
        assertThat(view.getLblTimeDisplay().getText()).isEqualTo("00:00.000");
    }

    @Test
    @DisplayName("Sollte Zeitstempel exakt im Format mm:ss.SSS formatieren")
    void shouldFormatTimeCorrectly() {
        assertThat(TimelineMainView.formatTime(0L)).isEqualTo("00:00.000");
        assertThat(TimelineMainView.formatTime(1500L)).isEqualTo("00:01.500");
        assertThat(TimelineMainView.formatTime(65432L)).isEqualTo("01:05.432");
    }

    @Test
    @DisplayName("Sollte linke Track-Header-Leiste mit stabiler Mindestbreite von mindestens 160px layouten")
    void shouldMaintainStableHeaderWidth() {
        TimelineService service = new TimelineService();
        TimelineMainView view = new TimelineMainView(service);

        assertThat(view.getTracksScrollPane().getMinWidth()).isGreaterThanOrEqualTo(160.0);
        assertThat(view.getTracksScrollPane().getPrefWidth()).isGreaterThanOrEqualTo(160.0);
    }

    @Test
    @DisplayName("Sollte TrackBox und TimeDisplay aktualisieren, wenn Song zugewiesen wird")
    void shouldUpdateTrackBoxAndTimeDisplayWhenSongAssigned() {
        TimelineService service = new TimelineService();
        TimelineMainView view = new TimelineMainView(service);
        java.io.File dummySong = new java.io.File("test.mp3");
        service.assignSongToTrack(service.getTracks().get(0).getId(), dummySong);
        assertThat(view.getTrackBoxes().get(0).getTrack().getName()).isEqualTo("test.mp3");
        assertThat(view.getLblTimeDisplay().getText()).contains(" / ");
    }

    @Test
    @DisplayName("Sollte bei Leertaste Playback starten und stoppen (Toggle)")
    void shouldTogglePlaybackWhenSpacebarPressed() {
        TimelineService service = new TimelineService();
        TimelineMainView view = new TimelineMainView(service);

        assertThat(service.isPlaying()).isFalse();

        // Leertaste drücken -> Start
        javafx.scene.input.KeyEvent spaceEvent1 = new javafx.scene.input.KeyEvent(
                javafx.scene.input.KeyEvent.KEY_PRESSED,
                " ", " ", javafx.scene.input.KeyCode.SPACE, false, false, false, false
        );
        view.fireEvent(spaceEvent1);
        assertThat(service.isPlaying()).isTrue();

        // Erneut Leertaste drücken -> Pause
        javafx.scene.input.KeyEvent spaceEvent2 = new javafx.scene.input.KeyEvent(
                javafx.scene.input.KeyEvent.KEY_PRESSED,
                " ", " ", javafx.scene.input.KeyCode.SPACE, false, false, false, false
        );
        view.fireEvent(spaceEvent2);
        assertThat(service.isPlaying()).isFalse();
    }

    @Test
    @DisplayName("Sollte bei Doppelklick auf Stop-Button auch die Scroll-Sicht an den Anfang zurücksetzen")
    void shouldResetViewToBeginningOnStopDoubleClick() {
        TimelineService service = new TimelineService();
        TimelineMainView view = new TimelineMainView(service);

        // Position simulieren und Viewport verschieben
        view.getTimelineGridView().setViewportSize(800, 450);
        service.setPositionMs(15000L);
        service.startPlayback();
        view.getTimelineGridView().setScrollX(400.0);

        assertThat(service.isPlaying()).isTrue();
        assertThat(service.getCurrentPositionMs()).isEqualTo(15000L);

        // Doppelklick auf Stop simulieren
        javafx.scene.input.MouseEvent doubleClick = new javafx.scene.input.MouseEvent(
                javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                0, 0, 0, 0, javafx.scene.input.MouseButton.PRIMARY, 2,
                false, false, false, false, true, false, false, false, false, false, null
        );
        view.getBtnStop().fireEvent(doubleClick);

        assertThat(service.isPlaying()).isFalse();
        assertThat(service.getCurrentPositionMs()).isEqualTo(0L);
        assertThat(view.getTimelineGridView().getScrollX()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Sollte Zoom-Buttons in der Toolbar bereitstellen und steuern")
    void shouldProvideZoomControlsInToolbar() {
        TimelineService service = new TimelineService();
        TimelineMainView view = new TimelineMainView(service);

        assertThat(view.getTimelineGridView().getPixelsPerSecond()).isEqualTo(TimelineGridView.DEFAULT_PIXELS_PER_SECOND);
        assertThat(view.getTopToolbar().getChildren()).isNotEmpty();

        // Zoom In testen
        view.getTimelineGridView().zoomIn();
        assertThat(view.getTimelineGridView().getPixelsPerSecond()).isEqualTo(72.0);

        // Zoom Reset testen
        view.getTimelineGridView().resetZoom();
        assertThat(view.getTimelineGridView().getPixelsPerSecond()).isEqualTo(60.0);
    }

    @Test
    @DisplayName("Sollte Follow-Playhead Button in der Toolbar bereitstellen (standardmäßig aktiviert)")
    void shouldProvideFollowPlayheadToggleButtonActiveByDefault() {
        TimelineService service = new TimelineService();
        TimelineMainView view = new TimelineMainView(service);

        assertThat(view.getBtnFollowPlayhead()).isNotNull();
        assertThat(view.getBtnFollowPlayhead().isSelected()).isTrue();
        assertThat(view.isFollowPlayheadEnabled()).isTrue();

        view.getBtnFollowPlayhead().fire();
        assertThat(view.getBtnFollowPlayhead().isSelected()).isFalse();
        assertThat(view.isFollowPlayheadEnabled()).isFalse();
    }

    @Test
    @DisplayName("Sollte Viewport beim Abspielen automatisch mit dem Playhead mitscrollen wenn Follow aktiv ist")
    void shouldAutoScrollPlayheadWhenFollowIsEnabledAndPlaying() {
        TimelineService service = new TimelineService();
        service.getTracks().get(0).setDurationMs(120_000L); // 2 Minuten Song -> 7200px breit
        TimelineMainView view = new TimelineMainView(service);

        // Viewport-Größe explizit simulieren (800px Breite)
        view.resize(1000, 500);
        view.layout();
        view.getTimelineGridView().setViewportSize(800, 450);

        // Follow aktiv, Playback starten und Position auf 60.000ms (3600px) setzen
        assertThat(view.isFollowPlayheadEnabled()).isTrue();
        service.startPlayback();
        service.setPositionMs(60_000L);
        view.updateFromService();

        // Viewport muss deutlich nach rechts gescrollt sein
        assertThat(view.getTimelineGridView().getScrollX()).isGreaterThan(1000.0);

        // Follow deaktivieren -> Viewport bleibt bei weiterem Fortschritt stehen
        double savedScrollX = view.getTimelineGridView().getScrollX();
        view.getBtnFollowPlayhead().setSelected(false);
        service.setPositionMs(90_000L);
        view.updateFromService();

        assertThat(view.getTimelineGridView().getScrollX()).isEqualTo(savedScrollX);
        service.stopPlayback();
    }

    @Test
    @DisplayName("Sollte beim Page-Turn Follow ruhig bleiben und erst am rechten Rand (Viewport-Ende) umblättern")
    void shouldPageTurnOnlyWhenPlayheadApproachesRightEdge() {
        TimelineService service = new TimelineService();
        service.getTracks().get(0).setDurationMs(600_000L); // 36.000 px breite Timeline
        TimelineMainView view = new TimelineMainView(service);
        view.resize(1000, 500);
        view.layout();
        view.getTimelineGridView().setViewportSize(800, 450);

        service.startPlayback();

        // 1. Bis kurz vor dem Viewport-Ende (z. B. 12.600ms = 756px < viewW 800px) -> scrollX bleibt 0!
        service.setPositionMs(12_600L);
        view.updatePlaybackFrame(12_600L);
        assertThat(view.getTimelineGridView().getScrollX()).isEqualTo(0.0);

        // 2. Erreicht das Ende des Viewports (13.340ms = 800.4px >= 800px) -> blättert genau einmal um!
        // Ziel: Playhead landet links bei 10% von 800px (= 80px), also scrollX ≈ 800 - 80 = 720px
        service.setPositionMs(13_340L);
        view.updatePlaybackFrame(13_340L);
        assertThat(view.getTimelineGridView().getScrollX()).isBetween(715.0, 725.0);
        assertThat(view.getHorizontalScrollBar().getValue()).isBetween(715.0, 725.0);

        // 3. Nächste Frames danach (z. B. 15.000ms = 900px, auf Bildschirm bei ~180px) -> scrollX bleibt exakt stehen!
        double pageScrollX = view.getTimelineGridView().getScrollX();
        service.setPositionMs(15_000L);
        view.updatePlaybackFrame(15_000L);
        assertThat(view.getTimelineGridView().getScrollX()).isEqualTo(pageScrollX);

        // 4. Seek zurück an den Anfang (1.000ms = 60px < pageScrollX) -> springt zurück auf 0!
        service.setPositionMs(1_000L);
        view.updatePlaybackFrame(1_000L);
        assertThat(view.getTimelineGridView().getScrollX()).isEqualTo(0.0);

        service.stopPlayback();
    }

    @Test
    @DisplayName("Sollte lblTimeDisplay als feste Relayout-Boundary konfigurieren")
    void shouldIsolateTimeDisplayLayoutBoundary() {
        TimelineService service = new TimelineService();
        TimelineMainView view = new TimelineMainView(service);

        assertThat(view.getLblTimeDisplay().getMinWidth()).isEqualTo(view.getLblTimeDisplay().getPrefWidth());
        assertThat(view.getLblTimeDisplay().getMaxWidth()).isEqualTo(view.getLblTimeDisplay().getPrefWidth());
        assertThat(view.getLblTimeDisplay().getMinHeight()).isEqualTo(view.getLblTimeDisplay().getPrefHeight());
        assertThat(view.getLblTimeDisplay().getMaxHeight()).isEqualTo(view.getLblTimeDisplay().getPrefHeight());
    }
}

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
        service.setPositionMs(15000L);
        service.startPlayback();
        view.getGridScrollPane().setHvalue(0.8);

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
        assertThat(view.getGridScrollPane().getHvalue()).isEqualTo(0.0);
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
}

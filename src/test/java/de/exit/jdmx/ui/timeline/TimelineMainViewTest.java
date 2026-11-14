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
}

package de.exit.jdmx.ui.timeline;

import de.exit.jdmx.core.model.TimelineTrack;
import de.exit.jdmx.service.TimelineService;
import javafx.application.Platform;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für die {@link TimelineTrackHeaderBox} (linke Track-Box im FL Studio Stil).
 */
class TimelineTrackHeaderBoxTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit aktiv
        }
    }

    @Test
    @DisplayName("Sollte Track-Name, Mute- (M) und Solo- (S) Buttons initialisieren")
    void shouldInitializeWithTrackData() {
        TimelineService service = new TimelineService();
        TimelineTrack track = service.getTracks().get(0);
        TimelineTrackHeaderBox box = new TimelineTrackHeaderBox(track, service);

        assertThat(box.getLblName().getText()).isEqualTo("Track 1");
        
        assertThat(box.getBtnMute()).isNotNull();
        assertThat(box.getBtnMute().getText()).isEqualTo("M");
        assertThat(box.getBtnMute().getStyleClass()).contains("timeline-track-mute-btn");
        assertThat(box.getBtnMute().getStyleClass()).doesNotContain("muted");

        assertThat(box.getBtnSolo()).isNotNull();
        assertThat(box.getBtnSolo().getText()).isEqualTo("S");
        assertThat(box.getBtnSolo().getStyleClass()).contains("timeline-track-solo-btn");
        assertThat(box.getBtnSolo().getStyleClass()).doesNotContain("solo");
    }

    @Test
    @DisplayName("Sollte Mute-Button umschalten und StyleClass anpassen")
    void shouldToggleMuteOnClick() {
        TimelineService service = new TimelineService();
        TimelineTrack track = service.getTracks().get(0);
        TimelineTrackHeaderBox box = new TimelineTrackHeaderBox(track, service);

        assertThat(track.isMuted()).isFalse();
        assertThat(box.getBtnMute().getStyleClass()).doesNotContain("muted");

        // Klick auf Mute
        box.getBtnMute().fire();

        assertThat(track.isMuted()).isTrue();
        assertThat(box.getBtnMute().getStyleClass()).contains("muted");

        // Erneuter Klick auf Mute
        box.getBtnMute().fire();

        assertThat(track.isMuted()).isFalse();
        assertThat(box.getBtnMute().getStyleClass()).doesNotContain("muted");
    }

    @Test
    @DisplayName("Sollte Solo-Button umschalten und StyleClass anpassen")
    void shouldToggleSoloOnClick() {
        TimelineService service = new TimelineService();
        TimelineTrack track = service.getTracks().get(0);
        TimelineTrackHeaderBox box = new TimelineTrackHeaderBox(track, service);

        assertThat(track.isSolo()).isFalse();
        assertThat(box.getBtnSolo().getStyleClass()).doesNotContain("solo");

        // Klick auf Solo
        box.getBtnSolo().fire();

        assertThat(track.isSolo()).isTrue();
        assertThat(box.getBtnSolo().getStyleClass()).contains("solo");

        // Erneuter Klick auf Solo
        box.getBtnSolo().fire();

        assertThat(track.isSolo()).isFalse();
        assertThat(box.getBtnSolo().getStyleClass()).doesNotContain("solo");
    }

    @Test
    @DisplayName("Sollte Doppelklick auf Label Inline-Editing starten")
    void shouldStartInlineEditingOnDoubleClick() {
        TimelineService service = new TimelineService();
        TimelineTrack track = service.getTracks().get(0);
        TimelineTrackHeaderBox box = new TimelineTrackHeaderBox(track, service);

        assertThat(box.isEditing()).isFalse();

        // Doppelklick simulieren
        MouseEvent doubleClick = new MouseEvent(
                MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0,
                MouseButton.PRIMARY, 2, false, false, false, false,
                true, false, false, false, false, false, null
        );
        box.getLblName().fireEvent(doubleClick);

        assertThat(box.isEditing()).isTrue();
        assertThat(box.getTxtRename()).isNotNull();
        assertThat(box.getTxtRename().getText()).isEqualTo("Track 1");
    }

    @Test
    @DisplayName("Sollte feste Mindest- und Vorzugsbreite von mindestens 160px besitzen")
    void shouldHaveStableMinimumWidth() {
        TimelineService service = new TimelineService();
        TimelineTrack track = service.getTracks().get(0);
        TimelineTrackHeaderBox box = new TimelineTrackHeaderBox(track, service);

        assertThat(box.getMinWidth()).isGreaterThanOrEqualTo(160.0);
        assertThat(box.getPrefWidth()).isGreaterThanOrEqualTo(160.0);
    }
}

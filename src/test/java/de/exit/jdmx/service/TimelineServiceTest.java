package de.exit.jdmx.service;

import de.exit.jdmx.core.model.TimelineTrack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit-Tests für den {@link TimelineService}.
 */
class TimelineServiceTest {

    @Test
    @DisplayName("Sollte standardmäßig mit Track 1 starten")
    void shouldStartWithDefaultTrackOne() {
        TimelineService service = new TimelineService();

        List<TimelineTrack> tracks = service.getTracks();
        assertThat(tracks).hasSize(1);
        assertThat(tracks.get(0).getName()).isEqualTo("Track 1");
        assertThat(tracks.get(0).isMuted()).isFalse();
    }

    @Test
    @DisplayName("Sollte neue Tracks mit fortlaufender Nummerierung hinzufügen")
    void shouldAddTracksSequentially() {
        TimelineService service = new TimelineService();

        TimelineTrack track2 = service.addTrack();
        assertThat(track2.getName()).isEqualTo("Track 2");
        assertThat(service.getTracks()).hasSize(2);

        TimelineTrack track3 = service.addTrack();
        assertThat(track3.getName()).isEqualTo("Track 3");
        assertThat(service.getTracks()).hasSize(3);
    }

    @Test
    @DisplayName("Sollte Track 2 auch nach Umbenennen von Track 1 erzeugen")
    void shouldGenerateTrackTwoEvenIfTrackOneIsRenamed() {
        TimelineService service = new TimelineService();
        TimelineTrack track1 = service.getTracks().get(0);

        service.renameTrack(track1.getId(), "Test");
        assertThat(track1.getName()).isEqualTo("Test");

        TimelineTrack track2 = service.addTrack();
        assertThat(track2.getName()).isEqualTo("Track 2");
    }

    @Test
    @DisplayName("Sollte Track umbenennen und Validierung durchführen")
    void shouldRenameTrackWithValidation() {
        TimelineService service = new TimelineService();
        String trackId = service.getTracks().get(0).getId();

        service.renameTrack(trackId, "Showtanz 2026");
        assertThat(service.findTrackById(trackId)).isPresent();
        assertThat(service.findTrackById(trackId).get().getName()).isEqualTo("Showtanz 2026");

        assertThatThrownBy(() -> service.renameTrack(trackId, null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> service.renameTrack(trackId, "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Sollte Mute-Status umschalten und Listener benachrichtigen")
    void shouldToggleMuteAndNotifyListeners() {
        TimelineService service = new TimelineService();
        String trackId = service.getTracks().get(0).getId();

        AtomicInteger changeEvents = new AtomicInteger();
        service.addChangeListener(changeEvents::incrementAndGet);

        boolean muted = service.toggleTrackMute(trackId);
        assertThat(muted).isTrue();
        assertThat(service.findTrackById(trackId).get().isMuted()).isTrue();
        assertThat(changeEvents.get()).isEqualTo(1);

        muted = service.toggleTrackMute(trackId);
        assertThat(muted).isFalse();
        assertThat(service.findTrackById(trackId).get().isMuted()).isFalse();
        assertThat(changeEvents.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("Sollte Tracks entfernen können")
    void shouldRemoveTrack() {
        TimelineService service = new TimelineService();
        TimelineTrack track2 = service.addTrack();
        assertThat(service.getTracks()).hasSize(2);

        boolean removed = service.removeTrack(track2.getId());
        assertThat(removed).isTrue();
        assertThat(service.getTracks()).hasSize(1);
    }
}

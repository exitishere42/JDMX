package de.exit.jdmx.service;

import de.exit.jdmx.core.model.TimelineTrack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit-Tests für Song-Zuweisung und Track-Verknüpfung im {@link TimelineService}.
 */
class TimelineServiceSongAssignmentTest {

    @Test
    @DisplayName("Sollte Song einer Spur zuweisen und aktiven Song setzen")
    void shouldAssignSongToTrack() {
        TimelineService service = new TimelineService();
        TimelineTrack track = service.getTracks().get(0);
        File songFile = new File("fanfare.mp3");

        service.importSong(songFile);
        boolean assigned = service.assignSongToTrack(track.getId(), songFile);

        assertThat(assigned).isTrue();
        assertThat(track.hasSong()).isTrue();
        assertThat(track.getSongFile()).isEqualTo(songFile);
        assertThat(track.getSongName()).isEqualTo("fanfare.mp3");
        assertThat(service.getActiveSongFile()).isEqualTo(songFile);
    }

    @Test
    @DisplayName("Sollte Track automatisch umbenennen, wenn er noch Standardname trägt")
    void shouldAutoRenameDefaultTrackOnSongAssign() {
        TimelineService service = new TimelineService();
        TimelineTrack track = service.getTracks().get(0);
        assertThat(track.getName()).isEqualTo("Track 1");

        File songFile = new File("showtanz2026.wav");
        service.assignSongToTrack(track.getId(), songFile);

        assertThat(track.getName()).isEqualTo("showtanz2026.wav");
    }

    @Test
    @DisplayName("Sollte benutzerdefinierten Track-Namen bei Song-Zuweisung beibehalten")
    void shouldPreserveCustomTrackNameOnSongAssign() {
        TimelineService service = new TimelineService();
        TimelineTrack track = service.getTracks().get(0);
        service.renameTrack(track.getId(), "Hauptshow");

        File songFile = new File("showtanz2026.wav");
        service.assignSongToTrack(track.getId(), songFile);

        assertThat(track.getName()).isEqualTo("Hauptshow");
        assertThat(track.getSongName()).isEqualTo("showtanz2026.wav");
    }

    @Test
    @DisplayName("Sollte Song-Datei anhand des Namens auffinden")
    void shouldFindSongFileByName() {
        TimelineService service = new TimelineService();
        File song1 = new File("song1.mp3");
        File song2 = new File("song2.wav");
        service.importSong(song1);
        service.importSong(song2);

        assertThat(service.findSongFileByName("song1.mp3")).contains(song1);
        assertThat(service.findSongFileByName("song2.wav")).contains(song2);
        assertThat(service.findSongFileByName("unbekannt.mp3")).isEmpty();
    }
}

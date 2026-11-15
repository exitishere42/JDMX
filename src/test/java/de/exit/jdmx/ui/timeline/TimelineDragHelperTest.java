package de.exit.jdmx.ui.timeline;

import de.exit.jdmx.service.TimelineService;
import de.exit.jdmx.ui.stage.ProjectExplorerView;
import javafx.application.Platform;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit-Tests für {@link TimelineDragHelper} (Robuste Dateiextraktion aus Dragboards unter Windows & Linux).
 */
class TimelineDragHelperTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Bereits gestartet
        }
    }

    @Test
    @DisplayName("Sollte Audiodatei aus db.getFiles() extrahieren")
    void shouldExtractAudioFileFromFilesList(@TempDir Path tempDir) throws IOException {
        File mp3File = tempDir.resolve("track.mp3").toFile();
        Files.createFile(mp3File.toPath());

        TimelineService service = new TimelineService();
        Dragboard db = mock(Dragboard.class);
        when(db.hasFiles()).thenReturn(true);
        when(db.getFiles()).thenReturn(List.of(mp3File));

        Optional<File> extracted = TimelineDragHelper.extractAudioFile(db, service);

        assertThat(extracted).isPresent().contains(mp3File);
    }

    @Test
    @DisplayName("Sollte Audiodatei aus Linux file:// URL extrahieren")
    void shouldExtractAudioFileFromLinuxFileUrl(@TempDir Path tempDir) throws IOException {
        File wavFile = tempDir.resolve("test_sound.wav").toFile();
        Files.createFile(wavFile.toPath());

        TimelineService service = new TimelineService();
        Dragboard db = mock(Dragboard.class);
        when(db.hasFiles()).thenReturn(false);
        when(db.hasUrl()).thenReturn(true);
        when(db.getUrl()).thenReturn(wavFile.toURI().toString());

        Optional<File> extracted = TimelineDragHelper.extractAudioFile(db, service);

        assertThat(extracted).isPresent();
        assertThat(extracted.get().getAbsolutePath()).isEqualTo(wavFile.getAbsolutePath());
    }

    @Test
    @DisplayName("Sollte Audiodatei aus Linux text/uri-list String mit URL-Encoding extrahieren")
    void shouldExtractAudioFileFromLinuxUriListString(@TempDir Path tempDir) throws IOException {
        File mp3WithSpaces = tempDir.resolve("RCV_Einmarsch Blau Weiß Garde.mp3").toFile();
        Files.createFile(mp3WithSpaces.toPath());

        TimelineService service = new TimelineService();
        Dragboard db = mock(Dragboard.class);
        when(db.hasFiles()).thenReturn(false);
        when(db.hasUrl()).thenReturn(false);
        when(db.hasString()).thenReturn(true);
        // Typisches Nautilus-Format unter GNOME/Linux:
        String uriList = mp3WithSpaces.toURI().toString() + "\r\n";
        when(db.getString()).thenReturn(uriList);

        Optional<File> extracted = TimelineDragHelper.extractAudioFile(db, service);

        assertThat(extracted).isPresent();
        assertThat(extracted.get().getAbsolutePath()).isEqualTo(mp3WithSpaces.getAbsolutePath());
    }

    @Test
    @DisplayName("Sollte importierten Song anhand des Namens oder Explorer-IDs auflösen")
    void shouldResolveImportedSongByNameOrId(@TempDir Path tempDir) throws IOException {
        File songFile = tempDir.resolve("showtanz.mp3").toFile();
        Files.createFile(songFile.toPath());

        TimelineService service = new TimelineService();
        service.importSong(songFile);

        Dragboard db = mock(Dragboard.class);
        when(db.hasFiles()).thenReturn(false);
        when(db.hasUrl()).thenReturn(false);
        when(db.hasContent(ProjectExplorerView.EXPLORER_ITEM_FORMAT)).thenReturn(true);
        when(db.getContent(ProjectExplorerView.EXPLORER_ITEM_FORMAT)).thenReturn("song-showtanz.mp3");

        Optional<File> extracted = TimelineDragHelper.extractAudioFile(db, service);

        assertThat(extracted).isPresent().contains(songFile);
    }

    @Test
    @DisplayName("Sollte absolute Dateipfade direkt als File erkennen")
    void shouldResolveAbsoluteFilePathString(@TempDir Path tempDir) throws IOException {
        File songFile = tempDir.resolve("intro.wav").toFile();
        Files.createFile(songFile.toPath());

        TimelineService service = new TimelineService();
        Dragboard db = mock(Dragboard.class);
        when(db.hasFiles()).thenReturn(false);
        when(db.hasUrl()).thenReturn(false);
        when(db.hasString()).thenReturn(true);
        when(db.getString()).thenReturn(songFile.getAbsolutePath());

        Optional<File> extracted = TimelineDragHelper.extractAudioFile(db, service);

        assertThat(extracted).isPresent().contains(songFile);
    }

    @Test
    @DisplayName("Sollte leeres Optional liefern, wenn Dragboard keine Audiodaten enthält")
    void shouldReturnEmptyForNonAudioDragboard() {
        TimelineService service = new TimelineService();
        Dragboard db = mock(Dragboard.class);
        when(db.hasFiles()).thenReturn(false);
        when(db.hasUrl()).thenReturn(false);
        when(db.hasString()).thenReturn(false);

        Optional<File> extracted = TimelineDragHelper.extractAudioFile(db, service);

        assertThat(extracted).isEmpty();
    }
}

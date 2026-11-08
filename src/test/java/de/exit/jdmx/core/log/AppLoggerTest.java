package de.exit.jdmx.core.log;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Unit-Tests für den dateibasierten System- und DMX-Logger {@link AppLogger}.
 */
class AppLoggerTest {

    @BeforeAll
    static void setUp() {
        AppLogger.init();
    }

    @AfterAll
    static void tearDown() {
        AppLogger.flush();
    }

    @Test
    @DisplayName("Sollte die Log-Datei erfolgreich initialisieren und Pfad bereitstellen")
    void shouldInitializeLogFile() {
        Path logFile = AppLogger.getLogFilePath();
        assertThat(logFile).isNotNull();
        assertThat(Files.exists(logFile)).isTrue();
    }

    @Test
    @DisplayName("Sollte System- und DMX-Ereignisse strukturiert in die .log-Datei schreiben")
    void shouldWriteLogEntriesToFile() throws IOException, InterruptedException {
        String testMarker = "TEST-LOG-ENTRY-" + System.currentTimeMillis();
        AppLogger.info("Information: " + testMarker);
        AppLogger.dmx("[TEST-DMX] Uni=1 Ch=12 Val: 0 -> 255 | Marker=" + testMarker);
        AppLogger.logDmxChange(1, 15, 0, 180, "TestTrigger", "Spot 1 [Start=1]");

        AppLogger.flush();
        Thread.sleep(100);

        Path logFile = AppLogger.getLogFilePath();
        List<String> lines = Files.readAllLines(logFile);
        if (lines.stream().noneMatch(l -> l.contains(testMarker))) {
            // Falls eine andere JDMX-Instanz läuft, schreibt der FileHandler rotierend in jdmx.log.1 etc.
            try (var stream = Files.list(AppLogger.getLogDirectoryPath())) {
                for (Path p : stream.filter(p -> p.getFileName().toString().startsWith("jdmx.log")).toList()) {
                    List<String> altLines = Files.readAllLines(p);
                    if (altLines.stream().anyMatch(l -> l.contains(testMarker))) {
                        lines = altLines;
                        break;
                    }
                }
            }
        }

        boolean foundInfo = lines.stream().anyMatch(l -> l.contains(testMarker));
        boolean foundDmx = lines.stream().anyMatch(l -> l.contains("Uni=1 Ch=12 Val: 0 -> 255"));
        boolean foundChange = lines.stream().anyMatch(l -> l.contains("Uni=1 Ch=15 Val: 0 -> 180"));

        assertThat(foundInfo).isTrue();
        assertThat(foundDmx).isTrue();
        assertThat(foundChange).isTrue();
    }

    @Test
    @DisplayName("Sollte Log-Verzeichnis bereitstellen")
    void shouldProvideLogDirectory() {
        Path logDir = AppLogger.getLogDirectoryPath();
        assertThat(logDir).isNotNull();
        assertThat(Files.isDirectory(logDir)).isTrue();
    }
}

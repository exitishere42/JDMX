package de.exit.jdmx.core.log;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.logging.*;

/**
 * Zentraler, robuster Anwendungs- und DMX-File-Logger für JDMX.
 * <p>
 * Schreibt alle Systemereignisse, Exceptions und feingranulare DMX-Kanal-Änderungen
 * in eine rotierende Log-Datei ({@code jdmx.log}), damit Benutzer Logik- und
 * Hardwarefehler unkompliziert zur Diagnose und Behebung bereitstellen können.
 * </p>
 * <p>
 * Speicherort der Log-Datei:
 * <ul>
 *     <li><b>Windows:</b> {@code %USERPROFILE%\jdmx\logs\jdmx.log} (oder lokales {@code ./logs/jdmx.log})</li>
 *     <li><b>Linux:</b> {@code ~/jdmx/logs/jdmx.log}</li>
 * </ul>
 * </p>
 */
public final class AppLogger {

    private static final Logger LOGGER = Logger.getLogger("de.exit.jdmx");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static Path logFilePath;
    private static Path logDirPath;
    private static FileHandler fileHandler;
    private static boolean initialized = false;

    private AppLogger() {
        // Utility-Klasse
    }

    /**
     * Initialisiert das Logging-System und hängt den rotierenden FileHandler ein.
     */
    public static synchronized void init() {
        if (initialized) {
            return;
        }

        try {
            Path targetDir = resolveLogDirectory();
            Files.createDirectories(targetDir);

            logDirPath = targetDir;
            logFilePath = targetDir.resolve("jdmx.log");
            if (!Files.exists(logFilePath)) {
                Files.createFile(logFilePath);
            }

            // FileHandler: Schreibt zuverlässig und fortlaufend in jdmx.log
            fileHandler = new FileHandler(logFilePath.toString(), true);
            fileHandler.setLevel(Level.ALL);
            fileHandler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    String time = LocalDateTime.now().format(TIME_FORMAT);
                    String level = record.getLevel().getName();
                    String thread = Thread.currentThread().getName();
                    String source = record.getLoggerName() != null ? record.getLoggerName() : "App";

                    StringBuilder sb = new StringBuilder();
                    sb.append("[").append(time).append("] ")
                            .append("[").append(String.format("%-7s", level)).append("] ")
                            .append("[").append(thread).append("] ")
                            .append("[").append(source).append("] ")
                            .append(formatMessage(record))
                            .append(System.lineSeparator());

                    if (record.getThrown() != null) {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        record.getThrown().printStackTrace(pw);
                        sb.append(sw);
                    }

                    return sb.toString();
                }
            });

            // Root-Logger anhängen
            Logger rootLogger = Logger.getLogger("");
            rootLogger.addHandler(fileHandler);

            initialized = true;

            info("================================================================================");
            info("JDMX Application Logger gestartet");
            info("Log-Datei: %s", logFilePath.toAbsolutePath());
            info("Betriebssystem: %s (%s)", System.getProperty("os.name"), System.getProperty("os.arch"));
            info("Java Version: %s (%s)", System.getProperty("java.version"), System.getProperty("java.vendor"));
            info("================================================================================");

        } catch (Exception e) {
            System.err.println("[AppLogger] Warnung: Log-Datei konnte nicht initialisiert werden: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Path resolveLogDirectory() {
        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.isBlank()) {
            Path standardDir = Paths.get(userHome, "jdmx", "logs");
            try {
                Files.createDirectories(standardDir);
                return standardDir;
            } catch (Exception ignored) {
                // Fallback auf lokales Arbeitsverzeichnis
            }
        }
        return Paths.get("logs").toAbsolutePath();
    }

    /**
     * Schreibt eine Information in die Log-Datei.
     */
    public static void info(String format, Object... args) {
        init();
        String msg = (args != null && args.length > 0) ? String.format(format, args) : format;
        LOGGER.log(Level.INFO, msg);
    }

    /**
     * Schreibt eine Warnung in die Log-Datei.
     */
    public static void warn(String format, Object... args) {
        init();
        String msg = (args != null && args.length > 0) ? String.format(format, args) : format;
        LOGGER.log(Level.WARNING, msg);
    }

    /**
     * Schreibt einen Fehler samt Stacktrace in die Log-Datei.
     */
    public static void error(String message, Throwable t) {
        init();
        LOGGER.log(Level.SEVERE, message, t);
    }

    /**
     * Protokolliert eine dedizierte DMX-Aktion (z. B. Fader, Locate, Blackout).
     */
    public static void dmx(String format, Object... args) {
        init();
        String msg = (args != null && args.length > 0) ? String.format(format, args) : format;
        LOGGER.log(Level.INFO, "[DMX] " + msg);
    }

    /**
     * Protokolliert eine DMX-Kanal-Änderung mit voller Scheinwerfer- und Quellenzuordnung.
     *
     * @param universe      1-basiertes Universum (1 bis 4)
     * @param channel       1-basierter Kanal (1 bis 512)
     * @param oldVal        Vorheriger DMX-Wert
     * @param newVal        Neuer DMX-Wert
     * @param triggerSource Auslöser (z. B. "LightDesk", "Stage", "setIntensity")
     * @param fixtureInfo   Scheinwerfer-Information oder null
     */
    public static void logDmxChange(int universe, int channel, int oldVal, int newVal, String triggerSource, String fixtureInfo) {
        init();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Uni=%d Ch=%d Val: %d -> %d", universe, channel, oldVal, newVal));
        if (triggerSource != null && !triggerSource.isBlank()) {
            sb.append(" | Trigger='").append(triggerSource).append("'");
        }
        if (fixtureInfo != null && !fixtureInfo.isBlank()) {
            sb.append(" | Fixture=").append(fixtureInfo);
        }
        LOGGER.log(Level.INFO, "[DMX-CHANGE] " + sb);
    }

    /**
     * Erzwingt das sofortige Schreiben aller gepufferten Log-Einträge auf die Festplatte.
     */
    public static synchronized void flush() {
        if (fileHandler != null) {
            fileHandler.flush();
        }
    }

    /**
     * Liefert den absoluten Pfad zur aktuellen Log-Datei.
     */
    public static Path getLogFilePath() {
        init();
        return logFilePath;
    }

    /**
     * Liefert den Verzeichnispfad des Log-Ordners.
     */
    public static Path getLogDirectoryPath() {
        init();
        return logDirPath;
    }

    /**
     * Öffnet den Log-Ordner im Dateimanager des Betriebssystems (Windows & Linux kompatibel).
     *
     * @return true, falls der Ordner erfolgreich geöffnet werden konnte
     */
    public static boolean openLogFolder() {
        init();
        if (logDirPath == null) {
            return false;
        }

        try {
            File dir = logDirPath.toFile();
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(dir);
                return true;
            }

            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                new ProcessBuilder("explorer.exe", dir.getAbsolutePath()).start();
                return true;
            } else if (os.contains("linux") || os.contains("unix")) {
                new ProcessBuilder("xdg-open", dir.getAbsolutePath()).start();
                return true;
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", dir.getAbsolutePath()).start();
                return true;
            }
        } catch (Exception e) {
            warn("Konnte Log-Ordner nicht im Dateimanager öffnen: %s", e.getMessage());
        }
        return false;
    }
}

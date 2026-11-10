package de.exit.jdmx.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.exit.jdmx.core.project.model.ProjectData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Service für die Persistierung und das Laden von JDMX-Projektdateien (*.jdmx).
 * <p>
 * Verwaltet die Speicherorte abhängig von der Ausführungsumgebung:
 * Im Entwicklungsmodus (IntelliJ IDEA) wird das lokale Verzeichnis {@code ./projects/} verwendet.
 * In der installierten Standalone-Version wird das betriebssystemspezifische Konfigurationsverzeichnis
 * genutzt (Windows: {@code %APPDATA%/jdmx/projects/}, Linux: {@code ~/.config/jdmx/projects/}),
 * sodass Anwendungs-Updates bestehende Projekte und Voreinstellungen niemals überschreiben.
 * </p>
 */
public class ProjectStorageService {

    private static final Logger log = LoggerFactory.getLogger(ProjectStorageService.class);
    private static final String DEFAULT_PROJECT_FILENAME = "default.jdmx";

    private final ObjectMapper objectMapper;
    private final File projectsDirectory;

    /**
     * Erstellt eine neue Instanz des {@link ProjectStorageService} und initialisiert das Projektverzeichnis.
     */
    public ProjectStorageService() {
        this(resolveDefaultProjectsDirectory());
    }

    /**
     * Erstellt eine Instanz mit einem spezifischen Projektverzeichnis (vorrangig für Tests).
     *
     * @param projectsDirectory Das zu verwendende Projektverzeichnis
     */
    public ProjectStorageService(File projectsDirectory) {
        this.projectsDirectory = projectsDirectory;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        if (!this.projectsDirectory.exists()) {
            boolean created = this.projectsDirectory.mkdirs();
            if (created) {
                log.info("Projektverzeichnis erstellt: {}", this.projectsDirectory.getAbsolutePath());
            }
        }
    }

    /**
     * Ermittelt das primäre Projektverzeichnis abhängig von der Betriebsumgebung.
     *
     * @return Das Verzeichnis für Projektdateien
     */
    public static File resolveDefaultProjectsDirectory() {
        boolean isDevEnvironment = new File("./pom.xml").exists() && new File("./src").exists();
        if (isDevEnvironment) {
            return new File("./projects");
        }

        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) {
                return new File(appData, "jdmx/projects");
            }
            return new File(System.getProperty("user.home"), "AppData/Roaming/jdmx/projects");
        } else {
            return new File(System.getProperty("user.home"), ".config/jdmx/projects");
        }
    }

    /**
     * Gibt das aktive Projektverzeichnis zurück.
     *
     * @return Das Projektverzeichnis
     */
    public File getProjectsDirectory() {
        if (!projectsDirectory.exists()) {
            projectsDirectory.mkdirs();
        }
        return projectsDirectory;
    }

    /**
     * @deprecated default.jdmx wird nicht mehr unterstützt. Projekte müssen explizit benannt/geladen werden.
     */
    @Deprecated
    public File getDefaultProjectFile() {
        return new File(getProjectsDirectory(), DEFAULT_PROJECT_FILENAME);
    }

    /**
     * @deprecated default.jdmx wird nicht mehr unterstützt.
     */
    @Deprecated
    public boolean hasDefaultProject() {
        File file = getDefaultProjectFile();
        return file.exists() && file.isFile() && file.length() > 0;
    }

    /**
     * Speichert die übergebenen Projektdaten in einer Datei.
     *
     * @param project    Die zu speichernden Projektdaten
     * @param targetFile Die Zieldatei (*.jdmx)
     * @throws IOException Wenn beim Schreibvorgang ein Fehler auftritt
     */
    public void saveProject(ProjectData project, File targetFile) throws IOException {
        if (project == null) {
            throw new IllegalArgumentException("ProjectData darf nicht null sein");
        }
        if (targetFile == null) {
            throw new IllegalArgumentException("targetFile darf nicht null sein");
        }

        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        objectMapper.writeValue(targetFile, project);
        log.info("Projekt erfolgreich gespeichert in: {}", targetFile.getAbsolutePath());
    }

    /**
     * Lädt die Projektdaten aus einer Datei.
     *
     * @param sourceFile Die zu lesende Projektdatei (*.jdmx)
     * @return Die deserialisierten Projektdaten
     * @throws IOException Wenn beim Lesevorgang ein Fehler auftritt
     */
    public ProjectData loadProject(File sourceFile) throws IOException {
        if (sourceFile == null || !sourceFile.exists()) {
            throw new IOException("Projektdatei existiert nicht: " + (sourceFile != null ? sourceFile.getAbsolutePath() : "null"));
        }
        ProjectData project = objectMapper.readValue(sourceFile, ProjectData.class);
        log.info("Projekt erfolgreich geladen aus: {} (Name: '{}')", sourceFile.getAbsolutePath(), project.projectName());
        return project;
    }

    /**
     * @deprecated default.jdmx wird nicht mehr unterstützt.
     */
    @Deprecated
    public void saveDefaultProject(ProjectData project) throws IOException {
        saveProject(project, getDefaultProjectFile());
    }

    /**
     * @deprecated default.jdmx wird nicht mehr unterstützt.
     */
    @Deprecated
    public ProjectData loadDefaultProject() throws IOException {
        return loadProject(getDefaultProjectFile());
    }
}

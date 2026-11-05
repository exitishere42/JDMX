package de.exit.jdmx.service;

import de.exit.jdmx.core.fixture.parser.FixtureParser;
import de.exit.jdmx.core.fixture.parser.GdtfParser;
import de.exit.jdmx.core.fixture.parser.QxfParser;
import de.exit.jdmx.core.model.FixtureDefinition;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service zur Verwaltung aller verfügbaren Scheinwerfer-Definitionen (Fixture Library).
 * <p>
 * Lädt vorinstallierte Standardgeräte aus den Ressourcen und ermöglicht den flexiblen
 * Import von Profilen im QLC+ (.qxf) und GDTF (.gdtf) Format.
 * </p>
 */
public class FixtureLibraryService {

    private static final Logger LOGGER = Logger.getLogger(FixtureLibraryService.class.getName());

    private static final String[] BUILTIN_FIXTURES = {
            "/fixtures/generic-dimmer.qxf",
            "/fixtures/generic-rgb-par.qxf",
            "/fixtures/generic-rgbw-par.qxf",
            "/fixtures/generic-moving-head.qxf"
    };

    private final List<FixtureDefinition> definitions = new CopyOnWriteArrayList<>();
    private final List<FixtureParser> parsers = new ArrayList<>();

    public FixtureLibraryService() {
        // Parser registrieren
        parsers.add(new QxfParser());
        parsers.add(new GdtfParser());

        // Vorinstallierte Standard-Geräte laden
        loadBuiltinFixtures();
    }

    private void loadBuiltinFixtures() {
        for (String resourcePath : BUILTIN_FIXTURES) {
            try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
                if (in != null) {
                    String fileName = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
                    importFixture(in, fileName);
                } else {
                    LOGGER.warning("Built-in fixture resource not found: " + resourcePath);
                }
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Failed to load built-in fixture: " + resourcePath, ex);
            }
        }
    }

    /**
     * Importiert eine Scheinwerfer-Datei (*.qxf oder *.gdtf).
     *
     * @param file Die zu importierende Datei
     * @return Die geparste {@link FixtureDefinition}
     * @throws Exception Wenn kein passender Parser existiert oder die Datei fehlerhaft ist
     */
    public FixtureDefinition importFixture(File file) throws Exception {
        Objects.requireNonNull(file, "file must not be null");
        try (InputStream in = new FileInputStream(file)) {
            return importFixture(in, file.getName());
        }
    }

    /**
     * Importiert ein Scheinwerfer-Profil aus einem Datenstrom.
     *
     * @param in       Der Datenstrom
     * @param fileName Der Dateiname zur Erkennung des Formats
     * @return Die geparste und registrierte {@link FixtureDefinition}
     * @throws Exception Bei Parsing-Fehlern
     */
    public FixtureDefinition importFixture(InputStream in, String fileName) throws Exception {
        Objects.requireNonNull(in, "in must not be null");
        Objects.requireNonNull(fileName, "fileName must not be null");

        FixtureParser matchedParser = null;
        for (FixtureParser parser : parsers) {
            if (parser.supports(fileName)) {
                matchedParser = parser;
                break;
            }
        }

        if (matchedParser == null) {
            throw new IllegalArgumentException("Unsupported fixture format: " + fileName + ". Expected .qxf or .gdtf");
        }

        FixtureDefinition def = matchedParser.parse(in, fileName);
        addDefinition(def);
        return def;
    }

    /**
     * Fügt eine Definition manuell hinzu (z. B. für programmatisch erstellte Geräte).
     */
    public void addDefinition(FixtureDefinition def) {
        Objects.requireNonNull(def, "def must not be null");
        // Bestehende Definition mit gleicher ID ggf. ersetzen
        definitions.removeIf(d -> d.getId().equals(def.getId()));
        definitions.add(def);
    }

    /**
     * Gibt alle registrierten Scheinwerfer-Profile zurück.
     */
    public List<FixtureDefinition> getAllDefinitions() {
        return Collections.unmodifiableList(definitions);
    }

    /**
     * Gibt alle vorkommenden Hersteller alphabetisch sortiert zurück.
     */
    public List<String> getManufacturers() {
        Set<String> manufacturers = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (FixtureDefinition def : definitions) {
            manufacturers.add(def.getManufacturer());
        }
        return new ArrayList<>(manufacturers);
    }

    /**
     * Filtert alle Geräte eines bestimmten Herstellers.
     */
    public List<FixtureDefinition> getByManufacturer(String manufacturer) {
        if (manufacturer == null || manufacturer.isBlank()) {
            return Collections.emptyList();
        }
        List<FixtureDefinition> results = new ArrayList<>();
        for (FixtureDefinition def : definitions) {
            if (def.getManufacturer().equalsIgnoreCase(manufacturer.trim())) {
                results.add(def);
            }
        }
        return results;
    }

    /**
     * Durchsucht alle Profile nach Hersteller- oder Modellname.
     */
    public List<FixtureDefinition> search(String query) {
        if (query == null || query.isBlank()) {
            return getAllDefinitions();
        }
        String q = query.toLowerCase(Locale.ROOT).trim();
        List<FixtureDefinition> results = new ArrayList<>();
        for (FixtureDefinition def : definitions) {
            if (def.getManufacturer().toLowerCase(Locale.ROOT).contains(q)
                    || def.getModel().toLowerCase(Locale.ROOT).contains(q)
                    || def.getFullName().toLowerCase(Locale.ROOT).contains(q)) {
                results.add(def);
            }
        }
        return results;
    }
}

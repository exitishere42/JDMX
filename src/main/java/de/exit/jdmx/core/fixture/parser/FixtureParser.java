package de.exit.jdmx.core.fixture.parser;

import de.exit.jdmx.core.model.FixtureDefinition;

import java.io.InputStream;

/**
 * Universelle Schnittstelle für Scheinwerfer-Profil-Parser (QXF, GDTF, JSON).
 */
public interface FixtureParser {

    /**
     * Prüft, ob dieser Parser das angegebene Dateiformat anhand des Dateinamens unterstützt.
     *
     * @param fileName Der Dateiname (z. B. "device.qxf" oder "fixture.gdtf")
     * @return True, falls dieser Parser das Format verarbeiten kann
     */
    boolean supports(String fileName);

    /**
     * Parst eine Scheinwerfer-Definition aus einem Input-Stream.
     *
     * @param inputStream      Der binäre oder textuelle Datenstrom
     * @param sourceIdentifier Name oder Pfad zur Identifikation und Fehlerbehandlung
     * @return Die geparste {@link FixtureDefinition}
     * @throws Exception Bei syntaktischen oder strukturellen Parsing-Fehlern
     */
    FixtureDefinition parse(InputStream inputStream, String sourceIdentifier) throws Exception;
}

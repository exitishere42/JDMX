package de.exit.jdmx;

import de.exit.jdmx.core.log.AppLogger;
import javafx.application.Application;

/**
 * Standard-Einstiegspunkt für die JDMX Anwendung.
 * <p>
 * Initialisiert den Logger und trennt den `main`-Aufruf von der JavaFX {@link Application}-Klasse, um
 * reibungsloses Starten über Standalone-JARs und Maven Plugins sicherzustellen.
 * </p>
 */
public final class Main {

    private Main() {
        // Utility-Klasse
    }

    /**
     * Hauptmethode zum Starten der Anwendung.
     *
     * @param args Kommandozeilenargumente
     */
    public static void main(String[] args) {
        AppLogger.init();
        Application.launch(JdmxApplication.class, args);
    }
}

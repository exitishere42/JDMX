package de.exit.jdmx;

import javafx.application.Application;

/**
 * Standard-Einstiegspunkt für die JDMX Anwendung.
 * <p>
 * Trennt den `main`-Aufruf von der JavaFX {@link Application}-Klasse, um
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
        Application.launch(JdmxApplication.class, args);
    }
}

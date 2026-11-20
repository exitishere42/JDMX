package de.exit.jdmx.service.support;

import de.exit.jdmx.service.UpdateService;

/**
 * Erfasst relevante System- und Laufzeitinformationen für Support- und Bug-Reports.
 *
 * @param osName       Name des Betriebssystems (z. B. "Windows 11", "Linux")
 * @param osArch       Architektur des Betriebssystems (z. B. "amd64", "aarch64")
 * @param javaVersion  Installierte Java-Laufzeitumgebung
 * @param javaVendor   Hersteller der JVM (z. B. "Eclipse Adoptium")
 * @param appVersion   Aktuelle Versionsnummer von JDMX
 * @param dmxRate      Aktuelle DMX-Paketrate in Paketen/Sekunde
 * @param recentLogs   Auszug der letzten Anwendungs-Logs
 */
public record SupportSystemInfo(
        String osName,
        String osArch,
        String javaVersion,
        String javaVendor,
        String appVersion,
        int dmxRate,
        String recentLogs
) {

    /**
     * Sammelt die aktuellen Systemdaten der laufenden Instanz.
     *
     * @param dmxRate    Aktuelle DMX-Ausgaberate
     * @param recentLogs Letzte Logzeilen zur Diagnose
     * @return Vollständige {@link SupportSystemInfo}
     */
    public static SupportSystemInfo collectCurrent(int dmxRate, String recentLogs) {
        String os = System.getProperty("os.name", "Unbekannt");
        String arch = System.getProperty("os.arch", "Unbekannt");
        String javaVer = System.getProperty("java.version", "21");
        String javaVen = System.getProperty("java.vendor", "Eclipse Adoptium");
        String appVer = UpdateService.CURRENT_VERSION;

        return new SupportSystemInfo(
                os,
                arch,
                javaVer,
                javaVen,
                appVer != null && !appVer.isBlank() ? appVer : "1.20.10",
                Math.max(0, dmxRate),
                recentLogs != null ? recentLogs : ""
        );
    }
}

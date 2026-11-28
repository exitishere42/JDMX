package de.exit.jdmx.core.model;

/**
 * Update-Kanal für die Software-Aktualisierung von JDMX.
 * <p>
 * Erlaubt das Umschalten zwischen stabilen Meilenstein-Releases und kontinuierlichen
 * Entwicklungs-Snapshots (Rolling Pre-Releases).
 * </p>
 */
public enum UpdateChannel {

    /**
     * Stabile Meilenstein-Releases von GitHub (/releases/latest).
     */
    RELEASE,

    /**
     * Kontinuierliche Entwicklungs- und Test-Builds (/releases/tags/snapshot).
     */
    SNAPSHOT;

    /**
     * Ermittelt den Standard-Kanal anhand der aktuell installierten Version.
     *
     * @param currentVersion Aktuell installierte Version (z. B. "0.36.0-SNAPSHOT" oder "0.35.0")
     * @return {@link #SNAPSHOT}, wenn die Version "-SNAPSHOT" enthält, sonst {@link #RELEASE}
     */
    public static UpdateChannel defaultForVersion(String currentVersion) {
        if (currentVersion != null && currentVersion.toUpperCase().contains("SNAPSHOT")) {
            return SNAPSHOT;
        }
        return RELEASE;
    }
}

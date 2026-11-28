package de.exit.jdmx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für den Update-Dienst {@link UpdateService}.
 * Prüft Versionsvergleiche (SemVer) und URL-Konfigurationen.
 */
class UpdateServiceTest {

    @Test
    @DisplayName("Sollte SemVer-Versionen präzise vergleichen")
    void shouldCompareVersionsCorrectly() {
        // Neuere Versionen
        assertThat(UpdateService.isVersionNewer("v1.2.1", "1.2.0")).isTrue();
        assertThat(UpdateService.isVersionNewer("1.3.0", "1.2.0")).isTrue();
        assertThat(UpdateService.isVersionNewer("v2.0.0", "1.2.0")).isTrue();
        assertThat(UpdateService.isVersionNewer("1.2.0.1", "1.2.0")).isTrue();

        // Ältere oder gleiche Versionen
        assertThat(UpdateService.isVersionNewer("1.2.0", "1.2.0")).isFalse();
        assertThat(UpdateService.isVersionNewer("v1.2.0", "1.2.0")).isFalse();
        assertThat(UpdateService.isVersionNewer("v1.1.9", "1.2.0")).isFalse();
        assertThat(UpdateService.isVersionNewer("1.0.0", "1.2.0")).isFalse();
        assertThat(UpdateService.isVersionNewer("v0.9.0", "1.2.0")).isFalse();

        // Null-Safety & ungültige Eingaben
        assertThat(UpdateService.isVersionNewer(null, "1.2.0")).isFalse();
        assertThat(UpdateService.isVersionNewer("v1.2.0", null)).isFalse();
        assertThat(UpdateService.isVersionNewer(null, null)).isFalse();
        assertThat(UpdateService.isVersionNewer("", "")).isFalse();
    }

    @Test
    @DisplayName("Sollte korrekte Konstanten für Repository und Version besitzen")
    void shouldHaveValidRepoAndVersionConstants() {
        assertThat(UpdateService.CURRENT_VERSION).matches("^\\d+\\.\\d+\\.\\d+.*$");
        assertThat(UpdateService.isSnapshot()).isEqualTo(UpdateService.CURRENT_VERSION.contains("-SNAPSHOT"));
        assertThat(UpdateService.GITHUB_REPO).isEqualTo("exitishere42/JDMX");
        assertThat(UpdateService.API_URL).isEqualTo("https://api.github.com/repos/exitishere42/JDMX/releases/latest");
        assertThat(UpdateService.SNAPSHOT_TAG).isEqualTo("snapshot");
        assertThat(UpdateService.SNAPSHOT_API_URL).isEqualTo("https://api.github.com/repos/exitishere42/JDMX/releases/tags/snapshot");
        assertThat(UpdateService.REPO_URL).isEqualTo("https://github.com/exitishere42/JDMX");
    }

    @Test
    @DisplayName("Sollte Version mit SNAPSHOT-Endung präzise mit Releases vergleichen")
    void shouldNotConsiderSameOrOlderVersionAsNewer() {
        assertThat(UpdateService.isVersionNewer("v0.35.0", "0.36.0-SNAPSHOT")).isFalse();
        assertThat(UpdateService.isVersionNewer("v0.34.0", "0.36.0-SNAPSHOT")).isFalse();
        assertThat(UpdateService.isVersionNewer("v0.36.0", "0.36.0-SNAPSHOT")).isFalse();
        assertThat(UpdateService.isVersionNewer("v0.36.1", "0.36.0-SNAPSHOT")).isTrue();
        assertThat(UpdateService.isVersionNewer("v0.37.0", "0.36.0-SNAPSHOT")).isTrue();
        assertThat(UpdateService.isVersionNewer("v1.0.0", "0.36.0-SNAPSHOT")).isTrue();
    }

    @Test
    @DisplayName("Sollte Snapshot-Aktualisierungen anhand von Zeitstempeln prüfen")
    void shouldCheckSnapshotUpdatesByTimestamp() {
        String olderTimestamp = "2026-11-28T10:00:00Z";
        String newerTimestamp = "2026-11-28T14:00:00Z";

        // Wenn remote neuer ist als lokal -> true
        assertThat(UpdateService.isSnapshotNewer(newerTimestamp, olderTimestamp, "0.36.0-SNAPSHOT")).isTrue();

        // Wenn remote und lokal derselbe Build sind (z. B. 30s Verzögerung bei GitHub Publish) -> false
        String sameBuildTimestamp = "2026-11-28T10:00:30Z";
        assertThat(UpdateService.isSnapshotNewer(sameBuildTimestamp, olderTimestamp, "0.36.0-SNAPSHOT")).isFalse();

        // Wenn remote älter ist als lokal -> false
        assertThat(UpdateService.isSnapshotNewer(olderTimestamp, newerTimestamp, "0.36.0-SNAPSHOT")).isFalse();

        // Wenn lokal ein Release läuft und remote ein Snapshot geprüft wird -> true (Wechsel auf Snapshot immer möglich)
        assertThat(UpdateService.isSnapshotNewer(olderTimestamp, newerTimestamp, "0.35.0")).isTrue();

        // Ungültige/fehlende Zeitstempel
        assertThat(UpdateService.isSnapshotNewer(null, newerTimestamp, "0.36.0-SNAPSHOT")).isFalse();
        assertThat(UpdateService.isSnapshotNewer(newerTimestamp, null, "0.36.0-SNAPSHOT")).isFalse();
    }

    @Test
    @DisplayName("Sollte Release-Wechsel aus einem Snapshot als verfügbar einstufen")
    void shouldAllowSwitchingFromSnapshotToRelease() {
        // Wer auf Snapshot ist, bekommt stabile Releases als Wechselmöglichkeit angeboten
        assertThat(UpdateService.isReleaseNewer("v0.35.0", "0.36.0-SNAPSHOT")).isTrue();
        assertThat(UpdateService.isReleaseNewer(null, "0.36.0-SNAPSHOT")).isFalse();
        assertThat(UpdateService.isReleaseNewer("", "0.36.0-SNAPSHOT")).isFalse();

        // Für normale Release-Installationen gilt striktes SemVer
        assertThat(UpdateService.isReleaseNewer("v0.35.0", "0.35.0")).isFalse();
        assertThat(UpdateService.isReleaseNewer("v0.36.0", "0.35.0")).isTrue();
    }
}

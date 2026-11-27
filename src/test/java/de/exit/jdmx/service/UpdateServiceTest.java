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
        assertThat(UpdateService.CURRENT_VERSION).isEqualTo("0.34.0");
        assertThat(UpdateService.GITHUB_REPO).isEqualTo("exitishere42/JDMX");
        assertThat(UpdateService.API_URL).isEqualTo("https://api.github.com/repos/exitishere42/JDMX/releases/latest");
        assertThat(UpdateService.REPO_URL).isEqualTo("https://github.com/exitishere42/JDMX");
    }

    @Test
    @DisplayName("Sollte Version 0.34.0 nicht als veraltet einstufen wenn GitHub denselben Tag liefert")
    void shouldNotConsiderSameOrOlderVersionAsNewer() {
        assertThat(UpdateService.isVersionNewer("v0.34.0", UpdateService.CURRENT_VERSION)).isFalse();
        assertThat(UpdateService.isVersionNewer("v0.33.0", UpdateService.CURRENT_VERSION)).isFalse();
        assertThat(UpdateService.isVersionNewer("v0.32.1", UpdateService.CURRENT_VERSION)).isFalse();
        assertThat(UpdateService.isVersionNewer("v0.32.0", UpdateService.CURRENT_VERSION)).isFalse();
        assertThat(UpdateService.isVersionNewer("v0.31.0", UpdateService.CURRENT_VERSION)).isFalse();
        assertThat(UpdateService.isVersionNewer("v0.30.0", UpdateService.CURRENT_VERSION)).isFalse();
        assertThat(UpdateService.isVersionNewer("v0.28.3", UpdateService.CURRENT_VERSION)).isFalse();
        assertThat(UpdateService.isVersionNewer("v0.28.2", UpdateService.CURRENT_VERSION)).isFalse();
        assertThat(UpdateService.isVersionNewer("v0.28.1", UpdateService.CURRENT_VERSION)).isFalse();
        assertThat(UpdateService.isVersionNewer("v0.28.0", UpdateService.CURRENT_VERSION)).isFalse();
        assertThat(UpdateService.isVersionNewer("v0.27.1", UpdateService.CURRENT_VERSION)).isFalse();
        assertThat(UpdateService.isVersionNewer("v0.27.0", UpdateService.CURRENT_VERSION)).isFalse();
        assertThat(UpdateService.isVersionNewer("v0.26.4", UpdateService.CURRENT_VERSION)).isFalse();
        assertThat(UpdateService.isVersionNewer("v0.26.3", UpdateService.CURRENT_VERSION)).isFalse();
        assertThat(UpdateService.isVersionNewer("v0.26.2", UpdateService.CURRENT_VERSION)).isFalse();
        assertThat(UpdateService.isVersionNewer("v0.26.1", UpdateService.CURRENT_VERSION)).isFalse();
        assertThat(UpdateService.isVersionNewer("v0.26.0", UpdateService.CURRENT_VERSION)).isFalse();
        assertThat(UpdateService.isVersionNewer("v0.25.1", UpdateService.CURRENT_VERSION)).isFalse();
        assertThat(UpdateService.isVersionNewer("v0.25.0", UpdateService.CURRENT_VERSION)).isFalse();
        assertThat(UpdateService.isVersionNewer("v0.24.0", UpdateService.CURRENT_VERSION)).isFalse();
        assertThat(UpdateService.isVersionNewer("v0.23.0", UpdateService.CURRENT_VERSION)).isFalse();
        assertThat(UpdateService.isVersionNewer("v0.1.0", UpdateService.CURRENT_VERSION)).isFalse();
        assertThat(UpdateService.isVersionNewer("v0.29.1", UpdateService.CURRENT_VERSION)).isFalse();
        assertThat(UpdateService.isVersionNewer("v0.34.1", UpdateService.CURRENT_VERSION)).isTrue();
        assertThat(UpdateService.isVersionNewer("v0.35.0", UpdateService.CURRENT_VERSION)).isTrue();
        assertThat(UpdateService.isVersionNewer("v1.0.0", UpdateService.CURRENT_VERSION)).isTrue();
    }
}

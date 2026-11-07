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
        assertThat(UpdateService.CURRENT_VERSION).isEqualTo("1.7.0");
        assertThat(UpdateService.GITHUB_REPO).isEqualTo("exitishere42/JDMX");
        assertThat(UpdateService.API_URL).isEqualTo("https://api.github.com/repos/exitishere42/JDMX/releases/latest");
        assertThat(UpdateService.REPO_URL).isEqualTo("https://github.com/exitishere42/JDMX");
    }
}

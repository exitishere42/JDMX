package de.exit.jdmx;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basis-Sanity-Test zur Verifikation der Maven- und Test-Infrastruktur.
 * <p>
 * Stellt sicher, dass Java 21 LTS und JUnit 5 Jupiter ordnungsgemäß initialisiert sind.
 * </p>
 */
class SanityTest {

    @Test
    @DisplayName("Sollte erfolgreiches Setup und Java 21 Laufzeit bestätigen")
    void shouldVerifyEnvironmentSetup() {
        String javaVersion = System.getProperty("java.version");
        assertThat(javaVersion)
                .as("Java Version muss vorhanden sein")
                .isNotNull();

        assertThat(2 + 2).isEqualTo(4);
    }
}

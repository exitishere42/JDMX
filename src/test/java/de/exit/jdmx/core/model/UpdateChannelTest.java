package de.exit.jdmx.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für das {@link UpdateChannel} Enum.
 */
class UpdateChannelTest {

    @Test
    @DisplayName("Sollte korrekten Standard-Kanal anhand von Versionsstrings ermitteln")
    void shouldDetermineDefaultForVersion() {
        assertThat(UpdateChannel.defaultForVersion("0.36.0-SNAPSHOT")).isEqualTo(UpdateChannel.SNAPSHOT);
        assertThat(UpdateChannel.defaultForVersion("1.0.0-SNAPSHOT")).isEqualTo(UpdateChannel.SNAPSHOT);
        assertThat(UpdateChannel.defaultForVersion("0.35.0")).isEqualTo(UpdateChannel.RELEASE);
        assertThat(UpdateChannel.defaultForVersion("v0.35.0")).isEqualTo(UpdateChannel.RELEASE);
        assertThat(UpdateChannel.defaultForVersion(null)).isEqualTo(UpdateChannel.RELEASE);
        assertThat(UpdateChannel.defaultForVersion("")).isEqualTo(UpdateChannel.RELEASE);
    }
}

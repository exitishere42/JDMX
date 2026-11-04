package de.exit.jdmx.ui;

import de.exit.jdmx.ui.icon.FlagIcon;
import javafx.application.Platform;
import javafx.scene.Node;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für {@link FlagIcon}.
 */
class FlagIconTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit aktiv
        }
    }

    @Test
    @DisplayName("Sollte Vektor-Flaggen für de, en und ru erzeugen")
    void shouldCreateVectorFlags() {
        Node flagDe = FlagIcon.create("de");
        assertThat(flagDe).isNotNull();

        Node flagEn = FlagIcon.create("en");
        assertThat(flagEn).isNotNull();

        Node flagRu = FlagIcon.create("ru");
        assertThat(flagRu).isNotNull();
    }
}

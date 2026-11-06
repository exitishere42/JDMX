package de.exit.jdmx.ui.component;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Unit-Tests für {@link CustomDropdownButton}.
 */
class CustomDropdownButtonTest {

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Bereits initialisiert
        }
    }

    @Test
    @DisplayName("Sollte Text und Auswahlelemente korrekt initialisieren")
    void shouldInitializeWithItemsAndSelection() {
        CustomDropdownButton dropdown = new CustomDropdownButton("Art-Net (127.0.0.1, Uni 0)", List.of(
                "Art-Net (127.0.0.1, Uni 0)",
                "JMS USB to DMX PRO (COM3)",
                "Kein Output"
        ));

        assertThat(dropdown.getSelectedItem()).isEqualTo("Art-Net (127.0.0.1, Uni 0)");
        assertThat(dropdown.getItems()).containsExactly(
                "Art-Net (127.0.0.1, Uni 0)",
                "JMS USB to DMX PRO (COM3)",
                "Kein Output"
        );
    }

    @Test
    @DisplayName("Sollte Auswahl aktualisieren und Listener benachrichtigen")
    void shouldUpdateSelectionAndNotifyListener() {
        CustomDropdownButton dropdown = new CustomDropdownButton("Kein Input", List.of("Kein Input", "Loopback"));

        final String[] selected = new String[1];
        dropdown.setOnItemSelected(val -> selected[0] = val);

        dropdown.selectItem("Loopback");
        assertThat(dropdown.getSelectedItem()).isEqualTo("Loopback");
        assertThat(selected[0]).isEqualTo("Loopback");
    }

    @Test
    @DisplayName("Sollte maßvolle feste Breitenbeschränkungen besitzen")
    void shouldHaveCompactWidthLimits() {
        CustomDropdownButton dropdown = new CustomDropdownButton("Kein Input", List.of("Kein Input"));

        assertThat(dropdown.getPrefWidth()).isLessThanOrEqualTo(240);
        assertThat(dropdown.getMaxWidth()).isLessThanOrEqualTo(240);
    }
}

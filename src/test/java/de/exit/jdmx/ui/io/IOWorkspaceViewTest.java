package de.exit.jdmx.ui.io;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Unit-Tests für {@link IOWorkspaceView} (Tab 5: I/O Patch).
 * Prüft die Einbindung der UniversePatchTable im oberen Drittel
 * mit reichlich freiem Raum nach unten für zukünftige Erweiterungen.
 */
class IOWorkspaceViewTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit aktiv
        }
    }

    @Test
    @DisplayName("Sollte UniversePatchTable im oberen Bereich bereitstellen ohne fest belegte Matrix")
    void shouldAssembleUniversePatchTableInUpperSection() {
        IOWorkspaceView view = new IOWorkspaceView();

        assertThat(view.getPatchTable()).isNotNull();
        assertThat(view.getChildren()).contains(view.getPatchTable());
    }

    @Test
    @DisplayName("Sollte Platzhalterbereich nach unten für zukünftige Erweiterungen besitzen")
    void shouldHaveExpansionAreaBelow() {
        IOWorkspaceView view = new IOWorkspaceView();

        assertThat(view.getExpansionArea()).isNotNull();
        assertThat(view.getChildren()).contains(view.getExpansionArea());
    }
}

package de.exit.jdmx.ui.stage;

import de.exit.jdmx.ui.icon.LucideIcon;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für die schwebende Pill-Toolbar der 2D-Bühne ({@link StageFloatingToolbar}).
 */
class StageFloatingToolbarTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit aktiv
        }
    }

    @Test
    @DisplayName("Sollte mit Plus-Button als erstem Werkzeug, Canvas-Tools und Einklapp-Button initialisiert werden")
    void shouldInitializeWithButtonsInPillStyle() {
        StageFloatingToolbar toolbar = new StageFloatingToolbar();

        assertThat(toolbar.getStyleClass()).contains("stage-floating-toolbar");
        assertThat(toolbar.getBtnAddFixture()).isNotNull();
        assertThat(toolbar.getBtnAddFixture().getGraphic()).isInstanceOf(LucideIcon.class);
        assertThat(((LucideIcon) toolbar.getBtnAddFixture().getGraphic()).getName()).isEqualTo("plus");

        assertThat(toolbar.getBtnSelect()).isNotNull();
        assertThat(toolbar.getBtnGrid()).isNotNull();
        assertThat(toolbar.getBtnBeams()).isNotNull();
        assertThat(toolbar.getBtnCollapse()).isNotNull();
    }

    @Test
    @DisplayName("Sollte Einklappen und Ausklappen mit Zustandsänderung unterstützen")
    void shouldToggleCollapseState() {
        StageFloatingToolbar toolbar = new StageFloatingToolbar();

        assertThat(toolbar.isCollapsed()).isFalse();

        // 1. Einklappen
        toolbar.collapse();
        assertThat(toolbar.isCollapsed()).isTrue();
        assertThat(toolbar.getStyleClass()).contains("collapsed");

        // 2. Ausklappen
        toolbar.expand();
        assertThat(toolbar.isCollapsed()).isFalse();
        assertThat(toolbar.getStyleClass()).doesNotContain("collapsed");

        // 3. Toggle
        toolbar.toggleCollapse();
        assertThat(toolbar.isCollapsed()).isTrue();
        toolbar.toggleCollapse();
        assertThat(toolbar.isCollapsed()).isFalse();
    }

    @Test
    @DisplayName("Sollte Listener für Add-Fixture und Tools aufrufen")
    void shouldTriggerActionListeners() {
        StageFloatingToolbar toolbar = new StageFloatingToolbar();

        boolean[] addCalled = new boolean[1];
        toolbar.setOnAddFixture(() -> addCalled[0] = true);
        toolbar.getBtnAddFixture().fire();
        assertThat(addCalled[0]).isTrue();

        boolean[] beamsToggled = new boolean[1];
        toolbar.setOnToggleBeams(active -> beamsToggled[0] = active);
        // Da beamsActive initial true ist, schaltet der erste Klick auf false um
        toolbar.getBtnBeams().fire();
        assertThat(beamsToggled[0]).isFalse();
        // Zweiter Klick schaltet wieder auf true
        toolbar.getBtnBeams().fire();
        assertThat(beamsToggled[0]).isTrue();
    }
}

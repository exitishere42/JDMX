package de.exit.jdmx.ui.menu;

import de.exit.jdmx.ui.icon.LucideIcon;
import javafx.application.Platform;
import javafx.scene.control.Button;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für das Hinzufügen-Dropdown-Menü des Project Explorers ({@link ExplorerAddMenu}).
 */
class ExplorerAddMenuTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit aktiv
        }
    }

    @Test
    @DisplayName("Sollte Menüeinträge für Gerät, Gerätegruppe, Chaser, Effekt und Preset bereitstellen")
    void shouldInitializeWithStandardAddItems() {
        ExplorerAddMenu menu = new ExplorerAddMenu();

        Button btnFixture = menu.getItem("fixture");
        assertThat(btnFixture).isNotNull();
        assertThat(btnFixture.getText()).isEqualTo("Gerät");
        assertThat(btnFixture.getGraphic()).isInstanceOf(LucideIcon.class);

        Button btnGroup = menu.getItem("group");
        assertThat(btnGroup).isNotNull();
        assertThat(btnGroup.getText()).isEqualTo("Gerätegruppe");

        Button btnChaser = menu.getItem("chaser");
        assertThat(btnChaser).isNotNull();
        assertThat(btnChaser.getText()).isEqualTo("Chaser");

        Button btnEffect = menu.getItem("effect");
        assertThat(btnEffect).isNotNull();
        assertThat(btnEffect.getText()).isEqualTo("Effekt");

        Button btnPreset = menu.getItem("preset");
        assertThat(btnPreset).isNotNull();
        assertThat(btnPreset.getText()).isEqualTo("Preset");
    }

    @Test
    @DisplayName("Sollte registrierte Aktionen beim Auslösen zuverlässig ausführen")
    void shouldTriggerActionCallbacks() {
        ExplorerAddMenu menu = new ExplorerAddMenu();

        boolean[] fixtureCalled = new boolean[1];
        menu.setOnAction("fixture", () -> fixtureCalled[0] = true);
        menu.triggerAction("fixture");
        assertThat(fixtureCalled[0]).isTrue();

        boolean[] effectCalled = new boolean[1];
        menu.setOnAction("effect", () -> effectCalled[0] = true);
        menu.triggerAction("effect");
        assertThat(effectCalled[0]).isTrue();
    }
}

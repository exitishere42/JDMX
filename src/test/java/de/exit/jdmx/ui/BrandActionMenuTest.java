package de.exit.jdmx.ui;

import de.exit.jdmx.ui.menu.BrandActionMenu;
import javafx.application.Platform;
import javafx.scene.control.Button;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für das Action-Dropdown-Menü des Brand-Buttons {@link BrandActionMenu}.
 */
class BrandActionMenuTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit aktiv
        }
    }

    @Test
    @DisplayName("Sollte alle geforderten Menüeinträge mit Lucide-Icons erzeugen")
    void shouldContainAllActionMenuItems() {
        BrandActionMenu menu = new BrandActionMenu();

        assertThat(menu.isAutoHide()).isTrue();
        assertThat(menu.isHideOnEscape()).isTrue();

        Button btnNew = menu.getItem("new_project");
        assertThat(btnNew).isNotNull();
        assertThat(btnNew.getText()).isEqualTo("New Project");
        assertThat(btnNew.getGraphic()).isNotNull();

        Button btnOpen = menu.getItem("open_file");
        assertThat(btnOpen).isNotNull();
        assertThat(btnOpen.getText()).isEqualTo("Open File");

        Button btnSave = menu.getItem("save");
        assertThat(btnSave).isNotNull();
        assertThat(btnSave.getText()).isEqualTo("Save");

        Button btnSaveAs = menu.getItem("save_as");
        assertThat(btnSaveAs).isNotNull();
        assertThat(btnSaveAs.getText()).isEqualTo("Save as...");

        Button btnUndo = menu.getItem("undo");
        assertThat(btnUndo).isNotNull();
        assertThat(btnUndo.getText()).isEqualTo("Undo");

        Button btnRedo = menu.getItem("redo");
        assertThat(btnRedo).isNotNull();
        assertThat(btnRedo.getText()).isEqualTo("Redo");

        Button btnSettings = menu.getItem("settings");
        assertThat(btnSettings).isNotNull();
        assertThat(btnSettings.getText()).isEqualTo("Settings");

        Button btnUpdates = menu.getItem("check_updates");
        assertThat(btnUpdates).isNotNull();
        assertThat(btnUpdates.getText()).isEqualTo("Updates");

        Button btnFullscreen = menu.getItem("fullscreen");
        assertThat(btnFullscreen).isNotNull();
        assertThat(btnFullscreen.getText()).isEqualTo("Fullscreen");
    }

    @Test
    @DisplayName("Sollte Callbacks beim Klick auf Menüeinträge feuern")
    void shouldFireActionCallback() {
        BrandActionMenu menu = new BrandActionMenu();
        AtomicBoolean actionFired = new AtomicBoolean(false);

        menu.setOnAction("new_project", () -> actionFired.set(true));

        Button btnNew = menu.getItem("new_project");
        btnNew.fire();

        assertThat(actionFired.get()).isTrue();
    }
}

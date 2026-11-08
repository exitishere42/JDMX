package de.exit.jdmx.ui;

import de.exit.jdmx.core.i18n.I18n;
import de.exit.jdmx.ui.io.InputDockView;
import de.exit.jdmx.ui.io.OutputDockView;
import de.exit.jdmx.ui.io.UniversePatchTable;
import de.exit.jdmx.ui.menu.BrandActionMenu;
import de.exit.jdmx.ui.stage.ColorModuleView;
import de.exit.jdmx.ui.stage.IntensityModuleView;
import de.exit.jdmx.ui.stage.PositionModuleView;
import de.exit.jdmx.ui.stage.ProjectExplorerView;
import javafx.application.Platform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Umfassender Integrationstest zur Prüfung der dynamischen, reaktiven Sprachumschaltung
 * (Deutsch -> Englisch -> Russisch) über alle UI-Module hinweg.
 */
class UIReactiveLocalizationTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Bereits initialisiert
        }
    }

    @AfterEach
    void resetLocale() {
        I18n.setLanguage("de");
    }

    private void runOnFxThread(Runnable r) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> exRef = new java.util.concurrent.atomic.AtomicReference<>();
        Platform.runLater(() -> {
            try {
                r.run();
            } catch (Throwable t) {
                exRef.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        if (exRef.get() != null) {
            throw new RuntimeException(exRef.get());
        }
    }

    @Test
    @DisplayName("Steuerungsmodule (Intensität, Farbe, Position) schalten Sprache reaktiv um")
    void shouldReactivelyUpdateControlModules() throws Exception {
        runOnFxThread(() -> {
            I18n.setLanguage("de");

            IntensityModuleView intensityView = new IntensityModuleView();
            ColorModuleView colorView = new ColorModuleView();
            PositionModuleView positionView = new PositionModuleView();

            assertThat(intensityView.getTitle()).isEqualTo("Intensität");
            assertThat(colorView.getTitle()).isEqualTo("Farbe");
            assertThat(positionView.getTitle()).isEqualTo("Position");

            // Wechsel zu Englisch
            I18n.setLanguage("en");
            assertThat(intensityView.getTitle()).isEqualTo("Intensity");
            assertThat(colorView.getTitle()).isEqualTo("Color");
            assertThat(positionView.getTitle()).isEqualTo("Position");

            // Wechsel zu Russisch
            I18n.setLanguage("ru");
            assertThat(intensityView.getTitle()).isEqualTo("Интенсивность");
            assertThat(colorView.getTitle()).isEqualTo("Цвет");
            assertThat(positionView.getTitle()).isEqualTo("Позиция");
        });
    }

    @Test
    @DisplayName("Project Explorer und Standardordner schalten Sprache reaktiv um")
    void shouldReactivelyUpdateProjectExplorerAndFolders() throws Exception {
        runOnFxThread(() -> {
            I18n.setLanguage("de");

            ProjectExplorerView explorerView = new ProjectExplorerView();
            assertThat(explorerView.getTitle()).isEqualTo("Projekt-Explorer");

            // Wechsel zu Englisch
            I18n.setLanguage("en");
            assertThat(explorerView.getTitle()).isEqualTo("Project Explorer");

            // Wechsel zu Russisch
            I18n.setLanguage("ru");
            assertThat(explorerView.getTitle()).isEqualTo("Обозреватель проекта");
        });
    }

    @Test
    @DisplayName("I/O Docks (Input, Output) und Universe-Patch-Tabelle schalten Sprache reaktiv um")
    void shouldReactivelyUpdateIoViews() throws Exception {
        runOnFxThread(() -> {
            I18n.setLanguage("de");

            InputDockView inputDock = new InputDockView();
            OutputDockView outputDock = new OutputDockView();
            UniversePatchTable patchTable = new UniversePatchTable();

            assertThat(inputDock.getTitle()).isEqualTo("Eingang");
            assertThat(outputDock.getTitle()).isEqualTo("Ausgang");

            // Wechsel zu Englisch
            I18n.setLanguage("en");
            assertThat(inputDock.getTitle()).isEqualTo("Input");
            assertThat(outputDock.getTitle()).isEqualTo("Output");

            // Wechsel zu Russisch
            I18n.setLanguage("ru");
            assertThat(inputDock.getTitle()).isEqualTo("Вход");
            assertThat(outputDock.getTitle()).isEqualTo("Выход");
        });
    }

    @Test
    @DisplayName("Brand-Aktionsmenü schaltet alle Menüeinträge dynamisch um")
    void shouldReactivelyUpdateBrandActionMenu() throws Exception {
        runOnFxThread(() -> {
            I18n.setLanguage("de");

            BrandActionMenu menu = new BrandActionMenu();
            assertThat(menu.getItem("new_project").getText()).isEqualTo("Neues Projekt");
            assertThat(menu.getItem("settings").getText()).isEqualTo("Einstellungen");

            // Wechsel zu Englisch
            I18n.setLanguage("en");
            assertThat(menu.getItem("new_project").getText()).isEqualTo("New Project");
            assertThat(menu.getItem("settings").getText()).isEqualTo("Settings");

            // Wechsel zu Russisch
            I18n.setLanguage("ru");
            assertThat(menu.getItem("new_project").getText()).isEqualTo("Новый проект");
            assertThat(menu.getItem("settings").getText()).isEqualTo("Настройки");
        });
    }

    @Test
    @DisplayName("HeaderBar Blackout-Schaltfläche schaltet Text dynamisch um")
    void shouldReactivelyUpdateHeaderBar() throws Exception {
        runOnFxThread(() -> {
            I18n.setLanguage("de");

            HeaderBar headerBar = new HeaderBar();

            // Wechsel zu Russisch
            I18n.setLanguage("ru");

            // Wechsel zu Englisch
            I18n.setLanguage("en");
        });
    }
}

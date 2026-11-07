package de.exit.jdmx.ui.dialog;

import de.exit.jdmx.core.model.UniverseConfig;
import de.exit.jdmx.ui.MainWindow;
import javafx.application.Platform;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Unit-Tests für {@link UniverseConfigDialog}.
 * Prüft den modalen In-App Dialog (Fenster im Fenster) für Universums- & Art-Net Einstellungen.
 */
class UniverseConfigDialogTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit aktiv
        }
    }

    @Test
    @DisplayName("Sollte Dialog-Karte mit Universums-Konfiguration korrekt aufbauen")
    void shouldBuildDialogCardFromConfig() {
        UniverseConfig config = UniverseConfig.defaultFor(0);
        UniverseConfigDialog dialog = new UniverseConfigDialog(null, config, updated -> {});

        VBox view = (VBox) dialog.getView();
        assertThat(view).isNotNull();
        assertThat(view.getStyleClass()).contains("settings-dialog-card");
        assertThat(dialog.getTitleLabel().getText()).contains("Universum 1");
        assertThat(dialog.getTxtIp().getText()).isEqualTo("127.0.0.1");
        assertThat(dialog.getTxtPort().getText()).isEqualTo("6454");
    }

    @Test
    @DisplayName("Sollte geänderte Art-Net Werte beim Speichern an den Callback übergeben")
    void shouldSaveUpdatedConfigOnSaveAction() {
        MainWindow mainWindow = new MainWindow();
        UniverseConfig config = UniverseConfig.defaultFor(0);
        AtomicReference<UniverseConfig> savedConfig = new AtomicReference<>();

        UniverseConfigDialog dialog = new UniverseConfigDialog(mainWindow, config, savedConfig::set);
        mainWindow.showOverlay(dialog.getView());
        assertThat(mainWindow.isOverlayShowing()).isTrue();

        // Werte anpassen
        dialog.getTxtIp().setText("192.168.1.50");
        dialog.getTxtPort().setText("6455");
        dialog.getTxtSubnet().setText("1");
        dialog.getTxtUniverse().setText("2");
        dialog.getChkBroadcast().setSelected(true);

        dialog.getBtnSave().fire();

        assertThat(savedConfig.get()).isNotNull();
        assertThat(savedConfig.get().artNetIp()).isEqualTo("192.168.1.50");
        assertThat(savedConfig.get().artNetPort()).isEqualTo(6455);
        assertThat(savedConfig.get().artNetSubnet()).isEqualTo(1);
        assertThat(savedConfig.get().artNetUniverse()).isEqualTo(2);
        assertThat(savedConfig.get().isBroadcast()).isTrue();
        assertThat(mainWindow.isOverlayShowing()).isFalse();
    }

    @Test
    @DisplayName("Sollte Dialog bei Abbrechen ohne Speichern schließen")
    void shouldCloseOverlayOnCancel() {
        MainWindow mainWindow = new MainWindow();
        UniverseConfig config = UniverseConfig.defaultFor(0);
        AtomicReference<UniverseConfig> savedConfig = new AtomicReference<>();

        UniverseConfigDialog dialog = new UniverseConfigDialog(mainWindow, config, savedConfig::set);
        mainWindow.showOverlay(dialog.getView());
        assertThat(mainWindow.isOverlayShowing()).isTrue();

        dialog.getBtnCancel().fire();

        assertThat(savedConfig.get()).isNull();
        assertThat(mainWindow.isOverlayShowing()).isFalse();
    }

    @Test
    @DisplayName("Sollte grünen Haken für Speichern und rotes X für Schließen besitzen")
    void shouldHaveGreenSaveCheckmarkAndRedCloseButton() {
        UniverseConfig config = UniverseConfig.defaultFor(0);
        UniverseConfigDialog dialog = new UniverseConfigDialog(null, config, updated -> {});

        assertThat(dialog.getBtnSave().getGraphic()).isInstanceOf(de.exit.jdmx.ui.icon.LucideIcon.class);
        de.exit.jdmx.ui.icon.LucideIcon saveIcon = (de.exit.jdmx.ui.icon.LucideIcon) dialog.getBtnSave().getGraphic();
        assertThat(saveIcon.getName()).isEqualTo("check");

        assertThat(dialog.getBtnClose().getGraphic()).isInstanceOf(de.exit.jdmx.ui.icon.LucideIcon.class);
        de.exit.jdmx.ui.icon.LucideIcon closeIcon = (de.exit.jdmx.ui.icon.LucideIcon) dialog.getBtnClose().getGraphic();
        assertThat(closeIcon.getName()).isEqualTo("x");
    }
}

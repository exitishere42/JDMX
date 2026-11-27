package de.exit.jdmx.ui.timeline;

import de.exit.jdmx.ui.icon.LucideIcon;
import javafx.application.Platform;
import javafx.scene.control.Button;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Unit-Tests für das benutzerdefinierte Timeline-Kontextmenü {@link TimelineContextMenu}.
 * <p>
 * Überprüft das Vorhandensein des Test-Buttons mit Labor-Icon ("flask")
 * sowie das Schließen und Ausführen von Aktionen.
 * </p>
 */
class TimelineContextMenuTest {

    @BeforeAll
    static void initJavaFx() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(() -> {
                Platform.setImplicitExit(false);
                latch.countDown();
            });
        } catch (IllegalStateException e) {
            Platform.setImplicitExit(false);
            latch.countDown();
        }
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("Sollte Test-Button mit Labor-Icon (flask) initialisieren und Klickaktion ausführen")
    void shouldInitializeTestButtonWithLabIconAndTriggerAction() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                TimelineContextMenu menu = new TimelineContextMenu();

                Button btnTest = menu.getItemButton("test");
                assertThat(btnTest).isNotNull();
                assertThat(btnTest.getText()).isEqualTo("Test");
                assertThat(btnTest.getStyleClass()).contains("action-menu-item");

                assertThat(btnTest.getGraphic()).isInstanceOf(LucideIcon.class);
                LucideIcon icon = (LucideIcon) btnTest.getGraphic();
                assertThat(icon.getName()).isEqualTo("flask");

                AtomicBoolean actionExecuted = new AtomicBoolean(false);
                menu.setOnAction("test", () -> actionExecuted.set(true));

                btnTest.fire();
                assertThat(actionExecuted.get()).isTrue();
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("Sollte Farbe-Button mit Palette- und Chevron-Icon initialisieren und Flyout bereitstellen")
    void shouldInitializeColorButtonWithPaletteAndFlyout() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                TimelineContextMenu menu = new TimelineContextMenu();

                Button btnColor = menu.getItemButton("color");
                assertThat(btnColor).isNotNull();
                assertThat(btnColor.getText()).isEqualTo("Farbe");
                assertThat(btnColor.getStyleClass()).contains("action-menu-item");

                // Icon-Prüfung: Pfeil nach rechts (chevron-right) statt Platzhalter-Kreis
                assertThat(btnColor.getGraphic()).isNotNull();
                assertThat(btnColor.getGraphic()).isInstanceOf(javafx.scene.layout.HBox.class);
                javafx.scene.layout.HBox box = (javafx.scene.layout.HBox) btnColor.getGraphic();
                LucideIcon chevron = (LucideIcon) box.getChildren().getLast();
                assertThat(chevron.getName()).isEqualTo("chevron-right");
                assertThat(LucideIcon.getSvgPathData("chevron-right")).contains("m9 18");
                assertThat(LucideIcon.getSvgPathData("chevron-right")).doesNotContain("M12 2a10 10");

                assertThat(menu.getColorFlyout()).isNotNull();
                assertThat(menu.getColorFlyout().getColorSwatches()).hasSize(6);

                AtomicReference<String> pickedColor = new AtomicReference<>();
                menu.setOnColorSelected(pickedColor::set);

                // Klick auf das zweite Farbfeld im Flyout (Cyan)
                menu.getColorFlyout().getColorSwatches().get(1).fire();
                assertThat(pickedColor.get()).isEqualTo("#38BDF8");
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("Sollte TimelineContextMenu in TimelineGridView bereitstellen und linke obere Ecke am Mauszeiger positionieren")
    void shouldProvideTimelineContextMenuInGridViewAndPositionAtCursor() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                de.exit.jdmx.service.TimelineService service = new de.exit.jdmx.service.TimelineService();
                TimelineGridView gridView = new TimelineGridView(service);
                assertThat(gridView.getTimelineContextMenu()).isNotNull();
                assertThat(gridView.getTimelineContextMenu().getItemButton("test")).isNotNull();

                javafx.stage.Stage stage = new javafx.stage.Stage();
                javafx.scene.Scene scene = new javafx.scene.Scene(gridView, 400, 300);
                scene.getStylesheets().add(getClass().getResource("/de/exit/jdmx/styles/studio-dark.css").toExternalForm());
                stage.setScene(scene);
                stage.show();

                TimelineContextMenu menu = gridView.getTimelineContextMenu();
                double mouseX = 150.0;
                double mouseY = 200.0;
                menu.showMenu(stage, mouseX, mouseY);

                // Sichtbare Randkoordinaten des Hauptmenüs (ohne Dropshadow-Erweiterung)
                javafx.geometry.Bounds visibleRootBounds = menu.getMenuContainer().localToScreen(
                        menu.getMenuContainer().getLayoutBounds());

                Button btnColor = menu.getItemButton("color");
                javafx.geometry.Bounds visibleBtnBounds = btnColor.localToScreen(btnColor.getLayoutBounds());
                menu.getColorFlyout().showFlyout(stage, visibleRootBounds.getMaxX(), visibleBtnBounds.getMinY());

                // Sichtbare Randkoordinaten des Flyout-Untermenüs (ohne Dropshadow-Erweiterung)
                javafx.geometry.Bounds visibleFlyoutBounds = menu.getColorFlyout().getMenuContainer().localToScreen(
                        menu.getColorFlyout().getMenuContainer().getLayoutBounds());

                double visibleGap = visibleFlyoutBounds.getMinX() - visibleRootBounds.getMaxX();
                // Exakt 2px sichtbarer Studio-Abstand zwischen den Menü-Außenrahmen
                assertThat(visibleGap).isCloseTo(2.0, org.assertj.core.data.Offset.offset(0.5));
                // Oberkante des Flyouts schließt bündig mit der Oberkante des Farbe-Buttons ab
                assertThat(visibleFlyoutBounds.getMinY()).isCloseTo(visibleBtnBounds.getMinY(), org.assertj.core.data.Offset.offset(0.5));
                menu.getColorFlyout().hide();

                double visibleLeftCornerX = menu.getX() + 15.0;
                double visibleTopCornerY = menu.getY() + 7.0;
                assertThat(visibleLeftCornerX).isEqualTo(mouseX);
                assertThat(visibleTopCornerY).isEqualTo(mouseY);

                // Sicherstellen dass die Scene und der Root des Popups transparent sind (kein eckiger Kasten)
                assertThat(menu.getScene()).isNotNull();
                assertThat(menu.getScene().getFill()).isEqualTo(javafx.scene.paint.Color.TRANSPARENT);
                assertThat(menu.getScene().getRoot().getStyle()).contains("-fx-background-color: transparent");

                menu.hide();
                stage.close();
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }
}

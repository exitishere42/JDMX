package de.exit.jdmx.ui.dialog;

import de.exit.jdmx.core.model.ChannelDefinition;
import de.exit.jdmx.core.model.ChannelType;
import de.exit.jdmx.core.model.DmxMode;
import de.exit.jdmx.core.model.ExplorerItem;
import de.exit.jdmx.core.model.ExplorerItemType;
import de.exit.jdmx.core.model.FixtureDefinition;
import de.exit.jdmx.core.model.FixtureInstance;
import de.exit.jdmx.service.ProjectExplorerService;
import de.exit.jdmx.ui.stage.StageCanvasView;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für den {@link RenameItemDialog}.
 */
class RenameItemDialogTest {

    @BeforeAll
    static void initJavaFx() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            latch.countDown();
        }
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("Sollte Dialog initial mit aktuellem Item-Namen füllen und Speichern ausführen")
    void shouldInitializeWithCurrentNameAndSave() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                ProjectExplorerService service = new ProjectExplorerService();
                StageCanvasView canvas = new StageCanvasView(service);

                ChannelDefinition ch = new ChannelDefinition(1, "Dimmer", ChannelType.DIMMER, 0, false);
                DmxMode mode = new DmxMode("1CH", List.of(ch));
                FixtureDefinition def = new FixtureDefinition("generic-spot", "Generic", "Spot", ExplorerItemType.FIXTURE, List.of(mode));
                FixtureInstance instance = new FixtureInstance("f1", "Spot 1", def, mode, 1, 1, 100, 100);
                ExplorerItem item = new ExplorerItem("item-1", "Spot 1", ExplorerItemType.FIXTURE, false, "lamp", Color.BLUE, instance);

                service.addItemToFolder("folder-devices", item);
                canvas.placeFixture(item, 100, 100);

                RenameItemDialog dialog = new RenameItemDialog(null, item, service, canvas);

                assertThat(dialog.getView()).isNotNull();
                assertThat(dialog.getTxtName().getText()).isEqualTo("Spot 1");

                // Neuen Namen eingeben
                dialog.getTxtName().setText("Moving Head A");
                dialog.getBtnSave().fire();

                // Item und FixtureInstance müssen synchron umbenannt worden sein
                assertThat(item.getName()).isEqualTo("Moving Head A");
                assertThat(instance.getName()).isEqualTo("Moving Head A");

                // Badge auf StageCanvas muss aktualisiert sein
                assertThat(canvas.getPlacedFixtures().get(0).getNameBadge().getText()).contains("Moving Head A");
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("Sollte leeren Namen beim Speichern ablehnen und roten Rahmen setzen")
    void shouldRejectBlankName() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                ProjectExplorerService service = new ProjectExplorerService();
                ExplorerItem item = new ExplorerItem("item-1", "Spot 1", ExplorerItemType.FIXTURE, false, "lamp", Color.BLUE);
                service.addItemToFolder("folder-devices", item);

                RenameItemDialog dialog = new RenameItemDialog(null, item, service, null);

                dialog.getTxtName().setText("   ");
                dialog.getBtnSave().fire();

                // Name darf nicht geändert werden
                assertThat(item.getName()).isEqualTo("Spot 1");
                assertThat(dialog.getTxtName().getStyle()).contains("#EF4444");
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }
}

package de.exit.jdmx.ui.desk;

import de.exit.jdmx.core.model.*;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Unit-Tests für {@link DeskFixtureListView}.
 * Prüft Fixture-Auflistung, Selektion, Auto-Scroll-Callback und Quick-Actions (Locate, Full, Zero).
 */
class DeskFixtureListViewTest {

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

    private ExplorerItem createTestFixture(String id, String name, int universe, int startAddress) {
        List<ChannelDefinition> channels = List.of(
                new ChannelDefinition(1, "Pan", ChannelType.PAN, 128, false),
                new ChannelDefinition(2, "Tilt", ChannelType.TILT, 128, false),
                new ChannelDefinition(3, "Dimmer", ChannelType.DIMMER, 0, false),
                new ChannelDefinition(4, "Red", ChannelType.RED, 255, false)
        );
        DmxMode mode = new DmxMode("4CH", channels);
        FixtureDefinition def = new FixtureDefinition("def-" + id, "Generic", "Spot", ExplorerItemType.FIXTURE, List.of(mode));
        FixtureInstance instance = new FixtureInstance(id, name, def, mode, universe, startAddress, 0, 0);
        return new ExplorerItem(id, name, ExplorerItemType.FIXTURE, false, "box", Color.CYAN, instance);
    }

    @Test
    @DisplayName("Sollte gepatchte Geräte nach Universum filtern und im UI anzeigen")
    void shouldFilterAndDisplayFixturesByUniverse() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                DeskFixtureListView view = new DeskFixtureListView();

                ExplorerItem fix1 = createTestFixture("f1", "Moving Head 1", 1, 1);
                ExplorerItem fix2 = createTestFixture("f2", "Moving Head 2", 1, 17);
                ExplorerItem fixOtherUni = createTestFixture("f3", "Floor Spot", 2, 1);

                // Universum 1 setzen -> fix1 und fix2 sichtbar, fixOtherUni ausgefiltert
                view.setFixtures(List.of(fix1, fix2, fixOtherUni), 1);

                assertThat(view.getDisplayedFixturesCount()).isEqualTo(2);
                assertThat(view.getSelectedFixture()).isNull();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        if (error.get() != null) throw error.get();
    }

    @Test
    @DisplayName("Sollte bei Selektion eines Fixtures Auto-Scroll Callback mit Startadresse auslösen")
    void shouldTriggerAutoScrollOnFixtureSelection() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                DeskFixtureListView view = new DeskFixtureListView();
                ExplorerItem fix = createTestFixture("f1", "Spot Links", 1, 25);
                view.setFixtures(List.of(fix), 1);

                AtomicInteger scrolledAddress = new AtomicInteger(-1);
                view.setOnJumpToChannel(scrolledAddress::set);

                // Fixture selektieren
                view.selectFixture(fix.getFixtureInstance());

                assertThat(view.getSelectedFixture()).isEqualTo(fix.getFixtureInstance());
                assertThat(scrolledAddress.get()).isEqualTo(25);
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        if (error.get() != null) throw error.get();
    }

    @Test
    @DisplayName("Sollte Quick-Actions (Locate, Full, Zero) korrekt per Callback weiterleiten")
    void shouldTriggerQuickActionsCorrectly() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                DeskFixtureListView view = new DeskFixtureListView();
                ExplorerItem fix = createTestFixture("f1", "Spot Links", 1, 1);
                view.setFixtures(List.of(fix), 1);
                view.selectFixture(fix.getFixtureInstance());

                AtomicBoolean locateTriggered = new AtomicBoolean(false);
                AtomicBoolean fullTriggered = new AtomicBoolean(false);
                AtomicBoolean zeroTriggered = new AtomicBoolean(false);

                view.setOnLocateFixture(f -> locateTriggered.set(true));
                view.setOnFullFixture(f -> fullTriggered.set(true));
                view.setOnZeroFixture(f -> zeroTriggered.set(true));

                view.triggerLocate();
                assertThat(locateTriggered.get()).isTrue();

                view.triggerFull();
                assertThat(fullTriggered.get()).isTrue();

                view.triggerZero();
                assertThat(zeroTriggered.get()).isTrue();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        if (error.get() != null) throw error.get();
    }
}

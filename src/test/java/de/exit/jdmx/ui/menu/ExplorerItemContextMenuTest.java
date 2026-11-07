package de.exit.jdmx.ui.menu;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für das Rechtsklick-Kontextmenü {@link ExplorerItemContextMenu}.
 */
class ExplorerItemContextMenuTest {

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
    @DisplayName("Sollte Umbenennen- und Entfernen-Aktionen besitzen und Klicks auslösen")
    void shouldTriggerRenameAndDeleteActions() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                ExplorerItemContextMenu menu = new ExplorerItemContextMenu();

                assertThat(menu.getItemButton("rename")).isNotNull();
                assertThat(menu.getItemButton("delete")).isNotNull();

                AtomicBoolean renameTriggered = new AtomicBoolean(false);
                AtomicBoolean deleteTriggered = new AtomicBoolean(false);

                menu.setOnRename(() -> renameTriggered.set(true));
                menu.setOnDelete(() -> deleteTriggered.set(true));

                // 1. Klick auf Umbenennen
                menu.getItemButton("rename").fire();
                assertThat(renameTriggered.get()).isTrue();
                assertThat(deleteTriggered.get()).isFalse();

                // 2. Klick auf Entfernen
                menu.getItemButton("delete").fire();
                assertThat(deleteTriggered.get()).isTrue();
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }
}

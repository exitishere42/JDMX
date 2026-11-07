package de.exit.jdmx.ui.desk;

import de.exit.jdmx.core.i18n.I18n;
import de.exit.jdmx.core.model.*;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Unit-Tests für {@link DeskFixtureBarView}.
 * Verifiziert die schlanke Fixture-Leiste im unteren Bereich des LightDesk:
 * - Einzeilige Formatierung: Brand Model | Start-End | N Kanäle
 * - Gekürzte vertikale Trennstriche
 * - Klick zum Fader-Sprung
 * - I18n-Reaktivität
 */
class DeskFixtureBarViewTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Bereits initialisiert
        }
    }

    @BeforeEach
    void setUp() {
        I18n.setLanguage("de");
    }

    @Test
    @DisplayName("Sollte leere Leiste mit Überschrift erzeugen, wenn keine Fixtures gepatcht sind")
    void shouldCreateEmptyBarWithTitle() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<DeskFixtureBarView> viewRef = new AtomicReference<>();

        Platform.runLater(() -> {
            DeskFixtureBarView bar = new DeskFixtureBarView();
            bar.setFixtures(List.of(), 1);
            viewRef.set(bar);
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        DeskFixtureBarView bar = viewRef.get();
        assertThat(bar.getTitleLabel().getText()).isEqualTo("Fixture-Liste");
        assertThat(bar.getFixtureRowCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Sollte gepatchte Fixtures korrekt formatieren mit Brand, Adressen und Kanälen")
    void shouldFormatPatchedFixturesCorrectly() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<DeskFixtureBarView> viewRef = new AtomicReference<>();

        Platform.runLater(() -> {
            DeskFixtureBarView bar = new DeskFixtureBarView();

            DmxMode mode13Ch = new DmxMode("13 Channel", List.of(
                    new ChannelDefinition(1, "Pan", ChannelType.PAN, 128, false),
                    new ChannelDefinition(2, "Pan fine", ChannelType.PAN_FINE, 0, true),
                    new ChannelDefinition(3, "Tilt", ChannelType.TILT, 128, false),
                    new ChannelDefinition(4, "Tilt fine", ChannelType.TILT_FINE, 0, true),
                    new ChannelDefinition(5, "Speed", ChannelType.SPEED, 0, false),
                    new ChannelDefinition(6, "Dimmer", ChannelType.DIMMER, 0, false),
                    new ChannelDefinition(7, "Strobe", ChannelType.SHUTTER, 0, false),
                    new ChannelDefinition(8, "Red", ChannelType.RED, 255, false),
                    new ChannelDefinition(9, "Green", ChannelType.GREEN, 255, false),
                    new ChannelDefinition(10, "Blue", ChannelType.BLUE, 255, false),
                    new ChannelDefinition(11, "White", ChannelType.WHITE, 255, false),
                    new ChannelDefinition(12, "Mode", ChannelType.EFFECT, 0, false),
                    new ChannelDefinition(13, "Reset", ChannelType.MAINTENANCE, 0, false)
            ));

            FixtureDefinition def = new FixtureDefinition("uking-zq-02042", "UKing", "ZQ-02042",
                    ExplorerItemType.FIXTURE, List.of(mode13Ch));
            FixtureInstance instance = new FixtureInstance("inst-1", "Moving Head", def, mode13Ch, 1, 1, 0, 0);

            ExplorerItem item = new ExplorerItem("item-1", "ZQ-02042", ExplorerItemType.FIXTURE, false, "lamp", Color.CYAN, instance);

            bar.setFixtures(List.of(item), 1);
            viewRef.set(bar);
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        DeskFixtureBarView bar = viewRef.get();
        assertThat(bar.getFixtureRowCount()).isEqualTo(1);

        HBox row = bar.getFixtureRow(0);
        assertThat(row).isNotNull();

        // Zeile enthält Name (UKing ZQ-02042), Start-End (1-13) und Kanäle (13 Kanäle)
        String rowText = extractTextFromRow(row);
        assertThat(rowText).contains("UKing");
        assertThat(rowText).contains("ZQ-02042");
        assertThat(rowText).contains("1–13");
        assertThat(rowText).contains("13 Kanäle");
    }

    @Test
    @DisplayName("Sollte beim Klick auf eine Zeile onJumpToChannel mit der Startadresse auslösen")
    void shouldTriggerJumpToChannelOnRowClick() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger jumpedChannel = new AtomicInteger(-1);

        Platform.runLater(() -> {
            DeskFixtureBarView bar = new DeskFixtureBarView();
            bar.setOnJumpToChannel(jumpedChannel::set);

            DmxMode mode4Ch = new DmxMode("4 Channel", List.of(
                    new ChannelDefinition(1, "R", ChannelType.RED, 255, false),
                    new ChannelDefinition(2, "G", ChannelType.GREEN, 255, false),
                    new ChannelDefinition(3, "B", ChannelType.BLUE, 255, false),
                    new ChannelDefinition(4, "W", ChannelType.WHITE, 255, false)
            ));
            FixtureDefinition def = new FixtureDefinition("generic-rgbw", "Generic", "RGBW Par",
                    ExplorerItemType.FIXTURE, List.of(mode4Ch));
            FixtureInstance instance = new FixtureInstance("inst-2", "PAR 1", def, mode4Ch, 1, 14, 0, 0);
            ExplorerItem item = new ExplorerItem("item-2", "PAR 1", ExplorerItemType.FIXTURE, false, "lamp", Color.MAGENTA, instance);

            bar.setFixtures(List.of(item), 1);
            HBox row = bar.getFixtureRow(0);

            // Simulierter Klick auf die Zeile
            row.getOnMouseClicked().handle(null);
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(jumpedChannel.get()).isEqualTo(14);
    }

    @Test
    @DisplayName("Sollte Text bei Sprachwechsel auf Englisch aktualisieren")
    void shouldUpdateTextWhenLanguageChanges() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<DeskFixtureBarView> viewRef = new AtomicReference<>();

        Platform.runLater(() -> {
            DeskFixtureBarView bar = new DeskFixtureBarView();
            I18n.setLanguage("en");
            viewRef.set(bar);
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        DeskFixtureBarView bar = viewRef.get();
        assertThat(bar.getTitleLabel().getText()).isEqualTo("Fixture List");
    }

    private String extractTextFromRow(HBox row) {
        StringBuilder sb = new StringBuilder();
        for (var child : row.getChildren()) {
            if (child instanceof Label lbl) {
                sb.append(lbl.getText()).append(" ");
            }
        }
        return sb.toString();
    }
}

package de.exit.jdmx.ui.desk;

import de.exit.jdmx.core.i18n.I18n;
import de.exit.jdmx.core.model.ChannelType;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für die Hover-Tooltips und Hit-Detection der Fader-Icons in {@link CanvasDeskView}.
 */
class CanvasDeskViewTooltipTest {

    private CanvasDeskView deskView;

    @BeforeAll
    static void initJavaFX() throws Exception {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Bereits initialisiert
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        I18n.setLanguage("de");
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            deskView = new CanvasDeskView();
            deskView.setSimulatedDimensions(800, 300);
            latch.countDown();
        });
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @AfterEach
    void tearDown() {
        I18n.setLanguage("de");
    }

    @Test
    @DisplayName("Hit-Detection: Liefert nur true in der Icon-Headerzone (Y 4..33px) bei gepatchtem Kanal")
    void testHitDetectionIconZone() {
        // Kanal 1 patchen
        deskView.assignFixtureChannel(1, "Varytec ZQ-02040", ChannelType.PAN, Color.CYAN, 1, "Pan");

        // Kanalbreite bei 800px / 32 = 25px. Mitte Kanal 1: x = 12.5px
        double ch1X = 12.5;

        // In Icon-Zone
        assertThat(deskView.isOverIcon(ch1X, 17.0)).isTrue();
        assertThat(deskView.isOverIcon(ch1X, 5.0)).isTrue();
        assertThat(deskView.isOverIcon(ch1X, 32.0)).isTrue();

        // Außerhalb Icon-Zone (zu weit oben / zu weit unten im Track)
        assertThat(deskView.isOverIcon(ch1X, 2.0)).isFalse();
        assertThat(deskView.isOverIcon(ch1X, 35.0)).isFalse();
        assertThat(deskView.isOverIcon(ch1X, 150.0)).isFalse();

        // Ungepatchter Kanal (z. B. Kanal 2)
        double ch2X = 37.5;
        assertThat(deskView.isOverIcon(ch2X, 17.0)).isFalse();
    }

    @Test
    @DisplayName("Formatierung Tooltip-Text (Deutsch)")
    void testFormatTooltipTextGerman() {
        deskView.assignFixtureChannel(1, "Varytec ZQ-02040", ChannelType.PAN, Color.CYAN, 1, "Pan");
        deskView.setChannelValue(1, 128);

        DeskChannelData data = deskView.getChannelData(1);
        String tooltip = deskView.buildIconTooltipText(data);

        assertThat(tooltip).contains("Pan");
        assertThat(tooltip).contains("Varytec ZQ-02040");
        assertThat(tooltip).contains("DMX 1");
        assertThat(tooltip).contains("128");
        assertThat(tooltip).contains("50%");
    }

    @Test
    @DisplayName("Formatierung Tooltip-Text (Englisch & Russisch)")
    void testFormatTooltipTextEnglishAndRussian() {
        deskView.assignFixtureChannel(1, "Moving Head 1", ChannelType.TILT, Color.CYAN, 2, "Tilt");
        deskView.setChannelValue(1, 255);
        DeskChannelData data = deskView.getChannelData(1);

        // Englisch
        I18n.setLanguage("en");
        String tooltipEn = deskView.buildIconTooltipText(data);
        assertThat(tooltipEn).contains("Tilt");
        assertThat(tooltipEn).contains("Moving Head 1");
        assertThat(tooltipEn).contains("Value: 255 / 255 (100%)");

        // Russisch
        I18n.setLanguage("ru");
        String tooltipRu = deskView.buildIconTooltipText(data);
        assertThat(tooltipRu).contains("Tilt");
        assertThat(tooltipRu).contains("Moving Head 1");
        assertThat(tooltipRu).contains("Значение: 255 / 255 (100%)");
    }

    @Test
    @DisplayName("Alle ChannelType-Werte besitzen eine gültige Übersetzung")
    void testAllChannelTypesLocalized() {
        for (ChannelType type : ChannelType.values()) {
            I18n.setLanguage("de");
            String de = deskView.getLocalizedChannelTypeName(type);
            assertThat(de).as("DE translation for " + type).isNotBlank();

            I18n.setLanguage("en");
            String en = deskView.getLocalizedChannelTypeName(type);
            assertThat(en).as("EN translation for " + type).isNotBlank();

            I18n.setLanguage("ru");
            String ru = deskView.getLocalizedChannelTypeName(type);
            assertThat(ru).as("RU translation for " + type).isNotBlank();
        }
    }

    @Test
    @DisplayName("Tooltip wird ausgeblendet wenn Maus den Bereich verlässt")
    void testTooltipHidesCorrectly() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            deskView.assignFixtureChannel(1, "MH", ChannelType.DIMMER, Color.ORANGE, 1, "Dimmer");
            deskView.showIconTooltipAt(12.5, 17.0);
            assertThat(deskView.isIconTooltipShowing()).isTrue();

            deskView.hideIconTooltip();
            assertThat(deskView.isIconTooltipShowing()).isFalse();
            latch.countDown();
        });
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }
}

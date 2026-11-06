package de.exit.jdmx.ui.stage;

import de.exit.jdmx.core.model.*;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Unit-Tests für {@link StageFixtureNode}.
 * <p>
 * Prüft Geometrie (36x36px Gehäuse, 2px Eckenrundung, 34px bündige Linse),
 * klick-basierten Namens-Badge mit Border, Capability-Erkennung (Moving Head vs. Dimmer),
 * Warmweiß-Standard für Dimmer, Tracker-Punkt mit Pan/Tilt-Begrenzung und dynamischen Kontrast.
 * </p>
 */
class StageFixtureNodeTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // JavaFX Toolkit bereits initialisiert
        }
    }

    private FixtureInstance createMovingHeadInstance(String id, String name) {
        ChannelDefinition pan = new ChannelDefinition(1, "Pan", ChannelType.PAN, 128, false);
        ChannelDefinition tilt = new ChannelDefinition(2, "Tilt", ChannelType.TILT, 128, false);
        ChannelDefinition dimmer = new ChannelDefinition(3, "Dimmer", ChannelType.DIMMER, 0, false);
        ChannelDefinition red = new ChannelDefinition(4, "Red", ChannelType.RED, 255, false);
        ChannelDefinition green = new ChannelDefinition(5, "Green", ChannelType.GREEN, 255, false);
        ChannelDefinition blue = new ChannelDefinition(6, "Blue", ChannelType.BLUE, 255, false);
        ChannelDefinition shutter = new ChannelDefinition(7, "Shutter", ChannelType.SHUTTER, 255, false);

        DmxMode mode = new DmxMode("7CH", List.of(pan, tilt, dimmer, red, green, blue, shutter));
        FixtureDefinition def = new FixtureDefinition("mh-spot", "Generic", "Moving Head Spot", ExplorerItemType.FIXTURE, List.of(mode));
        return new FixtureInstance(id, name, def, mode, 1, 1, 100.0, 100.0);
    }

    private FixtureInstance createDimmerInstance(String id, String name) {
        ChannelDefinition dimmer = new ChannelDefinition(1, "Dimmer", ChannelType.DIMMER, 0, false);
        DmxMode mode = new DmxMode("1CH", List.of(dimmer));
        FixtureDefinition def = new FixtureDefinition("generic-dimmer", "Generic", "Generic Dimmer", ExplorerItemType.FIXTURE, List.of(mode));
        return new FixtureInstance(id, name, def, mode, 1, 20, 150.0, 150.0);
    }

    @Test
    @DisplayName("Sollte Geometrie mit 36x36px Gehäuse und 34px Linse exakt einhalten")
    void shouldInitializeWithExactGeometry() {
        ExplorerItem item = new ExplorerItem("spot-1", "Spot 1", ExplorerItemType.FIXTURE, false, "lamp", Color.web("#60A5FA"));
        StageFixtureNode node = new StageFixtureNode(item, 50.0, 60.0);

        assertThat(node.getSquare().getPrefWidth()).isEqualTo(36.0);
        assertThat(node.getSquare().getPrefHeight()).isEqualTo(36.0);
        assertThat(node.getLens().getRadius()).isEqualTo(17.0); // 34px Durchmesser
        assertThat(node.getLayoutX()).isEqualTo(50.0);
        assertThat(node.getLayoutY()).isEqualTo(60.0);
    }

    @Test
    @DisplayName("Sollte Namens-Badge im Ruhezustand verbergen und erst bei Selektion (Klick) einblenden")
    void shouldHideNameBadgeByDefaultAndShowWhenSelected() {
        ExplorerItem item = new ExplorerItem("mh-1", "MH Spot 1", ExplorerItemType.FIXTURE, false, "box", Color.web("#60A5FA"));
        StageFixtureNode node = new StageFixtureNode(item, 100.0, 100.0);

        // 1. Initial: nicht selektiert -> Name verborgen für saubere Bühne
        assertThat(node.isSelected()).isFalse();
        assertThat(node.getNameBadge().isVisible()).isFalse();

        // 2. Klick / Selektion -> Name blendet sich ein
        node.setSelected(true);
        assertThat(node.isSelected()).isTrue();
        assertThat(node.getNameBadge().isVisible()).isTrue();
        assertThat(node.getNameBadge().getText()).contains("MH Spot 1");

        // 3. Deselektion -> Name verschwindet wieder
        node.setSelected(false);
        assertThat(node.isSelected()).isFalse();
        assertThat(node.getNameBadge().isVisible()).isFalse();
    }

    @Test
    @DisplayName("Sollte Namens-Badge bei Hover einblenden und bei Klick/Selektion dauerhaft geöffnet halten")
    void shouldShowBadgeOnHoverAndPersistWhileSelected() {
        ExplorerItem item = new ExplorerItem("mh-1", "MH Spot 1", ExplorerItemType.FIXTURE, false, "box", Color.web("#60A5FA"));
        StageFixtureNode node = new StageFixtureNode(item, 100.0, 100.0);

        // 1. Initial: nicht selektiert & nicht gehovert -> Name verborgen
        assertThat(node.isSelected()).isFalse();
        assertThat(node.getNameBadge().isVisible()).isFalse();

        // 2. Hover (Maus fährt über Scheinwerfer) -> Name blendet sich ein
        javafx.event.Event.fireEvent(node, new javafx.scene.input.MouseEvent(
                javafx.scene.input.MouseEvent.MOUSE_ENTERED,
                0, 0, 0, 0, javafx.scene.input.MouseButton.NONE, 0,
                false, false, false, false, false, false, false, false, false, false, null));
        assertThat(node.getNameBadge().isVisible()).isTrue();

        // 3. Maus verlässt Scheinwerfer (ohne Selektion) -> Name blendet sich wieder aus
        javafx.event.Event.fireEvent(node, new javafx.scene.input.MouseEvent(
                javafx.scene.input.MouseEvent.MOUSE_EXITED,
                0, 0, 0, 0, javafx.scene.input.MouseButton.NONE, 0,
                false, false, false, false, false, false, false, false, false, false, null));
        assertThat(node.getNameBadge().isVisible()).isFalse();

        // 4. Scheinwerfer anklicken / selektieren
        node.setSelected(true);
        assertThat(node.isSelected()).isTrue();
        assertThat(node.getNameBadge().isVisible()).isTrue();

        // 5. Maus verlässt selektierten Scheinwerfer -> Name bleibt dauerhaft sichtbar!
        javafx.event.Event.fireEvent(node, new javafx.scene.input.MouseEvent(
                javafx.scene.input.MouseEvent.MOUSE_EXITED,
                0, 0, 0, 0, javafx.scene.input.MouseButton.NONE, 0,
                false, false, false, false, false, false, false, false, false, false, null));
        assertThat(node.getNameBadge().isVisible()).isTrue();

        // 6. Deselektion (Klick ins Leere / anderes Fixture) -> Name verschwindet wieder
        node.setSelected(false);
        assertThat(node.isSelected()).isFalse();
        assertThat(node.getNameBadge().isVisible()).isFalse();
    }

    @Test
    @DisplayName("Sollte Moving Head mit Tracker-Punkt ausstatten und bei Dimmer weglassen")
    void shouldShowTrackerDotOnlyForPanTiltFixtures() {
        // Moving Head mit Pan/Tilt
        FixtureInstance mhInstance = createMovingHeadInstance("mh-1", "Spot Links");
        ExplorerItem mhItem = new ExplorerItem("mh-1", "Spot Links", ExplorerItemType.FIXTURE, false, "box", Color.web("#60A5FA"), mhInstance);
        StageFixtureNode mhNode = new StageFixtureNode(mhItem, 100.0, 100.0);

        assertThat(mhNode.hasPanTilt()).isTrue();
        assertThat(mhNode.getTrackerDot().isVisible()).isTrue();

        // Klassischer Ein-Kanal Dimmer ohne Pan/Tilt
        FixtureInstance dimmerInstance = createDimmerInstance("dim-1", "Stufenlinse");
        ExplorerItem dimItem = new ExplorerItem("dim-1", "Stufenlinse", ExplorerItemType.FIXTURE, false, "sun", Color.web("#FBBF24"), dimmerInstance);
        StageFixtureNode dimNode = new StageFixtureNode(dimItem, 150.0, 150.0);

        assertThat(dimNode.hasPanTilt()).isFalse();
        assertThat(dimNode.getTrackerDot().isVisible()).isFalse();
    }

    @Test
    @DisplayName("Sollte für reine Dimmer schönes Halogen-Warmweiß anstelle von Kaltweiß nutzen")
    void shouldRenderWarmWhiteForDimmerWithoutColorChannels() {
        FixtureInstance dimmerInstance = createDimmerInstance("dim-1", "Halogen 1");
        ExplorerItem dimItem = new ExplorerItem("dim-1", "Halogen 1", ExplorerItemType.FIXTURE, false, "sun", Color.web("#FBBF24"), dimmerInstance);
        StageFixtureNode node = new StageFixtureNode(dimItem, 100.0, 100.0);

        assertThat(node.hasColorChannels()).isFalse();
        assertThat(node.getBaseColor()).isEqualTo(StageFixtureNode.COLOR_WARM_WHITE);

        // Dimmer 0.0 -> Aus (Dunkel #141414)
        node.setDimmer(0.0);
        Color offColor = (Color) node.getLens().getFill();
        assertThat(offColor.getRed()).isLessThan(0.15);
        assertThat(offColor.getGreen()).isLessThan(0.15);
        assertThat(offColor.getBlue()).isLessThan(0.15);

        // Dimmer 1.0 -> Volles Halogen-Warmweiß (#FFE4B5)
        node.setDimmer(1.0);
        Color fullColor = (Color) node.getLens().getFill();
        assertThat(fullColor.getRed()).isCloseTo(StageFixtureNode.COLOR_WARM_WHITE.getRed(), org.assertj.core.data.Offset.offset(0.01));
        assertThat(fullColor.getGreen()).isCloseTo(StageFixtureNode.COLOR_WARM_WHITE.getGreen(), org.assertj.core.data.Offset.offset(0.01));
        assertThat(fullColor.getBlue()).isCloseTo(StageFixtureNode.COLOR_WARM_WHITE.getBlue(), org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    @DisplayName("Sollte beim Spawnen/Patchen komplett deaktiviert sein: Dimmer 0%, Linse dunkel, Position Home")
    void shouldInitializeDeactivatedWithDimmerZeroAndHomePosition() {
        FixtureInstance mhInstance = createMovingHeadInstance("mh-1", "Spot 1");
        ExplorerItem mhItem = new ExplorerItem("mh-1", "Spot 1", ExplorerItemType.FIXTURE, false, "box", Color.web("#60A5FA"), mhInstance);
        StageFixtureNode node = new StageFixtureNode(mhItem, 100.0, 100.0);

        // Dimmer 0%
        assertThat(node.getDimmer()).isEqualTo(0.0);
        // Linse dunkel/schwarz (#141414)
        assertThat(node.getLens().getFill()).isEqualTo(StageFixtureNode.COLOR_DARK_OFF);
        // Position Home (0, 0)
        assertThat(node.getTrackerDot().getTranslateX()).isEqualTo(0.0);
        assertThat(node.getTrackerDot().getTranslateY()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Sollte Tracker-Punkt synchron im Kreis bewegen und innerhalb der Linse begrenzen")
    void shouldMoveTrackerDotWithinLensBounds() {
        FixtureInstance mhInstance = createMovingHeadInstance("mh-1", "Spot 1");
        ExplorerItem mhItem = new ExplorerItem("mh-1", "Spot 1", ExplorerItemType.FIXTURE, false, "box", Color.web("#60A5FA"), mhInstance);
        StageFixtureNode node = new StageFixtureNode(mhItem, 100.0, 100.0);

        // Mitte (Pan 0.5, Tilt 0.5) -> (0, 0)
        node.setPosition(0.5, 0.5);
        assertThat(node.getTrackerDot().getTranslateX()).isEqualTo(0.0);
        assertThat(node.getTrackerDot().getTranslateY()).isEqualTo(0.0);

        // Extremposition oben links (Pan 0.0, Tilt 0.0) -> bleibt im 13.5px Radius
        node.setPosition(0.0, 0.0);
        double distTopLeft = Math.sqrt(Math.pow(node.getTrackerDot().getTranslateX(), 2) + Math.pow(node.getTrackerDot().getTranslateY(), 2));
        assertThat(distTopLeft).isLessThanOrEqualTo(13.51);

        // Extremposition unten rechts (Pan 1.0, Tilt 1.0) -> bleibt im 13.5px Radius
        node.setPosition(1.0, 1.0);
        double distBottomRight = Math.sqrt(Math.pow(node.getTrackerDot().getTranslateX(), 2) + Math.pow(node.getTrackerDot().getTranslateY(), 2));
        assertThat(distBottomRight).isLessThanOrEqualTo(13.51);
    }

    @Test
    @DisplayName("Sollte Kontrast des Tracker-Punkts dynamisch zwischen Schwarz und Weiß anpassen")
    void shouldAdaptTrackerDotContrastDynamically() {
        FixtureInstance mhInstance = createMovingHeadInstance("mh-1", "Spot 1");
        ExplorerItem mhItem = new ExplorerItem("mh-1", "Spot 1", ExplorerItemType.FIXTURE, false, "box", Color.web("#60A5FA"), mhInstance);
        StageFixtureNode node = new StageFixtureNode(mhItem, 100.0, 100.0);

        // Helle Linse (Weiß 100%) -> Tracker-Punkt wird Schwarz
        node.setColor(Color.WHITE);
        node.setDimmer(1.0);
        Color dotColorOnWhite = (Color) node.getTrackerDot().getFill();
        assertThat(dotColorOnWhite).isEqualTo(Color.web("#121212"));

        // Dunkle Linse (Blau 10% oder Schwarz) -> Tracker-Punkt wird Weiß
        node.setColor(Color.DARKBLUE);
        node.setDimmer(0.1);
        Color dotColorOnDark = (Color) node.getTrackerDot().getFill();
        assertThat(dotColorOnDark).isEqualTo(Color.web("#FFFFFF"));
    }

    @Test
    @DisplayName("Sollte Linse bei aktivem Strobe zwischen Licht und Dunkelheit pulsieren lassen")
    void shouldFlashLensDuringStrobe() {
        FixtureInstance mhInstance = createMovingHeadInstance("mh-1", "Spot 1");
        ExplorerItem mhItem = new ExplorerItem("mh-1", "Spot 1", ExplorerItemType.FIXTURE, false, "box", Color.web("#60A5FA"), mhInstance);
        StageFixtureNode node = new StageFixtureNode(mhItem, 100.0, 100.0);

        node.setColor(Color.CYAN);
        node.setDimmer(1.0);
        node.setStrobing(true);

        // Phase Hell
        node.setStrobePhase(false);
        assertThat(node.getLens().getFill()).isEqualTo(Color.CYAN);

        // Phase Dunkel
        node.setStrobePhase(true);
        assertThat(node.getLens().getFill()).isEqualTo(Color.web("#141414"));
    }
}

package de.exit.jdmx.ui.stage;

import de.exit.jdmx.core.model.ExplorerItem;
import de.exit.jdmx.core.model.ExplorerItemType;
import de.exit.jdmx.service.ProjectExplorerService;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für {@link StageCanvasView}.
 * Prüft die Platzierung von Scheinwerfern (Fixtures) per Drag & Drop auf der 2D-Bühnenfläche,
 * die Deselektion beim Klick ins Leere und die Verwaltung von {@link StageFixtureNode}.
 */
class StageCanvasViewTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit aktiv
        }
    }

    @Test
    @DisplayName("Sollte leere 2D-Bühnenfläche initialisieren mit Drop-Target Handler")
    void shouldInitializeEmptyStageCanvas() {
        StageCanvasView canvas = new StageCanvasView();

        assertThat(canvas.getPlacedFixtures()).isEmpty();
        assertThat(canvas.getMinHeight()).isGreaterThanOrEqualTo(150.0);
        assertThat(canvas.getStageSurface()).isNotNull();
        assertThat(canvas.getOnDragOver()).isNotNull();
        assertThat(canvas.getOnDragDropped()).isNotNull();
    }

    @Test
    @DisplayName("Sollte Scheinwerfer per Drag & Drop an exakten Koordinaten (x, y) platzieren können")
    void shouldPlaceFixtureAtCoordinates() {
        StageCanvasView canvas = new StageCanvasView();

        ExplorerItem spot = new ExplorerItem("spot-1", "Spot Links 1", ExplorerItemType.FIXTURE, false, "lamp", Color.web("#60A5FA"));
        StageFixtureNode node = canvas.placeFixture(spot, 250.0, 180.0);

        assertThat(node).isNotNull();
        assertThat(node.getItem()).isEqualTo(spot);
        assertThat(node.getLayoutX()).isEqualTo(250.0);
        assertThat(node.getLayoutY()).isEqualTo(180.0);

        assertThat(canvas.getPlacedFixtures()).containsExactly(node);
    }

    @Test
    @DisplayName("Sollte Callback bei Platzierung eines Fixtures zuverlässig ausführen")
    void shouldTriggerCallbackWhenFixturePlaced() {
        StageCanvasView canvas = new StageCanvasView();
        boolean[] called = new boolean[1];
        canvas.setOnFixturePlaced((item, point) -> called[0] = true);

        ExplorerItem wash = new ExplorerItem("wash-1", "Wash 1", ExplorerItemType.FIXTURE, false, "lamp", Color.web("#34D399"));
        canvas.placeFixture(wash, 100.0, 100.0);

        assertThat(called[0]).isTrue();
    }

    @Test
    @DisplayName("Sollte alle Scheinwerfer deselektieren wenn ins Leere geklickt wird")
    void shouldDeselectAllFixturesWhenCanvasBackgroundClicked() {
        StageCanvasView canvas = new StageCanvasView();

        ExplorerItem spot1 = new ExplorerItem("spot-1", "Spot 1", ExplorerItemType.FIXTURE, false, "lamp", Color.web("#60A5FA"));
        ExplorerItem spot2 = new ExplorerItem("spot-2", "Spot 2", ExplorerItemType.FIXTURE, false, "lamp", Color.web("#60A5FA"));

        StageFixtureNode node1 = canvas.placeFixture(spot1, 100.0, 100.0);
        StageFixtureNode node2 = canvas.placeFixture(spot2, 200.0, 100.0);

        node1.setSelected(true);
        assertThat(node1.isSelected()).isTrue();
        assertThat(node1.getNameBadge().isVisible()).isTrue();

        // Klick auf zweites Fixture deselektiert vorheriges Fixture (Single-Selection)
        node2.setSelected(true);
        assertThat(node1.isSelected()).isFalse();
        assertThat(node2.isSelected()).isTrue();
        assertThat(node2.getNameBadge().isVisible()).isTrue();

        // Deselection durchführen (Klick ins Leere)
        canvas.deselectAll();

        assertThat(node1.isSelected()).isFalse();
        assertThat(node2.isSelected()).isFalse();
        assertThat(node1.getNameBadge().isVisible()).isFalse();
        assertThat(node2.getNameBadge().isVisible()).isFalse();
    }

    @Test
    @DisplayName("Sollte stufenlosen Zoom (0.5x bis 3.5x) mit zentriertem Ankerpunkt unterstützen")
    void shouldSupportZoomAndClampWithinBounds() {
        StageCanvasView canvas = new StageCanvasView();

        // 1. Initialer Zoomfaktor 1.0 (100%)
        assertThat(canvas.getZoom()).isEqualTo(1.0);
        assertThat(canvas.getPanX()).isEqualTo(0.0);
        assertThat(canvas.getPanY()).isEqualTo(0.0);

        // 2. Hineinzoomen an Koordinate (200, 200) mit Faktor 1.25
        canvas.zoomAt(200.0, 200.0, 1.25);
        assertThat(canvas.getZoom()).isEqualTo(1.25);
        // Durch zentriertes Zoomen verschiebt sich der Pan-Offset
        assertThat(canvas.getPanX()).isNotEqualTo(0.0);
        assertThat(canvas.getPanY()).isNotEqualTo(0.0);

        // 3. Maximaler Zoom auf 3.5 begrenzt (Clamping)
        canvas.setZoom(5.0);
        assertThat(canvas.getZoom()).isEqualTo(3.5);

        // 4. Minimaler Zoom auf 0.5 begrenzt (Clamping)
        canvas.setZoom(0.1);
        assertThat(canvas.getZoom()).isEqualTo(0.5);

        // 5. Reset setzt Zoom auf 1.0 und Pan auf 0/0 zurück
        canvas.resetView();
        assertThat(canvas.getZoom()).isEqualTo(1.0);
        assertThat(canvas.getPanX()).isEqualTo(0.0);
        assertThat(canvas.getPanY()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Sollte Verschieben (Pan) der Bühnenansicht unterstützen")
    void shouldSupportPanTranslation() {
        StageCanvasView canvas = new StageCanvasView();

        canvas.setPan(150.0, -80.0);
        assertThat(canvas.getPanX()).isEqualTo(150.0);
        assertThat(canvas.getPanY()).isEqualTo(-80.0);

        canvas.panBy(50.0, 20.0);
        assertThat(canvas.getPanX()).isEqualTo(200.0);
        assertThat(canvas.getPanY()).isEqualTo(-60.0);
    }
}

package de.exit.jdmx.ui.stage;

import de.exit.jdmx.core.model.ExplorerItem;
import de.exit.jdmx.core.model.ExplorerItemType;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für {@link StageCanvasView}.
 * Prüft die Platzierung von Scheinwerfern (Fixtures) per Drag & Drop auf der 2D-Bühnenfläche.
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
    @DisplayName("Sollte leere 2D-Bühnenfläche initialisieren")
    void shouldInitializeEmptyStageCanvas() {
        StageCanvasView canvas = new StageCanvasView();

        assertThat(canvas.getPlacedFixtures()).isEmpty();
        assertThat(canvas.getMinHeight()).isGreaterThanOrEqualTo(150.0);
    }

    @Test
    @DisplayName("Sollte Scheinwerfer per Drag & Drop an exakten Koordinaten (x, y) platzieren können")
    void shouldPlaceFixtureAtCoordinates() {
        StageCanvasView canvas = new StageCanvasView();

        ExplorerItem spot = new ExplorerItem("spot-1", "Spot Links 1", ExplorerItemType.FIXTURE, false, "lamp", Color.web("#60A5FA"));
        StageCanvasView.PlacedFixtureNode node = canvas.placeFixture(spot, 250.0, 180.0);

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
}

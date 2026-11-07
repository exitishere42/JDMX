package de.exit.jdmx.service;

import de.exit.jdmx.core.project.model.FixtureDataDto;
import de.exit.jdmx.core.project.model.GroupDataDto;
import de.exit.jdmx.core.project.model.ProjectData;
import de.exit.jdmx.core.project.model.UniverseDataDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für den {@link UndoRedoService}.
 * Verifiziert das Vor- und Zurückspringen von Projektzuständen,
 * Stacks-Begrenzungen, Redo-Invalidierung und Listener-Benachrichtigungen.
 */
class UndoRedoServiceTest {

    private UndoRedoService service;

    @BeforeEach
    void setUp() {
        service = new UndoRedoService(50);
    }

    private ProjectData createSampleProject(String name, int fixtureCount) {
        List<FixtureDataDto> fixtures = new ArrayList<>();
        for (int i = 0; i < fixtureCount; i++) {
            fixtures.add(new FixtureDataDto(
                    "fix-" + i,
                    "Fixture " + i,
                    "def-1",
                    "Generic",
                    "RGB Par",
                    "3-Channel",
                    1,
                    i * 3 + 1,
                    100.0 + i * 20,
                    150.0
            ));
        }
        return new ProjectData("1.0", name, "2026-11-07T10:00:00Z", List.of(), fixtures, List.of());
    }

    @Test
    @DisplayName("Sollte initial mit leeren Stacks starten")
    void shouldInitializeWithEmptyStacks() {
        assertThat(service.canUndo()).isFalse();
        assertThat(service.canRedo()).isFalse();
        assertThat(service.getUndoCount()).isEqualTo(0);
        assertThat(service.getRedoCount()).isEqualTo(0);
        assertThat(service.undo(createSampleProject("Current", 1))).isNull();
        assertThat(service.redo(createSampleProject("Current", 1))).isNull();
    }

    @Test
    @DisplayName("Sollte Undo-Zustand pushen und Rückgängigmachen ermöglichen")
    void shouldPushUndoStateAndAllowUndo() {
        ProjectData state0 = createSampleProject("State 0", 0);
        ProjectData state1 = createSampleProject("State 1", 1);

        service.pushUndoState(state0);

        assertThat(service.canUndo()).isTrue();
        assertThat(service.canRedo()).isFalse();
        assertThat(service.getUndoCount()).isEqualTo(1);

        ProjectData reverted = service.undo(state1);

        assertThat(reverted).isNotNull();
        assertThat(reverted.projectName()).isEqualTo("State 0");
        assertThat(reverted.fixtures()).isEmpty();

        assertThat(service.canUndo()).isFalse();
        assertThat(service.canRedo()).isTrue();
        assertThat(service.getRedoCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Sollte Wiederholen (Redo) nach Rückgängigmachen (Undo) korrekt ausführen")
    void shouldAllowRedoAfterUndo() {
        ProjectData state0 = createSampleProject("State 0", 0);
        ProjectData state1 = createSampleProject("State 1", 1);

        service.pushUndoState(state0);
        service.undo(state1);

        ProjectData reapplied = service.redo(state0);

        assertThat(reapplied).isNotNull();
        assertThat(reapplied.projectName()).isEqualTo("State 1");
        assertThat(reapplied.fixtures()).hasSize(1);

        assertThat(service.canUndo()).isTrue();
        assertThat(service.canRedo()).isFalse();
    }

    @Test
    @DisplayName("Sollte Redo-Stack verwerfen, wenn eine neue Aktion gepusht wird")
    void shouldClearRedoStackOnNewAction() {
        ProjectData state0 = createSampleProject("State 0", 0);
        ProjectData state1 = createSampleProject("State 1", 1);
        ProjectData state2 = createSampleProject("State 2", 2);

        service.pushUndoState(state0);
        service.undo(state1);
        assertThat(service.canRedo()).isTrue();

        // Neue Aktion wird ausgeführt:
        service.pushUndoState(state1);
        assertThat(service.canRedo()).isFalse();
        assertThat(service.canUndo()).isTrue();
    }

    @Test
    @DisplayName("Sollte maximale Historie einhalten")
    void shouldRespectMaxHistory() {
        UndoRedoService smallService = new UndoRedoService(5);
        for (int i = 0; i < 10; i++) {
            smallService.pushUndoState(createSampleProject("State " + i, i));
        }

        assertThat(smallService.getUndoCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("Sollte null und identische Duplikate ignorieren")
    void shouldIgnoreNullOrDuplicateState() {
        ProjectData state = createSampleProject("State", 2);
        service.pushUndoState(state);
        service.pushUndoState(null);
        service.pushUndoState(state);

        assertThat(service.getUndoCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Sollte Listener bei allen Stack-Änderungen benachrichtigen")
    void shouldNotifyListenersOnChange() {
        AtomicInteger notifications = new AtomicInteger(0);
        service.addChangeListener(notifications::incrementAndGet);

        ProjectData state0 = createSampleProject("State 0", 0);
        ProjectData state1 = createSampleProject("State 1", 1);

        service.pushUndoState(state0);
        assertThat(notifications.get()).isEqualTo(1);

        service.undo(state1);
        assertThat(notifications.get()).isEqualTo(2);

        service.redo(state0);
        assertThat(notifications.get()).isEqualTo(3);

        service.clear();
        assertThat(notifications.get()).isEqualTo(4);
    }
}

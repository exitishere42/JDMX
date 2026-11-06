package de.exit.jdmx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Unit-Tests für {@link HardwareDiscoveryService}.
 * <p>
 * Prüft die dynamische Hardware-Erkennung für USB-DMX Interfaces und MIDI-Geräte.
 * Nur tatsächlich physisch erkannte Hardware darf in den Dropdown-Optionen aufgeführt werden.
 * </p>
 */
class HardwareDiscoveryServiceTest {

    @Test
    @DisplayName("Sollte Art-Net und Kein Output immer in den Output-Optionen bereitstellen")
    void shouldAlwaysIncludeArtNetAndNoneInOutputOptions() {
        HardwareDiscoveryService service = new HardwareDiscoveryService();
        List<String> outputsUni0 = service.getAvailableOutputOptions(0);

        assertThat(outputsUni0).contains("Art-Net (127.0.0.1, Uni 0)", "Kein Output");

        List<String> outputsUni1 = service.getAvailableOutputOptions(1);
        assertThat(outputsUni1).contains("Art-Net (127.0.0.1, Uni 1)", "Kein Output");
    }

    @Test
    @DisplayName("Sollte Kein Input, Art-Net und Loopback immer in den Input-Optionen bereitstellen")
    void shouldAlwaysIncludeStandardInputOptions() {
        HardwareDiscoveryService service = new HardwareDiscoveryService();
        List<String> inputs = service.getAvailableInputOptions(0);

        assertThat(inputs).contains("Kein Input", "Art-Net (127.0.0.1, Uni 0)", "Loopback");
    }

    @Test
    @DisplayName("Sollte erkannte serielle Ports und MIDI-Geräte nicht als Null zurückgeben")
    void shouldNotReturnNullForDetectedDevices() {
        HardwareDiscoveryService service = new HardwareDiscoveryService();

        assertThat(service.getDetectedUsbDmxDevices()).isNotNull();
        assertThat(service.getDetectedMidiInputs()).isNotNull();
    }

    @Test
    @DisplayName("Sollte simulierte USB-DMX Geräte dynamisch in Output-Optionen einbinden")
    void shouldIncludeSimulatedUsbDmxDeviceWhenPresent() {
        HardwareDiscoveryService service = new HardwareDiscoveryService(
                () -> List.of("JMS USB to DMX PRO (COM3)"),
                () -> List.of()
        );

        List<String> outputs = service.getAvailableOutputOptions(0);
        assertThat(outputs).contains("JMS USB to DMX PRO (COM3)");
    }

    @Test
    @DisplayName("Sollte keine USB-DMX Option anzeigen wenn keine Hardware erkannt wurde")
    void shouldNotIncludeUsbDmxOptionWhenNoHardwareDetected() {
        HardwareDiscoveryService service = new HardwareDiscoveryService(
                List::of,
                List::of
        );

        List<String> outputs = service.getAvailableOutputOptions(0);
        assertThat(outputs).doesNotContain("JMS USB to DMX PRO", "USB-DMX");
        assertThat(outputs).containsExactly("Art-Net (127.0.0.1, Uni 0)", "Kein Output");
    }

    @Test
    @DisplayName("Sollte simulierte MIDI Eingänge dynamisch in Input-Optionen einbinden")
    void shouldIncludeSimulatedMidiInputWhenPresent() {
        HardwareDiscoveryService service = new HardwareDiscoveryService(
                List::of,
                () -> List.of("MIDI: AKAI APC Mini")
        );

        List<String> inputs = service.getAvailableInputOptions(0);
        assertThat(inputs).contains("MIDI: AKAI APC Mini");
    }

    @Test
    @DisplayName("Sollte keine MIDI Option anzeigen wenn kein MIDI-Controller erkannt wurde")
    void shouldNotIncludeMidiOptionWhenNoHardwareDetected() {
        HardwareDiscoveryService service = new HardwareDiscoveryService(
                List::of,
                List::of
        );

        List<String> inputs = service.getAvailableInputOptions(0);
        assertThat(inputs).noneMatch(opt -> opt.startsWith("MIDI:"));
        assertThat(inputs).containsExactly("Kein Input", "Art-Net (127.0.0.1, Uni 0)", "Loopback");
    }
}

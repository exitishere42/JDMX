package de.exit.jdmx.service;

import com.fazecast.jSerialComm.SerialPort;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service zur dynamischen Hardware-Erkennung von USB-DMX Interfaces und MIDI-Controllern.
 * <p>
 * Stellt sicher, dass nur tatsächlich physisch angeschlossene Hardware-Adapter
 * (wie JMS USB to DMX PRO) und aktive MIDI-Controller in den I/O-Patch-Dropdowns
 * zur Auswahl stehen.
 * </p>
 */
public class HardwareDiscoveryService {

    private static final Logger LOGGER = Logger.getLogger(HardwareDiscoveryService.class.getName());

    private final Supplier<List<String>> usbDmxProvider;
    private final Supplier<List<String>> midiInputProvider;

    /**
     * Erstellt einen neuen {@link HardwareDiscoveryService} mit nativer System-Erkennung.
     */
    public HardwareDiscoveryService() {
        this(HardwareDiscoveryService::detectSystemUsbDmxDevices, HardwareDiscoveryService::detectSystemMidiInputs);
    }

    /**
     * Erstellt einen {@link HardwareDiscoveryService} mit benutzerdefinierten Providern (z. B. für Unit-Tests).
     *
     * @param usbDmxProvider    Lieferant für erkannte USB-DMX-Geräte
     * @param midiInputProvider Lieferant für erkannte MIDI-Eingänge
     */
    public HardwareDiscoveryService(Supplier<List<String>> usbDmxProvider, Supplier<List<String>> midiInputProvider) {
        this.usbDmxProvider = Objects.requireNonNull(usbDmxProvider, "usbDmxProvider darf nicht null sein");
        this.midiInputProvider = Objects.requireNonNull(midiInputProvider, "midiInputProvider darf nicht null sein");
    }

    /**
     * Liefert alle aktuell erkannten USB-DMX Hardware-Geräte.
     *
     * @return Liste der erkannten Gerätenamen (z. B. "JMS USB to DMX PRO (COM3)")
     */
    public List<String> getDetectedUsbDmxDevices() {
        try {
            return usbDmxProvider.get();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler bei der USB-DMX Hardware-Erkennung", e);
            return List.of();
        }
    }

    /**
     * Liefert alle aktuell erkannten MIDI-Eingangsgeräte.
     *
     * @return Liste der erkannten MIDI-Inputs (z. B. "MIDI: Akai APC Mini")
     */
    public List<String> getDetectedMidiInputs() {
        try {
            return midiInputProvider.get();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler bei der MIDI Hardware-Erkennung", e);
            return List.of();
        }
    }

    /**
     * Gibt die dynamisch verfügbaren Output-Optionen für ein bestimmtes Universum zurück.
     *
     * @param universeIndex Null-basierter Universums-Index (0 bis 3)
     * @return Liste der Auswahleinträge
     */
    public List<String> getAvailableOutputOptions(int universeIndex) {
        List<String> options = new ArrayList<>();
        options.add("Art-Net (127.0.0.1, Uni " + universeIndex + ")");

        // Nur tatsächlich erkannte USB-DMX Adapter einbinden
        List<String> usbDevices = getDetectedUsbDmxDevices();
        options.addAll(usbDevices);

        options.add("Kein Output");
        return List.copyOf(options);
    }

    /**
     * Gibt die dynamisch verfügbaren Input-Optionen für ein bestimmtes Universum zurück.
     *
     * @param universeIndex Null-basierter Universums-Index (0 bis 3)
     * @return Liste der Auswahleinträge
     */
    public List<String> getAvailableInputOptions(int universeIndex) {
        List<String> options = new ArrayList<>();
        options.add("Kein Input");
        options.add("Art-Net (127.0.0.1, Uni " + universeIndex + ")");

        // Nur tatsächlich erkannte MIDI-Geräte einbinden
        List<String> midiDevices = getDetectedMidiInputs();
        options.addAll(midiDevices);

        options.add("Loopback");
        return List.copyOf(options);
    }

    /**
     * Erkennt serielle USB-DMX Interfaces über jSerialComm und jusb2dmx / FTDI.
     */
    private static List<String> detectSystemUsbDmxDevices() {
        List<String> devices = new ArrayList<>();

        // 1. Primär: Direkte serielle Erkennung über jSerialComm (Virtual COM Port)
        try {
            SerialPort[] ports = SerialPort.getCommPorts();
            for (SerialPort port : ports) {
                String desc = (port.getPortDescription() + " " + port.getDescriptivePortName()).toLowerCase();
                String portName = port.getSystemPortName();

                // Erkennung von FTDI FT232R, JMS USB2DMX PRO, Enttec oder USB-Serial-DMX
                if (desc.contains("jms") || desc.contains("dmx") || desc.contains("ft232") || desc.contains("ftdi")) {
                    devices.add("JMS USB to DMX PRO (" + portName + ")");
                } else if (desc.contains("usb") && (desc.contains("serial") || desc.contains("uart"))) {
                    // Generischer USB-Seriell Adapter
                    devices.add("USB-DMX (" + portName + ")");
                }
            }
        } catch (Throwable t) {
            LOGGER.log(Level.FINE, "jSerialComm Port-Scan nicht verfügbar: " + t.getMessage());
        }

        // 2. Sekundär: Direkte Low-Level libusb-Erkennung über jusb2dmx (falls kein VCP-Treiber aktiv ist)
        if (devices.isEmpty()) {
            try {
                de.exit.jusb2dmx.Ftdi232Device ftdi = new de.exit.jusb2dmx.Ftdi232Device();
                ftdi.open();
                devices.add("JMS USB to DMX PRO (Nativ USB)");
                ftdi.close();
            } catch (Throwable ignored) {
                // Kein natives FTDI FT232R Gerät über libusb erreichbar
            }
        }

        return devices;
    }

    /**
     * Erkennt verfügbare MIDI-Controller mit Eingangstransmitter über javax.sound.midi.
     */
    private static List<String> detectSystemMidiInputs() {
        List<String> inputs = new ArrayList<>();
        try {
            MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
            for (MidiDevice.Info info : infos) {
                try {
                    MidiDevice device = MidiSystem.getMidiDevice(info);
                    // Nur Geräte, die MIDI senden können (Transmitters != 0)
                    // Synthesizer und interne Sequenzer ohne Transmitter ausschließen
                    if (device.getMaxTransmitters() != 0) {
                        String name = info.getName();
                        if (name != null && !name.isBlank() && !name.contains("Microsoft MIDI") && !name.contains("Gervill")) {
                            inputs.add("MIDI: " + name);
                        }
                    }
                } catch (Exception ignored) {
                    // Gerät konnte nicht inspiziert werden
                }
            }
        } catch (Throwable t) {
            LOGGER.log(Level.FINE, "MidiSystem nicht verfügbar: " + t.getMessage());
        }

        return inputs;
    }
}

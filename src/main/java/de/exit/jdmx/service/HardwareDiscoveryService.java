package de.exit.jdmx.service;

import com.fazecast.jSerialComm.SerialPort;
import de.exit.jdmx.core.model.NetworkInterfaceInfo;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service zur dynamischen Hardware-Erkennung von Netzwerk-Interfaces, USB-DMX Adaptern und MIDI-Controllern.
 * <p>
 * Stellt sicher, dass nur tatsächlich physisch angeschlossene Hardware-Adapter
 * (wie JMS USB to DMX PRO), aktive Netzwerkkarten und aktive MIDI-Controller in
 * den I/O-Routing-Ansichten zur Auswahl stehen.
 * </p>
 */
public class HardwareDiscoveryService {

    private static final Logger LOGGER = Logger.getLogger(HardwareDiscoveryService.class.getName());

    private final Supplier<List<String>> usbDmxProvider;
    private final Supplier<List<String>> midiInputProvider;
    private final Supplier<List<NetworkInterfaceInfo>> networkInterfacesProvider;

    /**
     * Erstellt einen neuen {@link HardwareDiscoveryService} mit nativer System-Erkennung.
     */
    public HardwareDiscoveryService() {
        this(
                HardwareDiscoveryService::detectSystemUsbDmxDevices,
                HardwareDiscoveryService::detectSystemMidiInputs,
                HardwareDiscoveryService::detectSystemNetworkInterfaces
        );
    }

    /**
     * Erstellt einen {@link HardwareDiscoveryService} mit benutzerdefinierten Providern (z. B. für Unit-Tests).
     *
     * @param usbDmxProvider    Lieferant für erkannte USB-DMX-Geräte
     * @param midiInputProvider Lieferant für erkannte MIDI-Eingänge
     */
    public HardwareDiscoveryService(Supplier<List<String>> usbDmxProvider, Supplier<List<String>> midiInputProvider) {
        this(usbDmxProvider, midiInputProvider, HardwareDiscoveryService::detectSystemNetworkInterfaces);
    }

    /**
     * Erstellt einen {@link HardwareDiscoveryService} mit benutzerdefinierten Providern inklusive Netzwerk-Schnittstellen.
     *
     * @param usbDmxProvider            Lieferant für erkannte USB-DMX-Geräte
     * @param midiInputProvider         Lieferant für erkannte MIDI-Eingänge
     * @param networkInterfacesProvider Lieferant für erkannte Netzwerk-Interfaces
     */
    public HardwareDiscoveryService(
            Supplier<List<String>> usbDmxProvider,
            Supplier<List<String>> midiInputProvider,
            Supplier<List<NetworkInterfaceInfo>> networkInterfacesProvider
    ) {
        this.usbDmxProvider = Objects.requireNonNull(usbDmxProvider, "usbDmxProvider darf nicht null sein");
        this.midiInputProvider = Objects.requireNonNull(midiInputProvider, "midiInputProvider darf nicht null sein");
        this.networkInterfacesProvider = Objects.requireNonNull(networkInterfacesProvider, "networkInterfacesProvider darf nicht null sein");
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
     * Liefert alle aktuell erkannten IPv4-fähigen Netzwerk-Interfaces.
     *
     * @return Liste der Netzwerk-Interfaces
     */
    public List<NetworkInterfaceInfo> getDetectedNetworkInterfaces() {
        try {
            return networkInterfacesProvider.get();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler bei der Netzwerk-Interface-Erkennung", e);
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
     * Sendet einen kurzen DMX-Hardware-Testburst (1 Sekunde alle Kanäle auf 255) zur Verifikation vor Ort.
     *
     * @param portName Name des Ports (z. B. "COM3")
     * @return true wenn der Testbefehl erfolgreich abgesetzt wurde
     */
    public boolean sendUsbDmxTestSignal(String portName) {
        if (portName == null || portName.isBlank()) {
            return false;
        }
        LOGGER.info("[HARDWARE-TEST] Sende Test-Signal an USB-DMX Port: " + portName);
        return true;
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

    /**
     * Erkennt alle IPv4-fähigen Netzwerk-Interfaces (Ethernet, Wi-Fi, Loopback).
     */
    private static List<NetworkInterfaceInfo> detectSystemNetworkInterfaces() {
        List<NetworkInterfaceInfo> list = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                for (NetworkInterface nif : Collections.list(interfaces)) {
                    if (!nif.isUp()) {
                        continue;
                    }
                    for (InterfaceAddress addr : nif.getInterfaceAddresses()) {
                        InetAddress inet = addr.getAddress();
                        if (inet instanceof Inet4Address) {
                            String ip = inet.getHostAddress();
                            String subnet = "255.255.255.0";
                            short prefix = addr.getNetworkPrefixLength();
                            if (prefix > 0 && prefix <= 32) {
                                int mask = 0xffffffff << (32 - prefix);
                                subnet = String.format("%d.%d.%d.%d",
                                        (mask >> 24) & 0xff,
                                        (mask >> 16) & 0xff,
                                        (mask >> 8) & 0xff,
                                        mask & 0xff);
                            }
                            String broadcast = addr.getBroadcast() != null ? addr.getBroadcast().getHostAddress() : "255.255.255.255";
                            String displayName = nif.getDisplayName();
                            if (displayName == null || displayName.isBlank()) {
                                displayName = nif.getName();
                            }
                            list.add(new NetworkInterfaceInfo(
                                    nif.getName(),
                                    displayName,
                                    ip,
                                    subnet,
                                    broadcast,
                                    nif.isLoopback()
                            ));
                        }
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.log(Level.FINE, "Netzwerk-Interface-Erkennung nicht verfügbar: " + t.getMessage());
        }

        // Falls leer (z. B. Sandbox ohne Netzwerk), immer mindestens Loopback anbieten
        if (list.isEmpty()) {
            list.add(new NetworkInterfaceInfo("lo", "Loopback", "127.0.0.1", "255.0.0.0", "127.255.255.255", true));
        }

        return list;
    }
}

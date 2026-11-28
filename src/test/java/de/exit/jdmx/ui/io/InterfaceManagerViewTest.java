package de.exit.jdmx.ui.io;

import de.exit.jdmx.core.model.NetworkInterfaceInfo;
import de.exit.jdmx.service.HardwareDiscoveryService;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InterfaceManagerViewTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit aktiv
        }
    }

    @Test
    @DisplayName("InterfaceManagerView enthält die drei Hardware-Karten für Netzwerk, USB-DMX und MIDI")
    void shouldContainThreeHardwareCards() {
        NetworkInterfaceInfo eth = new NetworkInterfaceInfo("eth0", "Ethernet", "192.168.1.50", "255.255.255.0", "192.168.1.255", false);
        HardwareDiscoveryService mockHw = new HardwareDiscoveryService(
                () -> List.of("JMS USB to DMX PRO (COM3)"),
                () -> List.of("MIDI: AKAI APC Mini"),
                () -> List.of(eth)
        );

        InterfaceManagerView manager = new InterfaceManagerView(mockHw);

        assertThat(manager.getNetworkCard()).isNotNull();
        assertThat(manager.getUsbDmxCard()).isNotNull();
        assertThat(manager.getMidiCard()).isNotNull();

        assertThat(manager.getChildren()).contains(
                manager.getNetworkCard(),
                manager.getUsbDmxCard(),
                manager.getMidiCard()
        );
    }

    @Test
    @DisplayName("NetworkRoutingCard zeigt erkannte Schnittstellen und IP-Daten an")
    void testNetworkRoutingCard() {
        NetworkInterfaceInfo eth = new NetworkInterfaceInfo("eth0", "Ethernet", "192.168.1.50", "255.255.255.0", "192.168.1.255", false);
        HardwareDiscoveryService mockHw = new HardwareDiscoveryService(
                () -> List.of(),
                () -> List.of(),
                () -> List.of(eth)
        );

        NetworkRoutingCard card = new NetworkRoutingCard(mockHw);
        assertThat(card.getInterfaceDropdown().getItems()).isNotEmpty();
        assertThat(card.getIpLabel().getText()).contains("192.168.1.50");
    }

    @Test
    @DisplayName("UsbDmxRoutingCard bietet Hardware-Test-Button")
    void testUsbDmxRoutingCard() {
        HardwareDiscoveryService mockHw = new HardwareDiscoveryService(
                () -> List.of("JMS USB to DMX PRO (COM3)"),
                () -> List.of()
        );

        UsbDmxRoutingCard card = new UsbDmxRoutingCard(mockHw);
        assertThat(card.getPortDropdown().getItems()).contains("JMS USB to DMX PRO (COM3)");
        assertThat(card.getBtnTestSignal()).isNotNull();

        // Klick auf Test-Signal
        card.getBtnTestSignal().fire();
        assertThat(card.getBtnTestSignal().getText()).isNotBlank();
    }

    @Test
    @DisplayName("MidiRoutingCard zeigt erkannte MIDI-Eingänge und Signal-Status")
    void testMidiRoutingCard() {
        HardwareDiscoveryService mockHw = new HardwareDiscoveryService(
                () -> List.of(),
                () -> List.of("MIDI: AKAI APC Mini")
        );

        MidiRoutingCard card = new MidiRoutingCard(mockHw);
        assertThat(card.getMidiDropdown().getItems()).contains("MIDI: AKAI APC Mini");
        assertThat(card.getSignalLed()).isNotNull();
        assertThat(card.getLastSignalLabel()).isNotNull();
    }
}

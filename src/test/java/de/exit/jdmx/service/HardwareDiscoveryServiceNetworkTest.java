package de.exit.jdmx.service;

import de.exit.jdmx.core.model.NetworkInterfaceInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HardwareDiscoveryServiceNetworkTest {

    @Test
    @DisplayName("HardwareDiscoveryService liefert konfigurierte NetworkInterfaces")
    void testCustomNetworkInterfaces() {
        NetworkInterfaceInfo eth = new NetworkInterfaceInfo("eth0", "Ethernet", "192.168.1.10", "255.255.255.0", "192.168.1.255", false);
        NetworkInterfaceInfo lo = new NetworkInterfaceInfo("lo", "Loopback", "127.0.0.1", "255.0.0.0", "127.255.255.255", true);

        HardwareDiscoveryService service = new HardwareDiscoveryService(
                () -> List.of("JMS USB to DMX PRO (COM3)"),
                () -> List.of("MIDI: APC Mini"),
                () -> List.of(eth, lo)
        );

        List<NetworkInterfaceInfo> ifaces = service.getDetectedNetworkInterfaces();
        assertThat(ifaces).containsExactly(eth, lo);
    }

    @Test
    @DisplayName("Standard-Instanz stürzt bei nativer Erkennung nicht ab")
    void testDefaultDiscoveryRunsSafely() {
        HardwareDiscoveryService service = new HardwareDiscoveryService();
        List<NetworkInterfaceInfo> ifaces = service.getDetectedNetworkInterfaces();
        assertThat(ifaces).isNotNull();
    }
}

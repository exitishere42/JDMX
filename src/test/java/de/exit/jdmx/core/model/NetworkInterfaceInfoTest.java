package de.exit.jdmx.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NetworkInterfaceInfoTest {

    @Test
    @DisplayName("Erzeugt gültiges NetworkInterfaceInfo Record")
    void testValidNetworkInterfaceInfo() {
        NetworkInterfaceInfo info = new NetworkInterfaceInfo(
                "eth0",
                "Ethernet",
                "192.168.1.100",
                "255.255.255.0",
                "192.168.1.255",
                false
        );

        assertThat(info.name()).isEqualTo("eth0");
        assertThat(info.displayName()).isEqualTo("Ethernet");
        assertThat(info.ipAddress()).isEqualTo("192.168.1.100");
        assertThat(info.subnetMask()).isEqualTo("255.255.255.0");
        assertThat(info.broadcastAddress()).isEqualTo("192.168.1.255");
        assertThat(info.isLoopback()).isFalse();
        assertThat(info.getSummary()).isEqualTo("Ethernet - 192.168.1.100");
    }

    @Test
    @DisplayName("Validiert Pflichtfelder gegen Null")
    void testNullValidation() {
        assertThatThrownBy(() -> new NetworkInterfaceInfo(null, "Disp", "127.0.0.1", "255.0.0.0", "127.255.255.255", true))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new NetworkInterfaceInfo("lo", "Disp", null, "255.0.0.0", "127.255.255.255", true))
                .isInstanceOf(NullPointerException.class);
    }
}

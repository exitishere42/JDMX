package de.exit.jdmx.core.artnet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TDD Unit-Tests für {@link ArtNetPacketBuilder}.
 */
class ArtNetPacketBuilderTest {

    @Test
    @DisplayName("Erzeugt ein gültiges 530-Byte Art-Net DMX-Paket (Header, OpCode, Version, Payload)")
    void buildDmxPacket_validHeaderAndStructure() {
        byte[] dmxData = new byte[512];
        dmxData[0] = (byte) 255; // Kanal 1 voll
        dmxData[511] = (byte) 128; // Kanal 512

        byte[] packet = ArtNetPacketBuilder.buildDmxPacket(
                0, // Net
                1, // Subnet
                2, // Universe
                5, // Sequence
                dmxData
        );

        assertThat(packet).hasSize(530);

        // 1. Header: "Art-Net\0"
        byte[] expectedId = new byte[]{'A', 'r', 't', '-', 'N', 'e', 't', 0x00};
        for (int i = 0; i < 8; i++) {
            assertThat(packet[i]).isEqualTo(expectedId[i]);
        }

        // 2. OpCode: 0x5000 (Little Endian: 0x00, 0x50)
        assertThat(packet[8]).isEqualTo((byte) 0x00);
        assertThat(packet[9]).isEqualTo((byte) 0x50);

        // 3. ProtVer: 14 (Big Endian: 0x00, 0x0E)
        assertThat(packet[10]).isEqualTo((byte) 0x00);
        assertThat(packet[11]).isEqualTo((byte) 0x0E);

        // 4. Sequence & Physical
        assertThat(packet[12]).isEqualTo((byte) 5);
        assertThat(packet[13]).isEqualTo((byte) 0x00);

        // 5. SubUni: Subnet 1, Uni 2 -> (1 << 4) | 2 = 0x12
        assertThat(packet[14]).isEqualTo((byte) 0x12);

        // 6. Net: 0
        assertThat(packet[15]).isEqualTo((byte) 0x00);

        // 7. Length: 512 (Big Endian: 0x02, 0x00)
        assertThat(packet[16]).isEqualTo((byte) 0x02);
        assertThat(packet[17]).isEqualTo((byte) 0x00);

        // 8. DMX Payload
        assertThat(packet[18] & 0xFF).isEqualTo(255);
        assertThat(packet[529] & 0xFF).isEqualTo(128);
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0, 0x00",
            "0, 15, 0x0F",
            "1, 0, 0x10",
            "3, 7, 0x37",
            "15, 15, 0xFF"
    })
    @DisplayName("Subnet und Universe werden korrekt im SubUni-Byte kodiert")
    void buildDmxPacket_subUniEncoding(int subnet, int universe, int expectedHex) {
        byte[] packet = ArtNetPacketBuilder.buildDmxPacket(0, subnet, universe, 0, new byte[512]);
        assertThat(packet[14] & 0xFF).isEqualTo(expectedHex);
    }

    @Test
    @DisplayName("DMX-Daten mit weniger als 512 Bytes werden sauber auf 512 Bytes aufgefüllt")
    void buildDmxPacket_padsShortPayload() {
        byte[] shortDmx = new byte[]{10, 20, 30};
        byte[] packet = ArtNetPacketBuilder.buildDmxPacket(0, 0, 0, 0, shortDmx);

        assertThat(packet).hasSize(530);
        assertThat(packet[18]).isEqualTo((byte) 10);
        assertThat(packet[19]).isEqualTo((byte) 20);
        assertThat(packet[20]).isEqualTo((byte) 30);
        assertThat(packet[21]).isZero();
        assertThat(packet[529]).isZero();
    }

    @Test
    @DisplayName("Null als DMX-Daten erzeugt ein leeres 512-Byte Universum ohne Absturz")
    void buildDmxPacket_handlesNullDmxData() {
        byte[] packet = ArtNetPacketBuilder.buildDmxPacket(0, 0, 0, 0, null);
        assertThat(packet).hasSize(530);
        for (int i = 18; i < 530; i++) {
            assertThat(packet[i]).isZero();
        }
    }
}

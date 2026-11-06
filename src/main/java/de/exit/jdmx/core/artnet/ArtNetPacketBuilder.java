package de.exit.jdmx.core.artnet;

import java.util.Arrays;

/**
 * Erstellt binär-konforme Art-Net 4 DMX-Pakete (OpDmx / OpOutput, OpCode {@code 0x5000})
 * gemäß der offiziellen Artistic Licence Art-Net Spezifikation.
 * <p>
 * Paketaufbau (Gesamtlänge 530 Bytes):
 * <ul>
 *     <li>Bytes 0–7: Identifikation {@code "Art-Net\0"}</li>
 *     <li>Bytes 8–9: OpCode {@code 0x5000} (Little Endian: {@code 0x00, 0x50})</li>
 *     <li>Bytes 10–11: Protokoll-Version 14 (Big Endian: {@code 0x00, 0x0E})</li>
 *     <li>Byte 12: Sequenzzähler (1–255 oder 0 bei Deaktivierung)</li>
 *     <li>Byte 13: Physikalischer Eingangs-Port (0)</li>
 *     <li>Byte 14: SubUni (Subnetz 0–15 in Bits 4–7, Universum 0–15 in Bits 0–3)</li>
 *     <li>Byte 15: Net (0–127)</li>
 *     <li>Bytes 16–17: Datenlänge 512 (Big Endian: {@code 0x02, 0x00})</li>
 *     <li>Bytes 18–529: 512 Bytes DMX512-Nutzdaten</li>
 * </ul>
 * </p>
 */
public final class ArtNetPacketBuilder {

    /** Standard-UDP-Port für Art-Net Kommunikation. */
    public static final int ARTNET_PORT = 6454;

    /** Fester Header-String gemäß Art-Net Spezifikation. */
    private static final byte[] ARTNET_HEADER = new byte[]{'A', 'r', 't', '-', 'N', 'e', 't', 0x00};

    /** Art-Net OpCode für DMX-Ausgabe (OpOutput / OpDmx = 0x5000). */
    private static final int OP_CODE_DMX = 0x5000;

    /** Protokoll-Revision (Art-Net 4 = Revision 14). */
    private static final int PROTOCOL_VERSION = 14;

    /** Feste DMX-Puffergröße für 512 Kanäle. */
    private static final int DMX_LENGTH = 512;

    /** Gesamte Paketgröße: 18 Bytes Header + 512 Bytes Nutzdaten. */
    public static final int PACKET_SIZE = 18 + DMX_LENGTH;

    private ArtNetPacketBuilder() {
        // Utility-Klasse, keine Instanziierung
    }

    /**
     * Baut ein vollständiges 530-Byte Art-Net DMX-Paket.
     *
     * @param net      Art-Net Net (0 bis 127)
     * @param subnet   Art-Net Subnet (0 bis 15)
     * @param universe Art-Net Universe (0 bis 15)
     * @param sequence Fortlaufende Sequenznummer (0 bis 255)
     * @param dmxData  512-Byte Array mit DMX-Werten (falls null oder kürzer, wird automatisch gepaddet)
     * @return Vollständiges 530-Byte Array für die UDP-Übertragung
     */
    public static byte[] buildDmxPacket(int net, int subnet, int universe, int sequence, byte[] dmxData) {
        byte[] packet = new byte[PACKET_SIZE];

        // 1. Identifikation: "Art-Net\0"
        System.arraycopy(ARTNET_HEADER, 0, packet, 0, 8);

        // 2. OpCode: 0x5000 (Little Endian)
        packet[8] = (byte) (OP_CODE_DMX & 0xFF);
        packet[9] = (byte) ((OP_CODE_DMX >> 8) & 0xFF);

        // 3. Protokollversion: 14 (Big Endian)
        packet[10] = (byte) ((PROTOCOL_VERSION >> 8) & 0xFF);
        packet[11] = (byte) (PROTOCOL_VERSION & 0xFF);

        // 4. Sequenz & Physical
        packet[12] = (byte) (sequence & 0xFF);
        packet[13] = 0x00;

        // 5. SubUni: (Subnet << 4) | (Universe & 0x0F)
        int subUni = ((subnet & 0x0F) << 4) | (universe & 0x0F);
        packet[14] = (byte) (subUni & 0xFF);

        // 6. Net: (Net & 0x7F)
        packet[15] = (byte) (net & 0x7F);

        // 7. Datenlänge: 512 (Big Endian: 0x02, 0x00)
        packet[16] = (byte) ((DMX_LENGTH >> 8) & 0xFF);
        packet[17] = (byte) (DMX_LENGTH & 0xFF);

        // 8. DMX Payload
        if (dmxData != null) {
            int copyLen = Math.min(dmxData.length, DMX_LENGTH);
            System.arraycopy(dmxData, 0, packet, 18, copyLen);
        }

        return packet;
    }
}

package de.exit.jdmx.core.artnet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Integrationstests für den UDP-Netzwerk-Sender {@link ArtNetSender}.
 */
class ArtNetSenderTest {

    private ArtNetSender sender;
    private DatagramSocket testReceiverSocket;
    private int receiverPort;

    @BeforeEach
    void setUp() throws IOException {
        // Freier lokaler UDP-Port für den Empfängertest
        testReceiverSocket = new DatagramSocket(0, InetAddress.getByName("127.0.0.1"));
        testReceiverSocket.setSoTimeout(2000);
        receiverPort = testReceiverSocket.getLocalPort();

        sender = new ArtNetSender();
    }

    @AfterEach
    void tearDown() {
        if (sender != null) {
            sender.close();
        }
        if (testReceiverSocket != null && !testReceiverSocket.isClosed()) {
            testReceiverSocket.close();
        }
    }

    @Test
    @DisplayName("Sendet ein 530-Byte Art-Net UDP-Paket erfolgreich an die Ziel-IP und Port")
    void sendDmx_sendsDatagramSuccessfully() throws Exception {
        byte[] dmxData = new byte[512];
        dmxData[0] = (byte) 200; // Kanal 1
        dmxData[10] = (byte) 150; // Kanal 11

        sender.sendDmx("127.0.0.1", receiverPort, 0, 0, 1, dmxData);

        byte[] rxBuffer = new byte[1024];
        DatagramPacket rxPacket = new DatagramPacket(rxBuffer, rxBuffer.length);
        testReceiverSocket.receive(rxPacket);

        assertThat(rxPacket.getLength()).isEqualTo(530);

        // Header prüfen
        assertThat(new String(rxBuffer, 0, 7)).isEqualTo("Art-Net");
        // DMX Payload prüfen
        assertThat(rxBuffer[18] & 0xFF).isEqualTo(200);
        assertThat(rxBuffer[28] & 0xFF).isEqualTo(150);
    }

    @Test
    @DisplayName("Inkrementiert die Sequenznummer pro Frame automatisch von 1 bis 255")
    void sendDmx_incrementsSequenceNumber() throws Exception {
        byte[] dmxData = new byte[512];

        // 1. Frame
        sender.sendDmx("127.0.0.1", receiverPort, 0, 0, dmxData);
        byte[] rxBuffer1 = new byte[600];
        DatagramPacket p1 = new DatagramPacket(rxBuffer1, rxBuffer1.length);
        testReceiverSocket.receive(p1);
        int seq1 = rxBuffer1[12] & 0xFF;

        // 2. Frame
        sender.sendDmx("127.0.0.1", receiverPort, 0, 0, dmxData);
        byte[] rxBuffer2 = new byte[600];
        DatagramPacket p2 = new DatagramPacket(rxBuffer2, rxBuffer2.length);
        testReceiverSocket.receive(p2);
        int seq2 = rxBuffer2[12] & 0xFF;

        assertThat(seq1).isGreaterThanOrEqualTo(1);
        assertThat(seq2).isEqualTo((seq1 % 255) + 1);
    }
}

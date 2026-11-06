package de.exit.jdmx.service;

import de.exit.jdmx.core.artnet.ArtNetSender;
import de.exit.jdmx.core.dmx.DmxUniverse;
import de.exit.jdmx.core.model.*;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TDD Unit- und Integrationstests für den zentralen {@link DmxService}.
 */
class DmxServiceTest {

    private DmxService dmxService;
    private FixtureInstance testMovingHead;
    private FixtureInstance testRgbPar;
    private DatagramSocket testReceiverSocket;
    private int receiverPort;

    @BeforeEach
    void setUp() throws Exception {
        // Freier lokaler UDP-Socket als Art-Net Testempfänger
        testReceiverSocket = new DatagramSocket(0, InetAddress.getByName("127.0.0.1"));
        testReceiverSocket.setSoTimeout(1500);
        receiverPort = testReceiverSocket.getLocalPort();

        dmxService = new DmxService();
        // Universum 1 (Index 0) auf lokalen Test-Empfänger konfigurieren
        UniverseConfig config = UniverseConfig.defaultFor(0)
                .withArtNetIp("127.0.0.1")
                .withArtNetPort(receiverPort)
                .withOutputTarget("Art-Net (127.0.0.1, Uni 0)");
        dmxService.setUniverseConfig(0, config);

        // Test-Fixture 1: 16-Bit Moving Head (Pan, PanFine, Tilt, TiltFine, Dimmer, Shutter, Red, Green, Blue)
        List<ChannelDefinition> mhChannels = List.of(
                new ChannelDefinition(1, "Pan", ChannelType.PAN, 128, false),
                new ChannelDefinition(2, "Pan Fine", ChannelType.PAN_FINE, 0, true),
                new ChannelDefinition(3, "Tilt", ChannelType.TILT, 128, false),
                new ChannelDefinition(4, "Tilt Fine", ChannelType.TILT_FINE, 0, true),
                new ChannelDefinition(5, "Dimmer", ChannelType.DIMMER, 0, false),
                new ChannelDefinition(6, "Shutter", ChannelType.SHUTTER, 255, false),
                new ChannelDefinition(7, "Red", ChannelType.RED, 255, false),
                new ChannelDefinition(8, "Green", ChannelType.GREEN, 255, false),
                new ChannelDefinition(9, "Blue", ChannelType.BLUE, 255, false)
        );
        DmxMode mhMode = new DmxMode("9CH", mhChannels);
        FixtureDefinition mhDef = new FixtureDefinition("mh1", "Generic", "Spot", ExplorerItemType.FIXTURE, List.of(mhMode));
        testMovingHead = new FixtureInstance("f1", "MH Spot 1", mhDef, mhMode, 1, 1, 100, 100);

        // Test-Fixture 2: Einfacher RGB Par auf DMX-Startadresse 20
        List<ChannelDefinition> rgbChannels = List.of(
                new ChannelDefinition(1, "Dimmer", ChannelType.DIMMER, 0, false),
                new ChannelDefinition(2, "Red", ChannelType.RED, 0, false),
                new ChannelDefinition(3, "Green", ChannelType.GREEN, 0, false),
                new ChannelDefinition(4, "Blue", ChannelType.BLUE, 0, false)
        );
        DmxMode rgbMode = new DmxMode("4CH", rgbChannels);
        FixtureDefinition rgbDef = new FixtureDefinition("par1", "Generic", "Par", ExplorerItemType.FIXTURE, List.of(rgbMode));
        testRgbPar = new FixtureInstance("f2", "Par 1", rgbDef, rgbMode, 1, 20, 200, 100);
    }

    @AfterEach
    void tearDown() {
        if (dmxService != null) {
            dmxService.close();
        }
        if (testReceiverSocket != null && !testReceiverSocket.isClosed()) {
            testReceiverSocket.close();
        }
    }

    @Test
    @DisplayName("DmxService initialisiert 4 unabhängige Universen")
    void initialization_hasFourUniverses() {
        assertThat(dmxService.getUniverseCount()).isEqualTo(4);
        for (int i = 0; i < 4; i++) {
            DmxUniverse uni = dmxService.getUniverse(i);
            assertThat(uni).isNotNull();
            assertThat(uni.getUniverseIndex()).isEqualTo(i);
        }
    }

    @Test
    @DisplayName("setIntensity setzt den DMX-Dimmerkanal des Fixtures präzise (0-100% -> 0-255)")
    void setIntensity_mapsCorrectlyToDimmerChannel() {
        // 100% Intensität auf Moving Head (Startadresse 1, Dimmer ist Kanal 5)
        dmxService.setIntensity(testMovingHead, 1.0);
        assertThat(dmxService.getUniverse(0).getChannel(5)).isEqualTo(255);

        // 50% Intensität
        dmxService.setIntensity(testMovingHead, 0.5);
        assertThat(dmxService.getUniverse(0).getChannel(5)).isEqualTo(128);

        // 0% Intensität
        dmxService.setIntensity(testMovingHead, 0.0);
        assertThat(dmxService.getUniverse(0).getChannel(5)).isEqualTo(0);

        // RGB Par auf Startadresse 20 (Dimmer ist Kanal 1 -> absolute DMX-Adresse 20)
        dmxService.setIntensity(testRgbPar, 0.75);
        assertThat(dmxService.getUniverse(0).getChannel(20)).isEqualTo((int) Math.round(0.75 * 255));
    }

    @Test
    @DisplayName("setColor setzt die DMX-Kanäle RED, GREEN und BLUE entsprechend der Farbe")
    void setColor_mapsCorrectlyToRgbChannels() {
        // Reines Cyan: R=0, G=255, B=255 auf Moving Head (Red=Kanal 7, Green=Kanal 8, Blue=Kanal 9)
        dmxService.setColor(testMovingHead, Color.CYAN);

        DmxUniverse uni = dmxService.getUniverse(0);
        assertThat(uni.getChannel(7)).isEqualTo(0);    // Red
        assertThat(uni.getChannel(8)).isEqualTo(255);  // Green
        assertThat(uni.getChannel(9)).isEqualTo(255);  // Blue

        // Gelb auf Par (Startadresse 20: Red=21, Green=22, Blue=23)
        dmxService.setColor(testRgbPar, Color.YELLOW);
        assertThat(uni.getChannel(21)).isEqualTo(255); // Red
        assertThat(uni.getChannel(22)).isEqualTo(255); // Green
        assertThat(uni.getChannel(23)).isEqualTo(0);   // Blue
    }

    @Test
    @DisplayName("setPosition berechnet 16-Bit Pan und Tilt (Coarse + Fine)")
    void setPosition_calculates16BitPanTilt() {
        // Pan=50% (Center: 32768 -> Coarse=128, Fine=0)
        // Tilt=100% (65535 -> Coarse=255, Fine=255)
        dmxService.setPosition(testMovingHead, 0.5, 1.0);

        DmxUniverse uni = dmxService.getUniverse(0);
        assertThat(uni.getChannel(1)).isEqualTo(128); // Pan Coarse
        assertThat(uni.getChannel(2)).isEqualTo(0);   // Pan Fine
        assertThat(uni.getChannel(3)).isEqualTo(255); // Tilt Coarse
        assertThat(uni.getChannel(4)).isEqualTo(255); // Tilt Fine
    }

    @Test
    @DisplayName("Blackout setzt alle Kanäle aller Universen auf 0")
    void blackout_clearsAllUniverses() {
        dmxService.setIntensity(testMovingHead, 1.0);
        dmxService.setColor(testMovingHead, Color.WHITE);

        dmxService.blackout();

        for (int i = 1; i <= 512; i++) {
            assertThat(dmxService.getUniverse(0).getChannel(i)).isZero();
        }
    }

    @Test
    @DisplayName("setIntensity aktiviert bei RGBW-Geräten auch Weiß/Farbe und setzt Dimmer auf 255 bei 1.0 (Lumos) sowie 0 bei 0.0 (Nox)")
    void setIntensity_handlesRgbwFixturesCorrectly() {
        List<ChannelDefinition> ukingChannels = List.of(
                new ChannelDefinition(1, "Pan", ChannelType.PAN, 0, false),
                new ChannelDefinition(2, "Pan fine", ChannelType.PAN_FINE, 0, true),
                new ChannelDefinition(3, "Tilt", ChannelType.TILT, 0, false),
                new ChannelDefinition(4, "Tilt fine", ChannelType.TILT_FINE, 0, true),
                new ChannelDefinition(5, "P/T speed", ChannelType.SPEED, 0, false),
                new ChannelDefinition(6, "Dimming", ChannelType.DIMMER, 0, false),
                new ChannelDefinition(7, "Strobe", ChannelType.SHUTTER, 0, false),
                new ChannelDefinition(8, "Red", ChannelType.RED, 0, false),
                new ChannelDefinition(9, "Green", ChannelType.GREEN, 0, false),
                new ChannelDefinition(10, "Blue", ChannelType.BLUE, 0, false),
                new ChannelDefinition(11, "White", ChannelType.WHITE, 0, false),
                new ChannelDefinition(12, "Mode", ChannelType.EFFECT, 0, false),
                new ChannelDefinition(13, "Reset", ChannelType.MAINTENANCE, 0, false)
        );
        DmxMode ukingMode = new DmxMode("13 Channel", ukingChannels);
        FixtureDefinition ukingDef = new FixtureDefinition("uking-zq-02042", "UKing", "ZQ-02042", ExplorerItemType.FIXTURE, List.of(ukingMode));
        FixtureInstance ukingFixture = new FixtureInstance("uking-1", "UKing ZQ-02042", ukingDef, ukingMode, 1, 1, 0, 0);

        // Lumos (100%): Dimmer auf 255. Weil RGBW noch 0 waren, muss auch Weiß (Kanal 11) auf 255 aktiviert werden!
        dmxService.setIntensity(ukingFixture, 1.0);

        DmxUniverse uni = dmxService.getUniverse(0);
        assertThat(uni.getChannel(6)).isEqualTo(255); // Dimming
        assertThat(uni.getChannel(11)).isEqualTo(255); // White aktiviert!
        assertThat(uni.getChannel(7)).isEqualTo(0);   // Strobe darf NICHT auf 255 stehen (kein ungewolltes Blitzen)

        // Nox (0%): Dimmer auf 0
        dmxService.setIntensity(ukingFixture, 0.0);
        assertThat(uni.getChannel(6)).isEqualTo(0); // Dimming aus!
    }

    @Test
    @DisplayName("sendUniverse sendet DMX-Daten per UDP an die konfigurierte Art-Net IP")
    void sendUniverse_transmitsOverUdp() throws Exception {
        dmxService.getUniverse(0).setChannel(5, 255); // Dimmer
        dmxService.getUniverse(0).setChannel(7, 255); // Red

        dmxService.sendUniverse(0);

        byte[] rxBuffer = new byte[1024];
        DatagramPacket rxPacket = new DatagramPacket(rxBuffer, rxBuffer.length);
        testReceiverSocket.receive(rxPacket);

        assertThat(rxPacket.getLength()).isEqualTo(530);
        assertThat(new String(rxBuffer, 0, 7)).isEqualTo("Art-Net");

        // Dimmer (Kanal 5 -> Byte Index 18 + 4 = 22)
        assertThat(rxBuffer[22] & 0xFF).isEqualTo(255);
        // Red (Kanal 7 -> Byte Index 18 + 6 = 24)
        assertThat(rxBuffer[24] & 0xFF).isEqualTo(255);
    }

    @Test
    @DisplayName("Paketzähler misst reale Übertragungen und berechnet DMX-Rate")
    void packetRateCounter_measuresActualTransmissionRate() throws Exception {
        int initialCount = dmxService.getTransmittedPacketCount();
        dmxService.sendUniverse(0);
        dmxService.sendUniverse(0);

        assertThat(dmxService.getTransmittedPacketCount()).isEqualTo(initialCount + 2);

        int rate = dmxService.measureAndResetRate();
        assertThat(rate).isGreaterThanOrEqualTo(2);
        assertThat(dmxService.getCurrentRate()).isEqualTo(rate);
    }
}

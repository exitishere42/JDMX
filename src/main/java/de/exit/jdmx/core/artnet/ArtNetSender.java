package de.exit.jdmx.core.artnet;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Performanter UDP-Socket-Sender für Art-Net 4 DMX-Pakete.
 * <p>
 * Versendet binär-konforme 530-Byte Datagramme an konfigurierte Ziel-IP-Adressen
 * (Unicast) oder Subnetz-Broadcasts auf dem Art-Net Standard-Port 6454.
 * </p>
 * <p>
 * Thread-Sicherheits-Garantie: Diese Klasse ist vollständig thread-sicher und für
 * den parallelen Aufruf durch den 40-Hz-Frame-Loop ausgelegt.
 * </p>
 */
public class ArtNetSender implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(ArtNetSender.class.getName());

    private final DatagramSocket socket;
    private final AtomicInteger sequenceCounter = new AtomicInteger(1);
    private volatile boolean closed = false;

    /**
     * Erstellt einen neuen ArtNetSender mit einem Standard-DatagramSocket.
     *
     * @throws SocketException falls der Socket nicht geöffnet werden kann
     */
    public ArtNetSender() throws SocketException {
        this.socket = new DatagramSocket();
        this.socket.setBroadcast(true);
    }

    /**
     * Sendet ein DMX512-Universum über Art-Net (Net = 0).
     *
     * @param targetIp   Ziel-IP-Adresse (z. B. "127.0.0.1" oder "192.168.1.255")
     * @param targetPort UDP-Zielport (Standard: 6454)
     * @param subnet     Art-Net Subnetz (0 bis 15)
     * @param universe   Art-Net Universum (0 bis 15)
     * @param dmxData    512 Bytes DMX-Werte
     * @throws IOException bei Netzwerkübertragungsfehlern
     */
    public void sendDmx(String targetIp, int targetPort, int subnet, int universe, byte[] dmxData) throws IOException {
        sendDmx(targetIp, targetPort, 0, subnet, universe, dmxData);
    }

    /**
     * Sendet ein DMX512-Universum mit vollständiger Art-Net 4 Adressierung (Net, Subnet, Universe).
     *
     * @param targetIp   Ziel-IP-Adresse
     * @param targetPort UDP-Zielport
     * @param net        Art-Net Net (0 bis 127)
     * @param subnet     Art-Net Subnetz (0 bis 15)
     * @param universe   Art-Net Universum (0 bis 15)
     * @param dmxData    512 Bytes DMX-Werte
     * @throws IOException bei Netzwerkübertragungsfehlern
     */
    public void sendDmx(String targetIp, int targetPort, int net, int subnet, int universe, byte[] dmxData) throws IOException {
        if (closed) {
            return;
        }
        Objects.requireNonNull(targetIp, "targetIp darf nicht null sein");

        int seq = nextSequence();
        byte[] payload = ArtNetPacketBuilder.buildDmxPacket(net, subnet, universe, seq, dmxData);

        InetAddress destination = InetAddress.getByName(targetIp);
        DatagramPacket packet = new DatagramPacket(payload, payload.length, destination, targetPort);

        socket.send(packet);
    }

    /**
     * Ermittelt die nächste fortlaufende Sequenznummer (1 bis 255).
     */
    private int nextSequence() {
        return sequenceCounter.getAndUpdate(current -> (current % 255) + 1);
    }

    /**
     * Prüft, ob der Socket noch geöffnet und sendebereit ist.
     *
     * @return true wenn aktiv
     */
    public boolean isOpen() {
        return !closed && !socket.isClosed();
    }

    @Override
    public void close() {
        closed = true;
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Fehler beim Schließen des Art-Net Sockets", e);
        }
    }
}

package de.exit.jdmx.core.dmx;

import java.util.Arrays;

/**
 * Repräsentiert ein einzelnes DMX512-Universum mit 512 Datenkanälen.
 * <p>
 * Bietet atomaren, thread-sicheren Zugriff auf DMX-Kanalwerte (1-basiert, 1 bis 512)
 * im Wertebereich von 0 bis 255.
 * </p>
 * <p>
 * Thread-Sicherheits-Garantie: Alle Lese- und Schreiboperationen sind intern
 * synchronisiert, sodass parallele Zugriffe durch die JavaFX-UI und den 40-Hz-Frame-Loop
 * stets konsistente DMX-Pufferzustände garantieren.
 * </p>
 */
public class DmxUniverse {

    /** Anzahl der Kanäle pro DMX512-Universum. */
    public static final int CHANNEL_COUNT = 512;

    private final int universeIndex;
    private final byte[] channels = new byte[CHANNEL_COUNT];
    private final Object lock = new Object();

    /**
     * Erstellt ein neues Universum mit dem angegebenen Null-basierten Index.
     *
     * @param universeIndex Null-basierter Index des Universums (z. B. 0 für Universum 1)
     */
    public DmxUniverse(int universeIndex) {
        this.universeIndex = universeIndex;
    }

    /**
     * Liefert den Null-basierten Index dieses Universums.
     *
     * @return Universums-Index (0 bis 3)
     */
    public int getUniverseIndex() {
        return universeIndex;
    }

    /**
     * Setzt den DMX-Wert für einen bestimmten Kanal.
     *
     * @param channel 1-basierte Kanalnummer (1 bis 512)
     * @param value   DMX-Wert (0 bis 255)
     * @throws IllegalArgumentException falls der Kanal oder Wert außerhalb des gültigen Bereichs liegt
     */
    public void setChannel(int channel, int value) {
        validateChannel(channel);
        validateValue(value);
        synchronized (lock) {
            channels[channel - 1] = (byte) (value & 0xFF);
        }
    }

    /**
     * Liest den aktuellen DMX-Wert eines Kanals aus.
     *
     * @param channel 1-basierte Kanalnummer (1 bis 512)
     * @return Aktueller Wert des Kanals (0 bis 255)
     * @throws IllegalArgumentException falls die Kanalnummer außerhalb von 1 bis 512 liegt
     */
    public int getChannel(int channel) {
        validateChannel(channel);
        synchronized (lock) {
            return channels[channel - 1] & 0xFF;
        }
    }

    /**
     * Schreibt mehrere aufeinanderfolgende Kanalwerte in den Universumspuffer (Bulk-Update).
     *
     * @param startChannel 1-basierte Startkanalnummer (1 bis 512)
     * @param values       Array mit DMX-Werten
     * @throws IllegalArgumentException falls der Adressbereich den Puffer von 512 Kanälen übersteigt
     */
    public void setChannels(int startChannel, byte[] values) {
        validateChannel(startChannel);
        if (values == null) {
            return;
        }
        if (startChannel + values.length - 1 > CHANNEL_COUNT) {
            throw new IllegalArgumentException("DMX-Kanalbereich übersteigt 512 Kanäle: Start=" +
                    startChannel + ", Länge=" + values.length);
        }
        synchronized (lock) {
            System.arraycopy(values, 0, channels, startChannel - 1, values.length);
        }
    }

    /**
     * Liefert eine defensive Kopie des gesamten 512-Byte DMX-Puffers für die Netzwerksendung.
     *
     * @return 512-Byte Array mit DMX-Werten
     */
    public byte[] getDmxData() {
        synchronized (lock) {
            return Arrays.copyOf(channels, CHANNEL_COUNT);
        }
    }

    /**
     * Setzt alle 512 Kanäle dieses Universums sofort auf den Wert 0 zurück (Blackout).
     */
    public void blackout() {
        synchronized (lock) {
            Arrays.fill(channels, (byte) 0);
        }
    }

    private static void validateChannel(int channel) {
        if (channel < 1 || channel > CHANNEL_COUNT) {
            throw new IllegalArgumentException("Ungültige DMX-Kanalnummer: " + channel +
                    " (erwartet: 1 bis " + CHANNEL_COUNT + ")");
        }
    }

    private static void validateValue(int value) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException("Ungültiger DMX-Wert: " + value +
                    " (erwartet: 0 bis 255)");
        }
    }
}

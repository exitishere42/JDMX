package de.exit.jdmx.service;

import de.exit.jdmx.core.artnet.ArtNetSender;
import de.exit.jdmx.core.dmx.DmxUniverse;
import de.exit.jdmx.core.log.AppLogger;
import de.exit.jdmx.core.model.ChannelDefinition;
import de.exit.jdmx.core.model.ChannelType;
import de.exit.jdmx.core.model.FixtureInstance;
import de.exit.jdmx.core.model.UniverseConfig;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.net.SocketException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Zentraler DMX- und Art-Net-Service der Core-Schicht (Reines MVC).
 * <p>
 * Verwaltet 4 DMX512-Universen, führt das Mapping von Fixture-Parametern
 * (Intensität, RGB-Farbe, Pan/Tilt-Position, Strobo) auf absolute DMX-Kanaladressen durch
 * und sendet kontinuierliche Art-Net 4 UDP-Frames (Standard 40 Hz) an die in
 * {@link UniverseConfig} konfigurierten Ziel-Netzwerkadressen.
 * </p>
 * <p>
 * Thread-Sicherheits-Garantie: Vollständig thread-sicher. Benutzereingaben aus der
 * JavaFX-UI werden thread-sicher in die Universumspuffer geschrieben und sofort
 * sowie im 40-Hz-Takt über UDP emittiert.
 * </p>
 */
public class DmxService implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(DmxService.class.getName());

    /** Standard-Anzahl verwalteter Universen. */
    public static final int UNIVERSE_COUNT = 4;

    /**
     * Listener für Echtzeit-Kanaländerungen in einem DMX-Universum.
     */
    @FunctionalInterface
    public interface ChannelChangeListener {
        void onChannelChanged(int universeIndex, int channel, int value);
    }

    private final DmxUniverse[] universes = new DmxUniverse[UNIVERSE_COUNT];
    private final UniverseConfig[] configs = new UniverseConfig[UNIVERSE_COUNT];
    private final ArtNetSender artNetSender;
    private final ScheduledExecutorService frameLoopExecutor;
    private final List<ChannelChangeListener> channelChangeListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    private final java.util.concurrent.atomic.AtomicInteger packetCounter = new java.util.concurrent.atomic.AtomicInteger(0);
    private final java.util.concurrent.atomic.AtomicInteger currentRate = new java.util.concurrent.atomic.AtomicInteger(0);
    private java.util.function.Consumer<Integer> rateUpdateListener;

    private volatile boolean frameLoopRunning = false;

    /**
     * Erstellt einen neuen DmxService mit 4 Universen, ArtNetSender und 40-Hz-Frame-Loop.
     */
    public DmxService() {
        this(createDefaultSender());
    }

    /**
     * Erstellt einen DmxService mit einem benutzerdefinierten {@link ArtNetSender} (z. B. für Tests).
     *
     * @param artNetSender Art-Net UDP-Sender
     */
    public DmxService(ArtNetSender artNetSender) {
        this.artNetSender = Objects.requireNonNull(artNetSender, "artNetSender darf nicht null sein");

        for (int i = 0; i < UNIVERSE_COUNT; i++) {
            universes[i] = new DmxUniverse(i);
            universes[i].setChangeListener(this::notifyChannelChanged);
            configs[i] = UniverseConfig.defaultFor(i);
        }

        this.frameLoopExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Dmx-FrameLoop-40Hz");
            t.setDaemon(true);
            return t;
        });

        startFrameLoop();
    }

    private static ArtNetSender createDefaultSender() {
        try {
            return new ArtNetSender();
        } catch (SocketException e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Erstellen des ArtNetSender-Sockets", e);
            throw new RuntimeException("Art-Net UDP Socket konnte nicht geöffnet werden", e);
        }
    }

    /**
     * Liefert die Anzahl der verwalteten DMX-Universen (immer 4).
     */
    public int getUniverseCount() {
        return UNIVERSE_COUNT;
    }

    /**
     * Liefert das Universum für den angegebenen Null-basierten Index (0 bis 3).
     *
     * @param universeIndex Index des Universums (0 bis 3)
     * @return Das {@link DmxUniverse}
     */
    public DmxUniverse getUniverse(int universeIndex) {
        validateUniverseIndex(universeIndex);
        return universes[universeIndex];
    }

    /**
     * Liefert die Konfiguration für ein Universum.
     *
     * @param universeIndex Index des Universums (0 bis 3)
     * @return Die {@link UniverseConfig}
     */
    public UniverseConfig getUniverseConfig(int universeIndex) {
        validateUniverseIndex(universeIndex);
        return configs[universeIndex];
    }

    /**
     * Aktualisiert die Netzwerkkonfiguration für ein Universum.
     *
     * @param universeIndex Index des Universums (0 bis 3)
     * @param config        Neue Universums-Konfiguration
     */
    public void setUniverseConfig(int universeIndex, UniverseConfig config) {
        validateUniverseIndex(universeIndex);
        configs[universeIndex] = Objects.requireNonNull(config, "config darf nicht null sein");
    }

    /**
     * Registriert einen Listener für Echtzeit-Kanaländerungen in allen Universen.
     *
     * @param listener Callback für geänderte Kanalwerte
     */
    public void addChannelChangeListener(ChannelChangeListener listener) {
        if (listener != null) {
            channelChangeListeners.add(listener);
        }
    }

    /**
     * Entfernt einen registrierten Kanal-Listener.
     *
     * @param listener Zu entfernender Listener
     */
    public void removeChannelChangeListener(ChannelChangeListener listener) {
        channelChangeListeners.remove(listener);
    }

    private void notifyChannelChanged(int universeIndex, int channel, int value) {
        for (ChannelChangeListener listener : channelChangeListeners) {
            try {
                listener.onChannelChanged(universeIndex, channel, value);
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Fehler in ChannelChangeListener", e);
            }
        }
    }

    /**
     * Setzt den DMX-Wert für einen einzelnen Kanal in einem bestimmten Universum und sendet diesen asynchron.
     *
     * @param universeIndex Null-basierter Index des Universums (0 bis 3)
     * @param channel       1-basierte Kanalnummer (1 bis 512)
     * @param value         DMX-Wert (0 bis 255)
     */
    public void setChannel(int universeIndex, int channel, int value) {
        validateUniverseIndex(universeIndex);
        AppLogger.dmx("setChannel -> Uni=%d, Ch=%d, Val=%d", universeIndex + 1, channel, value);
        universes[universeIndex].setChannel(channel, value);
        sendUniverseAsync(universeIndex);
    }

    /**
     * Liest den aktuellen DMX-Wert eines Kanals in einem bestimmten Universum.
     *
     * @param universeIndex Null-basierter Index des Universums (0 bis 3)
     * @param channel       1-basierte Kanalnummer (1 bis 512)
     * @return DMX-Wert (0 bis 255)
     */
    public int getChannel(int universeIndex, int channel) {
        validateUniverseIndex(universeIndex);
        return universes[universeIndex].getChannel(channel);
    }

    /**
     * Setzt die Intensität (Dimmer) für ein konkretes Scheinwerfer-Gerät.
     *
     * @param fixture   Scheinwerfer-Instanz
     * @param intensity Intensitätswert von 0.0 (0%) bis 1.0 (100%)
     */
    public void setIntensity(FixtureInstance fixture, double intensity) {
        if (fixture == null || fixture.getActiveMode() == null) {
            return;
        }

        int uniIdx = fixture.getUniverse() - 1;
        if (uniIdx < 0 || uniIdx >= UNIVERSE_COUNT) {
            return;
        }

        DmxUniverse universe = universes[uniIdx];
        int startAddress = fixture.getStartAddress();
        int dmxVal = (int) Math.round(clamp(intensity, 0.0, 1.0) * 255.0);
        AppLogger.dmx("setIntensity -> Fixture='%s' (Start=%d, Uni=%d) -> %.2f (DMX: %d)", fixture.getName(), startAddress, fixture.getUniverse(), intensity, dmxVal);

        boolean hasDimmer = false;
        boolean hasColorChannels = false;
        boolean allColorChannelsZero = true;

        for (ChannelDefinition ch : fixture.getActiveMode().getChannels()) {
            if (ch.type() == ChannelType.DIMMER) {
                hasDimmer = true;
                int absAddr = startAddress + ch.index() - 1;
                universe.setChannel(absAddr, dmxVal);
            } else if (ch.type() == ChannelType.RED || ch.type() == ChannelType.GREEN
                    || ch.type() == ChannelType.BLUE || ch.type() == ChannelType.WHITE) {
                hasColorChannels = true;
                int absAddr = startAddress + ch.index() - 1;
                if (universe.getChannel(absAddr) > 0) {
                    allColorChannelsZero = false;
                }
            } else if (ch.type() == ChannelType.SHUTTER && intensity > 0.0) {
                // Bei mechanischem Shutter sicherstellen (voll geöffnet = 255)
                // Bei Strobe-Kanälen ist 0 = "Kein Strobe (Open)", daher nicht auf 255 setzen
                boolean isStrobe = ch.name().toLowerCase(Locale.ROOT).contains("strobe");
                if (!isStrobe) {
                    int absAddr = startAddress + ch.index() - 1;
                    if (universe.getChannel(absAddr) == 0) {
                        universe.setChannel(absAddr, 255);
                    }
                }
            }
        }

        // Falls das Gerät Farbdioden besitzt:
        if (hasColorChannels) {
            if (!hasDimmer) {
                // Geräte ohne Dimmerkanal (z. B. einfache RGB-Pars) werden direkt über Farben gedimmt
                for (ChannelDefinition ch : fixture.getActiveMode().getChannels()) {
                    int absAddr = startAddress + ch.index() - 1;
                    if (ch.type() == ChannelType.RED || ch.type() == ChannelType.GREEN || ch.type() == ChannelType.BLUE || ch.type() == ChannelType.WHITE) {
                        universe.setChannel(absAddr, dmxVal);
                    }
                }
            } else if (intensity > 0.0 && allColorChannelsZero) {
                // RGB/RGBW-Geräte mit Dimmer: Wenn alle Farbkanäle bisher auf 0 standen,
                // aktivieren wir Weiß (oder RGB), damit die LEDs bei 'Lumos' / Helligkeit physisch leuchten!
                boolean hasWhite = fixture.getActiveMode().getChannels().stream().anyMatch(c -> c.type() == ChannelType.WHITE);
                for (ChannelDefinition ch : fixture.getActiveMode().getChannels()) {
                    int absAddr = startAddress + ch.index() - 1;
                    if (hasWhite && ch.type() == ChannelType.WHITE) {
                        universe.setChannel(absAddr, 255);
                    } else if (!hasWhite && (ch.type() == ChannelType.RED || ch.type() == ChannelType.GREEN || ch.type() == ChannelType.BLUE)) {
                        universe.setChannel(absAddr, 255);
                    }
                }
            }
        }

        sendUniverseAsync(uniIdx);
    }

    /**
     * Setzt die RGB-Farbe für ein konkretes Scheinwerfer-Gerät.
     *
     * @param fixture Scheinwerfer-Instanz
     * @param color   JavaFX-Farbe
     */
    public void setColor(FixtureInstance fixture, Color color) {
        if (fixture == null || fixture.getActiveMode() == null || color == null) {
            return;
        }

        int uniIdx = fixture.getUniverse() - 1;
        if (uniIdx < 0 || uniIdx >= UNIVERSE_COUNT) {
            return;
        }

        DmxUniverse universe = universes[uniIdx];
        int startAddress = fixture.getStartAddress();

        int r = (int) Math.round(clamp(color.getRed(), 0.0, 1.0) * 255.0);
        int g = (int) Math.round(clamp(color.getGreen(), 0.0, 1.0) * 255.0);
        int b = (int) Math.round(clamp(color.getBlue(), 0.0, 1.0) * 255.0);
        int w = Math.min(r, Math.min(g, b));
        AppLogger.dmx("setColor -> Fixture='%s' (Start=%d, Uni=%d) -> R=%d, G=%d, B=%d, W=%d", fixture.getName(), startAddress, fixture.getUniverse(), r, g, b, w);

        for (ChannelDefinition ch : fixture.getActiveMode().getChannels()) {
            int absAddr = startAddress + ch.index() - 1;
            if (ch.type() == ChannelType.RED) {
                universe.setChannel(absAddr, r);
            } else if (ch.type() == ChannelType.GREEN) {
                universe.setChannel(absAddr, g);
            } else if (ch.type() == ChannelType.BLUE) {
                universe.setChannel(absAddr, b);
            } else if (ch.type() == ChannelType.WHITE) {
                universe.setChannel(absAddr, w);
            }
        }

        sendUniverseAsync(uniIdx);
    }

    /**
     * Setzt die Position (Pan und Tilt) für ein Moving-Light Scheinwerfer-Gerät (8-Bit oder 16-Bit).
     *
     * @param fixture Scheinwerfer-Instanz
     * @param pan     Pan-Position von 0.0 bis 1.0 (0.5 = Mitte)
     * @param tilt    Tilt-Position von 0.0 bis 1.0 (0.5 = Mitte)
     */
    public void setPosition(FixtureInstance fixture, double pan, double tilt) {
        if (fixture == null || fixture.getActiveMode() == null) {
            return;
        }

        int uniIdx = fixture.getUniverse() - 1;
        if (uniIdx < 0 || uniIdx >= UNIVERSE_COUNT) {
            return;
        }

        DmxUniverse universe = universes[uniIdx];
        int startAddress = fixture.getStartAddress();

        boolean hasPanFine = fixture.getActiveMode().getChannels().stream()
                .anyMatch(ch -> ch.type() == ChannelType.PAN_FINE);
        boolean hasTiltFine = fixture.getActiveMode().getChannels().stream()
                .anyMatch(ch -> ch.type() == ChannelType.TILT_FINE);

        int panCoarse;
        int panFine;
        if (hasPanFine) {
            int pan16 = (int) Math.round(clamp(pan, 0.0, 1.0) * 65535.0);
            panCoarse = (pan16 >> 8) & 0xFF;
            panFine = pan16 & 0xFF;
        } else {
            panCoarse = (int) Math.round(clamp(pan, 0.0, 1.0) * 255.0);
            panFine = 0;
        }

        int tiltCoarse;
        int tiltFine;
        if (hasTiltFine) {
            int tilt16 = (int) Math.round(clamp(tilt, 0.0, 1.0) * 65535.0);
            tiltCoarse = (tilt16 >> 8) & 0xFF;
            tiltFine = tilt16 & 0xFF;
        } else {
            tiltCoarse = (int) Math.round(clamp(tilt, 0.0, 1.0) * 255.0);
            tiltFine = 0;
            AppLogger.dmx("setPosition -> Fixture='%s' (Start=%d, Uni=%d) -> Pan=%.2f, Tilt=%.2f", fixture.getName(), startAddress, fixture.getUniverse(), pan, tilt);
        }

        for (ChannelDefinition ch : fixture.getActiveMode().getChannels()) {
            int absAddr = startAddress + ch.index() - 1;
            if (ch.type() == ChannelType.PAN) {
                universe.setChannel(absAddr, panCoarse);
            } else if (ch.type() == ChannelType.PAN_FINE) {
                universe.setChannel(absAddr, panFine);
            } else if (ch.type() == ChannelType.TILT) {
                universe.setChannel(absAddr, tiltCoarse);
            } else if (ch.type() == ChannelType.TILT_FINE) {
                universe.setChannel(absAddr, tiltFine);
            }
        }

        sendUniverseAsync(uniIdx);
    }

    /**
     * Setzt die Stroboskop-Frequenz für ein Scheinwerfer-Gerät (0 bis 25 Hz).
     *
     * @param fixture  Scheinwerfer-Instanz
     * @param strobeHz Frequenz in Hz (0.0 = Strobe Aus / Shutter Open)
     */
    public void setStrobe(FixtureInstance fixture, double strobeHz) {
        if (fixture == null || fixture.getActiveMode() == null) {
            return;
        }

        int uniIdx = fixture.getUniverse() - 1;
        if (uniIdx < 0 || uniIdx >= UNIVERSE_COUNT) {
            return;
        }

        DmxUniverse universe = universes[uniIdx];
        int startAddress = fixture.getStartAddress();

        // 0 Hz = 0 (Kein Strobe / Off), 1-25 Hz = 10..240
        int dmxVal;
        if (strobeHz <= 0.01) {
            dmxVal = 0;
        } else {
            dmxVal = (int) Math.round(10.0 + (clamp(strobeHz, 1.0, 25.0) / 25.0) * 230.0);
        }

        for (ChannelDefinition ch : fixture.getActiveMode().getChannels()) {
            if (ch.type() == ChannelType.SHUTTER) {
                int absAddr = startAddress + ch.index() - 1;
                universe.setChannel(absAddr, dmxVal);
            }
        }

        sendUniverseAsync(uniIdx);
    }

    /**
     * Schaltet alle 4 Universen sofort dunkel (Blackout) und sendet die Null-Frames.
     */
    public void blackout() {
        for (int i = 0; i < UNIVERSE_COUNT; i++) {
            universes[i].blackout();
        }
        sendAllUniverses();
    }

    /**
     * Sendet das angegebene Universum synchron per Art-Net UDP.
     *
     * @param universeIndex Index des Universums (0 bis 3)
     * @throws IOException bei Netzwerkfehlern
     */
    public void sendUniverse(int universeIndex) throws IOException {
        validateUniverseIndex(universeIndex);
        UniverseConfig config = configs[universeIndex];
        byte[] dmxData = universes[universeIndex].getDmxData();

        artNetSender.sendDmx(
                config.artNetIp(),
                config.artNetPort(),
                config.artNetSubnet(),
                config.artNetUniverse(),
                dmxData
        );
        packetCounter.incrementAndGet();
    }

    /**
     * Liefert die Gesamtzahl der seit Start bzw. letztem Reset übertragenen Pakete.
     */
    public int getTransmittedPacketCount() {
        return packetCounter.get();
    }

    /**
     * Liefert die zuletzt gemessene DMX-Paketrate in Paketen pro Sekunde.
     */
    public int getCurrentRate() {
        return currentRate.get();
    }

    /**
     * Misst die seit dem letzten Aufruf aufgelaufenen Pakete und setzt den Zähler für die nächste Sekunde zurück.
     *
     * @return Gemessene Paketrate pro Sekunde
     */
    public int measureAndResetRate() {
        int rate = packetCounter.getAndSet(0);
        currentRate.set(rate);
        if (rateUpdateListener != null) {
            try {
                rateUpdateListener.accept(rate);
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Fehler im RateUpdateListener", e);
            }
        }
        return rate;
    }

    /**
     * Registriert einen Listener, der bei jeder sekündlichen Ratenberechnung benachrichtigt wird.
     *
     * @param listener Callback mit der aktuellen Rate in DMX/s
     */
    public void setOnRateUpdated(java.util.function.Consumer<Integer> listener) {
        this.rateUpdateListener = listener;
    }

    /**
     * Sendet das angegebene Universum asynchron (non-blocking für UI).
     *
     * @param universeIndex Index des Universums (0 bis 3)
     */
    public void sendUniverseAsync(int universeIndex) {
        frameLoopExecutor.execute(() -> {
            try {
                sendUniverse(universeIndex);
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Fehler beim asynchronen Senden von Universum " + universeIndex, e);
            }
        });
    }

    /**
     * Sendet alle 4 Universen synchron.
     */
    public void sendAllUniverses() {
        for (int i = 0; i < UNIVERSE_COUNT; i++) {
            try {
                sendUniverse(i);
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Fehler beim Senden von Universum " + i, e);
            }
        }
    }

    /**
     * Startet den 40-Hz-Frame-Loop (25 ms Taktzeit) sowie den sekündlichen Raten-Monitor.
     */
    public synchronized void startFrameLoop() {
        if (frameLoopRunning) {
            return;
        }
        frameLoopRunning = true;
        // 40 Hz = 25 ms Intervall
        frameLoopExecutor.scheduleAtFixedRate(this::tickFrameLoop, 25, 25, TimeUnit.MILLISECONDS);
        // Sekündliche Raten-Berechnung
        frameLoopExecutor.scheduleAtFixedRate(this::measureAndResetRate, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    private void tickFrameLoop() {
        if (!frameLoopRunning) {
            return;
        }
        for (int i = 0; i < UNIVERSE_COUNT; i++) {
            UniverseConfig config = configs[i];
            // Nur senden wenn Output nicht explizit "Kein Output" ist
            if (config.outputTarget() != null && !config.outputTarget().equals("Kein Output")) {
                try {
                    sendUniverse(i);
                } catch (Exception e) {
                    LOGGER.log(Level.FINE, "Frame-Loop Tick Fehler für Uni " + i, e);
                }
            }
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void validateUniverseIndex(int index) {
        if (index < 0 || index >= UNIVERSE_COUNT) {
            throw new IllegalArgumentException("Ungültiger Universums-Index: " + index + " (0 bis 3)");
        }
    }

    @Override
    public void close() {
        frameLoopRunning = false;
        frameLoopExecutor.shutdown();
        try {
            if (!frameLoopExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                frameLoopExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            frameLoopExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        artNetSender.close();
    }
}

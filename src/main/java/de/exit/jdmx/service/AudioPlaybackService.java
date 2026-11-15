package de.exit.jdmx.service;

import de.exit.jdmx.core.log.AppLogger;
import de.exit.jdmx.core.model.TimelineTrack;
import javazoom.jl.player.advanced.AdvancedPlayer;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service für die parallele Multitrack-Wiedergabe von Audio-Songs (MP3 und WAV) über die Systemsoundkarte.
 * <p>
 * Unterstützt:
 * <ul>
 *     <li><b>Multitrack Audio:</b> Gleichzeitiges Abspielen beliebig vieler paralleler Audiospuren</li>
 *     <li><b>Zeitversatz (startTimeMs):</b> Dynamischer Start jedes Songs zum exakten Timeline-Zeitpunkt</li>
 *     <li><b>MP3:</b> Reine, native Java-Dekodierung und parallele Wiedergabe über JLayer {@link AdvancedPlayer}</li>
 *     <li><b>WAV:</b> Hardwarebeschleunigte PCM-Wiedergabe über Java Sound {@link Clip}</li>
 *     <li>Unterbrechungsfreies Pausieren, Stoppen und nahtloses Scrubben (Seek) zu beliebigen Millisekunden</li>
 *     <li>Plattformübergreifend stabil auf Windows und Linux (PipeWire / ALSA / PulseAudio)</li>
 * </ul>
 * </p>
 */
public class AudioPlaybackService implements AutoCloseable {

    /** Durchschnittliche Dauer eines MP3-Layer-III Frames bei 44.1 kHz in Millisekunden. */
    private static final double MP3_MS_PER_FRAME = 26.122;

    private final AtomicBoolean playing = new AtomicBoolean(false);
    private final AtomicLong currentPositionMs = new AtomicLong(0L);
    private final AtomicLong sessionGeneration = new AtomicLong(0L);
    private volatile double volume = 1.0;

    private final Map<String, TrackAudioChannel> activeChannels = new ConcurrentHashMap<>();
    private volatile List<TimelineTrack> currentTracks = Collections.emptyList();
    private volatile File currentFile;

    /**
     * Repräsentiert einen einzelnen Audio-Wiedergabekanal für eine Spur.
     */
    private static class TrackAudioChannel {
        private final String trackId;
        private final File audioFile;
        private final long startTimeMs;
        private final long durationMs;
        private final AtomicBoolean active = new AtomicBoolean(false);

        private volatile AdvancedPlayer mp3Player;
        private volatile Clip wavClip;
        private volatile InputStream stream;
        private volatile Thread thread;

        TrackAudioChannel(String trackId, File audioFile, long startTimeMs, long durationMs) {
            this.trackId = trackId;
            this.audioFile = audioFile;
            this.startTimeMs = startTimeMs;
            this.durationMs = durationMs;
        }

        boolean isPlaying() {
            return active.get();
        }

        void start(long offsetWithinClipMs, long generation, AtomicLong sessionGen, double currentVolume) {
            stop();
            if (audioFile == null || !audioFile.exists() || !audioFile.isFile() || audioFile.length() == 0) {
                return;
            }
            active.set(true);

            String name = audioFile.getName().toLowerCase();
            AppLogger.info("[AUDIO-PLAYBACK] Track '%s': Starte '%s' ab Clip-Offset %d ms (Gen=%d)",
                    trackId, audioFile.getName(), offsetWithinClipMs, generation);

            if (name.endsWith(".wav")) {
                playWav(offsetWithinClipMs, generation, sessionGen, currentVolume);
            } else {
                playMp3(offsetWithinClipMs, generation, sessionGen);
            }
        }

        private void playWav(long offsetMs, long generation, AtomicLong sessionGen, double currentVolume) {
            try {
                AudioInputStream ais = AudioSystem.getAudioInputStream(audioFile);
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                applyVolume(clip, currentVolume);
                clip.setMicrosecondPosition(offsetMs * 1000L);
                clip.start();
                this.wavClip = clip;

                thread = new Thread(() -> {
                    try {
                        while (active.get() && sessionGen.get() == generation && wavClip != null && wavClip.isRunning()) {
                            Thread.sleep(50);
                        }
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    } finally {
                        active.set(false);
                    }
                }, "WAV-Channel-" + trackId);
                thread.setDaemon(true);
                thread.start();
            } catch (Exception e) {
                AppLogger.error("[AUDIO-PLAYBACK] Fehler beim Abspielen von WAV (Track %s): %s", trackId, e.getMessage());
                active.set(false);
            }
        }

        private void playMp3(long offsetMs, long generation, AtomicLong sessionGen) {
            int startFrame = (int) Math.round(offsetMs / MP3_MS_PER_FRAME);

            thread = new Thread(() -> {
                try (InputStream in = new BufferedInputStream(new FileInputStream(audioFile))) {
                    if (sessionGen.get() != generation || !active.get()) {
                        return;
                    }
                    this.stream = in;
                    AdvancedPlayer player = new AdvancedPlayer(in);
                    this.mp3Player = player;

                    player.play(startFrame, Integer.MAX_VALUE);
                } catch (Exception e) {
                    if (active.get() && sessionGen.get() == generation) {
                        AppLogger.warn("[AUDIO-PLAYBACK] MP3-Track '%s' beendet oder unterbrochen: %s", trackId, e.getMessage());
                    }
                } finally {
                    active.set(false);
                }
            }, "MP3-Channel-" + trackId);
            thread.setDaemon(true);
            thread.start();
        }

        void stop() {
            active.set(false);
            if (wavClip != null) {
                try {
                    wavClip.stop();
                    wavClip.close();
                } catch (Exception ignored) {}
                wavClip = null;
            }
            if (mp3Player != null) {
                try {
                    mp3Player.close();
                } catch (Exception ignored) {}
                mp3Player = null;
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception ignored) {}
                stream = null;
            }
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
                thread = null;
            }
        }

        private void applyVolume(Clip clip, double vol) {
            try {
                if (clip != null && clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float min = gainControl.getMinimum();
                    float max = gainControl.getMaximum();
                    if (vol <= 0.0) {
                        gainControl.setValue(min);
                    } else {
                        float gain = (float) (Math.log10(vol) * 20.0);
                        gainControl.setValue(Math.max(min, Math.min(max, gain)));
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    /**
     * Erstellt einen neuen {@link AudioPlaybackService}.
     */
    public AudioPlaybackService() {
        // Initialisiert
    }

    /**
     * Startet die parallele Multitrack-Wiedergabe aller ungemuteten Spuren ab der Timeline-Position.
     *
     * @param tracks             Liste der Spuren in der Timeline
     * @param timelinePositionMs Abspielposition der Timeline in Millisekunden
     */
    public synchronized void playTimeline(List<TimelineTrack> tracks, long timelinePositionMs) {
        long generation = sessionGeneration.incrementAndGet();
        this.currentTracks = tracks != null ? new ArrayList<>(tracks) : Collections.emptyList();
        long safeStartMs = Math.max(0L, timelinePositionMs);
        this.currentPositionMs.set(safeStartMs);
        this.playing.set(true);

        closeAllChannels();

        if (tracks == null || tracks.isEmpty()) {
            return;
        }

        for (TimelineTrack track : tracks) {
            syncTrackChannel(track, safeStartMs, generation);
        }
    }

    /**
     * Aktualisiert den Status aller Spuren während der laufenden Timeline-Uhr.
     * Startet versetzte Songs automatisch, sobald deren Startzeit erreicht wird.
     *
     * @param timelinePositionMs Aktuelle Abspielposition in Millisekunden
     */
    public void onTimelineTick(long timelinePositionMs) {
        if (!playing.get()) return;
        this.currentPositionMs.set(timelinePositionMs);
        long generation = sessionGeneration.get();

        for (TimelineTrack track : currentTracks) {
            if (track.isMuted() || !track.hasSong()) {
                TrackAudioChannel ch = activeChannels.remove(track.getId());
                if (ch != null) {
                    ch.stop();
                }
                continue;
            }

            long startMs = track.getStartTimeMs();
            long durMs = track.getDurationMs() > 0 ? track.getDurationMs() : Long.MAX_VALUE;
            long endMs = startMs + durMs;

            TrackAudioChannel ch = activeChannels.get(track.getId());
            if (ch == null || !ch.isPlaying()) {
                if (timelinePositionMs >= startMs && timelinePositionMs < endMs) {
                    syncTrackChannel(track, timelinePositionMs, generation);
                }
            } else {
                if (timelinePositionMs >= endMs) {
                    activeChannels.remove(track.getId());
                    ch.stop();
                }
            }
        }
    }

    private synchronized void syncTrackChannel(TimelineTrack track, long timelinePositionMs, long generation) {
        if (track.isMuted() || !track.hasSong() || track.getSongFile() == null || !track.getSongFile().exists()) {
            return;
        }

        long startMs = track.getStartTimeMs();
        long durMs = track.getDurationMs() > 0 ? track.getDurationMs() : Long.MAX_VALUE;
        long endMs = startMs + durMs;

        if (timelinePositionMs >= startMs && timelinePositionMs < endMs) {
            long offsetWithinClip = timelinePositionMs - startMs;
            TrackAudioChannel channel = new TrackAudioChannel(track.getId(), track.getSongFile(), startMs, durMs);
            activeChannels.put(track.getId(), channel);
            channel.start(offsetWithinClip, generation, sessionGeneration, volume);
        }
    }

    /**
     * Startet die Wiedergabe einer einzelnen Audiodatei ab der gewünschten Millisekunden-Position.
     * (Abwärtskompatible Methode für Einzelaufrufe).
     *
     * @param audioFile       die abzuspielende Datei (MP3 oder WAV)
     * @param startPositionMs Startposition in Millisekunden (>= 0)
     */
    public synchronized void play(File audioFile, long startPositionMs) {
        long generation = sessionGeneration.incrementAndGet();
        closeAllChannels();

        if (audioFile == null || !audioFile.exists() || !audioFile.isFile() || audioFile.length() == 0) {
            AppLogger.warn("[AUDIO-PLAYBACK] Wiedergabe abgebrochen: Datei ist ungültig oder existiert nicht (%s)", audioFile);
            playing.set(false);
            return;
        }

        this.currentFile = audioFile;
        long safeStartMs = Math.max(0L, startPositionMs);
        this.currentPositionMs.set(safeStartMs);
        this.playing.set(true);

        TrackAudioChannel singleChannel = new TrackAudioChannel("single", audioFile, 0L, Long.MAX_VALUE);
        activeChannels.put("single", singleChannel);
        singleChannel.start(safeStartMs, generation, sessionGeneration, volume);
    }

    /**
     * Pausiert die aktuelle Wiedergabe aller Spuren und speichert die aktuelle Position.
     */
    public synchronized void pause() {
        sessionGeneration.incrementAndGet();
        if (!playing.get()) return;
        playing.set(false);
        closeAllChannels();
        AppLogger.info("[AUDIO-PLAYBACK] Wiedergabe pausiert bei %d ms.", currentPositionMs.get());
    }

    /**
     * Stoppt die aktuelle Wiedergabe aller Spuren vollständig und setzt die Position auf 0 zurück.
     */
    public synchronized void stop() {
        sessionGeneration.incrementAndGet();
        playing.set(false);
        closeAllChannels();
        currentPositionMs.set(0L);
        AppLogger.info("[AUDIO-PLAYBACK] Wiedergabe vollständig gestoppt.");
    }

    /**
     * Setzt die Position des Players in Millisekunden.
     * Wenn gerade abgespielt wird, wird die Multitrack-Wiedergabe an der neuen Position nahtlos fortgesetzt.
     *
     * @param targetMs Zielposition in ms
     */
    public synchronized void setPosition(long targetMs) {
        long safeMs = Math.max(0L, targetMs);
        currentPositionMs.set(safeMs);

        if (playing.get()) {
            if (!currentTracks.isEmpty()) {
                playTimeline(currentTracks, safeMs);
            } else if (currentFile != null && currentFile.exists()) {
                play(currentFile, safeMs);
            }
        }
    }

    /**
     * Springt an eine bestimmte Position in Millisekunden. Wenn gerade abgespielt wird,
     * wird die Wiedergabe an der neuen Stelle nahtlos fortgesetzt.
     *
     * @param targetMs Zielposition in ms
     */
    public synchronized void seek(long targetMs) {
        setPosition(targetMs);
    }

    private synchronized void closeAllChannels() {
        for (TrackAudioChannel channel : activeChannels.values()) {
            try {
                channel.stop();
            } catch (Exception ignored) {}
        }
        activeChannels.clear();
    }

    /**
     * Prüft, ob aktuell Audio wiedergegeben wird.
     *
     * @return {@code true}, wenn aktiv
     */
    public boolean isPlaying() {
        return playing.get();
    }

    /**
     * Liefert die aktuelle Millisekunden-Position der Audiowiedergabe.
     *
     * @return Position in ms
     */
    public long getCurrentPositionMs() {
        return currentPositionMs.get();
    }

    /**
     * Setzt die globale Lautstärke (0.0 = stumm, 1.0 = maximal).
     *
     * @param volume Lautstärkewert
     */
    public void setVolume(double volume) {
        this.volume = Math.max(0.0, Math.min(1.0, volume));
    }

    /**
     * Liefert die aktuell eingestellte Lautstärke.
     *
     * @return Lautstärkewert (0.0 bis 1.0)
     */
    public double getVolume() {
        return volume;
    }

    /**
     * Liefert die Datei des Einzel-Wiedergabemodus oder {@code null}.
     *
     * @return aktuelle Datei
     */
    public File getCurrentFile() {
        return currentFile;
    }

    @Override
    public void close() {
        stop();
    }
}

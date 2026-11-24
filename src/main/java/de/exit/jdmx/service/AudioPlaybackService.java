package de.exit.jdmx.service;

import de.exit.jdmx.core.log.AppLogger;
import de.exit.jdmx.core.model.TimelineTrack;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.SourceDataLine;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service für die parallele Multitrack-Wiedergabe von Audio-Songs (MP3 und WAV) über die Systemsoundkarte.
 * <p>
 * Unterstützt:
 * <ul>
 *     <li><b>Multitrack Audio:</b> Gleichzeitiges Abspielen beliebig vieler paralleler Audiospuren</li>
 *     <li><b>Zeitversatz (startTimeMs):</b> Dynamischer Start jedes Songs zum exakten Timeline-Zeitpunkt</li>
 *     <li><b>MP3:</b> Reine, native Java-Dekodierung via JLayer {@link Bitstream}/{@link Decoder} mit {@link SourceDataLine} Hardware-Clock</li>
 *     <li><b>WAV:</b> Hardwarebeschleunigte PCM-Wiedergabe über Java Sound {@link Clip}</li>
 *     <li>Unterbrechungsfreies Pausieren, Stoppen und nahtloses Scrubben (Seek) zu beliebigen Millisekunden</li>
 *     <li>Plattformübergreifend stabil auf Windows und Linux (PipeWire / ALSA / PulseAudio)</li>
 * </ul>
 * </p>
 */
public class AudioPlaybackService implements AutoCloseable {

    private final AtomicBoolean playing = new AtomicBoolean(false);
    private final AtomicLong currentPositionMs = new AtomicLong(0L);
    private final AtomicLong sessionGeneration = new AtomicLong(0L);
    private volatile double volume = 1.0;

    private final Map<String, TrackAudioChannel> activeChannels = new ConcurrentHashMap<>();
    private final ScheduledExecutorService syncScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "AudioPlayback-ResyncDebounce");
        t.setDaemon(true);
        return t;
    });
    private final Map<String, ScheduledFuture<?>> pendingResyncs = new ConcurrentHashMap<>();
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

        private volatile SourceDataLine mp3Line;
        private volatile Clip wavClip;
        private volatile InputStream stream;
        private volatile Thread thread;
        private volatile long clipSeekOffsetMs = 0L;
        private volatile boolean hardwareStarted = false;

        TrackAudioChannel(String trackId, File audioFile, long startTimeMs, long durationMs) {
            this.trackId = trackId;
            this.audioFile = audioFile;
            this.startTimeMs = startTimeMs;
            this.durationMs = durationMs;
        }

        boolean isPlaying() {
            return active.get();
        }

        /**
         * Liefert die aktuelle Hardware-Wiedergabeposition dieses Kanals bezogen auf die Timeline (in ms).
         * Verhindert Startlatenz-Versatz und Audio-Clock-Drift vollständig.
         */
        long getCurrentTimelinePositionMs() {
            if (!active.get()) {
                return -1L;
            }
            Clip clip = wavClip;
            if (clip != null && clip.isOpen()) {
                long us = clip.getMicrosecondPosition();
                if (us > 0 || clip.isRunning()) {
                    hardwareStarted = true;
                    return startTimeMs + (us / 1000L);
                }
                return startTimeMs + clipSeekOffsetMs;
            }
            SourceDataLine line = mp3Line;
            if (line != null && line.isOpen()) {
                long us = line.getMicrosecondPosition();
                if (us > 0 || line.isRunning()) {
                    hardwareStarted = true;
                    return startTimeMs + clipSeekOffsetMs + (us / 1000L);
                }
            }
            // Während Header-Load / Seek noch nicht abgeschlossen ist -> Position am Seek-Punkt einfrieren
            return startTimeMs + clipSeekOffsetMs;
        }

        void start(long offsetWithinClipMs, long generation, AtomicLong sessionGen, double currentVolume) {
            stop();
            if (audioFile == null || !audioFile.exists() || !audioFile.isFile() || audioFile.length() == 0) {
                return;
            }
            this.clipSeekOffsetMs = Math.max(0L, offsetWithinClipMs);
            this.hardwareStarted = false;
            active.set(true);

            String name = audioFile.getName().toLowerCase();
            AppLogger.info("[AUDIO-PLAYBACK] Track '%s': Starte '%s' ab Clip-Offset %d ms (Gen=%d)",
                    trackId, audioFile.getName(), offsetWithinClipMs, generation);

            if (name.endsWith(".wav")) {
                playWav(offsetWithinClipMs, generation, sessionGen, currentVolume);
            } else {
                playMp3(offsetWithinClipMs, generation, sessionGen, currentVolume);
            }
        }

        private void playWav(long offsetMs, long generation, AtomicLong sessionGen, double currentVolume) {
            try {
                AudioInputStream ais = AudioSystem.getAudioInputStream(audioFile);
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                applyVolume(clip, currentVolume);
                clip.setMicrosecondPosition(offsetMs * 1000L);

                synchronized (this) {
                    if (sessionGen.get() != generation || !active.get()) {
                        clip.close();
                        ais.close();
                        return;
                    }
                    clip.start();
                    this.wavClip = clip;
                }

                thread = new Thread(() -> {
                    try {
                        while (active.get() && sessionGen.get() == generation && wavClip != null && wavClip.isRunning()) {
                            Thread.sleep(50);
                        }
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    } finally {
                        synchronized (this) {
                            active.set(false);
                        }
                    }
                }, "WAV-Channel-" + trackId);
                thread.setDaemon(true);
                thread.start();
            } catch (Exception e) {
                AppLogger.error("[AUDIO-PLAYBACK] Fehler beim Abspielen von WAV (Track %s): %s", trackId, e.getMessage());
                synchronized (this) {
                    active.set(false);
                }
            }
        }

        private void playMp3(long offsetMs, long generation, AtomicLong sessionGen, double currentVolume) {
            thread = new Thread(() -> {
                SourceDataLine line = null;
                try (InputStream in = new BufferedInputStream(new FileInputStream(audioFile))) {
                    Bitstream bitstream = new Bitstream(in);
                    Decoder decoder = new Decoder();

                    // 1. Schnelles, exaktes Frame-Skipping ohne PCM-Dekodierung (CBR, VBR & 48kHz-sicher)
                    double accumulatedMs = 0.0;
                    Header header = null;
                    while (true) {
                        if (sessionGen.get() != generation || !active.get()) {
                            return;
                        }
                        try {
                            header = bitstream.readFrame();
                        } catch (Exception e) {
                            try { bitstream.closeFrame(); } catch (Exception ignored) {}
                            continue;
                        }
                        if (header == null) {
                            return; // Dateiende vor Seek-Offset erreicht
                        }
                        double frameMs = header.ms_per_frame();
                        if (accumulatedMs + (frameMs * 0.5) >= offsetMs) {
                            break;
                        }
                        accumulatedMs += frameMs;
                        bitstream.closeFrame();
                    }

                    // Exakten Start-Offset an das erreichte MP3-Frame-Timing binden
                    this.clipSeekOffsetMs = Math.round(accumulatedMs);

                    // 2. Ersten Frame dekodieren für AudioFormat (SampleRate & Channels)
                    SampleBuffer firstOutput = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                    bitstream.closeFrame();
                    if (firstOutput == null || sessionGen.get() != generation || !active.get()) {
                        return;
                    }

                    int sampleRate = decoder.getOutputFrequency();
                    int channels = decoder.getOutputChannels();
                    AudioFormat format = new AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED,
                            sampleRate,
                            16,
                            channels,
                            channels * 2,
                            sampleRate,
                            false
                    );

                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                    line = (SourceDataLine) AudioSystem.getLine(info);
                    int bufferBytes = Math.max(4096, (int) (sampleRate * channels * 2 * 0.10));
                    line.open(format, bufferBytes);
                    applyVolume(line, currentVolume);

                    synchronized (this) {
                        if (sessionGen.get() != generation || !active.get()) {
                            line.close();
                            return;
                        }
                        this.stream = in;
                        this.mp3Line = line;
                        line.start();
                    }

                    byte[] pcmBuf = new byte[4608 * 2];
                    writePcmSamples(line, firstOutput.getBuffer(), firstOutput.getBufferLength(), pcmBuf);

                    // 3. Kontinuierliche Echtzeit-Dekodierung
                    while (active.get() && sessionGen.get() == generation) {
                        Header nextHeader;
                        try {
                            nextHeader = bitstream.readFrame();
                        } catch (Exception e) {
                            try { bitstream.closeFrame(); } catch (Exception ignored) {}
                            continue;
                        }
                        if (nextHeader == null) {
                            break;
                        }
                        try {
                            SampleBuffer output = (SampleBuffer) decoder.decodeFrame(nextHeader, bitstream);
                            if (output != null && output.getBufferLength() > 0) {
                                int len = output.getBufferLength();
                                if (pcmBuf.length < len * 2) {
                                    pcmBuf = new byte[len * 2];
                                }
                                writePcmSamples(line, output.getBuffer(), len, pcmBuf);
                            }
                        } catch (Exception ignored) {
                        } finally {
                            bitstream.closeFrame();
                        }
                    }

                    if (active.get() && sessionGen.get() == generation) {
                        line.drain();
                    }
                } catch (Exception e) {
                    if (active.get() && sessionGen.get() == generation) {
                        AppLogger.warn("[AUDIO-PLAYBACK] MP3-Track '%s' beendet oder unterbrochen: %s", trackId, e.getMessage());
                    }
                } finally {
                    synchronized (this) {
                        active.set(false);
                        if (line != null) {
                            try {
                                line.stop();
                                line.close();
                            } catch (Exception ignored) {}
                        }
                        mp3Line = null;
                    }
                }
            }, "MP3-Channel-" + trackId);
            thread.setDaemon(true);
            thread.start();
        }

        private static void writePcmSamples(SourceDataLine line, short[] samples, int length, byte[] buf) {
            int byteIdx = 0;
            for (int i = 0; i < length; i++) {
                short s = samples[i];
                buf[byteIdx++] = (byte) (s & 0xFF);
                buf[byteIdx++] = (byte) ((s >> 8) & 0xFF);
            }
            line.write(buf, 0, byteIdx);
        }

        void stop() {
            synchronized (this) {
                active.set(false);
                if (wavClip != null) {
                    try {
                        wavClip.stop();
                        wavClip.close();
                    } catch (Exception ignored) {}
                    wavClip = null;
                }
                if (mp3Line != null) {
                    try {
                        mp3Line.stop();
                        mp3Line.close();
                    } catch (Exception ignored) {}
                    mp3Line = null;
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
        }

        private void applyVolume(Line line, double vol) {
            try {
                if (line != null && line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
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
     * Resynchronisiert sofort eine einzelne Spur bei Positionsverschiebung während der Wiedergabe.
     * Bricht eventuell geplante Debounce-Synchronisationen für diese Spur ab.
     *
     * @param track              Die verschobene Spur
     * @param timelinePositionMs Aktuelle Timeline-Position in Millisekunden
     */
    public synchronized void resyncTrackImmediate(TimelineTrack track, long timelinePositionMs) {
        if (!playing.get() || track == null) {
            return;
        }
        ScheduledFuture<?> future = pendingResyncs.remove(track.getId());
        if (future != null) {
            future.cancel(false);
        }

        long generation = sessionGeneration.get();
        TrackAudioChannel oldCh = activeChannels.remove(track.getId());
        if (oldCh != null) {
            oldCh.stop();
        }
        syncTrackChannel(track, timelinePositionMs, generation);
    }

    /**
     * Resynchronisiert eine einzelne Spur verzögert (debounced um 80 ms), um während kontinuierlicher
     * Mausbewegungen (Drag) ein Überlasten der Audio-Linien und Stottern zu verhindern.
     *
     * @param track              Die verschobene Spur
     * @param timelinePositionMs Aktuelle Timeline-Position in Millisekunden
     */
    public synchronized void resyncTrackDebounced(TimelineTrack track, long timelinePositionMs) {
        if (!playing.get() || track == null) {
            return;
        }
        ScheduledFuture<?> existing = pendingResyncs.get(track.getId());
        if (existing != null && !existing.isDone()) {
            existing.cancel(false);
        }

        ScheduledFuture<?> next = syncScheduler.schedule(() -> {
            resyncTrackImmediate(track, currentPositionMs.get());
        }, 80, TimeUnit.MILLISECONDS);
        pendingResyncs.put(track.getId(), next);
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
        for (ScheduledFuture<?> f : pendingResyncs.values()) {
            if (f != null) {
                f.cancel(false);
            }
        }
        pendingResyncs.clear();
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
     * Liefert die Anzahl der aktuell aktiven Audio-Kanäle.
     *
     * @return Anzahl aktiver Kanäle
     */
    public int getActiveChannelCount() {
        return activeChannels.size();
    }

    /**
     * Liefert die aktuelle Hardware-Master-Position in Millisekunden bezogen auf die Timeline
     * vom führenden (zuerst gestarteten) aktiven Audio-Kanal.
     *
     * @return Hardware-Position in ms oder {@code -1L}, falls aktuell kein Audio-Kanal aktiv tönt
     */
    public long getMasterPlaybackPositionMs() {
        if (!playing.get() || activeChannels.isEmpty()) {
            return -1L;
        }
        TrackAudioChannel master = null;
        for (TrackAudioChannel ch : activeChannels.values()) {
            if (ch.isPlaying()) {
                if (master == null || ch.startTimeMs < master.startTimeMs) {
                    master = ch;
                }
            }
        }
        return master != null ? master.getCurrentTimelinePositionMs() : -1L;
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
        syncScheduler.shutdownNow();
    }
}

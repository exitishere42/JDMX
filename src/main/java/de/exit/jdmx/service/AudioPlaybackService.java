package de.exit.jdmx.service;

import de.exit.jdmx.core.log.AppLogger;
import javazoom.jl.player.advanced.AdvancedPlayer;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service für die auditive Wiedergabe von Audio-Songs (MP3 und WAV) über die Systemsoundkarte.
 * <p>
 * Unterstützt:
 * <ul>
 *     <li><b>MP3:</b> Reine, native Java-Dekodierung und Wiedergabe über JLayer {@link AdvancedPlayer}</li>
 *     <li><b>WAV:</b> Hardwarebeschleunigte PCM-Wiedergabe über Java Sound {@link Clip}</li>
 *     <li>Unterbrechungsfreies Pausieren, Stoppen und Scrubben (Seek) zu beliebigen Millisekunden</li>
 *     <li>Plattformübergreifend stabil auf Windows (WASAPI / DirectSound) und Linux (ALSA / PipeWire / PulseAudio)</li>
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

    private volatile File currentFile;
    private volatile Thread playbackThread;
    private volatile AdvancedPlayer mp3Player;
    private volatile Clip wavClip;
    private volatile InputStream currentStream;

    /**
     * Erstellt einen neuen {@link AudioPlaybackService}.
     */
    public AudioPlaybackService() {
        // Initialisiert
    }

    /**
     * Startet die Wiedergabe der angegebenen Audiodatei ab der gewünschten Millisekunden-Position.
     *
     * @param audioFile       die abzuspielende Datei (MP3 oder WAV)
     * @param startPositionMs Startposition in Millisekunden (>= 0)
     */
    public synchronized void play(File audioFile, long startPositionMs) {
        long generation = sessionGeneration.incrementAndGet();
        closeCurrentMedia();

        if (audioFile == null || !audioFile.exists() || !audioFile.isFile() || audioFile.length() == 0) {
            AppLogger.warn("[AUDIO-PLAYBACK] Wiedergabe abgebrochen: Datei ist ungültig oder existiert nicht (%s)", audioFile);
            playing.set(false);
            return;
        }

        this.currentFile = audioFile;
        long safeStartMs = Math.max(0L, startPositionMs);
        this.currentPositionMs.set(safeStartMs);
        this.playing.set(true);

        String name = audioFile.getName().toLowerCase();
        AppLogger.info("[AUDIO-PLAYBACK] Starte Wiedergabe von '%s' ab Position %d ms (Gen=%d)",
                audioFile.getName(), safeStartMs, generation);

        if (name.endsWith(".wav")) {
            playWav(audioFile, safeStartMs, generation);
        } else {
            playMp3(audioFile, safeStartMs, generation);
        }
    }

    private void playWav(File file, long startMs, long generation) {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(file);
            wavClip = AudioSystem.getClip();
            wavClip.open(ais);
            applyVolumeToClip(wavClip);
            wavClip.setMicrosecondPosition(startMs * 1000L);
            wavClip.start();

            playbackThread = new Thread(() -> {
                try {
                    while (playing.get() && sessionGeneration.get() == generation && wavClip != null && wavClip.isRunning()) {
                        currentPositionMs.set(wavClip.getMicrosecondPosition() / 1000L);
                        Thread.sleep(25);
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } finally {
                    if (sessionGeneration.get() == generation) {
                        playing.set(false);
                    }
                }
            }, "WAV-Playback-Monitor");
            playbackThread.setDaemon(true);
            playbackThread.start();
        } catch (Exception e) {
            AppLogger.error("[AUDIO-PLAYBACK] Fehler beim Abspielen der WAV-Datei '%s': %s", file.getName(), e.getMessage());
            if (sessionGeneration.get() == generation) {
                playing.set(false);
            }
        }
    }

    private void playMp3(File file, long startMs, long generation) {
        int startFrame = (int) Math.round(startMs / MP3_MS_PER_FRAME);

        playbackThread = new Thread(() -> {
            try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
                if (sessionGeneration.get() != generation) {
                    return;
                }
                this.currentStream = in;
                AdvancedPlayer player = new AdvancedPlayer(in);
                this.mp3Player = player;

                long startTime = System.currentTimeMillis();
                long baseMs = startMs;

                // Separater Thread zur kontinuierlichen Positionsaktualisierung
                Thread posTracker = new Thread(() -> {
                    while (playing.get() && sessionGeneration.get() == generation && mp3Player != null) {
                        long elapsed = System.currentTimeMillis() - startTime;
                        currentPositionMs.set(baseMs + elapsed);
                        try {
                            Thread.sleep(25);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }, "MP3-Position-Tracker");
                posTracker.setDaemon(true);
                posTracker.start();

                player.play(startFrame, Integer.MAX_VALUE);
            } catch (Exception e) {
                if (playing.get() && sessionGeneration.get() == generation) {
                    AppLogger.warn("[AUDIO-PLAYBACK] MP3-Wiedergabe beendet oder unterbrochen: %s", e.getMessage());
                }
            } finally {
                if (sessionGeneration.get() == generation) {
                    playing.set(false);
                }
            }
        }, "MP3-Playback-Worker");
        playbackThread.setDaemon(true);
        playbackThread.start();
    }

    /**
     * Pausiert die aktuelle Wiedergabe und speichert die aktuelle Position.
     */
    public synchronized void pause() {
        sessionGeneration.incrementAndGet();
        if (!playing.get()) return;
        playing.set(false);
        closeCurrentMedia();
        AppLogger.info("[AUDIO-PLAYBACK] Wiedergabe pausiert bei %d ms.", currentPositionMs.get());
    }

    /**
     * Stoppt die aktuelle Wiedergabe vollständig und setzt die Position auf 0 zurück.
     */
    public synchronized void stop() {
        sessionGeneration.incrementAndGet();
        playing.set(false);
        closeCurrentMedia();
        currentPositionMs.set(0L);
        AppLogger.info("[AUDIO-PLAYBACK] Wiedergabe vollständig gestoppt.");
    }

    /**
     * Setzt die Position des Players in Millisekunden.
     * Wenn gerade abgespielt wird, wird die Wiedergabe an der neuen Position nahtlos fortgesetzt.
     *
     * @param targetMs Zielposition in ms
     */
    public synchronized void setPosition(long targetMs) {
        long safeMs = Math.max(0L, targetMs);
        currentPositionMs.set(safeMs);

        if (playing.get() && currentFile != null && currentFile.exists()) {
            play(currentFile, safeMs);
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

    private void closeCurrentMedia() {
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

        if (currentStream != null) {
            try {
                currentStream.close();
            } catch (Exception ignored) {}
            currentStream = null;
        }

        if (playbackThread != null && playbackThread.isAlive()) {
            playbackThread.interrupt();
            playbackThread = null;
        }
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
        if (wavClip != null) {
            applyVolumeToClip(wavClip);
        }
    }

    /**
     * Liefert die aktuell eingestellte Lautstärke.
     *
     * @return Lautstärkewert (0.0 bis 1.0)
     */
    public double getVolume() {
        return volume;
    }

    private void applyVolumeToClip(Clip clip) {
        try {
            if (clip != null && clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float min = gainControl.getMinimum();
                float max = gainControl.getMaximum();
                if (volume <= 0.0) {
                    gainControl.setValue(min);
                } else {
                    float gain = (float) (Math.log10(volume) * 20.0);
                    gainControl.setValue(Math.max(min, Math.min(max, gain)));
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void close() {
        stop();
    }
}

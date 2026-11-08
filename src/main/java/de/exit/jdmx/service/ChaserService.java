package de.exit.jdmx.service;

import de.exit.jdmx.core.model.ChaserDefinition;
import de.exit.jdmx.core.model.ChaserDirection;
import de.exit.jdmx.core.model.ChaserStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service für die Ausführung und Verwaltung von DMX-Chasern.
 * <p>
 * Steuert die schrittweise Wiedergabe von Szenen-Sequenzen im Takt (BPM / Hold-Time),
 * unterstützt Loop-Modi (Vorwärts, Rückwärts, Ping-Pong, Zufall) und steuert
 * weiche Überblendungen über {@link SceneService}.
 * </p>
 */
public class ChaserService implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ChaserService.class);

    /**
     * Listener für Chaser-Ereignisse (Schritt-Wechsel, Play/Pause/Stop).
     */
    public interface ChaserListener {
        void onStepChanged(String chaserId, int stepIndex);
        void onStateChanged(String chaserId, boolean isPlaying);
    }

    private final SceneService sceneService;
    private final Map<String, ChaserDefinition> chasers = new ConcurrentHashMap<>();
    private final Map<String, ChaserRunner> activeRunners = new ConcurrentHashMap<>();
    private final List<ChaserListener> listeners = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService chaserExecutor;

    public ChaserService(SceneService sceneService) {
        this.sceneService = Objects.requireNonNull(sceneService, "sceneService must not be null");
        this.chaserExecutor = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "Chaser-Engine-Thread");
            t.setDaemon(true);
            return t;
        });
    }

    public void addChaserListener(ChaserListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeChaserListener(ChaserListener listener) {
        listeners.remove(listener);
    }

    public void registerChaser(ChaserDefinition chaser) {
        if (chaser != null) {
            chasers.put(chaser.getId(), chaser);
        }
    }

    public ChaserDefinition getChaser(String chaserId) {
        if (chaserId == null) return null;
        return chasers.get(chaserId);
    }

    public List<ChaserDefinition> getAllChasers() {
        return new ArrayList<>(chasers.values());
    }

    public void deleteChaser(String chaserId) {
        if (chaserId != null) {
            stop(chaserId);
            chasers.remove(chaserId);
        }
    }

    public void addStep(String chaserId, String sceneId, int holdMs, int fadeMs) {
        ChaserDefinition chaser = getChaser(chaserId);
        if (chaser != null) {
            String stepId = "step-" + UUID.randomUUID().toString().substring(0, 8);
            chaser.addStep(new ChaserStep(stepId, sceneId, holdMs, fadeMs));
        }
    }

    public void removeStep(String chaserId, int stepIndex) {
        ChaserDefinition chaser = getChaser(chaserId);
        if (chaser != null) {
            chaser.removeStep(stepIndex);
        }
    }

    public void moveStep(String chaserId, int fromIndex, int toIndex) {
        ChaserDefinition chaser = getChaser(chaserId);
        if (chaser != null) {
            chaser.moveStep(fromIndex, toIndex);
        }
    }

    /**
     * Startet oder setzt die Wiedergabe des Chasers fort.
     */
    public synchronized void play(String chaserId) {
        ChaserDefinition chaser = getChaser(chaserId);
        if (chaser == null || chaser.getStepCount() == 0) {
            return;
        }

        ChaserRunner runner = activeRunners.get(chaserId);
        if (runner == null) {
            runner = new ChaserRunner(chaser);
            activeRunners.put(chaserId, runner);
            runner.start();
            notifyStateChanged(chaserId, true);
        }
    }

    /**
     * Pausiert die Wiedergabe.
     */
    public synchronized void pause(String chaserId) {
        stop(chaserId);
    }

    /**
     * Stoppt die Wiedergabe vollständig und setzt den Schrittindex zurück.
     */
    public synchronized void stop(String chaserId) {
        ChaserRunner runner = activeRunners.remove(chaserId);
        if (runner != null) {
            runner.stop();
            notifyStateChanged(chaserId, false);
            notifyStepChanged(chaserId, -1);
        }
    }

    public boolean isPlaying(String chaserId) {
        return activeRunners.containsKey(chaserId);
    }

    public int getCurrentStepIndex(String chaserId) {
        ChaserRunner runner = activeRunners.get(chaserId);
        return runner != null ? runner.getCurrentIndex() : -1;
    }

    private void notifyStepChanged(String chaserId, int stepIndex) {
        for (ChaserListener l : listeners) {
            try {
                l.onStepChanged(chaserId, stepIndex);
            } catch (Exception ex) {
                log.error("Fehler im ChaserListener", ex);
            }
        }
    }

    private void notifyStateChanged(String chaserId, boolean isPlaying) {
        for (ChaserListener l : listeners) {
            try {
                l.onStateChanged(chaserId, isPlaying);
            } catch (Exception ex) {
                log.error("Fehler im ChaserListener", ex);
            }
        }
    }

    @Override
    public void close() {
        for (ChaserRunner r : activeRunners.values()) {
            r.stop();
        }
        activeRunners.clear();
        chaserExecutor.shutdownNow();
    }

    /**
     * Ausführende Ticker-Instanz für einen aktiven Chaser.
     */
    private class ChaserRunner implements Runnable {
        private final ChaserDefinition chaser;
        private volatile boolean running = false;
        private int currentIndex = -1;
        private boolean pingPongForward = true;
        private ScheduledFuture<?> future;

        ChaserRunner(ChaserDefinition chaser) {
            this.chaser = chaser;
        }

        void start() {
            running = true;
            scheduleNextStep(0);
        }

        void stop() {
            running = false;
            if (future != null && !future.isDone()) {
                future.cancel(false);
            }
        }

        int getCurrentIndex() {
            return currentIndex;
        }

        private void scheduleNextStep(long delayMs) {
            if (!running) return;
            future = chaserExecutor.schedule(this, delayMs, TimeUnit.MILLISECONDS);
        }

        @Override
        public void run() {
            if (!running || chaser.getStepCount() == 0) return;

            // Nächsten Index ermitteln
            if (currentIndex < 0) {
                currentIndex = 0;
            } else {
                if (chaser.getDirection() == ChaserDirection.PING_PONG) {
                    if (currentIndex >= chaser.getStepCount() - 1) {
                        pingPongForward = false;
                    } else if (currentIndex <= 0) {
                        pingPongForward = true;
                    }
                }
                currentIndex = chaser.computeNextStepIndex(currentIndex, pingPongForward);
            }

            if (currentIndex >= 0 && currentIndex < chaser.getStepCount()) {
                ChaserStep step = chaser.getSteps().get(currentIndex);
                sceneService.applyScene(step.getSceneId(), step.getFadeTimeMs());
                notifyStepChanged(chaser.getId(), currentIndex);

                // Haltezeit berechnen (falls BPM definiert ist, kann diese auch genutzt werden)
                int hold = step.getHoldTimeMs();
                scheduleNextStep(hold);
            }
        }
    }
}

package de.exit.jdmx.service;

import de.exit.jdmx.core.model.FixtureInstance;
import de.exit.jdmx.core.model.SceneDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service für die Erstellung, Verwaltung und Ausführung von DMX-Lichtszenen (Cues).
 * <p>
 * Ermöglicht Snapshot-Erfassung des aktuellen Lichtzustands sowie weiche Überblendungen
 * (Fading) über den 40-Hz-Takt.
 * </p>
 */
public class SceneService implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(SceneService.class);

    private final DmxService dmxService;
    private final Map<String, SceneDefinition> scenes = new ConcurrentHashMap<>();
    private final ScheduledExecutorService fadeExecutor;
    private ScheduledFuture<?> activeFadeTask;

    public SceneService(DmxService dmxService) {
        this.dmxService = Objects.requireNonNull(dmxService, "dmxService must not be null");
        this.fadeExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Scene-Fade-Engine");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Erfasst den aktuellen Live-DMX-Zustand als neue Szene.
     *
     * @param name          Name der Szene
     * @param defaultFadeMs Standard-Überblendzeit in ms
     * @param fixtures      Liste der einzubeziehenden Scheinwerfer (oder leer/null für alle aktiven Kanäle)
     * @return Neu erstellte SceneDefinition
     */
    public SceneDefinition captureScene(String name, int defaultFadeMs, List<FixtureInstance> fixtures) {
        String id = "scene-" + UUID.randomUUID().toString().substring(0, 8);
        return captureScene(id, name, defaultFadeMs, fixtures);
    }

    /**
     * Erfasst den aktuellen DMX-Zustand als neue Szene (Snapshot) mit expliziter ID.
     *
     * @param id            Explizite Szenen-ID
     * @param name          Szenenname
     * @param defaultFadeMs Standard-Fade-Zeit in Millisekunden
     * @param fixtures      Liste der einzubeziehenden Scheinwerfer (oder leer/null für alle aktiven Kanäle)
     * @return Neu erstellte SceneDefinition
     */
    public SceneDefinition captureScene(String id, String name, int defaultFadeMs, List<FixtureInstance> fixtures) {
        String sceneId = id != null && !id.isBlank() ? id : "scene-" + UUID.randomUUID().toString().substring(0, 8);
        Map<Integer, Map<Integer, Integer>> channelMap = new HashMap<>();

        if (fixtures != null && !fixtures.isEmpty()) {
            for (FixtureInstance fix : fixtures) {
                if (fix == null) continue;
                int uni = Math.max(0, Math.min(3, fix.getUniverse() - 1));
                int start = fix.getStartAddress();
                int count = fix.getChannelCount();

                Map<Integer, Integer> uniChannels = channelMap.computeIfAbsent(uni, k -> new HashMap<>());
                for (int i = 0; i < count; i++) {
                    int ch = start + i;
                    if (ch >= 1 && ch <= 512) {
                        int val = dmxService.getChannel(uni, ch);
                        uniChannels.put(ch, val);
                    }
                }
            }
        } else {
            // Gesamtes Universum 0 als Fallback
            Map<Integer, Integer> uni0 = channelMap.computeIfAbsent(0, k -> new HashMap<>());
            for (int ch = 1; ch <= 512; ch++) {
                int val = dmxService.getChannel(0, ch);
                if (val > 0) {
                    uni0.put(ch, val);
                }
            }
        }

        SceneDefinition scene = new SceneDefinition(sceneId, name, defaultFadeMs, channelMap);
        registerScene(scene);
        log.info("Szene erfasst: '{}' (ID: {}, Kanäle: {})", name, sceneId, channelMap.size());
        return scene;
    }

    /**
     * Benennt eine vorhandene Szene um.
     *
     * @param sceneId Szenen-ID
     * @param newName Neuer Name
     */
    public void renameScene(String sceneId, String newName) {
        if (sceneId == null || newName == null || newName.isBlank()) return;
        synchronized (scenes) {
            SceneDefinition sc = scenes.get(sceneId);
            if (sc != null) {
                sc.setName(newName);
            }
        }
    }

    /**
     * Registriert eine Szene.
     *
     * @param scene Szenendefinition
     */
    public void registerScene(SceneDefinition scene) {
        if (scene != null) {
            scenes.put(scene.getId(), scene);
        }
    }

    /**
     * Liefert eine Szene anhand ihrer ID oder null.
     */
    public SceneDefinition getScene(String sceneId) {
        if (sceneId == null) return null;
        return scenes.get(sceneId);
    }

    /**
     * Liefert alle registrierten Szenen.
     */
    public List<SceneDefinition> getAllScenes() {
        return new ArrayList<>(scenes.values());
    }

    /**
     * Entfernt eine Szene.
     */
    public void deleteScene(String sceneId) {
        if (sceneId != null) {
            scenes.remove(sceneId);
        }
    }

    /**
     * Wendet eine Szene anhand ihrer ID an.
     *
     * @param sceneId      ID der Szene
     * @param customFadeMs Optionale Überblendzeit (-1 verwendet standard fadeTime)
     */
    public void applyScene(String sceneId, int customFadeMs) {
        SceneDefinition scene = getScene(sceneId);
        if (scene != null) {
            int fade = customFadeMs >= 0 ? customFadeMs : scene.getDefaultFadeTimeMs();
            applyScene(scene, fade);
        }
    }

    /**
     * Wendet eine Szene direkt an (mit weicher Überblendung oder Snap).
     *
     * @param scene  Szenendefinition
     * @param fadeMs Überblendzeit in Millisekunden
     */
    public void applyScene(SceneDefinition scene, int fadeMs) {
        if (scene == null) return;

        Map<Integer, Map<Integer, Integer>> targetChannels = scene.getChannelValues();
        if (targetChannels.isEmpty()) return;

        if (fadeMs <= 25) {
            // Sofortiger Snap
            for (Map.Entry<Integer, Map<Integer, Integer>> uniEntry : targetChannels.entrySet()) {
                int uni = uniEntry.getKey();
                for (Map.Entry<Integer, Integer> chEntry : uniEntry.getValue().entrySet()) {
                    dmxService.setChannel(uni, chEntry.getKey(), chEntry.getValue());
                }
            }
            return;
        }

        // Weicher Fade: Start-Werte ablesen
        Map<Integer, Map<Integer, Integer>> startChannels = new HashMap<>();
        for (Map.Entry<Integer, Map<Integer, Integer>> uniEntry : targetChannels.entrySet()) {
            int uni = uniEntry.getKey();
            Map<Integer, Integer> startMap = startChannels.computeIfAbsent(uni, k -> new HashMap<>());
            for (int ch : uniEntry.getValue().keySet()) {
                startMap.put(ch, dmxService.getChannel(uni, ch));
            }
        }

        if (activeFadeTask != null && !activeFadeTask.isDone()) {
            activeFadeTask.cancel(false);
        }

        long startTime = System.currentTimeMillis();
        long duration = fadeMs;

        activeFadeTask = fadeExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTime;
                double progress = Math.min(1.0, (double) elapsed / (double) duration);

                for (Map.Entry<Integer, Map<Integer, Integer>> uniEntry : targetChannels.entrySet()) {
                    int uni = uniEntry.getKey();
                    Map<Integer, Integer> startMap = startChannels.get(uni);
                    for (Map.Entry<Integer, Integer> chEntry : uniEntry.getValue().entrySet()) {
                        int ch = chEntry.getKey();
                        int targetVal = chEntry.getValue();
                        int startVal = startMap != null ? startMap.getOrDefault(ch, 0) : 0;
                        int currentVal = (int) Math.round(startVal + ((targetVal - startVal) * progress));
                        dmxService.setChannel(uni, ch, currentVal);
                    }
                }

                if (progress >= 1.0) {
                    throw new RuntimeException("FADE_DONE"); // Beendet den scheduled task
                }
            }
        }, 0, 25, TimeUnit.MILLISECONDS);
    }

    /**
     * Stoppt eine derzeit laufende Überblendung (Fading).
     */
    public synchronized void stopFade() {
        if (activeFadeTask != null && !activeFadeTask.isDone()) {
            activeFadeTask.cancel(true);
        }
    }

    /**
     * Bricht laufende Fades ab und leert alle registrierten Lichtszenen.
     */
    public synchronized void clearScenes() {
        stopFade();
        scenes.clear();
    }

    @Override
    public void close() {
        fadeExecutor.shutdownNow();
    }
}

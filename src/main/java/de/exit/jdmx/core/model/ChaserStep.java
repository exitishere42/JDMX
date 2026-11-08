package de.exit.jdmx.core.model;

import java.util.Objects;

/**
 * Ein einzelner Schritt innerhalb eines Chasers.
 * <p>
 * Referenziert eine {@link SceneDefinition} und definiert individuelle
 * Halte- (Hold) und Überblendzeiten (Fade).
 * </p>
 */
public class ChaserStep {

    private final String stepId;
    private String sceneId;
    private int holdTimeMs;
    private int fadeTimeMs;

    public ChaserStep(String stepId, String sceneId, int holdTimeMs, int fadeTimeMs) {
        this.stepId = Objects.requireNonNull(stepId, "stepId must not be null");
        this.sceneId = Objects.requireNonNull(sceneId, "sceneId must not be null");
        this.holdTimeMs = Math.max(10, holdTimeMs);
        this.fadeTimeMs = Math.max(0, fadeTimeMs);
    }

    public String getStepId() {
        return stepId;
    }

    public String getSceneId() {
        return sceneId;
    }

    public void setSceneId(String sceneId) {
        this.sceneId = Objects.requireNonNull(sceneId, "sceneId must not be null");
    }

    public int getHoldTimeMs() {
        return holdTimeMs;
    }

    public void setHoldTimeMs(int holdTimeMs) {
        this.holdTimeMs = Math.max(10, holdTimeMs);
    }

    public int getFadeTimeMs() {
        return fadeTimeMs;
    }

    public void setFadeTimeMs(int fadeTimeMs) {
        this.fadeTimeMs = Math.max(0, fadeTimeMs);
    }
}

package de.exit.jdmx.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Definition eines DMX-Chasers mit konfigurierbaren Einzelschritten, BPM und Laufrichtung.
 */
public class ChaserDefinition {

    private final String id;
    private String name;
    private double bpm;
    private ChaserDirection direction;
    private final List<ChaserStep> steps = new ArrayList<>();
    private final Random random = new Random();

    public ChaserDefinition(String id, String name) {
        this(id, name, 120.0, ChaserDirection.FORWARD);
    }

    public ChaserDefinition(String id, String name, double bpm, ChaserDirection direction) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = name != null && !name.isBlank() ? name : "Chaser";
        this.bpm = Math.max(1.0, Math.min(999.0, bpm));
        this.direction = direction != null ? direction : ChaserDirection.FORWARD;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null && !name.isBlank() ? name : this.name;
    }

    public double getBpm() {
        return bpm;
    }

    public void setBpm(double bpm) {
        this.bpm = Math.max(1.0, Math.min(999.0, bpm));
    }

    public ChaserDirection getDirection() {
        return direction;
    }

    public void setDirection(ChaserDirection direction) {
        this.direction = direction != null ? direction : ChaserDirection.FORWARD;
    }

    public List<ChaserStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    public void addStep(ChaserStep step) {
        if (step != null) {
            steps.add(step);
        }
    }

    public void addStep(int index, ChaserStep step) {
        if (step != null) {
            int targetIdx = Math.max(0, Math.min(steps.size(), index));
            steps.add(targetIdx, step);
        }
    }

    public void removeStep(int index) {
        if (index >= 0 && index < steps.size()) {
            steps.remove(index);
        }
    }

    public void moveStep(int fromIndex, int toIndex) {
        if (fromIndex >= 0 && fromIndex < steps.size() && toIndex >= 0 && toIndex < steps.size()) {
            ChaserStep step = steps.remove(fromIndex);
            steps.add(toIndex, step);
        }
    }

    public int getStepCount() {
        return steps.size();
    }

    /**
     * Berechnet den nächsten Schrittindex basierend auf aktueller Position und Laufrichtung.
     *
     * @param currentIndex Aktueller Index (0-basiert)
     * @param forward      Laufrichtung bei Ping-Pong (true = vorwärts, false = rückwärts)
     * @return Nächster 0-basierter Schrittindex
     */
    public int computeNextStepIndex(int currentIndex, boolean forward) {
        int count = steps.size();
        if (count <= 1) return 0;

        return switch (direction) {
            case FORWARD -> (currentIndex + 1) % count;
            case BACKWARD -> (currentIndex - 1 + count) % count;
            case PING_PONG -> {
                if (forward) {
                    yield currentIndex + 1 < count ? currentIndex + 1 : count - 2;
                } else {
                    yield currentIndex - 1 >= 0 ? currentIndex - 1 : 1;
                }
            }
            case RANDOM -> random.nextInt(count);
        };
    }
}

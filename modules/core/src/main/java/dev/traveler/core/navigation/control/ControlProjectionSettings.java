package dev.traveler.core.navigation.control;

import dev.traveler.core.settings.TravelerSettings;

public record ControlProjectionSettings(double pressThreshold, double releaseThreshold) {
    public ControlProjectionSettings {
        requireThreshold(pressThreshold, "pressThreshold");
        requireThreshold(releaseThreshold, "releaseThreshold");
        if (releaseThreshold > pressThreshold) {
            throw new IllegalArgumentException("releaseThreshold cannot exceed pressThreshold.");
        }
    }

    public static ControlProjectionSettings standard() {
        return TravelerSettings.standard().controlProjectionSettings();
    }

    private static void requireThreshold(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(name + " must be between 0 and 1.");
        }
    }
}

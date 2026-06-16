package dev.traveler.core.common.api;

import java.util.Objects;

public record SettingsSection<T>(String key, T settings) {
    public SettingsSection {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(settings, "settings");
    }
}

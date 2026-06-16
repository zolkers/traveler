package dev.traveler.core.common.api;

import java.util.Optional;

public interface TravelerRegistry {
    <T extends TravelerPort> Optional<T> port(Class<T> portType);
}

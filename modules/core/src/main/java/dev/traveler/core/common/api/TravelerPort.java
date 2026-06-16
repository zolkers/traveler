package dev.traveler.core.common.api;

import java.util.Objects;

public interface TravelerPort {
    String id();

    default String requireId() {
        return Objects.requireNonNull(id(), "id");
    }
}

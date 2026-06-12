package dev.traveler.core.event;

public record ClientTickEvent(long tickIndex) {
    public ClientTickEvent {
        if (tickIndex < 0) {
            throw new IllegalArgumentException("tickIndex must be non-negative");
        }
    }
}

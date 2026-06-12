package dev.traveler.mc.v1_21_11.common.event;

public record ClientTickEvent(long tickIndex) {
    public ClientTickEvent {
        if (tickIndex < 0) {
            throw new IllegalArgumentException("tickIndex must be non-negative");
        }
    }
}

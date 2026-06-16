package dev.traveler.core.world.behavior.api;

public enum SupportSemantics {
    NONE(false),
    STANDABLE(true);

    private final boolean standingSurface;

    SupportSemantics(boolean standingSurface) {
        this.standingSurface = standingSurface;
    }

    public boolean supportsStanding() {
        return standingSurface;
    }
}

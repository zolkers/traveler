package dev.traveler.core.event;

public final class TravelerClientEvents {
    public static final TravelerEvent<ClientTickEvent> CLIENT_TICK = TravelerEvent.create();
    public static final TravelerEvent<WorldRenderEvent> WORLD_RENDER = TravelerEvent.create();

    private TravelerClientEvents() {}
}

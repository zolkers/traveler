package dev.traveler.mc.v1_21_11.fabric.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.traveler.core.event.TravelerClientEvents;
import dev.traveler.core.event.TravelerEventSubscription;
import dev.traveler.core.event.WorldRenderEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class FabricEventBootstrapTest {
    @Test
    void emitsTravelerClientTickEventsWithIncreasingTickIndex() {
        List<Long> ticks = new ArrayList<>();
        FabricEventBootstrap bootstrap = new FabricEventBootstrap(context -> {});

        TravelerEventSubscription subscription =
                TravelerClientEvents.CLIENT_TICK.register(event -> ticks.add(event.tickIndex()));
        try {
            bootstrap.emitClientTick();
            bootstrap.emitClientTick();
        } finally {
            subscription.close();
        }

        assertEquals(List.of(0L, 1L), ticks);
    }

    @Test
    void worldRenderPublishesTravelerEventAndDelegatesRenderer() {
        List<WorldRenderEvent> events = new ArrayList<>();
        AtomicInteger renders = new AtomicInteger();
        AtomicInteger navigationUpdates = new AtomicInteger();
        FabricEventBootstrap bootstrap =
                new FabricEventBootstrap(context -> renders.incrementAndGet(), navigationUpdates::incrementAndGet);
        WorldRenderEvent renderEvent = new WorldRenderEvent(0.5f, 1.0, 2.0, 3.0);

        TravelerEventSubscription subscription = TravelerClientEvents.WORLD_RENDER.register(events::add);
        try {
            bootstrap.renderWorld(null, renderEvent);
        } finally {
            subscription.close();
        }

        assertEquals(List.of(renderEvent), events);
        assertEquals(1, navigationUpdates.get());
        assertEquals(1, renders.get());
    }
}

package dev.traveler.mc.v1_21_11.common.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TravelerEventTest {
    @Test
    void dispatchesListenersByDescendingPriorityThenRegistrationOrder() {
        TravelerEvent<String> event = TravelerEvent.create();
        List<String> calls = new ArrayList<>();

        event.register(payload -> calls.add("normal:" + payload));
        event.register(payload -> calls.add("high:" + payload), 100);
        event.register(payload -> calls.add("normal-second:" + payload));

        event.dispatch("tick");

        assertEquals(List.of("high:tick", "normal:tick", "normal-second:tick"), calls);
    }

    @Test
    void unsubscribeStopsFutureDispatches() {
        TravelerEvent<String> event = TravelerEvent.create();
        List<String> calls = new ArrayList<>();
        TravelerEventSubscription subscription = event.register(calls::add);

        subscription.close();
        event.dispatch("ignored");

        assertEquals(List.of(), calls);
    }

    @Test
    void dispatchUsesStableSnapshotWhenListenerUnsubscribesItself() {
        TravelerEvent<String> event = TravelerEvent.create();
        List<String> calls = new ArrayList<>();
        TravelerEventSubscription[] subscription = new TravelerEventSubscription[1];
        subscription[0] = event.register(payload -> {
            calls.add("first:" + payload);
            subscription[0].close();
        });
        event.register(payload -> calls.add("second:" + payload));

        event.dispatch("one");
        event.dispatch("two");

        assertEquals(List.of("first:one", "second:one", "second:two"), calls);
    }

    @Test
    void rejectsNullListenersAndPayloads() {
        TravelerEvent<String> event = TravelerEvent.create();

        assertThrows(NullPointerException.class, () -> event.register(null));
        assertThrows(NullPointerException.class, () -> event.dispatch(null));
    }
}

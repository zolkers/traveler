package dev.traveler.core.event;

import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class TravelerEvent<T> {
    private final CopyOnWriteArrayList<RegisteredListener<T>> listeners = new CopyOnWriteArrayList<>();
    private final AtomicLong sequence = new AtomicLong();

    private TravelerEvent() {}

    public static <T> TravelerEvent<T> create() {
        return new TravelerEvent<>();
    }

    public TravelerEventSubscription register(TravelerEventListener<T> listener) {
        return register(listener, 0);
    }

    public TravelerEventSubscription register(TravelerEventListener<T> listener, int priority) {
        RegisteredListener<T> registered = new RegisteredListener<>(
                Objects.requireNonNull(listener, "listener"), priority, sequence.getAndIncrement());
        listeners.add(registered);
        listeners.sort(RegisteredListener.ORDER);
        return new Subscription<>(listeners, registered);
    }

    public void dispatch(T event) {
        T payload = Objects.requireNonNull(event, "event");
        for (RegisteredListener<T> listener : listeners) {
            listener.dispatch(payload);
        }
    }

    private record RegisteredListener<T>(TravelerEventListener<T> listener, int priority, long sequence) {
        private static final Comparator<RegisteredListener<?>> ORDER = Comparator
                .<RegisteredListener<?>>comparingInt(RegisteredListener::priority)
                .reversed()
                .thenComparingLong(RegisteredListener::sequence);

        private void dispatch(T event) {
            listener.handle(event);
        }
    }

    private static final class Subscription<T> implements TravelerEventSubscription {
        private final CopyOnWriteArrayList<RegisteredListener<T>> listeners;
        private final RegisteredListener<T> listener;
        private final AtomicBoolean closed = new AtomicBoolean();

        private Subscription(CopyOnWriteArrayList<RegisteredListener<T>> listeners, RegisteredListener<T> listener) {
            this.listeners = listeners;
            this.listener = listener;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                listeners.remove(listener);
            }
        }
    }
}

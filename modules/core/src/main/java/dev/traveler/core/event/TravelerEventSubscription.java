package dev.traveler.core.event;

@FunctionalInterface
public interface TravelerEventSubscription extends AutoCloseable {
    @Override
    void close();
}

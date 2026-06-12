package dev.traveler.core.event;

@FunctionalInterface
public interface TravelerEventListener<T> {
    void handle(T event);
}

package dev.traveler.mc.v1_21_11.common.event;

@FunctionalInterface
public interface TravelerEventListener<T> {
    void handle(T event);
}

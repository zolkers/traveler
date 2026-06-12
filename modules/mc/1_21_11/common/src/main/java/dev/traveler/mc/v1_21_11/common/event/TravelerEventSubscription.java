package dev.traveler.mc.v1_21_11.common.event;

@FunctionalInterface
public interface TravelerEventSubscription extends AutoCloseable {
    @Override
    void close();
}

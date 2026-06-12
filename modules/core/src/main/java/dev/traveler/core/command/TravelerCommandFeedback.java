package dev.traveler.core.command;

@FunctionalInterface
public interface TravelerCommandFeedback {
    void reply(String message);
}

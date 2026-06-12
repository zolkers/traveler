package dev.traveler.core.command;

@FunctionalInterface
public interface TravelerCommandHandler {
    TravelerCommandResult execute(TravelerCommandContext context);
}

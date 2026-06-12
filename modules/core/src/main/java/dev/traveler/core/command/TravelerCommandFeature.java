package dev.traveler.core.command;

@FunctionalInterface
public interface TravelerCommandFeature {
    void register(TravelerCommandCatalog.Builder registry);
}

package dev.traveler.core.navigation.recovery;

@FunctionalInterface
public interface MovementHealthPolicy {
    MovementHealthEvaluation evaluate(MovementHealthSnapshot snapshot, MovementHealthSettings settings);
}

package dev.traveler.core.smooth;

@FunctionalInterface
public interface PathNodePreservation<N> {
    boolean mustPreserve(N previous, N current, N next);

    static <N> PathNodePreservation<N> none() {
        return (previous, current, next) -> false;
    }
}

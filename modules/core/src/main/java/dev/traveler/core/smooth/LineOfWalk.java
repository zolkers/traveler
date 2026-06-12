package dev.traveler.core.smooth;

@FunctionalInterface
public interface LineOfWalk<N> {
    boolean hasLineOfWalk(N from, N to);
}

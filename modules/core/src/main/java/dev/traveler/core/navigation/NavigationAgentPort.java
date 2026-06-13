package dev.traveler.core.navigation;

import java.util.Optional;

public interface NavigationAgentPort {
    Optional<NavigationFrameInput> frameInput(double deltaSeconds);

    void apply(NavigationControlFrame frame);

    void release();
}

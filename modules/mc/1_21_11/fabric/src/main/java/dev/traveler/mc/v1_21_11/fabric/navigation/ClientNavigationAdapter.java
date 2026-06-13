package dev.traveler.mc.v1_21_11.fabric.navigation;

import dev.traveler.core.navigation.NavigationControlFrame;
import dev.traveler.core.navigation.NavigationFrameInput;
import java.util.Optional;

public interface ClientNavigationAdapter {
    Optional<NavigationFrameInput> frameInput(double deltaSeconds);

    void apply(NavigationControlFrame frame);

    void release();
}

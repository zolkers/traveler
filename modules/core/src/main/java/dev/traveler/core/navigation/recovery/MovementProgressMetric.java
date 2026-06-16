package dev.traveler.core.navigation.recovery;

import dev.traveler.core.navigation.NavigationControlFrame;
import dev.traveler.core.navigation.NavigationFrameInput;
import dev.traveler.core.navigation.follow.NavigationPath;

@FunctionalInterface
public interface MovementProgressMetric {
    ProgressSample sample(NavigationPath path, NavigationFrameInput input, NavigationControlFrame frame);
}

package dev.traveler.core.capability.execution.spi;

import dev.traveler.core.navigation.NavigationFrameInput;
import dev.traveler.core.navigation.control.ControlProjectionFrame;
import dev.traveler.core.navigation.control.MovementIntent;

public interface ControlProjectionStrategy {
    ControlProjectionFrame project(
            ControlProjectionIntent intent,
            NavigationFrameInput input,
            MovementIntent previousIntent);
}

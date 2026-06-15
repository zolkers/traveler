package dev.traveler.core.command;

import java.util.Optional;

public interface TravelerCommandPosition {
    Optional<TravelerCommandBlockPosition> blockPosition();
}

package dev.traveler.core.navigation.recovery;

import dev.traveler.core.navigation.NavigationFrameInput;
import dev.traveler.core.navigation.control.MovementIntent;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.util.Objects;
import java.util.Optional;

public final class MovementProgressMonitor {
    private final MovementHealthSettings settings;
    private NavigationPoint lastProgressPosition;
    private double stagnantSeconds;

    public MovementProgressMonitor(MovementHealthSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public Optional<MovementFailure> update(NavigationFrameInput input, MovementIntent intent) {
        NavigationFrameInput frameInput = Objects.requireNonNull(input, "input");
        MovementIntent movementIntent = Objects.requireNonNull(intent, "intent");
        if (!movementIntent.moving()) {
            reset();
            return Optional.empty();
        }
        if (lastProgressPosition == null) {
            lastProgressPosition = frameInput.position();
            stagnantSeconds = frameInput.deltaSeconds();
            return failureIfStuck();
        }
        if (lastProgressPosition.distanceTo(frameInput.position()) >= settings.minimumProgressDistance()) {
            lastProgressPosition = frameInput.position();
            stagnantSeconds = 0.0;
            return Optional.empty();
        }
        stagnantSeconds += frameInput.deltaSeconds();
        return failureIfStuck();
    }

    public void reset() {
        lastProgressPosition = null;
        stagnantSeconds = 0.0;
    }

    private Optional<MovementFailure> failureIfStuck() {
        if (stagnantSeconds < settings.stuckAfterSeconds()) {
            return Optional.empty();
        }
        return Optional.of(new MovementFailure(
                MovementFailureKind.STUCK_NO_PROGRESS,
                "movement commanded but position did not progress"));
    }
}

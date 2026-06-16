package dev.traveler.core.navigation.diagnostics;

import dev.traveler.core.navigation.NavigationControlFrame;
import dev.traveler.core.navigation.NavigationFrameInput;
import dev.traveler.core.navigation.NavigationSession;
import dev.traveler.core.navigation.recovery.MovementFailure;
import java.util.Objects;

public record MovementFailureReportContext(
        NavigationSession session,
        NavigationFrameInput input,
        NavigationControlFrame frame,
        MovementFailure failure) {
    public MovementFailureReportContext {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(frame, "frame");
        Objects.requireNonNull(failure, "failure");
    }
}

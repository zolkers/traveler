package dev.traveler.core.navigation.diagnostics;

import java.util.Objects;

@FunctionalInterface
public interface MovementFailureReporter {
    void report(MovementFailureReportContext context);

    static MovementFailureReporter noop() {
        return context -> Objects.requireNonNull(context, "context");
    }
}

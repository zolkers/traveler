package dev.traveler.command.buildmycommand.testing;

import dev.riege.buildmycommand.api.CommandResult;
import dev.traveler.command.buildmycommand.TravelerCommandModule;
import java.time.Duration;
import java.util.Objects;
import java.util.function.BooleanSupplier;

public final class CommandModuleTestHarness {
    private static final Duration JOB_TIMEOUT = Duration.ofSeconds(2L);

    private CommandModuleTestHarness() {}

    public static CommandResult dispatchAndDrain(
            TravelerCommandModule module,
            TestCommandSource source,
            String command) {
        TravelerCommandModule safeModule = Objects.requireNonNull(module, "module");
        CommandResult result = safeModule.framework().dispatch(source, command);
        waitForJobs(safeModule, () -> safeModule.debugState().latestResult().isPresent());
        return result;
    }

    public static void waitForJobs(TravelerCommandModule module, BooleanSupplier condition) {
        TravelerCommandModule safeModule = Objects.requireNonNull(module, "module");
        BooleanSupplier safeCondition = Objects.requireNonNull(condition, "condition");
        long deadline = System.nanoTime() + JOB_TIMEOUT.toNanos();
        while (!safeCondition.getAsBoolean() && System.nanoTime() < deadline) {
            safeModule.drainPathJobs();
            Thread.onSpinWait();
        }
        if (!safeCondition.getAsBoolean()) {
            throw new AssertionError("Timed out waiting for Traveler command jobs.");
        }
    }
}

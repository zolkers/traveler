package dev.traveler.mc.v1_21_11.common.command;

import dev.traveler.core.job.PathJob;
import java.util.Objects;
import java.util.Optional;

record TravelerPathSearchSubmission(
        Optional<PathJob<TravelerPathSearchResult>> job,
        Optional<TravelerPathSearchResult> immediateResult) {
    TravelerPathSearchSubmission {
        job = Objects.requireNonNull(job, "job");
        immediateResult = Objects.requireNonNull(immediateResult, "immediateResult");
        if (job.isPresent() == immediateResult.isPresent()) {
            throw new IllegalArgumentException("Submission must contain exactly one outcome.");
        }
    }

    static TravelerPathSearchSubmission queued(PathJob<TravelerPathSearchResult> job) {
        return new TravelerPathSearchSubmission(Optional.of(Objects.requireNonNull(job, "job")), Optional.empty());
    }

    static TravelerPathSearchSubmission immediate(TravelerPathSearchResult result) {
        return new TravelerPathSearchSubmission(
                Optional.empty(),
                Optional.of(Objects.requireNonNull(result, "result")));
    }
}

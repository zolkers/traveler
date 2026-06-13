package dev.traveler.mc.v1_21_11.common.command;

import dev.traveler.core.job.PathJob;
import java.util.Objects;
import java.util.Optional;

record TravelerPathSearchSubmission(
        Optional<PathJob<TravelerPathSearchResult>> job,
        Optional<TravelerPathSearchResult> immediateResult,
        Optional<TravelerPathSearchService.SnapshotBlockSearch> snapshotSearch) {
    TravelerPathSearchSubmission {
        job = Objects.requireNonNull(job, "job");
        immediateResult = Objects.requireNonNull(immediateResult, "immediateResult");
        snapshotSearch = Objects.requireNonNull(snapshotSearch, "snapshotSearch");
        if (outcomeCount(job, immediateResult, snapshotSearch) != 1) {
            throw new IllegalArgumentException("Submission must contain exactly one outcome.");
        }
    }

    static TravelerPathSearchSubmission queued(PathJob<TravelerPathSearchResult> job) {
        return new TravelerPathSearchSubmission(
                Optional.of(Objects.requireNonNull(job, "job")),
                Optional.empty(),
                Optional.empty());
    }

    static TravelerPathSearchSubmission immediate(TravelerPathSearchResult result) {
        return new TravelerPathSearchSubmission(
                Optional.empty(),
                Optional.of(Objects.requireNonNull(result, "result")),
                Optional.empty());
    }

    static TravelerPathSearchSubmission snapshot(TravelerPathSearchService.SnapshotBlockSearch snapshotSearch) {
        return new TravelerPathSearchSubmission(
                Optional.empty(),
                Optional.empty(),
                Optional.of(Objects.requireNonNull(snapshotSearch, "snapshotSearch")));
    }

    private static int outcomeCount(
            Optional<?> first,
            Optional<?> second,
            Optional<?> third) {
        return present(first) + present(second) + present(third);
    }

    private static int present(Optional<?> value) {
        return value.isPresent() ? 1 : 0;
    }
}

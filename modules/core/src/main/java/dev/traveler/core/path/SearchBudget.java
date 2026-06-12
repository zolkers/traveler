package dev.traveler.core.path;

final class SearchBudget {
    private final boolean unlimited;
    private final long deadlineNanos;

    private SearchBudget(boolean unlimited, long deadlineNanos) {
        this.unlimited = unlimited;
        this.deadlineNanos = deadlineNanos;
    }

    static SearchBudget unlimited() {
        return new SearchBudget(true, 0L);
    }

    static SearchBudget limited(long budgetNanos) {
        if (budgetNanos < 0L) {
            throw new IllegalArgumentException("Search budget must be non-negative.");
        }
        if (budgetNanos == Long.MAX_VALUE) {
            return unlimited();
        }
        return new SearchBudget(false, System.nanoTime() + Math.max(1L, budgetNanos));
    }

    boolean isSpent() {
        if (unlimited) {
            return false;
        }
        return System.nanoTime() - deadlineNanos >= 0L;
    }
}

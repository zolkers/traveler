package dev.traveler.core.command;

import java.util.Objects;
import java.util.Optional;

@FunctionalInterface
public interface TravelerCommandSource {
    <T> Optional<T> unwrap(Class<T> type);

    static TravelerCommandSource empty() {
        return new EmptySource();
    }

    final class EmptySource implements TravelerCommandSource {
        @Override
        public <T> Optional<T> unwrap(Class<T> type) {
            Objects.requireNonNull(type, "type");
            return Optional.empty();
        }
    }
}

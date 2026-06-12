package dev.traveler.core.layer;

import java.util.Objects;

@FunctionalInterface
public interface BlockClassifier<C> {
    BlockClassification classifyContext(C context);

    default BlockClassification classify(C context) {
        return classifyContext(Objects.requireNonNull(context, "context"));
    }
}

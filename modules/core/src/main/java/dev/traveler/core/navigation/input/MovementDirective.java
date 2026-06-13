package dev.traveler.core.navigation.input;

import dev.traveler.core.navigation.spatial.HorizontalVector;
import java.util.Objects;

record MovementDirective(HorizontalVector desiredVector, boolean allowsSpecialAction) {
    MovementDirective {
        Objects.requireNonNull(desiredVector, "desiredVector");
    }
}

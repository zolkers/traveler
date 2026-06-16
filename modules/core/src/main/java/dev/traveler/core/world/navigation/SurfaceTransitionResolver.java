package dev.traveler.core.world.navigation;

import dev.traveler.core.world.behavior.decision.MovementDecision;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record SurfaceTransitionResolver(List<SurfaceTransitionProvider> providers) {
    public SurfaceTransitionResolver {
        providers = List.copyOf(Objects.requireNonNull(providers, "providers"));
    }

    public MovementDecision decision(SurfaceTransitionContext context) {
        SurfaceTransitionContext safeContext = Objects.requireNonNull(context, "context");
        for (SurfaceTransitionProvider provider : providers) {
            Optional<MovementDecision> decision = provider.decision(safeContext);
            if (decision.isPresent()) {
                return decision.orElseThrow();
            }
        }
        return MovementDecision.blocked();
    }
}

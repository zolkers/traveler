package dev.traveler.core.world.behavior.special;

import dev.traveler.core.world.behavior.BlockBehaviorKey;
import dev.traveler.core.world.behavior.context.HorizontalFacing;
import java.util.Objects;
import java.util.Set;

public final class VineBlockBehavior extends ClimbableBlockBehavior {
    private static final Set<HorizontalFacing> CEILING_FACES = Set.of(
            HorizontalFacing.NORTH,
            HorizontalFacing.SOUTH,
            HorizontalFacing.WEST,
            HorizontalFacing.EAST);

    private final Set<HorizontalFacing> attachedFaces;
    private final boolean ceilingAttached;

    public VineBlockBehavior(Set<HorizontalFacing> attachedFaces, boolean ceilingAttached) {
        this.attachedFaces = Set.copyOf(Objects.requireNonNull(attachedFaces, "attachedFaces"));
        this.ceilingAttached = ceilingAttached;
        if (this.attachedFaces.isEmpty() && !ceilingAttached) {
            throw new IllegalArgumentException("Vines must attach to at least one face.");
        }
    }

    @Override
    public BlockBehaviorKey key() {
        return BlockBehaviorKey.VINE;
    }

    public Set<HorizontalFacing> attachedFaces() {
        return attachedFaces;
    }

    public boolean ceilingAttached() {
        return ceilingAttached;
    }

    @Override
    public Set<HorizontalFacing> climbableFaces() {
        if (attachedFaces.isEmpty()) {
            return CEILING_FACES;
        }
        return attachedFaces.stream()
                .map(HorizontalFacing::opposite)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}

package dev.traveler.core.layer;

import dev.traveler.core.world.behavior.context.HorizontalFacing;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record BlockBehaviorSpec(
        Kind kind,
        Optional<HorizontalFacing> facing,
        Set<HorizontalFacing> attachedFaces,
        boolean ceilingAttached) {
    public BlockBehaviorSpec {
        Objects.requireNonNull(kind, "kind");
        facing = Optional.ofNullable(Objects.requireNonNull(facing, "facing").orElse(null));
        attachedFaces = copyAttachedFaces(attachedFaces);
        validate(kind, facing, attachedFaces);
    }

    public static BlockBehaviorSpec automatic() {
        return simple(Kind.AUTOMATIC);
    }

    public static BlockBehaviorSpec air() {
        return simple(Kind.AIR);
    }

    public static BlockBehaviorSpec fullBlock() {
        return simple(Kind.FULL_BLOCK);
    }

    public static BlockBehaviorSpec slab() {
        return simple(Kind.SLAB);
    }

    public static BlockBehaviorSpec stair(HorizontalFacing facing) {
        return new BlockBehaviorSpec(
                Kind.STAIR,
                Optional.of(Objects.requireNonNull(facing, "facing")),
                Set.of(),
                false);
    }

    public static BlockBehaviorSpec fluid() {
        return simple(Kind.FLUID);
    }

    public static BlockBehaviorSpec carpet() {
        return simple(Kind.CARPET);
    }

    public static BlockBehaviorSpec ladder(HorizontalFacing facing) {
        return new BlockBehaviorSpec(
                Kind.LADDER,
                Optional.of(Objects.requireNonNull(facing, "facing")),
                Set.of(),
                false);
    }

    public static BlockBehaviorSpec vine(Set<HorizontalFacing> attachedFaces, boolean ceilingAttached) {
        return new BlockBehaviorSpec(Kind.VINE, Optional.empty(), attachedFaces, ceilingAttached);
    }

    public static BlockBehaviorSpec fence() {
        return simple(Kind.FENCE);
    }

    public static BlockBehaviorSpec wall() {
        return simple(Kind.WALL);
    }

    private static BlockBehaviorSpec simple(Kind kind) {
        return new BlockBehaviorSpec(kind, Optional.empty(), Set.of(), false);
    }

    private static Set<HorizontalFacing> copyAttachedFaces(Set<HorizontalFacing> attachedFaces) {
        Objects.requireNonNull(attachedFaces, "attachedFaces");
        if (attachedFaces.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(EnumSet.copyOf(attachedFaces));
    }

    private static void validate(
            Kind kind,
            Optional<HorizontalFacing> facing,
            Set<HorizontalFacing> attachedFaces) {
        if ((kind == Kind.STAIR || kind == Kind.LADDER) && facing.isEmpty()) {
            throw new IllegalArgumentException(kind + " requires a horizontal facing.");
        }
        if (kind != Kind.STAIR && kind != Kind.LADDER && facing.isPresent()) {
            throw new IllegalArgumentException(kind + " does not accept a horizontal facing.");
        }
        if (kind != Kind.VINE && !attachedFaces.isEmpty()) {
            throw new IllegalArgumentException(kind + " does not accept attached faces.");
        }
    }

    public enum Kind {
        AUTOMATIC,
        AIR,
        FULL_BLOCK,
        SLAB,
        STAIR,
        FLUID,
        CARPET,
        LADDER,
        VINE,
        FENCE,
        WALL
    }
}

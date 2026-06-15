package dev.traveler.core.layer;

import dev.traveler.core.world.behavior.BlockBehavior;
import dev.traveler.core.world.behavior.BlockBehaviorClassificationPolicy;
import dev.traveler.core.world.behavior.BlockBehaviorKey;
import dev.traveler.core.world.behavior.BlockBehaviorRegistry;
import dev.traveler.core.world.behavior.special.WaterloggedBlockBehavior;
import dev.traveler.core.world.block.BlockPassability;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.core.world.movement.FluidHandling;
import java.util.Objects;

public final class SurfaceBlockFactory {
    private final BlockBehaviorRegistry behaviorRegistry;
    private final BlockBehaviorClassificationPolicy classificationPolicy;

    public SurfaceBlockFactory() {
        this(BlockBehaviorRegistry.defaults());
    }

    public SurfaceBlockFactory(BlockBehaviorRegistry behaviorRegistry) {
        this(behaviorRegistry, new BlockBehaviorClassificationPolicy());
    }

    public SurfaceBlockFactory(
            BlockBehaviorRegistry behaviorRegistry,
            BlockBehaviorClassificationPolicy classificationPolicy) {
        this.behaviorRegistry = Objects.requireNonNull(behaviorRegistry, "behaviorRegistry");
        this.classificationPolicy = Objects.requireNonNull(classificationPolicy, "classificationPolicy");
    }

    public SurfaceBlock create(SurfaceBlockSample sample) {
        SurfaceBlockSample safeSample = Objects.requireNonNull(sample, "sample");
        BlockBehavior dryBehavior = dryBehaviorFor(safeSample);
        BlockBehavior behavior = behaviorFor(safeSample, dryBehavior);
        BlockClassification classification = classificationPolicy.classify(baseClassification(safeSample), behavior);
        BlockShape shape = navigationShape(safeSample.collisionShape(), behavior);
        return new SurfaceBlock(classification, shape, behavior);
    }

    private BlockClassification baseClassification(SurfaceBlockSample sample) {
        return new BlockClassification(basePassability(sample), fluidHandling(sample));
    }

    private static BlockPassability basePassability(SurfaceBlockSample sample) {
        if (sample.air()) {
            return BlockPassability.PASSABLE;
        }
        if (!sample.collisionShape().isEmpty()) {
            return BlockPassability.SOLID;
        }
        if (sample.containsFluid()) {
            return BlockPassability.PASSABLE;
        }
        return BlockPassability.WALKABLE;
    }

    private static FluidHandling fluidHandling(SurfaceBlockSample sample) {
        return sample.containsFluid() ? FluidHandling.ALLOW : FluidHandling.AVOID;
    }

    private BlockBehavior behaviorFor(SurfaceBlockSample sample, BlockBehavior dryBehavior) {
        if (!sample.containsFluid()) {
            return dryBehavior;
        }
        if (sample.collisionShape().isEmpty() || dryBehavior.key() == BlockBehaviorKey.AIR) {
            return behaviorRegistry.behavior(BlockBehaviorKey.FLUID);
        }
        return behaviorRegistry.waterlogged(dryBehavior);
    }

    private BlockBehavior dryBehaviorFor(SurfaceBlockSample sample) {
        BlockBehaviorSpec spec = sample.behaviorSpec();
        return switch (spec.kind()) {
            case AUTOMATIC -> automaticBehavior(sample);
            case AIR -> behaviorRegistry.behavior(BlockBehaviorKey.AIR);
            case FULL_BLOCK -> behaviorRegistry.behavior(BlockBehaviorKey.FULL_BLOCK);
            case SLAB -> behaviorRegistry.behavior(BlockBehaviorKey.SLAB);
            case STAIR -> behaviorRegistry.stair(spec.facing().orElseThrow());
            case FLUID -> behaviorRegistry.behavior(BlockBehaviorKey.FLUID);
            case CARPET -> behaviorRegistry.behavior(BlockBehaviorKey.CARPET);
            case LADDER -> behaviorRegistry.ladder(spec.facing().orElseThrow());
            case VINE -> behaviorRegistry.vine(spec.attachedFaces(), spec.ceilingAttached());
            case FENCE -> behaviorRegistry.behavior(BlockBehaviorKey.FENCE);
            case WALL -> behaviorRegistry.behavior(BlockBehaviorKey.WALL);
        };
    }

    private BlockBehavior automaticBehavior(SurfaceBlockSample sample) {
        if (sample.air() || sample.collisionShape().isEmpty()) {
            return behaviorRegistry.behavior(BlockBehaviorKey.AIR);
        }
        return behaviorRegistry.behavior(BlockBehaviorKey.FULL_BLOCK);
    }

    private static BlockShape navigationShape(BlockShape rawShape, BlockBehavior behavior) {
        if (isClimbable(behavior)) {
            return BlockShape.empty();
        }
        return rawShape;
    }

    private static boolean isClimbable(BlockBehavior behavior) {
        if (behavior.key() == BlockBehaviorKey.LADDER || behavior.key() == BlockBehaviorKey.VINE) {
            return true;
        }
        if (behavior instanceof WaterloggedBlockBehavior waterlogged) {
            return isClimbable(waterlogged.delegate());
        }
        return false;
    }
}

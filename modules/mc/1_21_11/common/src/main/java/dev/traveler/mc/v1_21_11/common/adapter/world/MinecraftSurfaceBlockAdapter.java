package dev.traveler.mc.v1_21_11.common.adapter.world;

import dev.traveler.mc.v1_21_11.common.adapter.block.MinecraftBlockClassifier;
import dev.traveler.mc.v1_21_11.common.adapter.block.MinecraftBlockContext;
import dev.traveler.core.layer.BlockClassification;
import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.world.behavior.BlockBehavior;
import dev.traveler.core.world.behavior.BlockBehaviorKey;
import dev.traveler.core.world.behavior.BlockBehaviorRegistry;
import dev.traveler.core.world.behavior.context.HorizontalFacing;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.core.world.geometry.CollisionBox;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class MinecraftSurfaceBlockAdapter {
    private final MinecraftBlockClassifier classifier;
    private final BlockBehaviorRegistry behaviorRegistry;

    public MinecraftSurfaceBlockAdapter() {
        this(new MinecraftBlockClassifier(), BlockBehaviorRegistry.defaults());
    }

    public MinecraftSurfaceBlockAdapter(MinecraftBlockClassifier classifier, BlockBehaviorRegistry behaviorRegistry) {
        this.classifier = Objects.requireNonNull(classifier, "classifier");
        this.behaviorRegistry = Objects.requireNonNull(behaviorRegistry, "behaviorRegistry");
    }

    public SurfaceBlock surfaceBlock(MinecraftBlockContext context) {
        MinecraftBlockContext safeContext = Objects.requireNonNull(context, "context");
        BlockClassification classification = classifier.classify(safeContext);
        BlockShape shape = shapeOf(safeContext);
        return new SurfaceBlock(classification, shape, behaviorFor(safeContext.state(), shape));
    }

    private BlockShape shapeOf(MinecraftBlockContext context) {
        VoxelShape shape = context.state().getCollisionShape(context.blockGetter(), context.position());
        List<CollisionBox> boxes = new ArrayList<>();
        for (AABB box : shape.toAabbs()) {
            addBox(boxes, box);
        }
        return BlockShape.of(boxes);
    }

    private void addBox(List<CollisionBox> boxes, AABB box) {
        BoxBounds bounds = BoxBounds.clamped(box);
        if (!bounds.isValid()) {
            return;
        }
        boxes.add(new CollisionBox(
                bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ()));
    }

    private BlockBehavior behaviorFor(BlockState state, BlockShape shape) {
        BlockBehavior dryBehavior = dryBehaviorFor(state, shape);
        if (state.getFluidState().isEmpty()) {
            return dryBehavior;
        }
        if (shape.isEmpty()) {
            return behaviorRegistry.behavior(BlockBehaviorKey.FLUID);
        }
        return behaviorRegistry.waterlogged(dryBehavior);
    }

    private BlockBehavior dryBehaviorFor(BlockState state, BlockShape shape) {
        if (shape.isEmpty()) {
            return behaviorRegistry.behavior(BlockBehaviorKey.AIR);
        }
        if (state.getBlock() instanceof SlabBlock) {
            return behaviorRegistry.behavior(BlockBehaviorKey.SLAB);
        }
        if (StairBlock.isStairs(state)) {
            return behaviorRegistry.stair(horizontalFacing(state.getValue(StairBlock.FACING)));
        }
        return behaviorRegistry.behavior(BlockBehaviorKey.FULL_BLOCK);
    }

    private static HorizontalFacing horizontalFacing(Direction direction) {
        return switch (Objects.requireNonNull(direction, "direction")) {
            case NORTH -> HorizontalFacing.NORTH;
            case SOUTH -> HorizontalFacing.SOUTH;
            case WEST -> HorizontalFacing.WEST;
            case EAST -> HorizontalFacing.EAST;
            default -> throw new IllegalArgumentException("Expected horizontal stair facing, got " + direction + ".");
        };
    }

    private record BoxBounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        static BoxBounds clamped(AABB box) {
            Objects.requireNonNull(box, "box");
            return new BoxBounds(
                    clamp(box.minX),
                    clamp(box.minY),
                    clamp(box.minZ),
                    clamp(box.maxX),
                    clamp(box.maxY),
                    clamp(box.maxZ));
        }

        boolean isValid() {
            return maxX > minX && maxY > minY && maxZ > minZ;
        }

        private static double clamp(double value) {
            return Math.max(0.0, Math.min(1.0, value));
        }
    }
}

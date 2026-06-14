package dev.traveler.mc.v1_21_11.common.adapter.world;

import dev.traveler.mc.v1_21_11.common.adapter.block.MinecraftBlockClassifier;
import dev.traveler.mc.v1_21_11.common.adapter.block.MinecraftBlockContext;
import dev.traveler.core.layer.BlockClassification;
import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.world.behavior.BlockBehavior;
import dev.traveler.core.world.behavior.BlockBehaviorKey;
import dev.traveler.core.world.behavior.BlockBehaviorClassificationPolicy;
import dev.traveler.core.world.behavior.BlockBehaviorRegistry;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.core.world.geometry.CollisionBox;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class MinecraftSurfaceBlockAdapter {
    private final MinecraftBlockClassifier classifier;
    private final BlockBehaviorRegistry behaviorRegistry;
    private final BlockBehaviorClassificationPolicy classificationPolicy;
    private final List<MinecraftBlockBehaviorResolver> behaviorResolvers;

    public MinecraftSurfaceBlockAdapter() {
        this(new MinecraftBlockClassifier(), BlockBehaviorRegistry.defaults(), new BlockBehaviorClassificationPolicy());
    }

    public MinecraftSurfaceBlockAdapter(MinecraftBlockClassifier classifier, BlockBehaviorRegistry behaviorRegistry) {
        this(classifier, behaviorRegistry, new BlockBehaviorClassificationPolicy());
    }

    public MinecraftSurfaceBlockAdapter(
            MinecraftBlockClassifier classifier,
            BlockBehaviorRegistry behaviorRegistry,
            BlockBehaviorClassificationPolicy classificationPolicy) {
        this(classifier, behaviorRegistry, classificationPolicy, MinecraftBlockBehaviorCatalog.defaultResolvers());
    }

    public MinecraftSurfaceBlockAdapter(
            MinecraftBlockClassifier classifier,
            BlockBehaviorRegistry behaviorRegistry,
            BlockBehaviorClassificationPolicy classificationPolicy,
            List<MinecraftBlockBehaviorResolver> behaviorResolvers) {
        this.classifier = Objects.requireNonNull(classifier, "classifier");
        this.behaviorRegistry = Objects.requireNonNull(behaviorRegistry, "behaviorRegistry");
        this.classificationPolicy = Objects.requireNonNull(classificationPolicy, "classificationPolicy");
        this.behaviorResolvers = List.copyOf(Objects.requireNonNull(behaviorResolvers, "behaviorResolvers"));
        if (this.behaviorResolvers.isEmpty()) {
            throw new IllegalArgumentException("behaviorResolvers must not be empty.");
        }
    }

    public SurfaceBlock surfaceBlock(MinecraftBlockContext context) {
        MinecraftBlockContext safeContext = Objects.requireNonNull(context, "context");
        BlockShape shape = shapeOf(safeContext);
        BlockBehavior behavior = behaviorFor(safeContext, shape);
        BlockClassification classification = classificationFor(safeContext, behavior);
        return new SurfaceBlock(classification, shape, behavior);
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

    private BlockBehavior behaviorFor(MinecraftBlockContext context, BlockShape shape) {
        BlockBehavior dryBehavior = dryBehaviorFor(context, shape);
        if (context.state().getFluidState().isEmpty()) {
            return dryBehavior;
        }
        if (shape.isEmpty()) {
            return behaviorRegistry.behavior(BlockBehaviorKey.FLUID);
        }
        return behaviorRegistry.waterlogged(dryBehavior);
    }

    private BlockBehavior dryBehaviorFor(MinecraftBlockContext context, BlockShape shape) {
        for (MinecraftBlockBehaviorResolver resolver : behaviorResolvers) {
            Optional<BlockBehavior> behavior = resolver.resolve(context, shape, behaviorRegistry);
            if (behavior.isPresent()) {
                return behavior.orElseThrow();
            }
        }
        throw new IllegalStateException("No Minecraft block behavior resolver accepted the block state.");
    }

    private BlockClassification classificationFor(MinecraftBlockContext context, BlockBehavior behavior) {
        BlockClassification base = classifier.classify(context);
        return classificationPolicy.classify(base, behavior);
    }

    private record BoxBounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        static BoxBounds clamped(AABB box) {
            Objects.requireNonNull(box, "box");
            return new BoxBounds(
                    clampHorizontal(box.minX),
                    clampVerticalFloor(box.minY),
                    clampHorizontal(box.minZ),
                    clampHorizontal(box.maxX),
                    clampVerticalFloor(box.maxY),
                    clampHorizontal(box.maxZ));
        }

        boolean isValid() {
            return maxX > minX && maxY > minY && maxZ > minZ;
        }

        private static double clampHorizontal(double value) {
            return Math.max(0.0, Math.min(1.0, value));
        }

        private static double clampVerticalFloor(double value) {
            return Math.max(0.0, value);
        }
    }
}

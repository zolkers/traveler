package dev.traveler.mc.v1_21_11.common.adapter.world;

import dev.traveler.mc.v1_21_11.common.adapter.block.MinecraftBlockContext;
import dev.traveler.core.layer.BlockBehaviorSpec;
import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceBlockFactory;
import dev.traveler.core.layer.SurfaceBlockSample;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.core.world.geometry.CollisionBox;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class MinecraftSurfaceBlockAdapter {
    private final SurfaceBlockFactory surfaceBlockFactory;

    public MinecraftSurfaceBlockAdapter() {
        this(new SurfaceBlockFactory());
    }

    public MinecraftSurfaceBlockAdapter(SurfaceBlockFactory surfaceBlockFactory) {
        this.surfaceBlockFactory = Objects.requireNonNull(surfaceBlockFactory, "surfaceBlockFactory");
    }

    public SurfaceBlock surfaceBlock(MinecraftBlockContext context) {
        MinecraftBlockContext safeContext = Objects.requireNonNull(context, "context");
        BlockShape collisionShape = shapeOf(safeContext);
        SurfaceBlockSample sample = new SurfaceBlockSample(
                safeContext.state().isAir(),
                !safeContext.state().getFluidState().isEmpty(),
                collisionShape,
                behaviorSpecFor(safeContext, collisionShape));
        return surfaceBlockFactory.create(sample);
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

    private BlockBehaviorSpec behaviorSpecFor(MinecraftBlockContext context, BlockShape shape) {
        return MinecraftBlockBehaviorSpecCatalog.resolve(context, shape);
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

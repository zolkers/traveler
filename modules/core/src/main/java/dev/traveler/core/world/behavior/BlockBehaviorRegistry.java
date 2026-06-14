package dev.traveler.core.world.behavior;

import dev.traveler.core.world.behavior.context.HorizontalFacing;
import dev.traveler.core.world.behavior.special.AirBlockBehavior;
import dev.traveler.core.world.behavior.special.CarpetBlockBehavior;
import dev.traveler.core.world.behavior.special.FenceBlockBehavior;
import dev.traveler.core.world.behavior.special.FluidBlockBehavior;
import dev.traveler.core.world.behavior.special.FullBlockBehavior;
import dev.traveler.core.world.behavior.special.LadderBlockBehavior;
import dev.traveler.core.world.behavior.special.SlabBlockBehavior;
import dev.traveler.core.world.behavior.special.StairBlockBehavior;
import dev.traveler.core.world.behavior.special.VineBlockBehavior;
import dev.traveler.core.world.behavior.special.WallBlockBehavior;
import dev.traveler.core.world.behavior.special.WaterloggedBlockBehavior;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class BlockBehaviorRegistry {
    private final Map<BlockBehaviorKey, BlockBehavior> behaviors = new EnumMap<>(BlockBehaviorKey.class);
    private final Map<HorizontalFacing, BlockBehavior> stairs = new EnumMap<>(HorizontalFacing.class);
    private final Map<HorizontalFacing, BlockBehavior> ladders = new EnumMap<>(HorizontalFacing.class);

    private BlockBehaviorRegistry() {}

    public static BlockBehaviorRegistry defaults() {
        BlockBehaviorRegistry registry = new BlockBehaviorRegistry();
        registry.register(new AirBlockBehavior());
        registry.register(new FullBlockBehavior());
        registry.register(new SlabBlockBehavior());
        registry.register(registry.stair(HorizontalFacing.NORTH));
        registry.register(new FluidBlockBehavior());
        registry.register(new WaterloggedBlockBehavior(registry.behavior(BlockBehaviorKey.FULL_BLOCK)));
        registry.register(new CarpetBlockBehavior());
        registry.register(registry.ladder(HorizontalFacing.NORTH));
        registry.register(registry.vine(Set.of(HorizontalFacing.NORTH), false));
        registry.register(new FenceBlockBehavior());
        registry.register(new WallBlockBehavior());
        return registry;
    }

    public void register(BlockBehavior behavior) {
        BlockBehavior safeBehavior = Objects.requireNonNull(behavior, "behavior");
        behaviors.put(safeBehavior.key(), safeBehavior);
    }

    public BlockBehavior behavior(BlockBehaviorKey key) {
        BlockBehaviorKey safeKey = Objects.requireNonNull(key, "key");
        BlockBehavior behavior = behaviors.get(safeKey);
        if (behavior == null) {
            throw new IllegalStateException("No block behavior registered for " + safeKey + ".");
        }
        return behavior;
    }

    public BlockBehavior stair(HorizontalFacing facing) {
        HorizontalFacing safeFacing = Objects.requireNonNull(facing, "facing");
        return stairs.computeIfAbsent(safeFacing, StairBlockBehavior::new);
    }

    public BlockBehavior ladder(HorizontalFacing facing) {
        HorizontalFacing safeFacing = Objects.requireNonNull(facing, "facing");
        return ladders.computeIfAbsent(safeFacing, LadderBlockBehavior::new);
    }

    public BlockBehavior vine(Set<HorizontalFacing> attachedFaces, boolean ceilingAttached) {
        return new VineBlockBehavior(attachedFaces, ceilingAttached);
    }

    public BlockBehavior waterlogged(BlockBehavior delegate) {
        return new WaterloggedBlockBehavior(Objects.requireNonNull(delegate, "delegate"));
    }
}

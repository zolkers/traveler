package dev.traveler.mc.v1_21_11.common.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.traveler.command.buildmycommand.TravelerCommandModule;
import dev.traveler.core.command.TravelerCommandBlockPosition;
import dev.traveler.core.command.TravelerCommandPosition;
import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.mc.v1_21_11.common.adapter.testing.AbstractTestBlockGetter;
import dev.traveler.mc.v1_21_11.common.adapter.testing.MinecraftTestBootstrap;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MinecraftTravelerCommandBridgeTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.bootstrap();
    }

    @Test
    void bridgeCreatesCoreCommandModuleBackedByCapturedMinecraftSnapshot() {
        SnapshotOnlyBlockGetter blockGetter = new SnapshotOnlyBlockGetter();
        TravelerCommandModule module =
                MinecraftTravelerCommandBridge.create(new PathfinderDebugState(), () -> blockGetter);

        CommandResult result = dispatchAndDrain(
                module,
                new TestSource(new BlockPosition(0, 64, 0)),
                "traveler path block 2 63 0");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertTrue(blockGetter.readDuringCapture);
        assertTrue(module.debugState().latestSnapshot().orElseThrow().hasSurfaceNodes());
    }

    @Test
    void bridgeDoesNotReadMinecraftWorldBeforeQueuedCoreWorkRuns() {
        CountingBlockGetter blockGetter = new CountingBlockGetter();
        TravelerCommandModule module =
                MinecraftTravelerCommandBridge.create(new PathfinderDebugState(), () -> blockGetter);

        CommandResult result = module.framework().dispatch(
                new TestSource(new BlockPosition(0, 64, 0)),
                "traveler path block 2 63 0");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertTrue(result.reply().orElseThrow().contains("path queued id="));
        assertEquals(0, blockGetter.blockReads);

        waitForJobs(module, () -> blockGetter.blockReads > 0);
    }

    private static CommandResult dispatchAndDrain(TravelerCommandModule module, TestSource source, String command) {
        CommandResult result = module.framework().dispatch(source, command);
        waitForJobs(module, () -> module.debugState().latestResult().isPresent());
        return result;
    }

    private static void waitForJobs(TravelerCommandModule module, BooleanSupplier condition) {
        long deadline = System.nanoTime() + Duration.ofSeconds(2L).toNanos();
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            module.drainPathJobs();
            Thread.onSpinWait();
        }
        assertTrue(condition.getAsBoolean());
    }

    private static final class TestSource implements CommandSource, TravelerCommandPosition {
        private final List<String> replies = new ArrayList<>();
        private final BlockPosition position;

        private TestSource(BlockPosition position) {
            this.position = position;
        }

        @Override
        public Optional<TravelerCommandBlockPosition> blockPosition() {
            return Optional.ofNullable(position)
                    .map(pos -> new TravelerCommandBlockPosition(pos.x(), pos.y(), pos.z()));
        }

        @Override
        public <T> Optional<T> unwrap(Class<T> type) {
            if (!type.isInstance(this)) {
                return Optional.empty();
            }
            return Optional.of(type.cast(this));
        }

        @Override
        public void reply(String message) {
            replies.add(message);
        }
    }

    private static final class SnapshotOnlyBlockGetter extends AbstractTestBlockGetter {
        private boolean readDuringCapture;

        @Override
        public BlockState getBlockState(BlockPos position) {
            requireSnapshotCapture();
            readDuringCapture = true;
            if (position.getY() == 63) {
                return Blocks.STONE.defaultBlockState();
            }
            return Blocks.AIR.defaultBlockState();
        }

        @Override
        public FluidState getFluidState(BlockPos position) {
            return getBlockState(position).getFluidState();
        }

        private static void requireSnapshotCapture() {
            boolean captureStack = StackWalker.getInstance().walk(frames -> frames.anyMatch(
                    frame -> frame.getClassName().contains("ImmutableMinecraftWorldSnapshot")));
            if (!captureStack) {
                throw new AssertionError("live minecraft world read outside immutable path snapshot capture");
            }
        }
    }

    private static final class CountingBlockGetter extends AbstractTestBlockGetter {
        private int blockReads;

        @Override
        public BlockState getBlockState(BlockPos position) {
            blockReads++;
            if (position.getY() == 63) {
                return Blocks.STONE.defaultBlockState();
            }
            return Blocks.AIR.defaultBlockState();
        }

        @Override
        public FluidState getFluidState(BlockPos position) {
            return getBlockState(position).getFluidState();
        }
    }
}

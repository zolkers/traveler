package dev.traveler.mc.v1_21_11.common.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riege.buildmycommand.api.CommandResult;
import dev.traveler.command.buildmycommand.TravelerCommandModule;
import dev.traveler.command.buildmycommand.testing.CommandModuleTestHarness;
import dev.traveler.command.buildmycommand.testing.TestCommandSource;
import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.layer.WorldNavigationBudget;
import dev.traveler.core.navigation.TravelerNavigationState;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.mc.v1_21_11.common.adapter.testing.AbstractTestBlockGetter;
import dev.traveler.mc.v1_21_11.common.adapter.testing.MinecraftTestBootstrap;
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

        CommandResult result = CommandModuleTestHarness.dispatchAndDrain(
                module,
                new TestCommandSource(new BlockPosition(0, 64, 0)),
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
                new TestCommandSource(new BlockPosition(0, 64, 0)),
                "traveler path block 2 63 0");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertTrue(result.reply().orElseThrow().contains("path queued id="));
        assertEquals(0, blockGetter.blockReads);

        CommandModuleTestHarness.waitForJobs(module, () -> blockGetter.blockReads > 0);
    }

    @Test
    void bridgePassesNavigationBudgetToCorePathSearch() {
        SnapshotOnlyBlockGetter blockGetter = new SnapshotOnlyBlockGetter();
        TravelerCommandModule module = MinecraftTravelerCommandBridge.create(
                new PathfinderDebugState(),
                new TravelerNavigationState(),
                () -> blockGetter,
                () -> new WorldNavigationBudget(64));
        TestCommandSource source = new TestCommandSource(new BlockPosition(0, 64, 0));

        CommandResult result = CommandModuleTestHarness.dispatchAndDrain(
                module, source, "traveler path xz 10000 10000");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertTrue(source.replies().stream().anyMatch(reply -> reply.contains("active=32,64,32")));
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

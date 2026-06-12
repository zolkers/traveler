package dev.traveler.mc.v1_21_11.common.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.traveler.core.graph.GraphPath;
import dev.traveler.core.layer.BlockClassification;
import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.path.PathfinderResult;
import dev.traveler.core.world.BlockPosition;
import dev.traveler.core.world.BlockPassability;
import dev.traveler.core.world.FluidHandling;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TravelerCommandModuleTest {
    @Test
    void exposesModularCommandCatalog() {
        TravelerCommandModule module = new TravelerCommandModule();

        assertEquals(2, module.catalog().routes().size());
        assertTrue(module.catalog().route("traveler path test").isPresent());
        assertTrue(module.catalog().route("traveler path block <x:int> <y:int> <z:int>").isPresent());
    }

    @Test
    void registersPathTestCommandAndUpdatesDebugState() {
        TravelerCommandModule module = new TravelerCommandModule();
        TestSource source = new TestSource();

        CommandResult result = module.framework().dispatch(source, "traveler path test");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertTrue(module.debugState().latestResult().isPresent());
        assertTrue(module.debugState().latestMessage().orElseThrow().contains("path test"));
        assertEquals(List.of(result.reply().orElseThrow()), source.replies());
    }

    @Test
    void registersPathBlockCommandAndIncludesTargetInDebugMessage() {
        TravelerCommandModule module = new TravelerCommandModule();

        CommandResult result = module.framework().dispatch(new TestSource(), "traveler path block 1 2 3");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertTrue(module.debugState().latestResult().isPresent());
        assertTrue(module.debugState().latestMessage().orElseThrow().contains("1,2,3"));
    }

    @Test
    void pathBlockStartsAtCommandSourcePositionWhenAvailable() {
        TravelerCommandModule module = new TravelerCommandModule();
        BlockPosition start = new BlockPosition(8, 70, -4);

        CommandResult result = module.framework().dispatch(new TestSource(start), "traveler path block 1 2 3");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        PathfinderResult<BlockPosition> pathResult = module.debugState().latestResult().orElseThrow();
        GraphPath<BlockPosition> path = pathResult.path();
        assertEquals(start, path.nodeAt(0));
        assertEquals(new BlockPosition(1, 2, 3), path.nodeAt(path.nodeCount() - 1));
    }

    @Test
    void pathBlockUsesWorldTraversalWhenLayerIsAvailable() {
        BlockPosition start = new BlockPosition(0, 64, 0);
        BlockPosition goal = new BlockPosition(2, 64, 0);
        BlockPosition wall = new BlockPosition(1, 64, 0);
        TravelerCommandModule module = new TravelerCommandModule(new TestWorldLayer(Set.of(wall, wall.above())));

        module.framework().dispatch(new TestSource(start), "traveler path block 2 64 0");

        GraphPath<BlockPosition> path = module.debugState().latestResult().orElseThrow().path();
        assertTrue(path.nodeCount() > 3);
        assertPathAvoids(path, wall);
        assertEquals(goal, path.nodeAt(path.nodeCount() - 1));
    }

    private static void assertPathAvoids(GraphPath<BlockPosition> path, BlockPosition blocked) {
        for (BlockPosition node : path) {
            assertTrue(!node.equals(blocked));
        }
    }

    private static final class TestSource implements CommandSource, TravelerCommandPosition {
        private final List<String> replies = new ArrayList<>();
        private final BlockPosition position;

        private TestSource() {
            this(null);
        }

        private TestSource(BlockPosition position) {
            this.position = position;
        }

        private List<String> replies() {
            return List.copyOf(replies);
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

    private record TestWorldLayer(Set<BlockPosition> blockedFeet) implements WorldLayer {
        private TestWorldLayer {
            blockedFeet = new HashSet<>(blockedFeet);
        }

        @Override
        public BlockClassification classify(BlockPosition position) {
            if (blockedFeet.contains(position)) {
                return new BlockClassification(BlockPassability.SOLID, FluidHandling.AVOID);
            }
            if (position.y() == 63) {
                return new BlockClassification(BlockPassability.SOLID, FluidHandling.AVOID);
            }
            return new BlockClassification(BlockPassability.PASSABLE, FluidHandling.AVOID);
        }
    }
}

package dev.traveler.mc.v1_21_11.common.command;

import dev.riege.buildmycommand.annotation.AnnotationCommandScanner;
import dev.riege.buildmycommand.annotation.Route;
import dev.riege.buildmycommand.annotation.RouteCtx;
import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;
import dev.traveler.core.graph.Connection;
import dev.traveler.core.graph.Graph;
import dev.traveler.core.path.AStarPathfinder;
import dev.traveler.core.path.PathfinderRequest;
import dev.traveler.core.path.PathfinderResult;
import dev.traveler.core.world.BlockPassability;
import dev.traveler.core.world.BlockPosition;
import dev.traveler.mc.v1_21_11.common.adapter.MinecraftBlockClassifier;
import dev.traveler.mc.v1_21_11.common.adapter.MinecraftWorldSnapshot;
import dev.traveler.mc.v1_21_11.common.debug.PathfinderDebugState;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public final class TravelerCommandModule {
    private static final BlockPosition TEST_START = new BlockPosition(0, 64, 0);
    private static final BlockPosition TEST_GOAL = new BlockPosition(3, 64, 0);

    private final CommandFramework framework;
    private final PathfinderDebugState debugState;
    private final Supplier<? extends BlockGetter> blockGetterSupplier;
    private final MinecraftBlockClassifier classifier = new MinecraftBlockClassifier();

    public TravelerCommandModule() {
        this(new PathfinderDebugState(), () -> null);
    }

    public TravelerCommandModule(PathfinderDebugState debugState, Supplier<? extends BlockGetter> blockGetterSupplier) {
        this.debugState = Objects.requireNonNull(debugState, "debugState");
        this.blockGetterSupplier = Objects.requireNonNull(blockGetterSupplier, "blockGetterSupplier");
        framework = CommandFramework.builder().middleware(TravelerCommandModule::replyWithResultMessage).build();
        AnnotationCommandScanner.register(framework.registry(), this);
    }

    public CommandFramework framework() {
        return framework;
    }

    public PathfinderDebugState debugState() {
        return debugState;
    }

    @Route("traveler path test")
    public CommandResult pathTest(@RouteCtx CommandContext context) {
        PathfinderResult<BlockPosition> result = findPath(TEST_START, TEST_GOAL);
        String message = "path test status=" + result.status() + " nodes=" + result.path().nodeCount();
        debugState.update(result, message);
        return Results.success(message);
    }

    @Route("traveler path block <x:int> <y:int> <z:int>")
    public CommandResult pathBlock(@RouteCtx CommandContext context) {
        BlockPosition target = new BlockPosition(
                context.arg("x", int.class), context.arg("y", int.class), context.arg("z", int.class));
        PathfinderResult<BlockPosition> result = findPath(target.above(), target);
        String message = blockMessage(target, result);
        debugState.update(result, message);
        return Results.success(message);
    }

    private String blockMessage(BlockPosition target, PathfinderResult<BlockPosition> result) {
        BlockGetter blockGetter = blockGetterSupplier.get();
        if (blockGetter == null) {
            return "path block " + format(target) + " status=" + result.status() + " world=unavailable";
        }
        MinecraftWorldSnapshot snapshot = new MinecraftWorldSnapshot(blockGetter);
        BlockPos position = MinecraftWorldSnapshot.toMinecraft(target);
        BlockState blockState = snapshot.blockState(position);
        FluidState fluidState = snapshot.fluidState(position);
        BlockPassability passability = classifier.classify(blockState, blockGetter, position);
        return "path block "
                + format(target)
                + " status="
                + result.status()
                + " passability="
                + passability
                + " fluid="
                + classifier.hasFluid(fluidState);
    }

    private PathfinderResult<BlockPosition> findPath(BlockPosition start, BlockPosition goal) {
        PathfinderRequest<BlockPosition> request = new PathfinderRequest<>(
                new DirectBlockGraph(goal), start, goal, TravelerCommandModule::distance);
        return new AStarPathfinder<BlockPosition>().search(request);
    }

    private static double distance(BlockPosition from, BlockPosition to) {
        return Math.abs(from.x() - to.x()) + Math.abs(from.y() - to.y()) + Math.abs(from.z() - to.z());
    }

    private static String format(BlockPosition position) {
        return position.x() + "," + position.y() + "," + position.z();
    }

    private static CommandResult replyWithResultMessage(
            CommandContext context,
            CommandNode command,
            List<String> commandPath,
            dev.riege.buildmycommand.api.CommandMiddleware.Chain next) {
        CommandResult result = next.proceed(context);
        result.message().ifPresent(context.source()::reply);
        return result;
    }

    private static final class DirectBlockGraph implements Graph<BlockPosition> {
        private final BlockPosition goal;

        private DirectBlockGraph(BlockPosition goal) {
            this.goal = Objects.requireNonNull(goal, "goal");
        }

        @Override
        public Iterable<Connection<BlockPosition>> outgoingConnections(BlockPosition node) {
            if (node.equals(goal)) {
                return List.of();
            }
            return List.of(new Connection<>(node, goal, distance(node, goal)));
        }
    }
}

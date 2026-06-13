package dev.traveler.core.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.graph.MutableGraphPath;
import dev.traveler.core.navigation.NavigationControlFrame;
import dev.traveler.core.navigation.NavigationControllerState;
import dev.traveler.core.navigation.NavigationFrameInput;
import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.control.MovementIntent;
import dev.traveler.core.navigation.follow.MovementTarget;
import dev.traveler.core.navigation.follow.PathProgress;
import dev.traveler.core.navigation.locomotion.LocomotionExecutionState;
import dev.traveler.core.navigation.plan.ActionIntent;
import dev.traveler.core.navigation.plan.MovementVectorIntent;
import dev.traveler.core.navigation.plan.NavigationFramePlan;
import dev.traveler.core.navigation.plan.NavigationPhase;
import dev.traveler.core.navigation.plan.PlannedMovementMode;
import dev.traveler.core.navigation.plan.SpeedIntent;
import dev.traveler.core.navigation.plan.ToleranceProfile;
import dev.traveler.core.navigation.spatial.HorizontalVector;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.path.PathfinderResult;
import dev.traveler.core.path.PathfinderStatus;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.surface.SurfaceNode;
import org.junit.jupiter.api.Test;

class PathDebugRenderModelTest {
    @Test
    void emptyStateProducesEmptyFrame() {
        PathDebugRenderModel model = PathDebugRenderModel.defaultModel();

        DebugRenderFrame frame = model.frameFor(new PathfinderDebugState().snapshot());

        assertTrue(frame.lines().isEmpty());
    }

    @Test
    void nonFoundResultProducesEmptyFrame() {
        PathDebugRenderModel model = PathDebugRenderModel.defaultModel();
        PathfinderDebugState state = new PathfinderDebugState();
        state.update(new PathfinderResult<>(PathfinderStatus.NOT_FOUND, new MutableGraphPath<>()));

        DebugRenderFrame frame = model.frameFor(state.snapshot());

        assertTrue(frame.lines().isEmpty());
    }

    @Test
    void pathNodesBecomeCenteredLineSegmentsAndTransparentBlockBoxes() {
        ColorRgba color = new ColorRgba(0.1f, 0.6f, 1.0f, 0.85f);
        PathDebugRenderModel model = PathDebugRenderModel.defaultModel();
        PathfinderDebugState state = new PathfinderDebugState();
        state.update(foundPath(
                new BlockPosition(1, 64, 1),
                new BlockPosition(2, 64, 1),
                new BlockPosition(3, 65, 1)));

        DebugRenderFrame frame = model.frameFor(state.snapshot());

        assertEquals(3, frame.lines().size());
        assertEquals(3, frame.boxes().size());
        assertEquals(new RenderVertex(1.5, 64.5, 1.5), frame.lines().getFirst().from());
        assertEquals(new RenderVertex(2.5, 64.5, 1.5), frame.lines().getFirst().to());
        assertEquals(new RenderVertex(2.5, 64.5, 1.5), frame.lines().get(1).from());
        assertEquals(new RenderVertex(2.5, 65.5, 1.5), frame.lines().get(1).to());
        assertEquals(new RenderVertex(2.5, 65.5, 1.5), frame.lines().get(2).from());
        assertEquals(new RenderVertex(3.5, 65.5, 1.5), frame.lines().get(2).to());
        assertEquals(new RenderVertex(1.0, 64.0, 1.0), frame.boxes().getFirst().min());
        assertEquals(new RenderVertex(2.0, 65.0, 2.0), frame.boxes().getFirst().max());
        assertTrue(frame.boxes().getFirst().color().alpha() < color.alpha());
        assertEquals(0.08, frame.lines().getFirst().thickness());
    }

    @Test
    void stepDownSegmentsMoveHorizontallyBeforeDropping() {
        ColorRgba color = new ColorRgba(0.1f, 0.6f, 1.0f, 0.85f);
        PathDebugRenderModel model = new PathDebugRenderModel(color, 0.35);
        PathfinderDebugState state = new PathfinderDebugState();
        state.update(foundPath(
                new BlockPosition(2, 65, 1),
                new BlockPosition(3, 64, 1),
                new BlockPosition(4, 64, 1)));

        DebugRenderFrame frame = model.frameFor(state.snapshot());

        assertEquals(new RenderVertex(2.5, 65.35, 1.5), frame.lines().getFirst().from());
        assertEquals(new RenderVertex(3.5, 65.35, 1.5), frame.lines().getFirst().to());
        assertEquals(new RenderVertex(3.5, 65.35, 1.5), frame.lines().get(1).from());
        assertEquals(new RenderVertex(3.5, 64.35, 1.5), frame.lines().get(1).to());
    }

    @Test
    void surfacePathsRenderLinesThroughVisualBlockCentersAndBoxesAroundVisualBlocks() {
        ColorRgba color = new ColorRgba(0.1f, 0.6f, 1.0f, 0.85f);
        PathDebugRenderModel model = new PathDebugRenderModel(color, 0.5);
        PathfinderDebugState state = new PathfinderDebugState();
        state.updateSurface(foundSurfacePath(
                new SurfaceNode(new BlockPosition(1, 63, 1), 1, 1, 63.5),
                new SurfaceNode(new BlockPosition(2, 63, 1), 0, 1, 64.0)));

        DebugRenderFrame frame = model.frameFor(state.snapshot());

        assertEquals(1, frame.lines().size());
        assertEquals(new RenderVertex(1.75, 63.5, 1.75), frame.lines().getFirst().from());
        assertEquals(new RenderVertex(2.25, 64.5, 1.75), frame.lines().getFirst().to());
        assertEquals(new RenderVertex(1.0, 63.0, 1.0), frame.boxes().getFirst().min());
        assertEquals(new RenderVertex(2.0, 64.0, 2.0), frame.boxes().getFirst().max());
        assertEquals(new RenderVertex(2.0, 64.0, 1.0), frame.boxes().get(1).min());
        assertEquals(new RenderVertex(3.0, 65.0, 2.0), frame.boxes().get(1).max());
    }

    @Test
    void surfacePathBoxesAreDeduplicatedPerVisualBlock() {
        ColorRgba color = new ColorRgba(0.1f, 0.6f, 1.0f, 0.85f);
        PathDebugRenderModel model = new PathDebugRenderModel(color, 0.35);
        BlockPosition support = new BlockPosition(1, 63, 1);
        PathfinderDebugState state = new PathfinderDebugState();
        state.updateSurface(foundSurfacePath(
                new SurfaceNode(support, 0, 0, 63.5),
                new SurfaceNode(support, 1, 1, 63.5)));

        DebugRenderFrame frame = model.frameFor(state.snapshot());

        assertEquals(1, frame.boxes().size());
        assertEquals(new RenderVertex(1.0, 63.0, 1.0), frame.boxes().getFirst().min());
        assertEquals(new RenderVertex(2.0, 64.0, 2.0), frame.boxes().getFirst().max());
    }

    @Test
    void navigationDebugAddsTargetMovementAndCameraOverlay() {
        PathDebugRenderModel model = PathDebugRenderModel.defaultModel();
        PathfinderDebugState state = new PathfinderDebugState();
        state.updateNavigation(
                new NavigationFrameInput(
                        new NavigationPoint(0.0, 64.0, 0.0),
                        new CameraAngles(0.0, 0.0),
                        0.016),
                navigationFrame());

        DebugRenderFrame frame = model.frameFor(state);

        assertEquals(3, frame.lines().size());
        assertEquals(1, frame.boxes().size());
        assertEquals(new RenderVertex(0.0, 65.2, 0.0), frame.lines().getFirst().from());
        assertEquals(new RenderVertex(0.0, 65.2, 1.5), frame.lines().getFirst().to());
        assertEquals(new RenderVertex(0.75, 64.75, 2.75), frame.boxes().getFirst().min());
        assertEquals(new RenderVertex(1.25, 65.25, 3.25), frame.boxes().getFirst().max());
    }

    @Test
    void colorChannelsMustStayInUnitRange() {
        assertThrows(IllegalArgumentException.class, () -> new ColorRgba(-0.1f, 0.0f, 0.0f, 1.0f));
        assertThrows(IllegalArgumentException.class, () -> new ColorRgba(0.0f, 1.1f, 0.0f, 1.0f));
    }

    @Test
    void lineThicknessMustBePositive() {
        ColorRgba color = new ColorRgba(0.1f, 0.6f, 1.0f, 0.85f);

        assertThrows(IllegalArgumentException.class, () -> new PathDebugRenderModel(color, 0.5, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new PathDebugRenderModel(color, 0.5, -0.01));
    }

    private static PathfinderResult<BlockPosition> foundPath(
            BlockPosition first, BlockPosition second, BlockPosition third) {
        MutableGraphPath<BlockPosition> path = new MutableGraphPath<>();
        path.addNode(first);
        path.addNode(second);
        path.addNode(third);
        return new PathfinderResult<>(PathfinderStatus.FOUND, path);
    }

    private static PathfinderResult<SurfaceNode> foundSurfacePath(SurfaceNode first, SurfaceNode second) {
        MutableGraphPath<SurfaceNode> path = new MutableGraphPath<>();
        path.addNode(first);
        path.addNode(second);
        return new PathfinderResult<>(PathfinderStatus.FOUND, path);
    }

    private static NavigationControlFrame navigationFrame() {
        MovementIntent intent = new MovementIntent(true, false, false, false, false, true);
        MovementTarget target = MovementTarget.follow(new NavigationPoint(1.0, 65.0, 3.0));
        NavigationFramePlan plan = new NavigationFramePlan(
                NavigationPhase.APPROACH,
                PathProgress.start(),
                target,
                new MovementVectorIntent(new HorizontalVector(0.0, 1.0), PlannedMovementMode.DIRECT, true),
                new CameraAngles(0.0, 0.0),
                ActionIntent.none(),
                new SpeedIntent(1.0, true),
                ToleranceProfile.standard(),
                LocomotionExecutionState.start(),
                false);
        return new NavigationControlFrame(
                new NavigationControllerState(PathProgress.start(), intent, LocomotionExecutionState.start()),
                intent,
                new CameraAngles(0.0, 0.0),
                target,
                plan,
                false);
    }
}
